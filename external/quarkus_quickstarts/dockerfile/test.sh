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

source $(dirname "$0")/test_base_functions.sh

# Set up Java to be used by the the quarkus quickstarts test

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
	echo "Using docker image default Java"
	java_path=$(type -p java)
	suffix="/java"
	java_root=${java_path%$suffix}
	export JAVA_BIN="$java_root"
	echo "JAVA_BIN is: $JAVA_BIN"
	export JAVA_HOME="${java_root%/bin}"
fi

echo_setup

export MAVEN_OPTS="-Xmx1g"

cd /quarkus-quickstarts
pwd
echo "Compile and run quarkus_quickstarts tests"
mvn -pl !:hibernate-orm-quickstart,!:hibernate-orm-panache-quickstart,\
!:hibernate-search-elasticsearch-quickstart,!:mqtt-quickstart,\
!:quartz-quickstart,!:security-jdbc-quickstart,!:security-keycloak-authorization-quickstart,\
!:security-openid-connect-web-authentication-quickstart,\
!:security-openid-connect-multi-tenancy-quickstart,!:spring-data-jpa-quickstart,\
!:vertx-quickstart,!:context-propagation-quickstart,!:getting-started-reactive-rest,\
!:kafka-quickstart,!:neo4j-quickstart,!:rest-client-quickstart,!:rest-client-multipart-quickstart clean install
test_exit_code=$?
echo "Build quarkus_quickstarts completed"

find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
echo "Test results copied"

exit $test_exit_code
