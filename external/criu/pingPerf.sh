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

    curl -OLJSks https://github.com/eclipse-openj9/openj9/files/8774222/criuseccompprofile.json.txt
    mv criuseccompprofile.json.txt criuseccompprofile.json
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
    echo "create restore image ..."
    sudo podman run --name ol-instanton-test-checkpoint-container --privileged --env WLP_CHECKPOINT=applications ol-instanton-test-pingperf:latest
    sudo podman commit ol-instanton-test-checkpoint-container ol-instanton-test-pingperf-restore
    sudo podman rm ol-instanton-test-checkpoint-container
}

unprivilegedRestore() {
    echo "unprivileged restore ..."
    echo -ne "CONTAINER_ID=" > containerId.log
    sudo podman run \
        --rm \
        --detach \
        -p 9080:9080 \
        --cap-add=CHECKPOINT_RESTORE \
        --cap-add=NET_ADMIN \
        --cap-add=SYS_PTRACE \
        --security-opt seccomp=criuseccompprofile.json \
        ol-instanton-test-pingperf-restore >> containerId.log
}

privilegedRestore() {
    echo "privileged restore ..."
    echo -ne "CONTAINER_ID=" > containerId.log
    sudo podman run \
        --rm \
        --detach \
        --privileged \
        -p 9080:9080 \
        ol-instanton-test-pingperf-restore >> containerId.log
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
