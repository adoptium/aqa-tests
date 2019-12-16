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

# This Dockerfile in external/thorntail-mp-tck/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-thorntail-mp-tck -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version> -t adoptopenjdk-thorntail-mp-tck .`

ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=latest

FROM $IMAGE_NAME:$IMAGE_VERSION

ARG THORNTAIL_TAG=2.6.0.Final

# Install test dependent executable files
RUN apt-get update; \
    apt-get install -y --no-install-recommends \
        git \
        maven \
    ; \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY ./dockerfile/thorntail-mp-tck.sh /thorntail-mp-tck.sh

ENV THORNTAIL_HOME $WORKDIR
RUN mkdir testResults

# Clone thorntail repo
ENV THORNTAIL_TAG=$THORNTAIL_TAG
RUN git clone https://github.com/thorntail/thorntail.git
WORKDIR /thorntail/
RUN git checkout ${THORNTAIL_TAG}
WORKDIR /

ENTRYPOINT ["/bin/bash", "/thorntail-mp-tck.sh"]

# Used to pass in a default value for TEST_OPTIONS
#CMD [""]