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

# script runs in 5 modes - prepare / build / run / load / clean

set -e

source $(dirname "$0")/provider.sh

if [ -z "${EXTRA_DOCKER_ARGS}" ] ; then
  echo \
"   # ================= WARNING ================= WARNING ================= WARNING ================= #
   # EXTRA_DOCKER_ARGS are not set. You will be testing java which is already in container and not TEST_JDK_HOME
   # TEST_JDK_HOME is set to $TEST_JDK_HOME but will not be used. See test_base_functions.sh for order of search
   # You should set your's TEST_JDK_HOME to mount to /opt/java/openjdk, eg:                          #
   # export EXTRA_DOCKER_ARGS=\"-v \$TEST_JDK_HOME:/opt/java/openjdk\"                               #
   # ================= WARNING ================= WARNING ================= WARNING ================= #"
else
  echo \
"   # =================== Info =================== Info =================== Info =================== #
   # EXTRA_DOCKER_ARGS set as \"$EXTRA_DOCKER_ARGS\"                                #
   # =================== Info =================== Info =================== Info =================== #"
  if echo "${EXTRA_DOCKER_ARGS}" | grep -q "$TEST_JDK_HOME"  ; then
    echo \
"   # =================== Info =================== Info =================== Info =================== #
   # TEST_JDK_HOME of $TEST_JDK_HOME is used in EXTRA_DOCKER_ARGS                                #
   # =================== Info =================== Info =================== Info =================== #"
  else
    echo \
"   # ================= WARNING ================= WARNING ================= WARNING ================= #
   # TEST_JDK_HOME of $TEST_JDK_HOME is NOT used in EXTRA_DOCKER_ARGS                                #
   # ================= WARNING ================= WARNING ================= WARNING ================= #"
  fi
fi


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
docker_image_source_job_name=""
build_number=$BUILD_NUMBER
node_name=""
node_labels=""
node_label_micro_architecture=""
node_label_current_os=""
container_run="$(getExternalImageCommand) run"
container_login="$(getExternalImageCommand) login"
container_inspect="$(getExternalImageCommand) inspect"
container_cp="$(getExternalImageCommand) cp"
container_commit="$(getExternalImageCommand) commit"
container_tag="$(getExternalImageCommand) tag"
container_logout="$(getExternalImageCommand) logout"
container_push="$(getExternalImageCommand) push"
container_pull="$(getExternalImageCommand) pull"
container_rm="$(getExternalImageCommand) rm"
container_rmi="$(getExternalImageCommand) rmi"
docker_registry_required="false"
docker_registry_url=""
docker_registry_dir=""
base_docker_registry_url="default"
base_docker_registry_dir="default"
reportdst="false"
reportsrc="false"
docker_args=""
mountV=""
mount_jdk="true"
imageArg=""


usage () {
	echo 'Usage : external.sh  --dir TESTDIR --tag DOCKERIMAGE_TAG --version JDK_VERSION --impl JDK_IMPL [--docker_os docker_os][--platform PLATFORM] [--portable portable] [--node_name node_name] [--node_labels node_labels] [--docker_registry_required docker_registry_required] [--docker_registry_url DOCKER_REGISTRY_URL] [--docker_registry_dir DOCKER_REGISTRY_DIR] [--base_docker_registry_url baseDockerRegistryUrl] [--base_docker_registry_dir baseDockerRegistryDir] [--mount_jdk mount_jdk] [--test_root TEST_ROOT] [--reportsrc appReportDir] [--reportdst REPORTDIR] [--testtarget target] [--docker_args EXTRA_DOCKER_ARGS] [--build|--run|--load|--clean|--prune]'
}

supported_tests="external_custom aot camel criu-functional criu-portable-checkpoint  criu-portable-restore criu-ubi-portable-checkpoint criu-ubi-portable-restore derby elasticsearch jacoco jenkins functional-test kafka lucene-solr openliberty-mp-tck payara-mp-tck quarkus quarkus_quickstarts scala system-test tck-ubi-test tomcat tomee wildfly wycheproof netty spring"

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

				if [[ "${test}" == *"criu"* || "${test}" == tck-* ]]; then
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

			"--docker_os" | "-dos" )
				docker_os="$1"; shift;;

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
					if [[ -z "$node_label_micro_architecture" && "$label" == "hw.arch."*"."* ]]; then #hw.arch.x86.skylake
						node_label_micro_architecture=$label
						echo "node_label_micro_architecture is $node_label_micro_architecture"
					elif [[ -z "$node_label_current_os" && "$label" == "sw.os."*"."* ]]; then # sw.os.ubuntu.22 sw.os.rhel.8
						node_label_current_os=$label
						echo "node_label_current_os is $node_label_current_os"
					elif [[ -n "$node_label_current_os" && -n "$node_label_micro_architecture" ]]; then
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
				docker_image_source_job_name=${dir_array[0]}
				build_number=${dir_array[1]};;

			"--base_docker_registry_url" )
				if [ -z "$1" ]; then 
					base_docker_registry_url="default";
				else 
  					base_docker_registry_url="$1";
				fi
				shift;;

			"--base_docker_registry_dir" )
				if [ -z "$1" ]; then 
					base_docker_registry_dir="default";
				else 
  					base_docker_registry_dir="$1";
				fi
				shift;;

			"--criu_default_image_job_name" )
				criu_default_image_job_name="$1"; shift;;

			"--test_root" )
				test_root="$1"; shift;;

			"--reportsrc" )
				reportsrc="$1"; shift;;

			"--reportdst" )
				reportdst="$1"; shift;;

			"--testtarget" )
				testtarget="$1"; shift;;

			"--prepare" | "-p" )
				command_type=prepare;;

			"--build" | "-b" )
				command_type=build;;

			"--run" | "-r" )
				command_type=run;;

			"--load" | "-l" )
				command_type=load;;

			"--clean" | "-c" )
				command_type=clean;;

			"--prune" | "-p" )
				command_type=prune;;

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

if [ $command_type == "prepare" ]; then
	# Specify docker.io or internal registry to prepare base image with login to increase pull limit or authenticate; Redhat Registry no login.
	if [[ $base_docker_registry_url != "default" ]]; then
		# Container credential check. 
		# Temporarily host criu-ubi image with criu binary on internal/private hub.  In that case, provide USR/PSW for access 
		if [[ "${test}" == *"criu-ubi"* && ! -z $DOCKER_REGISTRY_CREDENTIALS_USR ]]; then
			BASE_DOCKER_REGISTRY_CREDENTIAL_USR=$DOCKER_REGISTRY_CREDENTIALS_USR
			BASE_DOCKER_REGISTRY_CREDENTIAL_PSW=$DOCKER_REGISTRY_CREDENTIALS_PSW
		fi
		if [[ ! -z $BASE_DOCKER_REGISTRY_CREDENTIAL_USR ]]; then
			echo "Base Docker Registry login starts to obtain Base Docker Image:"
			echo $BASE_DOCKER_REGISTRY_CREDENTIAL_PSW | $container_login --username=$BASE_DOCKER_REGISTRY_CREDENTIAL_USR --password-stdin $base_docker_registry_url
		else
			echo "No credential available for container registry, will proceed without login..."
		fi

		if [[ $base_docker_registry_dir == "default" ]]; then
			base_docker_image_name="eclipse-temurin:${JDK_VERSION}-jdk"
			if [[ "${JDK_IMPL}" == *"openj9"* ]]; then
				base_docker_image_name="ibm-semeru-runtimes:open-${JDK_VERSION}-jdk"
			fi
		else
			base_docker_image_name="$base_docker_registry_dir:latest"
		fi

		echo "$container_pull $base_docker_registry_url/$base_docker_image_name"
		$container_pull $base_docker_registry_url/$base_docker_image_name

		if [[ ! -z $BASE_DOCKER_REGISTRY_CREDENTIAL_USR || ! -z $DOCKER_REGISTRY_CREDENTIALS_USR ]]; then
			$container_logout $base_docker_registry_url
		fi
	fi
fi

if [ $command_type == "build" ]; then
	echo "build_image.sh $test $version $impl $docker_os $package $build_type $platform $base_docker_registry_dir $check_external_custom $imageArg"
	source $(dirname "$0")/build_image.sh $test $version $impl $docker_os $package $build_type $platform "$base_docker_registry_dir" $check_external_custom $imageArg
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

				restore_ready_checkpoint_image_folder="${docker_registry_url}/${docker_image_source_job_name}/${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${node_label_current_os}-${node_label_micro_architecture}"
				tagged_restore_ready_checkpoint_image_num="${restore_ready_checkpoint_image_folder}:${build_number}"

				# Push a docker image with build_num for records
				echo "tagged_restore_ready_checkpoint_image_num is $tagged_restore_ready_checkpoint_image_num"
				$container_commit --change='ENTRYPOINT ["/bin/bash", "/test_restore.sh"]' $test-test $tagged_restore_ready_checkpoint_image_num
				echo "Pushing docker image ${tagged_restore_ready_checkpoint_image_num}"
				$container_push $tagged_restore_ready_checkpoint_image_num
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

			if [[ $JOB_NAME == "Grinder" ]]; then
			    echo "Testing image from docker_registry_dir"
				restore_docker_image_name_list+=("${docker_registry_url}/$docker_image_source_job_name:${build_number}")
			else
				echo "Testing images from nightly builds"
				# - is shell metacharacter. In PLATFORM value, replace - with _
				platValue=$(echo $PLATFORM | sed "s/-/_/")
				comboList=CRIU_COMBO_LIST_$platValue
				if [[ "$PLATFORM" =~ "linux_390-64" ]]; then
					micro_architecture=$(echo $node_label_micro_architecture | sed "s/hw.arch.s390x.//")
					comboList=$comboList_$micro_architecture
				fi
				image_os_micro_architecture_list="${!comboList}"
				echo "${comboList}: ${image_os_micro_architecture_list}"
				for image_os_micro_architecture in ${image_os_micro_architecture_list[@]}
				do
					restore_docker_image_name_list+=("${docker_registry_url}/$docker_image_source_job_name/${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${image_os_micro_architecture}:${build_number}")
				done
			fi

			echo "The host machine OS is ${node_label_current_os}, and micro-architecture is ${node_label_micro_architecture}"
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
		docker_image_name="docker.io/library/eclipse-temurin:${JDK_VERSION}-jdk"
		if [[ "${JDK_IMPL}" == *"openj9"* ]]; then
			docker_image_name="docker.io/library/ibm-semeru-runtimes:open-${JDK_VERSION}-jdk"
		fi
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

if [ "${command_type}" == "clean" ] ; then
	if [[ ${test} == 'external_custom' ]]; then
			test="$(echo ${EXTERNAL_CUSTOM_REPO} | awk -F'/' '{print $NF}' | sed 's/.git//g')"
	fi
	if [ "${EXTERNAL_AQA_CONTAINER_CLEAN}" == "false" ] ; then
			echo "to debug, put '-i --entrypoint /bin/bash' before container name"
			container_rm="echo to clean, run manually: $container_rm"
			container_rmi="echo to clean, run manually: $container_rmi"
	fi
	$container_rm -f $test-test; $container_rmi -f adoptopenjdk-$test-test:${JDK_VERSION}-$package-$docker_os-${JDK_IMPL}-$build_type
	$container_rm -f restore-test
	$container_rmi -f ${docker_registry_url}/${docker_image_source_job_name}/${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${platform}-${node_label_current_os}-${node_label_micro_architecture}:${build_number}
	$container_rmi -f ${docker_registry_url}/${docker_image_source_job_name}:${build_number}
fi

if [ "${command_type}" == "prune" ] ; then
	$(getExternalImageCommand) system prune --all --force
fi
