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

# This Dockerfile in external/quarkus-test/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-quarkus-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version >-t adoptopenjdk-quarkus-test .`

 
ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=latest

FROM $IMAGE_NAME:$IMAGE_VERSION

# Install test dependent executable files
RUN apt-get update \
	&& apt-get -y install \
	ant \
	apt-transport-https \
	ca-certificates \
	curl \
	git \
	make \
	unzip \
	vim 

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"
ENV MODE="java"

COPY ./dockerfile/quarkus-test.sh /quarkus-test.sh
RUN mkdir testResults
WORKDIR /
RUN pwd
RUN git clone https://github.com/quarkusio/quarkus.git
  
# if MODE=-Dnative, then get graalvm
# RUN wget https://ci.adoptopenjdk.net/view/all/job/jdk8-with-graal_build_x86-64_linux/lastSuccessfulBuild/artifact/build/x86_64/linux_macos/jdk8-with-graal-local/jdk8-with-graal-jdk.tar.gz

ENTRYPOINT ["/bin/bash", "/quarkus-test.sh"]
CMD ["$MODE"]
