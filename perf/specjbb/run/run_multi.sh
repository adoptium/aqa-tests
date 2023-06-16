#!/usr/bin/env bash

echo "Backend opts: $JAVA_OPTS_BE"
echo "Number of runs: ${NUM_OF_RUNS}"
echo "Results dir: ${RESULTS_DIR}"

for ((n=1; $n<=$NUM_OF_RUNS; n=$n+1)); do

  # Create result directory                
  temp="${RESULTS_DIR%\"}"
  temp="${temp#\"}"
  timestamp=$(date '+%y-%m-%d_%H%M%S')
  result="${temp}/${timestamp}"
  mkdir -pv $result
  
  # Copy current config to the result directory
  cp -r $SPECJBB_CONFIG $result

  cd $result

  echo "Run $n: $timestamp"
  echo "Launching SPECjbb2015 in MultiJVM mode..."
  echo

  JAVA="${JAVA%\"}"
  JAVA="${JAVA#\"}"
  echo "Start Controller JVM"
  $JAVA $JAVA_OPTS_C $SPECJBB_OPTS_C -jar $SPECJBB_JAR -m MULTICONTROLLER $MODE_ARGS_C 2>controller.log 2>&1 | tee controller.out &

  CTRL_PID=$!
  echo "Controller PID = $CTRL_PID"

  sleep 3

  for ((gnum=1; $gnum<$GROUP_COUNT+1; gnum=$gnum+1)); do

    GROUPID=Group$gnum
    echo -e "\nStarting JVMs from $GROUPID:"

    for ((jnum=1; $jnum<$TI_JVM_COUNT+1; jnum=$jnum+1)); do

        JVMID=txiJVM$jnum
        TI_NAME=$GROUPID.TxInjector.$JVMID

        echo "    Start $TI_NAME"
        $JAVA $JAVA_OPTS_TI $SPECJBB_OPTS_TI -jar $SPECJBB_JAR -m TXINJECTOR -G=$GROUPID -J=$JVMID $MODE_ARGS_TI > $TI_NAME.log 2>&1 &
        echo -e "\t$TI_NAME PID = $!"
        sleep 1
    done

    JVMID=beJVM
    BE_NAME=$GROUPID.Backend.$JVMID
    
    # Add GC logging to the backend's JVM options 
    JAVA_OPTS_BE_WITH_GC_LOG="$JAVA_OPTS_BE -Xlog:gc*:file=${BE_NAME}_gc.log"
    echo "    Start $BE_NAME"
    $JAVA $JAVA_OPTS_BE_WITH_GC_LOG $SPECJBB_OPTS_BE -jar $SPECJBB_JAR -m BACKEND -G=$GROUPID -J=$JVMID $MODE_ARGS_BE > $BE_NAME.log 2>&1 &
    echo -e "\t$BE_NAME PID = $!"
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
  
  cd $WORKING_DIR

done

exit 0
