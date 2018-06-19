#!/bin/sh
rootdirectory="$PWD"
dirs="external/sepolicy frameworks/av system/core system/netd"

for dir in $dirs ; do
	cd $rootdirectory
	cd $dir
    echo "Applying $dir patches..."
	git apply $rootdirectory/device/xiaomi/hermes/patches/$dir/*.patch
done

echo "Done!"
cd $rootdirectory
