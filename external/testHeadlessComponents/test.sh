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

source $(dirname "$0")/test_base_functions.sh
#Set up Java to be used by the wildfly test
echo_setup

# Replace the following with the initial command lines that trigger execution of your test
set -e
echo "Building wildfly  using maven , by invoking build.sh" && \
./build.sh

echo "Wildfly Build - Completed"

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

./mvnw --batch-mode --fail-at-end $excludeProject install -DallTests
set +e