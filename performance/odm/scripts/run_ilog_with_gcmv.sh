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

# Support for CYGWIN environment
UNAME_OUT=`uname`
if [[ ${UNAME_OUT} == *"CYGWIN"* ]]
then
	WORKDIR=`cygpath ${WORKDIR}`
	TPROF_DIR="${WORKDIR}/../../Dpiperf/bin"
	# Declare a list of files that we want to backup after each run of ILOG 
	FILES_LIST="${WORKDIR}/verbosegc*.log ${WORKDIR}/javacore.*.txt ${WORKDIR}/Snap.*.trc ${WORKDIR}/gcmvData.txt ${TPROF_DIR}/log-* ${TPROF_DIR}/tprof.* ${TPROF_DIR}/swtrace.nrm2"
else
	TPROF_DIR="${WORKSPACE}/Dpiperf/bin"
	# Declare a list of files that we want to backup after each run of ILOG
	FILES_LIST="${WORKDIR}/verbosegc*.log ${WORKDIR}/javacore.*.txt ${WORKDIR}/Snap.*.trc ${WORKDIR}/gcmvData.txt ${WORKDIR}/log-* ${WORKDIR}/tprof.* ${WORKDIR}/swtrace.nrm2"
fi

# Declare and initialise STDOUT and STDERR files
ILOG_STDOUT=${WORKDIR}/ILOG_STDOUT.txt
ILOG_STDERR=${WORKDIR}/ILOG_STDERR.txt
echo "" > ${ILOG_STDOUT}
echo "" > ${ILOG_STDERR}

echo "Working directory is = ${WORKDIR}" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
cd ${WORKDIR}

# Configure connection pool size
echo "rulesetUsageMonitorEnabled set to ${RULEUSAGE}" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
if [[ ${RULEUSAGE} == "true" ]]
then
    if [[ ${UNAME_OUT} == *"390"* ]]
    then
    	echo "Running configure.RUMEnabled.ebcdic.sh" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
    	sh configure.RUMEnabled.ebcdic.sh >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	else
    	echo "Running configure.RUMEnabled.sh" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
        sh configure.RUMEnabled.sh >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
    fi
else
    if [[ ${UNAME_OUT} == *"390"* ]]
    then
    	echo "Running configure.ebcdic.sh" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
    	sh configure.ebcdic.sh >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	else
    	echo "Running configure.sh" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
        sh configure.sh >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
    fi
fi

echo "Checking value of rulesetUsageMonitorEnabled property in ra.xml" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
if [[ ${UNAME_OUT} == *"390"* ]]
then
	echo "Detected z/OS" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	iconv -f 819 -t 1047 bin/ra.xml > bin/ra.xml.tmp
	RUMLine=`grep -n "rulesetUsageMonitorEnabled" bin/ra.xml.tmp | awk -F':' '{print $1}'`
	echo "rulesetUsageMonitorEnabled detected on line ${RUMLine}"
	GREPOUTPUT=`sed -n "${RUMLine},$((RUMLine+3))p" bin/ra.xml.tmp`
elif [[ ${UNAME_OUT} == *"AIX"* ]]
then
    echo "Detected AIX" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
    RUMLine=`grep -n "rulesetUsageMonitorEnabled" bin/ra.xml | awk -F':' '{print $1}'`
    echo "rulesetUsageMonitorEnabled detected on line ${RUMLine}"
    GREPOUTPUT=`sed -n "${RUMLine},$((RUMLine+3))p" bin/ra.xml`
else
	GREPOUTPUT=`grep -A 3 "rulesetUsageMonitorEnabled" bin/ra.xml`
fi
echo "${GREPOUTPUT}" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}

# Check if any residual files from a previous run are still in the WORKING DIRECTORY.
# If found, create a directory under "leftOverResults" and move the files
filecount=$(ls ${FILES_LIST} 2> /dev/null | wc -l)

SUFFIX=`date +%d%m%y-%H%M%S`
folderName="leftoverResults/${SUFFIX}"
mkdir -p ${folderName}

if [ ${filecount} -gt 0 ]
then
	echo "================================================"  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "= Status: Trace/Logs found from a previous run ="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "================================================"  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	filelist=$(ls ${FILES_LIST} 2> /dev/null)

	for files in ${filelist}
	do
		echo "Moving ${files} to directory ${folderName}" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
		mv ${files} ${folderName}
	done
else
	echo "No Residual Trace/Logs found"  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
fi

# Dumps will be stored under $PERFFARM_DUMP
#PERFFARM_DUMP=`staf local var list|grep perffarm_dump|awk {'print $3'}` //Want to eliminate STAF
PERFFARM_DUMP="${WORKSPACE}/dump"
if [ ! -d "$PERFFARM_DUMP" ]; then
  mkdir -p $PERFFARM_DUMP
  echo "Created $WORKSPACE/dump directory"
fi


# Collect dumps which are destined for a different target location	
FILES_LIST_DMP="${WORKDIR}/*.dmp"
filecount=$(ls ${FILES_LIST_DMP} 2> /dev/null | wc -l)

if [ ${filecount} -gt 0 ]
then
	echo "==========================================="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "= Status: Dumps found from a previous run ="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "==========================================="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	filelist=$(ls ${FILES_LIST_DMP} 2> /dev/null)
	for files in ${filelist}
	do
		echo "Moving ${files} to directory ${PERFFARM_DUMP}" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
		mv ${files} ${PERFFARM_DUMP} >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	done
else
	echo "No Residual Dumps found"  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
fi

# Run ILOG_WODM
SUFFIX=`date +%d%m%y-%H%M%S`
echo "" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
echo "=======================" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
echo "= Running ${WORKLOAD} =" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
echo "=======================" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
if [ -x "${JDK_DIR}/jre/bin/java" ]
then
	echo "${CPUAFFINITY} ${JDK_DIR}/jre/bin/java -cp ${CLSPATH} ${GCPOLICY} ${JDK_OPTIONS} -Xverbosegclog:verbosegc.${SUFFIX}.log ${MINHEAPSIZE} ${MAXHEAPSIZE} ${TENUREDSIZE} ${NURSERYSIZE} ${CLASS} ${BENCHMARKARGS} multithread=${MULTITHREAD} >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}"
	${CPUAFFINITY} ${JDK_DIR}/jre/bin/java -cp ${CLSPATH} ${GCPOLICY} ${JDK_OPTIONS} -Xverbosegclog:verbosegc.${SUFFIX}.log ${MINHEAPSIZE} ${MAXHEAPSIZE} ${TENUREDSIZE} ${NURSERYSIZE} ${CLASS} ${BENCHMARKARGS} multithread=${MULTITHREAD} >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
else
    echo "${CPUAFFINITY} ${JDK_DIR}/bin/java -cp ${CLSPATH} ${GCPOLICY} ${JDK_OPTIONS} -Xverbosegclog:verbosegc.${SUFFIX}.log ${MINHEAPSIZE} ${MAXHEAPSIZE} ${TENUREDSIZE} ${NURSERYSIZE} ${CLASS} ${BENCHMARKARGS} multithread=${MULTITHREAD} >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}"
    ${CPUAFFINITY} ${JDK_DIR}/bin/java -cp ${CLSPATH} ${GCPOLICY} ${JDK_OPTIONS} -Xverbosegclog:verbosegc.${SUFFIX}.log ${MINHEAPSIZE} ${MAXHEAPSIZE} ${TENUREDSIZE} ${NURSERYSIZE} ${CLASS} ${BENCHMARKARGS} multithread=${MULTITHREAD} >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}

fi

# Run headless GCMV using the generated verbosegclog and then parse using the Perl summarizer
if [ -f verbosegc.${SUFFIX}.log ];
then
	# Run Headless GCMV against the verbosegclog
	echo "File verbosegc.${SUFFIX}.log exists" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "Running Headless GCMV" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	${JDK_DIR}/bin/java -jar ../gcmv/gcmv.jar -f verbosegc.${SUFFIX}.log -p  ../gcmv/pauseTimeTemplate.epf >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}

	# Parse the generated data
	perl -I ${WORKDIR}/../gcmv/summarizer/lib ${WORKDIR}/../gcmv/summarizer/scripts/gcmv_summarizer.pl -f ${WORKDIR}/gcmvData.txt >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "*********************************" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "* GCMV Summarizer has completed *" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "*********************************" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
else
	echo "Error: verbosegc.${SUFFIX}.log does not exist" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "Error: Headless GCMV cannot operate without a verbosegclog" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
fi

# Move all LOG files and TPROF data into leftoverResults directory
filecount=$(ls ${FILES_LIST} 2> /dev/null | wc -l)

folderName="leftoverResults/${SUFFIX}"
if [ ${filecount} -gt 0 ]
then
	echo "=============================================================="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "= Status: Moving New Trace/Logs to leftoverResults/${SUFFIX} ="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "=============================================================="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	filelist=$(ls ${FILES_LIST} 2> /dev/null)
	mkdir -p ${folderName}

	for files in ${filelist}
	do
		echo "Moving ${files} to directory ${folderName}" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
		mv ${files} ${folderName}
	done
else
	echo "*** Error: No New Trace/Logs found"  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
fi

filecount=$(ls ${FILES_LIST_DMP} 2> /dev/null | wc -l)

if [ ${filecount} -gt 0 ]
then
	echo "========================================================="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "= Status: Moving New Dumps to leftoverResults/${SUFFIX} ="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	echo "========================================================="  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	filelist=$(ls ${FILES_LIST_DMP} 2> /dev/null)
	for files in ${filelist}
	do
		echo "Moving ${files} to directory ${PERFFARM_DUMP}" >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
		mv ${files} ${PERFFARM_DUMP} >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
	done
else
	echo "*** No New Dumps found"  >> ${ILOG_STDOUT} 2>>${ILOG_STDERR}
fi

TEST_STATUS=`grep -i "exception" ${ILOG_STDERR}`
if [[ -z ${TEST_STATUS} ]]
then
        #print ILOG_STDOUT to the console
        cat ${ILOG_STDOUT}
else
        echo "*** Test Failed with Exceptions ***"
        cat ${ILOG_STDOUT}
	cat ${ILOG_STDERR}
fi

#copy ILOG_STDOUT.txt to leftoverResults/${SUFFIX}
mv ${ILOG_STDOUT} ${folderName}
mv ${ILOG_STDERR} ${folderName}