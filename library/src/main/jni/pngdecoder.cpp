#include <stdlib.h>

#include "pngdecoder.h"

#define PNGSIGSIZE 8

using namespace agu;

typedef unsigned char uint8;

png_decoder::png_decoder(JNIEnv* env, jobject in)
{
    m_env = env;
    m_in = env->NewGlobalRef(in);

    jclass InputStream = env->FindClass("java/io/InputStream");
    InputStream_read1 = env->GetMethodID(InputStream, "read", "([BII)I");

    m_interlace_loaded = false;
    m_in_buf = NULL;
    m_png = NULL;
    m_scanline_buffer = NULL;
    m_rowbytes = 0;
    m_col_offset = 0;
    m_col_length = -1;
}

bool png_decoder::begin()
{
    //Allocate a buffer of 8 bytes, where we can put the file signature.
    jbyteArray pngsig = m_env->NewByteArray(PNGSIGSIZE);

    jint bytesRead = m_env->CallIntMethod(m_in, InputStream_read1, pngsig, 0, PNGSIGSIZE);
    if (m_env->ExceptionOccurred() != NULL)
    {
        m_env->ExceptionClear();
        return JNI_FALSE;
    }
    else if (bytesRead < 0)
    {
        return JNI_FALSE;
    }

    jbyte* pngsig_bytes = m_env->GetByteArrayElements(pngsig, NULL);

    //Let LibPNG check the sig. If this function returns 0, everything is OK.
    bool is_png = (png_sig_cmp((png_const_bytep)pngsig_bytes, 0, PNGSIGSIZE) == 0);
    m_env->ReleaseByteArrayElements(pngsig, pngsig_bytes, JNI_ABORT);

    if (!is_png)
        return JNI_FALSE;

    m_png = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if (!m_png)
        return JNI_FALSE;

    m_info = png_create_info_struct(m_png);
    if (!m_info)
        return JNI_FALSE;

    if (setjmp(png_jmpbuf(m_png)))
    {
        return JNI_FALSE;
    }

    png_set_read_fn(m_png, this, input_stream_reader);
    png_set_sig_bytes(m_png, PNGSIGSIZE);

    png_read_info(m_png, m_info);

    m_width =  png_get_image_width(m_png, m_info);
    m_height = png_get_image_height(m_png, m_info);

    png_uint_32 bit_depth   = png_get_bit_depth(m_png, m_info);
    png_uint_32 channels   = png_get_channels(m_png, m_info);
    m_color_type = png_get_color_type(m_png, m_info);
    m_interlace_type = png_get_interlace_type(m_png, m_info);
    m_rowbytes = png_get_rowbytes(m_png, m_info);

    if (m_color_type == PNG_COLOR_TYPE_PALETTE)
        png_set_palette_to_rgb(m_png);

    if (m_color_type == PNG_COLOR_TYPE_GRAY &&
        bit_depth < 8) png_set_expand_gray_1_2_4_to_8(m_png);

    if (png_get_valid(m_png, m_info,
        PNG_INFO_tRNS)) png_set_tRNS_to_alpha(m_png);

    if (bit_depth == 16)
        png_set_strip_16(m_png);

    if (bit_depth < 8)
        png_set_packing(m_png);

    if (m_color_type == PNG_COLOR_TYPE_GRAY ||
        m_color_type == PNG_COLOR_TYPE_GRAY_ALPHA)
          png_set_gray_to_rgb(m_png);

    return JNI_TRUE;
}

void png_decoder::set_pixel_format(const pixel_format& format)
{
    if (m_format == format) return;
    m_format = format;

    if (m_scanline_buffer)
    {
        delete [] m_scanline_buffer;
        m_scanline_buffer = NULL;
    }

    if (format == RGB888 || format == RGB565)
    {
        // RGB888
        if (m_color_type & PNG_COLOR_MASK_ALPHA)
            png_set_strip_alpha(m_png);
    }
    else if (format == RGBA8888 || format == RGBA4444)
    {
        // RGBA888
        if (!(m_color_type & PNG_COLOR_MASK_ALPHA))
            png_set_filler(m_png, 0xff, PNG_FILLER_AFTER);
    }
    else if (format == ARGB8888)
    {
        if (!(m_color_type & PNG_COLOR_MASK_ALPHA))
            png_set_filler(m_png, 0xff, PNG_FILLER_BEFORE);
        if (m_color_type == PNG_COLOR_TYPE_RGB_ALPHA)
            png_set_swap_alpha(m_png);
    }
}

void png_decoder::input_stream_reader(png_structp png, png_bytep data, png_size_t length)
{
    png_decoder* decoder = (png_decoder*)png_get_io_ptr(png);
    JNIEnv* env = decoder->m_env;

    jbyteArray buf = decoder->m_in_buf;
    if (buf == NULL || env->GetArrayLength(buf) < length)
    {
        env->DeleteGlobalRef(decoder->m_in_buf);
        buf = decoder->m_in_buf = (jbyteArray)env->NewGlobalRef(env->NewByteArray(length));
    }

    jobject in = decoder->m_in;
    int offset = 0;
    do
    {
        jint bytesRead = env->CallIntMethod(in, decoder->InputStream_read1, buf, offset, length);
        if (env->ExceptionOccurred() != NULL)
        {
            env->ExceptionClear();
            png_error(png, "");
        }
        else if (bytesRead < 0)
        {
            png_error(png, "");
        }

        offset += bytesRead;
        length -= bytesRead;
    }
    while (length > 0);

    env->GetByteArrayRegion(buf, 0, offset, (jbyte*)data);
}

bool png_decoder::read_row(uint8* out)
{
    if (setjmp(png_jmpbuf(m_png)))
    {
        return false;
    }

    if (m_interlace_type != 0 && !m_interlace_loaded)
    {
        unsigned char* scanline_buffer = get_scanline_buffer();
        int number_of_passes = png_set_interlace_handling(m_png);

        for (int i = 0; i < number_of_passes; ++i)
        {
            for (int j = 0; j < m_height; ++j)
            {
                png_read_row(m_png, scanline_buffer, NULL);
            }
        }

        m_interlace_loaded = true;
    }

    unsigned int col_length = (m_col_length >= 0 ? m_col_length : m_width);

    if (m_format == RGB565)
    {
        // RGB888 to RGB565

        unsigned char* scanline_buffer = get_scanline_buffer();
        png_read_row(m_png, scanline_buffer, NULL);

        if (out)
        {
            scanline_buffer += m_col_offset * 3;
            for (int i = 0; i < col_length; ++i)
            {
                m_format.composer(out, 0xff, scanline_buffer[0], scanline_buffer[1], scanline_buffer[2]);
                scanline_buffer += 3;
            }
        }
    }
    else if (m_format == RGBA4444)
    {
        // RGBA888 to RGBA4444

        unsigned char* scanline_buffer = get_scanline_buffer();
        png_read_row(m_png, scanline_buffer, NULL);

        if (out)
        {
            scanline_buffer += m_col_offset * 4;
            for (int i = 0; i < col_length; ++i)
            {
                m_format.composer(out, scanline_buffer[3], scanline_buffer[0], scanline_buffer[1], scanline_buffer[2]);
                scanline_buffer += 4;
            }
        }
    }
    else
    {
        if (m_col_offset == 0 && col_length >= m_width)
        {
            if (out)
            {
                png_read_row(m_png, out, NULL);
            }
            else
            {
                png_read_row(m_png, get_scanline_buffer(), NULL);
            }
        }
        else
        {
            unsigned int bytes_per_pixel = (m_format == RGB888 ? 3 : 4);
            unsigned char* scanline_buffer = get_scanline_buffer();
            png_read_row(m_png, scanline_buffer, NULL);

            if (out)
            {
                memcpy(out, &scanline_buffer[m_col_offset * bytes_per_pixel], col_length * bytes_per_pixel);
            }
        }
    }

    return true;
}

unsigned char* png_decoder::get_scanline_buffer()
{
    if (!m_scanline_buffer)
    {
        unsigned int rowbytes;
        if (m_format.alpha)
        {
            rowbytes = m_width * 4;
        }
        else
        {
            rowbytes = m_width * 3;
        }

        m_scanline_buffer = new unsigned char [rowbytes];
    }
    return m_scanline_buffer;
}

png_decoder::~png_decoder()
{
    if (m_in_buf)
    {
        m_env->DeleteGlobalRef(m_in_buf);
    }
    m_env->DeleteGlobalRef(m_in);

    if (m_scanline_buffer)
    {
        delete [] m_scanline_buffer;
    }

    if (m_png)
    {
        png_destroy_read_struct(&m_png,
            m_info != NULL ? &m_info : NULL,
            NULL);
    }
}