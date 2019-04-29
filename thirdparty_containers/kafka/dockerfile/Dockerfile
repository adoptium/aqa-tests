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

# This Dockerfile in thirdparty_containers/kafka/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-kafka-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version >-t adoptopenjdk-kafka-test .`
 
ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=latest

## Kafka 2.1 branch, 2.1.0 release
ARG KAFKA_VERSION="809be928f1ae004e11d65c307ea322bef126c834"

FROM $IMAGE_NAME:$IMAGE_VERSION

# Install test dependent executable files
RUN apt-get update \
	&& apt-get -y install \
	ant \
	git \
	make \
	unzip \
	vim \
	wget

#Install Gradle
RUN mkdir -p /usr/share/gradle \
         && cd /usr/share/gradle \
         && wget -q https://services.gradle.org/distributions/gradle-5.0-bin.zip \
         && unzip -qq gradle-5.0-bin.zip \
         && rm -rf gradle-5.0-bin.zip \
         && ln -s /usr/share/gradle/gradle-5.0/bin/gradle /usr/bin/gradle \
         && cd /

RUN cd .

ENV  JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

# This is the main script to run kafka tests. 
COPY ./dockerfile/kafka-test.sh /kafka-test.sh

# Clone the repo where kafka tests reside.
WORKDIR /
RUN pwd

RUN git clone https://github.com/apache/kafka.git
WORKDIR kafka
RUN git checkout $KAFKA_VERSION
WORKDIR /

ENTRYPOINT ["/bin/bash", "/kafka-test.sh"]
CMD [""]
