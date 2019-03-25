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

CP="-cp ${BASE}/urlclassloader.jar"

${JAVA_BIN}/java ${CP} UrlClassClose $TEST_STRINGS > output.txt 2>&1

diff output.txt ${BASE}/expected/${FULLLANG}.txt > diff.txt
RESULT=$?
exit ${RESULT}
