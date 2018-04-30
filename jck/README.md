<!--
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
-->

# How-to Run JCK Tests

* Prerequisites:
  * OpenJDK Community TCK License Agreement (OCTLA)
  * your own set of JCK test materials (JCK test source under OCTLA License): jck8b or jck9
  * ant 1.10.1 or above with ant-contrib.jar


1. Put unarchived jck test materials (jck8b or jck9) into an empty folder, for example:
* `/jck/jck8b/` and `/jck/jck9`

2. Export `JCK_ROOT=/jck` as an environment variable or pass it in when run as a make command

3. Export `JCK_VERSION=<your_jck_version>` as an environment variable or pass it in when run as a make command. For example `export JCK_VERSION=jck8b` 

4. Export `JAVA_HOME=<your_JDK_root>` as an environment variable

5. If you want to compile jck test only, export `BUILD_LIST=jck`. The other steps will stay the same as instructed in [openjdk-tests/README.md](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/README.md).


This test directory contains:
  * `build.xml` file - that clones AdoptOpenJDK/stf repo to pick up a test framework
  * `<test_subset>/playlist.xml` - to allow easy inclusion of JCK tests into automated builds
  * `jck.mk` - define extra settings for running JCK tests.


# How-to Run customized JCK test targets

There is one custom JCK test targets `jck-runtime-custom`. This test target is used as an example to run custom JCK test target in JCK runtime suite.

1. Follow the Steps 1 - 4 mentioned above. 

2. Export `JCK_TEST_TARGET=<jck_test_subset>` as an environment variable or pass it in when run as a make command. For example `export JCK_TEST_TARGET=api/java_math`

3. Make sure the JCK test subset is available in JCK test material folder, a.k.a. `$(JCK_ROOT)/$(JCK_VERSION)/`.

4. If you need to add extra Java options to JCK tests, you could export `EXTRA_OPTIONS="<java_options>"`. Then extra added Java options would be added to JCK test during execution.

5. Follow the steps remaining in [openjdk-tests/README.md](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/README.md)


# How-to Run JCK test targets within Docker container

With the help of this JCK dockerfile, users can execute JCK tests locally
without having environment setup problems. To utilize the advantage of 
docker-based JCK test, users can follow the step below to run JCK tests 
locally within Docker container.

1. Run the [openjdk-tests/get.sh](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/get.sh) to prepare the test framework and dependencies.

2. Follow the readme in [openjdk-tests/buildenv/docker/README.md](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/buildenv/docker/README.md) to build Docker image.

3. Prepare the JCK materials locally and mount it to the Docker container when you
   initialize the JCK docker image.

4. Once you are in the Docker container, follow [How-to Run customized JCK test targets](#how-to-run-customized-jck-test-targets) or [How-to Run JCK Tests](#how-to-run-jck-tests) to run JCK tests locally.

A quick start to run JCK test in docker image

```
// clone this openjdk-tests repo
git clone https://github.com/AdoptOpenJDK/openjdk-tests.git

// build docker image and run it
// the JCK_ROOT structure should be like
//root:jck_root root$ tree -L 2 ./
//./
//├── jck10
//├── jck8b
//└── jck9

cd openjdk-tests/buildenv/docker
docker build -t openjdk-test .
docker run -it -v <path_to_openjdk-tests_root>:/test  -v <jck_material_root>:/jck_root openjdk-test /bin/bash

// within docker container
cd /test
bash get.sh --testdir /test --customizedURL https://api.adoptopenjdk.net/openjdk8-openj9/nightly/x64_linux/latest/binary --sdkdir /java 
export JAVA_VERSION=SE80
export JAVA_BIN=/java/openjdkbinary/j2sdk-image/jre/bin/
export SPEC=linux_x86-64_cmprssptrs
export BUILD_LIST=jck
export JCK_ROOT=/jck_root
export JCK_VERSION=jck8b

cd TestConfig
make -f run_configure.mk
make compile
make _sanity.jck
```