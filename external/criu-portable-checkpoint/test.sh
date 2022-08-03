#/bin/bash
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
#

source $(dirname "$0")/test_base_functions.sh
# Criu needs to move mounted jdk inside docker, since restore requires generated files in jdk
MOUNTED_JDK_DIR="/opt/java/openjdk"
New_JDK_DIR="/internaljdk/java/openjdk"
if [ -d "$MOUNTED_JDK_DIR" ]; then
  mkdir -p $New_JDK_DIR
  cp -r $MOUNTED_JDK_DIR/* $New_JDK_DIR/
  export TEST_JDK_HOME=$New_JDK_DIR
  echo "TEST_JDK_HOME is : $TEST_JDK_HOME"
else
  echo "No JDK found!"
  exit 1
fi

echo_setup
export JDK_VERSION=
echo "JDK_VERSION has been unset, use auto-detect instead."
export DYNAMIC_COMPILE=true
export BUILD_LIST=functional

cd /aqa-tests
./get.sh
cd /aqa-tests/TKG

set -e
echo "Generating criu checkpoint"
make $1
set +e
