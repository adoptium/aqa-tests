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
#Set up Java to be used by the kafka test
echo_setup

# Initial command to trigger the execution of kafka test
set -e
echo "Building kafka  using gradle"
./gradlew -q jar

echo "Kafka Build - Completed"

echo "Running (ALL) Kafka tests :"

./gradlew -q test
set +e
echo "Kafka tests - Completed:"