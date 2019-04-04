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
cd ${OPENLIBERTY_HOME}/open-liberty/dev

#Build all projects and create the open-liberty image
./gradlew -q cnf:initialize
./gradlew -q releaseNeeded

echo "Build projects and create images done"

#Following are not enabled tests, may need to enable later
#com.ibm.ws.microprofile.reactive.streams.operators_fat_tck
#com.ibm.ws.concurrent.mp_fat_tck
#com.ibm.ws.microprofile.config_git_fat_tck
#com.ibm.ws.security.mp.jwt_fat_tck
#com.ibm.ws.microprofile.faulttolerance_git_fat_tck

#Exclude Metrics TCK
#Start MicroProfile Metrics TCK
./gradlew -q com.ibm.ws.microprofile.metrics_fat_tck:clean
./gradlew -q com.ibm.ws.microprofile.metrics_fat_tck:buildandrun

#Start MicroProfile Config TCK
./gradlew -q com.ibm.ws.microprofile.config.1.4_fat_tck:clean
./gradlew -q com.ibm.ws.microprofile.config.1.4_fat_tck:buildandrun

#Start MicroProfile FaultTolerance TCK
./gradlew -q com.ibm.ws.microprofile.faulttolerance_fat_tck:clean
./gradlew -q com.ibm.ws.microprofile.faulttolerance_fat_tck:buildandrun

#Start MicroProfile Rest Client TCK
./gradlew -q com.ibm.ws.microprofile.rest.client_fat_tck:clean
./gradlew -q com.ibm.ws.microprofile.rest.client_fat_tck:buildandrun

#Start MicroProfile OpenAPI TCK
./gradlew -q com.ibm.ws.microprofile.openapi_fat_tck:clean
./gradlew -q com.ibm.ws.microprofile.openapi_fat_tck:buildandrun

find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
