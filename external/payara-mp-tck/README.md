<!--
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
-->
# External Payara-mp-tck Tests

Payara-mp-tck(payara MicroProfile-tck) tests are part of the third-party application tests that helps verify that the Adoptium binaries are good by running a variety of Java applications inside of Docker containers. AdoptOpenJDK/openjdk-tests/Issue #172 lists the applications that we have initially targeted to best exercise the Adoptium binaries. For each application, we choose to run a selection of their functional tests.payara-mp-tck tests are functional tests pulled from the [payara/MicroProfile-TCK-Runners](https://github.com/payara/MicroProfile-TCK-Runners.git) repository.

## Running  External Payara-mp-tck tests locally
To run any AQA tests locally, you follow the same pattern:

0. Ensure your test machine is set up with [test prereqs](https://github.com/adoptium/aqa-tests/blob/master/doc/Prerequisites.md).  For external tests, you do need Docker installed.

1. Download/unpack the SDK that you want to test to your test machine.
2. `export TEST_JDK_HOME=</pathToWhereYouInstalledSDK>` 
3. `git clone https://github.com/adoptium/aqa-tests.git` 
4. `cd openjdk-tests`
5. `./get.sh`
6. `cd TKG`
7. Export required environment variables, BUILD_LIST and EXTRA_DOCKER_ARGS (example: `export BUILD_LIST=external/payara-mp-tck` and `export EXTRA_DOCKER_ARGS="-v $TEST_JDK_HOME:/opt/java/openjdk"`
8. `make compile`              (This fetches test material and compiles it, based on build.xml files in the test directories).
9. `make _payara-mp-tck_test`   (When you defined BUILD_LIST to point to a directory in [openjdk-tests/external](https://github.com/adoptium/aqa-tests/tree/master/external), then this is a testCaseName from the playlist.xml file within the directory you chose).

When [running these from the command-line](https://github.com/adoptium/aqa-tests/blob/master/doc/userGuide.md#local-testing-via-make-targets-on-the-commandline), these tests are grouped under a make target called 'external', so 'make external' would run the entire set of tests found in the openjdk-tests/external directory.  This is unadvisable!  Limit what you compile and run, BUILD_LIST=external/`<someSubDirectory>`, and TARGET=`<testCaseNameFromSubdirPlaylist>`  

These tests run regularly and results can be found in [TRSS Third Party Application view](https://trss.adoptopenjdk.net/ThirdPartyAppView).

