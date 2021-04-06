## Running External tests locally
To run any AQA tests locally, you follow the same pattern:

0. Ensure your test machine is set up with [test prereqs](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/doc/Prerequisites.md).  For external tests, you do need Docker installed.

1. Download/unpack the SDK you want to your test machine
2. `export TEST_JDK_HOME=</pathToWhereYouInstalledSDK>` 
3. `git clone https://github.com/AdoptOpenJDK/openjdk-tests.git` 
4. `cd openjdk-tests`
5. `./get.sh`
6. `cd TKG`
7. export required environment variables, BUILD_LIST and EXTRA_DOCKER_ARGS (example: `export BUILD_LIST=external/jacoco` and `export EXTRA_DOCKER_ARGS="-v $TEST_JDK_HOME:/opt/java/openjdk"`
8. `make wycheproof`        (This fetches test material and compiles it, based on build.xml files in the test directories)
9. `make wycheproof_test`   (When you defined BUILD_LIST to point to a directory in [openjdk-tests/external](https://github.com/AdoptOpenJDK/openjdk-tests/tree/master/external), then this is a testCaseName from the playlist.xml file within the directory you chose)
