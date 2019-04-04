#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

SDKDIR=""
TESTDIR=""
PLATFORM=""
JVMVERSION=""
SDK_RESOURCE="nightly"
CUSTOMIZED_SDK_URL=""
OPENJ9_REPO="https://github.com/eclipse/openj9.git"
OPENJ9_SHA=""
OPENJ9_BRANCH=""
VENDOR_REPOS=""
VENDOR_SHAS=""
VENDOR_BRANCHES=""
VENDOR_DIRS=""
JDK_VERSION="8"
JDK_IMPL="openj9"
RELEASES="latest"
TYPE="jdk"


usage ()
{
	echo 'Usage : get.sh  --testdir|-t openjdktestdir'
	echo '                --platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix'
	echo '                [--jdk_version|-j ]: optional. JDK version'
	echo '                [--jdk_impl|-i ]: optional. JDK implementation'
	echo '                [--releases|-R ]: optional. Example: latest, jdk8u172-b00-201807161800'
	echo '                [--type|-T ]: optional. jdk or jre'
	echo '                [--sdkdir|-s binarySDKDIR] : if do not have a local sdk available, specify preferred directory'
	echo '                [--sdk_resource|-r ] : indicate where to get sdk - releases, nightly , upstream or customized'
	echo '                [--customizedURL|-c ] : indicate sdk url if sdk source is set as customized.  Multiple urls can be passed with space as separator'
	echo '                [--username ] : indicate username required if customized url requiring authorization is used'
	echo '                [--password ] : indicate password required if customized url requiring authorization is used'
	echo '                [--openj9_repo ] : optional. OpenJ9 git repo. Default value https://github.com/eclipse/openj9.git is used if not provided'
	echo '                [--openj9_sha ] : optional. OpenJ9 pull request sha.'
	echo '                [--openj9_branch ] : optional. OpenJ9 branch.'
	echo '                [--vendor_repos ] : optional. Comma separated Git repository URLs of the vendor repositories'
	echo '                [--vendor_shas ] : optional. Comma separated SHAs of the vendor repositories'
	echo '                [--vendor_branches ] : optional. Comma separated vendor branches'
	echo '                [--vendor_dirs ] : optional. Comma separated directories storing vendor test resources'
}

parseCommandLineArgs()
{
	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--sdkdir" | "-s" )
				SDKDIR="$1"; shift;;

			"--testdir" | "-t" )
				TESTDIR="$1"; shift;;

			"--platform" | "-p" )
				PLATFORM="$1"; shift;;

			"--jdk_version" | "-j" )
				JDK_VERSION="$1"; shift;;

			"--jdk_impl" | "-i" )
				JDK_IMPL="$1"; shift;;

			"--releases" | "-R" )
				RELEASES="$1"; shift;;

			"--type" | "-T" )
				TYPE="$1"; shift;;

			"--sdk_resource" | "-r" )
				SDK_RESOURCE="$1"; shift;;

			"--customizedURL" | "-c" )
				CUSTOMIZED_SDK_URL="$1"; shift;;

			"--username" )
				USERNAME="$1"; shift;;

			"--password" )
				PASSWORD="$1"; shift;;

			"--openj9_repo" )
				OPENJ9_REPO="$1"; shift;;

			"--openj9_sha" )
				OPENJ9_SHA="$1"; shift;;

			"--openj9_branch" )
				OPENJ9_BRANCH="$1"; shift;;

			"--vendor_repos" )
				VENDOR_REPOS="$1"; shift;;

			"--vendor_shas" )
				VENDOR_SHAS="$1"; shift;;

			"--vendor_branches" )
				VENDOR_BRANCHES="$1"; shift;;

			"--vendor_dirs" )
				VENDOR_DIRS="$1"; shift;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
}

getBinaryOpenjdk()
{
	echo "get jdk binary..."
	cd $SDKDIR
	mkdir -p openjdkbinary
	cd openjdkbinary

	if [ "$CUSTOMIZED_SDK_URL" != "" ]; then
		download_url=$CUSTOMIZED_SDK_URL
                # if these are passed through via withCredentials(CUSTOMIZED_SDK_URL_CREDENTIAL_ID) these will not be visible within job output,
                # if supplied when run manually with --username and --password these will be seen in plaintext within job output
		if [ "$USERNAME" != "" ] && [ "$PASSWORD" != "" ]; then
			curl_options="--user $USERNAME:$PASSWORD"
		fi
	elif [ "$SDK_RESOURCE" == "nightly" ] || [ "$SDK_RESOURCE" == "releases" ]; then
		os=${PLATFORM#*_}
		os=${os%_largeHeap}
		arch=${PLATFORM%%_*}
		OPENJDK_VERSION="openjdk${JDK_VERSION}"
		heap_size="normal"
		if [[ $PLATFORM = *"largeHeap"* ]]; then
			heap_size="large"
		fi
		download_url="https://api.adoptopenjdk.net/v2/binary/${SDK_RESOURCE}/${OPENJDK_VERSION}?openjdk_impl=${JDK_IMPL}&os=${os}&arch=${arch}&release=${RELEASES}&type=${TYPE}&heap_size=${heap_size}"
	else
		download_url=""
		echo "--sdkdir is set to $SDK_RESOURCE. Therefore, skip download jdk binary"
	fi

	if [ "${download_url}" != "" ]; then
		for file in $download_url
		do
			echo "curl -OLJks ${curl_options} $file"
			curl -OLJks ${curl_options} $file
			if [ $? -ne 0 ]; then
				echo "Failed to retrieve $file, exiting. This is what we received of the file and MD5 sum:"
				ls -ld $file
				md5sum $file
				exit 1
			fi
		done
	fi

	jar_files=`ls`
	jar_file_array=(${jar_files//\\n/ })
	for jar_name in "${jar_file_array[@]}"
		do
			echo "unzip file: $jar_name ..."
			if [[ $jar_name == *zip || $jar_name == *jar ]]; then
				unzip -q $jar_name -d .
			else
				gzip -cd $jar_name | tar xof -
			fi
		done

	jar_dirs=`ls -d */`
	jar_dir_array=(${jar_dirs//\\n/ })
	for jar_dir in "${jar_dir_array[@]}"
		do
			jar_dir_name=${jar_dir%?}
			if [[ "$jar_dir_name" =~ jre*  &&  "$jar_dir_name" != "j2jre-image" ]]; then
				if [[ -d $jar_dir_name/Contents/Home ]]; then
					mv "$jar_dir_name/Contents/Home" j2jre-image
				else
					mv $jar_dir_name j2jre-image
				fi
			elif [[ "$jar_dir_name" =~ jdk*  &&  "$jar_dir_name" != "j2sdk-image" ]]; then
				if [[ -d $jar_dir_name/Contents/Home ]]; then
					mv "$jar_dir_name/Contents/Home" j2sdk-image
				else
					mv $jar_dir_name j2sdk-image
				fi
			# if native test libs folder is available, mv it under native-test-libs
			elif [[ "$jar_dir_name"  =~ native-test-libs*  &&  "$jar_dir_name" != "native-test-libs" ]]; then
				mv $jar_dir_name native-test-libs
			#The following only needed if openj9 has a different image name convention
			elif [[ "$jar_dir_name" != "j2sdk-image"  &&  "$jar_dir_name" != "native-test-libs" ]]; then
				mv $jar_dir_name j2sdk-image
			fi
		done
	chmod -R 755 j2sdk-image
}

getTestKitGenAndFunctionalTestMaterial()
{
	echo "get testKitGen and functional test material..."
	cd $TESTDIR

	if [ "$OPENJ9_BRANCH" != "" ]
	then
		OPENJ9_BRANCH="-b $OPENJ9_BRANCH"
	fi

	echo "git clone $OPENJ9_BRANCH $OPENJ9_REPO"
	git clone -q $OPENJ9_BRANCH $OPENJ9_REPO

	if [ "$OPENJ9_SHA" != "" ]
	then
		echo "update to openj9 sha: $OPENJ9_SHA"
		cd openj9
		git fetch -q --tags $OPENJ9_REPO +refs/pull/*:refs/remotes/origin/pr/*
		git checkout -q $OPENJ9_SHA
		cd $TESTDIR
	fi

	mv openj9/test/TestConfig TestConfig
	mv openj9/test/Utils Utils
    if [ -d functional ]; then
        mv openj9/test/functional/* functional/
    else
	    mv openj9/test/functional functional
    fi
	rm -rf openj9

	if [ "$VENDOR_REPOS" != "" ]; then
		declare -a vendor_repos_array
		declare -a vendor_branches_array
		declare -a vendor_shas_array
		declare -a vendor_dirs_array

		# convert VENDOR_REPOS to array
		vendor_repos_array=(`echo $VENDOR_REPOS | sed 's/,/\n/g'`)

		if [ "$VENDOR_BRANCHES" != "" ]; then
			# convert VENDOR_BRANCHES to array
			vendor_branches_array=(`echo $VENDOR_BRANCHES | sed 's/,/\n/g'`)
		fi

		if [ "$VENDOR_SHAS" != "" ]; then
			#convert VENDOR_SHAS to array
			vendor_shas_array=(`echo $VENDOR_SHAS | sed 's/,/\n/g'`)
		fi

		if [ "$VENDOR_DIRS" != "" ]; then
			#convert VENDOR_DIRS to array
			vendor_dirs_array=(`echo $VENDOR_DIRS | sed 's/,/\n/g'`)
		fi

		for i in "${!vendor_repos_array[@]}"; do
			# clone vendor source
			repoURL=${vendor_repos_array[$i]}
			branch=${vendor_branches_array[$i]}
			sha=${vendor_shas_array[$i]}
			dir=${vendor_dirs_array[$i]}
			dest="vendor_${i}"

			branchOption=""
			if [ "$branch" != "" ]; then
				branchOption="-b $branch"
			fi

			echo "git clone ${branchOption} $repoURL $dest"
			git clone -q --depth 1 $branchOption $repoURL $dest

			if [ "$sha" != "" ]; then
				cd $dest
				echo "update to $sha"
				git checkout $sha
				cd $TESTDIR
			fi

			# move resources
			if [[ "$dir" != "" ]] && [[ -d $dest/$dir ]]; then
				echo "Stage $dest/$dir to $TESTDIR/$dir"
				# already in TESTDIR, thus copy $dir to current directory
				cp -r $dest/$dir ./
			else
				echo "Stage $dest to $TESTDIR"
				# already in TESTDIR, thus copy the entire vendor repo content to current directory
				cp -r $dest/* ./
			fi

			# clean up
			rm -rf $dest
		done
	fi
}

testJavaVersion()
{
# use environment variable JAVA_BIN to run java -version
_java=${JAVA_BIN}/java
if [ -x ${_java} ]; then
	echo "Run ${_java} -version"
	${_java} -version
else
	echo "Cannot find java executable in JAVA_BIN: ${JAVA_BIN}!"
	exit 1
fi
}

parseCommandLineArgs "$@"
if [[ "$SDKDIR" != "" ]]; then
	getBinaryOpenjdk
fi

testJavaVersion

if [ ! -d "$TESTDIR/TestConfig" ]; then
	getTestKitGenAndFunctionalTestMaterial
fi
