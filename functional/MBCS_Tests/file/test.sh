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
CP="-cp ${BASE}/file.jar"

LOGFILE="test_log.txt"
DATAFILE=${FULLLANG}.tar.gz
${JAVA_BIN}/java ${CP} MimeDecoder ${BASE}/data/${DATAFILE}.base64 ${BASE}/data/${DATAFILE}
gzip -dc ${BASE}/data/${DATAFILE} | tar xf - 
if [ -e work ]; then
   rm -fr work
fi
mkdir  work

${JAVA_BIN}/java ${CP} FileOperator ${FULLLANG} work/${FULLLANG} C > ${LOGFILE} 2>&1
${JAVA_BIN}/java ${CP} FileOperator ${FULLLANG} work/${FULLLANG} R >> ${LOGFILE} 2>&1
${JAVA_BIN}/java ${CP} FileOperator ${FULLLANG} work/${FULLLANG} D >> ${LOGFILE} 2>&1

diff ${LOGFILE} ${BASE}/expected/${FULLLANG}.txt > diff.txt
RESULT=$?
exit ${RESULT}
