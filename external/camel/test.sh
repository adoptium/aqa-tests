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
# Set up Java to be used by the camel test
echo_setup

# See https://camel.apache.org/camel-quarkus/latest/contributor-guide.html
# for advise to set MAVEN_OPTS to avoid https://cwiki.apache.org/confluence/display/MAVEN/OutOfMemoryError
export MAVEN_OPTS="-Xmx1g"

#jdk17
excludeProject="-pl !:camel-quarkus-support-spring,\
!:camel-quarkus-support-xstream-deployment,\
!:camel-quarkus-support-xalan,\
!:camel-quarkus-support-xalan-deployment,\
!:camel-quarkus-support-mongodb-deployment,\
!:camel-quarkus-support-spring-deployment"

TEST_TARGET="${1:-smoke}"

set -e
if [ "$TEST_TARGET" = "full" ]; then
	echo "Compile and run camel tests"
	./mvnw --batch-mode --fail-at-end $excludeProject clean install -DallTests
	echo "Build camel completed"

	find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
	echo "Test results copied"
	set +e
else
	./mvnw --batch-mode -pl extensions-core --also-make compile -DskipTests
	set +e
	echo "Camel build completed"
fi
