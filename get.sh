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

set -eo pipefail
SDKDIR=""
TESTDIR="$(pwd)"
PLATFORM=""
JVMVERSION=""
SDK_RESOURCE="nightly"
CUSTOMIZED_SDK_URL=""
CUSTOMIZED_SDK_SOURCE_URL=""
CLONE_OPENJ9="true"
OPENJ9_REPO="https://github.com/eclipse-openj9/openj9.git"
OPENJ9_SHA=""
OPENJ9_BRANCH=""
TKG_REPO="https://github.com/adoptium/TKG.git"
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
CURL_OPTS="s"
CODE_COVERAGE=false
ADDITIONAL_ARTIFACTS_REQUIRED=""

usage ()
{
	echo 'Usage : get.sh -platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix'
	echo '                [--jdk_version|-j ] : optional. JDK version'
	echo '                [--jdk_impl|-i ] : optional. JDK implementation'
	echo '                [--releases|-R ] : optional. Example: latest, jdk8u172-b00-201807161800'
	echo '                [--type|-T ] : optional. jdk or jre'
	echo '                [--sdkdir|-s binarySDKDIR] : if do not have a local sdk available, specify preferred directory'
	echo '                [--sdk_resource|-r ] : indicate where to get sdk - releases, nightly , upstream or customized'
	echo '                [--customizedURL|-c ] : indicate sdk url if sdk source is set as customized.  Multiple urls can be passed with space as separator'
	echo '                [--customized_sourceURL|-S ] : indicate sdk source url if sdk source is set as customized.'
	echo '                [--username ] : indicate username required if customized url requiring authorization is used'
	echo '                [--password ] : indicate password required if customized url requiring authorization is used'
	echo '                [--clone_openj9 ] : optional. true or false. Clone openj9 if this flag is set to true. Default to true'
	echo '                [--openj9_repo ] : optional. OpenJ9 git repo. Default value https://github.com/eclipse-openj9/openj9.git is used if not provided'
	echo '                [--openj9_sha ] : optional. OpenJ9 pull request sha.'
	echo '                [--openj9_branch ] : optional. OpenJ9 branch.'
	echo '                [--tkg_repo ] : optional. TKG git repo. Default value https://github.com/adoptium/TKG.git is used if not provided'
	echo '                [--tkg_branch ] : optional. TKG branch.'
	echo '                [--vendor_repos ] : optional. Comma separated Git repository URLs of the vendor repositories'
	echo '                [--vendor_shas ] : optional. Comma separated SHAs of the vendor repositories'
	echo '                [--vendor_branches ] : optional. Comma separated vendor branches'
	echo '                [--vendor_dirs ] : optional. Comma separated directories storing vendor test resources'
	echo '                [--code_coverage ] : optional. indicate if code coverage is required'
}

parseCommandLineArgs()
{
	while [ $# -gt 0 ] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--sdkdir" | "-s" )
				SDKDIR="$1"; shift;;

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

			"--code_coverage" )
				CODE_COVERAGE="$1"; shift;;

			"--curl_opts" )
				CURL_OPTS="$1"; shift;;

			"--additional_artifacts_required" )
				ADDITIONAL_ARTIFACTS_REQUIRED="$1"; shift;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done

	echo "TESTDIR: $TESTDIR"
}

# If the current directory contains only a single directory then squash that directory into the current directory
squashSingleFolderContentsToCurrentDir()
{
        # Does current directory contain one and ONLY one directory?
        if [[ $(ls -1 | wc -l) -eq 1 ]]; then
          folder=$(ls -d */)
          if [ -d "$folder" ]; then
            echo "Removing top-level folder ${folder}"
            mv ${folder}* .
            rmdir "${folder}"
          fi
        fi
}

# Moves the given directory safely, ensuring the target does not exist, fail with error if it does exist
moveDirectorySafely()
{
	if [ $# -lt 2 ]; then
		echo "Syntax: moveDirectorySafely <sourceDirectory> <targetDirectory>"
		exit 1
	fi
	if [ -d "$2" ]; then
		echo "ERROR: moveDirectorySafely $1 $2 : target directory $2 already exists"
		exit 1
	else
		echo "Moving directory $1 to $2"
		mv "$1" "$2"
	fi
}

getBinaryOpenjdk()
{
	echo "get jdk binary..."
	cd $SDKDIR
	mkdir -p jdkbinary
	cd jdkbinary

	if [ "$SDK_RESOURCE" != "upstream" ]; then
		if [ "$(ls -A $SDKDIR/jdkbinary)" ]; then
			echo "$SDKDIR/jdkbinary is not an empty directory, please empty it or specify a different SDK directory."
			echo "This directory is used to download SDK resources into it and the script will not overwrite its contents."
			exit 1
		fi
	fi

	# if these are passed through via withCredentials(CUSTOMIZED_SDK_URL_CREDENTIAL_ID) these will not be visible within job output,
	# if supplied when run manually with --username and --password these will be seen in plaintext within job output
	if [ "$USERNAME" != "" ] && [ "$PASSWORD" != "" ]; then
		curl_options="--user $USERNAME:$PASSWORD"
	fi

	if [ "$SDK_RESOURCE" == "nightly" ] && [ "$CUSTOMIZED_SDK_URL" != "" ]; then
		result=$(curl -k ${curl_options} ${CUSTOMIZED_SDK_URL} | grep ">[0-9]*\/<" | sed -e 's/[^0-9/ ]//g' | sed 's/\/.*$//')
		IFS=' ' read -r -a array <<< "$result"
		arr=(${result/ / })
		max=${arr[0]}
		for n in "${arr[@]}" ; do
			((n > max)) && max=$n
		done
		latestBuildUrl="${CUSTOMIZED_SDK_URL}${max}/"
		echo "downloading files from $latestBuildUrl"
		download_urls=$(curl -k ${curl_options} ${latestBuildUrl} | grep -E ">.*pax<|>.*tar.gz<|>.*zip<" | sed 's/^.*">//' | sed 's/<\/a>.*//')
		arr=(${download_urls/ / })
		download_url=()
		for n in "${arr[@]}" ; do
			required=true
			checkURL "$n"
			if [ $required != false ]; then
				download_url+=" ${latestBuildUrl}${n}"
			fi
		done
	elif [ "$SDK_RESOURCE" == "customized" ] && [ "$CUSTOMIZED_SDK_URL" != "" ]; then
		download_url=$CUSTOMIZED_SDK_URL
		images="test-images.tar.gz debug-image.tar.gz"
		download_urls=($download_url)
		# for now, auto-download is enabled only if users provide one URL and filename contains OpenJ9-JDK
		if [ "${#download_urls[@]}" = 1 ]; then
			download_filename=${download_url##*/}
			if [[ "$download_filename" =~ "OpenJ9-JDK" ]]; then
				link=${download_url%$download_filename}
				for image in $images
				do
					required=true
					checkURL "$image"
					if [ $required != false ]; then
						download_url+=" ${link}${image}"
						echo "auto download: ${link}${image}"
					fi
				done
			fi
		fi
	elif [ "$SDK_RESOURCE" = "nightly" ] || [ "$SDK_RESOURCE" = "releases" ]; then
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
			if [ "$JDK_IMPL" == "openj9" ]; then
				arch="x86-32"
			else
				arch="x32"
			fi
		fi
		release_type="ea"
		if [ "$SDK_RESOURCE" = "releases" ]; then
			release_type="ga"
		fi

		if [ "$JDK_IMPL" == "openj9" ]; then
			if [ "$CUSTOMIZED_SDK_URL" == "" ]; then
				echo "Please use CUSTOMIZED_SDK_URL to provide the base SDK URL for Artifactory."
				exit 1
			else
				download_url_base="${CUSTOMIZED_SDK_URL}/${arch}_${os}/"
				# Artifactory cannot handle duplicate slashes (//) in URL. Remove // except after http:// or https://
				download_url_base=$(echo "$download_url_base" | sed -r 's|([^:])/+|\1/|g')
				echo "artifactory URL: ${download_url_base}"
				download_api_url_base=(${download_url_base//\/ui\/native\//\/artifactory\/api\/storage\/})
				echo "use artifactory API to get the jdk and/or test images: ${download_api_url_base}"
				download_urls=$(curl ${curl_options} ${download_api_url_base} | grep -E '.*\.tar\.gz"|.*\.zip"' | grep -E 'testimage|jdk|jre'| sed 's/.*"uri" : "\([^"]*\)".*/\1/')
				arr=(${download_urls/ / })
				download_url=()
				download_url_base=(${download_url_base//\/ui\/native\//\/artifactory\/})
				echo "downloading files from $latestBuildUrl"
				for n in "${arr[@]}" ; do
					if [[ $n =~ 'testimage' ]]; then
						if [ "$TEST_IMAGES_REQUIRED" == "true" ]; then
							download_url+=" ${download_url_base}${n}"
						fi
					else
						download_url+=" ${download_url_base}${n}"
					fi
				done
				download_url=$(echo "$download_url" | sed -r 's|([^:])/+|\1/|g')
			fi
		else
			download_url="https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/${release_type}/${os}/${arch}/jdk/${JDK_IMPL}/${heap_size}/adoptium?project=jdk https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/${release_type}/${os}/${arch}/sbom/${JDK_IMPL}/${heap_size}/adoptium?project=jdk"
			info_url="https://api.adoptium.net/v3/assets/feature_releases/${JDK_VERSION}/${release_type}?architecture=${arch}&heap_size=${heap_size}&image_type=jdk&jvm_impl=${JDK_IMPL}&os=${os}&project=jdk&vendor=eclipse"

			if [ "$JDK_VERSION" != "8" ]; then
				download_url+=" https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/${release_type}/${os}/${arch}/testimage/${JDK_IMPL}/${heap_size}/adoptium?project=jdk"
				info_url+=" https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/${release_type}/${os}/${arch}/testimage/${JDK_IMPL}/${heap_size}/adoptium?project=jdk"
			fi
		fi
	else
		download_url=""
		echo "--sdkdir is set to $SDK_RESOURCE. Therefore, skip download jdk binary"
	fi

	if [ "${download_url}" != "" ]; then
		for file in $download_url
		do
			if [ "$ADDITIONAL_ARTIFACTS_REQUIRED" == "RI_JDK" ]; then
				if [[ $file = *?[0-9] ]]; then
					fileName=$(curl -k ${curl_options} ${file}/ | grep href | sed 's/.*href="//' | sed 's/".*//' |  grep '^[a-zA-Z].*')
					file=${file}/${fileName}
				fi
			fi
			executeCmdWithRetry "${file##*/}" "_ENCODE_FILE_NEW=UNTAGGED curl -OLJSk${CURL_OPTS} ${curl_options} $file"
			rt_code=$?
			if [ $rt_code != 0 ]; then
				echo "curl error code: $rt_code"
				echo "Failed to retrieve $file. This is what we received of the file and MD5 sum:"
				ls -ld $file

				if [[ "$OSTYPE" == "darwin"* ]]; then
					md5 $file
				else
					md5sum $file
				fi

				exit 1
			fi
		done
	fi

	# use openapi, try to get the information of the download file
	# if it returns the status code other than 200, it means the parameters provided by user forms some invalid link, then fails early
	if [ -n "$info_url" ]; then
		for info in $info_url
		do
			if [[ $info == https://api.adoptopenjdk.net* ]]; then
				http_resp_info=$(curl -Is "$info" | grep "HTTP/" | tail -1)
				# 2nd field of HTTP status line is the http response code (both HTTP/1.1 & 2)
				validate=$(echo "${http_resp_info}" | tr -s ' ' | cut -d' ' -f2)
				if [ "$validate" != 200 ]; then
					echo "Download failure, invalid download link: ${info}."
					exit 1
				fi
			fi
		done
	fi

	jdk_files=`ls`
	jdk_file_array=(${jdk_files//\\n/ })
	last_index=$(( ${#jdk_file_array[@]} - 1 ))
	if [[ $last_index == 0 ]]; then
		if [[ $download_url =~ '*.tar.gz' ]] || [[ $download_url =~ '*.zip' ]] || [[ $jdk_files == '*.zip' ]]; then
			nested_zip="${jdk_file_array[0]}"
			echo "${nested_zip} is a nested zip"
			unzip -q $nested_zip -d .
			rm $nested_zip
			jdk_files=$(ls *jdk*.tar.gz *jre*.tar.gz *testimage*.tar.gz *debugimage*.tar.gz *jdk*.zip *jre*.zip *testimage*.zip *debugimage*.zip tests-*.tar.gz symbols-*.tar.gz *static-libs*.tar.gz 2> /dev/null || true)
			echo "Found files under ${nested_zip}:"
			echo "${jdk_files}"
			jdk_file_array=(${jdk_files//\\n/ })
			last_index=$(( ${#jdk_file_array[@]} - 1 ))
		fi
	fi

	# if $jdk_file_array contains debug-image, move debug-image element to the end of the array
	# debug image jar needs to be extracted after jdk as debug image jar extraction location depends on jdk structure
	# debug image jar extracts into j2sdk-image/jre dir if it exists. Otherwise, extracts into j2sdk-image dir
	for i in "${!jdk_file_array[@]}"; do
		if [[ "${jdk_file_array[$i]}" =~ "debug-image" ]] || [[ "${jdk_file_array[$i]}" =~ "debugimage" ]]; then
			if [ "$i" -ne "$last_index" ]; then
				debug_image_jar="${jdk_file_array[$i]}"

				# remove the element
				unset jdk_file_array[$i]

				# add $debug_image_jar to the end of the array
				jdk_file_array=( "${jdk_file_array[@]}" "${debug_image_jar}" )
				break
			fi
		fi
	done

	for file_name in "${jdk_file_array[@]}"
	do
		if [[ ! "$file_name" =~ "sbom" ]]; then
			if [[ $file_name == *xz ]]; then
				DECOMPRESS_TOOL=xz
			else
				# Noting that this will be set, but not used, for zip files
				DECOMPRESS_TOOL=gzip
			fi
			if [[ "$file_name" =~ "debug-image" ]] || [[ "$file_name" =~ "debugimage" ]] || [[ "$file_name" =~ "symbols-" ]]; then
				# if file_name contains debug-image, extract into j2sdk-image/jre or j2sdk-image dir
				# Otherwise, files will be extracted under ./tmp
				extract_dir="./j2sdk-image"
				if [ -d "$SDKDIR/jdkbinary/j2sdk-image/jre" ]; then
					extract_dir="./j2sdk-image/jre"
				fi
				echo "Uncompressing $file_name over $extract_dir..."

				# Debug image tarballs vary in top-level folders between Vendors, eg.location of bin folder
				#     temurin: jdk-21.0.5+11-debug-image/jdk-21.0.5+11/bin
				#     semeru:  jdk-21.0.4+7-debug-image/bin

				# Unpack into a temp directory, remove 1 or maybe 2 top-level single folders, then copy over extract_dir
				mkdir dir.$$ && cd dir.$$
				if [[ $file_name == *zip ]] || [[ $file_name == *jar ]]; then
					unzip -q ../$file_name
				else
					$DECOMPRESS_TOOL -cd ../$file_name | tar xof -
				fi

				# Remove 1 possibly 2 top-level folders (debugimage has 2)
				squashSingleFolderContentsToCurrentDir
				squashSingleFolderContentsToCurrentDir

				# Copy to extract_dir
				cp -R * "../${extract_dir}" && cd .. && rm -rf dir.$$
			else
				if [ -d "$SDKDIR/jdkbinary/tmp" ]; then
					rm -rf $SDKDIR/jdkbinary/tmp/*
				else
					mkdir $SDKDIR/jdkbinary/tmp
				fi
				echo "Uncompressing file: $file_name ..."
				if [[ $file_name == *zip ]] || [[ $file_name == *jar ]]; then
					unzip -q $file_name -d ./tmp
				elif [[ $file_name == *.pax* ]]; then
					cd ./tmp
					pax -p xam -rzf ../$file_name
				else
					$DECOMPRESS_TOOL -cd $file_name | (cd tmp && tar xof -)
				fi

				cd $SDKDIR/jdkbinary/tmp
				echo "List files in jdkbinary folder..."
				ls -l $SDKDIR/jdkbinary
				echo "List files in jdkbinary/tmp folder..."
				ls -l
				jar_dirs=`ls -d */`
				jar_dir_array=(${jar_dirs//\\n/ })
				len=${#jar_dir_array[@]}
				if [ "$len" == 1 ]; then
					jar_dir_name=${jar_dir_array[0]}
					if [[ "$jar_dir_name" =~ "test-image" ]] || [[ "$jar_dir_name" =~ "tests-" ]]; then
						if [ "$jar_dir_name" != "openjdk-test-image" ]; then
							moveDirectorySafely $jar_dir_name ../openjdk-test-image
						fi
					elif [[ "$jar_dir_name" =~ "static-libs" ]]; then
						moveDirectorySafely $jar_dir_name ../static-libs
                                        elif [[ "$jar_dir_name" =~ jdk.*-src/ ]]; then
                                                moveDirectorySafely $jar_dir_name ../source-image
					elif [[ "$jar_dir_name" =~ jre* ]] && [ "$jar_dir_name" != "j2re-image" ]; then
						moveDirectorySafely $jar_dir_name ../j2re-image
					elif [[ "$jar_dir_name" =~ jdk* ]] && [ "$jar_dir_name" != "j2sdk-image" ]; then
						# If test sdk has already been expanded, this one must be the additional sdk
						isAdditional=0
						if [ -f "./j2sdk-image/release" ]; then
							isAdditional=1
						else
							if [ "$ADDITIONAL_ARTIFACTS_REQUIRED" == "RI_JDK" ]; then
								# Check release info
								if [ -d "./$jar_dir_name/Contents" ]; then # Mac
									release_info=$( cat ./$jar_dir_name/Contents/Home/release )
									UNZIPPED_ADDITIONAL_SDK="./$jar_dir_name/Contents/Home/"
								else
									release_info=$( cat ./$jar_dir_name/release )
									UNZIPPED_ADDITIONAL_SDK="./$jar_dir_name/"
								fi
								if [[ "$release_info" == *"Oracle"* ]]; then
									isAdditional=1
								fi
							fi
						fi
						if [ $isAdditional == 1 ]; then
							if [ -d "$SDKDIR/additionaljdkbinary" ]; then
								rm -rf $SDKDIR/additionaljdkbinary
							else
								mkdir $SDKDIR/additionaljdkbinary
							fi
							mv $UNZIPPED_ADDITIONAL_SDK/* $SDKDIR/additionaljdkbinary
							echo "RI JDK available at $SDKDIR/additionaljdkbinary/"
							echo "RI JDK version:"
							$SDKDIR/additionaljdkbinary/bin/java -version
						else
							moveDirectorySafely $jar_dir_name ../j2sdk-image
						fi
					# The following only needed if openj9 has a different image name convention
					elif [ "$jar_dir_name" != "j2sdk-image" ]; then
						moveDirectorySafely $jar_dir_name ../j2sdk-image
					fi
				elif [ "$len" -gt 1 ]; then
					moveDirectorySafely ../tmp ../j2sdk-image
				fi
				cd $SDKDIR/jdkbinary
			fi
		fi
	done

	if [ "$PLATFORM" = "s390x_zos" ]; then
		chmod -R 755 j2sdk-image
	fi
}

checkURL() {
	local filename="$1"
	if [[ $filename =~ "test-image" ]]; then
		required=$TEST_IMAGES_REQUIRED
	elif [[ $filename =~ "debug-image" ]] || [[ "$file_name" =~ "debugimage" ]]; then
		required=$DEBUG_IMAGES_REQUIRED
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
	if [[ "$sources_file" == *zip ]] || [[ "$sources_file" == *jar ]]; then
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
	if [ "$TKG_REPO" = "" ]; then
		TKG_REPO="https://github.com/adoptium/TKG.git"
	fi

	executeCmdWithRetry "TKG" "git clone -q $TKG_REPO"
	rt_code=$?
	if [ $rt_code != 0 ]; then
		echo "git clone error code: $rt_code"
		exit 1
	fi

	cd TKG
	echo "git rev-parse $TKG_BRANCH"
	if ! tkg_sha=(`git rev-parse $TKG_BRANCH`); then
		echo "git rev-parse origin/$TKG_BRANCH"
		tkg_sha=(`git rev-parse origin/$TKG_BRANCH`)
	fi

	echo "git checkout -q -f $tkg_sha"
	git checkout -q -f $tkg_sha

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

executeCmdWithRetry()
{
	set +e
	count=0
	rt_code=-1
	# when the command is not found (code 127), do not retry
	while [ "$rt_code" != 0 ] && [ "$rt_code" != 127 ] && [ "$count" -le 5 ]
	do
		if [ "$count" -gt 0 ]; then
			sleep_time=300
			echo "error code: $rt_code. Sleep $sleep_time secs, then retry $count..."
			sleep $sleep_time

			echo "check for $1. If found, the file will be removed."
			if [ "$1" != "" ] && [ -f "$1" ]; then
				echo "remove $1 before retry..."
				rm $1
			fi
		fi
		echo "$2"
		eval "$2"
		rt_code=$?
		count=$(( $count + 1 ))
	done
	set -e
	return "$rt_code"
}

getFunctionalTestMaterial()
{
	echo "get functional test material..."
	cd $TESTDIR

	if [ "$OPENJ9_BRANCH" != "" ]
	then
		OPENJ9_BRANCH="-b $OPENJ9_BRANCH"
	fi

	executeCmdWithRetry "openj9" "git clone --depth 1 --reference-if-able ${HOME}/openjdk_cache $OPENJ9_BRANCH $OPENJ9_REPO"
	rt_code=$?
	if [ $rt_code != 0 ]; then
		echo "git clone error code: $rt_code"
		exit 1
	fi

	if [ "$OPENJ9_SHA" != "" ]
	then
		echo "update to openj9 sha: $OPENJ9_SHA"
		cd openj9
		executeCmdWithRetry "" "git fetch -q --unshallow"
		if ! git checkout $OPENJ9_SHA; then
			echo "SHA not yet found. Continue fetching PR refs and tags..."
			echo "git fetch -q --tags $OPENJ9_REPO +refs/pull/*:refs/remotes/origin/pr/*"
			git fetch -q --tags $OPENJ9_REPO +refs/pull/*:refs/remotes/origin/pr/*
			echo "git checkout -q $OPENJ9_SHA"
			if ! git checkout $OPENJ9_SHA; then
				echo "SHA not yet found. Continue fetching all the branches on origin..."
				echo "git fetch -q --tags $OPENJ9_REPO +refs/heads/*:refs/remotes/origin/*"
				git fetch -q --tags $OPENJ9_REPO +refs/heads/*:refs/remotes/origin/*
				echo "git checkout -q $OPENJ9_SHA"
				git checkout $OPENJ9_SHA
			fi
		fi
		cd $TESTDIR
	fi

	checkOpenJ9RepoSHA

	ls -l
	mv openj9/test/TestConfig TestConfig
	mv openj9/test/Utils Utils
	if [ -d functional ]; then
		mv openj9/test/functional/* functional/
	else
		mv openj9/test/functional functional
	fi

	rm -rf openj9
}

getVendorTestMaterial() {
	echo "get vendor test material..."
	cd $TESTDIR

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
		echo "vendor repo is $repoURL"
		branchOption=""
		if [ "$branch" != "" ]; then
			branchOption="-b $branch"
		fi

		if [[ "$dir" =~ "jck" ]]; then
			echo "BUILD_LIST is $BUILD_LIST"
			if [[ "$BUILD_LIST" =~ "jck" || "$BUILD_LIST" =~ "external" ||"$BUILD_LIST" =~ "all" ]]; then
				echo "Remove existing subdir. $repoURL will be used..."
				rm -rf jck
			else
				echo "Skip git clone $repoURL"
				continue
			fi
		fi
		
		echo "git clone ${branchOption} $repoURL $dest"
		git clone -q --depth 1 $branchOption $repoURL $dest

		if [ "$sha" != "" ]; then
			cd $dest
			echo "git fetch -q --unshallow"
			git fetch -q --unshallow
			echo "update to $sha"
			git checkout $sha
			cd $TESTDIR
		fi

		echo "check vendor repo sha"
		repoName=$(basename $repoURL .git)
		checkRepoSHA $dest $repoName

		# move resources
		if [ "$dir" != "" ] && [ -d $dest/$dir ]; then
			echo "Stage $dest/$dir to $TESTDIR/$dir"
			# already in TESTDIR, thus copy $dir to current directory
			cp -r $dest/$dir ./
			if [[ "$PLATFORM" == *"zos"* ]]; then
				cp -r $dest/.git ./$dir
			fi
		else
			echo "Stage $dest to $TESTDIR"
			# already in TESTDIR, thus copy the entire vendor repo content to current directory
			cp -r $dest/* ./
		fi

		# clean up
		rm -rf $dest
	done
}

testJavaVersion()
{
	# use environment variable TEST_JDK_HOME to run java -version
	if [ "$TEST_JDK_HOME" = "" ]; then
		TEST_JDK_HOME=$SDKDIR/jdkbinary/j2sdk-image
	fi
	_java=${TEST_JDK_HOME}/bin/java
	_release=${TEST_JDK_HOME}/release
	# Code_Coverage use different _java through searching javac for now, following path will be modified after refining files from BUILD
	if [[ "$CODE_COVERAGE" == "true" ]]; then
		_java=${TEST_JDK_HOME}/build/bin/java
		_release=${TEST_JDK_HOME}/build/release
	fi
	if [ -x ${_java} ]; then
		echo "Run ${_java} -version"
		echo "=JAVA VERSION OUTPUT BEGIN="
		${_java} -version
		echo "=JAVA VERSION OUTPUT END="
		if [ -e ${_release} ]; then
			echo "=RELEASE INFO BEGIN="
			cat ${_release}
			echo "=RELEASE INFO END="
		fi
	else
		# Search javac as java may not be unique
		if [[ "$CODE_COVERAGE" == "true" ]]; then
			echo "${TEST_JDK_HOME}/build/bin/java does not exist! Searching under TEST_JDK_HOME: ${TEST_JDK_HOME}..."
			javac_path=`find ${TEST_JDK_HOME} \( -name javac -o -name javac.exe \) | egrep '/images/jdk/bin/javac$|/images/jdk/bin/javac.exe$'`
		else
			echo "${TEST_JDK_HOME}/bin/java does not exist! Searching under TEST_JDK_HOME: ${TEST_JDK_HOME}..."
			javac_path=`find ${TEST_JDK_HOME} \( -name javac -o -name javac.exe \) | egrep 'bin/javac$|bin/javac.exe$'`
		fi
		if [ "$javac_path" != "" ]; then
			echo "javac_path: ${javac_path}"
			javac_path_array=(${javac_path//\\n/ })
			_javac=${javac_path_array[0]}

			# for windows, replace \ to /, otherwise, readProperties() in Jenkins script cannot read \
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
	sha_file="$TESTDIR/TKG/SHA.txt"
	testenv_file="$TESTDIR/testenv/testenv.properties"

	echo "$TESTDIR/TKG/scripts/getSHA.sh --repo_dir $1 --output_file $sha_file"
	$TESTDIR/TKG/scripts/getSHA.sh --repo_dir $1 --output_file $sha_file

	echo "$TESTDIR/TKG/scripts/getTestenvProperties.sh --repo_dir $1 --output_file $testenv_file --repo_name $2"
	$TESTDIR/TKG/scripts/getTestenvProperties.sh --repo_dir $1 --output_file $testenv_file --repo_name $2
}

checkTestRepoSHAs()
{
	echo "check adoptium repo and TKG repo SHA"

	output_file="$TESTDIR/TKG/SHA.txt"
	if [ -e ${output_file} ]; then
		echo "rm $output_file"
		rm ${output_file}
	fi

	checkRepoSHA "$TESTDIR" "ADOPTOPENJDK"
	checkRepoSHA "$TESTDIR/TKG" "TKG"
}

checkOpenJ9RepoSHA()
{
	echo "check OpenJ9 Repo sha"
	checkRepoSHA "$TESTDIR/openj9" "OPENJ9"
}

parseCommandLineArgs "$@"
if [ "$USE_TESTENV_PROPERTIES" = true ]; then
	teFile="./testenv/testenv.properties"
	if [[ "$PLATFORM" == *"zos"* ]]; then
		echo "load ./testenv/testenv_zos.properties"
		source ./testenv/testenv_zos.properties
		teFile="./testenv/testenv_zos.properties"
	elif [[ "$PLATFORM" == *"arm"* ]] && [[ "$JDK_VERSION" == "8" ]] ; then
		echo "load ./testenv/testenv_arm32.properties"
		source ./testenv/testenv_arm32.properties
		teFile="./testenv/testenv_arm32.properties"
	else
		echo "load ./testenv/testenv.properties"
		source ./testenv/testenv.properties
	fi
	if [[ $JDK_IMPL != "openj9" && $JDK_IMPL != "ibm" ]]; then
		echo "Running checkTags with $teFile and $JDK_VERSION"
		./scripts/testenv/checkTags.sh $teFile $JDK_VERSION
	fi
else
	> ./testenv/testenv.properties
fi

# unset LD_LIBRARY_PATH workaround for issue https://github.com/adoptium/infrastructure/issues/2934
if [[ $JDK_IMPL == 'hotspot' && $JDK_VERSION == '8' && $PLATFORM =~ 'alpine-linux' ]]; then
	unset LD_LIBRARY_PATH
fi

if [ "$SDKDIR" != "" ]; then
	getBinaryOpenjdk
	testJavaVersion
fi

if [ "$SDK_RESOURCE" == "customized" ] && [ "$CUSTOMIZED_SDK_SOURCE_URL" != "" ]; then
	getOpenJDKSources
fi

if [ ! -d "$TESTDIR/TKG" ]; then
	getTestKitGen
fi

if [ "$JTREG_URL" != "" ]; then
	getCustomJtreg
fi

if [ $CLONE_OPENJ9 != "false" ]; then
	getFunctionalTestMaterial
fi

if [ "$VENDOR_REPOS" != "" ]; then
	getVendorTestMaterial
fi
