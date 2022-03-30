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

source $(dirname "$0")/test_base_functions.sh
# Set up Java to be used by the quarkus-test
echo_setup

# See https://github.com/quarkusio/quarkus/blob/master/CONTRIBUTING.md#frequently-asked-questions
# for advise to set MAVEN_OPTS to avoid https://cwiki.apache.org/confluence/display/MAVEN/OutOfMemoryError
export MAVEN_OPTS="-Xmx1g"

export OPENJ9_JAVA_OPTIONS="-Xmx1g"

pwd
echo "Compile and run quarkus tests"

./mvnw --batch-mode -pl '!:quarkus-documentation' clean install
test_exit_code=$?

find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
exit $test_exit_code
