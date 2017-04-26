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
if [ "$#" -eq 2 ]
then
	openjdktest=$2
	echo 'Get binary openjdk...'
	cd $1
	mkdir openjdkbinary
	cd openjdkbinary
	download_url=`curl https://api.github.com/repos/AdoptOpenJDK/openjdk-releases/releases/latest | jq -r '.assets[] | select(.browser_download_url | contains("x64_Linux_jdk")) | {url: .browser_download_url} | select (.url | contains("tar.gz")) | .[]'`
	wget "${download_url}"
	#wget https://github.com/AdoptOpenJDK/openjdk-releases/releases/download/jdk8u152-b01/OpenJDK8_x64_Linux_jdk8u152-b01.tar.gz
	jar_file_url=$download_url
	jar_file_name=${jar_file_url##*/}
	tar -zxvf $jar_file_name
	#tar -zxvf OpenJDK8_x64_Linux_jdk8u152-b01.tar.gz
elif [ "$#" -eq 1 ]; then
	openjdktest=$1
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



