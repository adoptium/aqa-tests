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

#TODO: verify that all alternative ssh commands work if using them instead of staf
# Identifies that this script is for Liberty Benchmark
# Note: Backslashes need to be escaped so it looks properly formatted when actually printed


# Import the common utilities needed to run this benchmark
. "$(dirname $0)"/common_utils.sh

configureDB()
{
    printf '%s\n' "
.--------------------------
| Configuring Database
"

	if [ -z "${DB_SETUP}" ]; then
		echo "DB_SETUP is not set. Not configuring database."
	else
		echo "DB_SETUP is set"
		
		if [[ "${SCENARIO}" = DayTrader7 ]]; then		
			
			echo "SCENARIO=${SCENARIO}"
			DB_FILE="${LIBERTY_DIR}/usr/shared/resources/data/tradedb7/service.properties"
			
			if [ -e "${DB_FILE}" ]; then
				echo "DB_FILE=${DB_FILE} exists! No need to configure database"
			else		
				echo "DB_FILE=${DB_FILE} doesn't exist! Need to configure database"		
				
				startLibertyServer 1
				PORT="9080"
				
				COMMAND="wget -O- http://${LIBERTY_HOST}:${PORT}/daytrader/config?action=buildDBTables"
				echo "(Re)-create DayTrader Database Tables and Indexes. Running ${COMMAND}"
				OUTPUT="`${COMMAND}`"
				
				if [[ ${OUTPUT} =~ "DayTrader tables successfully created!" ]]; then
				    echo "Database tables were successfully created"
				else
				    echo "Warning: Database tables could NOT be successfully created"
				    echo "OUTPUT of ${COMMAND}=${OUTPUT}"
				fi
				
				stopLibertyServer
				startLibertyServer 1
				
				COMMAND="wget -O- http://${LIBERTY_HOST}:${PORT}/daytrader/config?action=buildDB"
				echo "(Re)-populate DayTrader Database. Running ${COMMAND}"
				OUTPUT="`${COMMAND}`"	
				
				if [[ $OUTPUT =~ "Account#" ]]; then
				    echo "Database was successfully populated"
				else
				    echo "Warning: Database could NOT be successfully populated"
				    echo "OUTPUT of ${COMMAND}=${OUTPUT}"
				fi
				stopLibertyServer					
			fi
			exit
		else
			echo "No matching scenario. Not configuring database."
		fi
	fi
}
