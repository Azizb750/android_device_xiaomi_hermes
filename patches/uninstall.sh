!/bin/sh
rootdirectory="$PWD"
dirs="bionic frameworks/av frameworks/native system/core system/sepolicy"

for dir in $dirs ; do
	cd $rootdirectory
	cd $dir
	echo "Cleaning $dir patches..."
	git checkout -- . && git clean -df
done

echo "Done!"
cd $rootdirectory
