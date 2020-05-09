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

# This Dockerfile in thirdpart_containers/scala/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-scala-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version > -t adoptopenjdk-scala-test .`

ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=nightly

FROM $IMAGE_NAME:$IMAGE_VERSION

ARG SBT_VERSION=1.3.4
ARG SCALA_VERSION=v2.13.1

# Install test dependent executable files
RUN apt-get update; \
    apt-get install -y --no-install-recommends \
        git \
        wget \
        curl \
        gpg \
        gpg-agent \
        tar \
    ; \
    rm -rf /var/lib/apt/lists/*

ENV SBT_VERSION=$SBT_VERSION
ENV SBT_HOME=/opt/sbt

# Install SBT
RUN wget --no-check-certificate --no-cookies https://piccolo.link/sbt-${SBT_VERSION}.tgz \
    && wget --no-check-certificate --no-cookies https://piccolo.link/sbt-${SBT_VERSION}.tgz.asc \
    && curl "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --import \
    && gpg --verify sbt-${SBT_VERSION}.tgz.asc sbt-${SBT_VERSION}.tgz \
    && tar -zvxf sbt-${SBT_VERSION}.tgz -C /opt/ \
    && rm -f sbt-${SBT_VERSION}.tgz \
    && rm -f sbt-${SBT_VERSION}.tgz.asc

ENV PATH ${PATH}:${SBT_HOME}/bin

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

# This is the main script to run scala tests
COPY ./dockerfile/scala-test.sh /scala-test.sh

# Clone scala src to /scala
ENV SCALA_VERSION=$SCALA_VERSION
RUN git clone https://github.com/scala/scala.git
WORKDIR /scala/
RUN git checkout ${SCALA_VERSION}
WORKDIR /

ENTRYPOINT ["/bin/bash", "/scala-test.sh"]

# Used to pass in a default value for TEST_SUITE
#CMD [""]