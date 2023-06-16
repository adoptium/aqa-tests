#!/bin/bash

jckAgentPID=0
rmiRegistryPID=0
rmidPID=0
tnameservPID=0
harnessExitCode=0

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

startJCKHarness() {
	echo "Starting JCK harness.."
	eval $1; return $?
}

startRMIRegistry() {
	echo "Starting RMI Registry.."
	eval $1
	rmiRegistryPID=$!
	echo "RMI Registry started with PID $rmiRegistryPID"
} 

stopRMIRegistry() {
	echo "Stopping RMI Registry.."
	kill -9 $rmiRegistryPID
}

startRMID() {
	echo "Starting RMID.."
	eval $1
	rmidPID=$!	
	echo "RMID started with PID $rmidPID"
}

stopRMID() {
	echo "Stopping RMID.."
	kill -9 $rmidPID
}

startTNameServ() {
	echo "Starting TNAMESERV.."
	eval $1
	tnameservPID=$!	
	echo "TNAMESERV started with PID $tnameservPID"
}

stopTNameServ() {
	echo "Stopping TNAMESERV.."
	kill -9 $tnameservPID
}

if [ $# -eq 2 ]; then
	startJCKAgent "$1"
	startJCKHarness "$2"
	harnessExitCode=$?
	stopJCKAgent
elif [ $# -eq 3 ]; then
	startJCKAgent "$1"
	startTNameServ "$2"
	startJCKHarness "$3"
	harnessExitCode=$?
	stopJCKAgent
	stopTNameServ
elif [ $# -eq 4 ]; then
	startJCKAgent "$1"
	startRMIRegistry "$2"
	startRMID "$3"
	startJCKHarness "$4"
	harnessExitCode=$?
	stopJCKAgent
	stopRMIRegistry
	stopRMID
elif [ $# -eq 5 ]; then
	startJCKAgent "$1"
	startRMIRegistry "$2"
	startRMID "$3"
	startTNameServ "$4"
	startJCKHarness "$5"
	harnessExitCode=$?
	stopJCKAgent
	stopRMIRegistry
	stopRMID
	stopTNameServ
fi

exit $harnessExitCode
