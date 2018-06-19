#!/bin/sh
rootdirectory="$PWD"
dirs="external/sepolicy frameworks/av system/core system/netd"

for dir in $dirs ; do
	cd $rootdirectory
	cd $dir
	echo "Cleaning $dir patches..."
	git checkout -- . && git clean -df
done

echo "Done!"
cd $rootdirectory
