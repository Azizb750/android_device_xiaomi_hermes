LOCAL_PATH := $(call my-dir)

# libshim_audio
include $(CLEAR_VARS)
LOCAL_SRC_FILES := audio.cpp
LOCAL_SHARED_LIBRARIES := libbinder libmedia
LOCAL_MODULE := libshim_audio
LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
include $(BUILD_SHARED_LIBRARY)

# libshim_xlog
include $(CLEAR_VARS)
LOCAL_SRC_FILES := xlog.c
LOCAL_SHARED_LIBRARIES := libbinder liblog
LOCAL_MODULE := libshim_xlog
LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
include $(BUILD_SHARED_LIBRARY)
