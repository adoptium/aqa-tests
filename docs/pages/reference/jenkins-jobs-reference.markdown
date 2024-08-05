<a name="reference"></a>
# Reference Documentation for Jenkins Test Jobs
These instructions describe running Grinder jobs at ci.adoptium.net but also loosely apply to Grinder jobs at the OpenJ9 Jenkins server (and internal servers).  Login to the ci.adoptium.net Jenkins server and click [Build with Parameters](https://ci.adoptium.net/view/Test_grinder/job/Grinder/build?delay=0sec), or go to the [Grinder job](https://ci.adoptium.net/view/Test_grinder/job/Grinder/) in your internal Jenkins instance and set your own parameters.

<a name="permissions"></a>
## Access Permissions
For access permissions to the Grinder job at ci.adoptium.net, you need to be added to the test-triage Github team (requires [2FA](https://docs.github.com/en/authentication/securing-your-account-with-two-factor-authentication-2fa/configuring-two-factor-authentication) on your Github account).

<a name="groupGranularity"></a>
## Grouping and Granularity 
We have many tests, so it is important to be able to slice and dice them in different ways in order to be efficient with testing, debugging and triage.  Tests are organized into groups typically based on where they came from and what type of test they are [openjdk|perf|system|functional|external].

<img width="726" alt="groupGranularity" src="https://user-images.githubusercontent.com/2836948/220690573-bacaa5ce-a98f-4ac1-a357-59a0571693f1.png">

<a name="parameters"></a>
## Input Parameters

AQAvit test jobs Parameters are grouped logically by the type of input they are.

<a name="testRepos"></a>
### Test Repositories Parameters
Repositories where we pull test material from. Unless you are testing test code, these do not need to be changed.

* ADOPTOPENJDK_REPO : optional: your fork of aqa-tests
* ADOPTOPENJDK_BRANCH : optional, your branch off of your fork of aqa-tests
* OPENJ9_REPO : optional, your fork of openj9
* OPENJ9_BRANCH : optional, your branch of your fork of openj9
* OPENJ9_SHA : optional, pin to a specific SHA of openj9
* JDK_REPO : optional, use test material from a particular OpenJDK repository
* JDK_BRANCH : optional, use test material from a particular OpenJDK branch
* OPENJDK_SHA : optional, pin to a particular OpenJDK SHA
* TKG_OWNER_BRANCH : optional, use a particular adoptium/TKG fork/branch
* ADOPTOPENJDK_SYSTEMTEST_OWNER_BRANCH : optional, use a particular adoptium/aqa-systemtest fork/branch
* OPENJ9_SYSTEMTEST_OWNER_BRANCH : optional, use a particular openj9/openj9-systemtest fork/branch
* STF_OWNER_BRANCH : optional, use a particular adoptium/STF fork/branch
* JCK_GIT_REPO : optional, use a particular private repository for JCK test material supplied under OCTLA

<a name="vendorRepos"></a>
### Non-AQA Test Repositories
Additional test repositories that can be overlaid onto existing AQA test material for extra testing.  This is used for laying down smoke tests in the temurin-build repository and installer tests in the installer repository, along with any private vendor tests that can not be run in the open.

* VENDOR_TEST_REPOS : optional, comma-separated list of repositories containing additional test material
* VENDOR_TEST_BRANCHES : optional, comma-separated list of branches for additional test material
* VENDOR_TEST_SHAS : optional, pin to particular SHAS of VENDOR_TEST_REPOS
* VENDOR_TEST_DIRS : optional, directories within VENDOR_TEST_REPOS where to find test material
* USER_CREDENTIALS_ID : optional, credentials to use if VENDOR_TEST_REPOS require them

<a name="platformsMachines"></a>
### Platform and Machine Selection Parameters
Choose which platform to run tests on, and optionally specify machine label to send the test job to a certain machine is desired.

* PLATFORM : required, platform on which to run the test (used to determine machines labels to find machines on which to run).  Refer to [PLATFORM_MAP](https://github.com/adoptium/aqa-tests/blob/master/buildenv/jenkins/openjdk_tests) for a complete list of supported platforms.
* LABEL : optional, set to hostname for a specific machine, otherwise, if blank, machines matching the PLATFORM labels will be used
* LABEL_ADDITION : optional, additional label to append to the LABEL for more specificity
* DOCKER_REQUIRED : optional, boolean appends `sw.tool.docker` to the LABEL for tests that require Docker to be installed
* DOCKERIMAGE_TAG : optional, used by external test group to pinpoint a particular image to pull and use by tests
* EXTRA_DOCKER_ARGS : optional, extra Docker arguments to pass into external test group
* SSH_AGENT_CREDENTIAL : optional, set if needed to fetch images from a secure registry
* ACTIVE_NODE_TIMEOUT : optional, minutes to wait on the label-matching node to become active

<a name="jdkSelection"></a>
### JDK Selection Parameters
Specify where to pick up JDK from and provide extra details if taking from upstream or customized.

* SDK_RESOURCE : required, choice between upstream|releases|nightly|customized
* JDK_VERSION : required, JDK_VERSION that matches the JDK binary under test e.g., 8, 11, 17, etc
* JDK_IMPL : required, different JVM implementations (hotspot, openj9, sap, ibm)
* CUSTOMIZED_SDK_URL : optional, use when SDK_RESOURCE=customized, URL to a JDK binary to use for testing, optional, include space-separated link to download native test libs or JRE (if using a different JRE than the one packaged with JDK used for compilation)
* CUSTOMIZED_SDK_URL_CREDENTIAL_ID : optional, if needed to access CUSTOMIZED_SDK_URL binary
* TEST_IMAGES_REQUIRED : optional, pick up test images from the same location as SDK_RESOURCE, if they are available
* UPSTREAM_JOB_NAME : optional, use when SDK_RESOURCE=upstream
* UPSTREAM_JOB_NUMBER : optional, use in tandem with UPSTREAM_JOB_NAME
* AUTO_DETECT : optional, boolean to AUTO_DETECT JDK_VERSION, JDK_IMPL and SPEC of JDK_RESOURCE=customized

<a name="testSelection"></a>
### Test Selection Parameters
Provide parameters for which test material to build and which test targets to run.

* BUILD_LIST : required, pinpoint the directories that will be compiled  e.g., openjdk, system, perf, external, functional.  Any standalone sub test directory you wish to compile, perf/dacapo, etc.
* DYNAMIC_COMPILE : optional, boolean, TKG figures out necessary dependencies and reduces the amount of test material compiled
* TARGET : required, which test target you want to run.  Top-level targets such as sanity.openjdk, extended.system are inappropriate for Grinders as they contain so many sub-targets and have a long execution time.  Test targets are defined in playlist.xml files as testCaseName tags (example jdk_math or [jdk_custom](https://github.com/adoptium/aqa-tests/blob/master/openjdk/playlist.xml#L18)).

* CUSTOM_TARGET : if TARGET=jdk_custom|hotspot_custom|langtools_custom, you can set this to be the specific test class name (or test directory or space-separated list of test classes). For example test/jdk/java/math/BigInteger/BigIntegerTest.java

<a name="testOptions"></a>
### Additional Test Options Parameters 
* TEST_FLAG : optional. Set to JITAAS for testing JITAAS SDK
* EXTRA_OPTIONS : optional, set this to **append** additional JVM options to the test run
<br>In general, JVM options can be directly used. Please try to use JVM options as it is. For example, `-Xaot:{jdk/incubator/vector/*Vector*Mask.lambda*anyTrue*}(traceFull,traceRelocatableDataCG,traceRelocatableDataDetailsCG,log=log.trc)` can be directly used for **openjdk tests**. However, for **system tests**, the special characters may need to be escaped in EXTRA_OPTIONS and JVM_OPTIONS.
                            <br>Example 1:<br>
                            `'-Xjit:count=0,\{java/lang/reflect/Method.getParameterAnnotations*\}\(traceFull,log=getParameterAnnotations.log\)'`
                            <br>Example 2:<br>
                            `-Xjit:\"{java/util/HashMap.*}(optlevel=noopt)\"`
* JVM_OPTIONS : optional, set this to **replace** the JVM options of the test run
* BUILD_IDENTIFIER :
* ITERATIONS : number of times to repeat the execution of the test run (on one machine)
* TIME_LIMIT : optional, hours at which point to limit the Jenkins job to run, if not completed, it aborts at this time limit

<a name="testParallelization"></a>
### Test Parallelization Parameters
Additional test options if you wish to run in various parallel modes

* PARALLEL : optional, several modes of parallelization supported [None|Dynamic|Subdir|NodesByIterations], where:
    *   **None** -> "run tests serially"
    *   **Dynamic** -> "when running multiple targets, try to retrieve test execution times and dynamically calculate how to divide tests across NUM_MACHINES"
    *   **NodesByIterations** -> "when running a single target, run ITERATIONS of a test across NUM_MACHINES"
* NUM_MACHINES : optional, how many machines to parallelize across
* GENERATE_JOBS : optional, boolean to force generate child jobs
* PERSONAL_BUILD : optional, boolean setting for indicating this is a personal build
* UPSTREAM_TEST_JOB_NAME : auto-populated, when child jobs generated
* UPSTREAM_TEST_JOB_NUMBER : auto-populated, when child jobs generated

<a name="postRun"></a>
### Post Run Parameters
Parameters to determine what to do with post run artifacts.

* ARCHIVE_TEST_RESULTS : optional. If checked, the test outputs will be archived regardless of the test result
* KEEP_REPORTDIR : optional, useful for showing NUnit output details with JUnit Jenkins plugin
* ARTIFACTORY_SERVER : optional, send artifacts to an Artifactory server if its configured
* ARTIFACTORY_REPO : optional, use in conjuction with ARTIFACTORY_SERVER
* ARTIFACTORY_ROOT_DIR : optional, use in conjuction with ARTIFACTORY_SERVER
* CLOUD_PROVIDER : optional, if set, Jenkins jobs may try to spin up dynamic agents to run tests on, if all real nodes are in use
* USE_TESTENV_PROPERTIES : optional, boolean, use the values provided in testenv.properties file to pin to particular versions of test material, ignoring all settings in "**Test Repositories Parameters**"
* RERUN_ITERATIONS : optional, if set, indicates that when test targets fail, they are to be rerun this many times
* RELATED_NODES :  setting the client machine label for use in client/server testing

<a name="grinderEtiquette"></a>
## Grinder Etiquette

1. When possible, avoid running top-level targets (all of sanity.openjdk tests, etc, since we have nightly runs for those), or runs with 100x+ ITERATIONS.  These can take hours, and will block the regular testing, especially on platforms where we have few machines.
2. Do not mark Grinder runs as “keep forever”, there is a limited amount of space on Jenkins master.  Download any artifacts you need from your run as soon as it finishes to attach to issues, etc, and consider the Jenkins job transient.
3. When reporting failures in issues, understand that Grinder jobs are transient (not kept for long on Jenkins).  Sharing a link to the Grinder is only useful for a short time.  Share the "Rerun in Grinder" link which has details of the impl/version/test target, output statistics & test artifacts in the issue, those are more concrete items when reproducing failures.  For openjdk test failures, here are some useful triage instructions: https://github.com/adoptium/aqa-tests/wiki/Guidance-for-Creating-OpenJDK-Test-Defects.
4. Pro-actively delete Grinder runs that you do not need (setup fails, or mistake in target, etc).  We keep last 50 runs, and if you delete your unneeded runs immediately, other jobs will stay a little bit longer giving others more time to grab artifacts.

