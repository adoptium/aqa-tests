#!/bin/bash 

setup() {
	VERSION1=$(echo "$REPO1" | sed 's/[^0-9]*//g')
	VERSION2=$(echo "$REPO2" | sed 's/[^0-9]*//g')
	
	if [[ "$VERSION1" == "$VERSION2" ]] ; then 
		echo "Please provide two different repositories to compare"
		exit 1; 
	fi

	if [[ "$VERSION1" > "$VERSION2" ]]; then
		temp="$VERSION1"
		VERSION1="$VERSION2"
		VERSION2="$temp"
		temp="$REPO1"
		REPO1="$REPO2"
		REPO2="$temp"
	fi

	WORKDIR=$(pwd)/workspace
	[ ! -d "$WORKDIR" ] && mkdir -p "$WORKDIR"
	if [ ! -d "$WORKDIR/logs" ] ; then 
		 mkdir -p "$WORKDIR/logs"
	else 
		rmdir "$WORKDIR/logs"
		mkdir -p "$WORKDIR/logs"
	fi
}

clone()	
{
	cd "$WORKDIR" || exit 1
	version=$1
	repo=$2
	if [ -d "$WORKDIR/$version" ] ; then 
		echo "Using existing test repo at: $WORKDIR/$version"
	else 
		echo "Git cloning test materials from $repo..."
		git clone --depth 1 -q "$repo" "$version"
		if [[ $? != 0 ]]; then 
			exit 1; 
		fi
	fi 
}

getVersionValue()
{	
	VERSION_VALUE1=$(find "${WORKDIR}/${VERSION1}" -maxdepth 1 -mindepth 1 -type d -name "JCK-runtime*" | awk -F'-' '{print $NF}') 
	VERSION_VALUE2=$(find "${WORKDIR}/${VERSION2}" -maxdepth 1 -mindepth 1 -type d -name "JCK-runtime*" | awk -F'-' '{print $NF}') 	
}

compareGroups()
{
	dirName=$1
	jdkVersion1TestDir="${WORKDIR}/${VERSION1}/JCK-${dirName}-${VERSION_VALUE1}/tests"
	jdkVersion2TestDir="${WORKDIR}/${VERSION2}/JCK-${dirName}-${VERSION_VALUE2}/tests"

	rc=0
	find "${jdkVersion1TestDir}" -maxdepth 2 -mindepth 2 -type d | sed "s|${jdkVersion1TestDir}||" | sort > "$WORKDIR/logs/$dirName-groups-${VERSION_VALUE1}.txt"
	find "${jdkVersion2TestDir}" -maxdepth 2 -mindepth 2 -type d | sed "s|${jdkVersion2TestDir}||" | sort > "$WORKDIR/logs/$dirName-groups-${VERSION_VALUE2}.txt"

	if [[ "${dirName}" == "runtime" ]]; then
		find "${jdkVersion1TestDir}/vm/verifier" -maxdepth 1 -mindepth 1 -type d | sed "s|${jdkVersion1TestDir}||" | sort >> "$WORKDIR/logs/$dirName-groups-${VERSION_VALUE1}.txt"
		find "${jdkVersion2TestDir}/vm/verifier" -maxdepth 1 -mindepth 1 -type d | sed "s|${jdkVersion2TestDir}||" | sort >> "$WORKDIR/logs/$dirName-groups-${VERSION_VALUE2}.txt"
	fi

	# Compare if playlist test groups are different
	diff "$WORKDIR/logs/$dirName-groups-$VERSION_VALUE1.txt" "$WORKDIR/logs/$dirName-groups-$VERSION_VALUE2.txt" > "$WORKDIR/logs/$dirName-groups-diff.txt" || rc=$?
	if [ $rc -eq 0 ]; then
		echo "SAME GROUPS : Groups under '$dirName' are identical in given versions: $VERSION1 & $VERSION2"
	else
		echo "DIFFERENT GROUPS: Groups under '$dirName' are different in two given versions: $VERSION1 & $VERSION2"
		echo "Please manually investigate the following differences in the two given repositories:"
		cat "$WORKDIR/logs/$dirName-groups-diff.txt"
	fi
	return $rc
}

compareNewTestsInSubGroups() {
	subGroups=("vm.verifier.instructions" "lang.CLSS" "lang.EXPR" "lang.CONV" "lang.INTF" "lang.LMBD" "lang.DASG" "lang.NAME" "lang.TYPE" "lang.ANNOT" "lang.STMT")
	rm -f "$WORKDIR/logs/NewTestsInSubGroups.txt"
	: > "$WORKDIR/logs/NewTestsInSubGroups.txt"
	for group in "${subGroups[@]}"; do
		grep "${group}" "${WORKDIR}/${VERSION2}/JCK-compiler-${VERSION_VALUE2}/NewTests.txt" | sort | uniq >> "$WORKDIR/logs/NewTestsInSubGroups.txt"
	done
	rc=0
	if [ ! -s "$WORKDIR/logs/NewTestsInSubGroups.txt" ]; then
		echo " No Subdir Group changes."
	else
		rm -f "$WORKDIR/logs/DiffsInSubGroups.txt"
		: > "$WORKDIR/logs/DiffsInSubGroups.txt"
		while IFS= read -r line; do
			subdir=$(echo "$line" | tr '.' '/' | sed 's/[[:blank:]]//g')
			component="compiler"
			if [[ "$subdir" == *"verifier"* ]]; then
				component="runtime"
			fi
			diff "${WORKDIR}/${VERSION1}/JCK-${component}-${VERSION_VALUE1}/tests/${subdir}" "${WORKDIR}/${VERSION2}/JCK-${component}-${VERSION_VALUE2}/tests/${subdir}" >> "$WORKDIR/logs/DiffsInSubGroups.txt"
		done < "$WORKDIR/logs/NewTestsInSubGroups.txt"
		sed -n '/Only in /p' "$WORKDIR/logs/DiffsInSubGroups.txt" | sort > "$WORKDIR/logs/tmp.txt" && mv "$WORKDIR/logs/tmp.txt" "$WORKDIR/logs/DiffsInSubGroups.txt"
		echo "NOTE: ACTION TO DO - Delete tests Only in ${WORKDIR}/${VERSION1}/ from jck/subdirs/jck${VERSION_VALUE2}.mk"
		echo "NOTE: ACTION TO DO - Add tests Only in ${WORKDIR}/${VERSION2}/ to jck/subdirs/jck${VERSION_VALUE2}.mk" 
		cat "$WORKDIR/logs/DiffsInSubGroups.txt"
		rc=1
	fi
	return $rc
}

jobTime() {
	end_time="$(date -u +%s)"
	date 
	elapsed="$((end_time-begin_time))"
	echo "Total analysis took ~ $elapsed seconds."
}

date
echo "Starting test materials scan.."

begin_time="$(date -u +%s)"
REPO1=$1
REPO2=$2
diffFound=0


setup
clone "$VERSION1" "$REPO1"
clone "$VERSION2" "$REPO2"
getVersionValue

echo "Compare per playlist TestCases =================================================="
echo ""
for comp in "runtime" "compiler"; do
	compareGroups "${comp}"
	diffFound=$(( diffFound + $? ))
done
echo "Compare per playlist TestCases Done =============================================="
echo ""
echo "Compare New Tests with Sub Groups in jckversion.mk ==============================="
# Check if new tests are part of subdir make targets to ensure tests groups are updated
compareNewTestsInSubGroups
diffFound=$(( diffFound + $? ))
echo "Compare New Tests with Sub Groups in jckversion.mk Done ==============================="
echo ""
jobTime

if [[ $diffFound != 0 ]]; then
	exit 1
fi
