LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libskia_graphics_opt
LOCAL_SRC_FILES_64 := libskia_graphics_opt.a
LOCAL_SRC_FILES_32 := arm/libskia_graphics_opt.a
LOCAL_MULTILIB := both
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_MODULE_SUFFIX := .a
include $(BUILD_PREBUILT)
