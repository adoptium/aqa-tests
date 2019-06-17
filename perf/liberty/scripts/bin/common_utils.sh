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
printLibertyHeader()
{
    local header="
  _     ___ ____  _____ ____ _______   __
 | |   |_ _| __ )| ____|  _ \\_   _\\ \\ / /
 | |    | ||  _ \\|  _| | |_) || |  \\ V / 
 | |___ | || |_) | |___|  _ < | |   | |  
 |_____|___|____/|_____|_| \_\\|_|   |_|                                     

LIBERTY BENCHMARK
"
    printf '%s\n' "$header"
}

#######################################################################################
#	ENVIRONMENT UTILS
#######################################################################################

##
## Identify what platform this benchmark script is currently running on
setMachinePlatform()
{
    printf '%s\n' "
.--------------------------
| Setting Machine Platform
"
    export PLATFORM=`/bin/uname | cut -f1 -d_`
    echo "Platform identified as: ${PLATFORM}"
}

##
## Identify the directory in which the benchmark script is located in
setBenchmarkTopLevelDir()
{
    printf '%s\n' "
.--------------------------
| Setting Benchmark Top Level Directory
"
    if [ -z "$PLATFORM" ]; then
        echo "Machine platform not set. Terminating."
        exit
    fi
    
    # find out the absolute path to the scripts
    local REL_DIR=$(dirname $0)
    BENCHMARK_DIR=`cd "${REL_DIR}"/..; pwd`

     # handle special case for windows
    if [ "$PLATFORM" = "CYGWIN" ]; then
        BENCHMARK_DIR=$(cygpath -m $BENCHMARK_DIR)
    fi
    
    echo "BENCHMARK_DIR=${BENCHMARK_DIR}"
}

##
## Environment variable setup common to all Liberty benchmarks
checkAndSetCommonEnvVars()
{
    printf '%s\n' "
.--------------------------
| Common Environment Setup
"
    if [ -z "$VERBOSE_MODE" ]; then
        echo "VERBOSE_MODE not set. Defaulting to true"
        VERBOSE_MODE="true"
    fi

    if [ -z "$JDK_DIR" ]; then
        echo "JDK_DIR not set"
        usage
        exit
    fi

    if [ -z "$JDK" ]; then
        echo "JDK not set"
        usage
        exit
    fi

    if [ -z "$JDK_OPTIONS" ]; then
        echo "JDK_OPTIONS not set"
        exit
    fi

    #warn about no affinity, but continue anyway
    if [ -z "$AFFINITY" ]; then
        case $PLATFORM in
            OS/390)
                echo "AFFINITY not set. On z/OS so is ok"
                ;;
            SunOS|HP-UX)
                echo "AFFINITY not set. On Sun/HP so is ok"
                ;;
            *)
                echo "AFFINITY not set"
                usage
                exit
                ;;
        esac
    fi
    
    if [ ! -z "$PROFILING_TOOL" ]; then
    
	    if [ -z "$PROFILING_JAVA_OPTION" ]; then
	        echo "Optional PROFILING_JAVA_OPTION not set."
			
			if [[ "${PROFILING_TOOL}" = jprof* ]]; then			  
			  export PROFILE_TYPE=$(awk -F " " '{print $2}' <<< ${PROFILING_TOOL})
			  echo "PROFILE_TYPE:${PROFILE_TYPE}"
			  PROFILING_JAVA_OPTION="-agentlib:jprof=${PROFILE_TYPE},logpath=${BENCHMARK_DIR}/tmp"
			elif [[ "${PROFILING_TOOL}" = perf* ]]; then
		 	  PROFILING_JAVA_OPTION="-Xjit:perfTool"
			fi   	        
	        echo "PROFILING_JAVA_OPTION=${PROFILING_JAVA_OPTION}"
	    else
	    	echo "Optional PROFILING_JAVA_OPTION is set to ${PROFILING_JAVA_OPTION}"
	        
	        # If there are multiple logpaths, then the last one takes effect. 
	        # We append this logpath so that we know where to copy the files from 
	        # and use them for archiving so that they can be viewed later.
	        if [[ "${PROFILING_TOOL}" = jprof* ]]; then
		        echo "Appending ',logpath=${BENCHMARK_DIR}/tmp' to PROFILING_JAVA_OPTION"
		        PROFILING_JAVA_OPTION=${PROFILING_JAVA_OPTION}+",logpath=${BENCHMARK_DIR}/tmp"
		        echo "PROFILING_JAVA_OPTION:${PROFILING_JAVA_OPTION}"
	        fi
	    fi
    fi
	
    if [ -z "$LAUNCH_SCRIPT" ]; then
        echo "Optional LAUNCH_SCRIPT not set. Setting to 'server'"
        LAUNCH_SCRIPT="server"
    fi

    if [ -z "$LIBERTY_BINARIES_DIR" ]; then
        LIBERTY_BINARIES_DIR="${BENCHMARK_DIR}/libertyBinaries"
        echo "Optional LIBERTY_BINARIES_DIR not set. Setting to '${LIBERTY_BINARIES_DIR}'"
    fi

    if [ -z "$LIBERTY_VERSION" ]; then
        case $PLATFORM in
            OS/390)
                LIBERTY_VERSION="liberty_zos"
                ;;
            *)
                LIBERTY_VERSION="liberty"
                ;;
        esac
        echo "Optional LIBERTY_VERSION not set. Setting to '${LIBERTY_VERSION}'"
    fi

    if [ -z "$LIBERTY_HOST" ]; then
        echo "LIBERTY_HOST not set"
        usage
        exit
    fi

    if [ -z "$SERVER_NAME" ]; then
        echo "SERVER_NAME not set."
        usage
        exit
    fi

    if [ -z "$SCENARIO" ]; then
        echo "SCENARIO not set. Exiting"
        exit
    fi

    # Handle only dt3 for now
    if [ -z "$APP_VERSION" ]; then
        if [[ "${SCENARIO}" = DayTrader* ]] && [[ ! "${SCENARIO}" = DayTrader7 ]]; then
            echo "Daytrader 3 Application Version not set. Setting to daytrader3.0.10.1-ee6-src"
            APP_VERSION="daytrader3.0.10.1-ee6-src"
        fi
    fi

    if [ -z "$RESULTS_DIR" ]; then
        echo "Optional RESULTS_DIR not set. Setting to 'libertyResults'"
        RESULTS_DIR="libertyResults"
    fi

    if [ -z "$RESULTS_MACHINE" ]; then
        echo "Optional RESULTS_MACHINE not set. Setting to 'webber'"
        RESULTS_MACHINE="webber"
    fi

    if [ -z "$ROOT_RESULTS_DIR" ]; then
        echo "Optional ROOT_RESULTS_DIR not set. Setting to '/was_results/libertyResults'"
        ROOT_RESULTS_DIR="/was_results/libertyResults"
    fi

    if [ "${SETUP_ONLY}" = "false" ]; then
        echo "Optional SETUP_ONLY set to false. Proceeding with full run."
    elif [ "${SETUP_ONLY}" = "true" ]; then
        echo "SETUP_ONLY MODE SET. Proceeding with a setup only run."
    else
        echo "Optional SETUP_ONLY not set. Setting to false and proceeding with full run."
        SETUP_ONLY="false"
    fi

    if [ "${NO_SETUP}" = "false" ]; then
        echo "Optional NO_SETUP set to false. Proceeding with liberty server setup before the run. Creating a Liberty server if it doesn't exist and replacing Liberty server default server.xml and bootstrap.properties (if available)"
    elif [ "${NO_SETUP}" = "true" ]; then
        echo "NO_SETUP MODE SET. No liberty server setup will be done before the run. A server will NOT be created if it doesn't exist and the Liberty server default server.xml and bootstrap.properties will NOT be replaced."
    else
        echo "Optional NO_SETUP not set. Proceeding with liberty server setup before the run. Creating a Liberty server if it doesn't exist and replacing Liberty server default server.xml and bootstrap.properties (if available)"
        NO_SETUP="false"
    fi

    # Conflict if both are set to true as there is nothing to do
    if [ "${NO_SETUP}" = "true" ] && [ "${SETUP_ONLY}" = "true" ]; then
        echo "NO_SETUP and SETUP_ONLY both set to true. This is a conflicting option set, run cannot proceed. Exiting"
        exit
    fi

    # if using healthcenter, need to allow more time to write hcd on JVM shutdown
    if [[ `echo ${JDK_OPTIONS} | grep -c "healthcenter"` -gt 0 ]]; then
        echo "Healthcenter in use. Allowing an extra 45 seconds on shutdown"
        export HEALTHCENTER=true
    fi
}

SUPPORTED_PROFILING_TOOLS_LIST=(
    "jprof tprof"
    "jprof scs"
    "jprof callflow"
    "jprof calltree"
    "jprof rtarcf"
    "perf stat"
    "perf record"
)

##
## Check if the given profiling tool is supported
supportedProfilingToolsValidation()
{ 
    printf '%s\n' "
.--------------------------
| Validating Given Profiling Tool for Support
"
	SUPPORTED_PROFILING_TOOLS_OUTPUT="List of Supported Profiling Tools: "
    # check the given profiling tool is found in the supported tools list
    for i in "${SUPPORTED_PROFILING_TOOLS_LIST[@]}"; do
        if [ "${PROFILING_TOOL}" = "${i}" ]; then
            echo "Profiling Tool '${PROFILING_TOOL}' is supported"
            local PROFILING_TOOL_SUPPORTED="true"
            break
        fi
        SUPPORTED_PROFILING_TOOLS_OUTPUT+="'${i}',"
    done
    
    # unsupported profiling tool
    if [ "${PROFILING_TOOL_SUPPORTED}" != "true" ]; then
    	echo "${SUPPORTED_PROFILING_TOOLS_OUTPUT}"
        echo "Profiling Tool '${PROFILING_TOOL}' is not supported. Terminating."
        exit
    fi
}
	
##
## Check if the given scenario is valid
scenarioValidation()
{
    printf '%s\n' "
.--------------------------
| Validating Given Scenario
"

    # check the scenario is recognised
    for i in "${VALID_SCENARIOS[@]}"; do
        if [ "${SCENARIO}" = "${i}" ]; then
            echo "Scenario ${SCENARIO} is valid"
            local VALID_SCENARIO="true"
            break
        fi
    done

    # invalid scenario
    if [ "${VALID_SCENARIO}" != "true" ]; then
        echo "The scenario \"${SCENARIO}\" was not recognised. Terminating."
        exit
    fi
}

##
## Platform dependent checks, setup routines, and environment variables that need to be set
platformSpecificSetup()
{
    printf '%s\n' "
.--------------------------
| Platform Specific Setup
"
    if [ -z "$PLATFORM" ]; then
        echo "Machine platform not set. Terminating."
        exit
    fi

    #Server script should ebcdic of OS390 and ascii for other platforms
    case $PLATFORM in
        OS/390)
            echo "Platform ${PLATFORM} requires the Liberty server tool to be in ebcdic format"
            cat ${LIBERTY_DIR}/bin/server | grep "\r" > /dev/null
            if [[ $? -gt 0 ]]; then
                if [ "${LIBERTY_VERSION}" = "liberty_zos" ]; then
                    echo "Server tool in ${LIBERTY_DIR} is not in ebcdic. This is the default tool, so something has gone wrong with copying the scripts. Exiting"
                    exit
                else
                    echo "Server tool in ${LIBERTY_DIR} is not in ebcdic. This will not work. Please provide a valid Liberty binary. Exiting"
                    exit
                fi
            else
                echo "Server tool in ${LIBERTY_DIR} is in valid ebcdic format. Proceeding with benchmark run"
            fi
            ;;

        *)
            echo "Platform ${PLATFORM} requires the Liberty server tool to be in ascii format"
            cat ${LIBERTY_DIR}/bin/server | grep "\r" > /dev/null
            if [[ $? -gt 0 ]]; then
                if [ "${LIBERTY_VERSION}" = "liberty" ]; then
                    echo "Server tool in ${LIBERTY_DIR} is not in ascii. This is the default tool, so something has gone wrong with copying the scripts. Fail job."
                    exit
                else
                    echo "Server tool in ${LIBERTY_DIR} is not in ascii. This will not work. Restoring to liberty folder."
                    export LIBERTY_VERSION="liberty"
                    setLibertyServerDirs
                fi
            else
                echo "Server tool in ${LIBERTY_DIR} is in a valid ascii format. Proceeding with benchmark run"
            fi
            ;;
    esac

    # HP-UX: Hostname to ip conversion for staf
    # STAF on HP-UX has some problem looking up our private hostnames from /etc/hosts, so
    # below is a hack to convert the machine names to IP addresses. Note that this requires
    # STAF on the other machines trust the HP machine's private IP address too.
    if [ "$PLATFORM" = "HP-UX" ] && [ "$BENCHMARK_TYPE" = "THROUGHPUT" ]; then
        echo "Converting private hostnames to IP addresses for HP-UX"
        echo "CLIENT = $CLIENT"
        echo "DB_MACHINE = $DB_MACHINE"
        echo "LIBERTY_HOST = $LIBERTY_HOST"
        CLIENT=`nslookup $CLIENT | grep Address | tail -n 1 | cut -d' ' -f3`
        DB_MACHINE=`nslookup $DB_MACHINE | grep Address | tail -n 1 | cut -d' ' -f3`
        LIBERTY_HOST=`nslookup $LIBERTY_HOST | grep Address | tail -n 1 | cut -d' ' -f3`
        RESULTS_MACHINE=`nslookup $RESULTS_MACHINE | grep Address | tail -n 1 | cut -d' ' -f3`
        echo "CLIENT = $CLIENT"
        echo "DB_MACHINE = $DB_MACHINE"
        echo "LIBERTY_HOST = $LIBERTY_HOST"
        echo "RESULTS_MACHINE = $RESULTS_MACHINE"
    fi
}

##
## Set the enviornment variables for the Liberty directory and Liberty Server directories
## If no Liberty directory is found, try finding an archive of the same name in LIBERTY_BINARIES_DIR and extract it
## Benchmark will terminate if no Liberty directory or corresponding archive is found
setLibertyServerDirs()
{
    printf '%s\n' "
.--------------------------
| Setting Liberty Directories
"
    # Remove "@" from SERVER_NAME - liberty doesn't like this
    export SERVER_NAME=`echo "${SERVER_NAME}" | sed 's/@//g'`
    echo "SERVER_NAME=${SERVER_NAME}"

    if [ ! -d "${LIBERTY_BINARIES_DIR}" ]; then
        echo "Liberty Binaries Directory does not exist. Exiting"
        exit
    fi
    
    LIBERTY_DIR="${LIBERTY_BINARIES_DIR}/${LIBERTY_VERSION}"

    # Check if liberty folder exists, otherwise extract it if the archive is available
    if [ -d "${LIBERTY_DIR}" ]; then
        echo "Using existing ${LIBERTY_DIR}"
    else
        local FOUND_LIBERTY_ARCHIVE=false
        echo "${LIBERTY_VERSION} directory not found in ${LIBERTY_BINARIES_DIR}"
        echo "Looking for ${LIBERTY_VERSION} archive in ${LIBERTY_BINARIES_DIR}"

        # Search for and extract an existing archive matching given LIBERTY_VERSION
        case $PLATFORM in
            OS/390)
                # Handle PAX.Z files
                if [ -e "${LIBERTY_DIR}.pax.Z" ]; then
                    echo "Found ${LIBERTY_DIR}.pax.Z"
                    echoAndRunCmd "cd ${LIBERTY_BINARIES_DIR}"
                    echoAndRunCmd "pax -r -f ${LIBERTY_VERSION}.pax.Z"
                    echoAndRunCmd "cd -"
                    FOUND_LIBERTY_ARCHIVE=true
                fi
                ;;
            *)
                # Handle ZIP files
                if [ -e "${LIBERTY_DIR}.zip" ]; then
                    echo "Found ${LIBERTY_DIR}.zip"
                    echoAndRunCmd "cd ${LIBERTY_BINARIES_DIR}"
                    echoAndRunCmd "unzip -q ${LIBERTY_VERSION}.zip"
                    echoAndRunCmd "cd -"
                    FOUND_LIBERTY_ARCHIVE=true

                # Handle TAR files
                elif [ -e "${LIBERTY_DIR}.tar.gz" ]; then
                    echo "Found ${LIBERTY_DIR}.tar.gz"
                    echoAndRunCmd "cd ${LIBERTY_BINARIES_DIR}"
                    echoAndRunCmd "tar -xzf ${LIBERTY_VERSION}.tar.gz"
                    echoAndRunCmd "cd -"
                    FOUND_LIBERTY_ARCHIVE=true
                fi
                ;;
        esac

        if [ "$FOUND_LIBERTY_ARCHIVE" = true ]; then
            if [ -d "${LIBERTY_DIR}" ]; then
                echoAndRunCmd "chmod -R 755 ${LIBERTY_DIR}"
                echo "Using extracted ${LIBERTY_DIR}"
            else
                echo "Error extracting ${LIBERTY_VERSION} archive in ${LIBERTY_BINARIES_DIR}. Exiting"
                exit
            fi
        else
            echo "${LIBERTY_VERSION} archive not found in ${LIBERTY_BINARIES_DIR}. Exiting"
            exit
        fi
    fi
    
    echo "LIBERTY_DIR=${LIBERTY_DIR}"
    SERVER_DIR="${LIBERTY_DIR}/usr/servers/${SERVER_NAME}"
    echo "SERVER_DIR=${SERVER_DIR}"
    
    #handle special case for windows
    if [ "$PLATFORM" = "CYGWIN" ]; then
        WIN_SERVER_DIR=$(cygpath -m "${SERVER_DIR}")
        echo "Windows Server Dir is ${WIN_SERVER_DIR}"
    fi

    # Add execute permission to the liberty start script
    echoAndRunCmd "chmod +x ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT}"
}

##
## Determine the correct location of the java executable in the given JDK_DIR/JDK path
## Benchmark will terminate if no java executable is found or if it returns an error on java -version
jdkEnvSetup()
{
    printf '%s\n' "
.--------------------------
| Java Environment Setup
"

    JAVA_PATH=$(dirname `find ${JDK_DIR}/${JDK} \( -name java -o -name java.exe \) -type f | grep -i 'bin/java'| awk '{ print length, $0 }'|sort -n | cut -d " " -f2 | head -n1`)
    if [ $? -gt 0 ]; then
        echo "JDK not found. Terminating."
        exit
    fi
    echo "Java executable found in ${JAVA_PATH}"
    export JAVA_HOME="${JAVA_PATH}/.."

    ${JAVA_HOME}/bin/java -version > /dev/null 2> /dev/null
    if [ $? -eq 0 ]; then
        echo "Found JDK is valid"
    else
        echo "Found JDK but invalid return code received from ${JAVA_HOME}/bin/java -version. Terminating."
        exit
    fi

    # Remove -xdump jdk option for OS/390
    if [ "$PLATFORM" = "OS/390" ]; then
        local newOptions=""
        
        for option in $JDK_OPTIONS; do 
            newOptions="${newOptions} `echo $option|grep -v -- "-Xdump:"`"
        done

        if [ "$JDK_OPTIONS" != "$newOptions" ]; then
            echo "Removed -Xdump options from JDK_OPTIONS, as not supported on z/OS"
            echo "Old JDK Options : ${JDK_OPTIONS}"
            echo "NewJDK Options : ${newOptions}"
            export JDK_OPTIONS="${newOptions}"
        fi
    fi

    # Set corresponding environment variables
    echo "JAVA_HOME=${JAVA_HOME}"
    echo "NOTE: JAVA_HOME is an environment variable also used in the Liberty/bin scripts"
    echo ""
    export JVM_ARGS="${JDK_OPTIONS}"
    echo "JVM_ARGS=${JVM_ARGS}"
    echo "NOTE: JVM_ARGS is an environment variable also used in the Liberty/bin scripts"
    echo ""
    export PID_FILE="${BENCHMARK_DIR}/tmp/server.pid"
    echo "PID_FILE=${PID_FILE}"
    echo "NOTE: PID_FILE is an environment variable also used in the Liberty/bin scripts"
    echo ""
    export LOG_FILE="console.log"
    echo "LOG_FILE=${LOG_FILE}"
    echo "NOTE: LOG_FILE is an environment variable also used in the Liberty/bin scripts. It points to a file that is located inside the Liberty server's logs directory"

    # check SDK for Xjit support
    ${JAVA_HOME}/bin/java -Xjit:verbose -version 2> /dev/null 1> /dev/null
    if [ $? -gt 0 ];  then
        export SDK_SUPPORTS_XJIT=false
        echo "Currently running a VM that doesn't recognise -Xjit"
        echo "All non-Xjit runs will proceed as normal. Xjit runs will be skipped"
    else
        export SDK_SUPPORTS_XJIT=true
    fi
    echo "SDK_SUPPORTS_XJIT=${SDK_SUPPORTS_XJIT}"

    # check SDK for Shared Class Cache support
    ${JAVA_HOME}/bin/java -Xshareclasses:name=pma-test-scc-name -version 2> /dev/null 1> /dev/null
    if [ $? -gt 0 ];  then
        export SDK_SUPPORTS_SCC=false
        echo "Currently running a VM that doesn't recognise -Xshareclasses"
    else
        ${JAVA_HOME}/bin/java -Xshareclasses:name=pma-test-scc-name,destroy 2> /dev/null 1> /dev/null
        export SDK_SUPPORTS_SCC=true
    fi
    echo "SDK_SUPPORTS_SCC=${SDK_SUPPORTS_SCC}"
}

##
## Work out scc name. Safer than relying on generating it from build name
## TODO: Check for Oracle
getSharedClassCacheName()
{
    printf '%s\n' "
.--------------------------
| Getting the Shared Class Cache Name from JVM_ARGS
"
  SCCNAME=`echo "$JVM_ARGS"|sed 's/ /#/g'|tr "#" "\n" |grep Xshareclasses|sed 's/,/#/g'|tr "#" "\n" |grep name|sed 's/name=//g'`
}

##
## Destroy any present Java shared class cache with the same name as SCCNAME and Liberty usr/servers/.classCache
## TODO: Check for Oracle
destroySharedClassCache()
{
    printf '%s\n' "
.--------------------------
| Destroying Shared Class Cache
"
    # No SCCNAME found
    if [ -z "$SCCNAME" ]; then
        echo "SCCNAME has not been set. Exiting"
        exit
    fi

    # Destroy Java shared class cache
    echo "Destroying Shared Class Cache: ${SCCNAME}"
    local CLEAR_SHARED="${JAVA_HOME}/bin/java -Xshareclasses:name=${SCCNAME},destroy"
    echo ${CLEAR_SHARED}
    ${CLEAR_SHARED}

    # Destroy Liberty class cache
    echo "Destroying Liberty Servers Class Cache: ${LIBERTY_DIR}/usr/servers/.classCache"
    local RemoveLibertySCC="${JAVA_HOME}/bin/java -Xshareclasses:cacheDir=${LIBERTY_DIR}/usr/servers/.classCache,destroyAll"
    echo ${RemoveLibertySCC}
    ${RemoveLibertySCC}

    # Destroy cognos specific shared class cache if any
    if [ "${SCENARIO}" = "Cognos" ]; then
        echo "Destroying Shared Class Cache: cognos10%u"
        local CogCLEAR_SHARED="${JAVA_HOME}/bin/java -Xshareclasses:name=cognos10%u,destroy"
        echo ${CogCLEAR_SHARED}
        ${CogCLEAR_SHARED}
    fi
}

##
## Print SCC info
printSharedClassCacheInfo()
{
    printf '%s\n' "
.--------------------------
| Printing shared class cache stats:
"
  echo "${JAVA_HOME}/bin/java -Xshareclasses:name=${SCCNAME},printStats"
  ${JAVA_HOME}/bin/java -Xshareclasses:name=${SCCNAME},printStats
}

##
## Print the Liberty and Java version
printAllApplicationVersionInfo()
{
    printf '%s\n' "
.--------------------------
| Application Version Information
"
  # Liberty version
  echo "Using Liberty Version:"
  ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} version
  echo ""
  # JDK version
  echo "Using Java Version:"
  ${JAVA_HOME}/bin/java -version
}

##
## Set TR_PrintCompStats and TR_PrintCompTime to allow Jit Thread Utilization info
enableJitThreadUtilizationInfo()
{
    printf '%s\n' "
.--------------------------
| Enable JIT Thread Utilization Information
"
  echo "Setting TR_PrintCompStats and TR_PrintCompTime to 1 to allow Jit Thread Utilization info"
  export TR_PrintCompTime=1
  export TR_PrintCompStats=1
}

##
## Unset TR_PrintCompStats and TR_PrintCompTime to disable Jit Thread Utilization info of future Java processes
disableJitThreadUtilizationInfo()
{
    printf '%s\n' "
.--------------------------
| Disable JIT Thread Utilization Information
"
  echo "Unsetting TR_PrintCompStats and TR_PrintCompTime to disable Jit Thread Utilization info of future Java processes"
  unset TR_PrintCompTime
  unset TR_PrintCompStats
}


#######################################################################################
#	ANALYSIS UTILS
#######################################################################################

##
## Calculate a processes memory footprint
## For linux: Footprint =
##   (Processe's Resident Set Size + System Huge Pages in use currently - System Huge Pages in use before given process started)
calculateFootprint()
{
    printf '%s\n' "
.--------------------------
| Calculating Footprint
"
  # add footprint collection to end of Liberty throughput - this is a cut+paste from sufpdt.sh
  case ${PLATFORM} in
    CYGWIN)
      # FULL_MEM=`pslist -m ${WINDOWS_PID} | tail -n 2`
      # echo "${FULL_MEM}"
      # uncomment this line if reporting on pslist throughput
      # FOOTPRINT=`echo "${FULL_MEM}" | tail -n 1 | awk '{ print $4 }'`
      if [ -z ${WINDOWS_PID} ]; then
        echo "No windows PID - not running footprint tool"
      else
        echo "vmmap output:"
        vmmap -64 -PID ${WINDOWS_PID} vmmap.out
        local VMMAP_MEM=`head -n 15 vmmap.out`
        echo "${VMMAP_MEM}"
        rm vmmap.out
        
        local FOOTPRINT=`echo "${VMMAP_MEM}" | head -n 5 | tail -n 1 | awk '{print $5}' | sed "s/,//g"`
        
        echo ""

        if [ "$SCENARIO" = "Cognos" ]; then
          local running_total=$FOOTPRINT
          local Other_PIDS=`ps -W  |grep 'jre/bin/java'|grep -v 'grep'| grep -v '${SERVER_PID}'|awk '{print $1}'`
          echo $Other_PIDS
          ps -W |grep java
          for PID in $Other_PIDS
          do
            local tmpWINDOWS_PID=`ps -W | grep java | grep ${PID} | grep -v grep | awk '{ print $4 }'`
            vmmap -64 -PID ${tmpWINDOWS_PID} tmpvmmap.out
            local tmpVMMAP_MEM=`head -n 15 tmpvmmap.out`
            echo "${tmpVMMAP_MEM}"
            rm tmpvmmap.out
            local tmpFOOTPRINT=`echo "${VMMAP_MEM}" | head -n 5 | tail -n 1 | awk '{print $5}' | sed "s/,//"`
            local tmpProcName=`ps -W |grep ${PID}| grep -v 'grep'|grep -v 'ws-server.jar'`
          
            #some logic should go here
            case $tmpProcName in
              *NetworkServerControl*)
                tmpProcName="Derby DB Server"
                ;;
              *CGSServer*)
                tmpProcName="CGSServer"
                ;;
              *ws-server.jar*)
                tmpProcName="Liberty why is this here!?"
                ;;
              *DQServer*)
                tmpProcName="DQServer"
                ;;
              *)
              ;;
            esac
            echo "${tmpProcName}=${tmpFOOTPRINT}"
            let running_total=$running_total+$tmpFOOTPRINT
          done
        fi

        local command=`tasklist /V /FI "IMAGENAME eq java*"`
        echo "Running : tasklist /V /FI \"IMAGENAME eq java*\""
        echo $command
        echo "Reporting on vmmap data"
      fi
      ;;
      
    AIX)
      procmap ${SERVER_PID} > footprint.tmp
      svmon -P ${SERVER_PID} -O segment=category,unit=KB,mapping=on,format=80 >> footprint.tmp
      awk -f ${BENCHMARK_DIR}/resource/aix_footprint.awk footprint.tmp > footprintAwk.tmp
      echo "ps output for reference" >> footprintAwk.tmp
      ps -T ${SERVER_PID} -o rssize,vsz,comm,pid >> footprintAwk.tmp
      # we want to store the procmap, svmon and awk outputs
      cp footprint.tmp ${BENCHMARK_DIR}/tmp/footprint.out
      cat footprintAwk.tmp >> ${BENCHMARK_DIR}/tmp/footprint.out
      
      # now find the footprint figure from the awk output
      local FOOTPRINT=`awk '/JTC footprint/ { print $8 }' footprintAwk.tmp`
      # TODO: Once verified, remove variables not used
      #local free=`vmstat -l | sed -e 's/  */ /g' |tr ' ' '\n' | tail -n 1 | tail -c 10`
      #active=`vmstat -l | sed -e 's/  */ /g' |tr ' ' '\n' | tail -n 2 | head -n 1| tail -c 10`
      
      if [ "$SCENARIO" = "Cognos" ]; then
        local running_total=$FOOTPRINT
        local Other_PIDS=`ps -ef -o pid,comm,args |grep 'jre/bin/java'|grep -v 'grep'| grep -v 'ws-server.jar'|awk '{print $1}'`
        for PID in $Other_PIDS
        do
          procmap ${PID} > footprint.tmp
          svmon -P ${PID} -O segment=category,unit=KB,mapping=on,format=80 >> footprint.tmp
          awk -f ${BENCHMARK_DIR}/resource/aix_footprint.awk footprint.tmp > footprintAwk.tmp
          echo "ps output for reference" >> footprintAwk.tmp
          ps -T ${PID} -o rssize,vsz,comm,pid >> footprintAwk.tmp
          # we want to store the procmap, svmon and awk outputs
          cp footprint.tmp ${BENCHMARK_DIR}/tmp/${PID}.footprint.out
          cat footprintAwk.tmp >> ${BENCHMARK_DIR}/tmp/${PID}.footprint.out
          local tmpFOOTPRINT=`awk '/JTC footprint/ { print $8 }' footprintAwk.tmp`
          # TODO: Once verified, remove variables not used
          # local tmpfree=`vmstat -l | sed -e 's/  */ /g' |tr ' ' '\n' | tail -n 1 | tail -c 10`
          #ps -ef -o pid,comm,args| grep ${PID} | grep -v 'grep'
          local tmpProcName=`ps -ef -o pid,comm,args |grep ${PID}| grep -v 'grep'|grep -v 'ws-server.jar'`
        
          #some logic should go here
          case $tmpProcName in
            *NetworkServerControl*)
              tmpProcName="Derby DB Server"
              ;;
            *CGSServer*)
              tmpProcName="CGSServer"
              ;;
            *ws-server.jar*)
              tmpProcName="Liberty why is this here!?"
              ;;
            *DQServer*)
              tmpProcName="DQServer"
              ;;
            *)
              ;;
          esac
          echo "${tmpProcName}=${tmpFOOTPRINT}"
          let running_total=$running_total+$tmpFOOTPRINT
        done
      fi

      local processOutput=`ps -ef | grep java`
      echo "Listing Process CPU usage"		
      echo $processOutput
      ;;

    SunOS)
      local FOOTPRINT=`/usr/bin/pmap ${SERVER_PID} 2>/dev/null | /usr/bin/tail -1 | /usr/bin/awk '{ print $2 }'`
      ;;

    OS/390)
      echo "Sleeping 10 seconds to make sure we get RMF data"
      sleep 10
      if [ "${IS_COLD_RUN}" = "true" ] || [ "${CLEAN_RUN}" = "true" ];then
        local RUN_TYPE="cold"
      else
        local RUN_TYPE="warm"
      fi
      echo "using RMF ID =  ${RMF_ID}"

      local ID=`date +%H%M%S`
      mkdir ${BENCHMARK_DIR}/tmp/rmfLogs

      echo "SDSF.rexx output active rmf ${RMF_ID} > ${BENCHMARK_DIR}/tmp/rmfLogs/file.txt${ID}"

      local var=`SDSF.rexx output active rmf ${RMF_ID} > ${BENCHMARK_DIR}/tmp/rmfLogs/file.txt${ID}`
      echo $var
      echo "Using file file.txt${RUN_ID}"
      local TIME_STAMP=$(getStartTimeStamp ${SERVER_DIR}/logs/messages.log daytrader3)

      echo "Contents of rmflog:"
      echo "Running cat ${BENCHMARK_DIR}/tmp/rmfLogs/file.txt${RUN_ID} | grep ${PROCESS_NAME}"
      cat ${BENCHMARK_DIR}/tmp/rmfLogs/file.txt${RUN_ID} | grep ${PROCESS_NAME}

      echo "perl rmfLogStdOut.pl ${BENCHMARK_DIR}/tmp/rmfLogs file.txt${ID} ${BENCHMARK_DIR}/tmp/rmfLogs log.out${RUN_TYPE}${j} ${PROCESS_NAME} ${TIME_STAMP}"

      var=`perl rmfLogStdOut.pl ${BENCHMARK_DIR}/tmp/rmfLogs file.txt${ID} ${BENCHMARK_DIR}/tmp/rmfLogs log.out${RUN_TYPE}${j} ${PROCESS_NAME} ${TIME_STAMP}`
      local FOOTPRINT=`echo "$var"|grep "Control_Real_Storage"|awk {'print $3'}`
      FILES_TO_STORE="${FILES_TO_STORE} ${BENCHMARK_DIR}/tmp/rmfLogs/file.txt${RUN_ID} ${BENCHMARK_DIR}/tmp/rmfLogs/log.out${RUN_TYPE}${j}"
      echo "$var"
      ;;

    *)
      local FOOTPRINT=`ps -p ${SERVER_PID} -o rss,vsz,comm,pid | tail -n 1 | awk '{ print $1 }'`
      local HPTOTAL=`cat /proc/meminfo | grep ^HugePages_Total | sed 's/HugePages.*: *//g' | head -n 1`
      local HPFREE=`cat /proc/meminfo | grep ^HugePages_Free | sed 's/HugePages.*: *//g' | head -n 2 | tail -n 1`
      # Once verified, confirm that HPRESVD is not used anywhere
      local HPRESVD=`cat /proc/meminfo | grep ^HugePages_Rsvd | sed 's/HugePages.*: *//g' | head -n 3 | tail -n 1`
      local HPINUSE
      let HPINUSE=$HPTOTAL-$HPFREE-$HPPREINUSE
      echo "HPs in use by liberty: "${HPINUSE}
      local HPSIZE=`cat /proc/meminfo | grep ^Hugepagesize | sed 's/[a-zA-Z :]*//g'`
      let HPSIZE=$HPINUSE*$HPSIZE
      echo "HP size in kb "${HPSIZE}
      let FOOTPRINT=$FOOTPRINT+$HPSIZE

      if [ "$SCENARIO" = "Cognos" ]; then		
        local running_total=$FOOTPRINT
        local Other_PIDS=`ps -o pid,comm,args |grep 'jre/bin/java'|grep -v 'grep'| grep -v 'ws-server.jar'|awk '{print $1}'`
        for PID in $Other_PIDS
        do
          local tmpFOOTPRINT=`ps -p ${PID} -o rss,vsz,comm,pid | tail -n 1 | awk '{ print $1 }'`
          #ps -o pid,comm,args| grep ${PID} | grep -v 'grep'
          local tmpProcName=`ps -o pid,comm,args |grep ${PID}| grep -v 'grep'|grep -v 'ws-server.jar'`
          #some logic should go here
          case $tmpProcName in
            *NetworkServerControl*)
              tmpProcName="Derby DB Server"
              ;;
            *CGSServer*)
              tmpProcName="CGSServer"
              ;;
            *ws-server.jar*)
              tmpProcName="Liberty why is this here!?"
              ;;
            *DQServer*)
              tmpProcName="DQServer"
              ;;
            *)
            ;;
          esac
          echo "${tmpProcName} (${PID}) =${tmpFOOTPRINT}"
          let running_total=$running_total+$tmpFOOTPRINT
        done
      fi

      echo "Displaying top output:"
      local command="top -b -n 1 -p ${SERVER_PID}"
      for PID in $Other_PIDS
      do
        command="$command -p ${PID}"
      done		
      echo "Command to run : ${command}"
      ${command}

      #adding sum of smaps Rss values for sanity checking
      echo "RSS from smaps:"
      for PID in $SERVER_PID $Other_PIDS
      do
        local SMAPS_RSS=`grep Rss /proc/${PID}/smaps | awk '{x+=$2} END {print x}'`
        echo "SMAPS_RSS ${PID}: ${SMAPS_RSS}"
        local SMAPS_PSS=`grep Pss /proc/${PID}/smaps | awk '{x+=$2} END {print x}'`
        echo "SMAPS_PSS ${PID}: ${SMAPS_PSS}"
      done
      ;;
  esac

  if [ "$xjitMode" = true ]; then
    echo "XjitFootprint is ${FOOTPRINT}"	
  else
    # footprint must be composed of decimal digits
    if ! [[ "${FOOTPRINT}" =~ ^[0-9]+$ ]] || [ -z "${FOOTPRINT}" ]; then
      echo "Failed to get footprint information."
      echo "Footprint string was: ${FOOTPRINT}"
    else
      echo "Footprint (kb)=${FOOTPRINT}"
    fi
  fi
}

##
## Converts DayTrader start message to a timestamp for RMF
## Expects two parameters:
##   $1 = messages.log file - in ascii
##   $2 = application name to search on
getStartTimeStamp()
{
    printf '%s\n' "
.--------------------------
| Calculating Start Timestamp
"
  local messagesLog=$1
  #TODO: Once verified, unused variable:
  #local appName=$2
  
  #TODO: Once verified, unused variable:
  # local convert=`iconv -fISO8859-1 -tIBM-1047 $messagesLog > ${messagesLog}.ebcdic`
  iconv -fISO8859-1 -tIBM-1047 $messagesLog > ${messagesLog}.ebcdic
  local timeStamp=`cat $messagesLog.ebcdic | grep "Application $2 started in"|awk {'print $2'}`
  local split=`echo $timeStamp | sed 's/:/ /g'`
  local total=0
  local multiply=3600
  for part in $split
  do
    total=`echo "$total+($part*$multiply)"|bc`
    multiply=`echo "$multiply/60"|bc`
    if [[ $multiply -le 1 ]]; then
      break
    fi
  done
  local TIME_STAMP=$total
  echo $TIME_STAMP
}

##
## Add the jprof/perf jvm option for runtime analysis
## Note: Order of precedence (from highest/rightmost to lowest/leftmost) given within Liberty "Launch Script" (Server):
##   OPENJ9_JAVA_OPTIONS, JVM_ARGS, JVM_OPTIONS_QUOTED (those within the jvm.options file) 
addProfilingOptions()
{
    printf '%s\n' "
.--------------------------
| Configuring Java Options for Profiling. 
| NOTE: Make sure you have the profiling tools on your path.
| Otherwise, the tools will fail to run.
"
    if [ -e ${SERVER_DIR}/jvm.options ]; then
        cp ${SERVER_DIR}/jvm.options ${BENCHMARK_DIR}/tmp/backup_jvmoptions
        rm ${SERVER_DIR}/jvm.options
        export OPTIONS_EXISTED=true
    else
        echo "jvm.options doesn't exist. Creating new file"
        export OPTIONS_EXISTED=false
    fi
    
    if [ ! -z "$PROFILING_TOOL" ]; then
    	echo "Adding ${PROFILING_JAVA_OPTION} to jvm.options"
    	echo "${PROFILING_JAVA_OPTION}" > ${SERVER_DIR}/jvm.options
    fi 
  
}

##
## Calculate huge pages in use before the Liberty server is started
getPreBenchmarkHugePagesInUse()
{
    printf '%s\n' "
.--------------------------
| Pre-Benchmark Huge Pages
"
  case ${PLATFORM} in
    Linux)
      local HPPRETOTAL=`cat /proc/meminfo | grep HugePages_Total | sed 's/HugePages.*: *//g' | head -n 1`
      local HPPREFREE=`cat /proc/meminfo | grep HugePages_Free | sed 's/HugePages.*: *//g' | head -n 2|tail -n 1`
      let HPPREINUSE=$HPPRETOTAL-$HPPREFREE
      echo "HP IN USE : " ${HPPREINUSE}
      ;;
    OS/390)
      #TODO: Once verified, keep this restartRMF enabled or disabled?
      #restartRMF
      echo "RMF restarted already - test"
      ;;
  esac
}


##
## Analyze verbosegc log with the Garbage Collection and Memory Visualizer tool
runGCMVTool()
{
    printf '%s\n' "
.--------------------------
| Running GCMV
"
  if [[ "$GCMV_ENABLED" = "true" ]] && [ -f ${BENCHMARK_DIR}/tools/gcmv/summarizer/scripts/gcmv_summarizer.pl ] && [ -f ${BENCHMARK_DIR}/tools/gcmv/gcmv.jar ] && [ -f ${SERVER_DIR}/verbosegc.xml ]; then
    echo "Found files required for headless GCMV"

    if [ "$PLATFORM" = "OS/390" ]; then
      sh ${BENCHMARK_DIR}/bin/encode.sh -execute ${BENCHMARK_DIR}/tools/gcmv/summarizer/scripts/gcmv_summarizer.pl
      sh ${BENCHMARK_DIR}/bin/encode.sh -execute ${BENCHMARK_DIR}/tools/gcmv/summarizer/lib/GCMV/Common.pm
    fi

    local OUTPUT_FILE=${SERVER_DIR}/gcmv-parser.out
    local ERROR_OUTPUT_FILE=${SERVER_DIR}/gcmv-errors.out

    # Run Headless GCMV against the verbosegclog
    echo "File ${SERVER_DIR}/verbosegc.xml exists" >> ${OUTPUT_FILE} 2>>${ERROR_OUTPUT_FILE}
    echo "" >> ${OUTPUT_FILE} 2>>${ERROR_OUTPUT_FILE}
    echo "Running Headless GCMV" >> ${OUTPUT_FILE} 2>>${ERROR_OUTPUT_FILE}

    echoAndRunCmd "cd ${BENCHMARK_DIR}/tmp"

    case ${PLATFORM} in
      CYGWIN)
        echo "Platform is Cygwin" >> ${OUTPUT_FILE} 2>>${ERROR_OUTPUT_FILE}
        ${JAVA_HOME}/bin/java -jar `cygpath -w ${BENCHMARK_DIR}/tools/gcmv/gcmv.jar` -f `cygpath -w ${SERVER_DIR}/verbosegc.xml` -p  `cygpath -w ${BENCHMARK_DIR}/tools/gcmv/pauseTimeTemplate.epf` >> ${OUTPUT_FILE} 2>>${ERROR_OUTPUT_FILE}
        ;;
      *)
        ${JAVA_HOME}/bin/java -jar ${BENCHMARK_DIR}/tools/gcmv/gcmv.jar -f ${SERVER_DIR}/verbosegc.xml -p ${BENCHMARK_DIR}/tools/gcmv/pauseTimeTemplate.epf >> ${OUTPUT_FILE} 2>>${ERROR_OUTPUT_FILE}
        ;;
    esac

    echoAndRunCmd "cd -"

    # Parse the generated data
    perl -I ${BENCHMARK_DIR}/tools/gcmv/summarizer/lib ${BENCHMARK_DIR}/tools/gcmv/summarizer/scripts/gcmv_summarizer.pl -f ${BENCHMARK_DIR}/tmp/gcmvData.txt >> ${OUTPUT_FILE} 2>>${ERROR_OUTPUT_FILE}
    echo "*********************************" >> ${OUTPUT_FILE}
    echo "* GCMV Summarizer has completed *" >> ${OUTPUT_FILE}
    echo "*********************************" >> ${OUTPUT_FILE}

    #check if ERROR_OUTPUT_FILE is empty
    if [ -f ${ERROR_OUTPUT_FILE} ] && [ `wc -c < ${ERROR_OUTPUT_FILE}` -gt 0 ]; then
      echo "GCMV errors have been found and recorded in ${ERROR_OUTPUT_FILE}" >> ${OUTPUT_FILE}
      FILES_TO_STORE="${FILES_TO_STORE} ${ERROR_OUTPUT_FILE}"
    else
      echo "No GCMV errors have been found." >> ${OUTPUT_FILE}
    fi

    FILES_TO_STORE="${FILES_TO_STORE} ${OUTPUT_FILE}"
    cat ${OUTPUT_FILE}

  else
    echo "Did not find files required for headless GCMV"
    echo "Error: File ${SERVER_DIR}/verbosegc.xml or Headless GCMV installation does not exist"
    echo "Error: Headless GCMV cannot operate"
  fi
}

##
## Analyze the verbosejit files to get JIT CPU utilization stats
calculateJitCpuUtilization()
{
    printf '%s\n' "
.--------------------------
| Calculating JIT CPU Utilization
"
  echo "Displaying JIT CPU Utilization from verbosejit files:"
  if [[ "$JVM_ARGS" = *"%pid" ]]; then
    echo "The vm args seem to contain %pid"
    if [ -e "${SERVER_DIR}/verbosejit_${SERVER_PID}.txt" ]; then
      cat ${SERVER_DIR}/verbosejit_${SERVER_PID}.txt | grep "Time spent in compilation thread"
    else
      echo "I couldnt find the file. I was looking for ${SERVER_DIR}/verbosejit_${SERVER_PID}.txt"
    fi		
  else
    for file in `ls -S ${SERVER_DIR}/verbosejit*.txt*`
    do
      echo "Displaying from ${file}"
      cat $file | grep "Time spent in compilation thread"
    done
  fi
}


#######################################################################################
#	FILE SYSTEM UTILS
#######################################################################################

##
## Use 1: Provide a list of files as arguments
## Use 2: storeFiles -d <directory>
## <directory> can be relative to the working dir or absolute
##
## Note: Use 1 moves files, Use 2 moves the whole directory
##
## Assumes the following are set:
##   SERVER_PID, RESULTS_DIR, SCENARIO, JDK, BENCHMARK_DIR, LIBERTY_HOST, DATE, RUN_ID
storeFiles()
{
    printf '%s\n' "
.--------------------------
| Storing Files to a Local Temporary Directory
"
  if [ -z $1 ]; then
    echo ""
    echo "WARNING in storeFiles function: Please provide at least 1 file to store"
    echo ""
    return
  fi
  
  # remove a pre-pending / from results dir if exists
  RESULTS_DIR=$(echo ${RESULTS_DIR} | sed 's%^/%%')
  
  local DIR=${BENCHMARK_DIR}/tmp/${RESULTS_DIR}/${SCENARIO}/${LIBERTY_HOST}/${JDK}/${RUN_DATE}/${RUN_ID}
  
  # check if the directory needs creating, or if the name is in use but not a directory
  if [ ! -e "${DIR}" ]; then
    mkdir -p ${DIR}
  elif [ ! -d "${DIR}" ]; then
    echo ""
    echo "ERROR in storeFiles function: Specified path exists but is not a directory: ${DIR}"
    echo "Cannot write to path. Function returning."
    echo ""
    return
  fi
  
  # Handle the -d (directory) option differently to files.
  if [ "$1" = "-d" ] && [ -z $2 ]; then
    echo ""
    echo "WARNING in storeFiles function: Please provide a directory and folder name for the -d option"
    echo ""
    return
    
  # storing a directory
  elif [ "$1" = "-d" ]; then
    echo ""
    echo "Moving $2 to ${DIR}"
    mv $2 ${DIR}
    
  # storing a list of files
  else
    echo ""
    echo "Moving files to ${DIR}"
    
    # loop over the files provided and move them to the local results dir
    for file in $@
    do
      # check if file already exists
      if [ -e "${DIR}/$(basename ${file}).${SERVER_PID}" ]; then
        # TODO: Once verified, test this
        echo "${file} -> ${DIR}/$(basename ${file}).${SERVER_PID}.$(date +'%s')"
        mv ${file} ${DIR}/$(basename ${file}).${SERVER_PID}.$(date +'%s')
      else
        echo "${file} -> ${DIR}/$(basename ${file}).${SERVER_PID}"
        mv ${file} ${DIR}/$(basename ${file}).${SERVER_PID}
      fi
    done
  fi	
}

##
## Zips up the results directory, removes original files then moves the zip to the results directory.
##
## Assumes the following are set:
##   SCENARIO, LIBERTY_HOST, JDK, RUN_DATE, RUN_ID, RESULTS_DIR, BENCHMARK_DIR
zipResults()
{
    printf '%s\n' "
.--------------------------
| Zipping Result Files
"
  # remove a pre-pending / from results dir if exists
  RESULTS_DIR=$(echo ${RESULTS_DIR} | sed 's%^/%%')
  
  local DIR=${BENCHMARK_DIR}/tmp/${RESULTS_DIR}/${SCENARIO}/${LIBERTY_HOST}/${JDK}/${RUN_DATE}/${RUN_ID}
  
  local ZIPNAME="results.zip"
  echo "Creating zip ${ZIPNAME}"
  if [ "$PLATFORM" = "OS/390" ]; then
    echoAndRunCmd "zip -rq ${ZIPNAME} ${DIR}"
  else
    echoAndRunCmd "zip -rjq ${ZIPNAME} ${DIR}"
  fi
  echo "Removing original files"
  # TODO: Once verified, replace wildcard with proper /. to remove ALL files in folder
  echoAndRunCmd "rm -r ${DIR}/*"
  
  echo "Moving zip to results dir"
  echoAndRunCmd "mv ${ZIPNAME} ${DIR}"
}

##
## Copies the contents of this run's results dir to the remote results machine
##
## Assumes the following are set:
##   ROOT_RESULTS_DIR, SCENARIO, LIBERTY_HOST, JDK, RUN_DATE, RUN_ID, RESULTS_MACHINE, PLATFORM
copyResultsToRemoteMachine()
{
    printf '%s\n' "
.--------------------------
| Copying Result Files to Remote Machine
"
  # remove a pre-pending / from results dir if exists
  RESULTS_DIR=$(echo ${RESULTS_DIR} | sed 's%^/%%')
  
  local DIR=${BENCHMARK_DIR}/tmp/${RESULTS_DIR}/${SCENARIO}/${LIBERTY_HOST}/${JDK}/${RUN_DATE}/${RUN_ID}
  
  echo ""
  echo "Storing results remotely on ${RESULTS_MACHINE}"
  
  # If we are on windows, we need to give STAF the windows path, not the cygwin path
  if [ "${PLATFORM}" = "CYGWIN" ]; then
    DIR=$(cygpath -m "${DIR}")
  fi
  
  # create results dir on the results machine (if exists, staf stil returns success)
  local FULL_REMOTE_DIR=${ROOT_RESULTS_DIR}/${SCENARIO}/${LIBERTY_HOST}/${JDK}/${RUN_DATE}/${RUN_ID}

  echoAndRunCmd "STAF ${RESULTS_MACHINE} FS CREATE DIRECTORY ${FULL_REMOTE_DIR} FULLPATH"
  #ssh ${RESULTS_MACHINE_USER}@${RESULTS_MACHINE} "${FULL_REMOTE_DIR}"


  # copy the files to the results machine - while the staf command says copy directory, it copies the 
  #	files in the directory, not the directory itself, into the todirectory location
  
  if [ -f ${DIR}/results.zip ]; then
      echoAndRunCmd "STAF local FS COPY FILE ${DIR}/results.zip TODIRECTORY ${FULL_REMOTE_DIR} TOMACHINE ${RESULTS_MACHINE}"
      #scp ${DIR}/results.zip ${RESULTS_MACHINE_USER}@${RESULTS_MACHINE}:${FULL_REMOTE_DIR}

    elif [ -d $DIR ]; then
      echoAndRunCmd "STAF local FS COPY DIRECTORY ${DIR} TODIRECTORY ${FULL_REMOTE_DIR} TOMACHINE ${RESULTS_MACHINE} RECURSE"
      #scp -r ${DIR} ${RESULTS_MACHINE_USER}@${RESULTS_MACHINE}:${FULL_REMOTE_DIR}
  fi

  echo ""
}

##
## Moves the contents of this run's results dir to the remote results machine
##
## Options:
##   -z    Zips the results before moving.
##
## Assumes the following are set:
##   ROOT_RESULTS_DIR, SCENARIO, LIBERTY_HOST, JDK, RUN_DATE, RUN_ID, RESULTS_MACHINE
moveResultsToRemoteMachine()
{
    printf '%s\n' "
.--------------------------
| Moving Result Files To Remote Machine
"
  if [ "$1" = "-z" ]; then
    zipResults
  fi

  # call the copy results function, then delete the local results
  copyResultsToRemoteMachine
  echo ""
  echo "Removing results from local machine"
  echo "rm -rf ${BENCHMARK_DIR}/tmp/${RESULTS_DIR}/${SCENARIO}/${LIBERTY_HOST}/${JDK}/${RUN_DATE}/${RUN_ID}"
  rm -rf "${BENCHMARK_DIR}/tmp/${RESULTS_DIR}/${SCENARIO}/${LIBERTY_HOST}/${JDK}/${RUN_DATE}/${RUN_ID}"
}

##
## Remove all files created during the run of this benchmark on the host
removeAllResultFilesOnHost()
{
    printf '%s\n' "
.--------------------------
| Removing Result Files from Host
"
  # list of files to remove
  local TMP_FILES=(
      "${BENCHMARK_DIR}/bin/verbosejit*.txt*"
      "${BENCHMARK_DIR}/bin/verbosejit*.xml*"
      "${BENCHMARK_DIR}/bin/verbosegc*.xml*"
    )

  # names of verbosejit files kept in the benchmark bin folder
  local TMP_BIN_FILENAMES=(`ls ${BENCHMARK_DIR}/bin |grep '\<verbosejit.*\.txt\>'`)

  # add all verbosejit files in bin folder to the list of files to remove
  for FILENAME in "${TMP_BIN_FILENAMES[@]}"
  do
    TMP_FILES+=("${BENCHMARK_DIR}/bin/${FILENAME}")
  done

  # Remove files not found in ${BENCHMARK_DIR}/tmp
  for TMPFILE in "${TMP_FILES[@]}"
  do
    if [ -e ${TMPFILE} ]; then
      echo "Removing previous file ${TMPFILE}"
      echoAndRunCmd "rm -f ${TMPFILE}"
    else
      echo "No ${TMPFILE} file to remove"
    fi
  done

  # Remove benchmark tmp directory
  if [ -d "${BENCHMARK_DIR}/tmp" ]; then
    echo "Removing ${BENCHMARK_DIR}/tmp"
    echoAndRunCmd "rm -fR ${BENCHMARK_DIR}/tmp"
  else
    echo "No ${BENCHMARK_DIR}/tmp directory to remove"
  fi
}


#######################################################################################
#	LIBERTY SERVER UTILS
#######################################################################################

##
## Create a liberty server with name being SERVER_NAME
createLibertyServer()
{
    printf '%s\n' "
.--------------------------
| Liberty Server Creation
"
    #if the server doesn't exist, create it
    if [ -d ${SERVER_DIR} ]; then
        echo "Using already existing Liberty server ${SERVER_NAME}"
    else
        echo "Liberty server ${SERVER_NAME} does not exist - creating it"
        echoAndRunCmd "${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} create ${SERVER_NAME}"
    fi
}

##
## Configure the Liberty port in the Liberty server's bootstrap.properties file
setBootstrapPropsLibertyPort()
{
    printf '%s\n' "
.--------------------------
| Setting Liberty Listening Port in bootstrap.properties
"
    # TradeApp, TradeAppDerby and Primitive use the same bootstrap properties for now, so do this here.
    # 	If that changes in the future, copy into the if blocks above and change accordingly
    echo "Setting the following values in bootstrap.properties:"
    echo "LIBERTY_PORT=${LIBERTY_PORT}"

    # convert from ascii for os390
    if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh ${SERVER_DIR}/bootstrap.properties
    fi

    #replace liberty port
    local BOOTSTRAP=`sed "s/LIBERTY_PORT_HERE/${LIBERTY_PORT}/g" ${SERVER_DIR}/bootstrap.properties`

    # check if placeholders replaced correctly
    if [ -n "${BOOTSTRAP}" ]; then
        echo "${BOOTSTRAP}" | tee ${SERVER_DIR}/bootstrap.properties
    else
        echo "Could not match required placeholders in bootstrap.properties. File before sed replace:"
        cat  ${SERVER_DIR}/bootstrap.properties
        exit
    fi

    # convert back to ascii for os390
    if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh -toascii ${SERVER_DIR}/bootstrap.properties
    fi
}

##
## Waits two minutes, then kills tail 
## (if startup hasnt finished in two minutes it probably isn't going to)
# TODO: Once verified if function is actually used
tailSentinal()
{
    printf '%s\n' "
.--------------------------
| Waiting for Tail
"
  local x=0
  while [ $x -le 120 ]
  do
    sleep 1
    x=`expr $x + 1`
    if [ "$foundComplete" = "true" ]; then
      break
    fi
  done
  if [ ! "$foundComplete" = "true" ]; then 
    export WaitJobFail="true"
    echo "killing tail. 2 minutes has elapsed"
    kill $(ps -e | grep tail | awk '{print $1}')
  fi
}

##
## Pipe a file into the function
##
## Assumes the following are set:
##   searchCriteria
## Optional:
##   WaitTimeOut
waitForLog()
{
    printf '%s\n' "
.--------------------------
| Waiting for Log
"
  if [ -z $WaitTimeOut ]; then
    export WaitTimeOut=120
  fi

  if [ $finishTime -le `date +%s` ]; then
    echo "Giving up waiting. Job fail"
    echo "killing tail. 2 minutes has elapsed"
    kill $(ps -e | grep tail | awk '{print $1}')
    export WaitJobFail="true"
    break
  fi

  while read line
    do
      if [ $finishTime -le `date +%s` ]; then
        echo "Giving up waiting. Job fail"
        echo "killing tail. 2 minutes has elapsed"
        kill $(ps -e | grep tail | awk '{print $1}')
        export WaitJobFail="true"
        break
      fi
      if echo $line|grep -q "${searchCriteria}" 
      then
        echo "${searchCriteria} found in line ${line}. Carrying on."
        kill $(ps -e | grep tail | awk '{print $1}')
        export foundComplete=true
        break
      fi
      # TODO: Once verified remove this i
      # i=$(($i + 1))
      sleep 1	
  done
}

##
## Start the liberty server with the given number of attempts
##
## Params:
##   1- Attempts
##   2- OPTIONAL - Attempt number (default: 0) - Internal function use only.
##
## Assumes the following are set:
##   PLATFORM, AFFINITY, LIBERTY_DIR, LAUNCH_SCRIPT, SERVER_NAME, CLEAN, PID_FILE
startLibertyServer()
{
    printf '%s\n' "
.--------------------------
| Starting the Liberty Server
"
    # Number of attempts must be given as first paramter to function
    if [ -z "$1" ]; then
        echo "BENCHMARK SCRIPT ERROR in startLibertyServer function: Must specify number of Liberty server startup attempts as first paramter of function. Exiting"
        exit
    # Number of attempts must be positive
    elif [ ${1} -lt 0 ]; then
        echo "BENCHMARK SCRIPT ERROR in startLibertyServer function: Negative startup attempts given. Must specify a positive number of Liberty server startup attempts as first paramter of function. Exiting"
        exit
    fi

    local NO_ATTEMPTS=$1
    local MAX_WAIT=30
    local WAIT_TIME=5
    local WAIT_TOTAL=0
    local CUR_ATTEMPT=0
    
    while [ ! -s ${PID_FILE} ] && [ ${CUR_ATTEMPT} -lt ${NO_ATTEMPTS} ]; do
        # TODO: Add STARTUP_FOOTPRINT_MODE to env variables
        if [ "${STARTUP_FOOTPRINT_MODE}" = "true" ]; then
            echo "Removing previous trace logs"
            rm -f ${SERVER_DIR}/logs/trace*.log
            rm -f ${SERVER_DIR}/logs/message*.log
        fi

        # Start Liberty server
        echo "Starting server ${SERVER_NAME} (attempt ${CUR_ATTEMPT})"
        LIBERTY_START_TIME=`perl ${BENCHMARK_DIR}/bin/time.pl`
        echo "Current time is: ${LIBERTY_START_TIME}"

        if [ "${STARTUP_FOOTPRINT_MODE}" = "true" ] && [ "$PLATFORM" = "OS/390" ]; then
            echo "${AFFINITY} ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} start ${SERVER_NAME} ${CLEAN}"
            export _CEE_RUNOPTS='HEAPP(ON)'
            export _CEE_RUNOPTS='RPTSTG(ON),RPTOPTS(ON),'$_CEE_RUNOPT
            ${AFFINITY} ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} start ${SERVER_NAME} ${CLEAN}
            unset _CEE_RUNOPTS
        else
            case ${PLATFORM} in
                CYGWIN)
                    # need to use sh to start the script, otherwise the .bat one is launched
                    echo "${AFFINITY} sh ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} start ${SERVER_NAME} ${CLEAN}"
                    ${AFFINITY} sh ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} start ${SERVER_NAME} ${CLEAN}
                    ;;
                *)
                    echo "${AFFINITY} ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} start ${SERVER_NAME} ${CLEAN}"
                    ${AFFINITY} ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} start ${SERVER_NAME} ${CLEAN}
                    ;;
            esac
        fi

        # Wait for the Liberty server to start until MAX_WAIT limit is reached
        while [ ${WAIT_TOTAL} -lt ${MAX_WAIT} ]; do
            sleep ${WAIT_TIME}
            let WAIT_TOTAL=WAIT_TOTAL+WAIT_TIME
            echo "Waited ${WAIT_TOTAL} seconds for Liberty to start"
            if [ -s ${PID_FILE} ]; then
                echo "Liberty started: Found pid file"
                echo "******************************************************
*** NOTE *** Ignore the Jit Thread Utilization info shown above (if any). This is from the Liberty server start process and not the Liberty server itself
******************************************************"
                return
            fi
        done

        echo "!!! ERROR !!! Liberty did not start after waiting ${WAIT_TOTAL} seconds"
        
        # Reset wait variables
        MAX_WAIT=30
        WAIT_TIME=5
        WAIT_TOTAL=0
        CUR_ATTEMPT=0

        # Increment attempt counter
        let CUR_ATTEMPT=${CUR_ATTEMPT}+1

        echo "Sleeping for 60s, then retrying to Liberty start"
        sleep 60
    done

    # Failed to start Liberty server
    echo "Maximum startup attempts have failed. Exiting."
    echo "Storing logs directory"
    storeFiles -d ${SERVER_DIR}/logs
    copyResultsToRemoteMachine
    exit
}

##
## Terminate the running Liberty server with pid being the same as the one contained in PID_FILE
# TODO: Once verified, combine with stop liberty server?
# TODO: Once verified, check with acutal ps command??
terminateRunningLibertyServer()
{
    printf '%s\n' "
.--------------------------
| Forcing Liberty Server Termination
"
  if [ -e ${PID_FILE} ] ; then
    # check that this server is not still running then remove the PID file
    echo "Just making sure the last liberty instance actually terminated"
    echoAndRunCmd "kill -9 `cat ${PID_FILE}`"
    echoAndRunCmd "rm -f ${PID_FILE}"
  else
    echo "No ${PID_FILE} file found: No Liberty server with this pid is running"
  fi
}

##
## Liberty will start with the --clean option removing any Liberty specific cache
setLibertyCleanRunOptions()
{
    printf '%s\n' "
.--------------------------
| Setting Liberty Clean Run Option
"
    echo "CLEAN_RUN=true. Liberty will start with the \"--clean\" option. Liberty's shared class cache will be cleared"
    CLEAN="--clean"
}

##
## Get the pid of the currently running Liberty server
getRunningServerPID()
{
    printf '%s\n' "
.--------------------------
| Calculating Running Liberty Server PID
"
  SERVER_PID=`cat ${PID_FILE}`
  echo "SERVER_PID=${SERVER_PID}"

  case ${PLATFORM} in
    # Get the PID according to zOS
    OS/390)
      PROCESS_NAME=`oeconsol 'D OMVS,U=BENCH'|grep $SERVER_PID|awk {'print $2'}|grep BENCH`
      if [ -z ${PROCESS_NAME} ]; then
        echo "Couldn't get process name. Script is configured to use username BENCH"
      else
        echo "Process name for ${SERVER_PID} is ${PROCESS_NAME}"
      fi
      ;;

    # Get the PID according to Microsoft
    CYGWIN)
      local PS_VERSION=`ps --version|grep cygwin|awk {'print $3'}`
      echo "PS version is ${PS_VERSION}"
      #PS changed returning Cygwin PID & Windows PID on same line sometime between 1.1.1 and 1.7.32
      if [ "$PS_VERSION" = "1.1.1" ]; then
        WINDOWS_PID=`ps -W | grep java | grep ${SERVER_PID} | grep -v grep | awk '{ print $4 }'`
      else
        local WINDOWS_CMD=`ps -W | grep java | grep ${SERVER_PID}| grep -v grep | awk '{ print $8 }'`
        #Get server command for Cygwin process (rather than assuming there's only one JVM running)
        WINDOWS_CMD=`cygpath -w ${WINDOWS_CMD}|sed 's|\\\|\\\\\\\\|g'`
        #Get the Windows path, and pad with extra \'s so grep can use it....
        WINDOWS_PID=`ps -W | grep java|grep "${WINDOWS_CMD}" |awk {'print $4'}`
      fi	
      echo "WINDOWS_PID=${WINDOWS_PID}"
      ;;
    *)
      ;;
  esac
}

##
## Print the currently running Liberty process command
printLibertyServerProcessCommand()
{
    printf '%s\n' "
.--------------------------
| Printing the Liberty Process
"
  # Get the full liberty command line
  # - use wmic for windows otherwise only executable name given, not args
  echo "Liberty command line:"
  case ${PLATFORM} in
    CYGWIN)
      wmic process where\(processid="${WINDOWS_PID}"\) get commandline
      ;;
    *)
      ps -ef | grep java | grep -v grep
      ;;
  esac
}

##
## Stop the liberty server properly via the Liberty stop script.
## Terminate the process if it is still running after trying to stop it.
stopLibertyServer()
{
    printf '%s\n' "
.--------------------------
| Stopping the Liberty Server
"
  # stop the server properly using Liberty's launch script
  case ${PLATFORM} in
    CYGWIN)
      # Need to use the sh script to stop liberty, because sh was used to start
      echoAndRunCmd "sh ${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} stop ${SERVER_NAME}"
      ;;
    *)
      echoAndRunCmd "${LIBERTY_DIR}/bin/${LAUNCH_SCRIPT} stop ${SERVER_NAME}"
      ;;
  esac

  echo "Waiting an additional 10 seconds"
  echoAndRunCmd "sleep 10"

  # wait for healthcenter to finish up
  if [ "$HEALTHCENTER" = "true" ]; then
    echo "Sleeping 45 seconds more for healthcenter to finish writing HCD"
    echoAndRunCmd "sleep 45"
  fi

  # if server is still running, terminate it
  kill -0 ${SERVER_PID} 2>/dev/null
  if [ "$?" = "0" ]; then
    echo "Killing server manually (PID=${SERVER_PID})"
    echoAndRunCmd "kill -9 ${SERVER_PID}"
    echoAndRunCmd "sleep 10"

    # check if the server still exists
    kill -0 ${SERVER_PID} 2>/dev/null
    if [ "$?" = "0" ]; then
      echo "!!! WARNING !!! Server kill failed. Please check machine for PID=${SERVER_PID}"
      # exit early in case this server process affects any iterations left to do
      exit
    else
      echo "Server killed successfully"
    fi
  else
    echo "Server shutdown successful"
  fi
}


#######################################################################################
#	SYSTEM UTILS
#######################################################################################

##
## Sets the following values for a run:
##		RUN_DATE 	- date at time of function call
##		RUN_ID 		- time of function call
##		startRMF	- Command to start RMF on z/OS
##		stopRMF	- Command to stop RMF on z/OS
##		startII	- Command to start RMF II on z/OS
##		modifyII	- Command to modift RMF II on z/OS
##		addrUsage	- Command to get address space usage on z/OS
setLocalEnv()
{
    printf '%s\n' "
.--------------------------
| Setting Local Environment Variables
"
  RUN_DATE=$(date +%y%m%d)
  echo "RUN_DATE=${RUN_DATE}"
  RUN_ID=$(date +%H%M%S)
  echo "RUN_ID=${RUN_ID}"

  # TODO: Once verified, remove this block as it's not used anywhere (could be used in restartRMF as those are hardcoded?)
  if [ "$PLATFORM" = "OS/390" ]; then
    startRMF="s rmf.rmf,reusasid=yes";
    stopRMF="p rmf.rmf";
    startII="f rmf,start II";
    modifyII="F RMF,MODIFY II,SINTV(10S),STOP(90S)";
    addrUsage="F AXR,DISPASID";
  fi
}

##
## Echos the command passed to it if VERBOSE_MODE=true is set in the environment. Then runs the command.
##   Command MUST be passed in quotes.
echoAndRunCmd()
{
  if [ -n "$1" ]; then
    if [ "${VERBOSE_MODE}" = "true" ] || [ "$2" = "-alwaysEcho" ]; then
      echo "$1"
    fi
    $1
    
  fi
  # else just ignore
}

##
## Restart the Resource Measurement Facility for z/OS
restartRMF()
{
    printf '%s\n' "
.--------------------------
| Restarting z/OS Resource Measurement Facility (RMF)
"
  local var=`oeconsol 'F AXR,DISPASID'`
  echo "$var"
  sleep 5
  echo "Stopping RMF"
  var=`oeconsol 'p rmf.rmf'`
  echo "$var"
  sleep 5
  echo "Starting RMF"
  var=`oeconsol 's rmf.rmf,reusasid=yes'`
  echo "$var"
  sleep 5
  echo "Starting II"
  var=`oeconsol 'f rmf,start II'`
  echo "$var"
  sleep 5
  if [ -z "$1" ]; then
    echo "Modifying II"
    var=`oeconsol 'F RMF,MODIFY II,SINTV(10S),STOP(90S)'`
    echo "$var"
    sleep 5
  else
    if [ -z "$2" ]; then
      local INTERVAL=60
    else
      local INTERVAL=$2
    fi
    local TIME=$1
    echo "Modifying II with ${TIME}"
    var=`oeconsol "F RMF,MODIFY II,SINTV(${INTERVAL}S),STOP(${TIME}S)"`
    echo "$var"
    sleep 5
  fi
  var=`SDSF.rexx list active rmf`
  echo $var | grep ACTIVE
  if [[ $? -eq 0 ]]; then
    export RMF_ID=`echo $var|awk {'print $2'}`
    echo "Restarted RMF: ID = ${RMF_ID}"
  fi
}

##
## Helper function for terminateRunningJavaProcs which gets the Java pids to terminate depending on the platform.
## Ignore Jenkins' Java processes.
terminateRunningJavaProcs_getJavaPIDS()
{
    case $PLATFORM in
        AIX)
            ps -ef -o pid,args | grep 'java' | grep -v 'grep' | grep -v 'slave.jar' | grep -v 'remoting.jar' | grep -v 'sufp' | grep -v 'throughput_benchmark' | grep -v 'durable' | awk '{print $1;}'
            ;;
        CYGWIN)
            ps | grep '/java' | grep -v 'grep' | grep -v 'slave.jar' | grep -v 'remoting.jar' | grep -v 'sufp' | grep -v 'throughput_benchmark' | grep -v 'durable' | awk '{print $1;}'
            ;;
        OS/390)
            ps -ef -o pid,args | grep 'java' | grep -v 'grep' | grep -v 'slave.jar' | grep -v 'remoting.jar' | grep -v 'sufp' | grep -v 'throughput_benchmark' | grep -v 'durable' | awk '{print $1;}'
            ;;
        *)
            ps -ef | grep 'java ' | grep -v 'grep' | grep -v 'slave.jar' | grep -v 'remoting.jar' | grep -v 'sufp' | grep -v 'throughput_benchmark' | grep -v 'durable' | awk '{print $2;}'
            ;;
    esac
}

##
## Terminate any running Java processes. Jenkins related Java processes are ignored.
terminateRunningJavaProcs()
{
    printf '%s\n' "
.--------------------------
| Terminating Any Running Java Processes
"

    if [ -z "$PLATFORM" ]; then
        echo "Machine platform not set. Terminating."
        exit
    fi

    echo "Local date: `date`"
    echo "List of Java processes to terminate:"
    local JAVA_PIDS=$(terminateRunningJavaProcs_getJavaPIDS)

    # Try killing them
    if [ -n "$JAVA_PIDS" ]; then
      echo "Killing: $JAVA_PIDS"
      kill $JAVA_PIDS
      echo "Waiting 10s for JVMs to exit"
      sleep 10
    else
      echo "No Java processes to terminate"
      return
    fi

    # Check to see if all JVMs exited
    JAVA_PIDS=$(terminateRunningJavaProcs_getJavaPIDS)

    local MAX_TRIES=5
    local TERMINATION_ATTEMPT=0
    # If not, try kill -9
    while [ -n "$JAVA_PIDS" ] && [[ ${TERMINATION_ATTEMPT} -lt ${MAX_TRIES} ]]; do
      echo "Trying kill -9: $JAVA_PIDS"
      kill -9 $JAVA_PIDS
      echo "`date`: Waiting 10s for JVMs to die"
      sleep 10
      JAVA_PIDS=$(terminateRunningJavaProcs_getJavaPIDS)
      let TERMINATION_ATTEMPT=TERMINATION_ATTEMPT+1
    done

    JAVA_PIDS=$(terminateRunningJavaProcs_getJavaPIDS)

    if [ -n "$JAVA_PIDS" ]; then
        echo "!!! WARNING !!! Could not terminate running Java processes but not exiting."
        
        #TODO: Temporarily commenting exit since the account used with Jenkins 
        #daemon may not have sudo access to kill other processes on Adopt. 
        #Need to request access.
        
        #exit
    fi
}

##
## Prints the thermal information from the sensors program, if the program is installed
printSensorInfo()
{
    printf '%s\n' "
.--------------------------
| Printing Host Machine Sensor Information
"
    which sensors 2>/dev/null
    if [ $? -gt 0 ]; then
        echo "Thermal info not available. Check if the appropriate sensors package is installed on the host machine"
    else
        local thermalInfo=`sensors`
        echo "Current Thermal Info:"
        echo "${thermalInfo}"
    fi
}

# Check if the Liberty server is still running. Terminate the benchmark if it's not
libertyServerRunningCheck()
{
    printf '%s\n' "
.--------------------------
| Checking if Liberty is Still Running
"
    kill -0 ${SERVER_PID} 2>/dev/null
    if [ "$?" != "0" ]; then
      echo "!!! ERROR !!! Looks like Liberty died - giving up."
      exit
    else
      echo "Liberty is still running"
    fi
}