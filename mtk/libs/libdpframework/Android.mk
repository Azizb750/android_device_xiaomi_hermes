LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libdpframework
LOCAL_SRC_FILES_64 := libdpframework.so
LOCAL_SRC_FILES_32 := arm/libdpframework.so
LOCAL_SHARED_LIBRARIES := libion libion_mtk libm4u libpq_prot libstdc++ libstlport
LOCAL_MULTILIB := both
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
include $(BUILD_PREBUILT)
