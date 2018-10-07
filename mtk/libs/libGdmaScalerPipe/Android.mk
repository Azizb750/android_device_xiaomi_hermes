LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libGdmaScalerPipe
LOCAL_SRC_FILES_64 := libGdmaScalerPipe.so
LOCAL_SRC_FILES_32 := arm/libGdmaScalerPipe.so
LOCAL_SHARED_LIBRARIES := libdpframework libstdc++
LOCAL_MULTILIB := both
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
include $(BUILD_PREBUILT)
