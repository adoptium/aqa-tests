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
#Set up Java to be used by the derby test
echo_setup
#clean previous build artifacts 
ant -f build.xml clobber
#build all
ant -f build.xml all
#create jars
ant -f build.xml buildjars

export jardir ${TEST_HOME}/jars/sane 
export tstjardir ${TEST_HOME}/tools/java 
export CLASSPATH "${jardir}/derbyrun.jar:${jardir}/derbyTesting.jar:${tstjardir}/junit.jar"
java -jar ${TEST_HOME}/jars/sane/derbyrun.jar sysinfo

set -e
#Run all tests
ant -Dderby.tests.basePort=1690 -Dderby.system.durability=test -DderbyTesting.oldReleasePath=${TEST_HOME}/jars junit-all
set +e
#Run only derbylang suite
#java -Dverbose=true -cp ${jardir}/derbyrun.jar:${jardir}/derbyTesting.jar:$tstjardir/junit.jar org.apache.derbyTesting.functionTests.harness.RunSuite #derbylang

