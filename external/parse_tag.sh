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

set -e

# Parse environment variable DOCKERIMAGE_TAG
# where all possible tags are 
# to create additional env vars 
# DOCKER_OS, BUILD_TYPE, PACKAGE

echo "Parsing DOCKERIMAGE_TAG = ${DOCKERIMAGE_TAG}"
export DOCKER_OS=ubuntu
export BUILD_TYPE=full
export PACKAGE=jdk

tag=${DOCKERIMAGE_TAG}

# set PACKAGE
case $tag in
   	*jre*) 
    	export PACKAGE=jre
	;;
esac  

# set BUILD_TYPE
case $tag in
   	*-slim*|*_slim*) 
   		export BUILD_TYPE=slim
   	;;
esac

# set DOCKER_OS
case $tag in
   	*alpine*) 
	   export DOCKER_OS=alpine
	;;
   	*debianslim*) 
	   export DOCKER_OS=debianslim
	;;
	*debian*) 
	   export DOCKER_OS=debian
	;;
	*centos*) 
	   export DOCKER_OS=centos
	;;
	*clefos*) 
	   export DOCKER_OS=clefos
	;;
	*ubi-minimal*) 
	   export DOCKER_OS=ubi-minimal
	;;
	*ubi*) 
	   export DOCKER_OS=ubi
	;;
	*ubuntu*|*latest*|*nightly*) 
	   export DOCKER_OS=ubuntu
	;;
   	*) echo "Unable to recognize DOCKER_OS from DOCKERIMAGE_TAG = $tag!";;
esac     

echo "DOCKER_OS = ${DOCKER_OS} BUILD_TYPE = ${BUILD_TYPE} PACKAGE = ${PACKAGE}"



