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

if [ $# -ne 7 ]; then
	echo
	echo "usage: $0 test version vm os package build check_external_custom [testtarget]"
	echo "test    = ${supported_tests}"
	echo "version = ${supported_versions}"
	echo "vm      = ${supported_jvms}"
	echo "os      = ${supported_os}"
	echo "package = ${supported_packages}"
	echo "build   = ${supported_builds}"
	echo "testtarget" = "Optional: CMD to pass to ENTRYPOINT script from Dockerfile"
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

	echo "The test in the build_image() function is ${test}"
    # Used for tagging the image
    tags="adoptopenjdk-${test}-test:${version}-${package}-${os}-${vm}-${build}"

	echo "#####################################################"
	echo "INFO: docker build --no-cache -t ${tags} -f ${file} $(realpath $(dirname "$0"))/"
	echo "#####################################################"
	docker build --no-cache -t ${tags} -f ${file} $(realpath $(dirname "$0"))/
	if [ $? != 0 ]; then
		echo "ERROR: Docker build of image: ${tags} from ${file} failed."
		exit 1
	fi
}

# Handle making the directory for organizing the Dockerfiles
echo "The test name in the build_image is ${test}"
dir="$(realpath $(dirname "$0"))/${test}/dockerfile/${version}/${package}/${os}"
mkdir -p ${dir}
path_now=$(pwd)
echo "The directory is ${dir}"
echo "The check_external_custom is ${check_external_custom}"
# File Path to Dockerfile
echo "Directory for ${dir} in build_image.sh"
file="${dir}/Dockerfile.${vm}.${build}"

# Generate Dockerfile
generate_dockerfile ${file} ${test} ${version} ${vm} ${os} ${package} ${build} ${testtarget} ${check_external_custom}

# Check if Dockerfile exists
if [ ! -f ${file} ]; then
    echo "ERROR: Dockerfile generation for ${file} failed."
	exit 1
fi

# Build Dockerfile that was generated
build_image ${file} ${test} ${version} ${vm} ${os} ${package} ${build}
