#!/bin/bash

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

#sh profile.sh ${WAS_HOST} ${MEASURE_TIME} ${WORKDIR}

# For profiling, make sure to run STAF or Jenkins Daemon with root permissions
# For tprof, we expect `setrunenv` file to be sourced somewhere in the bash startup scripts
# so that we have all the libraries and other things available in the right paths
# For perf, make sure that it's installed on the machine
 
WAS_HOST=$1
PROFILE_TIME=$2
SERVER_WORKDIR=$3

#TODO: Add ability to set Perf variables from the launcher such as events and sampling period
PERF_EVENTS="cycles,instructions,cache-misses,cache-references,branch-misses,branches,context-switches,page-faults,minor-faults,major-faults,alignment-faults,emulation-faults"
PERF_SAMPLING_PERIOD="1000"

echo "WAS_HOST:${WAS_HOST} PROFILE_TIME:${PROFILE_TIME} WORKDIR:${WORKDIR}"
echo "PERF_EVENTS:${PERF_EVENTS} PERF_SAMPLING_PERIOD:${PERF_SAMPLING_PERIOD}"

if [ "$PROFILING_TOOL" = "jprof tprof" ]; then
	echo "Running jprof tprof"
	STAF ${WAS_HOST} PROCESS START SHELL COMMAND run.tprof -s 0 -r ${PROFILE_TIME} WORKDIR ${SERVER_WORKDIR} STDERRTOSTDOUT RETURNSTDOUT WAIT
elif [[ "${PROFILING_TOOL}" = "jprof scs" || "${PROFILING_TOOL}" = "jprof callflow" || "${PROFILING_TOOL}" = "jprof calltree" || "${PROFILING_TOOL}" = "jprof rtarcf" ]]; then
	echo "Running $PROFILING_TOOL"
	STAF ${WAS_HOST} PROCESS START SHELL COMMAND "rtdriver -l -c start -c end ${PROFILE_TIME}" WORKDIR ${SERVER_WORKDIR} STDERRTOSTDOUT RETURNSTDOUT WAIT
elif [ "$PROFILING_TOOL" = "perf stat" ]; then
	echo "Running perf stat"
	STAF ${WAS_HOST} PROCESS START SHELL COMMAND perf stat -o ${SERVER_WORKDIR}/perf_stat.txt -e ${PERF_EVENTS} -- sleep ${PROFILE_TIME} WORKDIR ${SERVER_WORKDIR} STDERRTOSTDOUT RETURNSTDOUT WAIT
elif [ "$PROFILING_TOOL" = "perf record" ]; then
	echo "Running perf record"
	STAF ${WAS_HOST} PROCESS START SHELL COMMAND perf record -o ${SERVER_WORKDIR}/perf.data -e ${PERF_EVENTS} -m 4096 -r 1 -g -c ${PERF_SAMPLING_PERIOD} --call-graph dwarf -- sleep ${PROFILE_TIME} WORKDIR ${SERVER_WORKDIR} STDERRTOSTDOUT RETURNSTDOUT WAIT
fi    
	

