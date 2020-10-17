#!/usr/bin/env bash

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


SERVER_PHYSCPU_NUM=2
SMT=true

while [ -n "$1" ]; do
	case $1 in
		--exec_cmd)
			shift
			EXEC_CMD=$1
			;;	
		--test_root)
			shift
			TEST_ROOT=$1
			;;				
		--server-physcpu-num)
			shift
			SERVER_PHYSCPU_NUM=$1
			;;
		--smt)
			shift
			SMT=$1
			;;	
		*)
			echo "Unknown parameter: $1" >&2
			exit 1
			;;
	esac
	shift
done
	
. "${TEST_ROOT}/perf/affinity.sh" > /dev/null 2>&1
setServerDBLoadAffinities --server-physcpu-num $SERVER_PHYSCPU_NUM --smt $SMT > /dev/null 2>&1

if [ -z "${SERVER_AFFINITY_CMD}" ]; then
    echo "Warning!!! Affinity is NOT set. Affinity tool may NOT be installed/supported."
fi

EXEC_CMD_WITH_AFFINITY="${SERVER_AFFINITY_CMD} ${EXEC_CMD}"
echo "Running EXEC_CMD_WITH_AFFINITY=${EXEC_CMD_WITH_AFFINITY}"

${EXEC_CMD_WITH_AFFINITY}
