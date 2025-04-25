#!/bin/bash

jckAgentPID=0
harnessExitCode=0
jckHarnessPID=0

startJCKAgent() {
	echo "Starting JCK agent.."
	eval "$1"
	jckAgentPID=$!
	echo "Agent started with PID $jckAgentPID"
}

stopJCKAgent() {
	echo "Test complete. Stopping JCK Agent.."
	kill -9 $jckAgentPID
}

startJCKHarness() {
	echo "Starting JCK harness.."
	eval "$1"
    jckHarnessPID=$!
}

queryVmJdwpTest() {
    local jck_root_path="$1/JCK-runtime-$2/"
    eval "$3/bin/java -cp $jck_root_path/lib/javatest.jar com.sun.javatest.finder.ShowTests -finder com.sun.javatest.finder.HTMLTestFinder -end $jck_root_path/tests/testsuite.html -initial vm/jdwp | tr -d "[:blank:]""
}


# Query all of the tests in $(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/tests/vm/jdwp/
queryVmJdwpTest "$6" "$7" "$8" | while read -r test; 
do
    cp "$3" temp_jdwp.jtb
    # Replace tests=vm/jdwp line with tests=<Individual test case>
    sed -i -e "s/tests vm/jdwp/tests $test/g" temp_jdwp.jtb
    startJCKAgent "$1"
    # $(TEST_STATUS) and $(GEN_SUMMARY_GENERIC) are passed into startJCKHarness
	startJCKHarness "$2" temp_jdwp.jtb "$4" "$5" tests=$test testsuite=RUNTIME
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
