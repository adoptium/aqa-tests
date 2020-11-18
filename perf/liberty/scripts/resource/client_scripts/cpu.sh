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

CLIENT_CPU_RESULTS="client_CPU_results.xml"
SERVER_CPU_RESULTS="server_CPU_results.xml"
DB_CPU_RESULTS="db_CPU_results.xml"
STAF_CMD=""
SSH_CMD=""
LOCAL_CMD=""
PLATFORM=""
dirExist=""
DB_PATCH_STRING=""
WAS_CUR_STRING=""
DB_CUR_STRING=""
WAS_PLATFORM=""
DB_PLATFORM=""
CLIENT_PLATFORM=""
function runNetProtocolCmd()
{
    case ${NET_PROTOCOL} in
        "STAF") 
        	echo "cpu.sh STAF_CMD=${STAF_CMD}"
            ${STAF_CMD}
        	;; 
        "SSH") 
        	echo "cpu.sh SSH_CMD=${SSH_CMD}"
            ${SSH_CMD}
        	;;
        "LOCAL") 
        	echo "cpu.sh LOCAL_CMD=${LOCAL_CMD}"
            eval ${LOCAL_CMD}
        	;;
	esac
	echo "cpu.sh Done running the command"
}

# tagGeneration function is for merging STAF command for Windows & Non Windows System it is called once machine_users are set
# STAF on windows does not run on CYGWIN and requires different parsing system (e.g ^ instead of \ for literal) and different way of parsing \\\ 
# in attempt to not have extra Windows command for every command we use String tag to minimize the # of commands for simplicity.

function tagGeneration()
{
	WAS_PLATFORM=`getPlatform ${WAS_HOST} ${HOST_MACHINE_USER}`
	DB_PLATFORM=`getPlatform ${DB_HOST} ${DB_MACHINE_USER}`
	CLIENT_PLATFORM=`getPlatform ${CLIENT} ${CLIENT_MACHINE_USER}`
	if [ "${WAS_PLATFORM}" = "Windows" ]; then
		WAS_TAG_STRING="^"
		WAS_PATCH_STRING="\""
	else 
		WAS_TAG_STRING="\\"
		WAS_PATCH_STRING="\\\""
	fi
	if [ "${DB_PLATFORM}" = "Windows" ]; then
		DB_TAG_STRING="^"
		DB_PATCH_STRING="\""
	else 
		DB_TAG_STRING="\\"
		DB_PATCH_STRING="\\\""
	fi		
}
function getPlatform()
{
	if [ -z $1 ]; then
		echo "Must provide a machine name to the getPlatform function"
		return 0
	fi

	MACHINE_NAME=$1
	MACHINE_USER=$2
	STAF_CMD="STAF ${MACHINE_NAME} VAR GET SYSTEM VAR STAF/Config/OS/Name"
	SSH_CMD="ssh ${MACHINE_USER}@${MACHINE_NAME} uname"
	LOCAL_CMD="uname"
	PLATFORM=`runNetProtocolCmd`
	if [ `echo ${PLATFORM} | grep -c -i Win` != 0 ]; then
		echo "Windows"
	fi
	if [ `echo ${PLATFORM} | grep -c -i Linux` != 0 ]; then
		echo "Linux"
	fi
	if [ `echo ${PLATFORM} | grep -c -i AIX` != 0 ]; then
		echo "AIX"
	fi
	if [ `echo ${PLATFORM} | grep -c -i OS/390` != 0 ]; then
		echo "ZOS"
	fi
	if [ `echo ${PLATFORM} | grep -c -i HP-UX` != 0 ]; then
		echo "HP-UX"
	fi
	if [ `echo ${PLATFORM} | grep -c -i SunOS` != 0 ]; then
		echo "Solaris"
	fi
}

function getCommand()
{
	if [ -z $1 ]; then
		echo "Must provide a machine name to the getCommand function"
		return 0
	fi
	
	MACHINE_NAME=$1
	MACHINE_USER=$2
	PLATFORM=`getPlatform ${MACHINE_NAME} ${MACHINE_USER}`	
	case ${PLATFORM} in
		Linux)
			echo "vmstat -n 5 ${SAMPLES}"
			;;
		AIX | HP-UX | Solaris)
			echo "vmstat 5 ${SAMPLES}"
			;;
		Windows)
			# Write any output from this command to nul, so it doesnt go into the xml
			#	- logman writes to a file
			echo "logman start ${LOGMAN_COUNTER_NAME} >nul"
			;;
		ZOS)
			if [ "${ZWAS_USER}" = "" ]; then 
				if [ "${NET_PROTOCOL}" = "STAF" ]; then
			    	echo "monitor_cpu_vmstat ${SERVER_PID} 5 ${SAMPLES} ${NUM_CPUS} ${ZWAS_USER}" 
				else 
					#TODO: this should be added to path for all users? look into it
					echo "~/bin/monitor_cpu_vmstat ${SERVER_PID} 5 ${SAMPLES} ${NUM_CPUS} ${ZWAS_USER}" 
				fi
			else 
			    # When running zWAS, use a special CPU monitoring script
			    # as there are several processes run under different user ids 
			    echo "monitor_WAS 5 ${SAMPLES} ${NUM_CPUS}" 
			fi
			;;
		*)
			echo "OS not recognised: ${PLATFORM}" ;;
	
	esac
}



if [ "${1}" = "start" ]; then
	SERVER_WORKDIR=${2}
	WAS_HOST=${3}
	DB_HOST=${4}
	NET_PROTOCOL=${5}
	HOST_MACHINE_USER=${6}
	CLIENT_MACHINE_USER=${7}
	DB_MACHINE_USER=${8}
	if [ -z ${9} ]; then
		DB_SERVER_WORKDIR=${SERVER_WORKDIR}
	else
		DB_SERVER_WORKDIR=${9}
	fi
	tagGeneration
	echo "Creating CPU files"
	echo "<iteration>" > ${CLIENT_CPU_RESULTS}
	#Windows command on STAF is parsed 
	STAF_CMD="STAF ${WAS_HOST} PROCESS START SHELL COMMAND echo PARMS ${WAS_TAG_STRING}<iteration${WAS_TAG_STRING}> WAIT STDERRTOSTDOUT STDOUT ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} WORKLOAD cpu-measure"	
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} exec -a cpu-measure sh -c \"echo \<iteration\> > ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1\" & wait"
	LOCAL_CMD="eval \"echo \<iteration\> > ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1\""
	echo "cpu.sh before runNetProtocolCmd LOCAL_CMD=${LOCAL_CMD}"
	runNetProtocolCmd
	if [ -n "${DB_HOST}" ]; then
		STAF_CMD="STAF ${DB_HOST} LIST DIRECTORY ${DB_SERVER_WORKDIR} RETURNSTDOUT STDERRTOSTDOUT WAIT"
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} ls ${DB_SERVER_WORKDIR} 2>&1 & wait"
		LOCAL_CMD="ls ${DB_SERVER_WORKDIR} 2>&1 & wait"
		echo "LOCAL_CMD=${LOCAL_CMD}"
		dirExist=`runNetProtocolCmd`
		#STAF & shell returns different string 
		if [[ `echo $dirExist|grep -E 'does not exist|No such file'` = $dirExist ]]; then
			STAF_CMD="STAF ${DB_HOST} FS CREATE DIRECTORY ${DB_SERVER_WORKDIR} FULLPATH"
			SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} mkdir -p ${DB_SERVER_WORKDIR}"
			LOCAL_CMD="mkdir -p ${DB_SERVER_WORKDIR}"
			runNetProtocolCmd 
		fi
		STAF_CMD="STAF ${DB_HOST} PROCESS START SHELL COMMAND echo PARMS ${DB_TAG_STRING}<iteration${DB_TAG_STRING}> WAIT STDERRTOSTDOUT STDOUT ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} WORKLOAD cpu-measure"
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} exec -a cpu-measure sh -c \"echo \<iteration\> > ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1\" & wait"
		LOCAL_CMD="eval \"echo \<iteration\> > ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1\""
		runNetProtocolCmd 
	fi
	
	exit
	
elif [ "${1}" = "end" ]; then
	SERVER_WORKDIR=${2}
	WAS_HOST=${3}
	DB_HOST=${4}
	NET_PROTOCOL=${5}
	HOST_MACHINE_USER=${6}
	CLIENT_MACHINE_USER=${7}
	DB_MACHINE_USER=${8}
	if [ -z ${9} ]; then
		DB_SERVER_WORKDIR=${SERVER_WORKDIR}
	else
		DB_SERVER_WORKDIR=${9}
	fi
	tagGeneration
	# Make sure all previous remote workloads have stopped
	echo "Cleaning up CPU processes on ${WAS_HOST}, ${DB_HOST}"
	STAF_CMD="STAF ${WAS_HOST} PROCESS STOP WORKLOAD cpu-measure USING SIGKILLALL"
	#ZOS does not have pkill need to use other way around
	if [ ${WAS_PLATFORM} = "ZOS" ]; then
		ZOS_PIDS="ssh ${HOST_MACHINE_USER}@${WAS_HOST} ps -elf | grep -v grep | grep cpu-measure | awk '{ print \$2 }'"
		SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} kill `${ZOS_PIDS}`"
		LOCAL_CMD="kill `${ZOS_PIDS}`"
	else 
		SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} pkill -f cpu-measure"
		LOCAL_CMD="pkill -f cpu-measure"
	fi
	runNetProtocolCmd 
	if [ -n "${DB_HOST}" ]; then 
		STAF_CMD="STAF ${DB_HOST} PROCESS STOP WORKLOAD cpu-measure USING SIGKILLALL"
		if [ ${DB_PLATFORM} = "ZOS" ]; then
			ZOS_PIDS="ssh ${DB_MACHINE_USER}@${DB_HOST} ps -elf | grep -v grep | grep cpu-measure | awk '{ print \$2 }'"
			SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} kill `${ZOS_PIDS}`"
			LOCAL_CMD="kill `${ZOS_PIDS}`"
		else 
			SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} pkill -f cpu-measure"
			LOCAL_CMD="pkill -f cpu-measure"
		fi
		runNetProtocolCmd
	fi
	
	# now output the closing tags
	echo "Finishing CPU files"
	echo "</iteration>" >> ${CLIENT_CPU_RESULTS}
	STAF_CMD="STAF ${WAS_HOST} PROCESS START SHELL COMMAND echo PARMS ${WAS_TAG_STRING}</iteration${WAS_TAG_STRING}> WAIT STDERRTOSTDOUT STDOUTAPPEND ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} WORKLOAD cpu-measure"
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} exec -a cpu-measure sh -c \"echo \</iteration\> >>${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1 & wait\" 2>&1 & wait"
	LOCAL_CMD="eval \"echo \</iteration\> >>${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1\""
	runNetProtocolCmd 
	if [ -n "${DB_HOST}" ]; then
		STAF_CMD="STAF ${DB_HOST} PROCESS START SHELL COMMAND echo ${DB_TAG_STRING}</iteration${DB_TAG_STRING}> WAIT STDERRTOSTDOUT STDOUTAPPEND ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} WORKLOAD cpu-measure"
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} exec -a cpu-measure sh -c \"echo \</iteration\> >>${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1 & wait\" 2>&1 & wait"
		LOCAL_CMD="eval \"echo \</iteration\> >>${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1\""
		runNetProtocolCmd 
	fi
	exit
else
	let SAMPLES=$1/6 # setting to 6 seconds to tie in with IWL output
	TYPE=$2
	NUM=$3
	CLIENT=$4
	SERVER_WORKDIR=$5
	WAS_HOST=$6
	DB_HOST=$7
	NET_PROTOCOL=${8}
	HOST_MACHINE_USER=${9}
	CLIENT_MACHINE_USER=${10}
	DB_MACHINE_USER=${11}
	if [ -z ${12} ]; then
		DB_SERVER_WORKDIR=${SERVER_WORKDIR}

	else
		DB_SERVER_WORKDIR=${12}
	fi
	tagGeneration
fi

echo "SERVER_WORKDIR=${SERVER_WORKDIR}"

LOGMAN_COUNTER_NAME="liberty_tradelite_cpulog"
LOGMAN_FILE="logman.out"





# get the commands
CLIENT_COMMAND=`getCommand ${CLIENT} ${CLIENT_MACHINE_USER}`
SERVER_COMMAND=`getCommand ${WAS_HOST} ${HOST_MACHINE_USER}`
#If its not windows, we need to remove trailing 
if [ ${NET_PROTOCOL} != "STAF" ] && [ ${WAS_PLATFORM} = "Windows" ]; then
	SERVER_COMMAND=${SERVER_COMMAND/'>nul'/'1>/dev/null'} 
fi
echo "Client CPU command: ${CLIENT_COMMAND}"
echo "Server CPU command: ${SERVER_COMMAND}"

# runPrimitive does not send a DB_HOST
if [ -n "${DB_HOST}" ]; then
	DB_COMMAND=`getCommand ${DB_HOST} ${DB_MACHINE_USER}`
	if [ ${NET_PROTOCOL} != "STAF" ] && [ ${DB_PLATFORM} = "Windows" ]; then
		DB_COMMAND=${DB_COMMAND/'>nul'/'1>/dev/null'}
	fi
	echo "DB CPU command: ${DB_COMMAND}"
fi


# Make sure all previous remote workloads have stopped
echo "Cleaning up CPU processes on ${WAS_HOST}, ${DB_HOST}"
if [ ${WAS_PLATFORM} = "ZOS" ]; then
	ZOS_PIDS="ssh ${HOST_MACHINE_USER}@${WAS_HOST} ps -elf | grep -v grep | grep cpu-measure | awk '{ print \$2 }'"
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} kill `${ZOS_PIDS}`"
	LOCAL_CMD="kill `${ZOS_PIDS}`"
else 
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} pkill -f cpu-measure"
	LOCAL_CMD="pkill -f cpu-measure"
fi

runNetProtocolCmd 
if [ -n "${DB_HOST}" ]; then 
	STAF_CMD="STAF ${DB_HOST} PROCESS STOP WORKLOAD cpu-measure USING SIGKILLALL"
	if [ ${DB_PLATFORM} = "ZOS" ]; then
		ZOS_PIDS="ssh ${DB_MACHINE_USER}@${DB_HOST} ps -elf | grep -v grep | grep cpu-measure | awk '{ print \$2 }'"
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} kill `${ZOS_PIDS}`"
		LOCAL_CMD="kill `${ZOS_PIDS}`"
	else 
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} pkill -f cpu-measure"
		LOCAL_CMD="pkill -f cpu-measure"
	fi
	runNetProtocolCmd
fi


# open the data tags
echo "Opening CPU data tags"

# THE FOLLOWING LINES ARE MESSY AND UNREADABLE - FIX IF CAN
#	This is due to needing to use the cygwin echo on windows
echo "<data runType=\"${TYPE}\" runNo=\"${NUM}\" machine=\"${CLIENT}\" os=\"${CLIENT_PLATFORM}\">" >> ${CLIENT_CPU_RESULTS}
# Code below does not use runNetProtocolCmd as echoing double quote ("") within the string that is required for xml tag is impossible
# Echoing double quote inside 2 double quote requires preservation of multiple back slashes (\) + this causes bash parser error 
# e.g SSH_CMD="ssh jbench@leonard exec -a cpu-measure \"echo \<data runType=\\\"${TYPE}\\\" runNo=\\\"${NUM}\\\" machine=\\\"${WAS_HOST}\\\" \> >/java/perffarm/liberty-cleaned/testfile_2.txt 2>&1 & wait\""
# will not be parsed correctly as below command and will give error:
# bash: /home/jbench/echo \<data runType="" runNo="" machine="" \> >/java/perffarm/liberty-cleaned/testfile_2.txt 2>&1 & wait: No such file or directory
case $NET_PROTOCOL in
    "STAF") 
		STAF ${WAS_HOST} PROCESS START SHELL COMMAND "echo ${WAS_TAG_STRING}<data runType=${WAS_PATCH_STRING}${TYPE}${WAS_PATCH_STRING} runNo=${WAS_PATCH_STRING}${NUM}${WAS_PATCH_STRING} machine=${WAS_PATCH_STRING}${WAS_HOST}${WAS_PATCH_STRING} os=${WAS_PATCH_STRING}`STAF ${WAS_HOST} VAR GET SYSTEM VAR STAF/Config/OS/Name | tail -1`${WAS_PATCH_STRING}${WAS_TAG_STRING}>" WAIT STDERRTOSTDOUT STDOUTAPPEND ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS}
        ;; 
    "SSH") 
        ssh ${HOST_MACHINE_USER}@${WAS_HOST} exec -a cpu-measure "echo \<data runType=\\\"${TYPE}\\\" runNo=\\\"${NUM}\\\" machine=\\\"${WAS_HOST}\\\" os=\\\"${WAS_PLATFORM}\\\"\> >>${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1" & wait
    	;;
    "LOCAL") 
        echo \<data runType=\"${TYPE}\" runNo=\"${NUM}\" machine=\"${WAS_HOST}\" os=\"${WAS_PLATFORM}\"\> >>${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1
    	;;
esac
if [ -n "${DB_HOST}" ]; then
	case $NET_PROTOCOL in
        "STAF") 
			STAF ${DB_HOST} PROCESS START SHELL COMMAND "echo ${DB_TAG_STRING}<data runType=${DB_PATCH_STRING}${TYPE}${DB_PATCH_STRING} runNo=${DB_PATCH_STRING}${NUM}${DB_PATCH_STRING} machine=${DB_PATCH_STRING}${DB_HOST}${DB_PATCH_STRING} os=${DB_PATCH_STRING}`STAF ${DB_HOST} VAR GET SYSTEM VAR STAF/Config/OS/Name | tail -1`${DB_PATCH_STRING}${DB_TAG_STRING}>" WAIT STDERRTOSTDOUT STDOUTAPPEND ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS}
			;; 
        "SSH") 
        	ssh ${DB_MACHINE_USER}@${DB_HOST} exec -a cpu-measure "echo \<data runType=\\\"${TYPE}\\\" runNo=\\\"${NUM}\\\" machine=\\\"${DB_HOST}\\\" os=\\\"${DB_PLATFORM}\\\"\> >>${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1" & wait
        	;;
        "LOCAL") 
        	echo \<data runType=\"${TYPE}\" runNo=\"${NUM}\" machine=\"${DB_HOST}\" os=\"${DB_PLATFORM}\"\> >>${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1
        	;;
	esac
fi
echo ""
#loop over the machines to check if any are windows, if so create the logman counter
# don't include the client - has to be linux due to iwlengine
# will ignore DB_HOST if not set due to how for works

for i in ${WAS_HOST} ${DB_HOST}
do
	case `getPlatform ${i}` in
		Windows)	
			case ${NET_PROTOCOL} in
				"STAF")
					STAF ${i} PROCESS START SHELL COMMAND "logman create counter ${LOGMAN_COUNTER_NAME} -c \"\Processor(_Total)\% Processor Time\" \"\Memory\Available bytes\" -f csv -si 5 -sc ${SAMPLES} -o ${LOGMAN_FILE} -ow -m start" STDERRTOSTDOUT RETURNSTDOUT WAIT
					;;
				"SSH")
					if [ "${i}" = "${WAS_HOST}" ]; then
						TEMP_PATH=`ssh ${HOST_MACHINE_USER}@${WAS_HOST} cygpath -m ${SERVER_WORKDIR}`
						ssh ${HOST_MACHINE_USER}@${WAS_HOST} "logman create counter ${LOGMAN_COUNTER_NAME} -c \"\Processor(_Total)\% Processor Time\" \"\Memory\Available bytes\" -f csv -si 5 -sc ${SAMPLES} -o ${TEMP_PATH}/${LOGMAN_FILE} -ow -m start" 2>&1 & wait
					else 
						TEMP_PATH=`ssh ${DB_MACHINE_USER}@${DB_HOST} cygpath -m ${DB_SERVER_WORKDIR}`
						ssh ${DB_MACHINE_USER}@${DB_HOST} "logman create counter ${LOGMAN_COUNTER_NAME} -c \"\Processor(_Total)\% Processor Time\" \"\Memory\Available bytes\" -f csv -si 5 -sc ${SAMPLES} -o ${TEMP_PATH}/${LOGMAN_FILE} -ow -m start" 2>&1 & wait
					fi
					;;
				"LOCAL")
					TEMP_PATH=`cygpath -m ${SERVER_WORKDIR}`
					eval "logman create counter ${LOGMAN_COUNTER_NAME} -c \"\Processor(_Total)\% Processor Time\" \"\Memory\Available bytes\" -f csv -si 5 -sc ${SAMPLES} -o ${TEMP_PATH}/${LOGMAN_FILE} -ow -m start" 2>&1
					;;
			esac
			;;
	esac	
done

echo ""
#output the data
#Windows Command does not redirect output immediately, uses logman output later SSH.
echo "Running CPU commands"
STAF_CMD="STAF ${WAS_HOST} PROCESS START SHELL COMMAND \"${SERVER_COMMAND}\" ASYNC STDOUTAPPEND ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} WORKLOAD cpu-measure"
if [ "${WAS_PLATFORM}" != "Windows" ]; then
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} sh -c \"${SERVER_COMMAND} >> ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS}\" &"	
	LOCAL_CMD="eval ${SERVER_COMMAND} >> ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} &"
else 
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} exec -a cpu-measure ${SERVER_COMMAND} &"	
	LOCAL_CMD="eval ${SERVER_COMMAND} &"
fi
runNetProtocolCmd 
if [ -n "${DB_HOST}" ]; then
	STAF_CMD="STAF ${DB_HOST} PROCESS START SHELL COMMAND \"${DB_COMMAND}\" ASYNC STDOUTAPPEND ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} WORKLOAD cpu-measure"
	if [ "${DB_PLATFORM}" != "Windows" ]; then
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} sh -c \"${DB_COMMAND} >> ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS}\" &"
		LOCAL_CMD="eval ${DB_COMMAND} >> ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} &"
	else 
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} exec -a cpu-measure ${DB_COMMAND} &"
		LOCAL_CMD="eval ${DB_COMMAND} &"
	fi
	runNetProtocolCmd
fi
${CLIENT_COMMAND} >> ${CLIENT_CPU_RESULTS}
echo ""

# Make sure all previous remote workloads have stopped
echo "Cleaning up CPU processes on ${WAS_HOST}, ${DB_HOST}"
if [ "${WAS_PLATFORM}" = "ZOS" ]; then
	ZOS_PIDS="ssh ${HOST_MACHINE_USER}@${WAS_HOST} ps -elf | grep -v grep | grep cpu-measure | awk '{ print \$2 }'"
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} kill `${ZOS_PIDS}`"
else 
	SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} pkill -f cpu-measure"
fi
LOCAL_CMD="pkill -f cpu-measure"
runNetProtocolCmd 
if [ -n "${DB_HOST}" ]; then 
	STAF_CMD="STAF ${DB_HOST} PROCESS STOP WORKLOAD cpu-measure USING SIGKILLALL"
	if [ "${DB_PLATFORM}" = "ZOS" ]; then
		ZOS_PIDS="ssh ${DB_MACHINE_USER}@${DB_HOST} ps -elf | grep -v grep | grep cpu-measure | awk '{ print \$2 }'"
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} kill `${ZOS_PIDS}`"
	else 
		SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} pkill -f cpu-measure"
	fi
	LOCAL_CMD="pkill -f cpu-measure"
	runNetProtocolCmd
	fi
echo ""

#loop over the machines to check if any are windows, if so copy the logman output to the datafile
for i in ${WAS_HOST} ${DB_HOST}
do
	case `getPlatform ${i}` in
		Windows)
			#If we use SSH & STAF need to set LOGMAN Counters path in order to access the file
			# Sleeping for 5s is a hacky way to deal with windows sometimes being slow to release the CPU log file.
			# Idealy we should poll the state of the CPU log/CPU monitor to check it has completed, since 5s is an arbitary number.
			echo "Sleeping for 5s - Windows can be slow to release CPU log"
			sleep 5
			echo "Copying windows logman output to the cpu xml"
			
			if [ "${i}" = "${WAS_HOST}" ]; then
				STAF_CMD="STAF ${WAS_HOST} PROCESS START SHELL COMMAND \"cat ${LOGMAN_FILE}* >> ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS}\" STDERRTOSTDOUT RETURNSTDOUT WAIT"	
				SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} cat ${SERVER_WORKDIR}/${LOGMAN_FILE}* >> ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1 & wait"
				LOCAL_CMD="eval cat ${SERVER_WORKDIR}/${LOGMAN_FILE}* >> ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1 & wait"
			else 
				STAF_CMD="STAF ${DB_HOST} PROCESS START SHELL COMMAND \"cat ${LOGMAN_FILE}* >> ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS}\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
				SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} cat ${DB_SERVER_WORKDIR}/${LOGMAN_FILE}* >> ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1 & wait"
				LOCAL_CMD="eval cat ${DB_SERVER_WORKDIR}/${LOGMAN_FILE}* >> ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1"
			fi
			runNetProtocolCmd 

			if [ "${i}" = "${WAS_HOST}" ]; then
				STAF_CMD="STAF ${WAS_HOST} PROCESS START SHELL COMMAND \"rm -v ${LOGMAN_FILE}*\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
				SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} rm -v ${SERVER_WOKRDIR}/${LOGMAN_FILE}* 2>&1 & wait"
				LOCAL_CMD="eval rm -v ${SERVER_WORKDIR}/${LOGMAN_FILE}* 2>&1"
			else 
				STAF_CMD="STAF ${DB_HOST} PROCESS START SHELL COMMAND \"rm -v ${LOGMAN_FILE}*\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
				SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} rm -v ${DB_SERVER_WORKDIR}/${LOGMAN_FILE}* 2>&1 & wait"
				LOCAL_CMD="eval rm -v ${DB_SERVER_WORKDIR}/${LOGMAN_FILE}* 2>&1 & wait"
			fi
			runNetProtocolCmd 
			echo ""
			;;
	esac
done

# close the data tag
echo "Closing CPU data tags"	
echo "</data>" >> ${CLIENT_CPU_RESULTS}	
STAF_CMD="STAF ${WAS_HOST} PROCESS START SHELL COMMAND echo PARMS ${WAS_TAG_STRING}</data${WAS_TAG_STRING}> WAIT STDERRTOSTDOUT STDOUTAPPEND ${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} WORKLOAD cpu-measure"
SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} exec -a cpu-measure sh -c \"echo \</data\> >>${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1\" & wait"
LOCAL_CMD="eval \"echo \</data\> >>${SERVER_WORKDIR}/${SERVER_CPU_RESULTS} 2>&1\""
runNetProtocolCmd 
if [ -n "${DB_HOST}" ]; then
	STAF_CMD="STAF ${DB_HOST} PROCESS START SHELL COMMAND echo PARMS ${DB_TAG_STRING}</data${DB_TAG_STRING}> WAIT STDERRTOSTDOUT STDOUTAPPEND ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS}"
	SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} exec -a cpu-measure sh -c \"echo \</data\> >>${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1\" & wait"
	LOCAL_CMD="eval \"echo \</data\> >>${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1\""
	runNetProtocolCmd 
fi

#loop over the machines to check if any are windows, if so delete the logman counter
# don't include the client - has to be linux due to iwlengine
for i in ${WAS_HOST} ${DB_HOST}
do
	case "`getPlatform ${i}`" in
		Windows)
			LOGMAN_DELETE="logman delete ${LOGMAN_COUNTER_NAME}"
			STAF_CMD="STAF ${i} PROCESS START SHELL COMMAND \"${LOGMAN_DELETE}\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
			if [ "${i}" = "${WAS_HOST}" ]; then
				SSH_CMD="ssh ${HOST_MACHINE_USER}@${WAS_HOST} ${LOGMAN_DELETE} 2>&1"
			else 
				SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_HOST} ${LOGMAN_DELETE} 2>&1"
			fi
			LOCAL_CMD="eval ${LOGMAN_DELETE} 2>&1"
			runNetProtocolCmd 
			;;
	esac
done
