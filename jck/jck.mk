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
JCK_CUSTOM_TARGET ?=api/java_math/BigInteger
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

JCK_VERSION_NUMBER = $(JDK_VERSION)
ifeq (8, $(JDK_VERSION))
   JCK_VERSION_NUMBER = 8d
endif
ifeq (11, $(JDK_VERSION))
   JCK_VERSION_NUMBER = 11a
endif

JCK_VERSION = jck$(JCK_VERSION_NUMBER)

ifneq (,$(findstring zos,$(SPEC)))
   _BPXK_AUTOCVT:=ALL
endif

ifndef JCK_ROOT
   ifeq ($(CYGWIN),1)
      export JCK_ROOT=$(TEST_ROOT)/../../../jck_root/JCK$(JDK_VERSION)-unzipped
   else
      export JCK_ROOT=$(realpath $(TEST_ROOT)/../../../jck_root/JCK$(JDK_VERSION)-unzipped)
   endif 
  $(info JCK_ROOT is $(JCK_ROOT))
endif

ifndef CONFIG_ALT_PATH
  export CONFIG_ALT_PATH:=$(TEST_ROOT)$(D)jck$(D)jtrunner$(D)config
endif

OTHER_OPTS=
# if JDK_IMPL is openj9 or ibm
ifneq ($(filter openj9 ibm, $(JDK_IMPL)),)
  OTHER_OPTS= -Xtrace:maximal=all{level2} -Xfuture
  export CONFIG_ALT_PATH:=$(JCK_ROOT)$(D)config
  ifneq (,$(findstring zos, $(SPEC)))
    OTHER_OPTS += -Dcom.ibm.tools.attach.enable=yes
  endif
  ifneq (8, $(JDK_VERSION))
    OTHER_OPTS += --enable-preview
  endif
  # if JDK_VERSION >= 17
  ifeq ($(filter 8 9 10 11 12 13 14 15 16, $(JDK_VERSION)),)
    OTHER_OPTS += -XX:-OpenJ9CommandLineEnv
  endif
  ifeq (8, $(JDK_VERSION))
    ifneq (,$(findstring osx, $(SPEC)))
      OTHER_OPTS += -XstartOnFirstThread
    endif
  endif
endif

# If testsuite is not specified, default to RUNTIME
ifeq (,$(findstring testsuite, $(JCK_CUSTOM_TARGET)))
   override JCK_CUSTOM_TARGET := $(JCK_CUSTOM_TARGET) testsuite=RUNTIME
endif

# Additional JavaTestRunner options can be added via APPLICATION_OPTIONS
ifndef APPLICATION_OPTIONS
   APPLICATION_OPTIONS :=
endif

ifneq ($(filter openj9 ibm, $(JDK_IMPL)),)
	# TODO: APPLICATION_OPTIONS being overridden.
	APPLICATION_OPTIONS := customJtx=$(Q)
	DEV_EXCLUDES_HOME=$(JCK_ROOT)/excludes/dev
	ifneq (,$(wildcard $(DEV_EXCLUDES_HOME)/common.jtx))
		APPLICATION_OPTIONS+=$(DEV_EXCLUDES_HOME)/common.jtx
	endif
	ifneq (,$(wildcard $(DEV_EXCLUDES_HOME)/$(SPEC).jtx))
		APPLICATION_OPTIONS+=$(DEV_EXCLUDES_HOME)/$(SPEC).jtx
	endif
	ifneq (,$(findstring FIPS, $(TEST_FLAG)))
		ifneq (,$(wildcard $(DEV_EXCLUDES_HOME)/fips.jtx))
			APPLICATION_OPTIONS+=$(DEV_EXCLUDES_HOME)/fips.jtx
		endif
	endif
	APPLICATION_OPTIONS+=$(Q)
endif

JAVA_TO_TEST = $(JAVA_COMMAND)
ifeq ($(USE_JRE),1)
  JAVA_TO_TEST = $(JRE_COMMAND)
endif

JCK_CMD_TEMPLATE = $(JAVA_TO_TEST) -Djvm.options=$(Q)$(JVM_OPTIONS)$(Q) -Dother.opts=$(Q)$(OTHER_OPTS)$(Q) -cp $(TEST_ROOT)/jck/jtrunner/bin JavaTestRunner resultsRoot=$(REPORTDIR) testRoot=$(TEST_ROOT) jckRoot=$(JCK_ROOT) jckversion=$(JCK_VERSION) spec=$(SPEC) configAltPath=$(CONFIG_ALT_PATH) $(APPLICATION_OPTIONS)
WORKSPACE=/home/jenkins/jckshare/workspace/output_$(UNIQUEID)/$@

ifneq ($(filter aix_ppc-64 zos_390 linux_ppc-64_le linux_390-64, $(SPEC)),)
   # TODO: Replace with dynamically installed RI JDK
   REFERENCE_JAVA_CMD=/home/jenkins/jckshare/ri/jdk-11.0.19/bin/java
   USEQ='
   PREP = mkdir -p $(WORKSPACE); cp -rf $(CONFIG_ALT_PATH)$(D)jck$(JCK_VERSION_NUMBER)$(D)compiler.jti $(WORKSPACE)$(D); cp -rf $(TEST_ROOT)$(D)jck$(D)jtrunner $(WORKSPACE)
   GEN_JTB = $(PREP); ssh -o StrictHostKeyChecking=no jenkins@$(AGENT_NODE) $(USEQ)$(REFERENCE_JAVA_CMD) -Djvm.options=$(Q)$(JVM_OPTIONS)$(Q) -Dother.opts=$(Q)$(OTHER_OPTS)$(Q) -cp $(WORKSPACE)$(D)jtrunner$(D)bin JavatestUtil testRoot=$(TEST_ROOT) jckRoot=$(JCK_ROOT) jckversion=$(JCK_VERSION) testJava=$(JAVA_TO_TEST) riJava=$(REFERENCE_JAVA_CMD) workdir=$(WORKSPACE) configAltPath=$(CONFIG_ALT_PATH) agentHost=$(NODE_NAME) task=cmdfilegen spec=$(SPEC) testExecutionType=multijvm
   GEN_SUMMARY = $(JAVA_TO_TEST) -Djvm.options=$(Q)$(JVM_OPTIONS)$(Q) -Dother.opts=$(Q)$(OTHER_OPTS)$(Q) -cp $(TEST_ROOT)$(D)jck$(D)jtrunner$(D)bin JavatestUtil testRoot=$(TEST_ROOT) jckRoot=$(JCK_ROOT) jckversion=$(JCK_VERSION) configAltPath=$(CONFIG_ALT_PATH) workdir=$(WORKSPACE) spec=$(SPEC) task=summarygen
   START_AGENT = $(JAVA_TO_TEST) -Djavatest.security.allowPropertiesAccess=true -Djava.security.policy=$(JCK_ROOT)$(D)JCK-compiler-$(JCK_VERSION_NUMBER)$(D)lib$(D)jck.policy -classpath $(JCK_ROOT)$(D)JCK-compiler-$(JCK_VERSION_NUMBER)$(D)lib$(D)javatest.jar$(P)$(JCK_ROOT)$(D)JCK-compiler-$(JCK_VERSION_NUMBER)$(D)classes com.sun.javatest.agent.AgentMain -passive -trace &> $(WORKSPACE)$(D)agent.log &
   START_HARNESS = ssh -o StrictHostKeyChecking=no jenkins@$(AGENT_NODE) $(REFERENCE_JAVA_CMD) -jar $(JCK_ROOT)$(D)JCK-runtime-$(JCK_VERSION_NUMBER)$(D)lib$(D)javatest.jar -config $(WORKSPACE)$(D)compiler.jti @$(WORKSPACE)$(D)generated.jtb
else
   REFERENCE_JAVA_CMD=$(TEST_ROOT)/../additionaljdkbinary/bin/java
   GEN_JTB = $(REFERENCE_JAVA_CMD) -Djvm.options=$(Q)$(JVM_OPTIONS)$(Q) -Dother.opts=$(Q)$(OTHER_OPTS)$(Q) -cp $(TEST_ROOT)$(D)jck$(D)jtrunner$(D)bin JavatestUtil testRoot=$(TEST_ROOT) jckRoot=$(JCK_ROOT) jckversion=$(JCK_VERSION) testJava=$(JAVA_TO_TEST) riJava=$(REFERENCE_JAVA_CMD) workdir=$(REPORTDIR) configAltPath=$(CONFIG_ALT_PATH) task=cmdfilegen spec=$(SPEC) testExecutionType=multijvm 
   GEN_SUMMARY = $(JAVA_TO_TEST) -Djvm.options=$(Q)$(JVM_OPTIONS)$(Q) -Dother.opts=$(Q)$(OTHER_OPTS)$(Q) -cp $(TEST_ROOT)$(D)jck$(D)jtrunner$(D)bin JavatestUtil testRoot=$(TEST_ROOT) jckRoot=$(JCK_ROOT) jckversion=$(JCK_VERSION) configAltPath=$(CONFIG_ALT_PATH) workdir=$(REPORTDIR) spec=$(SPEC) task=summarygen
   START_AGENT = $(JAVA_TO_TEST) -Djavatest.security.allowPropertiesAccess=true -Djava.security.policy=$(JCK_ROOT)$(D)JCK-compiler-$(JCK_VERSION_NUMBER)$(D)lib$(D)jck.policy -classpath $(Q)$(JCK_ROOT)$(D)JCK-compiler-$(JCK_VERSION_NUMBER)$(D)lib$(D)javatest.jar$(P)$(JCK_ROOT)$(D)JCK-compiler-$(JCK_VERSION_NUMBER)$(D)classes$(Q) com.sun.javatest.agent.AgentMain -passive -trace &> $(REPORTDIR)$(D)agent.log &
   START_HARNESS = $(REFERENCE_JAVA_CMD) -jar $(JCK_ROOT)$(D)JCK-runtime-$(JCK_VERSION_NUMBER)$(D)lib$(D)javatest.jar -config $(CONFIG_ALT_PATH)$(D)jck$(JCK_VERSION_NUMBER)$(D)compiler.jti @$(REPORTDIR)$(D)generated.jtb
endif

$(shell chmod +x $(TEST_ROOT)$(D)jck$(D)agent-drive.sh)

START_MULTI_JVM_COMP_TEST = $(TEST_ROOT)$(D)jck$(D)agent-drive.sh '$(START_AGENT)' '$(START_HARNESS)'
GEN_JTB_GENERIC = $(JAVA_TO_TEST) -Djvm.options=$(Q)$(JVM_OPTIONS)$(Q) -Dother.opts=$(Q)$(OTHER_OPTS)$(Q) -cp $(TEST_ROOT)/jck/jtrunner/bin JavatestUtil testRoot=$(TEST_ROOT) jckRoot=$(JCK_ROOT) jckversion=$(JCK_VERSION) workdir=$(REPORTDIR) configAltPath=$(CONFIG_ALT_PATH) testJava=$(JAVA_TO_TEST) riJava=$(JAVA_TO_TEST) task=cmdfilegen spec=$(SPEC) $(APPLICATION_OPTIONS)
GEN_SUMMARY_GENERIC = $(JAVA_TO_TEST) -Djvm.options=$(Q)$(JVM_OPTIONS)$(Q) -Dother.opts=$(Q)$(OTHER_OPTS)$(Q) -cp $(TEST_ROOT)/jck/jtrunner/bin JavatestUtil testRoot=$(TEST_ROOT) jckRoot=$(JCK_ROOT) jckversion=$(JCK_VERSION) configAltPath=$(CONFIG_ALT_PATH) workdir=$(REPORTDIR) spec=$(SPEC) task=summarygen
START_AGENT_GENERIC = $(JAVA_TO_TEST) -Djavatest.security.allowPropertiesAccess=true -Djava.security.policy=$(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/lib/jck.policy -classpath $(Q)$(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/lib/javatest.jar$(P)$(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/classes$(Q) com.sun.javatest.agent.AgentMain -passive -trace &> $(REPORTDIR)/agent.log &
START_RMIREG = $(TEST_JDK_HOME)/bin/rmiregistry > $(REPORTDIR)$(D)rmiregistry.log &
START_RMID = $(TEST_JDK_HOME)/bin/rmid -J-Dsun.rmi.activation.execPolicy=none -J-Djava.security.policy=$(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/lib/jck.policy > $(REPORTDIR)$(D)rmid.log &
START_TNAMESRV = $(TEST_JDK_HOME)/bin/tnameserv -ORBInitialPort 9876 > $(REPORTDIR)$(D)tnameserv.log &
EXEC_RUNTIME_TEST = $(JAVA_TO_TEST) -jar $(JCK_ROOT)/JCK-runtime-$(JCK_VERSION_NUMBER)/lib/javatest.jar -config $(CONFIG_ALT_PATH)/$(JCK_VERSION)/runtime.jti @$(REPORTDIR)/generated.jtb
EXEC_COMPILER_TEST = $(JAVA_TO_TEST) -jar $(JCK_ROOT)/JCK-compiler-$(JCK_VERSION_NUMBER)/lib/javatest.jar -config $(CONFIG_ALT_PATH)/$(JCK_VERSION)/compiler.jti @$(REPORTDIR)/generated.jtb
EXEC_DEVTOOLS_TEST = $(JAVA_TO_TEST) -jar $(JCK_ROOT)/JCK-devtools-$(JCK_VERSION_NUMBER)/lib/javatest.jar -config $(CONFIG_ALT_PATH)/$(JCK_VERSION)/devtools.jti @$(REPORTDIR)/generated.jtb
EXEC_RUNTIME_TEST_WITH_AGENT = $(TEST_ROOT)/jck/agent-drive.sh '$(START_AGENT_GENERIC)' '$(EXEC_RUNTIME_TEST)'
EXEC_RUNTIME_TEST_WITH_RMI_SERVICES = $(EXEC_RUNTIME_TEST_WITH_AGENT)

ifeq ($(JDK_VERSION), 8)
	EXEC_RUNTIME_TEST_WITH_RMI_SERVICES = $(TEST_ROOT)/jck/agent-drive.sh '$(START_AGENT_GENERIC)' '$(START_RMIREG)' '$(START_RMID)' '$(START_TNAMESRV)' '$(EXEC_RUNTIME_TEST)'
	EXEC_RUNTIME_TEST_WITH_AGENT = $(TEST_ROOT)/jck/agent-drive.sh '$(START_AGENT_GENERIC)' '$(START_TNAMESRV)' '$(EXEC_RUNTIME_TEST)'
endif

ifeq ($(JDK_VERSION), 11)
	EXEC_RUNTIME_TEST_WITH_RMI_SERVICES = $(TEST_ROOT)/jck/agent-drive.sh '$(START_AGENT_GENERIC)' '$(START_RMIREG)' '$(START_RMID)' '$(EXEC_RUNTIME_TEST)'
endif

ifeq (8, $(JDK_VERSION))
   include $(TEST_ROOT)/jck/jck8.mk
endif 

ifeq (11, $(JDK_VERSION))
   include $(TEST_ROOT)/jck/jck11.mk
endif 

ifeq (17, $(JDK_VERSION))
   include $(TEST_ROOT)/jck/jck17.mk
endif

ifeq (19, $(JDK_VERSION))
   include $(TEST_ROOT)/jck/jck19.mk
endif

ifeq (20, $(JDK_VERSION))
   include $(TEST_ROOT)/jck/jck20.mk
endif

ifeq (21, $(JDK_VERSION))
   include $(TEST_ROOT)/jck/jck21.mk
endif
