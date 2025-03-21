#!/bin/bash

jckAgentPID=0
harnessExitCode=0
jckHarnessPID=0

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
	eval $1
    jckHarnessPID=$!
}

queryVmJdwpTest() {
    java -cp /home/jenkins/jck_root/JCK8-unzipped/JCK-runtime-8d/lib/javatest.jar com.sun.javatest.finder.ShowTests -finder com.sun.javatest.finder.HTMLTestFinder -end /home/jenkins/jck_root/JCK8-unzipped/JCK-runtime-8d/tests/testsuite.html -initial vm/jdwp | tr -d "[:blank:]"
}


# Query all of the tests in $(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/tests/vm/jdwp/
queryVmJdwpTest | while read -r test; 
do
    cp $3 temp_jdwp.jtb
    # Replace tests=vm/jdwp line with tests=<Individual test case>
    sed -i -e "s/tests vm/jdwp/tests $test/g" temp_jdwp.jtb
    startJCKAgent "$1"
	startJCKHarness "$2" temp_jdwp.jtb
    sleep 60 # 60 second timeout
    if kill -s 0 $jckHarnessPID 2>nul; then
        echo "Testcase $test : Process $jckHarnessPID is still running after 60 seconds... killing..."
        kill -9 $jckHarnessPID
        harnessExitCode=124
    else
        wait $jckHarnessPID
        harnessExitCode=$?
    fi
    stopJCKAgent
done

exit $harnessExitCode