#!/bin/bash

jckAgentPID=0
harnessExitCode=0
tnameservPID=0

startJCKAgent() {
	echo "Starting JCK agent.."
	eval $1
	jckAgentPID=$!
	echo "Agent started with PID $jckAgentPID"
}

stopJCKAgent() {
	echo "Test complete. Stopping JCK Agent.."
	kill -9 $jckAgentPID
}

startTNameServ() {
	echo "Starting TNAMESERV.."
	eval $1
	tnameservPID=$!	
	echo "TNAMESERV started with PID $tnameservPID"
}

startJCKHarness() {
	echo "Starting JCK harness.."
	eval $1; return $?
}

stopTNameServ() {
	echo "Stopping TNAMESERV.."
	kill -9 $tnameservPID
}

# Query all of the tests in $(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/tests/vm/jdwp/
find $4 -type f -name "*.java" | while read -r test; 
do
    startJCKAgent
    # cp generated.jtb temp_jdwp.jtb 
    # Replace tests=vm/jdwp line with tests=<TEST_CASE>
    # Not sure what to do here ^^
    startTNameServ "$2"
	startJCKHarness "$3"
    harnessExitCode=$?
    stopTNameServ
    stopJCKAgent
done