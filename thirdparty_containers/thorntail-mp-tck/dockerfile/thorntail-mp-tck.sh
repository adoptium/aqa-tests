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
	java -version
elif [ -d /java/bin ]; then
	echo "Using mounted Java"
	export JAVA_BIN=/java/bin
	export JAVA_HOME=/java
	export PATH=$JAVA_BIN:$PATH
	java -version
else
	echo "Using docker image default Java"
	java_path=$(type -p java)
	suffix="/java"
	java_root=${java_path%$suffix}
	export JAVA_BIN="$java_root"
	echo "JAVA_BIN is: $JAVA_BIN"
	$JAVA_BIN/java -version
	export JAVA_HOME="${java_root%/bin}"
fi

cd ${THORNTAIL_HOME}/

#Build Thorntail 
cd ${THORNTAIL_HOME}/thorntail/ 
mvn clean install

#Start MicroProfile Metrics TCK
cd ${THORNTAIL_HOME}/thorntail/testsuite/testsuite-microprofile-metrics
mvn integration-test

#Start MicroProfile Fault-Tolerance TCK
cd ${THORNTAIL_HOME}/thorntail/testsuite/testsuite-microprofile-fault-tolerance
mvn integration-test

#Start MicroProfile Rest Client TCK
cd ${THORNTAIL_HOME}/thorntail/testsuite/testsuite-microprofile-restclient 
mvn integration-test

#Start MicroProfile OpenAPI TCK
cd ${THORNTAIL_HOME}/thorntail/testsuite/testsuite-microprofile-openapi
mvn integration-test

#Start MicroProfile JWT TCK
cd ${THORNTAIL_HOME}/thorntail/testsuite/testsuite-microprofile-jwt
mvn integration-test
