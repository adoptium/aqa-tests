#/bin/bash
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
#

if [ -d /java/jre/bin ];then
	echo "Using mounted Java8"
	export JAVA_BIN=/java/jre/bin
	export JAVA_HOME=/java
	export PATH=$JAVA_BIN:$PATH
elif [ -d /java/bin ]; then
	echo "Using mounted Java"
	export JAVA_BIN=/java/bin
	export JAVA_HOME=/java
	export PATH=$JAVA_BIN:$PATH
else
	echo "Using docker image default Java"
	java_path=$(type -p java)
	suffix="/java"
	java_root=${java_path%$suffix}
	export JAVA_BIN="$java_root"
	echo "JAVA_BIN is: $JAVA_BIN"
	export JAVA_HOME="${java_root%/bin}"
fi

java -version
TEST_OPTIONS=$1
cd ${THORNTAIL_HOME}/

export OPENJ9_JAVA_OPTIONS="-Xmx1g"

#Build Thorntail 
cd ${THORNTAIL_HOME}/thorntail/ 
set -e
mvn --batch-mode clean install -Pmicroprofile-tck -Dmaven.test.skip=true
set +e
echo "build finished"

echo "microprofile tck testsuite start"
mvn --batch-mode test -Pmicroprofile-tck $TEST_OPTIONS
test_exit_code=$?
find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
exit $test_exit_code