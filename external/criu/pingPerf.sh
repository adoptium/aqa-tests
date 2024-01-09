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
ubiRepoFilePath="" # Temporarily This file is only for plinux ubi8
testJDKPath=""
jdkVersion=""
restoreImage="ol-instanton-test-pingperf-restore"
docker_image_source_job_name=""
build_number=$BUILD_NUMBER
docker_registry_dir=""
docker_os="ubi"
docker_os_version="8"
node_label_current_os=""
node_label_micro_architecture=""
restore_docker_image_name_list=()

getCriuseccompproFile() {
    if [[ ! -f "criuseccompprofile.json" ]]; then
        curl -OLJSks https://github.com/eclipse-openj9/openj9/files/8774222/criuseccompprofile.json.txt
        mv criuseccompprofile.json.txt criuseccompprofile.json
    fi
}

getSemeruDockerfile() {
    if [[ ! -f "Dockerfile.open.releases.full" ]]; then
        if [[ $jdkVersion ]]; then
            jdkVersionDir=$jdkVersion
            if [[ $jdkVersion == "21" ]]; then
                jdkVersionDir="$jdkVersion-ea"
            fi
            semeruDockerfile="Dockerfile.open.releases.full"
            semeruDockerfileUrlBase="https://raw.githubusercontent.com/ibmruntimes/semeru-containers/ibm/$jdkVersionDir/jdk/${docker_os}"
            if [[ $docker_os == "ubi" ]]; then
                echo "curl -OLJSks ${semeruDockerfileUrlBase}/${docker_os}${docker_os_version}/${semeruDockerfile}"
                curl -OLJSks ${semeruDockerfileUrlBase}/${docker_os}${docker_os_version}/${semeruDockerfile}
                if [[ ${PLATFORM} == *"ppc"*  &&  $docker_os_version == "8" ]]; then
                    findCommandAndReplace 'FROM registry.access.redhat.com/ubi8/ubi:latest' 'FROM registry.access.redhat.com/ubi8/ubi:latest \n COPY ubi.repo /etc/yum.repos.d/ \n ' $semeruDockerfile ";"
                fi
                findCommandAndReplace '\-H \"\${CRIU_AUTH_HEADER}\"' '--user \"\${DOCKER_REGISTRY_CREDENTIALS_USR}:\${DOCKER_REGISTRY_CREDENTIALS_PSW}\"' $semeruDockerfile ";"
                findCommandAndReplace 'RUN --mount.*' 'ARG DOCKER_REGISTRY_CREDENTIALS_USR \n ARG DOCKER_REGISTRY_CREDENTIALS_PSW \n RUN set -eux; \\' $semeruDockerfile
                # in 21-ea, /opt/java/openjdk/legal/java.base/LICENSE does not exist. No need to replace
                if [[ $jdkVersion != "21" ]]; then
                    findCommandAndReplace '\/opt\/java\/openjdk\/legal\/java.base\/LICENSE \/licenses;' "\/opt\/java\/openjdk\/legal\/java.base\/LICENSE \/licenses\/;" $semeruDockerfile
                fi
            else # docker_os is ubuntu
                echo "curl -OLJSks ${semeruDockerfileUrlBase}/${semeruDockerfile}"
                curl -OLJSks ${semeruDockerfileUrlBase}/${semeruDockerfile}
            fi

            findCommandAndReplace 'curl -LfsSo \/tmp\/openjdk.tar.gz ${BINARY_URL};' " " $semeruDockerfile
            findCommandAndReplace 'echo "\${ESUM} \*\/tmp\/openjdk.tar.gz" | sha256sum -c -;' " " $semeruDockerfile
            if [[ $jdkVersion == "21" ]]; then
                findCommandAndReplace 'mkdir -p \/opt\/java\/java-ea; \\' "mkdir -p \/opt\/java\/java-ea;" $semeruDockerfile
                findCommandAndReplace 'cd \/opt\/java\/java-ea; \\' "COPY NEWJDK\/ \/opt\/java\/java-ea" $semeruDockerfile
                findCommandAndReplace 'tar -xf \/tmp\/openjdk.tar.gz --strip-components=1;' "RUN \/opt\/java\/java-ea\/bin\/java --version" $semeruDockerfile
            else
                findCommandAndReplace 'mkdir -p \/opt\/java\/openjdk; \\' "mkdir -p \/opt\/java\/openjdk;" $semeruDockerfile
                findCommandAndReplace 'cd \/opt\/java\/openjdk; \\' "COPY NEWJDK\/ \/opt\/java\/openjdk" $semeruDockerfile
                findCommandAndReplace 'tar -xf \/tmp\/openjdk.tar.gz --strip-components=1;' "RUN \/opt\/java\/openjdk\/bin\/java --version" $semeruDockerfile

            fi

            mkdir NEWJDK
            cp -r $testJDKPath/. NEWJDK/
        else
            echo "jdkVersion value is not set. Cannot download docker file."
            exit 1
        fi
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

    if [[ ${PLATFORM} == *"ppc"* && $docker_os == "ubi" && $docker_os_version == "8" ]]; then
        if [ -f "$ubiRepoFilePath" ]; then
            rm -f ubi.repo
            cp "$ubiRepoFilePath" .
        else
            echo "${ubiRepoFilePath} does not exist."
            exit 1
        fi
    fi

    getCriuseccompproFile
    getSemeruDockerfile

    git clone https://github.com/OpenLiberty/ci.docker.git
    (
        cd ci.docker || exit
        if [ "$OPENLIBERTY_SHA" != "" ]
        then
            git checkout $OPENLIBERTY_SHA
        fi
        curCommitID=$(git rev-parse HEAD)
        echo "Using dockerfile from OpenLiberty/ci.docker repo branch $openLibertyBranch with commit hash $curCommitID"
        if [[ $docker_os == "ubi" ]]; then
            # Temporarily OpenLiberty ubi dockerfile only supports openjdk 17, not 11
            libertyDockerfilePath="releases/latest/beta/Dockerfile.${docker_os}.openjdk17"
            # replace OpenLiberty dockerfile base image
            findCommandAndReplace "FROM icr.io\/appcafe\/ibm-semeru-runtimes:open-17-jdk-${docker_os}" "FROM local-ibm-semeru-runtimes:latest" $libertyDockerfilePath '/'
        else # docker_os is ubuntu
            libertyDockerfilePath="releases/latest/beta/Dockerfile.${docker_os}.openjdk${jdkVersion}"
            findCommandAndReplace "FROM ibm-semeru-runtimes:open-${jdkVersion}-jre-jammy" "FROM local-ibm-semeru-runtimes:latest" $libertyDockerfilePath '/'

        fi
    )
}

findCommandAndReplace() {
    local oldCmd="$1"
    local newCmd="$2"
    local fileName="$3"
    # Default sed delimiter is ":"
    local sedDelimiter=":"
    if [ ! -z "$4" ]; then
        sedDelimiter="$4"
    fi

    echo "start grep: grep -c \"$oldCmd\" $fileName"
    local occurrences=$(grep -c "$oldCmd" $fileName)
    if [[ $occurrences -eq 0 ]]; then
        echo "Error: The command '$oldCmd' was not found in $fileName."
        exit 1
    else
	echo "replace command is 's$sedDelimiter$oldCmd$sedDelimiter$newCmd$sedDelimiter'"
        sed -i "s$sedDelimiter$oldCmd$sedDelimiter$newCmd$sedDelimiter" $fileName
    fi
}

buildImage() {
    echo "build image at $(pwd)..."
    sudo podman build -t local-ibm-semeru-runtimes:latest -f Dockerfile.open.releases.full . --build-arg DOCKER_REGISTRY_CREDENTIALS_USR=$DOCKER_REGISTRY_CREDENTIALS_USR --build-arg DOCKER_REGISTRY_CREDENTIALS_PSW=$DOCKER_REGISTRY_CREDENTIALS_PSW 2>&1 | tee build_semeru_image.log 
    # Temporarily OpenLiberty ubi dockerfile only supports openjdk 17, not 11, need to add jdkVersion for ubuntu support later
    sudo podman build -t icr.io/appcafe/open-liberty:beta-instanton -f ci.docker/releases/latest/beta/Dockerfile.${docker_os}.openjdk17 ci.docker/releases/latest/beta
    sudo podman build -t ol-instanton-test-pingperf:latest -f Dockerfile.pingperf .
}

createRestoreImage() {
    echo "create restore image $restoreImage ..."
    sudo podman run --name ol-instanton-test-checkpoint-container --privileged --env WLP_CHECKPOINT=afterAppStart ol-instanton-test-pingperf:latest
    sudo podman commit ol-instanton-test-checkpoint-container $restoreImage
    sudo podman rm ol-instanton-test-checkpoint-container
}

unprivilegedRestore() {
    echo "unprivileged restore $restoreImage ..."
    echo -ne "CONTAINER_ID=" > containerId.log
    echo "sudo podman run --rm --detach -p 9080:9080 --cap-add=CHECKPOINT_RESTORE --cap-add=SETPCAP $restoreImage"
    sudo podman run \
        --rm \
        --detach \
        -p 9080:9080 \
        --cap-add=CHECKPOINT_RESTORE \
        --cap-add=SETPCAP \
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

    restore_ready_checkpoint_image_folder="${DOCKER_REGISTRY_URL}/${docker_image_source_job_name}/pingperf_${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${docker_os_version}-${PLATFORM}-${node_label_current_os}-${node_label_micro_architecture}"
    tagged_restore_ready_checkpoint_image_num="${restore_ready_checkpoint_image_folder}:${build_number}"

    # Push a docker image with build_num for records
    echo "tag $restoreImage $tagged_restore_ready_checkpoint_image_num"
    sudo podman tag $restoreImage $tagged_restore_ready_checkpoint_image_num
    echo "Pushing docker image ${tagged_restore_ready_checkpoint_image_num}"
    sudo podman push $tagged_restore_ready_checkpoint_image_num

    dockerRegistryLogout
}

getImageNameList() {

    if [[ $JOB_NAME == "Grinder" ]]; then
			    echo "Testing specified image from docker_registry_dir"
				restore_docker_image_name_list+=("${DOCKER_REGISTRY_URL}/$docker_image_source_job_name:${build_number}")
    else
        echo "Testing all images from: ${docker_image_source_job_name}:${build_number}"
        # - is shell metacharacter. In PLATFORM value, replace - with _
        echo "PLATFORM: ${PLATFORM}"
        platValue=$(echo $PLATFORM | sed "s/-/_/")
        comboList=CRIU_COMBO_LIST_$platValue
        if [[ "$PLATFORM" =~ "linux_390-64" ]]; then
            micro_architecture=$(echo $node_label_micro_architecture | sed "s/hw.arch.s390x.//")
            comboList="${comboList}_${micro_architecture}"
        elif [[ "$PLATFORM" =~ "linux_ppc-64" ]]; then
            micro_architecture=$(echo $node_label_micro_architecture | sed "s/hw.arch.ppc64le.//")
            comboList="${comboList}_${micro_architecture}"
        fi

        image_os_combo_list="${!comboList}"
        echo "comboList: ${comboList}"
        echo "image_os_combo_list: ${image_os_combo_list}"
        for image_os_combo in ${image_os_combo_list[@]}
        do
            restore_docker_image_name_list+=("${DOCKER_REGISTRY_URL}/${docker_image_source_job_name}/pingperf_${JDK_VERSION}-${JDK_IMPL}-${docker_os}-${docker_os_version}-${PLATFORM}-${image_os_combo}:${build_number}")
        done
        if [[ -z "$restore_docker_image_name_list" ]]; then
            echo "Error: restore_docker_image_name_list is empty."
            exit 1
        else
            echo "restore_docker_image_name_list: $restore_docker_image_name_list"
        fi
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
        docker_image_source_job_name=${dir_array[0]}
        build_number=${dir_array[1]}
        echo "build_number: $build_number"
    fi
    echo "docker_image_source_job_name: $docker_image_source_job_name"

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
    ubiRepoFilePath=$3
    testJDKPath=$4
    jdkVersion=$5
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
    ubiRepoFilePath=$3
    testJDKPath=$4
    jdkVersion=$5
    docker_os=$6
    docker_os_version=$7
    testCreateRestoreImageOnly
elif [ "$1" == "testUnprivilegedRestoreOnly" ]; then
    testUnprivilegedRestoreOnly
elif [ "$1" == "testPrivilegedRestoreOnly" ]; then
    testPrivilegedRestoreOnly
elif [ "$1" == "pullImageUnprivilegedRestore" ]; then
    docker_os=$2
    docker_os_version=$3
    docker_registry_dir=$4 #docker_registry_dir can be empty
    setup
    pullImageUnprivilegedRestore
elif [ "$1" == "pullImagePrivilegedRestore" ]; then
    docker_os=$2
    docker_os_version=$3
    docker_registry_dir=$4 #docker_registry_dir can be empty
    setup
    pullImagePrivilegedRestore
elif [ "$1" == "testCreateRestoreImageAndPushToRegistry" ]; then
    pingPerfZipPath=$2
    ubiRepoFilePath=$3
    testJDKPath=$4
    jdkVersion=$5
    docker_os=$6
    docker_os_version=$7
    docker_registry_dir=$8
    setup
    testCreateRestoreImageOnly
    pushImage
elif [ "$1" == "testCreateImageAndUnprivilegedRestore" ]; then
    pingPerfZipPath=$2
    ubiRepoFilePath=$3
    testJDKPath=$4
    jdkVersion=$5
    docker_os=$6
    docker_os_version=$7
    testCreateImageAndUnprivilegedRestore
elif [ "$1" == "testCreateImageAndPrivilegedRestore" ]; then
    pingPerfZipPath=$2
    ubiRepoFilePath=$3
    testJDKPath=$4
    jdkVersion=$5
    docker_os=$6
    docker_os_version=$7
    testCreateImageAndPrivilegedRestore
else
    echo "unknown command"
fi
