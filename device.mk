LOCAL_PATH := device/xiaomi/hermes

# Vendor
$(call inherit-product, vendor/xiaomi/hermes/hermes-vendor.mk)

# Dalvik/HWUI
$(call inherit-product-if-exists, frameworks/native/build/phone-xxhdpi-2048-dalvik-heap.mk)
$(call inherit-product-if-exists, frameworks/native/build/phone-xxhdpi-2048-hwui-memory.mk)

# Overlays
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# Display
TARGET_SCREEN_HEIGHT := 1920
TARGET_SCREEN_WIDTH := 1080

# Screen density
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xxhdpi

# Recovery allowed devices
TARGET_OTA_ASSERT_DEVICE := hermes

# Compile apps
PRODUCT_PACKAGES += Snap

# Shim symbols
PRODUCT_PACKAGES += \
    libshim_c \
    libshim_camera_client \
    libshim_cutils \
    libshim_media \
    libshim_stagefright \
    libshim_ui \
    libshim_xlog

# Power HAL
PRODUCT_PACKAGES += power.mt6795

# Wi-Fi
PRODUCT_PACKAGES += \
    hostapd \
    lib_driver_cmd_mt66xx \
    libwpa_client \
    wpa_supplicant

# Graphic
PRODUCT_PACKAGES += \
    libGLES_android \
    libion

# Doze
#PRODUCT_PACKAGES += XiaomiDoze

# GPS
PRODUCT_PACKAGES += libcurl

# DRM
PRODUCT_PACKAGES += libmockdrmcryptoplugin

# FM Radio
#PRODUCT_PACKAGES += \
#	FMRadio \
#	libfmcust
	
# Audio
PRODUCT_PACKAGES += \
    audio.a2dp.default \
    audio.r_submix.default \
    audio.usb.default \
    libaudio-resampler \
    libtinyalsa \
    libtinycompress \
    libtinyxml

# Rootdir
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/fstab.mt6795:root/fstab.mt6795 \
    $(LOCAL_PATH)/rootdir/init.mt6795.rc:root/init.mt6795.rc \
    $(LOCAL_PATH)/rootdir/init.mt6795.usb.rc:root/init.mt6795.usb.rc \
    $(LOCAL_PATH)/rootdir/ueventd.mt6795.rc:root/ueventd.mt6795.rc \
    $(LOCAL_PATH)/rootdir/variant_check.sh:root/variant_check.sh

# Permissions
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.audio.low_latency.xml:/system/vendor/lib64/permissions/android.hardware.audio.low_latency.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:system/vendor/etc/permissions/android.hardware.bluetooth_le.xml \
    frameworks/native/data/etc/android.hardware.bluetooth.xml:/system/vendor/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.camera.autofocus.xml:/system/vendor/etc/permissions/android.hardware.camera.autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.flash-autofocus.xml:/system/vendor/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.front.xml:/system/vendor/etc/permissions/android.hardware.camera.front.xml \
    frameworks/native/data/etc/android.hardware.consumerir.xml:/system/vendor/etc/permissions/android.hardware.consumerir.xml \
    frameworks/native/data/etc/android.hardware.ethernet.xml:/system/vendor/etc/permissions/android.hardware.ethernet.xml \
    frameworks/native/data/etc/android.hardware.location.gps.xml:/system/vendor/etc/permissions/android.hardware.location.gps.xml \
    frameworks/native/data/etc/android.hardware.sensor.accelerometer.xml:/system/vendor/etc/permissions/android.hardware.sensor.accelerometer.xml \
    frameworks/native/data/etc/android.hardware.sensor.compass.xml:/system/vendor/etc/permissions/android.hardware.compass.xml \
    frameworks/native/data/etc/android.hardware.sensor.gyroscope.xml:/system/vendor/etc/permissions/android.hardware.sensor.gyroscope.xml \
    frameworks/native/data/etc/android.hardware.sensor.light.xml:/system/vendor/etc/permissions/android.hardware.sensor.light.xml \
    frameworks/native/data/etc/android.hardware.sensor.proximity.xml:/system/vendor/etc/permissions/android.hardware.sensor.proximity.xml \
    frameworks/native/data/etc/android.hardware.sensor.stepcounter.xml:/system/vendor/etc/permissions/android.hardware.sensor.stepcounter.xml \
    frameworks/native/data/etc/android.hardware.telephony.cdma.xml:/system/vendor/etc/permissions/android.hardware.telephony.cdma.xml \
    frameworks/native/data/etc/android.hardware.telephony.gsm.xml:/system/vendor/etc/permissions/android.hardware.telephony.gsm.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.distinct.xml:/system/vendor/etc/permissions/android.hardware.touchscreen.multitouch.distinct.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:/system/vendor/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.xml:/system/vendor/etc/permissions/android.hardware.touchscreen.multitouch.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.xml:/system/vendor/etc/permissions/android.hardware.touchscreen.xml \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:/system/vendor/etc/permissions/android.hardware.usb.accessory.xml \
    frameworks/native/data/etc/android.hardware.usb.host.xml:/system/vendor/etc/permissions/android.hardware.usb.host.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:/system/vendor/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.wifi.xml:/system/vendor/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.software.sip.voip.xml:/system/vendor/etc/permissions/android.software.sip.voip.xml \
    frameworks/native/data/etc/handheld_core_hardware.xml:/system/vendor/etc/permissions/handheld_core_hardware.xml

# RIL
PRODUCT_COPY_FILES += $(LOCAL_PATH)/configs/spn-conf.xml:system/etc/spn-conf.xml

# Camera
PRODUCT_COPY_FILES += $(LOCAL_PATH)/configs/mediaserver.rc:system/etc/init/mediaserver.rc

# Wi-Fi
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/wifi/p2p_supplicant_overlay.conf:/system/vendor/etc/wifi/p2p_supplicant_overlay.conf \
    $(LOCAL_PATH)/configs/wifi/wpa_supplicant.conf:/system/vendor/etc/wifi/wpa_supplicant.conf \
    $(LOCAL_PATH)/configs/wifi/wpa_supplicant_overlay.conf:/system/vendor/etc/wifi/wpa_supplicant_overlay.conf

# Hostapd
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/hostapd/hostapd.accept:system/vendor/etc/hostapd/hostapd.accept \
    $(LOCAL_PATH)/configs/hostapd/hostapd.deny:system/vendor/etc/hostapd/hostapd.deny \
    $(LOCAL_PATH)/configs/hostapd/hostapd_default.conf:system/vendor/etc/hostapd/hostapd_default.conf

# Audio
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/audio/audio_policy_configuration.xml:/system/vendor/etc/audio/audio_policy_configuration.xml \
    $(LOCAL_PATH)/configs/audio/a2dp_audio_policy_configuration.xml:/system/vendor/etc/audio/a2dp_audio_policy_configuration.xml \
    frameworks/av/services/audiopolicy/config/audio_policy_volumes.xml:/system/vendor/etc/audio/audio_policy_volumes.xml \
    frameworks/av/services/audiopolicy/config/default_volume_tables.xml:/system/vendor/etc/audio/default_volume_tables.xml \
    frameworks/av/services/audiopolicy/config/r_submix_audio_policy_configuration.xml:/system/audio/vendor/etc/r_submix_audio_policy_configuration.xml \
    frameworks/av/services/audiopolicy/config/usb_audio_policy_configuration.xml:/system/vendor/etc/audio/usb_audio_policy_configuration.xml

# Codecs
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/codecs/media_codecs_ffmpeg.xml:/system/vendor/etc/media_codecs_ffmpeg.xml \
    $(LOCAL_PATH)/configs/codecs/media_codecs_mediatek_audio.xml:/system/vendor/etc/media_codecs_mediatek_audio.xml \
    $(LOCAL_PATH)/configs/codecs/media_codecs_mediatek_video.xml:/system/vendor/etc/media_codecs_mediatek_video.xml \
    $(LOCAL_PATH)/configs/codecs/media_codecs_performance.xml:/system/vendor/etc/media_codecs_performance.xml \
    $(LOCAL_PATH)/configs/codecs/media_codecs.xml:/system/vendor/etc/media_codecs.xml \
    $(LOCAL_PATH)/configs/codecs/media_profiles.xml:/system/vendor/etc/media_profiles.xml \
    $(LOCAL_PATH)/configs/codecs/media_profiles.xml:/system/vendor/etc/media_profiles_V1_0.xml \
    $(LOCAL_PATH)/configs/codecs/mtk_omx_core.cfg:system/etc/mtk_omx_core.cfg \
    $(LOCAL_PATH)/configs/codecs/mtk_omx_core.cfg:system/vendor/etc/mtk_omx_core.cfg \
    frameworks/av/media/libstagefright/data/media_codecs_google_audio.xml:/system/vendor/etc/media_codecs_google_audio.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_telephony.xml:/system/vendor/etc/media_codecs_google_telephony.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_video_le.xml:/system/vendor/etc/media_codecs_google_video_le.xml


# Vulkan
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.vulkan.level-0.xml:system/etc/permissions/android.hardware.vulkan.level.xml \
    frameworks/native/data/etc/android.hardware.vulkan.version-1_0_3.xml:system/etc/permissions/android.hardware.vulkan.version.xml

    PRODUCT_DEFAULT_PROPERTY_OVERRIDES := \
     persist.sys.timezone=Europe/Athens

PRODUCT_PROPERTY_OVERRIDES += \
    persist.call_recording.enabled=true \
    persist.media.treble_omx=false \
    persist.call_recording.src=4

 # Seccomp policy
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/seccomp_policy/mediacodec.policy:system/vendor/etc/seccomp_policy/mediacodec.policy \
	$(LOCAL_PATH)/seccomp_policy/mediaextractor.policy:system/vendor/etc/seccomp_policy/mediaextractor.policy

#Audio HIDL
PRODUCT_PACKAGES += \
    android.hardware.audio@2.0-impl \
    android.hardware.audio@2.0-service \
    android.hardware.audio.effect@2.0-impl \
    android.hardware.broadcastradio@1.1-impl
#    android.hardware.soundtrigger@2.0-impl

# Camera HAL
#PRODUCT_PACKAGES += \
#    camera.device@1.0-impl \
#    camera.device@3.2-impl \
#    android.hardware.camera.provider@2.4-impl \
#    android.hardware.camera.provider@2.4-service

# ConfigStore HAL
PRODUCT_PACKAGES += \
    android.hardware.configstore@1.0-impl \
    android.hardware.configstore@1.0-service

# Drm
PRODUCT_PACKAGES += \
    android.hardware.drm@1.0-impl

#gralloc/graphics HAL/HW Composer
PRODUCT_PACKAGES += \
    android.hardware.graphics.allocator@2.0-impl \
    android.hardware.graphics.allocator@2.0-service \
    android.hardware.graphics.composer@2.1-impl \
    android.hardware.graphics.mapper@2.0-impl \
    android.hardware.renderscript@1.0-impl

# Health
PRODUCT_PACKAGES += \
    android.hardware.health@1.0-impl \
    android.hardware.health@1.0-service

# IR HAL
PRODUCT_PACKAGES += \
    android.hardware.ir@1.0

# Keymaster HIDL
PRODUCT_PACKAGES += \
    android.hardware.keymaster@3.0-impl
#    android.hardware.keymaster@3.0-service

# Lights
PRODUCT_PACKAGES += \
    android.hardware.light@2.0-impl \
    android.hardware.light@2.0-service

# OMX
PRODUCT_PACKAGES += \
    android.hardware.media.omx@1.0

#memtrack
PRODUCT_PACKAGES += \
    android.hardware.memtrack@1.0-impl \
    android.hardware.memtrack@1.0-service

# Bluetooth HAL
PRODUCT_PACKAGES += \
    android.hardware.bluetooth@1.0-impl \
    android.hardware.bluetooth@1.0-service

# Power
PRODUCT_PACKAGES += \
    android.hardware.power@1.1-impl

# Ril
PRODUCT_PACKAGES += \
    android.hardware.radio@1.1 \
    android.hardware.radio.deprecated@1.0 \
    android.hardware.broadcastradio@1.0

# Sensors
PRODUCT_PACKAGES += \
    android.hardware.sensors@1.0-impl \
    android.hardware.sensors@1.0-service

#USB HAL
PRODUCT_PACKAGES += \
    android.hardware.usb@1.0-service

# Gatekeeper HIDL
PRODUCT_PACKAGES += \
    android.hardware.gatekeeper@1.0-impl
#    android.hardware.gatekeeper@1.0-service

# Vibrator
PRODUCT_PACKAGES += \
    android.hardware.vibrator@1.1-impl \
    android.hardware.vibrator@1.1-service

# Wifi
PRODUCT_PACKAGES += \
    android.hardware.wifi@1.0-service \
    android.hardware.wifi.offload@1.0 \
    android.hardware.wifi.supplicant@1.0

# Sensors
PRODUCT_PACKAGES += \
    android.hardware.sensors@1.0-impl \
    android.hardware.sensors@1.0-service

#GNSS HAL
#PRODUCT_PACKAGES += \
#    android.hardware.gnss@1.0-impl

# GPS force mode
#PRODUCT_PROPERTY_OVERRIDES += \
#    persist.force.gps.mode=gnss

#PRODUCT_PACKAGES += \
#    android.hardware.tetheroffload.config@1.0 \
#    android.hardware.tetheroffload.control@1.0
