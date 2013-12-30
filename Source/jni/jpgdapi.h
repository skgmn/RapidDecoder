#ifndef JPGDAPI_H
#define JPGDAPI_H

#include <jni.h>
#include "jpgd.h"

class java_input_stream : public jpgd::jpeg_decoder_stream
{
public:
    java_input_stream(JNIEnv* env, jobject in);
    ~java_input_stream();

    int read(jpgd::uint8 *pBuf, int max_bytes_to_read, bool *pEOF_flag);

private:
    JavaVM* jvm;
    jmethodID InputStream_close;
    jmethodID InputStream_read3;

    jobject in;
    jbyteArray inBuf;
};

#endif // JPGDAPI_H
