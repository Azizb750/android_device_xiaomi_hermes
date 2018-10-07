LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libpng_filter
LOCAL_SRC_FILES_64 := libpng_filter.a
LOCAL_SRC_FILES_32 := arm/libpng_filter.a
LOCAL_MULTILIB := both
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_MODULE_SUFFIX := .a
include $(BUILD_PREBUILT)
