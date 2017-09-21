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

openjdktest=$(pwd)
# possible platforms : x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux 
if [ "$#" -gt 2 ]
then
platform=$3
jvm_version=$4
fi

if [ "$#" -eq 1 ]
then
	openjdktest=$1
else
	openjdktest=$2
	echo 'Get binary openjdk...'
	cd $1
	mkdir openjdkbinary
	cd openjdkbinary
	download_url="https://api.adoptopenjdk.net/$jvm_version/releases/$platform/latest/binary"
	wget -q --no-check-certificate --header 'Cookie: allow-download=1' ${download_url}
	if [ $? -ne 0 ]; then
		echo "Failed to retrieve the jdk binary, exiting"
		exit
	fi
	jar_file_name=`ls`
	tar -zxf $jar_file_name
	sdkDir=`ls -d */`
	sdkDirName=${sdkDir%?}
	mv $sdkDirName j2sdk-image
fi

cd $openjdktest/TestConfig
mkdir lib
cd lib
echo 'Get third party libs...'
wget -q http://download.forge.ow2.org/asm/asm-5.0.1.jar
wget -q https://downloads.sourceforge.net/project/junit/junit/4.10/junit-4.10.jar
mv asm-5.0.1.jar asm-all-5.0.1.jar

mkdir TestNG
cd TestNG
wget -q http://central.maven.org/maven2/org/testng/testng/6.10/testng-6.10.jar
wget -q http://central.maven.org/maven2/com/beust/jcommander/1.48/jcommander-1.48.jar
mv testng-6.10.jar testng.jar
mv jcommander-1.48.jar jcommander.jar

cd $openjdktest/TestConfig/lib
echo 'get jtreg...'
wget -q --no-check-certificate https://ci.adoptopenjdk.net/job/jtreg/lastSuccessfulBuild/artifact/jtreg-4.2.0-tip.tar.gz
if [ $? -ne 0 ]; then
	echo "Failed to retrieve the jtreg binary, exiting"
exit
fi
tar xf jtreg-4.2.0-tip.tar.gz

cd $openjdktest/OpenJDK_Playlist
echo 'Get openjdk...'

openjdkGit="openjdk-jdk8u"
if [[ $jvm_version =~ "openjdk9" ]]; then
	openjdkGit="openjdk-jdk9"
fi

git clone -b dev -q https://github.com/AdoptOpenJDK/$openjdkGit.git

openjdkDir=`ls -d */`
openjdkDirName=${openjdkDir%?}
mv $openjdkDirName openjdk-jdk