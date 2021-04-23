Running External tests locally
 To run any AQA tests locally, you follow the same pattern:

 Ensure your test machine is set up with test prereqs. For external tests, you do need Docker installed.

1. Download/unpack the SDK that you want to test to your test machine

2. export TEST_JDK_HOME=</pathToWhereYouInstalledSDK>

3. git clone https://github.com/AdoptOpenJDK/openjdk-tests.git

4. cd openjdk-tests

5. ./get.sh

6. cd TKG

7. export required environment variables, BUILD_LIST and EXTRA_DOCKER_ARGS (<export BUILD_LIST=external/jacoco> and <export EXTRA_DOCKER_ARGS="-v $TEST_JDK_HOME:/opt/java/openjdk">)

8. <make compile> (This fetches test material and compiles it, based on build.xml files in the test directories)

9.<make _jacoco_test> (When you defined BUILD_LIST to point to a directory in openjdk-tests/external, then this is a testCaseName from the playlist.xml file within the directory you chose)