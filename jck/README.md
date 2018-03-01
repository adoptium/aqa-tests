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

5. If you want to compile jck test only, export `BUILD_LIST=jck`. The other steps will stay the same as instructed in `openjdk-tests/README.md`.


This test directory contains:
  * build.xml file - that clones AdoptOpenJDK/stf repo to pick up a test framework
  * playlist.xml - to allow easy inclusion of JCK tests into automated builds
  * jck.mk - define extra settings for JCK tests.


# How-to Run customized JCK test targets

There is one custom JCK test targets `jck-runtime-custom`. This test target is used as an example to run custom JCK test target in JCK runtime suite.

1. Follow the Steps 1 - 4 mentioned above. 

2. Export `JCK_TEST_TARGET=<jck_test_subset>` as an environment variable or pass it in when run as a make command. For example `export JCK_TEST_TARGET=api/java_math`

3. Make sure the JCK test subset is available in JCK test material folder, a.k.a. `$(JCK_ROOT)/$(JCK_VERSION)/`.

4. If you need to add extra Java options to JCK tests, you could export `EXTRA_OPTIONS="<java_options>"`. Then extra added Java options would be added to JCK test during execution.

5. Follow the steps remaining in `openjdk-tests/README.md`