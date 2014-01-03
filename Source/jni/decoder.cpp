#include "decoder.h"
#include "sampler.h"

#include <android/log.h>

using namespace jpgd;

#define min(a, b) ((a) < (b) ? (a) : (b))

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libnav", __VA_ARGS__) 

jclass Bitmap;
jmethodID Bitmap_createBitmap1;
jmethodID Bitmap_recycle;

jfieldID Options_inSampleSize;
jfieldID Options_mCancel;

// Java apis

extern "C" JNIEXPORT
void JNICALL Java_agu_bitmap_AguDecoder_init(JNIEnv* env, jclass clazz)
{
    Bitmap = (jclass) env->NewGlobalRef(env->FindClass("android/graphics/Bitmap"));
    Bitmap_createBitmap1 = env->GetStaticMethodID(Bitmap, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    Bitmap_recycle = env->GetMethodID(Bitmap, "recycle", "()V");

    jclass Options = env->FindClass("android/graphics/BitmapFactory$Options");
    Options_inSampleSize = env->GetFieldID(Options, "inSampleSize", "I");
    Options_mCancel = env->GetFieldID(Options, "mCancel", "Z");
}

extern "C" JNIEXPORT
jlong JNICALL Java_agu_bitmap_jpeg_JpegDecoder_createNativeDecoder(JNIEnv* env, jclass clazz,
	jobject in)
{
    jpeg_decoder* decoder = new jpeg_decoder(env, in);
    return (jlong)decoder;
}

extern "C" JNIEXPORT
void JNICALL Java_agu_bitmap_jpeg_JpegDecoder_destroyNativeDecoder(JNIEnv* env, jclass clazz,
	jlong decoderPtr)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    delete decoder;
}

extern "C" JNIEXPORT
jboolean JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeBegin(JNIEnv* env, jclass clazz,
	jlong decoderPtr)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    return decoder->begin_decoding() == JPGD_SUCCESS;
}

extern "C" JNIEXPORT
jint JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeGetBytesPerPixel(JNIEnv* env, jclass clazz,
	jlong decoderPtr)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    return decoder->get_bytes_per_pixel();
}

extern "C" JNIEXPORT
jint JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeGetWidth(JNIEnv* env, jclass clazz,
	jlong decoderPtr)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    return decoder->get_width();
}

extern "C" JNIEXPORT
jint JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeGetHeight(JNIEnv* env, jclass clazz,
	jlong decoderPtr)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    return decoder->get_height();
}

extern "C" JNIEXPORT
jobject JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeDecode(JNIEnv* env, jclass clazz,
	jlong decoderPtr, jint left, jint top, jint right, jint bottom, jboolean filter, jobject config, jobject opts)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;

    if (left < 0)
    {
        left = top = 0;
        right = decoder->get_width();
        bottom = decoder->get_height();
    }

    int w = right - left;
    int h = bottom - top;

    jint sample_size = env->GetIntField(opts, Options_inSampleSize);
    int sampled_width = (sample_size > 1 ? w / sample_size : w);
    int sampled_height = (sample_size > 1 ? h / sample_size : h);

    jobject bitmap = env->CallStaticObjectMethod(Bitmap, Bitmap_createBitmap1, sampled_width, sampled_height, config);

	AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);

    pixel_format format;

    switch (info.format)
    {
    case ANDROID_BITMAP_FORMAT_RGBA_8888: format = RGBA8888; break;
    case ANDROID_BITMAP_FORMAT_RGBA_4444: format = RGBA4444; break;
    case ANDROID_BITMAP_FORMAT_RGB_565: format = RGB565; break;
    default:
        env->CallVoidMethod(bitmap, Bitmap_recycle);
        return NULL;
    }

    opaque_sampler* sampler = NULL;

    if (sample_size > 1)
    {
        sampler = new opaque_sampler(sampled_width, sample_size, filter, format);
        decoder->set_pixel_format(RGB888);
    }
    else
    {
        decoder->set_pixel_format(format);
    }

    uint8* pixels;
    AndroidBitmap_lockPixels(env, bitmap, (void**)&pixels);

    decoder->set_column_offset(left);
    decoder->set_column_length(w);

    const void* buffer;
    uint bytes_read;
    
    int dest_bypp = decoder->get_bytes_per_pixel();
    uint scan_line_length = w * dest_bypp;

    for (int i = 0; i < top; ++i)
    {
        int ret = decoder->skip(&bytes_read);
        if (ret == JPGD_DONE || ret == JPGD_FAILED ||
            bytes_read < scan_line_length ||
            env->GetBooleanField(opts, Options_mCancel)) goto canceled;
    }

    for (int i = top; i < bottom; ++i)
    {
        int ret = decoder->decode(&buffer, &bytes_read);
        if (ret == JPGD_DONE || ret == JPGD_FAILED ||
            bytes_read < scan_line_length ||
            env->GetBooleanField(opts, Options_mCancel)) goto canceled;

        if (sample_size > 1)
        {
            if (sampler->sample((const uint8*)buffer, 0, w, pixels))
            {
                pixels += info.stride;
            }
        }
        else
        {
            memcpy(pixels, buffer, scan_line_length);
            pixels += info.stride;
        }
    }

    if (sample_size > 1)
    {
        delete sampler;
    }
    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;

canceled:
    if (sample_size > 1)
    {
        delete sampler;
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    env->CallVoidMethod(bitmap, Bitmap_recycle);
    return NULL;
}