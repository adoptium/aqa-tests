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

# How-to Run JCK Tests

* Prerequisites:
  * OpenJDK Community TCK License Agreement (OCTLA)
  * your own set of JCK test materials (JCK test source under OCTLA License): jck8d or jck9
  * ant 1.10.1 or above with ant-contrib.jar

1. Create an empty folder where your JCK test materials will be stored. For example `makedir /jck`.

2. Export `JCK_GIT_REPO=<test_material_repo | test_material_folder>` as an environment variable or pass it in when run as a make command.
* If your test material is stored in a git repository it will be cloned to the empty folder created in step 1. For example `export JCK_GIT_REPO=git@github.com:<org>/<repo>.git`.
* Otherwise put your unarchived jck test materials into the empty folder created in step 1 and point `JCK_GIT_REPO` to this folder. For example `export JCK_GIT_REPO=/jck/jck8d`.

3. Export `JCK_ROOT=/jck/<test_material_folder>` as an environment variable or pass it in when run as a make command. For example `export JCK_ROOT=/jck/jck8d`.
* Optional. The default value is `<openjdk-test>/../../../jck_root/JCK$(JDK_VERSION)-unzipped`.
* if you export `SKIP_JCK_GIT_UPDATE=true`, then the `JCK_GIT_REPO` is not used at all, and `JCK_ROOT` is used directly, without needing to be a repo.

4. Export `TEST_JDK_HOME=<your_JDK_root>` as an environment variable.

5. If you want to compile jck test only, export `BUILD_LIST=jck`. The other steps will stay the same as instructed in [aqa-tests/README.md](https://github.com/adoptium/aqa-tests/blob/master/README.md).


This test directory contains:
  * `build.xml` file - that clones adoptium/stf repo to pick up a test framework
  * `<test_subset>/playlist.xml` - to allow easy inclusion of JCK tests into automated builds
  * `jck.mk` - define extra settings for running JCK tests.


# How-to Run customized JCK test targets

There are three custom JCK test targets `jckruntime_custom, jckcompiler_custom, jckdevtools_custom`. They are used to run custom JCK test targets in JCK runtime, compiler and devtools testsuites respectively. Take jckruntime_custom as example:

1. Follow the Steps 1 - 4 mentioned above. 

2. Export `JCKRUNTIME_CUSTOM_TARGET=<jck_test_subset>` as an environment variable or pass it in when run as a make command. For example `export JCKRUNTIME_CUSTOM_TARGET=api/java_math`. 

3. Make sure the JCK test subset is available in JCK test material folder, a.k.a. `$(JCK_ROOT)`.

4. If you need to add extra Java options to JCK tests, you could export `EXTRA_OPTIONS="<java_options>"`. Then extra added Java options would be added to JCK test during execution.

5. Follow the steps remaining in [aqa-tests/README.md](https://github.com/adoptium/aqa-tests/blob/master/README.md).


# How-to Run JCK test targets within Docker container

With the help of this JCK dockerfile, users can execute JCK tests locally
without having environment setup problems. To utilize the advantage of 
docker-based JCK test, users can follow the step below to run JCK tests 
locally within Docker container.

1. Run the [aqa-tests/get.sh](https://github.com/adoptium/aqa-tests/blob/master/get.sh) to prepare the test framework and dependencies.

2. Follow the readme in [aqa-tests/buildenv/docker/README.md](https://github.com/adoptium/aqa-tests/blob/master/buildenv/docker/README.md) to build Docker image.

3. Prepare the JCK materials locally and mount it to the Docker container when you
   initialize the JCK docker image.

4. Once you are in the Docker container, follow [How-to Run customized JCK test targets](#how-to-run-customized-jck-test-targets) or [How-to Run JCK Tests](#how-to-run-jck-tests) to run JCK tests locally.

## A quick start to run JCK test in docker image

```
// clone this aqa-tests repo
git clone https://github.com/adoptium/aqa-tests.git

// build docker image and run it
// the JCK_ROOT structure should be like
//root:jck root$ tree -L 2 ./
//./
//├── jck11a
//├── jck10
//├── jck8d
//└── jck9

cd aqa-tests/buildenv/docker
docker build -t openjdk-test .
docker run -it -v <path_to_aqa-tests_root>:/test  -v <jck_material_root>:/jck openjdk-test /bin/bash

// within docker container
cd /test
bash get.sh --testdir /test --customizedURL https://api.adoptopenjdk.net/openjdk8-openj9/nightly/x64_linux/latest/binary --sdkdir /java 
export TEST_JDK_HOME=/java/jdkbinary/j2sdk-image
export BUILD_LIST=jck
export JCK_GIT_REPO=git@github.com:mypretendcompany/jck8tests.git
export JCK_ROOT=/jck/jck8tests

cd TKG
make compile
make _sanity.jck
```

## TCK exclude lists 

We have three types of tck exclude lists: 

1. Dev excludes: This exclude file, ending with `-dev` (e.g. jck8d-dev.jtx), contains vendor specific excludes. All excludes related to automation run by a specific vendor would go into the `*dev.jtx` files in tck repositories maintained by that vendor for use during development not certification.

2. Test-flag specific excludes: These exclude files support development work by allowing developers to add feature specific temporary excludes. For example, the FIPS specific exclude file (e.g., jck8d-fips.jtx) contains list of excludes specific to FIPS that will only be in effect if `TEST_FLAG` is set to `fips`. These files are used for development, not for certification.

3. Standard excludes: These are the 3 standard exclude files (jtx and kfl) that come with tck materials. These constitute known failures and are not updated by vendors.

Note In regular automated runs in Jenkins, we will only see the exclude list of type 1 and 3. In grinder runs where `jck_custom` is used, Dev exclude is ignored. Type 1 and 2 are used during development, not for certification.
