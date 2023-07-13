#!/usr/bin/env bash

# SPECjbb2015 will run in composite mode
# That is, the Controller and TransactionInjector will be run in the same JVM as the Backend
export MODE="composite"

# Number of runs to execute
export NUM_OF_RUNS=5

# This will configure a basic run of SPECjbb in composite mode
# Therefore, the topography of the run includes 1 group consisting of
# 1 TransactionInjector and 1 Backend
export GROUP_COUNT=1
export TI_JVM_COUNT=1 


# TODO - This is a temporary hard coded configuration to get the run to complete
# Configuration for a Maxed out (from a scaling perspective) Standard_D64s_v5 run as per the Java Engineering Group Azure VM SKU SPECjbb2015 guide
export SPECJBB_OPTS_C="-Dspecjbb.group.count=$GROUP_COUNT -Dspecjbb.txi.pergroup.count=$TI_JVM_COUNT -Dspecjbb.forkjoin.workers.Tier1=128 -Dspecjbb.forkjoin.workers.Tier2=14 -Dspecjbb.forkjoin.workers.Tier3=24 -Dspecjbb.customerDriver.threads.saturate=96 -Dspecjbb.comm.connect.selector.runner.count=10"
export SPECJBB_OPTS_TI=""
export SPECJBB_OPTS_BE=""

# The Controller, TransactionInjector, and Backend (aka System Under Test; SUT) are configured to meet the minimum 
# hardware requirements suggested by SPECjbb 
# (see the SPECjbb 2015 user guide, section 2.3 'Minimum hardware requirements', for more details)
#
# This implies that a machine should have atleast 8GB of memory and 8 CPU cores to run this test
export JAVA_OPTS_C="-Xms2g -Xmx2g -Xmn1536m -XX:+UseParallelGC -XX:ParallelGCThreads=2 -XX:CICompilerCount=4"
export JAVA_OPTS_TI="${JAVA_OPTS_C}"
# export JAVA_OPTS_BE="-Xms4g -Xmx4g -Xmn3g -XX:+UseParallelGC -XX:ParallelGCThreads=4 -XX:-UseAdaptiveSizePolicy" # Default configuration

# TODO - This is a temporary hard coded configuration to get the run to complete
# Configuration for a Maxed out (from a scaling perspective) Standard_D64s_v5 run as per the Java Engineering Group Azure VM SKU SPECjbb2015 guide
export JAVA_OPTS_BE="-Xms192g -Xmx192g -Xmn173g -XX:+UseParallelGC -XX:ParallelGCThreads=64 -XX:+AlwaysPreTouch -XX:+UseLargePages -XX:+UseTransparentHugePages -XX:-UseAdaptiveSizePolicy -XX:-UsePerfData"

export MODE_ARGS_C=""
export MODE_ARGS_TI=""
export MODE_ARGS_BE=""
