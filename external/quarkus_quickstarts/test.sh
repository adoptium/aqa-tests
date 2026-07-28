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

source $(dirname "$0")/test_base_functions.sh
# Set up Java to be used by the quarkus quickstarts test
echo_setup
excludeProject="-pl !:hibernate-orm-multi-tenancy-quickstart,\
!:hibernate-search-orm-elasticsearch-quickstart,\
!:mqtt-quickstart,\
!:rabbitmq-quickstart-processor,\
!:redis-quickstart,\
!:security-jdbc-quickstart,\
!:security-openid-connect-multi-tenancy-quickstart"

if [ "$JDK_VERSION" == "17" ]; then
	excludeProject+=",!:kogito-quickstart"
fi

export MAVEN_OPTS="-Xmx1g"

TEST_TARGET="${1:-smoke}"

set -e
echo "Building quarkus_quickstarts"
mvn --batch-mode $excludeProject compile -DskipTests
set +e
echo "Quarkus quickstarts build completed"

if [ "$TEST_TARGET" = "full" ]; then
	echo "Compile and run quarkus_quickstarts tests"
	mvn --batch-mode $excludeProject clean install
	test_exit_code=$?
	echo "Build quarkus_quickstarts completed"
	find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
	echo "Test results copied"
	exit $test_exit_code
else
	echo "Building getting-started quickstart"
	mvn --batch-mode -pl getting-started package -DskipTests

	cleanup() {
		kill "${probe_pid}" 2>/dev/null || true
	}
	trap cleanup EXIT

	echo "Starting getting-started quickstart"
	java -jar getting-started/target/quarkus-app/quarkus-run.jar &
	probe_pid=$!

	echo "Waiting for getting-started to respond at http://localhost:8080/hello"
	if timeout "${STARTUP_TIMEOUT:-180}" bash -c "until curl -sf 'http://localhost:8080/hello' >/dev/null; do sleep 2; done"; then
		echo "Quarkus quickstarts startup verification PASSED"
		test_exit_code=0
	else
		echo "Quarkus quickstarts startup verification FAILED"
		test_exit_code=1
	fi
	exit $test_exit_code
fi
