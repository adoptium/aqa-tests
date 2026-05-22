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
export CLASSPATH=${BASE}/formatter.jar
OUTPUT=output.txt

. ${BASE}/check_env_unix.sh
CHARMAP=${FULLLANG}

# Get Java major version
FULL_VERSION=$(java -version 2>&1 | grep 'version' | cut -d '"' -f2)
JAVA_VERSION=${FULL_VERSION%%.*}

#_22 file is only applicable for taiwanese in AIX and Linux else it should fall back to default
if { [ "$CHARMAP" = "Linux_zh_TW.UTF-8" ] || [ "$CHARMAP" = "AIX_ZH_TW.UTF-8" ]; } && [ "$JAVA_VERSION" -ge 22 ]; then
    SOURCE="${CHARMAP}_22.txt"
else
    SOURCE="${CHARMAP}.txt"
fi

# Create the expected file name
EXP_FILE=expected_${SOURCE}

echo "invoking FormatterTest2" > ${OUTPUT}
${JAVA_BIN}/java FormatterTest2 abc${TEST_STRING} >> ${OUTPUT}
diff ${BASE}/${EXP_FILE} ${OUTPUT} > /dev/null 2>&1
RESULT=$?
exit ${RESULT}
