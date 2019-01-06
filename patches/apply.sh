cd bionic
git apply -v ../device/xiaomi/hermes/patches/bionic/*
cd ..
cd frameworks/av/
git remote add vk https://github.com/vishalk95/android_frameworks_av_mtk
git fetch vk
git cherry-pick d02c03000dcafa0d486c0cdddd13d2d827f99c9e 54b3086e29b56417614377b07d5421010bc42c2a
cd ../..
cd frameworks/opt/tele*
git remote add vk https://github.com/vishalk95/android_frameworks_opt_telephony_mtk
git fetch vk
git cherry-pick e3579299241783841c7dad2f6e5613e2d7f1302e
cd ../../../
cd system/core
git remote add vk https://github.com/vishalk95/android_system_core_mtk
git fetch vk
git cherry-pick 1e7718e6533b5fe99ff9752b01c355a05610329a^..d8da7015155ab563464883c6d2d52e82510cb786
cd ../..
cd hardware/interfaces
git remote add vk https://github.com/vishalk95/android_hardware_interfaces_mtk
git fetch vk
git cherry-pick e99f84841a1715159a7d7c4ce492fab54849ae6d 29c200b6076901fd5181ec29d574515d77d2751e
cd ../..
cd hardware/libhardware
git remote add vk https://github.com/vishalk95/android_hardware_libhardware_mtk
git fetch vk
git cherry-pick 6d05ca0682f4ffe461e30c1d7bf388c55a13da77
cd ../..