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
#Set up Java to be used by the wildfly test
echo_setup

TEST_TARGET="${1:-smoke}"

set -e
echo "Building wildfly using maven, by invoking build.sh"
./build.sh
echo "Wildfly Build - Completed"

if [ "$TEST_TARGET" = "full" ]; then
	echo "Running (ALL) wildfly tests :"

	echo "Setting user to blank"
	printenv
	export USER=""
	echo "Printing Environment Variables"
	printenv

	#jdk8,11
	excludeProject="-pl !:wildfly-ts-integ-elytron"

	if [ "$JDK_VERSION" == "11" ]; then
		excludeProject+=",!:wildfly-ts-integ-basic"
	fi

	if [ "$JDK_VERSION" == "17" ]; then
		excludeProject="-pl !:wildfly-iiop-openjdk"
	fi

	set +e
	./mvnw --batch-mode --fail-at-end $excludeProject install -DallTests
else
	WILDFLY_HOME=$(find . -name 'standalone.sh' | head -1 | xargs dirname | xargs dirname)

	cleanup() {
		"${WILDFLY_HOME}/bin/jboss-cli.sh" --connect command=:shutdown || true
	}
	trap cleanup EXIT

	echo "Starting WildFly"
	"${WILDFLY_HOME}/bin/standalone.sh" &

	echo "Waiting for WildFly to respond at ${WILDFLY_URL:-http://localhost:9990/}"
	if timeout "${STARTUP_TIMEOUT:-180}" bash -c "until curl -sf '${WILDFLY_URL:-http://localhost:9990/}' >/dev/null; do sleep 3; done"; then
		echo "WildFly startup verification PASSED"
		exit 0
	else
		echo "WildFly startup verification FAILED"
		find "${WILDFLY_HOME}/standalone/log" -name '*.log' -exec tail -n 100 {} \; || true
		exit 1
	fi
fi
