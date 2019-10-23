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

## Print out the help information if all mandatory environment variables are not set
usage()
{
echo "
Usage:

To customize the use of this script use the following environment variables:

VERBOSE_MODE        - Prints all commands before they are executed.

NO_SETUP            - Liberty server creation is skipped if set to true.
                      If false, a new server will be created and the corresponding customized server.xml
                      and customized bootstrap.properties (if exists) files will overwrite the server's default.

SETUP_ONLY          - Only Liberty server creation and replacement of Liberty server default server.xml and bootstrap.properties (if available) is done.

JDK                 - Name of JDK build to use.

JDK_DIR             - Absolute path to directory where JDK builds are located.
                      Ex: if the path to a JDK's bin directory is /tmp/all_jdks/jdk8/sdk/bin
                      Then JDK=jdk8 and JDK_DIR=/tmp/all_jdks

JDK_OPTIONS         - JVM command line options

OPENJ9_JAVA_OPTIONS    - OpenJ9 Java options

PROFILING_TOOL      - Profiling Tool to Use (Example: 'jprof tprof', 'perf stat')

PETERFP             - Enable Peter's Footprint Tool (Default: false).
                      Gives a more detailed version of the footprint used, breaking it down so you can tell what component changed the footprint. Only needed for diagnosis when the footprint changes

WARM                - Number of warm runs

COLD                - Number of cold runs

WARMUP              - Number of warmup runs

COGNOS_INSTALL_DIR  - Installation directory for cognos

COGNOS_WAIT         - Seconds to wait for cognos to finish starting up (Default: 120)

REMOUNTSCRIPT       - Only required for AIX

LIBERTY_HOST        - Hostname for Liberty host machine

LAUNCH_SCRIPT       - Liberty script that can create, start, stop a server

SERVER_NAME         - The given name will be used to identify the Liberty server.

SERVER_XML          - Liberty server.xml configuration

AFFINITY            - CPU pinning command prefixed on the Liberty server start command.

SCENARIO            - Supported scenarios: AcmeAir, Cognos, DayTrader, DayTrader7, HugeEJB, HugeEJBApp, SmallServlet, TradeLite

APP_VERSION
                    - The specific DayTrader 3 application version used for all scenarios
                      involving this particular benchmark. (Default: daytrader3.0.10.1-ee6-src)

RESULTS_MACHINE     - Hostname for results storage machine

RESULTS_DIR         - Name of directory on Liberty host where results are temporarily stored

ROOT_RESULTS_DIR    - Absolute path of Liberty results directory on remote storage machine

"
}

#######################################################################################
#	STARTUP TIME AND FOOTPRINT UTILS - Helper methods that are sufp specific
#######################################################################################

##
## Environment variables setup. Terminate script if a mandatory env variable is not set
checkAndSetSUFPEnvVars()
{
    printf '%s\n' "
.--------------------------
| SUFP Environment Setup
"

    if [ -z "$OPENJ9_JAVA_OPTIONS" ]; then
        echo "OpenJ9 JAVA Options not set."
    else
        export OPENJ9_JAVA_OPTIONS=${OPENJ9_JAVA_OPTIONS}
    fi

    # Peter's Footprint Tool
    if [ -z "$PETERFP" ]; then
        echo "Optional PETERFP not set. Setting to 'false'"
        PETERFP="false"
    fi

    if [ -z "$WARM" ]; then
        echo "WARM not set"
        echo $USAGE
        exit
    fi

    if [ -z "$COLD" ]; then
        echo "COLD not set"
        echo $USAGE
        exit
    fi

    if [ -z "$WARMUP" ]; then
        echo "Optional WARMUP not set. Setting to 0"
        WARMUP=0
    fi

    if [ -z "$REMOUNTSCRIPT" ]; then
        case $PLATFORM in
            AIX)
                echo "Optional REMOUNTSCRIPT not set. Required for AIX."
                usage
                
                ;;
        esac
    fi
}

##
## Scenario dependent environment variable setup
scenarioSpecificServerSetup()
{
    printf '%s\n' "
.--------------------------
| Scenario Specific Liberty Server Setup
"
    ###############
    #REPLACE DEFAULT SERVER.XML AND BOOTSTRAP.PROPERTIES WITH CUSTOM FILES
    ###############

    case ${SCENARIO} in
        DayTrader)
            export CLIENT_SERVERXML="sufpdtserver.xml"
            export CLIENT_BOOTSTRAP="sufpbootstrap.properties"
            ;;
            
        DayTrader7)
            export CLIENT_SERVERXML="sufpdt7server.xml"
            export CLIENT_BOOTSTRAP="sufpbootstrap.properties"
            ;;            

        DayTraderSec)
            export CLIENT_SERVERXML="sufpdtserver.xml"
            export CLIENT_BOOTSTRAP="sufpj2secbootstrap.properties"
            ;;
        
        Cognos)
            export CLIENT_SERVERXML="sufpcogserver.xml"
            export CLIENT_BOOTSTRAP="sufpcogbootstrap.properties"
            ;;

        TradeLite)
            export CLIENT_SERVERXML="sufpserver.xml"
            export CLIENT_BOOTSTRAP="sufpbootstrap.properties"
            ;;

        TradeLiteSec)
            export CLIENT_SERVERXML="sufpserver.xml"
            export CLIENT_BOOTSTRAP="sufpj2secbootstrap.properties"
            ;;

        AcmeAir)
            export CLIENT_SERVERXML="acmeair_server.xml"
            ;;

        HugeEJB)
            export CLIENT_SERVERXML="hugeejbserver.xml"
            ;;

        SmallServlet)
            export CLIENT_SERVERXML="smallservletserver.xml"
            ;;

        HugeEJBApp)
            export CLIENT_SERVERXML="hugeejbappserver.xml"
            ;;

        *)
            echo "Scenario not recognized. Terminating."
            exit
            ;;
    esac

    # If SERVER_XML is given as an environment variable, use that instead
    #TODO: Once verified, do we also let bootstrap.properties be given via an env variable?
    if [ -z "${SERVER_XML}" ]; then
        echo "Using ${CLIENT_SERVERXML} for server.xml"
    else
        echo "Server XML defined in job. Using ${SERVER_XML} instead of ${CLIENT_SERVERXML}"
        export CLIENT_SERVERXML="${SERVER_XML}"
    fi

    echo ""

    # Replace default server.xml and bootstrap.properties (if available)
    echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_SERVERXML} ${SERVER_DIR}/server.xml"
    if [ ! -z "${CLIENT_BOOTSTRAP}" ]; then
        echo "Using ${CLIENT_BOOTSTRAP} for bootstrap.properties"
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_BOOTSTRAP} ${SERVER_DIR}/bootstrap.properties"
    else
        echo "Liberty server's default bootstrap.properties will be used"
    fi

    # replace DayTrader3 App version in server.xml
    if [[ "${SCENARIO}" = DayTrader* ]] && [[ ! "${SCENARIO}" = DayTrader7 ]]; then

        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh ${SERVER_DIR}/server.xml
        fi

        # replace the placeholders in server.xml
        echo "Setting the following values in server.xml:"
        echo "APP_VERSION=${APP_VERSION}"

        local SERVER_XML_TMP=`sed "s/APP_VERSION_HERE/${APP_VERSION}/g" ${SERVER_DIR}/server.xml`

        ###############
        #CHECK IF CORRECTLY REPLACED PLACEHOLDERS IN SERVER.XML
        ###############

        if [ ! -z "${SERVER_XML_TMP}" ]; then
            echo ${SERVER_XML_TMP} | tee ${SERVER_DIR}/server.xml
        else
            echo "Could not match required placeholders in server.xml. File before sed replace:"
            cat  ${SERVER_DIR}/server.xml
            exit 1
        fi
        
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh -toascii ${SERVER_DIR}/server.xml
        fi

    fi

    # Cognos related setup
    if [ "${SCENARIO}" = "Cognos" ]; then
        if [ -z $COGNOS_INSTALL_DIR ]; then
            echo "COGNOS_INSTALL_DIR is not set. Cognos startup will not work. Exiting"
            exit
        fi

        if [ -z $LIB_PATH ]; then
            echo "LIB_PATH is not set. Setting to ${COGNOS_INSTALL_DIR}/bin"
            export LIB_PATH="${COGNOS_INSTALL_DIR}/bin"
        fi

        if [ -z $COGNOS_WAIT ]; then
            echo "COGNOS\_WAIT not set. Defaulting to 120 seconds"
        else
            echo "COGNOS_WAIT set to ${COGNOS_WAIT} seconds"
        fi

        case $PLATFORM in
            AIX)
                echo "Exporting LIBPATH=${LIB_PATH}:${LIBPATH:-}"
                export LIBPATH=${LIB_PATH}:${LIBPATH:-}
                export JDK_OPTIONS="-Djava.library.path=${LIBPATH} ${JDK_OPTIONS}"
                ;;
                
            CYGWIN)
                # TODO: confirm LD_LIBRARY_PATH = PATH for cygwin
                echo "Exporting: PATH=${LIB_PATH}:$PATH"
                export PATH=${LIB_PATH}:$PATH
                export JDK_OPTIONS="-Djava.library.path=${PATH} ${JDK_OPTIONS}"
                ;;
            Linux)
                echo "Exporting: LD_LIBRARY_PATH=${LIB_PATH}:$LD_LIBRARY_PATH"
                export LD_LIBRARY_PATH=${LIB_PATH}:$LD_LIBRARY_PATH
                ;;
            *)
                ;;
        esac

        echo "Added java.library.path to JDK_OPTIONS. They now are:"
        echo $JDK_OPTIONS

        ###############
        #REPLACE JAVA LIBRARY PATH AND INSTALL DIR INFO IN BOOTSTRAP.PROPERTIES
        ###############

        # In case cognos runs on OS390
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh ${SERVER_DIR}/bootstrap.properties
        fi

        echo "Setting the following values in bootstrap.properties:"
        echo "java.library.path=${LIB_PATH}"
        echo "install.dir=${COGNOS_INSTALL_DIR}"
        local BOOTSTRAP=`sed "s%LIBRARY_PATH_HERE%${LIB_PATH}%g" ${SERVER_DIR}/bootstrap.properties | sed "s%INSTALL_DIR_HERE%${COGNOS_INSTALL_DIR}%g"`
        
        ###############
        #CHECK IF CORRECTLY REPLACED PLACEHOLDERS IN BOOTSTRAP.PROPERTIES
        ###############

        if [ ! -z "${BOOTSTRAP}" ]; then
            echo "${BOOTSTRAP}" | tee ${SERVER_DIR}/bootstrap.properties
        else
            echo "Couldn't find placeholders in bootstrap.properties. Failing run."
            cat ${SERVER_DIR}/bootstrap.properties
            exit
        fi

        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh -toascii ${SERVER_DIR}/bootstrap.properties
        fi
    fi
}

##
## Set TRACE_FILE to the most recent Liberty Server logs/message* or logs/trace_
setNewestTraceFile()
{
    printf '%s\n' "
.--------------------------
| Set Newest Trace File
"
  # Get most recent trace file
  TRACE_FILE="`ls -t ${SERVER_DIR}/logs/ | grep message | head -1`"
  # Liberty changed the log file name from trace to messages, so check on trace too for backwards compatibility
  if [ -z "${TRACE_FILE}" ]; then
    TRACE_FILE="`ls -t ${SERVER_DIR}/logs/ | grep trace_ | head -1`"
  fi
  echo "TRACE_FILE=${TRACE_FILE}"
}

##
## Stop the cognos database service
stopCognos()
{
    printf '%s\n' "
.--------------------------
| Stopping Cognos
"
  case $PLATFORM in
    CYGWIN)
      echo "Stopping Cognos database service"
      NET STOP "Cognos Content Database"
      ;;
    Linux)
      echo "Stopping Cognos database service"
      ${COGNOS_INSTALL_DIR}/bin/derby.sh stop 1527 &
      ;;
    AIX)
      echo "Stopping Cognos database service"
      ${COGNOS_INSTALL_DIR}/bin/derby.sh stop 1527 &
      ;;
    *)
      # shouldn't get here
      ;;
  esac
}

##
## Starts and stops the Liberty server, calculating startup time and memory footprint
#
# Requires the following environment set:
#   BENCHMARK_DIR, LIBERTY_DIR, SERVER_DIR, JAVA_HOME, JDK_OPTIONS
#	
#	Optional environment:
#	  IS_COLD_RUN=true
startupFootprint()
{
  printf '%s\n' "
.--------------------------
| Startup and Footprint
"
  FILES_TO_STORE=""

  ###############
  #XJIT SETUP
  ###############

  #function received "Xjit" as a parameter
  if [ ! -z $1 ] && [ $1 = "Xjit" ]; then
    echo "Adding -Xjit:verbose={compilePerformance},disableSuffixLogs,vlog=verbosejit.txt to jvm.options. "

    #backup jvm.options file if it exists or create new one
    if [ -e ${SERVER_DIR}/jvm.options ]; then
      cp ${SERVER_DIR}/jvm.options ${SERVER_DIR}/backupoptfile
      export restorebkup=true
    else
      touch ${SERVER_DIR}/jvm.options
    fi

    #add xjit to jvm.options
    echo "-Xjit:verbose={compilePerformance},disableSuffixLogs,vlog=verbosejit.txt" >> ${SERVER_DIR}/jvm.options
    export xjitMode=true
    enableJitThreadUtilizationInfo
  fi

  export JVM_ARGS="${JDK_OPTIONS}"

  ###############
  #PETER'S FOOTPRINT
  ###############

  #TODO: Once verified, move this to inside the SERVER_DIR instead of LIBERTY_DIR
  if [ "${PETERFPAR}" = "true" ];then
    export JVM_ARGS="${JVM_ARGS} -Xdump:system:events=user,request=exclusive+compact+prepwalk -Xverbosegclog:${LIBERTY_DIR}/verbosegc.xml"
    echo "Settings -Xdump:system:events=user,request=exclusive+compact+prepwalk -Xverbosegclog:${LIBERTY_DIR}/verbosegc.xml to JVM ARGS for Peter's Footprint tool"
  fi

  echo "JVM_ARGS=${JVM_ARGS}"

  ###############
  #COLD RUN SETUP
  ###############

  # if this is a cold run, determine is we're running with --clean
  if [ "${IS_COLD_RUN}" = "true" ]; then
    setLibertyCleanRunOptions
    # Clear shared class caches
    if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
      destroySharedClassCache
    fi
  else
    echo "WARM RUN. Workarea and shared class caches will be warm."
    CLEAN=""
    if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
      printSharedClassCacheInfo
    fi
  fi

  ###############
  #PRE CLEANUP
  ###############

  # clear up after any previous runs
  terminateRunningLibertyServer

  ###############
  #GET HUGEPAGES
  ###############

  getPreBenchmarkHugePagesInUse

  ###############
  #START SERVER WITH MAX 2 ATTEMPTS
  ###############

  # TODO: Once verfied, enableJitThreadUtilizationInfo for runs other than xjit
  echoAndRunCmd "cd ${BENCHMARK_DIR}/tmp"
  startLibertyServer 1
  
  echoAndRunCmd "cd -"
  disableJitThreadUtilizationInfo
  echo ""
  getRunningServerPID
  echo ""
  libertyServerRunningCheck
  printLibertyServerProcessCommand
  echo ""

  setNewestTraceFile

  # Must wait for liberty to finish starting up. 10s should be fine, since target is 5s
  echo "Waiting for the Liberty server to finish starting up"
  echoAndRunCmd "sleep 10"

  ###############
  #GET FOOTPRINT
  ###############

  calculateFootprint
  
  ###############
  #RUN PETER'S FOOTPRINT TOOL
  ###############

  # Gives a more detailed version of the footprint used, breaking it down so you can tell what component changed the footprint. Only needed for diagnosis when the footprint changes.
  if [ "${PETERFPAR}" = "true" ]; then
    echo "Running Peter Shipton's footprint tool"
    case ${PLATFORM} in
      CYGWIN)
        sh ${BENCHMARK_DIR}/tools/fptool/peter_fp.sh ${BENCHMARK_DIR}/tools/fptool ${JAVA_HOME} ${LIBERTY_DIR} ${SERVER_PID} ${WINDOWS_PID}
        ;;
      AIX)
        sh ${BENCHMARK_DIR}/tools/fptool/peter_fp.sh ${BENCHMARK_DIR}/tools/fptool ${JAVA_HOME} ${LIBERTY_DIR} ${SERVER_PID}
        ;;
      SunOS)
        echo "Platform not supported yet"
        ;;
      OS/390)
        echo "Platform not supported yet"
        ;;
      *)
        sh ${BENCHMARK_DIR}/tools/fptool/peter_fp.sh ${BENCHMARK_DIR}/tools/fptool ${JAVA_HOME} ${LIBERTY_DIR} ${SERVER_PID}
        ;;
    esac
  fi

  echo ""

  ###############
  #BACKUP AND REMOVE JVM OPTIONS BEFORE STOPPING SERVER
  ###############

  #remove jvm args so we don't get any verbose logs (GC/JIT) overwritten when running the stopping server command
  local BACKUP_JVM_OPTIONS=${JVM_ARGS}
  export JVM_ARGS=""
  # TODO: Once verified, need -Xverbosegclog:stopSerververbosegc.xml like throughput?

  ###############
  #GET JAVA CORE
  ###############

  if [ "$REQUEST_CORE" = "true" ]; then
    case ${PLATFORM} in
      CYGWIN)
        echo "Coming soon"
        ;;
      OS/390)
        echo "Coming soon"
        ;;
      *)
        kill -3 ${SERVER_PID}
        sleep 5
        mv /proc/${SERVER_PID}/cwd/javacore*${SERVER_PID}*txt ${BENCHMARK_DIR}/tmp/
        FILES_TO_STORE="${FILES_TO_STORE} ${BENCHMARK_DIR}/tmp/javacore*${SERVER_PID}*txt"
        ;; 
    esac
  fi
      
  ###############
  #PROPERLY STOP SERVER WITH LIBERTY SCRIPT
  ###############

  echoAndRunCmd "cd ${BENCHMARK_DIR}/tmp"
  stopLibertyServer
  echoAndRunCmd "cd -"
  echo ""

  if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
    printSharedClassCacheInfo
  fi
  
  # restore previous JVM args
  export JVM_ARGS="${BACKUP_JVM_OPTIONS}"

  ###############
  #RESTORE JVM.OPTIONS BACKUP IF ANY
  ###############

  if [ "$restorebkup" = "true" ] && [ -e ${SERVER_DIR}/backupoptfile ]; then
    echo "There was a jvm.options file present already. Restoring from backup"
    mv ${SERVER_DIR}/backupoptfile ${SERVER_DIR}/jvm.options
  else
    if [ -e ${SERVER_DIR}/jvm.options ]; then
      # TODO: Once verified why we delete it
      rm ${SERVER_DIR}/jvm.options
    fi
  fi

  ###############
  #CALCULATE STARTUP TIME
  ###############

  setNewestTraceFile

  if [ "$PLATFORM" = "OS/390" ] && [ ! -z "${TRACE_FILE}" ]; then
    # TODO: Once verified why this is being stored in a variable. Is it to reduce output?:
    # convertFile=`iconv -fISO8859-1 -tIBM-1047 ${SERVER_DIR}/logs/${TRACE_FILE} > ${SERVER_DIR}/logs/${TRACE_FILE}.ebcdic`
    iconv -fISO8859-1 -tIBM-1047 ${SERVER_DIR}/logs/${TRACE_FILE} > ${SERVER_DIR}/logs/${TRACE_FILE}.ebcdic
    TRACE_FILE=${TRACE_FILE}.ebcdic
  fi

  if [ -z "${TRACE_FILE}" ]; then
    echo "!!! WARNING !!! No trace file found. Cannot process startup time. Contents of ${SERVER_DIR}/logs/:"
    ls -t ${SERVER_DIR}/logs/
  else
    echo "Trace file found: ${SERVER_DIR}/logs/${TRACE_FILE}"

    case $SCENARIO in
      DayTrader|DayTraderSec)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "daytrader3" ${SERVER_NAME}
        ;;
      DayTrader7)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "daytrader7" ${SERVER_NAME}
        ;;        
      TradeLite|TradeLiteSec)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "tradelite" ${SERVER_NAME}
        ;;
      AcmeAir)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "acmeair-webapp" ${SERVER_NAME}
        ;;
      HugeEJB)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "Servlet_35MB" ${SERVER_NAME} NoPrint
        local ServletSU=${SUTime}
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "EJB_500" ${SERVER_NAME} NoPrint
        local HugeEJBSU=${SUTime}
        local max=`echo "${ServletSU}\n${HugeEJB}"|sort -n|tail -n 1`
        echo "HugeEJB: ${HugeEJBSU}"
        echo "ServletSU: ${ServletSU}"
        echo "Startup time: ${max}"
        ;;
      SmallServlet)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "Servlet_35MB" ${SERVER_NAME}
        ;;
      HugeEJBApp)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "EJB_500" ${SERVER_NAME}
        ;;
      Cognos)
        calcTime "${SERVER_DIR}/logs/${TRACE_FILE}" ${LIBERTY_START_TIME} "cognos" ${SERVER_NAME}
        ;;
    esac
  fi

  if [ "$xjitMode" = "true" ]; then
    calculateJitCpuUtilization
  fi

  if [ "$PLATFORM" = "OS/390" ]; then
    local FP_LE=`cat ${SERVER_DIR}/logs/console.log|grep "Total heap storage used"|awk '{SUM+=$NF} END{print SUM/1024}'`
    echo "Footprint from LE: ${FP_LE}"
  fi

  ###############
  #STORE LOGS
  ###############

  FILES_TO_STORE="${FILES_TO_STORE} ${SERVER_DIR}/logs/${TRACE_FILE} ${SERVER_DIR}/logs/console.log"
  case ${PLATFORM} in
    AIX)
      FILES_TO_STORE="${FILES_TO_STORE} ${BENCHMARK_DIR}/tmp/footprint.out"
      ;;
  esac

  #TODO: Once verified if zOS files on webber are ascii
  # convert backup files to ASCII for os390
  if [[ "$PLATFORM" = "OS/390" ]]; then
    echo "Making sure all files are in ascii"
    ${BENCHMARK_DIR}/bin/encode.sh -toascii ${FILES_TO_STORE}
  fi

  storeFiles ${FILES_TO_STORE}
  FILES_TO_STORE=""
}

##
## Calculates the Liberty server's startup time
calcTime()
{
    printf '%s\n' "
.--------------------------
| Calculating Startup Time
"
	local FILE=$1
	local START_TIME=$2
	local APP_NAME=$3
	local SERVER_NAME=$4

  echo "FILE=${FILE}"
  echo "START_TIME=${START_TIME}"
  echo "APP_NAME=${APP_NAME}"
  echo "SERVER_NAME=${SERVER_NAME}"
  echo ""

	###############
	#COPY TRACELOG
	###############

	# awk doesn't like the formatting in the trace file name, so make a temp copy
	cp "${FILE}" trace.log 

	if [ ! -e trace.log ]; then
		echo "Could not find trace file."
		exit
	fi

	###############
	#GET START TIME
	###############

	echo "Start time: ${START_TIME}"
	local START_MILLIS=`echo ${START_TIME} | awk '
		BEGIN {
			# idealy use this regex, but requires gawk v4+
			#regex="[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{9}"
			regex="[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
			
			}
				{
					if(match($0,regex)) {
							start = RSTART;
							size = RLENGTH;
							hours=substr($0,start,start+1);
							minutes=substr($0,start+3,start+1);
							seconds=substr($0,start+6,start+1);
							millis=substr($0,start+9,start+2);
						fullMatch1=hours":"minutes":"seconds"."millis
							matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
			}else{
					regex="[0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
					if(match($0,regex)) {
							start = RSTART;
							size = RLENGTH;
							hours=substr($0,start,start);
							minutes=substr($0,start+2,start+1);
							seconds=substr($0,start+5,start+1);
							millis=substr($0,start+8,start+2);
							fullMatch1=hours":"minutes":"seconds"."millis
							matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
						}
			}
					
			}

		END {
			print matched;
		}
	'`

	echo "Start time in millis: ${START_MILLIS}"

	###############
	#GET END TIME
	###############

	local SEARCH
	case $APP_NAME in
		cognos)
			SEARCH="The dispatcher is ready to process requests."
			;;
		*)
			SEARCH="Application ${APP_NAME} started in"
			;;
	esac
	echo "Search string: ${SEARCH}"
	local END_TIME=$(awk ' /'"${SEARCH}"'/ { print $2 }' trace.log)

	echo "End time: ${END_TIME}"


	local END_MILLIS=$(awk '
		BEGIN {
			# idealy use this regex, but requires gawk v4+
			#regex="[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}"
			regex="[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
			
		}
		/'"${SEARCH}"'/{
			if(match($0,regex)) {
							start = RSTART;
							size = RLENGTH;
							hours=substr($0,start,start+1);
							minutes=substr($0,start+3,start+1);
							seconds=substr($0,start+6,start+1);
							millis=substr($0,start+9,start+2);
						fullMatch1=hours":"minutes":"seconds"."millis
							matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
			}else{
					regex="[0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
					if(match($0,regex)) {
							start = RSTART;
							size = RLENGTH;
							hours=substr($0,start,start);
							minutes=substr($0,start+2,start+1);
							seconds=substr($0,start+5,start+1);
							millis=substr($0,start+8,start+2);
							fullMatch1=hours":"minutes":"seconds"."millis
							matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
						}
			}
			
		}
		END {
			print matched;
		}
	' trace.log)

	echo "End time in millis: ${END_MILLIS}"

	echo ""

	###############
	#GET ELAPSED TIME
	###############

	# In case the end time is past midnight, add 24 hours then take the % remainder
	#	Have to use awk because maths in bash is rubbish
	local ELAPSED=`echo ${END_MILLIS} ${START_MILLIS} | awk '
		{
			print ( ( ($1 + (24*3600*1000)) - $2 ) % (24*3600*1000) )
		}
	'`

	# if no end time is found, the awk above will always return the start time,
	#	 since it takes start to be $1 instead of $2 when end time is missing

	if [ "${xjitMode}" = "true" ]; then
		echo "Startup time for the run using Xjit:verbose was ${ELAPSED}"
	else
		if [ "${ELAPSED}" = "${START_MILLIS}" ] || [ "${ELAPSED}" = "${END_MILLIS}" ]; then
			echo "An error occurred while trying to calculate startup time. Failing run"
			export FAILED="true"
		else
			if [ "$5" = "NoPrint" ]; then
				export SUTime=${ELAPSED}
			else
				echo "Startup time: ${ELAPSED}"
			fi
		fi
	fi


	###############
	#WAS GET START TIME
	###############

	#This is now the WAS Method of recording startup. Added as a test.

	if [ "${APP_NAME}" = "cognos" ]; then
		echo "WAS Perf measurement not relevant to Cognos. Exiting"

	else


		SEARCH="The server ${SERVER_NAME} has been launched"
		echo "Search string: ${SEARCH}"
		START_TIME=$(awk ' /'"${SEARCH}"'/ { print $2 }' trace.log)

		echo "START time: ${START_TIME}"


		START_MILLIS=$(awk '
			BEGIN {
				# idealy use this regex, but requires gawk v4+
				#regex="[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}"
				regex="[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
				
			}
			/'"${SEARCH}"'/{
				if(match($0,regex)) {
								start = RSTART;
								size = RLENGTH;
								hours=substr($0,start,start+1);
								minutes=substr($0,start+3,start+1);
								seconds=substr($0,start+6,start+1);
								millis=substr($0,start+9,start+2);
							fullMatch1=hours":"minutes":"seconds"."millis
								matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
				}else{
						regex="[0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
						if(match($0,regex)) {
								start = RSTART;
								size = RLENGTH;
								hours=substr($0,start,start);
								minutes=substr($0,start+2,start+1);
								seconds=substr($0,start+5,start+1);
								millis=substr($0,start+8,start+2);
								fullMatch1=hours":"minutes":"seconds"."millis
								matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
							}
				}
			}
			END {
				print matched;
			}
		' trace.log)

		echo "Start time in millis: ${START_MILLIS}"

		###############
		#WAS GET END TIME
		###############

		SEARCH="The server ${SERVER_NAME} is ready to run a smarter planet"
		echo "Search string: ${SEARCH}"
		END_TIME=$(awk ' /'"${SEARCH}"'/ { print $2 }' trace.log)
		
		if [ -z "$END_TIME" ]; then
		    echo "END_TIME is NULL. Couldn't find ${SEARCH}."
		    
		    #Open Liberty 19.0.0.4 uses this new string. 18.0.0.4 and before used the old one.
		    SEARCH="The ${SERVER_NAME} server is ready to run a smarter planet"
		    echo "Search new string: ${SEARCH}"
		    END_TIME=$(awk ' /'"${SEARCH}"'/ { print $2 }' trace.log)
		fi
		
		echo "End time: ${END_TIME}"


		END_MILLIS=$(awk '
			BEGIN {
				# idealy use this regex, but requires gawk v4+
				#regex="[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}"
				regex="[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
				
			}
			/'"${SEARCH}"'/{
				if(match($0,regex)) {
								start = RSTART;
								size = RLENGTH;
								hours=substr($0,start,start+1);
								minutes=substr($0,start+3,start+1);
								seconds=substr($0,start+6,start+1);
								millis=substr($0,start+9,start+2);
							fullMatch1=hours":"minutes":"seconds"."millis
								matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
				}else{
						regex="[0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]"
						if(match($0,regex)) {
								start = RSTART;
								size = RLENGTH;
								hours=substr($0,start,start);
								minutes=substr($0,start+2,start+1);
								seconds=substr($0,start+5,start+1);
								millis=substr($0,start+8,start+2);
								fullMatch1=hours":"minutes":"seconds"."millis
								matched = 0 + millis + (seconds * 1000) + (minutes * 1000 * 60) + (hours * 1000 * 60 * 60);
							}
				}
			}
			END {
				print matched;
			}
		' trace.log)

		echo "End time in millis: ${END_MILLIS}"

		echo ""

		###############
		#WAS GET ELAPSED TIME
		###############

		# In case the end time is past midnight, add 24 hours then take the % remainder
		#	Have to use awk because maths in bash is rubbish
		ELAPSED=`echo ${END_MILLIS} ${START_MILLIS} | awk '
			{
				print ( ( ($1 + (24*3600*1000)) - $2 ) % (24*3600*1000) )
			}
		'`

		# if no end time is found, the awk above will always return the start time,
		# since it takes start to be $1 instead of $2 when end time is missing

		if [ "${ELAPSED}" = "${START_MILLIS}" ] || [ "${ELAPSED}" = "${END_MILLIS}" ]; then
			echo "An error occurred while trying to calculate startup time. WAS measurement is just for info at the moment"
		else
			if [ "$xjitMode" != "true" ]; then
				echo "Startup time (Launched-Smarter Planet(WAS Perf Measurement): ${ELAPSED}"
			else
				echo "Was Startup time with Xjit:verbose was ${ELAPSED}"
			fi
		fi

		rm -f trace.log
	fi
}


#######################################################################################
#	BEGIN STARTUP TIME AND FOOTPRINT BENCHMARK
#######################################################################################

# Handle any script arguments given when running the benchmark
while [ ! $1 = "" ]; do
    case $1 in
        -help|--help)
            usage
            exit
            ;;
        *)
            echo "Invalid benchmark argument: ${1}"
            usage
            exit
            ;;
    esac
done

BENCHMARK_TYPE=SUFP

# Import the common utilities needed to run this benchmark
. "$(dirname $0)"/common_utils.sh

# Import the common utilities needed to run this benchmark
. "$(dirname $0)"/database_utils.sh

# Print Liberty header for script identification
printLibertyHeader

# Print Startup and Footprint header with script version
echo "Startup and Footprint Benchmark Launch Script for Liberty
###########################################################################

"

# echo the local time, helps match logs up if local time not correct
echo "Local date: `date`"
echo "Local time: $(perl $(dirname $0)/time.pl)"

setMachinePlatform
setBenchmarkTopLevelDir
checkAndSetCommonEnvVars
checkAndSetSUFPEnvVars

###############
# Liberty Host Environment Setup
###############

# remove results files from previous runs
removeAllResultFilesOnHost

# Tmp folder stores any files created during the running of the benchmark
if [ ! -d "${BENCHMARK_DIR}/tmp" ]; then
  mkdir ${BENCHMARK_DIR}/tmp
fi

VALID_SCENARIOS=(
  "AcmeAir"
  "Cognos"
  "DayTrader"
  "DayTrader7"
  "HugeEJB"
  "HugeEJBApp"
  "SmallServlet"
  "TradeLite"
)
scenarioValidation

jdkEnvSetup

if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
  #work out scc name = safer than relying on generating it from build name
  getSharedClassCacheName
  if [ -z "$SCCNAME" ]; then
    case ${SCENARIO} in
      AcmeAir)
        SCCNAME=liberty-acmeair-sufp-${JDK}
        ;;
      Cognos)
        SCCNAME=liberty-cognos-sufp-${JDK}
        ;;
      DayTrader)
        SCCNAME=liberty-dt-sufp-${JDK}
        ;;
      DayTrader7)
        SCCNAME=liberty-dt7-sufp-${JDK}
        ;;
      HugeEJB)
        SCCNAME=liberty-hugeejb-sufp-${JDK}
        ;;
      HugeEJBApp)
        SCCNAME=liberty-hugeejbapp-sufp-${JDK}
        ;;
      SmallServlet)
        SCCNAME=liberty-smallservlet-sufp-${JDK}
        ;;
      TradeLite)
        SCCNAME=liberty-tl-sufp-${JDK}
        ;;
      *)
        SCCNAME=liberty-sufp-${JDK}
        ;;
    esac
    echo "Couldn't work out SCCNAME - setting to ${SCCNAME}"
  else
    echo "SCCNAME worked out to be ${SCCNAME} from given JVM_ARGS"
  fi
  echo "SCCNAME: ${SCCNAME} will be used for destroying and printing the SCC"
fi

setLibertyServerDirs
platformSpecificSetup

# set environment vars for this shell
setLocalEnv
printSensorInfo

###############
# Liberty Server Setup
###############

# perform server setup only if user has requested it
if [ "${NO_SETUP}" != "true" ]; then
  # Only create if it doesn't exist
  createLibertyServer
  scenarioSpecificServerSetup
  # TODO: Once verified that sufp has no need to call setBootstrapPropsLibertyPort as nothing should be listening for any client
else
  echo "No setup - not changing server"
fi

# exit if user requested setup only
if [ "${SETUP_ONLY}" = "true" ]; then
  echo ""
  echo "SETUP_ONLY flag set. Setup complete. Exiting."
  exit
fi

configureDB

terminateRunningJavaProcs

# tprof will be done for Liberty server java process
#if [ ! -z "$PROFILING_TOOL" ]; then
#  addProfilingOptions
#fi

# print liberty and jdk version
printAllApplicationVersionInfo

# display jvm.options
if [ -e ${SERVER_DIR}/jvm.options ]; then
  echo "jvm.options file exists. Contains:"
  cat ${SERVER_DIR}/jvm.options
fi

#start the cognos db service
if [ $SCENARIO = "Cognos" ]; then
  case $PLATFORM in
    CYGWIN)
      echo "Starting Cognos database service"
      NET START "Cognos Content Database"
      ;;
    Linux)
      echo "Starting Cognos database service"
      ${COGNOS_INSTALL_DIR}/bin/derby.sh start 1527 &
      ;;
    AIX)
      echo "Starting Cognos database service"
      ${COGNOS_INSTALL_DIR}/bin/derby.sh start 1527 &
      ;;
    *)
      echo "not sure which OS this is. Exiting"
      exit
      ;;
  esac

  # Wait for cognos to start
  echo "Waiting for log to show Cognos has started."
  echo "Start time : "`date`
  export searchCriteria="The dispatcher is ready to process requests."
  
  #as the log file sits around waiting for the above message, we need to wait for a line to appear (searchCriteria)
  #but we don't want to wait for ever. 2 minutes seems a sensible time at the moment, perhaps for -Xint startup
  #we may need to increase this....
  
  if [ -z $COGNOS_WAIT ]; then
    export waitTime=120
  else
    export waitTime=${COGNOS_WAIT}
  fi
  finishTime=`date +%s`
  export finishTime=`expr ${finishTime} + $waitTime`
  tail -f ${SERVER_DIR}/logs/${TRACE_FILE} | waitForLog
  if [ "$WaitJobFail" = "true" ]; then	
    exit
  else
    echo "Log shows Cognos has started. Current time : "`date`
  fi
fi

###############
# Benchmark Cold Runs
###############

export IS_COLD_RUN="true"

if [ "${PETERFP}" = "true" ]; then
  echo "Enabling footprint tool for all runs"
  export PETERFPAR="true"
fi

if [ "$PLATFORM" = "OS/390" ]; then
  let TIME=$WARM+$COLD
  let TIME=$TIME*60
  INTERVAL=10
  echo "Restarting RMF WITH TIME=${TIME} and INTERVAL=${INTERVAL}"
  restartRMF $TIME $INTERVAL
fi

j=0
while [[ ${j}<${COLD} ]]
do
  # Need to keep --------------------------- for backwards compatibility with perffarm parser (splits it into sections)
  echo "
---------------------------
###############
Cold run ${j}
###############"
  startupFootprint
  if [ "${FAILED}" = "true" ]; then
    echo "Cold run failed. Storing files to remote machine and then exiting"
    moveResultsToRemoteMachine -z
    if [ $SCENARIO = "Cognos" ]; then
      stopCognos
    fi
    exit
  fi
  let j=j+1
done

if [ "${COLD}" -gt 0 ]; then
  echo "All cold runs completed successfully"
fi

###############
#WARMUP RUNS
###############

# export a value to let sufp.sh know this is NOT a cold run
export IS_COLD_RUN="false"

if [ "${PLATFORM}" = "AIX" ]; then
  echo "Platform identified as: ${PLATFORM}. Running remount script"
  `${REMOUNTSCRIPT}`
fi

j=0
while [[ ${j} -lt ${WARMUP} ]]
do
  echo "
---------------------------
###############
Warmup run ${j}
###############"
  startupFootprint
  if [ "${FAILED}" = "true" ]; then
    echo "Warmup run failed. Storing files to remote machine and then exiting"
    moveResultsToRemoteMachine -z
    if [ $SCENARIO = "Cognos" ]; then
      stopCognos
    fi
    exit
  fi
  let j=j+1
done

if [ "${WARMUP}" -gt 0 ]; then
  echo "All warmup runs completed successfully"
fi

###############
#WARM RUNS
###############

# export a value to let sufpdt.sh know this is NOT a cold run
export IS_COLD_RUN="false"

j=0
while [[ ${j} -lt ${WARM} ]]
do
  echo "
---------------------------
###############
Warm run ${j}
###############"
  startupFootprint
  if [ "${FAILED}" = "true" ]; then
    echo "Warm run failed. Storing files to remote machine and then exiting"
    moveResultsToRemoteMachine -z
    if [ $SCENARIO = "Cognos" ]; then
      stopCognos
    fi
    exit
  fi
  let j=j+1
done

if [ "${WARM}" -gt 0 ]; then
  echo "All warm runs completed successfully"
fi

###############
#XJIT RUN
###############

testXjit=true

echo $JDK_OPTIONS|grep Xint 2> /dev/null 1> /dev/null
if [ $? -eq 0 ];  then
  testXjit=false
  echo "Currently running with -Xint. No point in doing an Xjit run"
fi

if [ "${SDK_SUPPORTS_XJIT}" = "false" ]; then
  testXjit=false
  echo "Currently running a VM that doesn't recognise -Xjit. Not running Xjit run"
fi

if [ "$testXjit" = "true" ]; then
  echo "
---------------------------
###############
Final Run : with -Xjit:verbose
###############"
  startupFootprint Xjit
  if [ "${FAILED}" = "true" ]; then
    echo "The Xjit run failed. Storing files to remote machine and then exiting"
    moveResultsToRemoteMachine -z
    if [ $SCENARIO = "Cognos" ]; then
      stopCognos
    fi
    exit
  fi
  echo "All Xjit runs completed successfully"
fi

echo ""
echo "Startup and Footprint Benchmark has completed successfully"

###############
#CLEANUP
###############

if [ "$SCENARIO" = "Cognos" ]; then
  stopCognos
fi

moveResultsToRemoteMachine -z
# TODO: Once verified, check for successful transfer to remote before deleting files on host
removeAllResultFilesOnHost
