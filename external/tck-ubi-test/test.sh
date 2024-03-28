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
unset JDK_VERSION
echo "TEST_JDK_HOME has been reset to $TEST_JDK_HOME, use TKG auto-detect to detect JDK_VERSION and JDK_IMPL."
export AUTO_DETECT=true
export DYNAMIC_COMPILE=false
export BUILD_LIST=jck

export STAGE_JCK_MATERIAL_FROM_GIT=false
mkdir /jck_unzipped
cp -r /opt/jck_root/unzipped/* /jck_unzipped/
export JCK_ROOT=/jck_unzipped
export JCK_ROOT_USED=${JCK_ROOT}
echo "JCK_ROOT in docker is ${JCK_ROOT}"
echo "JCK_ROOT_USED in docker is ${JCK_ROOT_USED}"
export DISPLAY = "unix:0"
echo "DISPLAY in docker is ${DISPLAY}"

set -e
cd /aqa-tests
./get.sh
rm -rf /aqa-tests/jck
cp -R /opt/test_root/jck /aqa-tests/

cd /aqa-tests/TKG
echo "Generating make files and running the target tests"
make compile
make $1
set +e
