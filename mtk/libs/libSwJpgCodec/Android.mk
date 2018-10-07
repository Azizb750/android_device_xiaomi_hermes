LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libSwJpgCodec
LOCAL_SRC_FILES_64 := libSwJpgCodec.so
LOCAL_SRC_FILES_32 := arm/libSwJpgCodec.so
LOCAL_SHARED_LIBRARIES := libmtkjpeg libstdc++
LOCAL_MULTILIB := both
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
include $(BUILD_PREBUILT)
