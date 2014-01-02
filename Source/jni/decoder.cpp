#include "decoder.h"

using namespace jpgd;

#define min(a, b) ((a) < (b) ? (a) : (b))

jclass Bitmap;
jmethodID Bitmap_createBitmap1;
jmethodID Bitmap_recycle;

jfieldID Options_mCancel;

// Java apis

extern "C" JNIEXPORT
void JNICALL Java_agu_bitmap_AguDecoder_init(JNIEnv* env, jclass clazz)
{
    Bitmap = (jclass) env->NewGlobalRef(env->FindClass("android/graphics/Bitmap"));
    Bitmap_createBitmap1 = env->GetStaticMethodID(Bitmap, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    Bitmap_recycle = env->GetMethodID(Bitmap, "recycle", "()V");

    jclass Options = env->FindClass("android/graphics/BitmapFactory$Options");
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
	jlong decoderPtr, jint left, jint top, jint right, jint bottom, jobject config, jobject opts)
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

    jobject bitmap = env->CallStaticObjectMethod(Bitmap, Bitmap_createBitmap1, w, h, config);

	AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);

    switch (info.format)
    {
    case ANDROID_BITMAP_FORMAT_RGBA_8888: decoder->set_output_pixel_format(RGBA); break;
    case ANDROID_BITMAP_FORMAT_RGBA_4444: decoder->set_output_pixel_format(ARGB4444); break;
    case ANDROID_BITMAP_FORMAT_RGB_565: decoder->set_output_pixel_format(RGB565); break;
    default:
        env->CallVoidMethod(bitmap, Bitmap_recycle);
        return NULL;
    }

    uint8* pixels;
    AndroidBitmap_lockPixels(env, bitmap, (void**)&pixels);

    decoder->set_column_offset(left);
    decoder->set_column_length(w);

    const void* buffer;
    uint bytes_read;
    
    int dest_bypp;
    uint dest_stride;
    int offset;

    for (int i = 0; i < top; ++i)
    {
        int ret = decoder->skip(&bytes_read);
        if (ret == JPGD_DONE || ret == JPGD_FAILED ||
            env->GetBooleanField(opts, Options_mCancel)) goto canceled;
    }

    pixels += top * info.stride;

    dest_bypp = decoder->get_dest_bytes_per_pixel();
    dest_stride = w * dest_bypp;
    offset = left * dest_bypp;

    for (int i = top; i < bottom; ++i)
    {
        int ret = decoder->decode(&buffer, &bytes_read);
        if (ret == JPGD_DONE || ret == JPGD_FAILED ||
            env->GetBooleanField(opts, Options_mCancel)) goto canceled;

        uint bytes_to_copy = min(dest_stride, bytes_read);
        memcpy(&pixels[offset], buffer, bytes_to_copy);

        pixels += info.stride;
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return bitmap;

canceled:
    AndroidBitmap_unlockPixels(env, bitmap);
    env->CallVoidMethod(bitmap, Bitmap_recycle);
    return NULL;
}