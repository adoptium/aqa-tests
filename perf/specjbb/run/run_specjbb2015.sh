#!/usr/bin/env bash

# source the affinity script so we can split up the CPUs correctly
. "../../../perf/affinity.sh"

# Source in utility functions you can use for any benchmark
. "../../../perf/benchmark_setup.sh"

# echo out what our config is set to
function showConfig() {
  echo "=========================================================="
  echo "Mode: ${MODE}"
  echo "Number of Runs: ${NUM_OF_RUNS}"
  echo "Number of Groups: ${GROUP_COUNT}"
  echo "Number of Transaction Injectors: ${TI_JVM_COUNT}"
  echo "Controller Options: ${JAVA_OPTS_C}"
  echo "Transaction Injector Options: ${JAVA_OPTS_TI}"
  echo "Backend Options: ${JAVA_OPTS_BE}"
  echo "Results Directory: ${RESULTS_DIR}"
  echo "=========================================================="
}

# The make script passes in variables as strings and so we need to remove quotes and potentially other special characters
sanitizeIncomingVariables() {
    JAVA="${JAVA%\"}"
    JAVA="${JAVA#\"}"
}

# The main run script for SPECjbb2015 in MultiJVM mode
function runSpecJbbMulti() {

  for ((runNumber=1; runNumber<=NUM_OF_RUNS; runNumber=runNumber+1)); do

    # Create timestamp for use in logging, this is split over two lines as timestamp itself is a function
    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)

    # Some O/S setup before each run, see "../../../perf/benchmark_setup.sh"
    beforeEachRun

    # Create temp result directory                
    local result
    result="${RESULTS_DIR%\"}"
    result="${result#\"}"
    mkdir -pv "${result}"
    
    # Copy current SPECjbb2015 config for this run to the result directory
    cp -r "${SPECJBB_CONFIG}" "${result}"
    cd "${result}" || exit
    
    # Start logging
    echo "==============================================="
    echo "Launching SPECjbb2015 in ${MODE} mode...       "
    echo
    echo "Run $runNumber: $timestamp"
    echo

    sanitizeIncomingVariables

    echo "Starting the Controller JVM"
    # We don't double quote escape all arguments as some of those are being passed in as a list with spaces
    # shellcheck disable=SC2086
    # TODO check with Monica, won't the controller interfere with the cpu Range of 0-63 that we have resevered for the TI and BE here?
    local controllerCommand="${JAVA} ${JAVA_OPTS_C} ${SPECJBB_OPTS_C} -jar ${SPECJBB_JAR} -m MULTICONTROLLER ${MODE_ARGS_C} 2>controller.log 2>&1 | tee controller.out &"
    echo "$controllerCommand"
    eval "${controllerCommand}"

    # Save the PID of the Controller JVM
    CTRL_PID=$!
    echo "Controller PID: $CTRL_PID"

    # TODO This is brittle, let's detect proper controller start-up
    # Sleep for 3 seconds for the controller to start.
    sleep 3

    local cpuCount=0

    # Get the cpu count, see "../../../perf/benchmark_setup.sh"
    getTotalCPUs

    # Start the TransactionInjector and Backend JVMs for each group
    for ((groupNumber=1; groupNumber<GROUP_COUNT+1; groupNumber=groupNumber+1)); do

      local groupId="Group$groupNumber"

      echo -e "\nStarting Transaction Injector JVMs for group $groupId:"

      # Calculate CPUs avaialble via NUMA for this run. We use some math to create a CPU range string
      # E.g if totalCpuCount is 64, then we should use 0-63
      local cpuInit=$((cpuCount*TOTAL_CPU_COUNT))                 # e.g., 0 * 64 = 0
      local cpuMax=$(($(($((cpuCount+1))*TOTAL_CPU_COUNT))-1))    # e.g., 1 * 64 - 1 = 63
      local cpuRange="${cpuInit}-${cpuMax}"                       # e.g., 0-63
      echo "cpuRange is: $cpuRange"

      for ((injectorNumber=1; injectorNumber<TI_JVM_COUNT+1; injectorNumber=injectorNumber+1)); do

          local transactionInjectorJvmId="txiJVM$injectorNumber"
          local transactionInjectorName="$groupId.TxInjector.$transactionInjectorJvmId"

          echo "Start $transactionInjectorName"
          
          # We don't double quote escape all arguments as some of those are being passed in as a list with spaces
          # shellcheck disable=SC2086
          local transactionInjectorCommand="numactl --physcpubind=$cpuRange --localalloc ${JAVA} ${JAVA_OPTS_TI} ${SPECJBB_OPTS_TI} -jar ${SPECJBB_JAR} -m TXINJECTOR -G=$groupId -J=${transactionInjectorJvmId} ${MODE_ARGS_TI} > ${transactionInjectorName}.log 2>&1 &"
          echo "$transactionInjectorCommand"
          eval "${transactionInjectorCommand}"
          echo -e "\t${transactionInjectorName} PID = $!"

          # Sleep for 1 second to allow each transaction injector JVM to start. TODO this seems arbitrary
          sleep 1
      done

      local backendJvmId=beJVM
      local backendName="$groupId.Backend.${backendJvmId}"
      
      # Add GC logging to the backend's JVM options. We use the recommended settings for GCToolkit (https://www.github.com/microsoft/gctoolkit).
      JAVA_OPTS_BE_WITH_GC_LOG="$JAVA_OPTS_BE -Xlog:gc*,gc+ref=debug,gc+phases=debug,gc+age=trace,safepoint:file=${backendName}_gc.log"

      echo "Start $BE_NAME"
      # We don't double quote escape all arguments as some of those are being passed in as a list with spaces
      # shellcheck disable=SC2086
      local backendCommand="numactl --physcpubind=$cpuRange --localalloc ${JAVA} ${JAVA_OPTS_BE_WITH_GC_LOG} ${SPECJBB_OPTS_BE} -jar ${SPECJBB_JAR} -m BACKEND -G=$groupId -J=$backendJvmId ${MODE_ARGS_BE} > ${backendName}.log 2>&1 &"
      echo "$backendCommand"
      eval "${backendCommand}"
      echo -e "\t$BE_NAME PID = $!"

      # TODO 1 second seems arbitrary
      # Sleep for 1 second to allow each backend JVM to start.
      sleep 1

      # Increment the CPU count so that we use a new range for the next run
      # TODO This is actually pointless as we have ru = 1 n and group = 1 in our current experiment
      cpucount=$((cpucount+1))
    done

    echo
    echo "SPECjbb2015 is running..."
    echo "Please monitor $result/controller.out for progress"

    wait $CTRL_PID
    echo
    echo "Controller has stopped"

    echo "SPECjbb2015 has finished"
    echo
    
    cd "${WORKING_DIR}" || exit

  done
}

# The main run script for SPECjbb2015 in Composite mode
function runSpecJbbComposite() {

  for ((runNumber=1; runNumber<=NUM_OF_RUNS; runNumber=runNumber+1)); do

    # Create timestamp for use in logging, this is split over two lines as timestamp itself is a function
    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)

    # Some O/S setup before each run
    beforeEachRun

    # Create temp result directory                
    local result
    result="${RESULTS_DIR%\"}"
    result="${result#\"}"
    mkdir -pv "${result}"
    
    # Copy current SPECjbb2015 config for this run to the result directory
    cp -r "${SPECJBB_CONFIG}" "${result}"
    cd "${result}" || exit
    
    # Start logging
    echo "==============================================="
    echo "Launching SPECjbb2015 in Composite mode...     "
    echo
    echo "Run $runNumber: $timestamp"
    echo

    sanitizeIncomingVariables

    local cpuCount=0

    getTotalCPUs

    # Calculate CPUs avaialble via NUMA for this run. We use some math to create a CPU range string
    # E.g if totalCpuCount is 64, then we should use 0-63
    local cpuInit=$((cpuCount*TOTAL_CPU_COUNT))                 # e.g., 0 * 64 = 0
    local cpuMax=$(($(($((cpuCount+1))*TOTAL_CPU_COUNT))-1))    # e.g., 1 * 64 - 1 = 63
    local cpuRange="${cpuInit}-${cpuMax}"                       # e.g., 0-63
    echo "cpuRange is: $cpuRange"

    local backendJvmId=beJVM
    local backendName="$groupId.Backend.${backendJvmId}"
    
    # Add GC logging to the backend's JVM options. We use the recommendended settings for Microsoft's internal GC analysis tool called Censum
    JAVA_OPTS_BE_WITH_GC_LOG="$JAVA_OPTS_BE -Xlog:gc*,gc+ref=debug,gc+phases=debug,gc+age=trace,safepoint:file=${backendName}_gc.log"

    echo "Start $BE_NAME"
    # We don't double quote escape all arguments as some of those are being passed in as a list with spaces
    # shellcheck disable=SC2086
    local backendCommand="numactl --physcpubind=$cpuRange --localalloc ${JAVA} ${JAVA_OPTS_BE_WITH_GC_LOG} ${SPECJBB_OPTS_C} ${SPECJBB_OPTS_BE} -jar ${SPECJBB_JAR} -m COMPOSITE ${MODE_ARGS_BE} 2>&1 | tee composite.out &"
    echo "$backendCommand"
    eval "${backendCommand}"
    local compositePid=$!
    echo "Composite JVM PID = $compositePid"
    sleep 3

    # Increment the CPU count so that we use a new range for the next run
    # TODO This is actually pointless as run and group = 1 in our current experiment
    # cpucount=$((cpucount+1))
    echo
    echo "SPECjbb2015 is running..."
    echo "Please monitor $result/composite.out for progress"
    wait $compositePid

    echo "SPECjbb2015 has finished"
    echo
    
    cd "${WORKING_DIR}" || exit

  done
}

showConfig
checkHostReadiness

if [ "$MODE" == "multi-jvm" ]; then
    echo "Running in MultiJVM mode"
    runSpecJbbMulti
elif [ "$MODE" == "composite" ]; then
    echo "Running in Composite mode"
    runSpecJbbComposite
elif [ "$MODE" == "distributed" ]; then
    echo "Running in Distributed mode"
    runSpecJbbDistributed
fi

# exit gracefully once we are done
exit 0
