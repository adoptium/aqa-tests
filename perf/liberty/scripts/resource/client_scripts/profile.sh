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

#sh profile.sh ${WAS_HOST} ${MEASURE_TIME} ${WORKDIR}

# For profiling, make sure to run STAF or Jenkins Daemon with root permissions
# For tprof, we expect `setrunenv` file to be sourced somewhere in the bash startup scripts
# so that we have all the libraries and other things available in the right paths
# For perf, make sure that it's installed on the machine

# Instructions
# *** PERF INSTRUCTIONS ***
# To run the profiling tool: perf you need to install perf, for more information consult https://perf.wiki.kernel.org/index.php/Tutorial
# Specify perf record or perf stat in PROFILING_TOOL
# Params for perf: PROFILING_SLEEP_TIME -> Time the perf tool will wait before starting, set this equal to warmup time of benchmark
# PERF_EVENTS to record. The default is cycles. Please refer to https://perf.wiki.kernel.org/index.php/Tutorial for more events
# PERF_SAMPLING_PERIOD. Default 1000. Again if you wish to change it please refer to the tutorial or leave as is.
# PROFILING_PROFILE_TIME how long perf will run. Set to how how you want perf to collect data for.
# You may notice perf has a function called perf archive, however this has been deprecated in recent versions and thus unreliable
# This code contains an implementation of it and should archive data files properly allowing it to be read on a separate machine
# Results: to read the perf.data, unzip the perf_debug.tar.gz file and copy the perf-xxxx.map to /tmp on your machine
# Copy the .debug folder from within this zip file <UnzippedFileName>/home/<ProfileMachineUserName> and place it in your own ~/.debug
# Perf report can now be used to view symbols
# Recommended settings perf report --sort=dso,symbol

# *** JPROF INSTRUCTION ***
# Ensure jprof is installed on system, https://github.ibm.com/runtimes/performanceinspector/tree/standalone
# To run a specific profiler in JPROF the following can be set in PROFILING_TOOL: jprof tprof, jprof scs, jprof callflow, jprof rtarcf, jprof calltree
# All instructions are present in the github link provided

# Further instructions present in https://ibm.ent.box.com/s/qxl8gs3t9apmm797ary1pw7ffqbm6qdp

addJprofToPath () {
    export JPROF_DIR="/opt/performanceinspector-standalone/Dpiperf"
    export PATH=$PATH:${JPROF_DIR}/bin
    export LD_LIBRARY_PATH=${JPROF_DIR}/lib:$LD_LIBRARY_PATH:
    
    CURRENT_HOST=`hostname | cut -f1 -d'.'`
    if [[ $HOST != *$CURRENT_HOST* ]]; then
        echo "profile.sh is being run from client machine"
        STAF_START="STAF ${HOST} PROCESS START SHELL COMMAND export PATH=$PATH:${JPROF_DIR}/bin && export LD_LIBRARY_PATH=${JPROF_DIR}/lib:$LD_LIBRARY_PATH: && "
        STAF_END="WORKDIR ${WORKDIR} STDERRTOSTDOUT RETURNSTDOUT WAIT"
    fi
}

#TODO: Add ability to set Perf variables from the launcher such as events and sampling period
#PERF_EVENTS="cycles,instructions,cache-misses,cache-references,branch-misses,branches,context-switches,page-faults,minor-faults,major-faults,alignment-faults,emulation-faults"

setProfilingOptions () {
    if [ -z "$1" ]
    then
       echo "Please provide a logpath for profiling"
    else
        LOG_DIR=$1
    fi
    if [ -z "$PROFILING_JAVA_OPTION" ]; then
	        echo "Optional PROFILING_JAVA_OPTION not set."

	        if [[ "${PROFILING_TOOL}" = jprof* ]]; then			  
	          export PROFILE_TYPE=$(awk -F " " '{print $2}' <<< ${PROFILING_TOOL})
	          echo "PROFILE_TYPE:${PROFILE_TYPE}"
	          PROFILING_JAVA_OPTION="-agentlib:jprof=${PROFILE_TYPE},logpath=${LOG_DIR}"
	        elif [[ "${PROFILING_TOOL}" = perf* ]]; then
              if [[ $JDK_OPTIONS == *"Xjit"* ]]; then
                    # Replace -Xjit:options with -Xjit:options,perfTool
                    JDK_OPTIONS=`sed "s/-Xjit:\(\S*\)/-Xjit:\1,perfTool/" <<< ${JDK_OPTIONS}`                
              else
                    PROFILING_JAVA_OPTION="-Xjit:perfTool"
              fi
			fi   	        
	        echo "PROFILING_JAVA_OPTION=${PROFILING_JAVA_OPTION}"
	else
	    	echo "Optional PROFILING_JAVA_OPTION is set to ${PROFILING_JAVA_OPTION}"
	        
	        # If there are multiple logpaths, then the last one takes effect. 
	        # We append this logpath so that we know where to copy the files from 
	        # and use them for archiving so that they can be viewed later.
	        if [[ "${PROFILING_TOOL}" = jprof* ]]; then
		        echo "Appending ',logpath=${LOG_DIR}' to PROFILING_JAVA_OPTION"
		        PROFILING_JAVA_OPTION=${PROFILING_JAVA_OPTION}+",logpath=${LOG_DIR}"
		        echo "PROFILING_JAVA_OPTION:${PROFILING_JAVA_OPTION}"
	        fi
	    fi
	addJprofToPath
}

runJprof() {
    if [ "$PROFILING_TOOL" = "jprof tprof" ]; then
        echo "Running jprof tprof in directory"
        cd ${WORKDIR}
        echo `pwd`
        echo "${STAF_START}run.tprof -s ${PROFILING_SLEEP_TIME} -r ${PROFILING_PROFILE_TIME} ${STAF_END}"
        ${STAF_START}run.tprof -s ${PROFILING_SLEEP_TIME} -r ${PROFILING_PROFILE_TIME} ${STAF_END}   
    elif [[ "${PROFILING_TOOL}" = "jprof scs" || "${PROFILING_TOOL}" = "jprof callflow" || "${PROFILING_TOOL}" = "jprof calltree" || "${PROFILING_TOOL}" = "jprof rtarcf" ]]; then
        echo "Running $PROFILING_TOOL"
        ${STAF_START}"rtdriver -l -c start -c end ${PROFILING_PROFILE_TIME}" ${STAF_END}
    fi
}

runPerf() {
    if [ "$PROFILING_TOOL" = "perf stat" ]; then
        echo "Running perf stat"
        sleep ${PROFILING_SLEEP_TIME}
        ${STAF_START}perf stat -o ${WORKDIR}/perf_stat.txt -e ${PERF_EVENTS} -- sleep ${PROFILING_PROFILE_TIME} ${STAF_END}
    elif [ "$PROFILING_TOOL" = "perf record" ]; then
        echo "Running perf record"
        sleep ${PROFILING_SLEEP_TIME}
        echo "${STAF_START}perf record -o ${WORKDIR}/perf.data -a -e ${PERF_EVENTS} -c ${PERF_SAMPLING_PERIOD} -- sleep ${PROFILING_PROFILE_TIME} ${STAF_END}"
        ${STAF_START}perf record -o ${WORKDIR}/perf.data -a -e ${PERF_EVENTS} -c ${PERF_SAMPLING_PERIOD} -- sleep ${PROFILING_PROFILE_TIME} ${STAF_END}
    fi    
}

# The benchmark calling the runProfile function must set these 4 parameters in the following order
# HOST machine the benchmark is running on
# PROFILING_PROFILE_TIME how long to run profiling
# WORKDIR working directory of benchmark, required to point logpath to
# PROFILING_SLEEP_TIME how long to wait before starting profiling
runProfile () {
    HOST=$1
    PROFILING_PROFILE_TIME=$2
    WORKDIR=$3
    PROFILING_SLEEP_TIME=$4

    #Setting defaults for PERF_EVENTS, PERF_SAMPLING_PERIOD and PROFILING_SLEEP_TIME
    if [ -z "$PERF_EVENTS" ]; then
        PERF_EVENTS="cycles"
    fi
    
    if [ -z "$PERF_SAMPLING_PERIOD" ]; then
        PERF_SAMPLING_PERIOD="1000"
    fi

    if [ -z "$PROFILING_SLEEP_TIME" ]; then
        PROFILING_SLEEP_TIME="0"
    fi

    echo "HOST:${HOST} PROFILING_PROFILE_TIME:${PROFILING_PROFILE_TIME} WORKDIR:${WORKDIR}"
    echo "PERF_EVENTS:${PERF_EVENTS} PERF_SAMPLING_PERIOD:${PERF_SAMPLING_PERIOD}"

    addJprofToPath
    
    if [[ "${PROFILING_TOOL}" = jprof* ]]; then			  
        runJprof
    elif [[ "${PROFILING_TOOL}" = perf* ]]; then
        runPerf
    fi   	        
}
# First argument is the directory where perf.data is stored, usually the working directory of the running benchmark
zipPerfProfileFiles () {
    if [ "$PROFILING_TOOL" = "perf record" ]; then
        PERFWORKDIR=$1
        PERF_DATA=${PERFWORKDIR}/perf.data

        if [ -z $PERF_BUILDID_DIR ]; then
            PERF_BUILDID_DIR=~/.debug/
        fi  
        # List all the files required to read perf.data on a separate machine
        # -i to specify which perf.data file to read
        # --with-hits returns a list of all the files required to read perf.data
        debug_filelist=`perf buildid-list -i $PERF_DATA --with-hits`
        echo "Required files for perf report"
        echo "$debug_filelist"

        temp_dir="/tmp/perf_debug/"
        mkdir $temp_dir
        # Copy each file in the debug_filelist to the temporary directory
        while read -r line; do
        # Special case for .map file
        if [[ $line =~ .*map* ]]
            then   
            file=`echo "$line" | awk '{print $2}'`
            cp $file $temp_dir 
        else
            file=`echo "$line" | awk '{print $2}' | sed s:^/::`
            cp -r --parents $PERF_BUILDID_DIR$file $temp_dir
        fi
        done <<< "$debug_filelist"

        echo "Copying .buildids"
        # --parents creates the directory chain required. We do not want to copy all the files in this directory
        # example: if we wanted to copy /tmp/data/obj/item.txt, and not copy the entire /tmp/data/obj directory
        # --parents will create the directory /tmp/data/obj and then copy only the item specified
        # only the structure and the files present in debug_filelist
        cp -r --parents $PERF_BUILDID_DIR.build-id $temp_dir

        echo "Zipping up perf_debug"
        tar -C /tmp -czf ${PERFWORKDIR}/perf_debug.tar.gz perf_debug

        echo "Removing files from local tmp"
        rm -rf /tmp/perf_debug

        

    fi
}
	
