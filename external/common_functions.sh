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

# All supported JVMs
all_jvms="hotspot openj9"

# All supported packges
all_packages="jdk jre"

# All supported distros
all_os="alpine debian debianslim ubi ubi-minimal centos clefos ubuntu windowsservercore-ltsc2016"

# All support versions
all_versions="8 9 10 11 12 13"

# All supported builds
all_builds="slim full"

# All supported tests
all_tests="derby elasticsearch jenkins kafka lucene-solr openliberty payara quarkus scala thorntail tomcat tomee wildfly wycheproof"

function check_version() {
	version=$1
	case ${version} in
	8|9|10|11|12|13)
		;;
	*)
		echo "ERROR: Invalid version"
		;;
	esac
}

# Set a valid version
function set_version() {
	version=$1
	if [ ! -z "$(check_version ${version})" ]; then
		echo "ERROR: Invalid Version: ${version}"
		echo "Usage: $0 [${all_versions}]"
		exit 1
	fi
}

function check_os() {
	os=$1
	case ${os} in
	alpine|debian|debianslim|ubi|ubi-minimal|centos|clefos|ubuntu|windowsservercore-ltsc2016)
		;;
	*)
		echo "ERROR: Invalid os"
		;;
	esac
}

# Set a valid os
function set_os() {
	os=$1
	if [ ! -z "$(check_os ${os})" ]; then
		echo "ERROR: Invalid OS: ${os}"
		echo "Usage: $0 [${all_os}]"
		exit 1
	fi
}

function check_test() {
	test=$1
	case ${test} in
	derby|elasticsearch|jenkins|kafka|lucene-solr|openliberty|payara|quarkus|scala|thorntail|tomcat|tomee|wildfly|wycheproof)
		;;
	*)
		echo "ERROR: Invalid test"
		;;
	esac
}

# Set a valid test
function set_test() {
	test=$1
	if [ ! -z "$(check_test ${test})" ]; then
		echo "ERROR: Invalid Test: ${test}"
		echo "Usage: $0 [${all_tests}]"
		exit 1
	fi
}


function check_vm() {
	vm=$1
	case ${vm} in
	hotspot|openj9)
		;;
	*)
		echo "ERROR: Invalid VM"
		;;
	esac
}

# Set a valid vm
function set_vm() {
	vm=$1
	if [ ! -z "$(check_vm ${vm})" ]; then
		echo "ERROR: Invalid VM: ${vm}"
		echo "Usage: $0 [${all_jvms}]"
		exit 1
	fi
}

function check_package() {
	package=$1
	case ${package} in
	jdk|jre)
		;;
	*)
		echo "ERROR: Invalid Package"
		;;
	esac
}

# Set a valid package
function set_package() {
	package=$1
	if [ ! -z "$(check_package ${package})" ]; then
		echo "ERROR: Invalid Package: ${package}"
		echo "Usage: $0 [${all_packages}]"
		exit 1
	fi
}

function check_build() {
	build=$1
	case ${build} in
	slim|full)
		;;
	*)
		echo "ERROR: Invalid Build"
		;;
	esac
}

# Set a valid build
function set_build() {
	build=$1
	if [ ! -z "$(check_build ${build})" ]; then
		echo "ERROR: Invalid Build: ${build}"
		echo "Usage: $0 [${all_builds}]"
		exit 1
	fi
}

# Set the valid OSes for the current architecure.
function set_test_info() {
	test=$1
	case ${test} in
	derby)
	    github_url="https://github.com/apache/derby.git"
	    script="derby-test.sh"
	    home_path="\${WORKDIR}/derby"
	    tag_version="trunk"
	    ant_version="1.10.6"
		debian_packages="git wget tar"
		debian_slim_packages="${debian_packages}"
		ubuntu_packages="${debian_packages}"
		alpine_packages="git wget tar"
		centos_packages="git wget tar"
		clefos_packages="${centos_packages}"
		ubi_packages="git wget tar"
		ubi_minimal_packages="${ubi_packages}"
		;;
	elasticsearch)
		;;
	jenkins)
		;;
	kafka)
		;;
	lucene-solr)
		;;
    openliberty)
		;;
	payara)
		;;
	quarkus)
		;;
	scala)
	    github_url="https://github.com/scala/scala.git"
	    script="scala-test.sh"
	    home_path=""
	    tag_version="v2.13.1"
	    sbt_version="1.3.4"
		debian_packages="git wget tar curl gpg gpg-agent"
		debian_slim_packages="${debian_packages}"
		ubuntu_packages="${debian_packages}"
		alpine_packages="git wget tar curl gnupg"
		centos_packages="git wget tar curl gpg"
		clefos_packages="${centos_packages}"
		ubi_packages="git wget tar curl gpg"
		ubi_minimal_packages="${ubi_packages}"
		;;
	thorntail)
		;;
	tomcat)
		;;
	tomee)
		;;
	wildfly)
		;;
	wycheproof)
		;;
	*)
		echo "ERROR: Unsupported test:${test}, Exiting"
		exit 1
		;;
	esac
}