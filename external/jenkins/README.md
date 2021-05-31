## External Jenkins Tests

Jenkins tests are part of the external third-party application tests that help verify that the AdoptOpenJDK binaries are good by running a variety of Java applications inside of Docker containers. Adoptium/aqa-tests/Issue #172 lists the applications that we have initially targeted to best exercise the AdoptOpenJDK binaries. For each application, we choose to run a selection of their functional tests.

Jenkins test material is pulled from` https://github.com/jenkinsci/jenkins`

## Running External Jenkins tests locally

To run any AQA tests locally, you follow the same pattern:

0. Ensure your test machine is set up with [test prereqs]`(https://github.com/AdoptOpenJDK/aqa-tests/blob/master/doc/Prerequisites.md)`. For external tests, you do need Docker installed.
1. Download/unpack the SDK that you want to test to your test machine
1. `export TEST_JDK_HOME=</pathToWhereYouInstalledSDK>`
1. `git clone https://github.com/adoptium/aqa-tests.git`
1. `cd aqa-tests`
1. `./get.sh`
1. `cd TKG`
1. export required environment variables, BUILD_LIST and EXTRA_DOCKER_ARGS (`export BUILD_LIST=external/jenkins> and <export EXTRA_DOCKER_ARGS="-v $TEST_JDK_HOME:/opt/java/openjdk"`)
1. `make compile` (This fetches test material and compiles it, based on build.xml files in the test directories)
1. `make _jenkins_test` (When you defined BUILD_LIST to point to a directory in aqa-tests/external, then this is a testCaseName from the playlist.xml file within the directory you chose).

When [running these from the command-line]`(https://github.com/AdoptOpenJDK/aqa-tests/blob/master/doc/userGuide`.md#local-testing-via-make-targets-on-the-commandline), these tests are grouped under a make target called 'external', so 'make external' would run the entire set of tests found in the aqa-tests/external directory. This is unadvisable! Limit what you compile and run, BUILD_LIST=external/`<someSubDirectory>`, and TARGET=`<testCaseNameFromSubdirPlaylist>`
