# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_DEFAULT_CPP_EXTENSION := cpp    
LOCAL_MODULE := decoder
LOCAL_LDLIBS += -ljnigraphics -llog -lz
LOCAL_SRC_FILES := jpgd.cpp decoder.cpp pixelcomposer.cpp sampler.cpp pngdecoder.cpp \
    libpng/png.c libpng/pngerror.c libpng/pngget.c libpng/pngmem.c libpng/pngpread.c libpng/pngread.c libpng/pngrio.c libpng/pngrtran.c libpng/pngrutil.c libpng/pngset.c \
    libpng/pngtrans.c
LOCAL_CFLAGS += -DNDEBUG -DLOG

include $(BUILD_SHARED_LIBRARY)
