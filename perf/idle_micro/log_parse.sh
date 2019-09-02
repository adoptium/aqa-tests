#!/usr/bin/env bash
#
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
#

set -o pipefail

function get_usage
{
	inf=$1
	outf=$2
	grep -e "^top" -e "java" $inf | grep -vE "Path|<vmarg" | \
		awk '{
			if (match($1, "top")) {
				split($3, ts, ":");
			} else {
				if (match($6, "m") != 0) {
					split($6, memv, "m");
					printf("%s:%s:%s %s %s %s\n", ts[1], ts[2], ts[3], $1, memv[1], $9);
				} if (match($6, "g") != 0) {
					split($6, memv, "g");
					printf("%s:%s:%s %s %0.2f %s\n", ts[1], ts[2], ts[3], $1, memv[1]*1024, $9);	# converting gb to mb
				} else {
					printf("%s:%s:%s %s %0.2f %s\n", ts[1], ts[2], ts[3], $1, $6/1024, $9);		# converting kb to mb
				}
			}
		}' > $outf
}


function extract_from_usage_data
{
        inputfile=$1
        outputfile=$2
        start_time=$3
        end_time=$4

        start_line="$(grep -n -m 1 "$start_time"  $inputfile |sed  's/\([0-9]*\).*/\1/')";
        end_line="$(grep -n "$end_time" $inputfile | tail -1 | cut -d: -f1)";
        sed "$start_line,$end_line!d" $inputfile >> $outputfile

}

# Read the command line inputs
RESULTS_DIR=$1
top_output=$2
std_out=$3

get_usage $top_output ${RESULTS_DIR}/usage.dat

active_start=`cat $std_out | grep "Starting: Application inducing load" | cut -d' ' -f 2`
active_end=`cat $std_out | grep "Complete: Application inducing load" | cut -d' ' -f 2`

idle_start=`cat $std_out | grep "Starting: Application Long Idle" | cut -d' ' -f 2`
idle_end=`cat $std_out | grep "Complete: Application Long Idle" | cut -d' ' -f 2`

echo "active start: $active_start"
echo "active end: $active_end"
echo "idle start: $idle_start"
echo "idle end: $idle_end"


extract_from_usage_data ${RESULTS_DIR}/usage.dat ${RESULTS_DIR}/active.dat $active_start $active_end
extract_from_usage_data ${RESULTS_DIR}/usage.dat ${RESULTS_DIR}/idle.dat $idle_start $idle_end

active_max=$(awk -v max=0 '{if($3>max){max=$3}}END{print max}' ${RESULTS_DIR}/active.dat)
idle_min=$(awk -v min=99999999999 '{if($3<min){min=$3}}END{print min}' ${RESULTS_DIR}/idle.dat)

echo "ACTIVE MAX: $active_max"
echo "IDLE MIN: $idle_min"

if (( $(echo "$idle_min $active_max" | awk '{print ($1 < $2)}') )); then
	echo "TEST PASSED"	
	mem_diff=$(echo $active_max $idle_min | awk '{print $1-$2}')
	echo "IDLE Memory is less by $mem_diff MB"
else
	echo "TEST FAILED"
	mem_diff=$(echo $idle_min $active_max | awk '{print $1-$2}')
	echo "IDLE Memory is more by $mem_diff MB"
	exit 1 
fi

