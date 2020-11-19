#!/bin/bash 

setup() {
	cd $workspace
	case "$(uname -s)" in
		Linux)
     			PLATFORM=x86-64_linux
     			;;
		Darwin)
     			PLATFORM=x86-64_mac
     			;;
		
		CYGWIN*|MINGW32*|MSYS*|MINGW*)
     			PLATFORM=x86-64_windows
     			;;
		*)
     			echo "Can't detect platform automatically. Please manually set the platform value in the call for get.sh"
     			;;
	esac

	# Clone openjdk-tests materials if they don't already exist
	if [ -d "$workspace/openjdk-tests" ] ; then
		echo "Using existing test repo at: $workspace/openjdk-tests"
	else 
		git clone git@github.com:AdoptOpenJDK/openjdk-tests
	fi 
	
	# Run get.sh to get TKG and SDK to use if it does not exist already 
	if [ -d "$workspace/openjdkbinary/j2sdk-image" ] ; then
		echo "Using existing SDK from $workspace/openjdkbinary/j2sdk-image"
	else
		$workspace/openjdk-tests/get.sh -s $workspace -t $workspace/openjdk-tests -p $PLATFORM -r nightly -j $VERSION -i openj9 --openj9_repo https://github.com/eclipse/openj9.git --openj9_branch master --tkg_repo https://github.com/AdoptOpenJDK/TKG.git --tkg_branch master --test_images_required false --debug_images_required false
	fi 
	
	if [ $PLATFORM == x86-64_mac ]; then 
		export TEST_JDK_HOME=$workspace/openjdkbinary/j2sdk-image/Contents/Home/
	else
		export TEST_JDK_HOME=$workspace/openjdkbinary/j2sdk-image/
	fi 
	chmod 777 $TEST_JDK_HOME/*
		
	# Download the test materials if they don't already exist in Workspace 
	cd $workspace
	if [ -d "$workspace/test/config" ] ; then
		echo "Using existing test repo at: $workspace/test"
	else 
		echo "Git cloning test materials from $repo..."
		git clone --depth 1 -q $repo $workspace/test
		if [ $? != 0 ]; then 
			exit 1
		fi
	fi 
	export JCK_ROOT=$workspace/test
	export TEST_ROOT=$workspace/openjdk-tests
}

genTargetList() {

	inputFile=$TEST_ROOT/TKG/parallelList.mk
	outputFile=$listOfExistingTargets
	rm -rf $outputFile
	
	# If the TKG generated target list already exists don't generate it again
	if [ ! -f "$inputFile" ] ; then
		if [ ! -d "$TEST_ROOT/TKG" ] ; then
			echo "Can't find TKG under $TEST_ROOT/TKG"
			echo "Please ensure setup was done properly"
			exit 1
		else 
			echo "Generating list of existing playlist targets.." 
			cd $TEST_ROOT/TKG
			make genParallelList NUM_MACHINES=1 TEST=jck
		fi 
	else 
		echo "TKG generated target list already exists already. Using the existing one: $inputFile"
	fi 
	
	echo "Cleaning up generated target list.."
	while IFS= read -r line
	do
		if [[ $line == *"TESTLIST="* ]]; then
  			for i in $(echo $line | tr "," "\n")
			do
				if [[ $i == *"_testList"* ]]; then
					continue
				elif [[ $i == *"(MAKE)"* ]]; then
					continue
				elif [[ $i == *"="* ]]; then 
					IFS='=' tokens=( $i )
					echo ${tokens[1]} >> $outputFile
				else 
  					echo $i >> $outputFile
  				fi
			done
		fi
	done <"$inputFile"
	echo "Done!"
}

genTestFolderList() {
	if [ "$VERSION" -eq 8 ] ; then 
		ver="$VERSION"c
	else 
		ver="$VERSION"
	fi 
	# Generate the list of folders - two step deep, so that they can be used for scanning later 
	cd $JCK_ROOT/*-runtime-$ver/tests
	find . -maxdepth 2 -mindepth 2 -type d > $outputdir/runtime-dirs.txt
	
	cd $JCK_ROOT/*-compiler-$ver/tests
	find . -maxdepth 2 -mindepth 2 -type d > $outputdir/compiler-dirs.txt

	if [ $ver == "8c" ]; then 
		cd $JCK_ROOT/*-devtools-$ver/tests
		find . -maxdepth 2 -mindepth 2 -type d > $outputdir/devtools-dirs.txt
	fi
}

crossCheckTestFoldersIn() {
	testType=$1
	testTypeToUpper=`echo "$testType" | tr a-z A-Z`
	dirList=$outputdir/$testType-dirs.txt
	count=0

	if [ ! -f "$dirList" ]; then
		return 
	fi

	echo "--------------------------------------------------------"
	echo "     Cross-checking test folders under: $testType       "
	echo "--------------------------------------------------------"

	# Crosscheck 1
	# Read the file containing test folder names generated in genTestFolderList()
	# For each folder name, look for the corresponding target in the target list generated in genTargetList()
	while IFS= read -r line
	do
		IFS='/' tokens=( $line )
		tokenToSearch="$testType-${tokens[1]}-${tokens[2]}"
		existingTargets=$( cat "${listOfExistingTargets}" )
		if [[ ! " $existingTargets " =~ $tokenToSearch ]] ; then
			echo "New test folder detected: $line | Please add target: jck-$tokenToSearch"
			command='<command>$(JCK_CMD_TEMPLATE) -test-args=$(Q)tests='${tokens[1]}/${tokens[2]}',jckRoot=$(JCK_ROOT),jckversion=$(JCK_VERSION),testsuite='$testTypeToUpper'$(Q); \'
			status='$(TEST_STATUS)</command>'
			echo "<test>"
			echo "	<testCaseName>jck-$tokenToSearch</testCaseName>"
			echo "	<variations>"
			echo "		<variation>NoOptions</variation>"
			echo "	</variations>"
			echo "	$command"
			echo "	$status"
			echo "	<levels>"
			echo "		<level>extended</level>"
			echo "	</levels>"
			echo "	<groups>"
			echo "		<group>jck</group>"
			echo "	</groups>"
			echo "	<subsets>"
			echo "		<subset>$VERSION+</subset>"
			echo "	</subsets>"
			echo "</test>"
			count=$((count +1))	
		fi
	done <"$dirList"
	echo "Total new folders detected : $count"
}

usage() {
	echo 'If run locally, please Use : jckchk.sh {jck-repo-name}' 
	echo 'If run from build, please Use : jckchk.sh {jck-version} {jck-root}' 
}

date
begin_time="$(date -u +%s)"

export workspace=`pwd`/crosscheck
export outputdir=$workspace/output
export listOfExistingTargets=$outputdir/targetList.txt

mkdir $workspace
mkdir $workspace/output

if [ "$#" -eq 3 ]; then
	# The script is running from a build which sends in two parameters version and root. 
	# No setup() is needed in this case.   
	export VERSION=$(echo "$1" | sed 's/[^0-9]*//g')
	export JCK_ROOT=$2
	export TEST_ROOT=$3
elif [ "$#" -eq 1 ]; then
	# We are running locally, where only one parameter is sent in: test repo. 
	# Also, We will need setup() in this case. 
	export repo=$1
	export VERSION=$(echo "$repo" | sed 's/[^0-9]*//g')
	setup
else
	echo "Invalid input" 
	usage
	exit 1
fi

genTargetList
genTestFolderList

crossCheckTestFoldersIn compiler
crossCheckTestFoldersIn runtime
crossCheckTestFoldersIn devtools

end_time="$(date -u +%s)"
date 
elapsed="$(($end_time-$begin_time))"
echo "Total process took ~ $elapsed seconds."