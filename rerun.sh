#!/bin/bash

if [[ ! $# -gt 0 ]] || [[ "$1" != "--link"  ]] || [[ "$3" != "--file" ]]
then
	echo "Usage: "
	echo "rerun.sh --link rerunLink --file SHA_file"
	exit 1
fi

link=$2
sha=$4
declare -A values
state=false
# This loop reads the file line by line and stores the content in an array.
# Structure of array = url,sha=repo dir

while read line;
do
	if [[ $line == *"repo dir:"* ]]
	then
		read -ra value <<< "$line"
		dir="${value[2]}"
		value=""
	fi

	if [[ $line == *"Fetch URL:"* ]]
	then
		read -ra value <<< "$line"
		url="${value[2]}"
		value=""
	fi
		
	if [[ $state = true ]]
	then
		sha=$line
		state=false
		values["$url, $sha"]=$dir
	fi
			
	if [[ $line == *"sha"* ]]
	then
		state=true
	fi
			
done < "$sha"
: 'for i in "${!values[@]}"
do
	echo "$i - ${values[$i]}"

done
exit 1'
# This function parses the given link to obtain values such as BUILD_LIST, TARGET.
linkParse(){
	IFS='&'
	read -ra splitted<<<$link
	IFS='='
	for i in "${splitted[@]}"
	do
		if [[ $i == *"TARGET"* ]]
		then
			read -ra value<<<$i
			TARGET="${value[1]}"
		elif [[ $i == *"BUILD_LIST"* ]]
		then			
			read -ra value<<<$i
			BUILD_LIST="${value[1]}"
		elif [[ $i == *"JDK_VERSION"* ]]
		then			
			read -ra value<<<$i
			JDK_VERSION="${value[1]}"
		elif [[ $i == *"JDK_IMPL"* ]]
		then
			read -ra value<<<$i
			JDK_IMPL="${value[1]}"		
		fi
	done
}
linkParse 
for i in "${!values[@]}"
do
	IFS=', '
	read -ra j<<<$i
	if [[ "${j[0]}" == *"openjdk-tests.git"* ]]
	then
		echo "Fetching openjdk-tests..."
		git clone "${j[0]}"
		cd openjdk-tests
		git checkout "${j[1]}"
		break
	fi
done
cd ..
for i in "${!values[@]}"
do
# Here it splits the keys of array then controls the contents.
	IFS=', '
	read -ra j<<<$i
	if [[ "${j[0]}" == *"openjdk-tests.git"* ]]
	then
		continue
	elif [[ "${j[0]}" == *"openj9.git"* ]]
	then	
		echo "Fetching functional material..."
		echo "git clone --depth 1 ${j[0]}"
		git clone --depth 1 "${j[0]}" && cd "$(basename "${j[0]}" .git)"
		git checkout "${j[1]}"
		mv test/functional/* ../openjdk-tests/functional
		mv test/TestConfig ../openjdk-tests/TestConfig
		mv test/Utils ../openjdk-tests/Utils
		cd ..
		rm -rf openj9
	elif [[ "${j[0]}" == *"TKG.git"* ]] 
	then
		echo "Fetching TKG..."
		cd openjdk-tests	
		echo "git clone ${j[0]} "
		git clone "${j[0]}" 
		cd "$(basename "${j[0]}" .git)" 
		git checkout "${j[1]}"
		cd ..
		cd ..
	elif [[ "${values[$i]}" == *"openjdk-jdk"* && $BUILD_LIST == *"openjdk"* ]]
	then
		echo "Setting openjdk variables..."
		export JDK_REPO=${j[0]}
		export OPENJDK_SHA=${j[1]}
	fi		
done

cd openjdk-tests/TKG
export BUILD_LIST="${BUILD_LIST}"
export TARGET="${TARGET}"
export JDK_VERSION="${JDK_VERSION}"
export JDK_IMPL="${JDK_IMPL}"
export TEST_JDK_HOME=${TEST_JDK_HOME}

make compile 
make "_$TARGET"

