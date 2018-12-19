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
export CLASSPATH=${BASE}/regex.jar
CHARMAP=${FULLLANG}
SOURCE="${CHARMAP}.txt"
OUTPUT=result.txt

PATH=${JAVA_BIN}:$PATH
export PATH

. ${BASE}/../data/test_${FULLLANG}
${SHELL} ${BASE}/test_${FULLLANG} ${BASE}/${FULLLANG}.txt 2>./error.log < ${BASE}/answer.txt > ${OUTPUT}

diff ${OUTPUT} ${BASE}/expected_${FULLLANG} > /dev/null 2>&1
RESULT=$?
exit ${RESULT}
