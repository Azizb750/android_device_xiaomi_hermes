#!/bin/sh
rootdirectory="$PWD"
dirs="frameworks/av frameworks/opt/telephony system/core hardware/interfaces hardware/libhardware"

for dir in $dirs ; do
	cd $rootdirectory
	cd $dir
	echo "Cleaning $dir patches..."
	git checkout -- . && git clean -df
done

echo "Done!"
cd $rootdirectory
