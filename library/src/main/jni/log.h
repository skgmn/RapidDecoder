#ifndef LOG_H
#define LOG_H

#ifdef LOG
#include <android/log.h>
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "asdf", __VA_ARGS__) 
#endif

#endif // LOG_H