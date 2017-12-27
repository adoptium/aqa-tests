#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

RESULTS_DIR=$1

LOGFILE=${RESULTS_DIR}/top.out

while true;
	do
                jpid=`ps -ef | grep "IdleMicroBenchmark" | grep -v runIdleTest.sh | grep -v grep | awk '{ print $2}'`

                if [ "$jpid" == "" ]; then
                        break;
                fi

		top -b -n 1 -p $jpid

        done 2>&1 | tee -a ${LOGFILE}

#./log_parse.sh logdir topoutput stdout
chmod a+x log_parse.sh
./log_parse.sh ${RESULTS_DIR} ${LOGFILE} ${RESULTS_DIR}/std.out

set +e

