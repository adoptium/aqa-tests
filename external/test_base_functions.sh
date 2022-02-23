#!/usr/bin/env bash
#
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

echo_setup() {
	echo_env_var
	echo_java_version
}

echo_env_var() {
	echo APPLICATION_NAME=$APPLICATION_NAME
	echo APPLICATION_TAG=$APPLICATION_TAG
	echo OS_TAG=$OS_TAG
}

echo_java_version() {
	if [ -d /java/jre/bin ];then
		echo "Using mounted Java8"
		export JAVA_BIN=/java/jre/bin
		export JAVA_HOME=/java
		export PATH=$JAVA_BIN:$PATH
	elif [ -d /java/bin ]; then
		echo "Using mounted Java"
		export JAVA_BIN=/java/bin
		export JAVA_HOME=/java
		export PATH=$JAVA_BIN:$PATH
	else
		echo "Using default openjdk image Java"
		java_path=$(type -p java)
		suffix="/java"
		java_root=${java_path%$suffix}
		export JAVA_BIN="$java_root"
		export JAVA_HOME="${java_root%/bin}"
	fi
	echo "JAVA_BIN is: $JAVA_BIN"
	echo "=JAVA VERSION OUTPUT BEGIN="
	java -version
	echo "=JAVA VERSION OUTPUT END="
}