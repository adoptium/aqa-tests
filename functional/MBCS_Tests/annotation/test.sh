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

cp ${BASE}/*.java .

CP="-cp ${BASE}/annotation.jar"

TS=`${JAVA_BIN}/java ${CP} CheckValidData "${TEST_STRING}"`

echo "creating source file..."
sed "s/TEST_STRING/${TS}/g" DefineAnnotation_org.java > DefineAnnotation.java
sed "s/TEST_STRING/${TS}/g"  AnnotatedTest_org.java >  AnnotatedTest.java

SDKPATH=`${JAVA_BIN}/java ${CP} SDKPath`

echo "compiling..."
${SDKPATH}/javac DefineAnnotation.java AnnotatedTest.java

echo "execute javap"
${SDKPATH}/javap AnnotatedTest > javap.txt 2>&1
diff javap.txt ${BASE}/expected/${FULLLANG}.def.txt > diff.txt

${SDKPATH}/java ${CP} SourceVersionCheck ${BASE}/expected/${FULLLANG}.pro.txt 2>> diff.txt

if [ -s diff.txt ]
then
  # failed
  exit 1
else
  # passed
  exit 0
fi
