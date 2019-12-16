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

# This Dockerfile in external/kafka/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-kafka-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version > -t adoptopenjdk-kafka-test .`
 
ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=latest

FROM $IMAGE_NAME:$IMAGE_VERSION

ARG KAFKA_TAG=2.1.0
ARG GRADLE_VERSION=5.1

# Install test dependent executable files
RUN apt-get update; \
    apt-get install -y --no-install-recommends \
        git \
        wget \
        unzip \
    ; \
    rm -rf /var/lib/apt/lists/*

ENV GRADLE_VERSION=$GRADLE_VERSION
ENV GRADLE_HOME /opt/gradle

# Install Gradle
RUN wget --no-check-certificate --no-cookies https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && wget --no-check-certificate --no-cookies https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip.sha256 \
    && echo "$(cat gradle-${GRADLE_VERSION}-bin.zip.sha256) gradle-${GRADLE_VERSION}-bin.zip" | sha256sum -c \
    && unzip gradle-${GRADLE_VERSION}-bin.zip -d ${GRADLE_HOME} \
    && ln --symbolic "${GRADLE_HOME}/gradle-${GRADLE_VERSION}/bin/gradle" /usr/bin/gradle \
    && rm -f gradle-${GRADLE_VERSION}-bin.zip \
    && rm -f gradle-${GRADLE_VERSION}-bin.zip.sha256

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

# This is the main script to run kafka tests. 
COPY ./dockerfile/kafka-test.sh /kafka-test.sh

# Clone the repo where kafka tests reside.
ENV KAFKA_TAG=$KAFKA_TAG
RUN git clone https://github.com/apache/kafka.git
WORKDIR /kafka/
RUN git checkout ${KAFKA_TAG}
WORKDIR /

ENTRYPOINT ["/bin/bash", "/kafka-test.sh"]