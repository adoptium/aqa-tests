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
echo $BASE
export CLASSPATH=${BASE}/Compiler.jar:.

CHARMAP=${FULLLANG}
SOURCE="${CHARMAP}.txt"
OUTPUT=$PWD/output.txt

rm -rf tmp
mkdir tmp
cd tmp
. ${BASE}/check_env_unix.sh
TS=`${JAVA_BIN}/java CheckValidData "${TEST_STRING}"`

JAVAFILE=${TS}.java
sed "s/TEST_STRING/${TS}/g" ${BASE}/class_org.java > ${JAVAFILE}
echo "lauching CompilerTest1..." > ${OUTPUT}
${JAVA_BIN}/java CompilerTest1 ${JAVAFILE} >> ${OUTPUT}
cd ..

diff ${BASE}/expected_${SOURCE} ${OUTPUT} > /dev/null 2>&1
RESULT=$?
exit ${RESULT}
