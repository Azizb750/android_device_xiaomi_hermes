LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libmhalImageCodec
LOCAL_SRC_FILES_64 := libmhalImageCodec.so
LOCAL_SRC_FILES_32 := arm/libmhalImageCodec.so
LOCAL_SHARED_LIBRARIES := libJpgDecPipe libstdc++
LOCAL_MULTILIB := both
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
include $(BUILD_PREBUILT)
