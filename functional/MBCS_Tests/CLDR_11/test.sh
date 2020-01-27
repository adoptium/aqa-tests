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
OS=`uname`
LOC=`locale charmap`
FULLLANG=${OS}_${LANG%.*}.${LOC}

. ${BASE}/check_env_unix.sh
LANGTAG=`${JAVA_BIN}/java -cp ${BASE}/CLDR_11.jar PrintLanguageTag`
export LANGTAG
echo "Running ..."
${JAVA_BIN}/java -cp ${BASE}/CLDR_11.jar CheckZHTW
if [ "$?" = "1" ]; then
    export USE_ZHTW_WORKAROUND="true"
fi

${JAVA_BIN}/java -cp ${BASE}/CLDR_11.jar MainStarter

${JAVA_BIN}/java -cp ${BASE}/CLDR_11.jar CLDR11

if [ "$USE_ZHTW_WORKAROUND" = "true" ]; then
    for i in expected_TimeZoneTestA-zh-TW-CLDR.log TimeZoneTestA-zh-TW-JRE.log
    do
        cp ${i} ${i}.orig
        ${JAVA_BIN}/java -cp ${BASE}/CLDR_11.jar ModifyZHTW ${i}
    done
fi

perl $BASE/tap_compare.pl
RESULT=$?
exit ${RESULT}
