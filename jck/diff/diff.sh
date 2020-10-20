#!/bin/bash 

setup()
{
	cd $WORKDIR
	version=$1
	repo=$2
	
	if [ -d "$WORKDIR/JCK-$version" ] ; then 
		echo "Using existing JCK $version repo at: $WORKDIR/JCK-$version"
	else 
		echo "Git cloning JCK materials from $repo..."
		git clone $repo JCK-$version
		if [[ $? != 0 ]]; then 
			exit $rc; 
		fi
	fi 
}

compare()
{
	rm -rf $WORKDIR/*.lst
	rm -rf $WORKDIR/*.log

	dirName=$1

	cd $WORKDIR/JCK-$VERSION1/JCK-$dirName-$VERSION_VALUE1/tests
	echo "Listing test directories under $dirName at: `pwd`" 
	find . -maxdepth 2 -mindepth 2 -type d > $WORKDIR/$dirName-$VERSION_VALUE1.lst
	
	cd $WORKDIR/JCK-$VERSION2/JCK-$dirName-$VERSION_VALUE2/tests
	echo "Listing test directories under $dirName at: `pwd`" 
	find . -maxdepth 2 -mindepth 2 -type d > $WORKDIR/$dirName-$VERSION_VALUE2.lst


	if cmp -s $WORKDIR/$dirName-$VERSION_VALUE1.lst $WORKDIR/$dirName-$VERSION_VALUE2.lst; then 
		echo "SAME : JCK$VERSION1 $dirName, JCK$VERSION2 $dirName"
	else 
		diff $WORKDIR/$dirName-$VERSION_VALUE1.lst $WORKDIR/$dirName-$VERSION_VALUE2.lst > $WORKDIR/$dirName-diff.log 
		echo "DIFFERENT : JCK$VERSION1 $dirName and JCK$VERSION2 $dirName"
		echo "Please manually investigate the following differences in the two given repositories:"
		cat $WORKDIR/$dirName-diff.log
	fi 

	rm -rf $WORKDIR/*.lst
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
