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
# workaround of AIX7 defect
LOC2=`echo ${LANG%.*} | LANG=C tr 'A-Z_' 'a-z\-'`
FULLLANG=${OS}_${LANG%.*}.${LOC}

BASE=`dirname $0`
export CLASSPATH=${BASE}/jaxp14_11.jar
CHARMAP=${FULLLANG}
SOURCE="${CHARMAP}.txt"
OUTPUT=output.html

. ${BASE}/check_env_unix.sh
${JAVA_BIN}/java XSLTTest ${BASE}/drinks_${LOC2}.xml ${BASE}/drinks_${LOC2}.xsl > ${OUTPUT}
diff ${BASE}/expected_${LOC2}.html ${OUTPUT} > /dev/null 2>&1
RESULT=$?
exit ${RESULT}
