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

# Cleanup any old containers and images
echo "==============================================================================="
echo "                           Cleaning up images                                  "
echo "==============================================================================="
cleanup_images

# Loop through all possible images
for test in ${supported_tests}
do
    for version in ${supported_versions}
    do
        for vm in ${supported_jvms}
        do
            for os in ${supported_os}
            do
                for package in ${supported_packages}
                do
                    for build in ${supported_builds}
                    do
                        echo "==============================================================================="
                        echo "                                                                               "
                        echo " Building Docker Images for ${test} ${version} ${vm} ${os} ${package} ${build} "
                        echo "                                                                               "
                        echo "==============================================================================="
                        $(dirname "$0")/build_image.sh ${test} ${version} ${vm} ${os} ${package} ${build}
                        echo
                        echo
                    done
                done
            done
        done
    done
done