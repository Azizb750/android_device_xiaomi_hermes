LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libJpgDecPipe
LOCAL_SRC_FILES_64 := libJpgDecPipe.so
LOCAL_SRC_FILES_32 := arm/libJpgDecPipe.so
LOCAL_SHARED_LIBRARIES := libGdmaScalerPipe libSwJpgCodec libion libion_mtk libm4u libstdc++
LOCAL_MULTILIB := both
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
include $(BUILD_PREBUILT)
