#!/bin/sh
rootdirectory="$PWD"
dirs="bionic frameworks/av frameworks/base frameworks/native hardware/libhardware system/core system/netd packages/apps/FMRadio"

for dir in $dirs ; do
	cd $rootdirectory
	cd $dir
	echo "Cleaning $dir patches..."
	git checkout -- . && git clean -df
done

echo "Done!"
cd $rootdirectory
