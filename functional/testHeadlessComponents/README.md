# External TestHeadlessComponents Tests

Test of correct behavior of swing components in a headless jre environment.

## Running TestHeadlessComponents locally

### Setting up the environment:
Nothing special is required, just install the java package or unpack the portable you want the test.

### ENV variables:
The test utilizes *JAVA_HOME variable, so if it is not set it will not work correctly! In case of testing portable packages please set it up first.
BOOTJDK_DIR - directory to store bootjdk which is used to compile the code in case we are testing jre only installation. Uses ~/bootjdk by default and uses Adoptium latest build of relevant jdk for this purpose.
BOOTJDK_ARCHIVE_DIR - in case the user wants to use arbitrary jdk build, he can provide path to its archive and it will be used in jre execution. Creates $WORKSPACE/bootjdkarchive and downloads latest temurin archive if not set.
WORKSPACE - directory where the testsuite is going to execute all the tests. ~/workspace by default
TMPRESULTS - this is a location where the logfiles will be after the testsuite finishes. Same as WORKSPACE by default.
*JREJDK - This tells the testsuite whether we are testing jre or jdk.
ARCH - architecture of our system. The suite detects this automatically if left blank.
*OJDK_VERSION_NUMBER - number of jdk we are testing - this is used for downloading corresponding JDK from adoptium site.

Variables marked with "*" are mandatory and the testsuite wont run without them.

### Executing the testsuite

Now it is as simple as running bash script testHeadlessComponents.sh.


