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
CP="-cp ${BASE}/StAX.jar"

COUNTRY=`echo ${LANG%%_*} | LANG=C tr '[A-Z]' '[a-z]'`

if [ -f read_cursor.html ] ; then
	rm -f read_cursor.html
fi

if [ -f read_event.html ] ; then
	rm -f read_event.html
fi


if [ -f write_cursor.xml ] ; then
	rm -f write_cursor.xml
fi

if [ -f write_event.xml ] ; then
	rm -f write_event.xml
fi

java ${CP} StAXReadCursor ${BASE}/data/drinks_${COUNTRY}.xml >read_cursor.html 2>&1
java ${CP} StAXReadEveIter ${BASE}/data/drinks_${COUNTRY}.xml >read_event.html 2>&1
java ${CP} StAXWriteCursor $TEST_STRINGS
java ${CP} StAXWriteEveIter $TEST_STRINGS
