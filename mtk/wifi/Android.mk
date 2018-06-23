LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS += \
	-Wno-unused-parameter \
	-Wno-int-to-pointer-cast \
	-Wno-maybe-uninitialized \
	-Wno-parentheses

LOCAL_CPPFLAGS += -Wno-conversion-null

LOCAL_C_INCLUDES += \
	external/libnl/include \
	$(call include-path-for, libhardware_legacy)/hardware_legacy \
	external/wpa_supplicant_8/src/drivers

LOCAL_SRC_FILES := \
	wifi_hal.cpp \
	rtt.cpp \
	common.cpp \
	cpp_bindings.cpp \
	gscan.cpp \
	link_layer_stats.cpp

LOCAL_MODULE := libwifi-hal-mt66xx

include $(BUILD_STATIC_LIBRARY)

