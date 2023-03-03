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

set -e

pingPerfZipPath=""
testJDKPath=""
restoreImage="ol-instanton-test-pingperf-restore"
job_name=""
build_number=""
docker_registry_dir=""
docker_os="ubi"
node_label_current_os=""
node_label_micro_architecture=""
restore_docker_image_name_list=()

getCriuseccompproFile() {
    if [[ ! -f "criuseccompprofile.json" ]]; then
        curl -OLJSks https://github.com/eclipse-openj9/openj9/files/8774222/criuseccompprofile.json.txt
        mv criuseccompprofile.json.txt criuseccompprofile.json
    fi
}

prepare() {
    echo "prepare at $(pwd)..."
    if [ -f "$pingPerfZipPath" ]; then
        rm -f PingperfFiles.zip
        cp "$pingPerfZipPath" .
        unzip PingperfFiles.zip
    else 
        echo "${pingPerfZipPath} does not exist."
        exit 1
    fi

    getCriuseccompproFile

    git clone https://github.com/OpenLiberty/ci.docker.git
    (
        cd ci.docker || exit
        git checkout instanton

        # replace commands in openLiberty dockerfile
        libertyDockerfilePath="releases/latest/beta-instanton/Dockerfile.ubi.openjdk17"
        sed -i "s:ENV JAVA_VERSION jdk-17.0.4.1+1: :" $libertyDockerfilePath
        sed -i '/USER 1001.*/a RUN \/opt\/java\/openjdk\/bin\/java  --version' $libertyDockerfilePath

        commandToRemove='tar -xf /tmp/openjdk.tar.xz --strip-components=1; \\'
        commandAfterRemovedOne="rm -rf /tmp/openjdk.tar.xz;"
        attachCommand="COPY NEWJDK/ /opt/java/openjdk"
        sed -i "s:$commandAfterRemovedOne:$attachCommand:" $libertyDockerfilePath
        sed -i "s:$commandToRemove:$commandAfterRemovedOne:" $libertyDockerfilePath
 
        mkdir releases/latest/beta-instanton/NEWJDK
        cp -r $testJDKPath/. releases/latest/beta-instanton/NEWJDK/
    )
}

buildImage() {
    echo "build image at $(pwd)..."
    sudo podman build -t icr.io/appcafe/open-liberty:beta-instanton -f ci.docker/releases/latest/beta-instanton/Dockerfile.ubi.openjdk17 ci.docker/releases/latest/beta-instanton
    sudo podman build -t ol-instanton-test-pingperf:latest -f Dockerfile.pingperf .
}

createRestoreImage() {
    echo "create restore image $restoreImage ..."
    sudo podman run --name ol-instanton-test-checkpoint-container --privileged --env WLP_CHECKPOINT=applications ol-instanton-test-pingperf:latest
    sudo podman commit ol-instanton-test-checkpoint-container $restoreImage
    sudo podman rm ol-instanton-test-checkpoint-container
}

unprivilegedRestore() {
    echo "unprivileged restore $restoreImage ..."
    echo -ne "CONTAINER_ID=" > containerId.log
    sudo podman run \
        --rm \
        --detach \
        -p 9080:9080 \
        --cap-add=CHECKPOINT_RESTORE \
        --cap-add=NET_ADMIN \
        --cap-add=SYS_PTRACE \
        --security-opt seccomp=criuseccompprofile.json \
        $restoreImage >> containerId.log
}

privilegedRestore() {
    echo "privileged restore $restoreImage ..."
    echo -ne "CONTAINER_ID=" > containerId.log
    sudo podman run \
        --rm \
        --detach \
        --privileged \
        -p 9080:9080 \
        $restoreImage >> containerId.log
}

response() {
    echo "response ..."
    bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://127.0.0.1:9080/pingperf/ping/greeting)" != "200" ]]; do sleep .00001; done'
}

checkLog() {
    echo "check log ..."
    if [ -f ./containerId.log ]; then
        cat ./containerId.log
    else 
        echo "./containerId.log does not exist."
        exit 1
    fi
    sleep 1
    source ./containerId.log
    echo "podman logs --tail 10 ${CONTAINER_ID}"
    sudo podman logs --tail 10 "${CONTAINER_ID}"
    echo "find response ..."
    sudo podman logs --tail 10 "${CONTAINER_ID}" | grep "FR "
}

clean() {
    echo "clean ..."
    sudo podman stop --all
    sudo podman container rm --all -f
}

testCreateRestoreImageOnly() {
    clean
    prepare
    buildImage
    createRestoreImage
}

dockerRegistryLogin() {
    if [[ $DOCKER_REGISTRY_URL ]]; then
        echo "Private Docker Registry $DOCKER_REGISTRY_URL login..."
        echo $DOCKER_REGISTRY_CREDENTIALS_PSW | sudo podman login --username=$DOCKER_REGISTRY_CREDENTIALS_USR --password-stdin $DOCKER_REGISTRY_URL
    else
        echo "DOCKER_REGISTRY_URL is not set. Cannot lgoin."
        exit 1
    fi
}

dockerRegistryLogout() {
    if [[ $DOCKER_REGISTRY_URL ]]; then
        sudo podman logout $DOCKER_REGISTRY_URL
    else
        echo "DOCKER_REGISTRY_URL is not set. Cannot logout."
        exit 1
    fi
}

pushImage() {
    dockerRegistryLogin
    echo "Pushing docker image..."

    restore_ready_checkpoint_image_folder="${DOCKER_REGISTRY_URL}/${job_name}/pingperf_${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${PLATFORM}-${node_label_current_os}-${node_label_micro_architecture}"
    tagged_restore_ready_checkpoint_image_num="${restore_ready_checkpoint_image_folder}:${build_number}"
    tagged_restore_ready_checkpoint_image_latest="${restore_ready_checkpoint_image_folder}:latest"

    # Push a docker image with build_num for records
    echo "tag $restoreImage $tagged_restore_ready_checkpoint_image_num"
    sudo podman tag $restoreImage $tagged_restore_ready_checkpoint_image_num
    echo "Pushing docker image ${tagged_restore_ready_checkpoint_image_num}"
    sudo podman push $tagged_restore_ready_checkpoint_image_num

    if [[ "$job_name" == *"criu_image_upload"* ]]; then
        # Push another copy as the nightly default latest
        echo "tag $tagged_restore_ready_checkpoint_image_num $tagged_restore_ready_checkpoint_image_latest"
        sudo podman tag $tagged_restore_ready_checkpoint_image_num $tagged_restore_ready_checkpoint_image_latest
        echo "Pushing docker image ${tagged_restore_ready_checkpoint_image_latest} to docker registry"
        sudo podman push $tagged_restore_ready_checkpoint_image_latest
    fi

    dockerRegistryLogout
}

getImageNameList() {
    if [ ! -z "${docker_registry_dir}" ]; then
        echo "Testing image from specified docker_registry_dir: $docker_registry_dir"
        restore_docker_image_name_list+=("${DOCKER_REGISTRY_URL}/${docker_registry_dir}")
	else
        echo "Testing images from nightly builds"
        image_os_combo_list=($CRIU_XLINUX_COMBO_LIST)
        for image_os_combo in ${image_os_combo_list[@]}
        do
            restore_docker_image_name_list+=("${DOCKER_REGISTRY_URL}/criu_image_upload/pingperf_${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${PLATFORM}-${image_os_combo}:latest")
        done
    fi
}

pullImageUnprivilegedRestore() {
    dockerRegistryLogin
    getImageNameList
    echo "The host machine micro-architecture is ${node_label_micro_architecture}"
    for restore_docker_image_name in ${restore_docker_image_name_list[@]}
    do
        echo "Pulling image $restore_docker_image_name"
        sudo podman pull $restore_docker_image_name
        getCriuseccompproFile

        # restore
        restoreImage=$restore_docker_image_name
        testUnprivilegedRestoreOnly
        clean
    done

    dockerRegistryLogout
}

pullImagePrivilegedRestore() {
    dockerRegistryLogin
    getImageNameList
    echo "The host machine micro-architecture is ${node_label_micro_architecture}"
    for restore_docker_image_name in ${restore_docker_image_name_list[@]}
    do
        echo "Pulling image $restore_docker_image_name"
        sudo podman pull $restore_docker_image_name

        # restore
        restoreImage=$restore_docker_image_name
        testPrivilegedRestoreOnly
        clean
    done

    dockerRegistryLogout
}

testUnprivilegedRestoreOnly() {
    unprivilegedRestore
    response
    checkLog
}

testPrivilegedRestoreOnly() {
    privilegedRestore
    response
    checkLog
}

testCreateImageAndUnprivilegedRestore() {
    testCreateRestoreImageOnly
    testUnprivilegedRestoreOnly
}

testCreateImageAndPrivilegedRestore() {
    testCreateRestoreImageOnly
    testPrivilegedRestoreOnly
}

setup() {
    echo "NODE_LABELS: $NODE_LABELS"
    echo "PLATFORM: $PLATFORM"
    echo "uname -a: $(uname -a)"
    
    if [ -n "$(cat /etc/redhat-release | grep 'Red Hat')" ]; then
        cat /etc/redhat-release
    fi

    if [ ! -z "${docker_registry_dir}" ]; then
        docker_registry_dir=$(echo "$docker_registry_dir" | tr '[:upper:]' '[:lower:]')  # docker registry link must be lowercase
        IFS=':' read -r -a dir_array <<< "$docker_registry_dir"
        job_name=${dir_array[0]}
        build_number=${dir_array[1]}
        echo "job_name: $job_name"
        echo "build_number: $build_number"
    fi

    node_label_micro_architecture=""
    node_label_current_os=""
    for label in $NODE_LABELS
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
    done
}

if [ "$1" == "prepare" ]; then
    pingPerfZipPath=$2
    testJDKPath=$3
    prepare
elif [ "$1" == "buildImage" ]; then
    buildImage
elif [ "$1" == "createRestoreImage" ]; then
    createRestoreImage
elif [ "$1" == "unprivilegedRestore" ]; then
    unprivilegedRestore
elif [ "$1" == "privilegedRestore" ]; then
    privilegedRestore
elif [ "$1" == "response" ]; then
    response
elif [ "$1" == "checkLog" ]; then
    checkLog
elif [ "$1" == "clean" ]; then
    clean
elif [ "$1" == "testCreateRestoreImageOnly" ]; then
    pingPerfZipPath=$2
    testJDKPath=$3
    testCreateRestoreImageOnly
elif [ "$1" == "testUnprivilegedRestoreOnly" ]; then
    testUnprivilegedRestoreOnly
elif [ "$1" == "testPrivilegedRestoreOnly" ]; then
    testPrivilegedRestoreOnly
elif [ "$1" == "pullImageUnprivilegedRestore" ]; then
    docker_registry_dir=$2 #docker_registry_dir can be empty
    setup
    pullImageUnprivilegedRestore
elif [ "$1" == "pullImagePrivilegedRestore" ]; then
    docker_registry_dir=$2 #docker_registry_dir can be empty
    setup
    pullImagePrivilegedRestore
elif [ "$1" == "testCreateRestoreImageAndPushToRegistry" ]; then
    pingPerfZipPath=$2
    testJDKPath=$3
    docker_registry_dir=$4
    setup
    testCreateRestoreImageOnly
    pushImage
elif [ "$1" == "testCreateImageAndUnprivilegedRestore" ]; then
    pingPerfZipPath=$2
    testJDKPath=$3
    testCreateImageAndUnprivilegedRestore
elif [ "$1" == "testCreateImageAndPrivilegedRestore" ]; then
    pingPerfZipPath=$2
    testJDKPath=$3
    testCreateImageAndPrivilegedRestore
else
    echo "unknown command"
fi
