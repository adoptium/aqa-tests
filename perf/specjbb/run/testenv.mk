#!/usr/bin/env bash

SPECJBB_BUILD=$(JVM_TEST_ROOT)/perf/specjbb

export JAVA=$(JAVA_COMMAND)
export SPECJBB_JAR=$(SPECJBB_BUILD)/suite/specjbb2015.jar
export SPECJBB_CONFIG=$(SPECJBB_BUILD)/suite/config
export RUN_SCRIPTS_DIR=$(SPECJBB_BUILD)/run
export RUN_OPTIONS_DIR=$(SPECJBB_BUILD)/run/options
export RESULTS_DIR=$(REPORTDIR)
