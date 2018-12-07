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

# This script doesn't work on Java9, 10 nor 11.

BASE=`dirname $0`

OS=`uname`
LOC=`locale charmap`
FULLLANG=${OS}_${LANG%.*}.${LOC}

. ${BASE}/data/setjdbc41_${FULLLANG}
. ${BASE}/../data/test_${FULLLANG}

if [ "x${JDBC41_TABLE_NAME}" = "x" ] ; then
   echo "Environment variable JDBC41_TABLE_NAME is not defined"
   exit 1
fi

if [ "x${JDBC41_CNAME}" = "x" ] ; then
   echo "Environment variable JDBC41_CNAME is not defined"
   exit 1
fi

DERBY_JAR=${BASE}/derby/db-derby-10.14.2.0-lib/lib/derby.jar
CP="-cp ${BASE}/jdbc41.jar:${DERBY_JAR}"

TEST_STRING=`echo "$TEST_STRING" | sed -e 's/://g'`
TEST_STRINGS=`echo "$TEST_STRINGS" | sed -e 's/://g'`

java ${CP} jdbc41autoclose ${TEST_STRINGS} > out_auto.txt 2>&1

java ${CP} jdbc41RowSetProvider > out_row.txt 2>&1

java ${CP} jdbc41droptb > out_drop.txt 2>&1

RESULT=0
diff out_auto.txt ${BASE}/expected/${FULLLANG}_auto.txt > diff.txt
ret=$?
if [ $ret -ne 0 ] ;then
    RESULT=1
fi
diff out_row.txt ${BASE}/expected/${FULLLANG}_row.txt >> diff.txt
ret=$?
if [ $ret -ne 0 ] ;then
    RESULT=1
fi
diff out_drop.txt ${BASE}/expected/drop.txt >> diff.txt
ret=$?
if [ $ret -ne 0 ] ;then
    RESULT=1
fi

exit ${RESULT}
