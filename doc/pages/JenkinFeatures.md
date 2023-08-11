# Jenkins Features Reference
The "Jenkins Features Reference" is a comprehensive documentation outlining the various [configuration parameters](https://ci.adoptium.net/view/Test_grinder/job/Grinder/build?delay=0sec) and options available for running [Grinder jobs](https://ci.adoptium.net/view/Test_grinder/job/Grinder/) on the Jenkins server.
It serves as a guide for testers and developers using AQAvit Grinder to perform testing and triage on the ci.adoptium.net Jenkins server and the OpenJ9 Jenkins server (and internal servers).
The reference provides detailed explanations and instructions for each configuration parameter, enabling users to customize their test jobs effectively.

## Access Permissions
For access permissions to the Grinder job at ci.adoptium.net, you need to be part of the `test-triage` Github team (requires [2FA](https://docs.github.com/en/authentication/securing-your-account-with-two-factor-authentication-2fa/configuring-two-factor-authentication) on your Github account).

## Grouping and Granularity
We have many tests, so it is important to be able to slice and dice them in different ways to be efficient with testing, debugging, and triage.
Tests are organized into groups typically based on where they came from and what type of test they are: `openjdk`, `perf`, `system`,  `functional`, `external`.

## Input Parameters
AQAvit test job parameters are grouped logically by the type of input they are.

### Test Repositories Parameters
Repositories where we pull test material from. Unless you are testing test code, these do not need to be changed.

| Parameter                             | Description                                                                                               |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `ADOPTOPENJDK_REPO`                   | Optional. Your fork of `aqa-tests`.                                                                      |
| `ADOPTOPENJDK_BRANCH`                 | Optional. Your branch off of your fork of `aqa-tests`.                                                  |
| `OPENJ9_REPO`                         | Optional. Your fork of `openj9`.                                                                         |
| `OPENJ9_BRANCH`                       | Optional. Your branch of your fork of `openj9`.                                                         |
| `OPENJ9_SHA`                          | Optional. Pin to a specific SHA of `openj9`.                                                            |
| `JDK_REPO`                            | Optional. Use test material from a specific OpenJDK repository.                                         |
| `JDK_BRANCH`                          | Optional. Use test material from a specific OpenJDK branch.                                             |
| `OPENJDK_SHA`                         | Optional. Pin to a specific OpenJDK SHA.                                                                |
| `TKG_OWNER_BRANCH`                    | Optional. Use a specific `adoptium/TKG` fork/branch.                                                    |
| `ADOPTOPENJDK_SYSTEMTEST_OWNER_BRANCH`| Optional. Use a specific `adoptium/aqa-systemtest` fork/branch.                                         |
| `OPENJ9_SYSTEMTEST_OWNER_BRANCH`       | Optional. Use a specific `openj9/openj9-systemtest` fork/branch.                                        |
| `STF_OWNER_BRANCH`                    | Optional. Use a specific `adoptium/STF` fork/branch.                                                    |
| `JCK_GIT_REPO`                        | Optional. Use a specific private repository for JCK test material supplied under OCTLA.                |


## Non-AQA Test Repositories
Additional test repositories that can be overlaid onto existing AQA test material for extra testing.
This is used for laying down smoke tests in the temurin-build repository and installer tests in the installer repository, along with any private vendor tests that cannot be run in the open.

- `VENDOR_TEST_REPOS`: optional, comma-separated list of repositories containing additional test material.
- `VENDOR_TEST_BRANCHES`: optional, comma-separated list of branches for additional test material.
- `VENDOR_TEST_SHAS`: optional, pin to particular SHAS of VENDOR_TEST_REPOS.
- `VENDOR_TEST_DIRS`: optional, directories within VENDOR_TEST_REPOS where to find test material.
- `USER_CREDENTIALS_ID`: optional, credentials to use if VENDOR_TEST_REPOS require them.

### Platform and Machine Selection Parameters
Choose which platform to run tests on and optionally specify the machine label to send the test job to a certain machine if desired.

| Parameter              | Description                                                                                             |
|------------------------|---------------------------------------------------------------------------------------------------------|
| `PLATFORM`             | Required. Platform on which to run the test. Refer to [PLATFORM_MAP](https://github.com/adoptium/aqa-tests/blob/master/buildenv/jenkins/openjdk_tests) for supported platforms. |
| `LABEL`                | Optional. Set to the hostname for a specific machine; otherwise, use machines with matching PLATFORM labels. |
| `LABEL_ADDITION`       | Optional. Additional label to append to LABEL for more specificity.                                     |
| `DOCKER_REQUIRED`      | Optional. Boolean. Appends `sw.tool.docker` to LABEL for tests requiring Docker.                        |
| `DOCKERIMAGE_TAG`      | Optional. Used by the external test group to specify a specific Docker image tag.                        |
| `EXTRA_DOCKER_ARGS`    | Optional. Extra Docker arguments for the external test group.                                           |
| `SSH_AGENT_CREDENTIAL` | Optional. Set if needed to fetch images from a secure registry.                                         |
| `ACTIVE_NODE_TIMEOUT`  | Optional. Timeout in minutes to wait for the label-matching node to become active.                       |

### JDK Selection Parameters
Specify where to pick up JDK from and provide extra details if taking from upstream or customized.

| Parameter                           | Description                                                                                                 |
|------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `SDK_RESOURCE`                      | Required. Choose from `upstream`, `releases`, `nightly`, or `customized`.                                 |
| `JDK_VERSION`                       | Required. JDK version that matches the JDK binary under test (e.g., 8, 11, 17, etc.).                     |
| `JDK_IMPL`                          | Required. Different JVM implementations (`hotspot`, `openj9`, `sap`, `ibm`).                              |
| `CUSTOMIZED_SDK_URL`                | Optional. Required when `SDK_RESOURCE=customized`. URL to a JDK binary for testing.                       |
| `CUSTOMIZED_SDK_URL_CREDENTIAL_ID`  | Optional. Used to access the binary when `CUSTOMIZED_SDK_URL` is provided.                                |
| `TEST_IMAGES_REQUIRED`              | Optional. Picks up test images from the same location as `SDK_RESOURCE` if available.                      |
| `UPSTREAM_JOB_NAME`                 | Optional. Required when `SDK_RESOURCE=upstream`.                                                          |
| `UPSTREAM_JOB_NUMBER`               | Optional. Used in tandem with `UPSTREAM_JOB_NAME`.                                                        |
| `AUTO_DETECT`                       | Optional. Boolean to automatically detect `JDK_VERSION`, `JDK_IMPL`, and `SPEC` for `SDK_RESOURCE=customized`. |


### Test Selection Parameters
Provide parameters for which test material to build and which test targets to run.

| Parameter          | Description                                                                                                           |
|--------------------|-----------------------------------------------------------------------------------------------------------------------|
| `BUILD_LIST`       | Required. Specifies directories to be compiled, e.g., `openjdk`, `system`, `perf`, `external`, `functional`.         |
| `DYNAMIC_COMPILE`  | Optional. Boolean. TKG identifies dependencies and reduces compiled test material.                                 |
| `TARGET`           | Required. Specifies the test target. Cannot use top-level targets like `sanity.openjdk`, `extended.system`.        |
|                    | Test targets are defined in `playlist.xml` files as `testCaseName` tags, like `jdk_math` or [jdk_custom](https://github.com/adoptium/aqa-tests/blob/master/openjdk/playlist.xml#L18). |
| `CUSTOM_TARGET`    | Required when `TARGET=jdk_custom|hotspot_custom|langtools_custom`. Specifies a specific test class, directory, or space-separated list of test classes. For instance, `test/jdk/java/math/BigInteger/BigIntegerTest.java`. |

### Additional Test Options Parameters
| Parameter           | Description                                                                                              |
|---------------------|----------------------------------------------------------------------------------------------------------|
| `TEST_FLAG`         | Optional. Set to `JITAAS` for testing JITAAS SDK.                                                        |
| `EXTRA_OPTIONS`     | Optional. Appends additional JVM options to the test run.                                                |
|                     | - Use JVM options directly. For example, `-Xaot:{jdk/incubator/vector/*Vector*Mask.lambda*anyTrue*}(traceFull,traceRelocatableDataCG,traceRelocatableDataDetailsCG,log=log.trc)`. |
|                     | - Special characters may need escaping for system tests in `EXTRA_OPTIONS` and `JVM_OPTIONS`.            |
|                     | Example 1: `-Xjit:count=0,{java/lang/reflect/Method.getParameterAnnotations*}(traceFull,log=getParameterAnnotations.log)`. |
|                     | Example 2: `-Xjit:"{java/util/HashMap.*}(optlevel=noopt)"`.                                            |
| `JVM_OPTIONS`       | Optional. Replaces the JVM options of the test run.                                                     |
| `BUILD_IDENTIFIER`  | Placeholder for the build identifier.                                                                   |
| `ITERATIONS`        | Number of times to repeat the execution of the test run on one machine.                                 |
| `TIME_LIMIT`        | Optional. Hours at which to limit the Jenkins job; it aborts if not completed by this time limit.      |

### Test Parallelization Parameters
Additional test options if you wish to run in various parallel modes.

| Parameter                 | Description                                                                                             |
|--------------------------|---------------------------------------------------------------------------------------------------------|
| `PARALLEL`               | Optional. Modes of parallelization supported: `None`, `Dynamic`, `Subdir`, `NodesByIterations`.        |
|                          | - `None`: Run tests serially.                                                                          |
|                          | - `Dynamic`: Calculate test division across machines based on execution times.                         |
|                          | - `NodesByIterations`: Run iterations of a test across machines (single target).                       |
| `NUM_MACHINES`           | Optional. Number of machines to parallelize across.                                                   |
| `GENERATE_JOBS`          | Optional. Boolean to force generating child jobs.                                                     |
| `PERSONAL_BUILD`         | Optional. Boolean indicating this is a personal build.                                               |
| `UPSTREAM_TEST_JOB_NAME` | Auto-populated when child jobs are generated.                                                         |
| `UPSTREAM_TEST_JOB_NUMBER`| Auto-populated when child jobs are generated.                                                         |


### Post Run Parameters
Parameters to determine what to do with post-run artifacts.

| Parameter                  | Description                                                                                                                             |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `ARCHIVE_TEST_RESULTS`     | Optional. If checked, the test outputs will be archived regardless of the test result.                                                |
| `KEEP_REPORTDIR`           | Optional, useful for showing NUnit output details with the JUnit Jenkins plugin.                                                      |
| `ARTIFACTORY_SERVER`       | Optional, send artifacts to an Artifactory server if it's configured.                                                                 |
| `ARTIFACTORY_REPO`         | Optional, use in conjunction with ARTIFACTORY_SERVER.                                                                                  |
| `ARTIFACTORY_ROOT_DIR`     | Optional, use in conjunction with ARTIFACTORY_SERVER.                                                                                  |
| `CLOUD_PROVIDER`           | Optional, if set, Jenkins jobs may try to spin up dynamic agents to run tests on if all real nodes are in use.                        |
| `USE_TESTENV_PROPERTIES`   | Optional, boolean, use the values provided in the testenv.properties file to pin to particular versions of test material.            |
| `RERUN_ITERATIONS`         | Optional, if set, indicates that when test targets fail, they are to be rerun this many times.                                         |
| `RELATED_NODES`            | Setting the client machine label for use in client/server testing.                                                                     |

## Grinder Etiquette
- When possible, avoid running top-level targets (all of sanity.openjdk tests, etc., since we have nightly runs for those), or runs with 100x+ ITERATIONS.
These can take hours and will block the regular testing, especially on platforms where we have few machines.
- Do not mark Grinder runs as “keep forever,” there is a limited amount of space on Jenkins master.
Download any artifacts you need from your run as soon as it finishes to attach to issues, etc., and consider the Jenkins job transient.
- When reporting failures in issues, understand that Grinder jobs are transient (not kept for long on Jenkins).
Sharing a link to the Grinder is only useful for a short time.
Share the "Rerun in Grinder" link which has details of the impl/version/test target, output statistics & test artifacts in the issue; those are more concrete items when reproducing failures.
For openjdk test failures, here are some useful triage [instructions](https://github.com/adoptium/aqa-tests/wiki/Guidance-for-Creating-OpenJDK-Test-Defects).
- Proactively delete Grinder runs that you do not need (setup fails or mistake in target, etc.).
We keep the last 50 runs, and if you delete your unneeded runs immediately, other jobs will stay a little bit longer, giving others more time to grab artifacts.

**Note**: The above documentation provides a comprehensive reference for configuring Jenkins Test Jobs for AQAvit Grinder testing at ci.adoptium.net and OpenJ9 Jenkins server.
Make sure to follow the guidelines and etiquette to ensure efficient and effective testing processes.
