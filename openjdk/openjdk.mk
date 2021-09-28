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
	MEMORY_SIZE:=$(shell KMEMMB=`awk '/^MemTotal:/{print int($$2/1024)}' /proc/meminfo`; if [ -r /sys/fs/cgroup/memory/memory.limit_in_bytes ]; then CGMEM=`cat /sys/fs/cgroup/memory/memory.limit_in_bytes`; else CGMEM=`expr $${KMEMMB} \* 1024`; fi; CGMEMMB=`expr $${CGMEM} / 1048576`; if [ "$${KMEMMB}" -lt "$${CGMEMMB}" ]; then echo "$${KMEMMB}"; else echo "$${CGMEMMB}"; fi)
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
ifeq ($(OS),SunOS)	
	NPROCS:=$(shell psrinfo | wc -l)
	MEMORY_SIZE:=$(shell prtconf | awk '/^Memory size:/{print int($$3/1024)}')
endif	
ifeq ($(OS),OS/390)
	EXTRA_OPTIONS += -Dcom.ibm.tools.attach.enable=yes
endif
# Upstream OpenJDK, roughly, sets concurrency based on the
# following: min(NPROCS/2, MEM_IN_GB/2).
MEM := $(shell expr $(MEMORY_SIZE) / 2048)
CORE := $(shell expr $(NPROCS) / 2)
CONC := $(CORE)
ifeq ($(shell expr $(CORE) \> $(MEM)), 1)
	CONC := $(MEM)
endif
# Can't determine cores on zOS, use a reasonable default
ifeq ($(OS),OS/390)
	CONC := 4
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
# Multiple by 8 the timeout numbers, except on zOS use 2
ifneq ($(OS),OS/390)
	JTREG_TIMEOUT_OPTION =  -timeoutFactor:8
else
	JTREG_TIMEOUT_OPTION =  -timeoutFactor:2
endif
JTREG_BASIC_OPTIONS += $(JTREG_TIMEOUT_OPTION)
# Create junit xml
JTREG_XML_OPTION = -xml:verify
JTREG_BASIC_OPTIONS += $(JTREG_XML_OPTION)
# Add any extra options
JTREG_BASIC_OPTIONS += $(EXTRA_JTREG_OPTIONS)
JTREG_KEY_OPTIONS :=
VMOPTION_HEADLESS :=
ifneq ($(PLATFORM),x86-64_alpine-linux) 
	JTREG_KEY_OPTIONS := -k:'!headful'
	VMOPTION_HEADLESS := -Djava.awt.headless=true
endif
JTREG_BASIC_OPTIONS += $(JTREG_KEY_OPTIONS)

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

JDK_CUSTOM_TARGET ?= java/math/BigInteger/BigIntegerTest.java
ifneq (,$(findstring $(JDK_VERSION),8-9))
	JTREG_JDK_TEST_DIR := $(OPENJDK_DIR)$(D)jdk$(D)test
	JTREG_HOTSPOT_TEST_DIR := $(OPENJDK_DIR)$(D)hotspot$(D)test
	JTREG_LANGTOOLS_TEST_DIR := $(OPENJDK_DIR)$(D)langtools$(D)test
else
	JTREG_JDK_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)jdk
	JTREG_HOTSPOT_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)hotspot$(D)jtreg
	JTREG_LANGTOOLS_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)langtools
endif

JDK_NATIVE_OPTIONS :=
JVM_NATIVE_OPTIONS :=
CUSTOM_NATIVE_OPTIONS :=

ifneq ($(JDK_VERSION),8)
	ifdef TESTIMAGE_PATH
		JDK_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)jdk$(D)jtreg$(D)native"
		ifeq ($(JDK_IMPL), hotspot)
			JVM_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)hotspot$(D)jtreg$(D)native"
		# else if JDK_IMPL is openj9 or ibm
		else ifneq ($(filter openj9 ibm, $(JDK_IMPL)),)
			JVM_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)openj9"
		endif
		ifneq (,$(findstring /hotspot/, $(JDK_CUSTOM_TARGET))) 
			CUSTOM_NATIVE_OPTIONS := $(JVM_NATIVE_OPTIONS)
		else
			CUSTOM_NATIVE_OPTIONS := $(JDK_NATIVE_OPTIONS)
		endif
	endif
endif

PROBLEM_LIST_FILE:=excludes/ProblemList_openjdk$(JDK_VERSION).txt
PROBLEM_LIST_DEFAULT:=excludes/ProblemList_openjdk11.txt
TEST_VARIATION_DUMP:=
TEST_VARIATION_JIT_PREVIEW:=
TEST_VARIATION_JIT_AGGRESIVE:=
TIMEOUT_HANDLER:=

# if JDK_IMPL is openj9 or ibm
ifneq ($(filter openj9 ibm, $(JDK_IMPL)),)
	PROBLEM_LIST_FILE:=excludes/ProblemList_openjdk$(JDK_VERSION)-openj9.txt
	PROBLEM_LIST_DEFAULT:=excludes/ProblemList_openjdk11-openj9.txt
	TEST_VARIATION_DUMP:=-Xdump:system:none -Xdump:heap:none -Xdump:system:events=gpf+abort+traceassert+corruptcache
	TEST_VARIATION_JIT_PREVIEW:=-XX:-JITServerTechPreviewMessage
	TEST_VARIATION_JIT_AGGRESIVE:=-Xjit:enableAggressiveLiveness
	TIMEOUT_HANDLER:=-timeoutHandler:jtreg.openj9.CoreDumpTimeoutHandler -timeoutHandlerDir:$(Q)$(LIB_DIR)$(D)openj9jtregtimeouthandler.jar$(Q)
endif

# if cannot find the problem list file, set to default file
ifeq (,$(wildcard $(PROBLEM_LIST_FILE)))
	PROBLEM_LIST_FILE:=$(PROBLEM_LIST_DEFAULT)
endif

# ProblemList-graal.txt file only exists in jdk11 and jdk16. Refer to the file only when it is present.
GRAAL_PROBLEM_LIST_FILE:=
ifneq ($(filter 11 16, $(JDK_VERSION)),)
	GRAAL_PROBLEM_LIST_FILE:=-exclude:$(Q)$(JTREG_HOTSPOT_TEST_DIR)$(D)ProblemList-graal.txt$(Q)
endif
