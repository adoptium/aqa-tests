#!/bin/bash 

setup()
{
	cd $WORKDIR
	rm -rf $WORKDIR/*.lst
	version=$1
	repo=$2
	
	if [ -d "$WORKDIR/$version" ] ; then 
		echo "Using existing test repo at: $WORKDIR/$version"
	else 
		echo "Git cloning test materials from $repo..."
		git clone --depth 1 -q $repo $version
		if [[ $? != 0 ]]; then 
			echo "wrong repo, exit code = $rc" 
			exit $rc; 
		fi
	fi 
}

compare()
{
	rm -rf $WORKDIR/*.lst
	rm -rf $WORKDIR/*.log

	dirName=$1

	cd $WORKDIR/$VERSION1/JCK-$dirName-$VERSION_VALUE1/tests
	echo "Listing test directories under $dirName at: `pwd`" 
	find . -maxdepth 2 -mindepth 2 -type d > $WORKDIR/$dirName-$VERSION_VALUE1.lst
	
	cd $WORKDIR/$VERSION2/JCK-$dirName-$VERSION_VALUE2/tests
	echo "Listing test directories under $dirName at: `pwd`" 
	find . -maxdepth 2 -mindepth 2 -type d > $WORKDIR/$dirName-$VERSION_VALUE2.lst


	if cmp -s $WORKDIR/$dirName-$VERSION_VALUE1.lst $WORKDIR/$dirName-$VERSION_VALUE2.lst; then 
		echo "SAME : '$dirName' folder identical in both given versions: $VERSION1 & $VERSION2"
	else 
		diff $WORKDIR/$dirName-$VERSION_VALUE1.lst $WORKDIR/$dirName-$VERSION_VALUE2.lst > $WORKDIR/$dirName-diff.log 
		echo "DIFFERENT : '$dirName' folder content are different in two given versions: $VERSION1 & $VERSION2"
		echo "Please manually investigate the following differences in the two given repositories:"
		cat $WORKDIR/$dirName-diff.log
		exit 1
	fi 
}

date
echo "Starting test materials scan.."
begin_time="$(date -u +%s)"

REPO1=$1
REPO2=$2

VERSION1=$(echo "$REPO1" | sed 's/[^0-9]*//g')
VERSION2=$(echo "$REPO2" | sed 's/[^0-9]*//g')

if [[ "$VERSION1" -eq 8 ]] ; then 
	VERSION_VALUE1="$VERSION1"c
else 
	VERSION_VALUE1="$VERSION1"
fi 

if [[ "$VERSION2" -eq 8 ]] ; then 
	VERSION_VALUE2="$VERSION2"c
else 
	VERSION_VALUE2="$VERSION2"
fi 

WORKDIR=$(pwd)/workspace

echo "WORKDIR=$WORKDIR"
echo "REPO1=$REPO1" 
echo "REPO2=$REPO2"
	
[ ! -d "$WORKDIR" ] && mkdir -p "$WORKDIR"

setup $VERSION1 $REPO1
setup $VERSION2 $REPO2
compare runtime 
compare compiler

end_time="$(date -u +%s)"
echo "Done!"
date 
elapsed="$(($end_time-$begin_time))"
echo "Total analysis took ~ $elapsed seconds."
