#/bin/bash
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
#

source $(dirname "$0")/test_base_functions.sh
#Set up Java to be used by the tomcat test
echo_setup

git clone -q -b 1.6.x --single-branch https://github.com/apache/apr.git $TEST_HOME/tmp/apr
cd $TEST_HOME/tmp/apr
./buildconf
./configure --prefix=$TEST_HOME/tmp/apr-build
make
make install
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$TEST_HOME/tmp/apr-build/lib"

git clone -q https://github.com/apache/tomcat-native.git $TEST_HOME/tmp/tomcat-native
cd $TEST_HOME/tmp/tomcat-native/native
sh buildconf --with-apr=$TEST_HOME/tmp/apr
./configure --with-apr=$TEST_HOME/tmp/apr --with-java-home=$JAVA_HOME --with-ssl=yes --prefix=$TEST_HOME/tmp/tomcat-native-build
make
make install
cd $TEST_HOME
yes | cp build.properties.default build.properties
echo >> build.properties
echo "test.threads=4" >> build.properties
echo "test.relaxTiming=true" >> build.properties
echo "test.excludePerformance=true" >> build.properties
echo "test.openssl.path=/dev/null/openssl" >> build.properties
echo "test.apr.loc=$TEST_HOME/tmp/tomcat-native-build/lib" >> build.properties

set -e
echo "Building tomcat" && \
ant && \

echo "Running tomcat tests" 
#Run tests
ant test
set +e
