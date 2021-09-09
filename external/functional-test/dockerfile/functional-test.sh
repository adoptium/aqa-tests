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

source $(dirname "$0")/test_base_functions.sh

#Set up Java to be used by the functional-test

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
export BUILD_LIST=functional

echo_setup

cd /aqa-tests
./get.sh -t /aqa-tests

cd /aqa-tests/TKG

set -e

echo "Building functional test material..." 
make compile

echo "Generating make files and running the functional tests" 
make $1
set +e
