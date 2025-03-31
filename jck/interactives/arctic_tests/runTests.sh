#!/usr/bin/env bash
#
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

echo "run arctic command for TARGET=$TESTTARGET"
# Run each testcase defined for a particular target
# and save the result in a multiple formats
# where testcases will be in jck_run/arctic/tests

# java -jar Arctic.jar -c test run <testName> <testCase>
# java -jar Arctic.jar test finish <testName> <testCase> <result>
# java -jar Arctic.jar <xml|tap|jtr> save <filename>

testgrpdir=$1

if [[ -n "$testgrpdir" ]]; then
    echo "Running tests from $testgrpdir" 
else
    echo "Please provide a directory of tests to run"
    echo "runTests ./testDir"
fi

echo "Starting player in background with RMI..."
java -Darctic.logLevel=TRACE -jar build/jars/Arctic.jar -p &
rc=$?
if [[ $rc -ne 0 ]]; then
   echo "Unable to start Arctic player, rc=$rc"
   exit $rc
fi

# Allow 3 seconds for RMI server to start...
sleep 3

testdir=$1
testgroup="mytests"
testcase="test1"

java -jar build/jars/Arctic.jar -c test start "${testgroup}" "${testcase}"
rc=$?
if [[ $rc -ne 0 ]]; then
   echo "Unable to start playback for testcase ${testgroup}/${testcase}, rc=$rc"
   exit $rc
fi

sleep 3
result=$(java -jar build/jars/Arctic.jar -c test list ${testgroup}/${testcase})
rc=$?
status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
echo "==>" $status
while [[ $rc -eq 0 ]] && { [[ "$status" == "RUNNING" ]] || [[ "$status" == "STARTING" ]]; };
do
  sleep 3
  result=$(java -jar build/jars/Arctic.jar -c test list ${testgroup}/${testcase})
  rc=$?
  status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
  echo "==>" $status
done

echo "Terminating Arctic CLI..."
java -jar build/jars/Arctic.jar -c terminate

echo "Completed playback of ${testgroup}/${testcase} status: ${status}"
