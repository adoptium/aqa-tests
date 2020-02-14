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

source $(dirname "$0")/common_functions.sh
source $(dirname "$0")/dockerfile_functions.sh

if [ $# -ne 6 ]; then
	echo
	echo "usage: $0 test version vm os package build"
	echo "test    = ${supported_tests}"
	echo "version = ${supported_versions}"
	echo "vm      = ${supported_jvms}"
	echo "os      = ${supported_os}"
	echo "package = ${supported_packages}"
	echo "build   = ${supported_builds}"
	exit -1
fi

set_test $1
set_version $2
set_vm $3
set_os $4
set_package $5
set_build $6


# Build the Docker image with the given repo, build, build type and tags.
function build_image() {
    local file=$1
    local test=$2
    local version=$3
    local vm=$4
    local os=$5
    local package=$6
    local build=$7

    # Used for tagging the image
    tags="adoptopenjdk-${test}-test:${version}-${package}-${os}-${vm}-${build}"

	echo "#####################################################"
	echo "INFO: docker build --no-cache -t ${tags} -f ${file} ."
	echo "#####################################################"
	docker build --no-cache -t ${tags} -f ${file} .
	if [ $? != 0 ]; then
		echo "ERROR: Docker build of image: ${tags} from ${file} failed."
		exit 1
	fi
}

# Handle making the directory for organizing the Dockerfiles
dir="$(dirname "$0")/${test}/dockerfile/${version}/${package}/${os}"
mkdir -p ${dir}

# File Path to Dockerfile
file="${dir}/Dockerfile.${vm}.${build}"

# Generate Dockerfile
generate_dockerfile ${file} ${test} ${version} ${vm} ${os} ${package} ${build}

# Check if Dockerfile exists
if [ ! -f ${file} ]; then
    echo "ERROR: Dockerfile generation for ${file} failed."
	exit 1
fi

# Build Dockerfile that was generated
build_image ${file} ${test} ${version} ${vm} ${os} ${package} ${build}