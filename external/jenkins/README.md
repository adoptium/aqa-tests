## External (Third Party Container) Tests

Third Party container tests help verify that the AdoptOpenJDK binaries are good by running a variety of Java applications inside of Docker containers. AdoptOpenJDK/openjdk-tests/Issue #172 lists the applications that we have initially targeted to best exercise the AdoptOpenJDK binaries. For each application, we choose to run a selection of their functional tests.

## Running External tests locally

To run any AQA tests locally, you follow the same pattern:

Ensure your test machine is set up with test prereqs. For external tests, you do need Docker installed.

Please follow thi guide for prerequesties `https://github.com/adoptium/aqa-tests/blob/master/doc/Prerequisites.md`

- Download/unpack the SDK that you want to test to your test machine

- `export TEST_JDK_HOME=</pathToWhereYouInstalledSDK>`

- `git clone https://github.com/AdoptOpenJDK/openjdk-tests.git`

-` cd openjdk-tests`

- `./get.sh`

- `cd TKG`

export required environment variables, BUILD_LIST and EXTRA_DOCKER_ARGS (example: export BUILD_LIST=external/jenkins and export EXTRA_DOCKER_ARGS="-v $TEST_JDK_HOME:/opt/java/openjdk"

`make compile` (This fetches test material and compiles it, based on build.xml files in the test directories)

`make _jenkins_test` (When you defined BUILD_LIST to point to a directory in openjdk-tests/external, then this is a testCaseName from the playlist.xml file within the directory you chose)

When running these from the command-line, these tests are grouped under a make target called 'external', so 'make external' would run the entire set of tests found in the openjdk-tests/external directory. This is unadvisable! Limit what you compile and run, BUILD_LIST=external/<someSubDirectory>, and TARGET=<testCaseNameFromSubdirPlaylist>
