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

# This Dockerfile in thirdpart_containers/system-test/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build system: `docker build -t adoptopenjdk-system-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build systemtest: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version > -t adoptopenjdk-system-test .`

 
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
	cpio \
	gcc

# Install ant
ENV ANT_VERSION 1.10.5
RUN wget --no-check-certificate https://www.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    gunzip apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xvf apache-ant-${ANT_VERSION}-bin.tar

RUN apt-get -y install \
        libtext-csv-perl \
        libjson-perl


VOLUME ["/java"]
#ENV JAVA_HOME=/java \
#    PATH=/java/bin:$PATH \
#    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"
ENV  JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

# Install Docker module to run test framework
RUN apt-get -y install \
	libtext-csv-perl \
	libjson-perl \
	libxml-parser-perl

# This is the main script to run system tests.  
COPY ./dockerfile/system-test.sh /system-test.sh

# Clone the repo where the 3rd party application code and tests reside. 
WORKDIR /
RUN pwd

ENV ANT_HOME ${WORKDIR}/apache-ant-${ANT_VERSION}

RUN wget https://sourceforge.net/projects/ant-contrib/files/ant-contrib/1.0b3/ant-contrib-1.0b3-bin.tar.gz && \
    tar -zxvf ant-contrib-1.0b3-bin.tar.gz && \
    cp ant-contrib/ant-contrib-1.0b3.jar ${ANT_HOME}/lib


# Clone AdoptOpenJDK openjdk-tests repo
RUN git clone https://github.com/AdoptOpenJDK/openjdk-tests.git

ENV JAVA_BIN=$JAVA_HOME/bin
ENV SPEC=linux_x86-64_cmprssptrs
ENV BUILD_LIST=systemtest

RUN cd /openjdk-tests && \
    ./get.sh -t /openjdk-tests  

# Generates test targets applicable to the platform, implementation and version
RUN echo "Generating test targets applicable to the platform, implementation and version..." && \
    cd /openjdk-tests/TestConfig && \
    make -f run_configure.mk && \
    make compile

ENTRYPOINT ["/bin/bash", "/system-test.sh"]
CMD ["--version"]
