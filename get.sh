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
SYSTEMTEST=false

usage ()
{
	echo 'Usage : get.sh  --testdir|-t openjdktestdir'
	echo '                --platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux'
	echo '                --jvmversion|-v openjdk9-openj9 | openjdk9 | openjdk8'
	echo '                [--sdkdir|-s binarySDKDIR] : if do not have a local sdk available, specify preferred directory'
	echo '                [--systemtest|-S ] : indicate need system test materials'
	echo '                [--sdk_resource|-r ] : indicate where to get sdk - releases, nightly or upstream'
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

			"--systemtest" | "-S" )
				SYSTEMTEST=true;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
}

getBinaryOpenjdk()
{
	cd $SDKDIR
	if [ "$SDK_RESOURCE" != "upstream" ]; then
		echo 'Get binary openjdk...'
		mkdir openjdkbinary
		download_url="https://api.adoptopenjdk.net/$JVMVERSION/$SDK_RESOURCE/$PLATFORM/latest/binary"
		wget --no-check-certificate --header 'Cookie: allow-download=1' ${download_url} --directory-prefix=${SDKDIR}/openjdkbinary
		if [ $? -ne 0 ]; then
			echo "Failed to retrieve the jdk binary, exiting"
			exit 1
		fi
	fi
	cd openjdkbinary
	jar_file_name=`ls`
	tar -zxf $jar_file_name
	jarDir=`ls -d */`
	dirName=${jarDir%?}
	mv $dirName j2sdk-image
}

getTestDependencies()
{
	cd $TESTDIR/TestConfig
	mkdir lib
	cd lib
	echo 'Get third party libs...'
	wget -q --output-document=asm-all-5.0.1.jar http://download.forge.ow2.org/asm/asm-5.0.1.jar
	wget -q https://downloads.sourceforge.net/project/junit/junit/4.10/junit-4.10.jar

	echo 'get jtreg...'
	wget -q --no-check-certificate https://ci.adoptopenjdk.net/job/jtreg/lastSuccessfulBuild/artifact/jtreg-4.2.0-tip.tar.gz
	if [ $? -ne 0 ]; then
		echo "Failed to retrieve the jtreg binary, exiting"
		exit 1
	fi
	tar xf jtreg-4.2.0-tip.tar.gz

	mkdir TestNG
	cd TestNG
	wget -q --output-document=testng.jar http://central.maven.org/maven2/org/testng/testng/6.10/testng-6.10.jar
	wget -q --output-document=jcommander.jar http://central.maven.org/maven2/com/beust/jcommander/1.48/jcommander-1.48.jar
}

getOpenjdk()
{
	cd $TESTDIR/openjdk_regression
	echo 'Get openjdk...'

	openjdkGit="openjdk-jdk8u"
	if [[ $JVMVERSION =~ "openjdk9" ]]; then
		openjdkGit="openjdk-jdk9"
	fi

	git clone -b dev -q https://github.com/AdoptOpenJDK/$openjdkGit.git
	if [ $? -ne 0 ]; then
		echo "Failed to retrieve the openjdk, exiting"
		exit 1
	fi

	openjdkDir=`ls -d */`
	openjdkDirName=${openjdkDir%?}
	mv $openjdkDirName openjdk-jdk
}

getSystemTests()
{
	testrepo="https://github.com/AdoptOpenJDK/openjdk-systemtest"
	stfrepo="https://github.com/AdoptOpenJDK/stf"

	cd $TESTDIR
	echo "Clone systemtest from $testrepo..."
	git clone $testrepo

	echo "Clone stf from $stfrepo..."
	git clone $stfrepo

	echo 'Get systemtest prereqs...'
	cd $TESTDIR/openjdk-systemtest/openjdk.build && make configure
	if [ "$?" != "0" ]; then
	        echo "Error configuring openjdk-systemtest - see build output" 1>&2
	        exit 1
	fi
}

parseCommandLineArgs "$@"
if [[ "$SDKDIR" != "" ]]; then
	getBinaryOpenjdk
fi
getTestDependencies
getOpenjdk
if [[ "$SYSTEMTEST" == "true" ]]; then
	getSystemTests
fi
