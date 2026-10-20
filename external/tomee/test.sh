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
#Set up Java to be used by the tomee test
echo_setup

TEST_OPTIONS=$1
[ "$TEST_OPTIONS" = "full" ] && TEST_OPTIONS=""

set -e
echo "Build TomEE without running test"
mvn --batch-mode -Pquick -Dsurefire.useFile=false -DdisableXmlReport=true -DuniqueVersion=false -ff -Dassemble -DskipTests -DfailIfNoTests=false clean install
set +e
echo "Build TomEE completed"

if [ "$TEST_OPTIONS" = "smoke" ]; then
	TOMEE_HOME=$(find . -name 'catalina.sh' | head -1 | xargs -I{} dirname "{}" | xargs -I{} dirname "{}")
	if [ -z "${TOMEE_HOME}" ]; then
		echo "ERROR: Could not locate TomEE home directory (catalina.sh not found after build)"
		exit 1
	fi

	cleanup() {
		"${TOMEE_HOME}/bin/shutdown.sh" || true
	}
	trap cleanup EXIT

	echo "Starting TomEE"
	"${TOMEE_HOME}/bin/startup.sh"

	echo "Waiting for TomEE to respond at http://localhost:8080/"
	if timeout "${STARTUP_TIMEOUT:-180}" bash -c "until curl -sf 'http://localhost:8080/' >/dev/null; do sleep 3; done"; then
		echo "TomEE startup verification PASSED"
		test_exit_code=0
	else
		echo "TomEE startup verification FAILED"
		find "${TOMEE_HOME}/logs" -name '*.log' -exec tail -n 100 {} \; || true
		test_exit_code=1
	fi
	exit $test_exit_code
else
	echo "Run Microprofile TCK"
	cd tck/microprofile-tck
	mvn --batch-mode test -Denforcer.fail=false $TEST_OPTIONS
	test_exit_code=$?
	find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
	exit $test_exit_code
fi
