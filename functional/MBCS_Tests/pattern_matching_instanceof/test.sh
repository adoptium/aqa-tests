#!/bin/sh
################################################################################
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
################################################################################

OS=`uname`
LOC=`locale charmap`
FULLLANG=${OS}_${LANG%.*}.${LOC}

BASE=`dirname $0`
CP=${BASE}/pattern_matching_instanceof.jar
CP_junit=${BASE}/junit4.jar

. ${BASE}/check_env_unix.sh

${JAVA_BIN}/java ${JAVA_OPTIONS} -cp ${CP} GenerateTestSource ${TEST_STRINGS} > PatternMatchingInstanceOfTest.java

${JAVA_BIN}/javac ${JAVAC_OPTIONS} -cp ${CP_junit} PatternMatchingInstanceOfTest.java

${JAVA_BIN}/java -cp ${CP_junit}:. junit.textui.TestRunner PatternMatchingInstanceOfTest

RESULT=$?
exit ${RESULT}

