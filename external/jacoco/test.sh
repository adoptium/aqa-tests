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
# Set up Java to be used by the jacoco-test
echo_setup

cd org.jacoco.build

excludeProject=""

if [ "$JDK_VERSION" == "17" ]; then
	excludeProject="-pl !:org.jacoco.core.test.validation.groovy"
fi

TEST_OPTIONS=$1
[ "$TEST_OPTIONS" = "full" ] && TEST_OPTIONS=""

set -e
echo "Building jacoco"
mvn --batch-mode $excludeProject install -DskipTests
set +e
echo "Jacoco build completed"

if [ "$TEST_OPTIONS" = "smoke" ]; then
	echo "Probing JaCoCo version"
	JACOCO_CLI_JAR=$(find ../org.jacoco.cli/target -name 'org.jacoco.cli-*-nodeps.jar' | head -1)
	if [ -z "$JACOCO_CLI_JAR" ]; then
		echo "ERROR: Could not locate org.jacoco.cli-*-nodeps.jar under ../org.jacoco.cli/target"
		exit 1
	fi
	java -jar "$JACOCO_CLI_JAR" version
	test_exit_code=$?
	exit $test_exit_code
else
	echo "Compile and run jacoco tests"
	mvn --batch-mode --fail-at-end $excludeProject clean verify $TEST_OPTIONS
	test_exit_code=$?
	echo "Build jacoco completed"
	find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
	echo "Test results copied"
	exit $test_exit_code
fi
