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
# Set up Java to be used by the lucene test
echo_setup

set -e

${ANT_HOME}/bin/ant -Divy_install_path=${ANT_HOME}/lib -lib ${ANT_HOME}/lib ivy-bootstrap
echo "Compile lucene-solr test"
${ANT_HOME}/bin/ant -Divy_install_path=${ANT_HOME}/lib -lib ${ANT_HOME}/lib -f ${TEST_HOME}/build.xml -Duser.home=${TEST_HOME} -Dcommon.dir=${TEST_HOME}/lucene compile
echo "Execute lucene-solr test"
${ANT_HOME}/bin/ant -Divy_install_path=${ANT_HOME}/lib -lib ${ANT_HOME}/lib -f ${TEST_HOME}/build.xml -Duser.home=${TEST_HOME} -Dcommon.dir=${TEST_HOME}/lucene test

set +e