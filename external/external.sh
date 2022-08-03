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

# script runs in 4 modes - build / run / load / clean

set -e
tag=nightly
docker_os=ubuntu
build_type=full
package=jdk
command_type=build
version=8
impl=hotspot
test=derby
testtarget=""
platform="linux_x86-64"
portable="false"
node_name=""
node_labels=""
node_label_micro_architecture=""
docker_registry_url=""
reportdst="false"
reportsrc="false"
docker_args=""
mountV=""
imageArg=""


usage () {
	echo 'Usage : external.sh  --dir TESTDIR --tag DOCKERIMAGE_TAG --version JDK_VERSION --impl JDK_IMPL [--docker_os docker_os][--platform PLATFORM] [--portable portable] [--node_name node_name] [--node_labels node_labels] [--docker_registry_url DOCKER_REGISTRY_URL] [--reportsrc appReportDir] [--reportdst REPORTDIR] [--testtarget target] [--docker_args EXTRA_DOCKER_ARGS] [--build|--run|--load|--clean]'
}

supported_tests="external_custom camel criu-portable-checkpoint  criu-portable-restore criu-ubi-portable-checkpoint criu-ubi-portable-restore derby elasticsearch jacoco jenkins functional-test kafka lucene-solr openliberty-mp-tck payara-mp-tck quarkus quarkus_quickstarts scala system-test tomcat tomee wildfly wycheproof netty spring"

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

parseCommandLineArgs() {
	check_external_custom=0
	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift; 

		case "$opt" in
			"--dir" | "-d" )
				test="$1";
				echo "The test here is ${test}"
				if [ ${test} == 'external_custom' ]; then
				    test="$(echo ${EXTERNAL_CUSTOM_REPO} | awk -F'/' '{print $NF}' | sed 's/.git//g')"
					if (check_test ${test}); then
						check_external_custom=1
					fi
				fi
				echo "The directory in the external.sh is ${test}"

				if [[ "${test}" == *"ubi"* ]]; then
					docker_os=ubi
				fi
				shift;;
			
			"--version" | "-v" )
				version="$1"; shift;;
			
			"--impl" | "-i" )
				impl="$1"; shift;;

			"--docker_args" )
				if [ -z ${1+x} ]; then 
					echo "No EXTRA_DOCKER_ARGS set"; 
				else 
  					docker_args="$1"; shift;
  					parse_docker_args $docker_args;
				fi;;
				
			"--tag" | "-t" )
				if [ -z "$1" ]; then 
					echo "No DOCKERIMAGE_TAG set, tag as default 'nightly'"; 
				else 
  					tag="$1";
				fi
				shift;
				parse_tag;;

			"--platform" )
				platform="$1"; shift;;

			"--portable" )
				portable="$1"; shift;;

			"--node_name" )
				node_name="$1"; shift;;

			"--node_labels" )
				node_labels="$1"; shift;
				for label in $node_labels
				do 
					if [[ "$label" == "hw.arch."*"."* ]]; then
						node_label_micro_architecture=$label
						echo "node_label_micro_architecture is $node_label_micro_architecture"
						break
					fi
				done;;

			"--docker_registry_url" )
				docker_registry_url="$1"; shift;;

			"--reportsrc" )
				reportsrc="$1"; shift;;

			"--reportdst" )
				reportdst="$1"; shift;;

			"--testtarget" )
				testtarget="$1"; shift;;

			"--build" | "-b" )
				command_type=build;; 

			"--run" | "-r" )
				command_type=run;;

			"--load" | "-l" )
				command_type=load;;

			"--clean" | "-c" )
				command_type=clean;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done	
}

# Parse environment variable DOCKERIMAGE_TAG
# to set docker_os, build_type, package
function parse_tag() { 

	# set PACKAGE
	case $tag in
   		*jre*) 
    		package=jre
		;;
	esac
	
	# set DOCKER_OS
	case $tag in
		*ubuntu*|*latest*|*nightly*) 
			echo "DOCKERIMAGE_TAG $tag has been recognized.";;
   		*) echo "Unable to recognize DOCKER_OS from DOCKERIMAGE_TAG = $tag!";;
	esac
	
}

function parse_docker_args() {
# parse docker_args to two variable: mountV and  imageArg
	mountV=""; 
	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift; 

		case "$opt" in
			"--volume" | "-v" )
				mountV="${mountV} -v $1 ";
				shift;;
			"--tmpfs" | "-v" )
				mountV="${mountV} --tmpfs $1 ";
				shift;;
			"--image" | "-i" )
				imageArg="$1"; 
				shift;;
			*) echo >&2 "Invalid docker args option: ${opt}"; exit 1;
		esac
	done
}


function docker-ip() {
  docker inspect --format '{{ .NetworkSettings.IPAddress }}' "$@"
}

parseCommandLineArgs "$@"

# set DOCKER_HOST env variables 
# DOCKER_HOST=$(docker-ip $test-test)

if [ $command_type == "build" ]; then
	echo "build_image.sh $test $version $impl $docker_os $package $build_type $platform $check_external_custom $imageArg"
	source $(dirname "$0")/build_image.sh $test $version $impl $docker_os $package $build_type $platform $check_external_custom $imageArg
fi

if [ $command_type == "run" ]; then
	if [[ ${test} == 'external_custom' ]]; then
			test="$(echo ${EXTERNAL_CUSTOM_REPO} | awk -F'/' '{print $NF}' | sed 's/.git//g')"
	fi

	if [[ $reportsrc != "false" ]] || [[ $portable != "false" ]]; then
		echo "docker run --privileged $mountV --name $test-test adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type $testtarget"
		if [ -n "$testtarget" ]; then
			docker run --privileged $mountV --name $test-test adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type "$testtarget";
		else
			docker run --privileged $mountV --name $test-test adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type;
		fi
		if [ $reportsrc != "false" ]; then
			docker cp $test-test:$reportsrc $reportdst/external_test_reports;
		fi
		
		if [ $portable != "false" ]; then
			if [[ $docker_registry_url ]]; then
				echo "Private Docker Registry login starts:"
				echo $DOCKER_REGISTRY_CREDENTIALS_PSW | docker login --username=$DOCKER_REGISTRY_CREDENTIALS_USR --password-stdin $docker_registry_url
				
				restore_ready_checkpoint_image="criu-restore-ready-with-jdk"
				restore_ready_checkpoint_tag="${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${node_label_micro_architecture}"
				tagged_restore_ready_checkpoint_image="${docker_registry_url}/${restore_ready_checkpoint_image}:${restore_ready_checkpoint_tag}"
				echo "tagged_restore_ready_checkpoint_image is $tagged_restore_ready_checkpoint_image"
				docker commit --change='ENTRYPOINT ["/bin/bash", "/test_restore.sh"]' $test-test $tagged_restore_ready_checkpoint_image

				echo "Pushing docker image ${restore_ready_checkpoint_image}:${restore_ready_checkpoint_tag} to docker registry"
				docker push $tagged_restore_ready_checkpoint_image

				docker logout $docker_registry_url
			else
				echo "Docker Regsitry is not avaialble on this Jenkins"
				exit 1
			fi
		fi
	else
		echo "docker run --privileged $mountV --name $test-test --rm adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type $testtarget"
		if [ -n "$testtarget" ]; then
			docker run --privileged $mountV --name $test-test --rm adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type "$testtarget";
		else
			docker run --privileged $mountV --name $test-test --rm adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type;
		fi
	fi
fi

if [ $command_type == "load" ]; then
	# TO-DO: Use a different micro architecture (e.g. skylake vs broadwell) to test when machine label completed.
	if [[ $docker_registry_url ]]; then
		echo "Private Docker Registry login starts:"
		echo $DOCKER_REGISTRY_CREDENTIALS_PSW | docker login --username=$DOCKER_REGISTRY_CREDENTIALS_USR --password-stdin $docker_registry_url
		
		current_micro_architecture=$node_label_micro_architecture
		docker pull $docker_registry_url/criu-restore-ready-with-jdk:${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${current_micro_architecture}
		docker image ls
		# restore
		echo "docker run --privileged $mountV --name restore-checkpoint --rm $docker_registry_url/criu-restore-ready-with-jdk:${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${current_micro_architecture}"
		docker run --privileged $mountV --name restore-checkpoint --rm $docker_registry_url/criu-restore-ready-with-jdk:${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${current_micro_architecture}

		docker logout $docker_registry_url
	else
		echo "Docker Regsitry is not avaialble on this Jenkins"
		exit 1
	fi
fi

if [ $command_type == "clean" ]; then
	if [[ ${test} == 'external_custom' ]]; then
			test="$(echo ${EXTERNAL_CUSTOM_REPO} | awk -F'/' '{print $NF}' | sed 's/.git//g')"
	fi
	docker rm -f $test-test; docker rmi -f adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type
	docker rm -f restore-checkpoint
	docker rmi -f ${docker_registry_url}/criu-restore-ready-with-jdk:${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${node_label_micro_architecture}
fi
