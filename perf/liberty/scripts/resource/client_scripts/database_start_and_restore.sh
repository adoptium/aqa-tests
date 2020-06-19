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

# Check environment variables
if [ -z "${DB_HOST}" ]; then
	echo "DB_HOST not set"
	echo ${USAGE}
	exit
fi

if [ -z "${DB2_HOME}" ]; then
	echo "DB2_HOME not set"
	echo ${USAGE}
	exit
fi

if [ -z "${DB_NAME}" ]; then
	echo "DB_NAME not set"
	echo ${USAGE}
	exit
fi
if [ -z "${NET_PROTOCOL}" ]; then
    echo "NET_PROTOCOL not set"
    echo ${USAGE}
    exit
fi
echo "DB_HOST=${DB_HOST}"
echo "DB_NAME=${DB_NAME}"
echo "DB2_HOME=${DB2_HOME}"

case $SCENARIO in
    JMS)
        # Nothing to do
        ;;
    *)
        DB_HOST=`echo $DB_HOST|sed 's/[hH][sS]$//g'`

        echo "Restoring database ${DB_NAME} on ${DB_HOST}..."
        case $NET_PROTOCOL in
        "STAF") 
            DB_CMD="STAF ${DB_HOST} PROCESS START SHELL COMMAND ${DB2_HOME}/db2reset.sh ${DB_NAME} STDERRTOSTDOUT RETURNSTDOUT WAIT"
            ;; 
        "SSH") 
            DB_CMD="ssh ${MACHINE_USER}@${DB_HOST} ${DB2_HOME}/db2reset.sh ${DB_NAME} 2>&1 & wait"
            ;;
        "LOCAL")
            DB_CMD="${DB2_HOME}/db2reset.sh ${DB_NAME} 2>&1"
            ;;
        esac
        echo "${DB_CMD}"
        DB_RESPONSE=`${DB_CMD}`
        echo "${DB_RESPONSE}"
        RESTORE_COUNT=`echo "${DB_RESPONSE}" | grep -c -i "DB20000I  The RESTORE DATABASE command completed successfully."`

        if [ ${RESTORE_COUNT} -gt 0 ]; then
            echo "Database restore successful"
        else
            echo "!!! WARNING !!! Database restore not successful. Exiting"
            exit
        fi
        ;;
esac