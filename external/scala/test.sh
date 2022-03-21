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
# Set up Java to be used by the scala test
echo_setup

TEST_SUITE=$1
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"
#begin scala test
set -e

echo "Try to echo Scala version by using sbt \"scala -version\"" && \
sbt -Dsbt.log.noformat=true "scala -version"

echo "Begin to execute Scala test with cmd: sbt \"partest $TEST_SUITE\"" && \
sbt -Dsbt.log.noformat=true "partest --terse $TEST_SUITE"

set +e