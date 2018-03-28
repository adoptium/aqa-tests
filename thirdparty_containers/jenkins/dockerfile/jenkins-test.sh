#/bin/bash
#
# (C) Copyright IBM Corporation 2017.
#
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

if [ -d /java/jre/bin ];then
	echo "Using mounted Java8"
	export JAVA_BIN=/java/jre/bin
	export JAVA_HOME=/java
	export PATH=$JAVA_BIN:$PATH
	java -version
elif [ -d /java/bin ]; then
	echo "Using mounted Java9"
	export JAVA_BIN=/java/bin
	export JAVA_HOME=/java
	export PATH=$JAVA_BIN:$PATH
	java -version
else
	echo "Using docker image default Java"
	java_path=$(type -p java)
	suffix="/java"
	java_root=${java_path%$suffix}
	export JAVA_BIN="$java_root"
	echo "JAVA_BIN is: $JAVA_BIN"
	$JAVA_BIN/java -version
fi

TEST_SUITE=$1
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"
#begin jenkins test
echo "PATH is : $PATH"
echo "JAVA_HOME is : $JAVA_HOME"
echo "type -p java is :"
type -p java
echo "java -version is: \n"
java -version
cd /jenkins
ls .
pwd
echo "Build jenkins by using mvn \"mvn clean install -pl war -am -DskipTests\"" && \
mvn clean install -pl war -am -DskipTests -Denforcer.fail=false

echo "Building jenkins completed"

echo "Run jenkins test phase alone with cmd: \"mvn surefire:test\"" && \
mvn surefire:test -Denforcer.fail=false

echo "Executing jenkins tests alone completed"
