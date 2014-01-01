#include "jpgdapi.h"

using namespace jpgd;

#define min(a, b) ((a) < (b) ? (a) : (b))

JavaVM* jvm = NULL;

// Java apis

extern "C" JNIEXPORT
void JNICALL Java_agu_bitmap_jpeg_JpegDecoder_init(JNIEnv* env, jclass clazz)
{
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
jint JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeDecode(JNIEnv* env, jclass clazz,
	jlong decoderPtr, jintArray outPixels)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    
    const void* buffer;
    uint len;
    int ret = decoder->decode(&buffer, &len);

    jsize arrayLen = env->GetArrayLength(outPixels);
    jint* dest = env->GetIntArrayElements(outPixels, NULL);
    memcpy(dest, buffer, min(len, arrayLen * sizeof(jint)));
    env->ReleaseIntArrayElements(outPixels, dest, JNI_ABORT);

    switch (ret)
    {
    case JPGD_SUCCESS: return len;
    case JPGD_DONE: return 0;
    default: return -1;
    }
}

extern "C" JNIEXPORT
jint JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeSkipLine(JNIEnv* env, jclass clazz,
	jlong decoderPtr)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    
    uint len;
    int ret = decoder->skip(&len);

    switch (ret)
    {
    case JPGD_SUCCESS: return len;
    case JPGD_DONE: return 0;
    default: return -1;
    }
}

extern "C" JNIEXPORT
void JNICALL Java_agu_bitmap_jpeg_JpegDecoder_nativeSliceColumn(JNIEnv* env, jclass clazz,
	jlong decoderPtr, jint offset, jint length)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    decoder->set_column_offset(offset);
    decoder->set_column_length(length);
}