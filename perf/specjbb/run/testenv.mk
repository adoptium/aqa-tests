#!/usr/bin/env bash

SPECJBB_BUILD=$(JVM_TEST_ROOT)/perf/specjbb

export JAVA=$(JAVA_COMMAND)
export SPECJBB_JAR=$(SPECJBB_BUILD)/suite/specjbb2015.jar
export SPECJBB_CONFIG=$(SPECJBB_BUILD)/suite/config
export RUN_SCRIPTS_DIR=$(SPECJBB_BUILD)/run
export RUN_OPTIONS_DIR=$(SPECJBB_BUILD)/run/options
export RESULTS_DIR=$(REPORTDIR)

# SPECjbb2015 will run in multi-jvm mode
# That is, the Controller, TransactionInjector and Backend are all in separate JVMs
#export MODE=multi-jvm

# SPECjbb2015 will run in composite mode
# That is, the Controller and TransactionInjector will be run in the same JVM as the Backend
export MODE="composite"

# SPECjbb2015 will run in distributed mode
# That is, the Controller and TransactionInjector will be run on separate JVMs on a separate host from the Backend
# TODO - NOT SUPPORTED YET
#export MODE="distributed"
