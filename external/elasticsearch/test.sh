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
#Set up Java to be used by the elasticsearch test
echo_setup
TEST_OPTIONS=$1

# Initial command to trigger the execution of elasticsearch test 
set -e
echo "Building elasticsearch  using gradlew \"gradlew assemble\"" && \
./gradlew -q -g /tmp assemble \
--exclude-task :distribution:docker:buildAarch64CloudDockerImage \
--exclude-task :distribution:docker:buildAarch64CloudEssDockerImage \
--exclude-task :distribution:docker:buildAarch64DockerImage \
--exclude-task :distribution:docker:buildAarch64UbiDockerImage \
--exclude-task :distribution:docker:buildCloudDockerImage \
--exclude-task :distribution:docker:buildCloudEssDockerImage \
--exclude-task :distribution:docker:buildDockerImage \
--exclude-task :distribution:docker:buildUbiDockerImage \
--exclude-task :distribution:docker:buildAarch64IronBankDockerImage \
--exclude-task :distribution:docker:buildIronBankDockerImage

set +e
echo "Elasticsearch Build - Successful"
echo "================================"
echo ""
echo "Running elasticsearch tests :"

echo $TEST_OPTIONS

./gradlew -q -g /tmp test -Dtests.haltonfailure=false $TEST_OPTIONS
test_exit_code=$?
find ./ -type d -name 'testJunit' -exec cp -r "{}" /testResults \;
exit $test_exit_code
