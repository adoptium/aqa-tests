#!/usr/bin/env bash

echo "Backend opts: ${JAVA_OPTS_BE}"
echo "Number of runs: ${NUM_OF_RUNS}"
echo "Results dir: ${RESULTS_DIR}"

function runSpecJbbMulti() {
  for ((runNumber=1; runNumber<=NUM_OF_RUNS; runNumber=runNumber+1)); do

    # TODO we can check the output of this.
    numactl --show

    # Create timestamp for use in logging, this is split over two lines as timestamp itself is a function
    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)

    # Call sync to force any pending disk writes TODO we want to run this as sudo
    sync

    # TODO we need to give the azdo user permission to write to this file
    # The /proc/sys/vm/drop_caches file is a special interface in the Linux kernel for managing the system's cache.
    # 3: Clear both the page cache and the dentries/inodes cache (combined effect of 1 and 2).
    echo 3 > /proc/sys/vm/drop_caches

    # Create temp result directory                
    local result
    result="${RESULTS_DIR%\"}"
    result="${result#\"}"
    mkdir -pv "${result}"
    
    # Copy current config to the result directory
    cp -r "${SPECJBB_CONFIG}" "${result}"
    cd "${result}" || exit
    
    # Start logging
    echo "Run $runNumber: $timestamp"
    echo "Launching SPECjbb2015 in MultiJVM mode..."
    echo

    # Start the Controller JVM, note JAVA is a previously exported environment variable coming in from testenv.mk
    echo "Starting the Controller JVM"
    JAVA="${JAVA%\"}"
    JAVA="${JAVA#\"}"
    # We don't double quote escape all arguments as some of those are being passed in as a list with spaces
    # shellcheck disable=SC2086
    "${JAVA}" ${JAVA_OPTS_C} ${SPECJBB_OPTS_C} -jar "${SPECJBB_JAR}" -m MULTICONTROLLER ${MODE_ARGS_C} 2>controller.log 2>&1 | tee controller.out &

    # Save the PID of the Controller JVM
    CTRL_PID=$!
    echo "Controller PID = $CTRL_PID"

    # Sleep for 3 seconds for the controller to start. TODO This is brittle, let's detect proper controller start-up
    sleep 3

    # Start the TransactionInjector and Backend JVMs for each group
    for ((groupNumber=1; groupNumber<GROUP_COUNT+1; groupNumber=groupNumber+1)); do

      local groupId="Group$groupNumber"

      echo -e "\nStarting Transaction Injector JVMs from $groupId:"

      for ((injectorNumber=1; injectorNumber<TI_JVM_COUNT+1; injectorNumber=injectorNumber+1)); do

          local transactionInjectorJvmId="txiJVM$injectorNumber"
          local transactionInjectorName="$groupId.TxInjector.$transactionInjectorJvmId"

          echo "Start $transactionInjectorName"
          # We don't double quote escape all arguments as some of those are being passed in as a list with spaces
          # shellcheck disable=SC2086
          "${JAVA}" ${JAVA_OPTS_TI} ${SPECJBB_OPTS_TI} -jar "${SPECJBB_JAR}" -m TXINJECTOR -G=$groupId -J="${transactionInjectorJvmId}" ${MODE_ARGS_TI} > "${TI_NAME}.log" 2>&1 &
          echo -e "\t${transactionInjectorName} PID = $!"

          # Sleep for 1 second to allow each transaction injector JVM to start. TODO this seems arbitrary
          sleep 1
      done

      local backendJvmId=beJVM
      local backendName="$groupId.Backend.${backendJvmId}"
      
      # Add GC logging to the backend's JVM options. We use the recommendended settings for Microsoft's internal GC analysis tool called Censum
      JAVA_OPTS_BE_WITH_GC_LOG="$JAVA_OPTS_BE -Xlog:gc*,gc+ref=debug,gc+phases=debug,gc+age=trace,safepoint:file=${backendName}_gc.log"

      echo " Start $BE_NAME"
      # We don't double quote escape all arguments as some of those are being passed in as a list with spaces
      # shellcheck disable=SC2086
      "${JAVA}" ${JAVA_OPTS_BE_WITH_GC_LOG} ${SPECJBB_OPTS_BE} -jar "${SPECJBB_JAR}" -m BACKEND -G=$groupId -J=$backendJvmId ${MODE_ARGS_BE} > "${backendName}.log" 2>&1 &
      echo -e "\t$BE_NAME PID = $!"

      # Sleep for 1 second to allow each backend JVM to start. TODO this seems arbitrary
      sleep 1

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

runSpecJbbMulti

# exit gracefully once we are done
exit 0
