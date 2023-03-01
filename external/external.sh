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
job_name=""
build_number=""
node_name=""
node_labels=""
node_label_micro_architecture=""
container_run="docker run"
container_login="docker login"
container_inspect="docker inspect"
container_cp="docker cp"
container_commit="docker commit"
container_tag="docker tag"
container_logout="docker logout"
container_push="docker push"
container_pull="docker pull"
container_rm="docker rm"
container_rmi="docker rmi"
criu_micro_architecture_list=""
docker_registry_required="false"
docker_registry_url=""
docker_registry_dir=""
reportdst="false"
reportsrc="false"
docker_args=""
mountV=""
mount_jdk="true"
imageArg=""


usage () {
	echo 'Usage : external.sh  --dir TESTDIR --tag DOCKERIMAGE_TAG --version JDK_VERSION --impl JDK_IMPL [--docker_os docker_os][--platform PLATFORM] [--portable portable] [--node_name node_name] [--node_labels node_labels] [--docker_registry_required docker_registry_required] [--docker_registry_url DOCKER_REGISTRY_URL] [--docker_registry_dir DOCKER_REGISTRY_DIR] [--criu_micro_architecture_list CRIU_MICRO_ARCHITECTURE_LIST] [--mount_jdk mount_jdk] [--test_root TEST_ROOT] [--reportsrc appReportDir] [--reportdst REPORTDIR] [--testtarget target] [--docker_args EXTRA_DOCKER_ARGS] [--build|--run|--load|--clean]'
}

supported_tests="external_custom aot camel criu-portable-checkpoint  criu-portable-restore criu-ubi-portable-checkpoint criu-ubi-portable-restore derby elasticsearch jacoco jenkins functional-test kafka lucene-solr openliberty-mp-tck payara-mp-tck quarkus quarkus_quickstarts scala system-test tomcat tomee wildfly wycheproof netty spring"

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

				if [[ "${test}" == *"criu"* ]]; then
					container_run="sudo podman run"
					container_login="sudo podman login"
					container_inspect="sudo podman inspect"
					container_cp="sudo podman cp"
					container_commit="sudo podman commit"
					container_tag="sudo podman tag"
					container_logout="sudo podman logout"
					container_push="sudo podman push"
					container_pull="sudo podman pull"
					container_rm="sudo podman rm"
					container_rmi="sudo podman rmi"
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

			"--mount_jdk" )
				mount_jdk="$1"; shift;;

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

			"--docker_registry_required" )
				docker_registry_required="$1"; shift;;

			"--docker_registry_url" )
				docker_registry_url="$1"; shift;;

			"--docker_registry_dir" )
				docker_registry_dir="$1"; shift;
				docker_registry_dir=$(echo "$docker_registry_dir" | tr '[:upper:]' '[:lower:]')  # docker registry link must be lowercase
				IFS=':' read -r -a dir_array <<< "$docker_registry_dir"
				job_name=${dir_array[0]}
				build_number=${dir_array[1]};;

			"--criu_default_image_job_name" )
				criu_default_image_job_name="$1"; shift;;

			"--criu_micro_architecture_list" )
				criu_micro_architecture_list="$1"; shift;;

			"--test_root" )
				test_root="$1"; shift;;

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
  $container_inspect --format '{{ .NetworkSettings.IPAddress }}' "$@"
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
		echo "$container_run --privileged $mountV --name $test-test adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type $testtarget"
		if [ -n "$testtarget" ]; then
			$container_run --privileged $mountV --name $test-test adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type "$testtarget";
		else
			$container_run --privileged $mountV --name $test-test adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type;
		fi
		if [ $reportsrc != "false" ]; then
			$container_cp $test-test:$reportsrc $reportdst/external_test_reports;
		fi
		
		if [ $portable != "false" ]; then
			if [[ $docker_registry_url ]]; then
				echo "Private Docker Registry login starts:"
				echo $DOCKER_REGISTRY_CREDENTIALS_PSW | $container_login --username=$DOCKER_REGISTRY_CREDENTIALS_USR --password-stdin $docker_registry_url

				restore_ready_checkpoint_image_folder="${docker_registry_url}/${job_name}/${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${node_label_micro_architecture}"
				tagged_restore_ready_checkpoint_image_num="${restore_ready_checkpoint_image_folder}:${build_number}"
				tagged_restore_ready_checkpoint_image_latest="${restore_ready_checkpoint_image_folder}:latest"

				# Push a docker image with build_num for records
				echo "tagged_restore_ready_checkpoint_image_num is $tagged_restore_ready_checkpoint_image_num"
				$container_commit --change='ENTRYPOINT ["/bin/bash", "/test_restore.sh"]' $test-test $tagged_restore_ready_checkpoint_image_num
				echo "Pushing docker image ${tagged_restore_ready_checkpoint_image_num}"
				$container_push $tagged_restore_ready_checkpoint_image_num

				# Push another copy as the nightly default latest
				echo "Change Tag from build_number to latest"
				$container_tag $tagged_restore_ready_checkpoint_image_num $tagged_restore_ready_checkpoint_image_latest

				if [[ "$job_name" == *"$criu_default_image_job_name"* ]]; then
					echo "Pushing docker image ${tagged_restore_ready_checkpoint_image_latest} to docker registry"
					$container_push $tagged_restore_ready_checkpoint_image_latest
				fi
				$container_logout $docker_registry_url
			else
				echo "Docker Registry is not available on this Jenkins"
				exit 1
			fi
		fi
	else
		echo "$container_run --privileged $mountV --name $test-test --rm adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type $testtarget"
		if [ -n "$testtarget" ]; then
			$container_run --privileged $mountV --name $test-test --rm adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type "$testtarget";
		else
			$container_run --privileged $mountV --name $test-test --rm adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type;
		fi
	fi
fi

if [ $command_type == "load" ]; then
	if [[ $docker_registry_required != "false" ]]; then
		if [[ $docker_registry_url ]]; then
			echo "Private Docker Registry login starts:"
			echo $DOCKER_REGISTRY_CREDENTIALS_PSW | $container_login --username=$DOCKER_REGISTRY_CREDENTIALS_USR --password-stdin $docker_registry_url
			
			mount_options="$mountV"
			if [[ $mount_jdk == "false" ]]; then
				echo "JDK inside the docker image is used for testing"
				mount_options=""
			fi

			restore_docker_image_name_list=()
			if [ ! -z "${docker_registry_dir}" ]; then
				echo "Testing image from specified DOCKER_REGISTRY_DIR"
				restore_docker_image_name_list+=("${docker_registry_url}/${docker_registry_dir}")
			else
				echo "Testing images from nightly builds"
				image_micro_architecture_list=($criu_micro_architecture_list)
				for image_micro_architecture in ${image_micro_architecture_list[@]}
				do
					restore_docker_image_name_list+=("${docker_registry_url}/$criu_default_image_job_name/${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${image_micro_architecture}:latest")
				done
			fi
			
			echo "The host machine micro-architecture is ${node_label_micro_architecture}"
			for restore_docker_image_name in ${restore_docker_image_name_list[@]}
			do
				echo "Pulling image $restore_docker_image_name"
				$container_pull $restore_docker_image_name
				# restore
				echo "$container_run --privileged $mount_options --name restore-test --rm $restore_docker_image_name"
				$container_run --privileged $mount_options --name restore-test --rm $restore_docker_image_name
			done
			
			$container_logout $docker_registry_url
		else
			echo "Docker Registry is not available on this Jenkins"
			exit 1
		fi
	else # no need private docker registry
		docker_image_name="eclipse-temurin:${JDK_VERSION}-jdk"
		if [[ "${JDK_IMPL}" == *"openj9"* ]]; then
			docker_image_name="ibm-semeru-runtimes:open-${JDK_VERSION}-jdk"
		fi
		$container_pull $docker_image_name
		test_script_path="$test_root/external/$test/test.sh"
		chmod a+x $test_script_path
		mount_test_script="-v $test_script_path:/test.sh"
		mount_options=$mount_test_script
		if [[ $mount_jdk != "false" ]]; then
			echo "Mounting JDK and test script"
			mount_options="$mountV $mount_test_script"
		fi
		echo "$container_run --privileged $mount_options --name restore-test --rm $docker_image_name bash /test.sh"
		$container_run --privileged $mount_options --name restore-test --rm $docker_image_name bash /test.sh
	fi
fi

if [ $command_type == "clean" ]; then
	if [[ ${test} == 'external_custom' ]]; then
			test="$(echo ${EXTERNAL_CUSTOM_REPO} | awk -F'/' '{print $NF}' | sed 's/.git//g')"
	fi
	$container_rm -f $test-test; $container_rmi -f adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type
	$container_rm -f restore-test
	$container_rmi -f ${docker_registry_url}/${job_name}/${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${node_label_micro_architecture}:latest
	$container_rmi -f ${docker_registry_url}/${criu_default_image_job_name}/${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${node_label_micro_architecture}:latest
fi
