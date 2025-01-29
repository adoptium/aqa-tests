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

# Environment variable OSTYPE is set to cygwin if running under cygwin.
# Set our own macro to indicate we're running under cygwin.
ifndef CYGWIN
  OSTYPE?=$(shell echo $$OSTYPE)
  CYGWIN:=0
  ifeq ($(OSTYPE),cygwin)
    CYGWIN:=1
  endif
  ifeq ($(TERM),cygwin)
    CYGWIN:=1
  endif
endif
$(warning CYGWIN is $(CYGWIN))

ifeq ($(CYGWIN),1)
  # If we are running under cygwin, the tests need to run with a Windows perl port (e.g. Strawberry perl) rather
  # than the cygwin perl port. This assumes that version will be in a directory ending /perl/bin directory
  # and the cygwin version will not. Once found, that version of perl is added ahead of cygwin perl in the PATH.
  $(warning Running under cygwin, looking for Windows perl on path)
  PERL:=$(shell which -a perl.exe | grep /perl/bin | sort | uniq)
  ifeq (,$(PERL))
    $(error Unable to find Windows perl e.g. Strawberry perl in a /perl/bin subdirectory on PATH.  Install perl or add to PATH and retry)
  else
    $(warning Found perl in $(PERL))
  endif
  PERL:=$(dir $(PERL))
  export PATH:=$(PERL):$(PATH)
endif

SYSTEMTEST_RESROOT=$(TEST_RESROOT)/../

ifeq (,$(findstring $(JDK_IMPL),hotspot))
  OPENJ9_PRAM=;$(SYSTEMTEST_RESROOT)$(D)openj9-systemtest
else
  OPENJ9_PRAM=""
endif

# In JDK 24+ any attempts to enable the security manager will result in an error.
# In JDK18+, java.security.manager == null behaves as -Djava.security.manager=disallow.
# In JDK17-, java.security.manager == null behaves as -Djava.security.manager=allow.
# This is needed for Mauve tests that use the SecurityManager.
ifneq ($(filter 18 19 20 21 22 23, $(JDK_VERSION)),)
  export JAVA_TOOL_OPTIONS:=-Djava.security.manager=allow
  $(warning Environment variable JAVA_TOOL_OPTIONS is set to '$(JAVA_TOOL_OPTIONS)')
endif

JAVA_ARGS = $(JVM_OPTIONS)
ifeq (,$(findstring $(JDK_IMPL),hotspot))
  JAVA_ARGS += -Xdump:system:events=user
endif

ifndef APPLICATION_OPTIONS
  APPLICATION_OPTIONS :=
endif
ifdef JVM_OPTIONS
  APPLICATION_OPTIONS := $(APPLICATION_OPTIONS) -jvmArgs $(Q)$(JVM_OPTIONS)$(Q)
endif

ifndef SYSTEM_LIB_DIR
  SYSTEM_LIB_DIR=$(SYSTEMTEST_RESROOT)/systemtest_prereqs
endif

define SYSTEMTEST_CMD_TEMPLATE
perl $(SYSTEMTEST_RESROOT)$(D)STF$(D)stf.core$(D)scripts$(D)stf.pl \
  -test-root=$(Q)$(SYSTEMTEST_RESROOT)$(D)STF;$(SYSTEMTEST_RESROOT)$(D)aqa-systemtest$(OPENJ9_PRAM)$(Q) \
  -systemtest-prereqs=$(Q)$(SYSTEM_LIB_DIR)$(D)$(Q) \
  -java-args=$(SQ)$(JAVA_ARGS)$(SQ) \
  -results-root=$(REPORTDIR)
endef

# Default test to be run for system_custom in regular system test builds
CUSTOM_TARGET ?= -test=ClassloadingLoadTest

ifneq ($(JDK_VERSION),8)
  ADD_OPENS_CMD=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
else
  ADD_OPENS_CMD=
endif
