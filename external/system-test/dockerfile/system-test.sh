#/bin/bash
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
#

#Set up Java to be used by the system-test

if [ -d /java/bin ];then
	echo "Using mounted Java"
	export JAVA_HOME=/java
	export PATH=$JAVA_HOME/bin:$PATH
else
	echo "Using docker image default Java"
	java_path=$(type -p java)
	suffix="/java"
	java_root=${java_path%$suffix}
	java_home=$(dirname $java_root)
	export JAVA_HOME=$java_home
fi


export TEST_JDK_HOME=$JAVA_HOME
echo "TEST_JDK_HOME is : $TEST_JDK_HOME"
export BUILD_LIST=system

test=$@

cd /openjdk-tests
./get.sh -t /openjdk-tests

cd /openjdk-tests/TestConfig

echo "Generating make files..."
make -f run_configure.mk

echo "Building system test material..." 
make compile

echo "Running test $test..."
sh /openjdk-tests/maketest.sh /openjdk-tests _$test
