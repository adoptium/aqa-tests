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

# This Dockerfile in external/openliberty-mp-tck/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-openliberty-mp-tck -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version> -t adoptopenjdk-openliberty-mp-tck .`

ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=nightly

FROM $IMAGE_NAME:$IMAGE_VERSION

ARG ANT_VERSION=1.10.6
ARG OPENLIBERTY_TAG=gm-19.0.0.12

# Install test dependent executable files
RUN apt-get update; \
    apt-get install -y --no-install-recommends \
        git \
        maven \
        wget \
        tar \
    ; \
    rm -rf /var/lib/apt/lists/*

ENV ANT_VERSION=$ANT_VERSION
ENV ANT_HOME=/opt/ant

# Install Ant
RUN wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz \
    && wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz.sha512 \
    && echo "$(cat apache-ant-${ANT_VERSION}-bin.tar.gz.sha512) apache-ant-${ANT_VERSION}-bin.tar.gz" | sha512sum -c \
    && tar -zvxf apache-ant-${ANT_VERSION}-bin.tar.gz -C /opt/ \
    && ln -s /opt/apache-ant-${ANT_VERSION} /opt/ant \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz.sha512

# Add Ant to PATH
ENV PATH ${PATH}:${ANT_HOME}/bin

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY ./dockerfile/openliberty-mp-tck.sh /openliberty-mp-tck.sh

ENV OPENLIBERTY_HOME $WORKDIR
RUN mkdir testResults

# Clone openliberty repo
ENV OPENLIBERTY_TAG=$OPENLIBERTY_TAG
RUN git clone https://github.com/OpenLiberty/open-liberty.git
WORKDIR /open-liberty/
RUN git checkout ${OPENLIBERTY_TAG}
WORKDIR /

ENTRYPOINT ["/bin/bash", "/openliberty-mp-tck.sh"]