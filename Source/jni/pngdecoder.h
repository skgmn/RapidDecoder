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

    private:
        JNIEnv* m_env;
        jmethodID InputStream_close;
        jmethodID InputStream_read1;

        jobject m_in;
        jbyteArray m_in_buf;
        png_structp m_png;
        png_infop m_info;
        pixel_format m_format;

        png_uint_32 m_width;
        png_uint_32 m_height;
        png_uint_32 m_color_type;

        static void input_stream_reader(png_structp png, png_bytep data, png_size_t length);
        static void rgb565_transform(png_ptr ptr, row_info_ptr row_info, png_bytep data);
        static void rgba4444_transform(png_ptr ptr, row_info_ptr row_info, png_bytep data);
    }
}

#endif // PNGDECODER_H