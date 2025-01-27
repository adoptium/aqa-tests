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

###################################################################
## This script walks through all external tests                  ##
## and determines                                                ##
## 1) which virtualisation to use                                ##
## 2) which image to use                                         ##
###################################################################


###################################################################
##      part *1*  which virtualisation to use                    ##
## It recognizes two environment variables:                      ##
## EXTERNAL_AQA_RUNNER=docker|podman|...                         ##
## EXTERNAL_AQA_SUDO=sudo||runas                                 ##
##                                                               ##
## EXTERNAL_AQA_RUNNER defaults to podman if podman is installed ##
##                     otherwise to docker                       ##
## EXTERNAL_AQA_SUDO defaults to empty string                    ##
###################################################################

if [ -z "${EXTERNAL_AQA_RUNNER}" ]; then
  if which podman > /dev/null 2>&1; then
     EXTERNAL_AQA_RUNNER=podman
  else
     EXTERNAL_AQA_RUNNER=docker
  fi
fi

function getExternalImageBuildCommand() {
  #"sudo docker build"
  echo "$(getExternalImageCommand) build"
}

function getExternalImageCommand() {
  #"sudo docker"
  echo "${EXTERNAL_AQA_SUDO} ${EXTERNAL_AQA_RUNNER}"
}

function getProviderNice() {
  echo "${EXTERNAL_AQA_RUNNER}"
}

function getSudoNice() {
  if [ -z "${EXTERNAL_AQA_SUDO}" ]; then
    echo "not-as-root"
  else
    echo "${EXTERNAL_AQA_SUDO}"
  fi
}

function getProviderTile() {
  echo "$(getSudoNice)/$(getProviderNice)"
}

#####################################################################
##              part *2*  which image to use                       ##
## It recognizes one complex environment variable:                 ##
## EXTERNAL_AQA_IMAGE=optional_url/image:optional_version          ##
##   defaults to:                                                  ##
## docker.io/library/eclipse-temurin:${JDK_VERSION}-jdk"           ##
##   for most of the calls. Defaults to                            ##
## docker.io/library/ibm-semeru-runtimes:open-${JDK_VERSION}-jdk   ##
##   for openj9                                                    ##
## for prepare under "default" circumstances the domain is omitted ##
## for sometimes, the "latest" can slip as tag and for several     ##
## cases the registry.access.redhat.com are used as domain.        ##
## the:                                                            ##
##   ARG OS                                                        ##
##   ARG IMAGE_VERSION                                             ##
##   ARG TAG                                                       ##
## are deducted from this                                          ##
####################################################################

function isExternalImageEnabled() {
  if [ -n "${EXTERNAL_AQA_IMAGE}" ] ; then
    return 0
  else
    return 1
  fi
}

function getFullTemurinImage() {
  local JDK_VERSION="${1:-0}"
  local jreSdk="${2:-jdk}"
  if [ -z "${EXTERNAL_AQA_IMAGE}" ]; then
    echo "docker.io/library/eclipse-temurin:${JDK_VERSION}-$jreSdk"
  else
    echo "${EXTERNAL_AQA_IMAGE}"
  fi
}

function getFullOpenJ9Image() {
  local JDK_VERSION="${1:-0}"
  local jreSdk="${2:-jdk}"
  if [ -z "${EXTERNAL_AQA_IMAGE}" ]; then
    echo "docker.io/library/ibm-semeru-runtimes:open-${JDK_VERSION}-$jreSdk"
  else
    echo "${EXTERNAL_AQA_IMAGE}"
  fi
}

function getImageOs() {
  if [ -z "${EXTERNAL_AQA_IMAGE}" ]; then
    echo "ubuntu"
  else
     local osAndTag="${EXTERNAL_AQA_IMAGE##*/}"
     local os="${osAndTag%%:*}"
     echo "${os}"
  fi
}

function getImageOsVersion() {
  echo "nightly"
}

function getTemurinImageTag() {
  getImageTag `getFullTemurinImage ${1:-0} ${2:-jdk}`
}

function getOpenJ9ImageTag() {
  getImageTag `getFullOpenJ9Image ${1:-0} ${2:-jdk}`
}

function getImageTag() {
  local image="$1"
  local osAndTag="${image##*/}"
  local tag=${osAndTag##*:}
  if [ "${tag}" = "${osAndTag}" ] ; then
     echo "latest"
  else
     echo "${tag}"
  fi
}

function getTemurinImageName() {
  getImageName `getFullTemurinImage ${1:-0} ${2:-jdk}`
}

function getOpenJ9ImageName() {
  getImageName `getFullOpenJ9Image ${1:-0} ${2:-jdk}`
}

function getImageName() {
  local image="$1"
  local name="${image%%:*}"
  echo "${name}"
}

function getTemurinImageTag() {
  getImageTag `getFullTemurinImage ${1:-0} ${2:-jdk}`
}

function getOpenJ9ImageTag() {
  getImageTag `getFullOpenJ9Image ${1:-0} ${2:-jdk}`
}

function getImageTag() {
  local image="$1"
  local osAndTag="${image##*/}"
  local tag=${osAndTag##*:}
  if [ "${tag}" = "${osAndTag}" ] ; then
     echo "latest"
  else
     echo "${tag}"
  fi
}
