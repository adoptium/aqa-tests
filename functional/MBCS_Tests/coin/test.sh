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
CP="-cp ${BASE}/coin.jar"

mkdir tmp
cd tmp

${JAVA_BIN}/java ${JAVA_OPTIONS} ${CP} SwitchTest > SwitchTest.code
${JAVA_BIN}/java ${JAVA_OPTIONS} SwitchTestCode > SwitchTest.log 2>&1

${JAVA_BIN}/java ${JAVA_OPTIONS} ${CP} BinaryIntegralTest > BinaryIntegralTest.log

${JAVA_BIN}/java ${JAVA_OPTIONS} ${CP} ExceptionTest > ExceptionTest.code
${JAVA_BIN}/java ${JAVA_OPTIONS} ExceptionTestCode > ExceptionTest.log 2>&1

${JAVA_BIN}/java ${JAVA_OPTIONS} ${CP} DiamondTest > DiamondTest.code
${JAVA_BIN}/java ${JAVA_OPTIONS} DiamondTestCode > DiamondTest.log 2>&1

${JAVA_BIN}/java ${JAVA_OPTIONS} ${CP} TryWithResourcesTest > TryWithResourcesTest.code
${JAVA_BIN}/java ${JAVA_OPTIONS} TryWithResourcesTestCode > TryWithResourcesTest.log 2>&1

${JAVA_BIN}/java ${JAVA_OPTIONS} ${CP} SimplifiedVarargsTest > SimplifiedVarargsTest.code
${JAVA_BIN}/java ${JAVA_OPTIONS} SimplifiedVarargsTestCode > SimplifiedVarargsTest.log 2>&1
 
LANG=${LANG} perl ${BASE}/resultCheck.pl
