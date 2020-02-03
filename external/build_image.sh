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
set -o pipefail

source ./common_functions.sh
source ./dockerfile_functions.sh

if [ $# -ne 6 ]; then
	echo
	echo "usage: $0 os test version vm package build"
	echo "os      = ${all_os}"
	echo "test    = ${all_tests}"
	echo "version = ${all_versions}"
	echo "vm      = ${all_jvms}"
	echo "package = ${all_packages}"
	echo "build   = ${all_builds}"
	exit -1
fi

set_os $1
set_test $2
set_version $3
set_vm $4
set_package $5
set_build $6

# Build the Docker image with the given repo, build, build type and tags.
#function build_image() {
#	repo=$1; shift;
#	build=$1; shift;
#	btype=$1; shift;
#
#	tags=""
#
#	dockerfile="Dockerfile.${vm}.${build}.${btype}"
#
#	echo "#####################################################"
#	echo "INFO: docker build --no-cache ${tags} -f ${dockerfile} ."
#	echo "#####################################################"
#	docker build --pull --no-cache ${tags} -f ${dockerfile} .
#	if [ $? != 0 ]; then
#		echo "ERROR: Docker build of image: ${tags} from ${dockerfile} failed."
#		exit 1
#	fi
#}


# Generate all the Dockerfiles for each of the builds and build types
dir="${test}/dockerfile"
file="${dir}/Dockerfile.${os}"
generate_dockerfile ${file} ${os} ${test} ${version} ${vm} ${package} ${build}
if [ ! -f ${file} ]; then
    continue;
fi

