#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

SDKDIR=""
TESTDIR=""
PLATFORM=""
JVMVERSION=""
SDK_RESOURCE="nightly"
CUSTOMIZED_SDK_URL=""

usage ()
{
	echo 'Usage : get.sh  --testdir|-t openjdktestdir'
	echo '                --platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix'
	echo '                --jvmversion|-v openjdk8 | openjdk8-openj9 | openjdk9 | openjdk9-openj9 | openjdk10 | openjdk10-sap'
	echo '                [--sdkdir|-s binarySDKDIR] : if do not have a local sdk available, specify preferred directory'
	echo '                [--sdk_resource|-r ] : indicate where to get sdk - releases, nightly , upstream or customized'
	echo '                [--customizedURL|-c ] : indicate sdk url if sdk source is set as customized'
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

			"--jvmversion" | "-v" )
				JVMVERSION="$1"; shift;;

			"--sdk_resource" | "-r" )
				SDK_RESOURCE="$1"; shift;;
			
			"--customizedURL" | "-c" )
				CUSTOMIZED_SDK_URL="$1"; shift;;
			
			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
}

getBinaryOpenjdk()
{
	cd $SDKDIR
	if [[ "$CUSTOMIZED_SDK_URL" == "" ]]; then
		if [[ "$SDK_RESOURCE" == "nightly" || "$SDK_RESOURCE" == "releases" ]]; then
			echo 'Get binary openjdk...'
			mkdir openjdkbinary
			download_url="https://api.adoptopenjdk.net/$JVMVERSION/$SDK_RESOURCE/$PLATFORM/latest/binary"
			wgetSDK
		fi
	else 
		download_url=$CUSTOMIZED_SDK_URL
		wgetSDK
	fi
	
	cd openjdkbinary
	
	jar_file_name=`ls`
	if [[ $jar_file_name == *zip || $jar_file_name == *jar ]]; then
		unzip -q $jar_file_name -d .
	else
		echo $jar_file_name 
		tar -zxf $jar_file_name
	fi
	jarDir=`ls -d */`
	dirName=${jarDir%?}
	mv $dirName j2sdk-image
}

getTestKitGen()
{
	cd $TESTDIR
	git clone https://github.com/eclipse/openj9.git
	cd openj9
	git filter-branch --subdirectory-filter test/TestConfig

	rm extraSettings.mk
	cd $TESTDIR
	mv openj9 TestConfig
}

wgetSDK()
{
	wget --no-check-certificate --header 'Cookie: allow-download=1' ${download_url} --directory-prefix=${SDKDIR}/openjdkbinary
	if [ $? -ne 0 ]; then
		echo "Failed to retrieve the jdk binary, exiting"
		exit 1
	fi
}

parseCommandLineArgs "$@"
getTestKitGen
if [[ "$SDKDIR" != "" ]]; then
	getBinaryOpenjdk
fi
