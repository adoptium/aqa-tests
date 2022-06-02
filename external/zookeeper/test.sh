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
#Set up Java to be used by the netty test
echo_setup

testList="-Dtest=!org.apache.zookeeper.server.quorum.QuorumPeerMainTest,!org.apache.zookeeper.server.quorum.QuorumPeerMainMultiAddressTest,!org.apache.zookeeper.ZKUtilTest,!org.apache.zookeeper.server.util.RequestPathMetricsCollectorTest,!org.apache.zookeeper.test.ReadOnlyModeTest,!org.apache.zookeeper.server.NettyServerCnxnTest,!org.apache.zookeeper.server.ZooKeeperServerMainTest,!org.apache.zookeeper.server.quorum.EagerACLFilterTest,!org.apache.zookeeper.server.quorum.Zab1_0Test,!org.apache.zookeeper.server.quorum.UnifiedServerSocketTest,!org.apache.zookeeper.server.quorum.CommitProcessorConcurrencyTest,!org.apache.zookeeper.server.util.JvmPauseMonitorTest"

echo "Compile and run zookeeper tests"
echo mvn test --batch-mode --fail-at-end $testList
mvn test --batch-mode --fail-at-end $testList
test_exit_code=$?
echo "Build zookeeper completed"

find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
echo "Test results copied"

exit $test_exit_code