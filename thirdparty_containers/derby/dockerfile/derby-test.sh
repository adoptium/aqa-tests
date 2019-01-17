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

#Set up Java to be used by the the derby-test

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
	export JAVA_HOME="${java_root%/bin}"
fi

TEST_SUITE=$1

echo "PATH is : $PATH"
echo "JAVA_HOME is : $JAVA_HOME"
echo "type -p java is :"
type -p java
echo "java -version is: \n"
java -version

cd ${DERBY_HOME}
ls .
pwd

#clean previous build artifacts 
ant -f ${DERBY_HOME}/build.xml clobber

#build all
ant -f ${DERBY_HOME}/build.xml all

#create jars
ant -f ${DERBY_HOME}/build.xml buildjars

export jardir ${DERBY_HOME}/jars/sane 
export tstjardir ${DERBY_HOME}/tools/java 
export CLASSPATH "${jardir}/derbyrun.jar:${jardir}/derbyTesting.jar:${tstjardir}/junit.jar"

java -jar ${DERBY_HOME}/jars/sane/derbyrun.jar sysinfo

#Run all tests
ant -Dderby.tests.basePort=1690 -Dderby.system.durability=test -DderbyTesting.oldReleasePath=${DERBY_HOME}/jars junit-all

#Run only derbylang suite
#java -Dverbose=true -cp ${jardir}/derbyrun.jar:${jardir}/derbyTesting.jar:$tstjardir/junit.jar org.apache.derbyTesting.functionTests.harness.RunSuite #derbylang

