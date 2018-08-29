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

export JAVA_VERSION=SE80
export JAVA_BIN=/root/j2sdk-image/jre/bin
export SPEC=linux_x86-64_cmprssptrs
export JAVA_HOME=/root/j2sdk-image

echo "Starting a JITaaS Server..."

/root/j2sdk-image/jre/bin/java -XX:JITaaSServer &

echo "Wait for a few seconds for server to boot up"

sleep 5

echo "Running tests..."

echo "PATH is : $PATH"
echo "JAVA_HOME is : $JAVA_HOME"
echo "type -p java is :"
type -p java
echo "java -version is: \n"
java -version

cd /root/test/TestConfig

make _sanity EXTRA_OPTIONS=" -XX:JITaaSClient -Xjit:verbose={jitaas} "
