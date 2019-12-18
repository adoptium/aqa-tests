##############################################################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##############################################################################
NPROCS:=1
# Memory size in MB
MEMORY_SIZE:=1024

OS:=$(shell uname -s)

ifeq ($(OS),Linux)
	NPROCS:=$(shell grep -c ^processor /proc/cpuinfo)
	MEMORY_SIZE:=$(shell \
		expr `cat /proc/meminfo | grep MemTotal | awk '{print $$2}'` / 1024 \
		)
endif
ifeq ($(OS),Darwin)
	NPROCS:=$(shell sysctl -n hw.ncpu)
	MEMORY_SIZE:=$(shell expr `sysctl -n hw.memsize` / 1024 / 1024)
endif
ifeq ($(OS),FreeBSD)
	NPROCS:=$(shell sysctl -n hw.ncpu)
	MEMORY_SIZE:=$(shell expr `sysctl -n hw.memsize` / 1024 / 1024)
endif
ifeq ($(CYGWIN),1)
 	NPROCS:=$(NUMBER_OF_PROCESSORS)
	MEMORY_SIZE:=$(shell \
		expr `wmic computersystem get totalphysicalmemory -value | grep = \
		| cut -d "=" -f 2-` / 1024 / 1024 \
		)
endif
# Upstream OpenJDK, roughly, sets concurrency based on the
# following: min(NPROCS/2, MEM_IN_GB/2).
MEM := $(shell expr $(MEMORY_SIZE) / 2048)
CORE := $(shell expr $(NPROCS) / 2)
CONC := $(CORE)
ifeq ($(shell expr $(CORE) \> $(MEM)), 1)
	CONC := $(MEM)
endif
JTREG_CONC ?= 0
# Allow JTREG_CONC be set via parameter
ifeq ($(JTREG_CONC), 0)
	JTREG_CONC := $(CONC)
	ifeq ($(JTREG_CONC), 0)
		JTREG_CONC := 1
	endif
endif
EXTRA_JTREG_OPTIONS += -concurrency:$(JTREG_CONC)

JTREG_BASIC_OPTIONS += -agentvm
# Only run automatic tests
JTREG_BASIC_OPTIONS += -a
# Always turn on assertions
JTREG_ASSERT_OPTION = -ea -esa
JTREG_BASIC_OPTIONS += $(JTREG_ASSERT_OPTION)
# Report details on all failed or error tests, times, and suppress output for tests that passed
JTREG_BASIC_OPTIONS += -v:fail,error,time,nopass
# Retain all files for failing tests
JTREG_BASIC_OPTIONS += -retain:fail,error,*.dmp,javacore.*,heapdump.*,*.trc
# Ignore tests are not run and completely silent about it
JTREG_IGNORE_OPTION = -ignore:quiet
JTREG_BASIC_OPTIONS += $(JTREG_IGNORE_OPTION)
# Multiple by 4 the timeout numbers
JTREG_TIMEOUT_OPTION =  -timeoutFactor:8
JTREG_BASIC_OPTIONS += $(JTREG_TIMEOUT_OPTION)
# Create junit xml
JTREG_XML_OPTION = -xml:verify
JTREG_BASIC_OPTIONS += $(JTREG_XML_OPTION)
# Add any extra options
JTREG_BASIC_OPTIONS += $(EXTRA_JTREG_OPTIONS)

ifndef JRE_IMAGE
	JRE_ROOT := $(TEST_JDK_HOME)
	JRE_IMAGE := $(subst j2sdk-image,j2re-image,$(JRE_ROOT))
endif

ifdef OPENJDK_DIR 
# removing "
OPENJDK_DIR := $(subst ",,$(OPENJDK_DIR))
else
OPENJDK_DIR := $(TEST_ROOT)$(D)openjdk$(D)openjdk-jdk
endif

ifneq (,$(findstring $(JDK_VERSION),8-9))
	JTREG_JDK_TEST_DIR := $(OPENJDK_DIR)$(D)jdk$(D)test
	JTREG_HOTSPOT_TEST_DIR := $(OPENJDK_DIR)$(D)hotspot$(D)test
	JTREG_LANGTOOLS_TEST_DIR := $(OPENJDK_DIR)$(D)langtools$(D)test
	JDK_CUSTOM_TARGET ?= jdk/test/java/math/BigInteger/BigIntegerTest.java
else
	JTREG_JDK_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)jdk
	JTREG_HOTSPOT_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)hotspot$(D)jtreg
	JTREG_LANGTOOLS_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)langtools
	JDK_CUSTOM_TARGET ?= test/jdk/java/math/BigInteger/BigIntegerTest.java
endif

JDK_NATIVE_OPTIONS :=
JVM_NATIVE_OPTIONS :=
CUSTOM_NATIVE_OPTIONS :=

ifneq ($(JDK_VERSION),8)
	ifdef TESTIMAGE_PATH
		JDK_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)jdk$(D)jtreg$(D)native"
		ifeq ($(JDK_IMPL), hotspot)
			JVM_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)hotspot$(D)jtreg$(D)native"
		else ifeq ($(JDK_IMPL), openj9)
			JVM_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)openj9"
		endif
		ifneq (,$(findstring /hotspot/, $(JDK_CUSTOM_TARGET))) 
			CUSTOM_NATIVE_OPTIONS := $(JVM_NATIVE_OPTIONS)
		else
			CUSTOM_NATIVE_OPTIONS := $(JDK_NATIVE_OPTIONS)
		endif
	endif
endif
