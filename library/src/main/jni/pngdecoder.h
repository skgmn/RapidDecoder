#ifndef PNGDECODER_H
#define PNGDECODER_H

#include <jni.h>

#include "pixelcomposer.h"
#include "libpng/png.h"

namespace agu
{
    class png_decoder
    {
    public:
        png_decoder(JNIEnv* env, jobject in);
        ~png_decoder();

        bool begin();
        void set_pixel_format(const pixel_format& format);
        bool read_row(unsigned char* out);

        inline uint get_bytes_per_row() const { return m_rowbytes; }
        inline uint get_width() const { return m_width; }
        inline uint get_height() const { return m_height; }
        inline bool has_alpha() const { return (m_color_type & PNG_COLOR_MASK_ALPHA); }

        inline void slice(unsigned int col_offset, int col_length)
        {
            m_col_offset = col_offset;
            m_col_length = col_length;
        }

    private:
        JNIEnv* m_env;
        jmethodID InputStream_read1;

        jobject m_in;
        jbyteArray m_in_buf;
        png_structp m_png;
        png_infop m_info;
        pixel_format m_format;

        png_uint_32 m_width;
        png_uint_32 m_height;
        png_uint_32 m_color_type;
        png_uint_32 m_rowbytes;
        uint m_interlace_type;
        bool m_interlace_loaded;
        unsigned char* m_scanline_buffer;
        unsigned int m_col_offset;
        int m_col_length;

        static void input_stream_reader(png_structp png, png_bytep data, png_size_t length);

        unsigned char* get_scanline_buffer();
    };
}

#endif // PNGDECODER_H