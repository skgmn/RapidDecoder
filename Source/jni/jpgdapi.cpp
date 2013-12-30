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
    java_input_stream* jis = new java_input_stream(env, in);
    jpeg_decoder* decoder = new jpeg_decoder(jis);

    return (jlong)decoder;
}

extern "C" JNIEXPORT
void JNICALL Java_agu_bitmap_jpeg_JpegDecoder_destroyNativeDecoder(JNIEnv* env, jclass clazz,
	jlong decoderPtr)
{
    jpeg_decoder* decoder = (jpeg_decoder*)decoderPtr;
    jpeg_decoder_stream* stream = decoder->get_stream();
    delete stream;
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

// java_input_stream

java_input_stream::java_input_stream(JNIEnv* env, jobject in)
    : jpeg_decoder_stream()
{
    env->GetJavaVM(&jvm);

    jclass InputStream = env->FindClass("java/io/InputStream");
    InputStream_close = env->GetMethodID(InputStream, "close", "()V");
    InputStream_read3 = env->GetMethodID(InputStream, "read", "([BII)I");

    this->in = env->NewGlobalRef(in);
    this->inBuf = NULL;
}

java_input_stream::~java_input_stream()
{
    JNIEnv* env;
    jvm->GetEnv((void**)&env, JNI_VERSION_1_6);

    if (this->inBuf != NULL)
    {
        env->DeleteGlobalRef(this->inBuf);
    }

    env->CallVoidMethod(this->in, InputStream_close);
    env->DeleteGlobalRef(this->in);
}

int java_input_stream::read(uint8 *pBuf, int max_bytes_to_read, bool *pEOF_flag)
{
    JNIEnv* env;
    jvm->GetEnv((void**)&env, JNI_VERSION_1_6);

    jsize bufSize = (this->inBuf == NULL ? 0 : env->GetArrayLength(this->inBuf));
    if (bufSize < max_bytes_to_read)
    {
        if (this->inBuf != NULL)
        {
            env->DeleteGlobalRef(this->inBuf);
        }
        this->inBuf = (jbyteArray) env->NewGlobalRef(env->NewByteArray(max_bytes_to_read * 3 / 2));
    }

    jint bytesRead = env->CallIntMethod(this->in, InputStream_read3, this->inBuf, 0, max_bytes_to_read);
    if (env->ExceptionOccurred() != NULL)
    {
        return -1;
    }

    if (bytesRead < 0)
    {
        *pEOF_flag = true;
        return 0;
    }
    else
    {
        jbyte* buf = env->GetByteArrayElements(this->inBuf, NULL);
        memcpy(pBuf, buf, bytesRead);
        env->ReleaseByteArrayElements(this->inBuf, buf, JNI_ABORT);

        *pEOF_flag = false;
        return bytesRead;
    }
}