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

# Supported versions
supported_versions="8 11 17"

# Supported JVMs
supported_jvms="hotspot openj9"

# Supported distros
# Distros Not Included: windowsservercore-ltsc2016
supported_os="ubuntu ubi"

# Supported packges
supported_packages="jdk jre"

# Supported builds
supported_builds="full"

# Supported tests
supported_tests="external_custom camel criu-portable-checkpoint criu-portable-restore criu-ubi-portable-checkpoint criu-ubi-portable-restore derby elasticsearch jacoco jenkins functional-test kafka lucene-solr openliberty-mp-tck payara-mp-tck quarkus quarkus_quickstarts scala system-test tomcat tomee wildfly wycheproof netty spring zookeeper"

function check_version() {
    version=$1

    for current_version in ${supported_versions}
    do
        if [[ "${version}" == "${current_version}" ]]; then
            return 1
        fi
    done

    return 0
}

# Set a valid version
function set_version() {
    version=$1

    if (check_version ${version}); then
        echo "ERROR: Invalid Version: ${version}"
        echo "Usage: $0 [${supported_versions}]"
        exit 1
    fi
}

function check_os() {
    os=$1

    for current_os in ${supported_os}
    do
        if [[ "${os}" == "${current_os}" ]]; then
            return 1
        fi
    done

    return 0
}

# Set a valid os
function set_os() {
    os=$1
    if (check_os ${os}); then
        echo "ERROR: Invalid OS: ${os}"
        echo "Usage: $0 [${supported_os}]"
        exit 1
    fi
}

function check_test() {
    test=$1

    for current_test in ${supported_tests}
    do
        if [[ "${test}" == "${current_test}" ]]; then
            return 1
        fi
    done

    return 0
}

# Set a valid test
function set_test() {
    test=$1
    if (check_test ${test}); then
        echo "ERROR: Invalid Test: ${test}"
        echo "Usage: $0 [${supported_tests}]"
        exit 1
    fi
}

function check_vm() {
    vm=$1

    for current_vm in ${supported_jvms}
    do
        if [[ "${vm}" == "${current_vm}" ]]; then
            return 1
        fi
    done

    return 0
}

# Set a valid vm
function set_vm() {
    vm=$1
    if (check_vm ${vm}); then
        echo "ERROR: Invalid VM: ${vm}"
        echo "Usage: $0 [${supported_jvms}]"
        exit 1
    fi
}

function check_package() {
    package=$1

    for current_package in ${supported_packages}
    do
        if [[ "${package}" == "${current_package}" ]]; then
            return 1
        fi
    done

    return 0
}

# Set a valid package
function set_package() {
    package=$1
    if (check_package ${package}); then
        echo "ERROR: Invalid Package: ${package}"
        echo "Usage: $0 [${supported_packages}]"
        exit 1
    fi
}

function check_build() {
    build=$1

    for current_build in ${supported_builds}
    do
        if [[ "${build}" == "${current_build}" ]]; then
            return 1
        fi
    done

    return 0
}

# Set a valid build
function set_build() {
    build=$1
    if (check_build ${build}); then
        echo "ERROR: Invalid Build: ${build}"
        echo "Usage: $0 [${supported_builds}]"
        exit 1
    fi
}

# Set a platform
function set_platform() {
    # TO-DO: Add supported_platforms when portable tests support more platforms
    platform=$1
}

# Reading properties of test.properties file
function getProperty() {
    PROP_KEY=$1
    PROP_VALUE=`cat $PROPERTY_FILE | grep "$PROP_KEY" | cut -d'=' -f 2-`
    echo  `sed -e 's/^"//' -e 's/"$//' <<<"$PROP_VALUE"`
}

# Used for external_custom tests
function set_external_custom_test_info(){
    test=$1
    check_external_custom_test=$2
    github_url="${EXTERNAL_CUSTOM_REPO}"
    test_command="${EXTERNAL_TEST_CMD}"
    test_results="testResults"
    tag_version="${EXTERNAL_REPO_BRANCH}"
    environment_variable="MODE=java"
    ubuntu_packages="git"
    maven_version="3.8.5"
}

# Set the valid OSes for the current architectures.
function set_test_info() {
    test=$1
    check_external_custom_test=$2
    cd ../
    path_to_file=$(pwd)
    echo ${path_to_file}    
    PROPERTY_FILE=${path_to_file}/${test}/test.properties
    github_url=$(getProperty "github_url")
    test_options=$(getProperty "test_options")
    test_results=$(getProperty "test_results")
    ant_version=$(getProperty "ant_version")
    ant_contrib_version=$(getProperty "ant_contrib_version")
    ivy_version=$(getProperty "ivy_version")
    jdk_install=$(getProperty "jdk_install")
    tag_version=$(getProperty "tag_version")
    gradle_version=$(getProperty "gradle_version")
    sbt_version=$(getProperty "sbt_version")
    bazel_version=$(getProperty "bazel_version")
    openssl_version=$(getProperty "openssl_version")
    python_version=$(getProperty "python_version")
    criu_version=$(getProperty "criu_version")
    maven_version=$(getProperty "maven_version")
    environment_variable=$(getProperty "environment_variable")
    localPropertyFile=$(getProperty "localPropertyFile")
    ubuntu_packages=$(getProperty "ubuntu_packages")
    ubi_packages=$(getProperty "ubi_packages")
}

function cleanup_images() {
    # Delete any old containers that have exited.
    docker rm $(docker ps -a | grep "Exited" | awk '{ print $1 }') 2>/dev/null

    # Delete any old images for our target_repo on localhost.
    docker rmi -f $(docker images | grep -e "adoptopenjdk" | awk '{ print $3 }' | sort | uniq) 2>/dev/null
}
