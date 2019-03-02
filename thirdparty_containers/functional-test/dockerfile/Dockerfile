# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This Dockerfile in thirdpart_containers/functional-test/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build functional: `docker build -t adoptopenjdk-functional-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build functionaltest: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version > -t adoptopenjdk-functional-test .`

 
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
	dirmngr \
	curl \
	git \
	make \
	unzip \
	vim \
	wget \
	gcc

RUN apt-get -y install \
	libtext-csv-perl \
	libjson-perl

# Install Docker module to run test framework
RUN echo yes | cpan install JSON Text::CSV XML::Parser

# Install ant
ENV ANT_VERSION 1.10.5
RUN wget --no-check-certificate https://www.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    gunzip apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xvf apache-ant-${ANT_VERSION}-bin.tar


VOLUME ["/java"]
ENV JAVA_HOME=/java \
    PATH=/java/bin:$PATH \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"


# This is the main script to run functional tests.  
COPY ./dockerfile/functional-test.sh /functional-test.sh

# Clone the repo where the functional tests reside. 
WORKDIR /
RUN pwd

ENV ANT_HOME ${WORKDIR}/apache-ant-${ANT_VERSION}

RUN wget https://sourceforge.net/projects/ant-contrib/files/ant-contrib/1.0b3/ant-contrib-1.0b3-bin.tar.gz && \
    tar -zxvf ant-contrib-1.0b3-bin.tar.gz && \
    cp ant-contrib/ant-contrib-1.0b3.jar ${ANT_HOME}/lib

# Clone OpenJ9 functional test repo
RUN git clone https://github.com/eclipse/openj9.git

ENTRYPOINT ["/bin/bash", "/functional-test.sh"]
CMD ["--version"]
