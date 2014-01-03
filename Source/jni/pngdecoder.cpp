#include "pngdecoder.h"

using namespace agu;

png_decoder::png_decoder(JNIEnv* env, jobject in)
{
    m_env = env;
    m_in = env->NewGlobalRef(in);

    jclass InputStream = env->FindClass("java/io/InputStream");
    InputStream_close = env->GetMethodID(InputStream, "close", "()V");
    InputStream_read1 = env->GetMethodID(InputStream, "read", "([BII)I");
}

bool png_decoder::begin()
{
    //Allocate a buffer of 8 bytes, where we can put the file signature.
    jobject pngsig = m_env->NewByteArray(PNGSIGSIZE);
    int is_png = 0;

    jint bytesRead = m_env->CallIntMethod(m_in, InputStream_read1, 0, PNGSIGSIZE);
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
    bool is_png = (png_sig_cmp(pngsig, 0, PNGSIGSIZE) == 0);
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

    if (m_color_type == PNG_COLOR_TYPE_PALETTE)
        png_set_palette_to_rgb(m_png);

    if (m_color_type == PNG_COLOR_TYPE_GRAY &&
        bit_depth < 8) png_set_gray_1_2_4_to_8(m_png);

    if (png_get_valid(m_png, info_ptr,
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
    if (format == RGB565)
    {
        if (color_type & PNG_COLOR_MASK_ALPHA)
            png_set_strip_alpha(png_ptr);
        png_set_read_user_transform_fn(m_png, rgb565_transform);
        png_set_user_transform_info(m_png, this, 8, 2);
    }
    else if (format == RGB888)
    {
        if (color_type & PNG_COLOR_MASK_ALPHA)
            png_set_strip_alpha(png_ptr);
    }
    else if (format == RGBA4444)
    {
        if (!(color_type & PNG_COLOR_MASK_ALPHA))
            png_set_filler(m_png, 0xff, PNG_FILL_AFTER);
        png_set_read_user_transform_fn(m_png, rgba4444_transform);
        png_set_user_transform_info(m_png, this, 8, 2);
    }
    else if (format == RGBA8888)
    {
        if (!(color_type & PNG_COLOR_MASK_ALPHA))
            png_set_filler(m_png, 0xff, PNG_FILL_AFTER);
    }
    else if (format == ARGB8888)
    {
        if (!(color_type & PNG_COLOR_MASK_ALPHA))
            png_set_filler(m_png, 0xff, PNG_FILL_BEFORE);
        if (color_type == PNG_COLOR_TYPE_RGB_ALPHA)
            png_set_swap_alpha(m_png);
    }
}

void png_decoder::rgb565_transform(png_ptr png, row_info_ptr row_info, png_bytep data)
{
    // rgb888 -> rgb565

    png_decoder* d = (png_decoder*)png_get_user_transform_ptr(png);

    uint8* out = data;
    for (int i = 0; i < row_info.width; ++i)
    {
        uint8 r = data[i];
        uint8 g = data[i + 1];
        uint8 b = data[i + 2];

        RGB565.composer(out, 0xff, r, g, b);
        data += 3;
    }
}

void png_decoder::rgba4444_transform(png_ptr png, row_info_ptr row_info, png_bytep data)
{
    // rgba8888 -> rgba4444

    png_decoder* d = (png_decoder*)png_get_user_transform_ptr(png);

    uint8* out = data;
    for (int i = 0; i < row_info.width; ++i)
    {
        uint8 r = data[i];
        uint8 g = data[i + 1];
        uint8 b = data[i + 2];
        uint8 a = data[i + 3];

        RGBA4444.composer(out, a, r, g, b);
        data += 4;
    }
}

void png_decoder::rgba4444_transform(png_ptr ptr, row_info_ptr row_info, png_bytep data)
{
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

    jint bytesRead = env->CallIntMethod(in, decoder->InputStream_read1, buf, 0, length);
    if (env->ExceptionOccurred() != NULL)
    {
        env->ExceptionClear();
        png_error();
    }
    else if (bytesRead < 0)
    {
        png_error();
    }

    env->GetByteArrayRegion(buf, 0, length, data);
}

png_decoder::~png_decoder()
{
    m_env->DeleteGlobalRef(m_in_buf);
    m_env->DeleteGlobalRef(m_in);

    if (m_png)
    {
        png_destroy_read_struct(&m_png,
            m_info != NULL ? &m_info : NULL,
            NULL);
    }
}