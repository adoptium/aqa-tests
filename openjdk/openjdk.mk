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
ARCH:=$(shell uname -m)

ifeq ($(OS),Linux)
	NPROCS:=$(shell nproc)
	# This is the MEMORY_SIZE script below, with formatting for readability.
	#
	# // The number of megabytes of memory this machine has.
	# KMEMMB=`awk '/^MemTotal:/{print int($$2/1024)}' /proc/meminfo`; 
	#
	# // If this machine/container uses cgroups to limit the amount of 
	# // memory available to us, we should use that as out memory size.
	# if [ -r /sys/fs/cgroup/memory.max ]; then
	#     // Use this to identify memory maximum (bytes) for cgroup v2.
	#     CGMEM=`cat /sys/fs/cgroup/memory.max 2>1`; 
	# else
	#     // Else use this file for memory maximum (bytes) on cgroup v1.
	#     CGMEM=`cat /sys/fs/cgroup/memory/memory.limit_in_bytes 2>1`; 
	# fi; 
	#
	# // If those files were empty, or didn't exist, or had non-numbers
	# // in them, then use /proc/meminfo (converted to bytes).
	# if echo "$${CGMEM}" | grep -Eqv '^[0-9]+$$' ; then
	#     CGMEM=`expr $${KMEMMB} \* 1024 \* 1024`; 
	# fi; 
	#
	# CGMEMMB=`expr $${CGMEM} / 1024 / 1024`; 
	#
	# // Between memory limits in the cgroup and memory on the machine,
	# // use the lower limit. This protects us against situations
	# // where the cgroup has a value which is much bigger/smaller than
	# // the limits on the machine overall. We've seen both.
	# if [ "$${KMEMMB}" -lt "$${CGMEMMB}" ]; then 
	#     echo "$${KMEMMB}"; else echo "$${CGMEMMB}"; 
	# fi
	MEMORY_SIZE:=$(shell KMEMMB=`awk '/^MemTotal:/{print int($$2/1024)}' /proc/meminfo`; if [ -r /sys/fs/cgroup/memory.max ]; then CGMEM=`cat /sys/fs/cgroup/memory.max 2>1`; else CGMEM=`cat /sys/fs/cgroup/memory/memory.limit_in_bytes 2>1`; fi; if echo "$${CGMEM}" | grep -Eqv '^[0-9]+$$' ; then CGMEM=`expr $${KMEMMB} \* 1024 \* 1024`; fi; CGMEMMB=`expr $${CGMEM} / 1024 / 1024`; if [ "$${KMEMMB}" -lt "$${CGMEMMB}" ]; then echo "$${KMEMMB}"; else echo "$${CGMEMMB}"; fi)
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
CORE := $(shell expr $(NPROCS) / 2 + 1)
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
# riscv64 machines aren't very fast (yet!!)
ifeq ($(ARCH), riscv64)
	JTREG_TIMEOUT_OPTION = -timeoutFactor:16
# aarch64 linux requires extra time, set timeoutFactor to 12
else ifneq ($(filter linux_aarch64, $(SPEC)),)
	JTREG_TIMEOUT_OPTION =  -timeoutFactor:12
# Multiple by 8 the timeout numbers, except on zOS use 2
else ifeq ($(OS),OS/390)
	JTREG_TIMEOUT_OPTION =  -timeoutFactor:2
else
	JTREG_TIMEOUT_OPTION =  -timeoutFactor:8
endif

JTREG_BASIC_OPTIONS += $(JTREG_TIMEOUT_OPTION)
# Create junit xml
JTREG_XML_OPTION = -xml:verify
JTREG_BASIC_OPTIONS += $(JTREG_XML_OPTION)
# Add any extra options
JTREG_KEY_OPTIONS :=
VMOPTION_HEADLESS :=
libcVendor = $(shell ldd --version 2>&1 | sed -n '1s/.*\(musl\).*/\1/p')

ifeq ($(libcVendor),musl)
	JTREG_KEY_OPTIONS := -k:'!headful'
	VMOPTION_HEADLESS := -Djava.awt.headless=true
endif
# RISC-V is built in headless mode for now. See https://github.com/adoptium/ci-jenkins-pipelines/pull/867
ifeq ($(ARCH),riscv64)
	JTREG_KEY_OPTIONS := -k:'!headful'
	VMOPTION_HEADLESS := -Djava.awt.headless=true
endif
JTREG_BASIC_OPTIONS += $(JTREG_KEY_OPTIONS)

# set JTREG_BASIC_OPTIONS value into a new parameter before adding EXTRA_JTREG_OPTIONS
JTREG_BASIC_OPTIONS_WO_EXTRA_OPTS := $(JTREG_BASIC_OPTIONS)
JTREG_BASIC_OPTIONS += $(EXTRA_JTREG_OPTIONS)

# add another new parameter for concurrency
SPECIAL_CONCURRENCY=$(EXTRA_JTREG_OPTIONS)
# set SPECIAL_CONCURRENCY to 1 if the jdk is openj9 and the platform is linux_aarch64.
ifneq ($(filter openj9 ibm, $(JDK_IMPL)),)
	ifneq ($(filter linux_aarch64, $(SPEC)),)
		SPECIAL_CONCURRENCY= -concurrency:1
	endif
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
else
	JTREG_JDK_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)jdk
	JTREG_HOTSPOT_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)hotspot$(D)jtreg
	JTREG_LANGTOOLS_TEST_DIR := $(OPENJDK_DIR)$(D)test$(D)langtools
endif

JDK_CUSTOM_TARGET ?= java/math/BigInteger/BigIntegerTest.java
HOTSPOT_CUSTOM_TARGET ?= gc/stress/gclocker/TestExcessGCLockerCollections.java
HOTSPOT_CUSTOM_J9_TARGET ?= serviceability/jvmti/GetSystemProperty/JvmtiGetSystemPropertyTest.java
LANGTOOLS_CUSTOM_TARGET ?= tools/javac/4241573/T4241573.java
FULLPATH_JDK_CUSTOM_TARGET = $(foreach target,$(JDK_CUSTOM_TARGET),$(JTREG_JDK_TEST_DIR)$(D)$(target))
FULLPATH_HOTSPOT_CUSTOM_TARGET = $(foreach target,$(HOTSPOT_CUSTOM_TARGET),$(JTREG_HOTSPOT_TEST_DIR)$(D)$(target))
FULLPATH_HOTSPOT_CUSTOM_J9_TARGET = $(foreach target,$(HOTSPOT_CUSTOM_J9_TARGET),$(JTREG_HOTSPOT_TEST_DIR)$(D)$(target))

JDK_NATIVE_OPTIONS :=
JVM_NATIVE_OPTIONS :=

ifneq ($(JDK_VERSION),8)
	ifdef TESTIMAGE_PATH
		JDK_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)jdk$(D)jtreg$(D)native"
		ifeq ($(JDK_IMPL), hotspot)
			JVM_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)hotspot$(D)jtreg$(D)native"
		# else if JDK_IMPL is openj9 or ibm
		else ifneq ($(filter openj9 ibm, $(JDK_IMPL)),)
			JVM_NATIVE_OPTIONS := -nativepath:"$(TESTIMAGE_PATH)$(D)openj9"
		endif
	endif
endif

# Suitable values: 'docker' or 'podman'
CONTAINER_TEST_ENGINE=docker
# Run container tests on latest UBI 8 base image
OPENJDK_CONTAINER_TEST_OPTS:=-Djdk.test.docker.image.name=registry.access.redhat.com/ubi8/ubi -Djdk.test.docker.image.version=latest
ifneq ($(CONTAINER_TEST_ENGINE),docker)
  OPENJDK_CONTAINER_TEST_OPTS += -Djdk.test.container.command=$(CONTAINER_TEST_ENGINE)
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
	EXTRA_OPTIONS := -Xverbosegclog $(EXTRA_OPTIONS)
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

FEATURE_PROBLEM_LIST_FILE:=
ifneq (,$(findstring FIPS140_2, $(TEST_FLAG))) 
	FEATURE_PROBLEM_LIST_FILE:=-exclude:$(Q)$(JTREG_JDK_TEST_DIR)$(D)ProblemList-FIPS140_2.txt$(Q)
else ifneq (,$(findstring FIPS140_3_OpenJCEPlusFIPS.FIPS140-3, $(TEST_FLAG)))
	FEATURE_PROBLEM_LIST_FILE:=-exclude:$(Q)$(JTREG_JDK_TEST_DIR)$(D)ProblemList-FIPS140_3_OpenJCEPlusFIPS.FIPS140-3.txt$(Q)
else ifneq (,$(findstring FIPS140_3_OpenJCEPlus, $(TEST_FLAG)))
	FEATURE_PROBLEM_LIST_FILE:=-exclude:$(Q)$(JTREG_JDK_TEST_DIR)$(D)ProblemList-FIPS140_3_OpenJcePlus.txt$(Q)
endif

VENDOR_PROBLEM_LIST_FILE:=
ifeq ($(JDK_VENDOR),$(filter $(JDK_VENDOR),redhat azul alibaba microsoft eclipse))
	VENDOR_FILE:=excludes$(D)vendors$(D)$(JDK_VENDOR)$(D)ProblemList_openjdk$(JDK_VERSION).txt
	ifneq (,$(wildcard $(VENDOR_FILE)))
		VENDOR_PROBLEM_LIST_FILE:=-exclude:$(Q)$(TEST_ROOT)$(D)openjdk$(D)$(VENDOR_FILE)$(Q)
	endif
endif

# --add-modules jdk.incubator.foreign is removed for JDK19+
ADD_MODULES=
ifneq ($(filter 16 17 18, $(JDK_VERSION)),)
	ADD_MODULES=--add-modules jdk.incubator.foreign
endif
