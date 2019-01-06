DEVICE_PATH := device/xiaomi/hermes

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/aosp_base_telephony.mk)

# Inherit some common CM stuff.
$(call inherit-product, vendor/du/config/common_full_phone.mk)

# Inherit from hermes device
$(call inherit-product, $(DEVICE_PATH)/device.mk)

# Release name
PRODUCT_RELEASE_NAME := hermes

# Device display
TARGET_SCREEN_HEIGHT := 1920
TARGET_SCREEN_WIDTH := 1080

PRODUCT_DEVICE := hermes
PRODUCT_NAME := du_hermes
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := Redmi Note 2
PRODUCT_MANUFACTURER := Xiaomi

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi
#BUILD_FINGERPRINT := "Xiaomi/hermes/hermes:5.0.2/LRX22G/V9.5.4.0.LHMMIFA:user/release-keys"
#PRODUCT_BUILD_PROP_OVERRIDES += \
#    PRIVATE_BUILD_DESC="hermes-user 5.0.2 LRX22G V9.5.4.0.LHMMIFA release-keys"
