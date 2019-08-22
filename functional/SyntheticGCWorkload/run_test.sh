#!/bin/bash
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

# Test script for concurrentSlack=auto. 
# 
# Runs the latest version of the configuration of a given size matching the 
# pattern config_"$SIZE"_?.?.xml in the ./config/ directory. The size should be 
# given as the first parameter on the command line (eg: ./run_test.sh 1k). 
# 
# The exit code will be 0 if the test passes and nonzero otherwise. 
#

DATE_TIME=`date "+%Y-%m-%dT_%H-%M-%S"`

# Source for config files 
CONFIG_DIR="config"
GC_FRAG_OPTION=" -Xgc:concurrentSlack=macrofrag"
#######################################
# Select newest configuration of the given size. 
SIZE="$1"
PASS_PROPORTIONS="$2"
LOG_DIR="$3"
JDK_TEST_COMMAND="$4"
VM_OPTIONS_BASE="$5"

# Try to list configuration versions of the requested size. 
CONFIG_VERSIONS="`ls "$CONFIG_DIR"/"config_""$SIZE""_"*".xml"`" || exit 4
# Pick the last one. 
CONFIG="`ls $CONFIG_VERSIONS | sort -n | tail -n 1`"
# Get the name of the configuration 
CONFIG_NAME="${CONFIG##*/}"
#######################################

# Prefix for all log files
LOG_BASE=$LOG_DIR"/"$CONFIG_NAME"_"$DATE_TIME

# Run the workload $CONFIG using $VM_OPTIONS and $HEAP sending output to $VERBOSE_FILE and $STDOUT_FILE
run_workload(){	
	STDOUT_FILE=$LOG_BASE"_stdout_"$LOG_SUFFIX".txt"	
  
	echo "Start time: "`date "+%Y-%m-%dT_%H-%M-%S"`
	echo "Workload configuration: "$CONFIG
	echo "Options: "$VM_OPTIONS
	echo "Heap: "$HEAP
	TEST_CMD=$JDK_TEST_COMMAND" "$VM_OPTIONS" -Xverbosegclog:"$VERBOSE_FILE" -cp .:SyntheticGCWorkload.jar net.adoptopenjdk.casa.workload_sessions.Main "$CONFIG" --log_file "$STDOUT_FILE" -s"
	echo "run_workload() - Command: "$TEST_CMD
	
	$TEST_CMD
}

# Compare the log files VERBOSE_1 and VERBOSE_2 for the two runs. 
test_verbose_files(){	
	# The return status of the comparator will be 0 if the test passes 
	TEST_VF_CMD=$JDK_TEST_COMMAND" -cp SyntheticGCWorkload.jar net.adoptopenjdk.casa.verbose_gc_parser.VerboseGCComparator "$VERBOSE_1" "$VERBOSE_2" -e x,p -d 2,2 -p "$PASS_PROPORTIONS
	echo "test_verbose_files() - Command: "$TEST_VF_CMD
	
	$TEST_VF_CMD
}

# Test without option 
run_workload_1(){	
	VM_OPTIONS=$VM_OPTIONS_BASE	
	LOG_SUFFIX="original"
	VERBOSE_FILE=$LOG_BASE"_verbose_"$LOG_SUFFIX".xml"	
	
	VERBOSE_1=$VERBOSE_FILE
	run_workload
}

# Test with option 
run_workload_2(){	
	VM_OPTIONS=$VM_OPTIONS_BASE$GC_FRAG_OPTION
	LOG_SUFFIX="concurrentSlackAuto"
	VERBOSE_FILE=$LOG_BASE"_verbose_"$LOG_SUFFIX".xml"	

	VERBOSE_2=$VERBOSE_FILE	
	run_workload
}

#
# Tests the workload configuration, $CONFIG, with and without the extra option (-Xgc:concurrentSlack=macrofrag for J9)
# option. Exits when done.  
# 
# Exit codes: 
#  0 if the test passes
#  1 if the test fails
#  2 if the workload itself fails
#
# Prints "ALL TESTS PASSED" on success and "FAILED" otherwise. 
#
run_slack_auto_test(){		
	# Try to run the two workload configurations 
	if run_workload_1 && run_workload_2
	then 
		# Compare results and issue exit status based on test success
		if test_verbose_files 
		then 		
			# Test passed
			echo "ALL TESTS PASSED"
			exit 0 
		else 
			# Test failed
			echo "FAILED"
			exit 1
		fi 
	# One of the workloads failed
	else 
		# Dump the log file 
		cat "$STDOUT_FILE"
		echo "Workload Failure: test was not completed"
		echo "FAILED"
		exit 2
	fi
}

# Run the workloads and test the result. Exits. 
run_slack_auto_test

# Unreachable
exit 3
