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
ifndef JCK_CUSTOM_TARGET
JCK_CUSTOM_TARGET ?=api/java_math
endif

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


ifndef JCK_VERSION
  ifeq (8, $(JDK_VERSION))
    export JCK_VERSION=jck8c
  else
    export JCK_VERSION=jck$(JDK_VERSION)
  endif
endif

ifndef JCK_ROOT
  export JCK_ROOT=$(TEST_ROOT)/../../../jck_root/JCK$(JDK_VERSION)-unzipped
endif

SYSTEMTEST_RESROOT=$(TEST_RESROOT)/../../system

define JCK_CMD_TEMPLATE
perl $(TEST_RESROOT)$(D)..$(D)..$(D)system$(D)stf$(D)stf.core$(D)scripts$(D)stf.pl \
	-test-root=$(Q)$(TEST_RESROOT)$(D)..$(D)..$(D)system$(D)stf;$(TEST_RESROOT)$(D)..$(D)..$(D)system$(D)openjdk-systemtest$(Q) \
	-systemtest-prereqs=$(Q)$(SYSTEMTEST_RESROOT)$(D)systemtest_prereqs;$(JCK_ROOT)$(Q) \
	-java-args-setup=$(Q)$(JVM_OPTIONS)$(Q) \
	-results-root=$(REPORTDIR) \
	-test=Jck 
endef
