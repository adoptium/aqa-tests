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
export BASE
CP="-cp ${BASE}/unicode.jar"
. ${BASE}/set_variable.sh

${JAVA_BIN}/java ${CP} UnicodeChecker        2>err1.txt
cat err1.txt
${JAVA_BIN}/java ${CP} UnicodeBlockChecker   2>err2.txt
cat err2.txt
${JAVA_BIN}/java ${CP} UnicodeScriptChecker  2>err3.txt
cat err3.txt
${JAVA_BIN}/java ${CP} UnicodeScriptChecker3 2>err4.txt
cat err4.txt
${JAVA_BIN}/java ${CP} NormalizerTest        2>err5.txt
cat err5.txt

if [ -s err1.txt ] || [ -s err2.txt ] || [ -s err3.txt ] || [ -s err4.txt ] || [ -s err5.txt ]; then
  echo Test Failed
  exit 1
else
  echo Test Passed
  exit 0
fi
