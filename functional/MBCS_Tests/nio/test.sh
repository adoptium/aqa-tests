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
export CLASSPATH=${BASE}/nio.jar
CHARMAP=${FULLLANG}
SOURCE="${CHARMAP}.txt"

if [ "$FULLLANG" = "AIX_Zh_TW.big5" ]; then
  LOC="IBM-950";
elif [ "$FULLLANG" = "AIX_Ja_JP.IBM-943" ]; then
  LOC="IBM-943C";
fi

echo "system code page is " ${LOC}

. ${BASE}/check_env_unix.sh
echo "invoking ReadWriteTest..." 
${JAVA_BIN}/java ReadWriteTest ${BASE}/expected_${FULLLANG}.txt ${LOC} converted.txt ${LOC} > log 2>&1
diff ${BASE}/expected_${FULLLANG}.txt ./converted.txt > diff.txt 2>&1
RESULT=$?
if [ $RESULT = 0 ]; then
  for i in `ls ${BASE}/expected_${FULLLANG}-*.txt 2>/dev/null`
  do
    ${JAVA_BIN}/java ReadWriteTest ${i} ${LOC} converted.txt ${LOC} >> log 2>&1
    diff ${i} ./converted.txt >> diff.txt 2>&1
    RC=$?
    if [ $RC -gt $RESULT ]; then
      RESULT=$RC
    fi
  done
fi
exit ${RESULT}
