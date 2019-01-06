#!/bin/sh
rootdirectory="$PWD"
dirs="frameworks/av frameworks/opt/telephony system/core hardware/interfaces hardware/libhardware"

for dir in $dirs ; do
	cd $rootdirectory
	cd $dir
    echo "Applying $dir patches..."
	git apply $rootdirectory/device/xiaomi/hermes/patches/$dir/*.patch
done

echo "Done!"
cd $rootdirectory
