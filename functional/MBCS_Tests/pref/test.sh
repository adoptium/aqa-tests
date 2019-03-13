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
echo $FULLLANG

BASE=`dirname $0`
CLASSPATH=${BASE}/pref.jar:.
export CLASSPATH

. ${BASE}/check_env_unix.sh

OUTPUT=${PWD}/result_${FULLLANG}.txt
rm -f $OUTPUT

echo "registering $TEST_STRING ..." >> $OUTPUT
${JAVA_BIN}/java PrefTest $TEST_STRING >> $OUTPUT

echo "registered string" >> $OUTPUT
${JAVA_BIN}/java PrefTest -q >> $OUTPUT

echo "removing ..." >> $OUTPUT
${JAVA_BIN}/java PrefTest -r >> $OUTPUT
${JAVA_BIN}/java PrefTest -q >> $OUTPUT

perl $BASE/tap_compare.pl $OUTPUT ${BASE}/expected_${FULLLANG}.txt
RESULT=$?
exit ${RESULT}
