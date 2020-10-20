#!/bin/bash 

setup()
{
	cd $WORKDIR
	jckVersion=$1
	jckRepo=$2
	
	if [ -d "$WORKDIR/JCK-$jckVersion" ] ; then 
		echo "Using existing JCK $jckVersion repo at: $WORKDIR/JCK-$jckVersion"
	else 
		echo "Git cloning JCK materials from $jckRepo..."
		git clone $jckRepo JCK-$jckVersion
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

	cd $WORKDIR/JCK-$JCK_VERSION1/JCK-$dirName-$VERSION_VALUE1/tests
	echo "Listing test directories under $dirName at: `pwd`" 
	find . -maxdepth 2 -mindepth 2 -type d > $WORKDIR/$dirName-$VERSION_VALUE1.lst
	
	cd $WORKDIR/JCK-$JCK_VERSION2/JCK-$dirName-$VERSION_VALUE2/tests
	echo "Listing test directories under $dirName at: `pwd`" 
	find . -maxdepth 2 -mindepth 2 -type d > $WORKDIR/$dirName-$VERSION_VALUE2.lst


	if cmp -s $WORKDIR/$dirName-$VERSION_VALUE1.lst $WORKDIR/$dirName-$VERSION_VALUE2.lst; then 
		echo "SAME : JCK$JCK_VERSION1 $dirName, JCK$JCK_VERSION2 $dirName"
	else 
		diff $WORKDIR/$dirName-$VERSION_VALUE1.lst $WORKDIR/$dirName-$VERSION_VALUE2.lst > $WORKDIR/$dirName-diff.log 
		echo "DIFFERENT : JCK$JCK_VERSION1 $dirName and JCK$JCK_VERSION2 $dirName"
		echo "Please manually investigate the following differences in the two given repositories:"
		cat $WORKDIR/$dirName-diff.log
	fi 

	rm -rf $WORKDIR/*.lst
}

date
echo "Starting auto jck materials scan.."
begin_time="$(date -u +%s)"

JCK_REPO1=$1
JCK_REPO2=$2

JCK_VERSION1=$(echo "$JCK_REPO1" | sed 's/[^0-9]*//g')
JCK_VERSION2=$(echo "$JCK_REPO2" | sed 's/[^0-9]*//g')

if [[ "$JCK_VERSION1" -eq 8 ]] ; then 
	VERSION_VALUE1="$JCK_VERSION1"c
else 
	VERSION_VALUE1="$JCK_VERSION1"
fi 

if [[ "$JCK_VERSION2" -eq 8 ]] ; then 
	VERSION_VALUE2="$JCK_VERSION2"c
else 
	VERSION_VALUE2="$JCK_VERSION2"
fi 

WORKDIR=$(pwd)/workspace

echo "WORKDIR=$WORKDIR"
echo "JCK_REPO1=$JCK_REPO1" 
echo "JCK_REPO2=$JCK_REPO2"
	
[ ! -d "$WORKDIR" ] && mkdir -p "$WORKDIR"

setup $JCK_VERSION1 $JCK_REPO1
setup $JCK_VERSION2 $JCK_REPO2
compare runtime 
compare compiler

end_time="$(date -u +%s)"
echo "Done!"
date 
elapsed="$(($end_time-$begin_time))"
echo "Total analysis took ~ $elapsed seconds."
