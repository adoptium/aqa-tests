#!/bin/bash
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

source $(dirname "$0")/test_base_functions.sh
# Set up Java to be used by the openliberty test
echo_setup

cd dev

TEST_TARGET="${1:-smoke}"

set -e
#Build all projects and create the open-liberty image
./gradlew -q cnf:initialize
./gradlew -q releaseNeeded
echo "Build projects and create images done"

if [ "$TEST_TARGET" = "full" ]; then
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
	echo "Test results copied"
	set +e
else
	LIBERTY_HOME="build.image/wlp"

	cleanup() {
		"${LIBERTY_HOME}/bin/server" stop defaultServer || true
	}
	trap cleanup EXIT

	echo "Starting OpenLiberty"
	"${LIBERTY_HOME}/bin/server" start defaultServer

	echo "Waiting for OpenLiberty to respond at http://localhost:9080/"
	if timeout "${STARTUP_TIMEOUT:-180}" bash -c "until curl -sf 'http://localhost:9080/' >/dev/null; do sleep 3; done"; then
		echo "OpenLiberty startup verification PASSED"
		exit 0
	else
		echo "OpenLiberty startup verification FAILED"
		find "${LIBERTY_HOME}/usr/servers/defaultServer/logs" -name '*.log' -exec tail -n 100 {} \; || true
		exit 1
	fi
fi
