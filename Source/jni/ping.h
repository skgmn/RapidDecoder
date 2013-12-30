#ifndef LOG_H
#define LOG_H

#include <android/log.h>

#define PINGN(n) __android_log_print(ANDROID_LOG_ERROR, "PingFromNativeCode", "Ping %d", n)
#define PINGS(s, ...) __android_log_print(ANDROID_LOG_ERROR, "PingFromNativeCode", s, __VA_ARGS__)
#define PING() PINGN(__COUNTER__)

#endif // LOG_H