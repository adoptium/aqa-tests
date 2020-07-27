#!/usr/bin/env bash
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
TESTDIR="$(pwd)"
PLATFORM=""
JVMVERSION=""
SDK_RESOURCE="nightly"
CUSTOMIZED_SDK_URL=""
CUSTOMIZED_SDK_SOURCE_URL=""
CLONE_OPENJ9="true"
OPENJ9_REPO="https://github.com/eclipse/openj9.git"
OPENJ9_SHA=""
OPENJ9_BRANCH=""
TKG_REPO="https://github.com/AdoptOpenJDK/TKG.git"
TKG_SHA=""
TKG_BRANCH="master"
VENDOR_REPOS=""
VENDOR_SHAS=""
VENDOR_BRANCHES=""
VENDOR_DIRS=""
JDK_VERSION="8"
JDK_IMPL="openj9"
RELEASES="latest"
TYPE="jdk"
TEST_IMAGES_REQUIRED=true
DEBUG_IMAGES_REQUIRED=true

usage ()
{
	echo 'Usage : get.sh  --testdir|-t optional. path to openjdktestdir. Default value current dir (pwd) is used if not provided'
	echo '                --platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix'
	echo '                [--jdk_version|-j ]: optional. JDK version'
	echo '                [--jdk_impl|-i ]: optional. JDK implementation'
	echo '                [--releases|-R ]: optional. Example: latest, jdk8u172-b00-201807161800'
	echo '                [--type|-T ]: optional. jdk or jre'
	echo '                [--sdkdir|-s binarySDKDIR] : if do not have a local sdk available, specify preferred directory'
	echo '                [--sdk_resource|-r ] : indicate where to get sdk - releases, nightly , upstream or customized'
	echo '                [--customizedURL|-c ] : indicate sdk url if sdk source is set as customized.  Multiple urls can be passed with space as separator'
	echo '                [--customized_sourceURL|-S ] : indicate sdk source url if sdk source is set as customized.'
	echo '                [--username ] : indicate username required if customized url requiring authorization is used'
	echo '                [--password ] : indicate password required if customized url requiring authorization is used'
	echo '                [--clone_openj9 ] : optional. ture or false. Clone openj9 if this flag is set to true. Default to true'
	echo '                [--openj9_repo ] : optional. OpenJ9 git repo. Default value https://github.com/eclipse/openj9.git is used if not provided'
	echo '                [--openj9_sha ] : optional. OpenJ9 pull request sha.'
	echo '                [--openj9_branch ] : optional. OpenJ9 branch.'
	echo '                [--tkg_repo ] : optional. TKG git repo. Default value https://github.com/AdoptOpenJDK/TKG.git is used if not provided'
	echo '                [--tkg_sha ] : optional. TkG pull request sha.'
	echo '                [--tkg_branch ] : optional. TKG branch.'
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

			"--customized_sourceURL" | "-S" )
				CUSTOMIZED_SDK_SOURCE_URL="$1"; shift;;

			"--username" )
				USERNAME="$1"; shift;;

			"--password" )
				PASSWORD="$1"; shift;;

			"--clone_openj9" )
				CLONE_OPENJ9="$1"; shift;;

			"--openj9_repo" )
				OPENJ9_REPO="$1"; shift;;

			"--openj9_sha" )
				OPENJ9_SHA="$1"; shift;;

			"--openj9_branch" )
				OPENJ9_BRANCH="$1"; shift;;

			"--tkg_repo" )
				TKG_REPO="$1"; shift;;

			"--tkg_sha" )
				TKG_SHA="$1"; shift;;

			"--tkg_branch" )
				TKG_BRANCH="$1"; shift;;

			"--vendor_repos" )
				VENDOR_REPOS="$1"; shift;;

			"--vendor_shas" )
				VENDOR_SHAS="$1"; shift;;

			"--vendor_branches" )
				VENDOR_BRANCHES="$1"; shift;;

			"--vendor_dirs" )
				VENDOR_DIRS="$1"; shift;;

			"--test_images_required" )
				TEST_IMAGES_REQUIRED="$1"; shift;;
			
			"--debug_images_required" )
				DEBUG_IMAGES_REQUIRED="$1"; shift;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done

	# Check if TESTDIR exists and points to openjdk-tests
	if [[ ! -d "$TESTDIR" || "$TESTDIR" != *"openjdk-tests"* ]]; then
		echo "TESTDIR: $TESTDIR is invalid. Please use --testdir|-t to set valid TESTDIR under openjdk-tests. Default value current dir (pwd) is used if not provided."
		exit 1
	fi
	echo "TESTDIR: $TESTDIR"
}

getBinaryOpenjdk()
{
	echo "get jdk binary..."
	cd $SDKDIR
	mkdir -p openjdkbinary
	cd openjdkbinary

	if [ "$SDK_RESOURCE" != "upstream" ]; then
		if [ "$(ls -A $SDKDIR/openjdkbinary)" ]; then
        	echo "$SDKDIR/openjdkbinary is not an empty directory, please empty it or specify a different SDK directory."
        	echo "This directory is used to download SDK resources into it and the script will not overwrite its contents."
        	exit 1
        fi
    fi

	if [ "$CUSTOMIZED_SDK_URL" != "" ]; then
		download_url=$CUSTOMIZED_SDK_URL
                # if these are passed through via withCredentials(CUSTOMIZED_SDK_URL_CREDENTIAL_ID) these will not be visible within job output,
                # if supplied when run manually with --username and --password these will be seen in plaintext within job output
		if [ "$USERNAME" != "" ] && [ "$PASSWORD" != "" ]; then
			curl_options="--user $USERNAME:$PASSWORD"
		fi
		images="test-images.tar.gz debug-image.tar.gz"
		download_urls=($download_url)
		# for now, auto-download is enabled only if users provide one URL and filename contains OpenJ9-JDK
		if [[ "${#download_urls[@]}" == 1 ]]; then
			download_filename=${download_url##*/}
			if [[ "$download_filename" =~ "OpenJ9-JDK" ]]; then
				link=${download_url%$download_filename}
				for image in $images
				do
					required=true
					checkURL "$image"
					if [[ $required != false ]]; then
						download_url+=" ${link}${image}"
						echo "auto download: ${link}${image}"
					fi
				done
			fi
		fi
	elif [ "$SDK_RESOURCE" == "nightly" ] || [ "$SDK_RESOURCE" == "releases" ]; then
		os=${PLATFORM#*_}
		os=${os%_xl}
		arch=${PLATFORM%%_*}
		heap_size="normal"
		if [[ $PLATFORM = *"_xl"* ]]; then
			heap_size="large"
		fi
		if [[ $arch = *"x86-64"* ]]; then
			arch="x64"
		fi
		if [[ $arch = *"x86-32"* ]]; then
			arch="x32"
		fi
		release_type="ea"
		if [ "$SDK_RESOURCE" == "releases" ]; then
			release_type="ga"
		fi
		download_url="https://api.adoptopenjdk.net/v3/binary/latest/${JDK_VERSION}/${release_type}/${os}/${arch}/jdk/${JDK_IMPL}/${heap_size}/adoptopenjdk"

		if [[ "$JDK_VERSION" -ge "11" ]]; then
			download_url+=" https://api.adoptopenjdk.net/v3/binary/latest/${JDK_VERSION}/${release_type}/${os}/${arch}/testimage/${JDK_IMPL}/${heap_size}/adoptopenjdk"
		fi
	else
		download_url=""
		echo "--sdkdir is set to $SDK_RESOURCE. Therefore, skip download jdk binary"
	fi

	if [ "${download_url}" != "" ]; then
		for file in $download_url
		do
			set +e
			count=0
			download_exit_code=-1
			while [ $download_exit_code != 0 ] && [ $count -le 5  ]
			do
				if [ $count -gt 0 ]; then
					sleep_time=300
					echo "curl error code: $download_exit_code. Sleep $sleep_time secs, then retry $count..."
					sleep $sleep_time

					download_filename=${file##*/}
					echo "check for $download_filename. If found, the file will be removed."
					if [ -f "$download_filename" ]; then
						echo "remove $download_filename before retry..."
						rm $download_filename
					fi
				fi

				case "$VERBOSE_CURL" in
					VERBOSE)
						curl_verbosity="v"
						;;
					NORMAL)
						curl_verbosity=''
						;;
					*)
						curl_verbosity="s"
						;;
				esac

				echo "_ENCODE_FILE_NEW=UNTAGGED curl -OLJSk${curl_verbosity} ${curl_options} $file"
				_ENCODE_FILE_NEW=UNTAGGED curl -OLJSk${curl_verbosity} ${curl_options} $file
				download_exit_code=$?
				count=$(( $count + 1 ))
			done

			if [ $download_exit_code != 0 ]; then
				echo "curl error code: $download_exit_code"
				echo "Failed to retrieve $file, exiting. This is what we received of the file and MD5 sum:"
				ls -ld $file
				
				if [[ "$OSTYPE" == "darwin"* ]]; then
				    md5 $file
				 else
				    md5sum $file
				fi
						
				exit 1
			fi
			set -e
		done
	fi

	jar_files=`ls`
	jar_file_array=(${jar_files//\\n/ })

	# if $jar_file_array contains debug-image, move debug-image element to the end of the array
	# debug image jar needs to be extracted after jdk as debug image jar extraction location depends on jdk structure
	# debug image jar extracts into j2sdk-image/jre dir if it exists. Otherwise, extracts into j2sdk-image dir
	if [[ $DEBUG_IMAGES_REQUIRED = true ]]; then
		last_index=$(( ${#jar_file_array[@]} -1 ))
		for i in "${!jar_file_array[@]}"; do
			if [[ "${jar_file_array[$i]}" =~ "debug-image" || "${jar_file_array[$i]}" =~ "debugimage" ]]; then
				if [[ $i -ne $last_index ]]; then
					debug_image_jar="${jar_file_array[$i]}"

					#remove the element
					unset jar_file_array[$i]

					# add $debug_image_jar to the end of the array
					jar_file_array=( "${jar_file_array[@]}" "${debug_image_jar}" )
					break
				fi
			fi
		done
	fi

	for jar_name in "${jar_file_array[@]}"
		do
			# if jar_name contains debug-image, extract into j2sdk-image/jre or j2sdk-image dir
			# Otherwise, files will be extracted under ./tmp
			if [[ "$jar_name"  =~ "debug-image" || "$jar_name"  =~ "debugimage" ]]; then
				extract_dir="./j2sdk-image"
				if [ -d "$SDKDIR/openjdkbinary/j2sdk-image/jre" ]; then
					extract_dir="./j2sdk-image/jre"
				fi
				echo "unzip $jar_name in $extract_dir..."
				if [[ $jar_name == *zip || $jar_name == *jar ]]; then
					unzip -q $jar_name -d $extract_dir
				else
					# some debug-image tar has parent folder. --strip 1 is used to remove it
					gzip -cd $jar_name | tar xof - -C $extract_dir --strip 1
				fi
			else
				if [ -d "$SDKDIR/openjdkbinary/tmp" ]; then
					rm -rf $SDKDIR/openjdkbinary/tmp/*
				else
					mkdir $SDKDIR/openjdkbinary/tmp
				fi
				echo "unzip file: $jar_name ..."
				if [[ $jar_name == *zip || $jar_name == *jar ]]; then
					unzip -q $jar_name -d ./tmp
				elif [[ $jar_name == *pax ]]; then
					cd ./tmp
					pax -p xam -rzf ../$jar_name
				else
					gzip -cd $jar_name | tar xof - -C ./tmp
				fi

				cd $SDKDIR/openjdkbinary/tmp
				jar_dirs=`ls -d */`
				jar_dir_array=(${jar_dirs//\\n/ })
				len=${#jar_dir_array[@]}
				if [[ "$len" == 1 ]]; then
					jar_dir_name=${jar_dir_array[0]}
					if [[ "$jar_dir_name" =~ "test-image" && "$jar_dir_name" != "openjdk-test-image" ]]; then
						mv $jar_dir_name ../openjdk-test-image
					elif [[ "$jar_dir_name" =~ jre*  &&  "$jar_dir_name" != "j2re-image" ]]; then
						mv $jar_dir_name ../j2re-image
					elif [[ "$jar_dir_name" =~ jdk*  &&  "$jar_dir_name" != "j2sdk-image" ]]; then
						mv $jar_dir_name ../j2sdk-image
					# if native test libs folder is available, mv it under native-test-libs
					elif [[ "$jar_dir_name"  =~ native-test-libs*  &&  "$jar_dir_name" != "native-test-libs" ]]; then
						mv $jar_dir_name ../native-test-libs
					#The following only needed if openj9 has a different image name convention
					elif [[ "$jar_dir_name" != "j2sdk-image"  &&  "$jar_dir_name" != "native-test-libs" ]]; then
						mv $jar_dir_name ../j2sdk-image
					fi
				elif [[ "$len" > 1 ]]; then
					mv ../tmp ../j2sdk-image
				fi
				cd $SDKDIR/openjdkbinary
			fi
		done

	if [[ "$PLATFORM" == "s390x_zos" ]]; then
		chmod -R 755 j2sdk-image
	fi
}

checkURL() {
	local filename="$1"
	if [[ $filename =~ "test-image" && $TEST_IMAGES_REQUIRED = false ]]; then
		required=false
	elif [[ $filename =~ "debug-image" && $DEBUG_IMAGES_REQUIRED = false ]]; then
		required=false
	fi
}

getOpenJDKSources() {
	echo "get jdk sources..."
	cd $TESTDIR
	mkdir -p openjdk/src
	cd openjdk/src
	echo "_ENCODE_FILE_NEW=UNTAGGED curl -OLJks --retry 5 --retry-delay 300 ${curl_options} $CUSTOMIZED_SDK_SOURCE_URL"
	_ENCODE_FILE_NEW=UNTAGGED curl -OLJks --retry 5 --retry-delay 300 ${curl_options} $CUSTOMIZED_SDK_SOURCE_URL
	sources_file=`ls`
	if [[ $sources_file == *zip || $sources_file == *jar ]]; then
		unzip -q $sources_file -d .
	else
		gzip -cd $sources_file | tar xof -
	fi
	rm $sources_file
	folder=`ls -d */`
	mv $folder ../openjdk-jdk
	cd ../
	rm -rf src
}

getTestKitGen()
{
	echo "get testKitGen..."
	cd $TESTDIR

	if [ "$TKG_BRANCH" != "" ]
	then
		TKG_BRANCH="-b $TKG_BRANCH"
	fi

	echo "git clone $TKG_BRANCH $TKG_REPO"
	git clone -q $TKG_BRANCH $TKG_REPO

	if [ "$TKG_SHA" != "" ]
	then
		echo "update to tkg sha: $TKG_SHA"
		cd TKG
		echo "git fetch -q --tags $TKG_REPO +refs/pull/*:refs/remotes/origin/pr/*"
		git fetch -q --tags $TKG_REPO +refs/pull/*:refs/remotes/origin/pr/*
		echo "git checkout -q $TKG_SHA"
		git checkout -q $TKG_SHA
		cd $TESTDIR
	fi

	checkTestRepoSHAs
}

getCustomJtreg()
{
	echo "get custom Jtreg..."
	cd $TESTDIR/openjdk
	if [ "$USERNAME" != "" ] && [ "$PASSWORD" != "" ]; then
		curl_options="--user $USERNAME:$PASSWORD"
	fi
	echo "_ENCODE_FILE_NEW=UNTAGGED curl -LJks -o custom_jtreg.tar.gz --retry 5 --retry-delay 300 ${curl_options} $JTREG_URL"
	_ENCODE_FILE_NEW=UNTAGGED curl -LJks -o custom_jtreg.tar.gz --retry 5 --retry-delay 300 ${curl_options} $JTREG_URL

}

getFunctionalTestMaterial()
{
	echo "get functional test material..."
	cd $TESTDIR

	if [ "$OPENJ9_BRANCH" != "" ]
	then
		OPENJ9_BRANCH="-b $OPENJ9_BRANCH"
	fi

	echo "git clone --depth 1 $OPENJ9_BRANCH $OPENJ9_REPO"
	git clone --depth 1 -q $OPENJ9_BRANCH $OPENJ9_REPO

	if [ "$OPENJ9_SHA" != "" ]
	then
		echo "update to openj9 sha: $OPENJ9_SHA"
		cd openj9
		echo "git fetch -q --unshallow"
		git fetch -q --unshallow
		if ! git checkout $OPENJ9_SHA; then
			echo "SHA not yet found. Continue fetching PR refs and tags..."
			echo "git fetch -q --tags $OPENJ9_REPO +refs/pull/*:refs/remotes/origin/pr/*"
			git fetch -q --tags $OPENJ9_REPO +refs/pull/*:refs/remotes/origin/pr/*
			echo "git checkout -q $OPENJ9_SHA"
			git checkout -q $OPENJ9_SHA
		fi
		cd $TESTDIR
	fi

	mv openj9/test/TestConfig TestConfig
	mv openj9/test/Utils Utils
    if [ -d functional ]; then
        mv openj9/test/functional/* functional/
    else
	    mv openj9/test/functional functional
    fi
	checkOpenJ9RepoSHA

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
# use environment variable TEST_JDK_HOME to run java -version
if [[ $TEST_JDK_HOME == "" ]]; then
	TEST_JDK_HOME=$SDKDIR/openjdkbinary/j2sdk-image
fi
_java=${TEST_JDK_HOME}/bin/java
if [ -x ${_java} ]; then
	echo "Run ${_java} -version"
	echo "=JAVA VERSION OUTPUT BEGIN="
	${_java} -version
	echo "=JAVA VERSION OUTPUT END="
else
	echo "${TEST_JDK_HOME}/bin/java does not exist! Searching under TEST_JDK_HOME: ${TEST_JDK_HOME}..."
	# Search javac as java may not be unique
	javac_path=`find ${TEST_JDK_HOME} \( -path "*/bin/javac" -o -path "*/bin/javac.exe" \)`
	if [[ $javac_path != "" ]]; then
		echo "javac_path: ${javac_path}"
		javac_path_array=(${javac_path//\\n/ })
		_javac=${javac_path_array[0]}

		# for windows, replace \ to /. Otherwise, readProperties() in Jenkins script cannot read \
		if [[ "${_javac}" =~ "javac.exe" ]]; then
			_javac="${_javac//\\//}"
		fi

		java_dir=$(dirname "${_javac}")
		echo "Run: ${java_dir}/java -version"
		echo "=JAVA VERSION OUTPUT BEGIN="
		${java_dir}/java -version
		echo "=JAVA VERSION OUTPUT END="
		TEST_JDK_HOME=${java_dir}/../
		echo "TEST_JDK_HOME=${TEST_JDK_HOME}" > ${TESTDIR}/job.properties
	else
		echo "Cannot find javac under TEST_JDK_HOME: ${TEST_JDK_HOME}!"
		exit 1
	fi
fi
}

checkRepoSHA()
{
	output_file="$TESTDIR/TKG/SHA.txt"
	echo "$TESTDIR/TKG/scripts/getSHA.sh --repo_dir $1 --output_file $output_file"
	$TESTDIR/TKG/scripts/getSHA.sh --repo_dir $1 --output_file $output_file
}

checkTestRepoSHAs()
{
	echo "check AdoptOpenJDK repo and TKG repo SHA"

	output_file="$TESTDIR/TKG/SHA.txt"
	if [ -e ${output_file} ]; then
		echo "rm $output_file"
		rm ${output_file}
	fi

	checkRepoSHA "$TESTDIR"
	checkRepoSHA "$TESTDIR/TKG"
}

checkOpenJ9RepoSHA()
{
	echo "check OpenJ9 Repo sha"
	checkRepoSHA "$TESTDIR/openj9"
}

parseCommandLineArgs "$@"
if [[ "$SDKDIR" != "" ]]; then
	getBinaryOpenjdk
	testJavaVersion
fi
if [ "$SDK_RESOURCE" == "customized" ] && [ "$CUSTOMIZED_SDK_SOURCE_URL" != "" ]; then
	getOpenJDKSources
fi

if [ ! -d "$TESTDIR/TKG" ]; then
	getTestKitGen
fi

if [[ $JTREG_URL != "" ]]; then
	getCustomJtreg
fi

if [ $CLONE_OPENJ9 != "false" ]; then
	getFunctionalTestMaterial
fi
