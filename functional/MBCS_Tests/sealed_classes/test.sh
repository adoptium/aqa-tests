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
BASE=`dirname $0`
OS=`uname`
LOC=`locale charmap`
FULLLANG=${OS}_${LANG%.*}.${LOC}

. ${BASE}/check_env_unix.sh

CP=${BASE}/sealed_classes.jar
CP_junit=${BASE}/junit4.jar

${JAVA_BIN}/java -cp ${CP} GenerateTestSource ${TEST_STRINGS}

${JAVA_BIN}/javac -Xstdout compile_sbcs_err.out -cp ${CP_junit} SealedClassSbcsTest.java
${JAVA_BIN}/java -cp ${CP} FixFile compile_sbcs_err.out SealedClassSbcsTest SealedClassCETest ${TEST_STRINGS}
${JAVA_BIN}/javac -Xstdout compile_err.out -cp ${CP_junit} SealedClassCETest.java
diff compile_err.out expected_result.txt

RESULT=$?
if [ ${RESULT} != 0 ]; then
  exit ${RESULT} 
fi

${JAVA_BIN}/javac -cp ${CP_junit} SealedClassTest.java
${JAVA_BIN}/java -cp ${CP_junit}:. junit.textui.TestRunner SealedClassTest

RESULT=$?
exit ${RESULT}
