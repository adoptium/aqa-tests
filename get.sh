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
	wget --no-check-certificate ${download_url}
	jar_file_name=`ls`
	tar -zxvf $jar_file_name
	sdkDir=`ls -d */`
	sdkDirName=${sdkDir%?}
	mv $sdkDirName j2sdk-image
fi

cd $openjdktest/TestConfig
mkdir lib
cd lib
echo 'Get third party libs...'
wget http://download.forge.ow2.org/asm/asm-5.0.1.jar
wget https://downloads.sourceforge.net/project/junit/junit/4.10/junit-4.10.jar
mv asm-5.0.1.jar asm-all-5.0.1.jar

mkdir TestNG
cd TestNG
wget http://central.maven.org/maven2/org/testng/testng/6.10/testng-6.10.jar
wget http://central.maven.org/maven2/com/beust/jcommander/1.48/jcommander-1.48.jar
mv testng-6.10.jar testng.jar
mv jcommander-1.48.jar jcommander.jar

cd $openjdktest/TestConfig/lib
echo 'get jtreg...'
wget --no-check-certificate https://ci.adoptopenjdk.net/job/jtreg/lastSuccessfulBuild/artifact/jtreg-4.2.0-tip.tar.gz
if [ $? -ne 0 ]; then
	echo "Failed to retrieve the jtreg binary, exiting"
exit
fi
tar xf jtreg-4.2.0-tip.tar.gz

cd $openjdktest/OpenJDK_Playlist
echo 'Get openjdk...'
git clone -b dev https://github.com/AdoptOpenJDK/openjdk-jdk8u.git

