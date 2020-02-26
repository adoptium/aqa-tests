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

# script runs in 3 modes - build / run / clean

set -e
tag=${DOCKERIMAGE_TAG}
docker_os=ubuntu
build_type=full
package=jdk
command_type=build
version=8
impl=hotspot
test=derby

usage () {
	echo 'Usage : external.sh  --dir TESTDIR --tag DOCKERIMAGE_TAG --version JDK_VERSION --impl JDK_IMPL [--build|--run|--clean]'
}

parseCommandLineArgs()
{

	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;

		case "$opt" in
			"--dir" | "-d" )
				test="$1";;
			
			"--version" | "-v" )
				version="$1";;
			
			"--impl" | "-i" )
				impl="$1";;

			"--tag" | "-t" )
				tag="$1";;

			"--build" | "-b" )
				command_type=build; parse_tag;;

			"--run" | "-r" )
				command_type=run; parse_tag;;
			
			"--clean" | "-c" )
				command_type=clean; parse_tag;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done	
}

# Parse environment variable DOCKERIMAGE_TAG
# to set docker_os, build_type, package
function parse_tag () {

	# set PACKAGE
	case $tag in
   		*jre*) 
    		package=jre
		;;
	esac
	# set BUILD_TYPE
	case $tag in
   		*-slim*|*_slim*) 
   			build_type=slim
   		;;
	esac
	# set DOCKER_OS
	case $tag in
   		*alpine*) 
	   		docker_os=alpine
		;;
   		*debianslim*) 
	   		docker_os=debianslim
		;;
		*debian*) 
	   		docker_os=debian
		;;
		*centos*) 
	   		docker_os=centos
		;;
		*clefos*) 
	   		docker_os=clefos
		;;
		*ubi-minimal*) 
	   		docker_os=ubi-minimal
		;;
		*ubi*) 
	   		docker_os=ubi
		;;
		*ubuntu*|*latest*|*nightly*) 
	   		docker_os=ubuntu
		;;
   		*) echo "Unable to recognize DOCKER_OS from DOCKERIMAGE_TAG = $tag!";;
	esac     
}

parseCommandLineArgs "$@"

echo "Command type: $command_type, testdir: $test, version: $version, impl: $impl, docker_os: $docker_os, package: $package, build_type: $build_type"

if [ $command_type == "build" ]; then
	source $(dirname "$0")/build_image.sh $test $version $impl $docker_os $package $build_type
fi

if [ $command_type == "run" ]; then
	docker run --rm ${EXTRA_DOCKER_ARGS} adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type
fi

if [ $command_type == "clean" ]; then
	docker rmi -f adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type
fi