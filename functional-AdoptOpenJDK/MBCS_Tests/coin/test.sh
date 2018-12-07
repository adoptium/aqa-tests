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

. ${BASE}/../data/test_${FULLLANG}
CP="-cp ${BASE}/coin.jar"

java ${CP} SwitchTest > SwitchTest.code
java SwitchTestCode > SwitchTest.log 2>&1

java ${CP} BinaryIntegralTest > BinaryIntegralTest.log

java ${CP} ExceptionTest > ExceptionTest.code
java ExceptionTestCode > ExceptionTest.log 2>&1

java ${CP} DiamondTest > DiamondTest.code
java DiamondTestCode > DiamondTest.log 2>&1

java ${CP} TryWithResourcesTest > TryWithResourcesTest.code
java TryWithResourcesTestCode > TryWithResourcesTest.log 2>&1

java ${CP} SimplifiedVarargsTest > SimplifiedVarargsTest.code
java SimplifiedVarargsTestCode > SimplifiedVarargsTest.log 2>&1

