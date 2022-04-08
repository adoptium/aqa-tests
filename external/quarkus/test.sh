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
# Set up Java to be used by the quarkus-test
echo_setup

# See https://github.com/quarkusio/quarkus/blob/master/CONTRIBUTING.md#frequently-asked-questions
# for advise to set MAVEN_OPTS to avoid https://cwiki.apache.org/confluence/display/MAVEN/OutOfMemoryError
export MAVEN_OPTS="-Xmx1g"

export OPENJ9_JAVA_OPTIONS="-Xmx1g"

excludeProject="-pl !:quarkus-documentation,\
!:quarkus-openshift-deployment"

#JDK 17: Failed projects 
#!:quarkus-kubernetes-client,\
#!:quarkus-liquibase-deployment,\
#!:quarkus-vertx-graphql,\
#!:quarkus-smallrye-graphql,\
#!:quarkus-logging-json,\
#!:quarkus-logging-gelf,\
#!:quarkus-picocli,\
#!:quarkus-google-cloud-functions-http-deployment,\
#!:quarkus-google-cloud-functions,\
#!:quarkus-awt,\
#!:quarkus-project-core-extension-codestarts,\
#!:quarkus-opentelemetry-deployment,\
#!:quarkus-container-image-jib-deployment,\
#!:quarkus-vertx-graphql-deployment,
#!:quarkus-integration-test-shared-library"
#!:quarkus-awt-deployment,\
#!:quarkus-smallrye-graphql-deployment,\
#!:quarkus-smallrye-graphql-client,\
#!:quarkus-logging-gelf-deployment,\
#!:quarkus-picocli-deployment,\
#!:quarkus-google-cloud-functions-http,\
#!:quarkus-google-cloud-functions-deployment,\
#!:quarkus-integration-test-class-transformer,\
#!:quarkus-maven-plugin,\


echo "Compile and run quarkus tests"

if [ "$JDK_VERSION" == "11" ]; then
	excludeProject="-pl !:quarkus-documentation,\
!:quarkus-reactive-routes-deployment,\
!:quarkus-cli,\
!:quarkus-integration-test-no-awt,\
!:quarkus-avro-reload-test"
fi
./mvnw --batch-mode --fail-at-end $excludeProject clean install
test_exit_code=$?

find ./ -type d -name 'surefire-reports' -exec cp -r "{}" /testResults \;
exit $test_exit_code
