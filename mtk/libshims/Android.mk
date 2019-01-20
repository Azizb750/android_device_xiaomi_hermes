LOCAL_PATH := $(call my-dir)

# libshim_c
include $(CLEAR_VARS)
LOCAL_SRC_FILES := libc.cpp
# libc is going to be linked with libshim_c via patch to account for libhybris
# without LD_SHIM_LIBS support, so avoid creating circular dependency here
LOCAL_SYSTEM_SHARED_LIBRARIES :=
LOCAL_CXX_STL := none
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_MODULE := libshim_c
LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)

# libshim_media
include $(CLEAR_VARS)
LOCAL_SRC_FILES := libmedia.cpp
LOCAL_SHARED_LIBRARIES := libbinder libmedia
LOCAL_MODULE := libshim_media
LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)

# libshim_stagefright
include $(CLEAR_VARS)
LOCAL_SRC_FILES := libstagefright.cpp
LOCAL_SHARED_LIBRARIES := libbinder libstagefright
LOCAL_MODULE := libshim_stagefright
LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)

# libshim_xlog
include $(CLEAR_VARS)
LOCAL_SRC_FILES := liblog.c
LOCAL_MODULE := libshim_xlog
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)

# libshim_netutils
include $(CLEAR_VARS)
LOCAL_SRC_FILES := libnetutils.cpp
LOCAL_SHARED_LIBRARIES := libbinder libnetutils liblog
LOCAL_MODULE := libshim_netutils
LOCAL_CFLAGS := -Wno-unused-variable -Wno-unused-parameter
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_SHARED_LIBRARY)
