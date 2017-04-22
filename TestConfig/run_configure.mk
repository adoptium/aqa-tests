# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

.PHONY: autogen clean help

.DEFAULT_GOAL := autogen

help:
	@echo "This makefile is used to run perl script which generates makefiles for JVM tests before being built and executed."
	@echo "OPTS=help     Display help information for more options."

CURRENT_DIR := $(shell pwd)
OPTS=

D=/

ifneq (,$(findstring Win,$(OS)))
CURRENT_DIR := $(subst \,/,$(CURRENT_DIR))
endif

autogen:
	cd $(CURRENT_DIR)$(D)scripts$(D)testKitGen; \
	perl testKitGen.pl $(OPTS); \
	cd $(CURRENT_DIR);

AUTOGEN_FILES = $(wildcard $(CURRENT_DIR)$(D)jvmTest.mk)
AUTOGEN_FILES += $(wildcard $(CURRENT_DIR)$(D)..$(D)*$(D)autoGenTest.mk)

clean:
ifneq (,$(findstring .mk,$(AUTOGEN_FILES)))
	$(RM) $(AUTOGEN_FILES);
else
	@echo "Nothing to clean";
endif