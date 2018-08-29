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

# This Dockerfile in thirdparty_containers/example-test/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-example-test .`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version >-t adoptopenjdk-example-test .`

# This Dockerfile can be used as an example for external docker based tests. 

#ARG IMAGE_NAME=openj9
#ARG IMAGE_VERSION=latest
#FROM $IMAGE_NAME:$IMAGE_VERSION

FROM ubuntu:18.04

ARG openj9_repo=git@github.ibm.com:runtimes/Jaas_tr.open.git
ARG openj9_branch=milestone_10
ARG omr_repo=git@github.com:eclipse/omr.git
ARG omr_branch=master
ARG omr_sha=cdee39c0d09e1d1ce80d79f828dec5d33c48f0f7
ARG use_grpc=0 

# Install the required tools.
RUN apt-get update \
 && apt-get install -qq -y --no-install-recommends \
	  autoconf \
	  ca-certificates \
	  ccache \
	  cmake \
	  cpio \
	  file \
	  git \
	  libasound2-dev \
	  libcups2-dev \
	  libdwarf-dev \
	  libelf-dev \
	  libfreetype6-dev \
	  libnuma-dev \
	  libx11-dev \
	  libxext-dev \
	  libxrender-dev \
	  libxt-dev \
	  libxtst-dev \
	  make \
	  pkg-config \
      software-properties-common \
	  ssh \
	  unzip \
	  wget \
	  zip \
 && rm -rf /var/lib/apt/lists/*

# Install optional tools
RUN apt-get update \
  && apt-get install -qq -y --no-install-recommends \
    vim \
  && rm -rf /var/lib/apt/lists/*

# Download and setup freemarker.jar to /root/freemarker.jar
RUN cd /root \
  && wget https://sourceforge.net/projects/freemarker/files/freemarker/2.3.8/freemarker-2.3.8.tar.gz/download -O freemarker.tgz \
  && tar -xzf freemarker.tgz freemarker-2.3.8/lib/freemarker.jar --strip=2 \
  && rm -f freemarker.tgz

# Download the boot JDK from AdoptOpenJDK.
RUN cd /root \
 && wget https://api.adoptopenjdk.net/openjdk8-openj9/releases/x64_linux/latest/binary -O bootjdk8.tar.gz \
 && tar -xzf bootjdk8.tar.gz \
 && rm -f bootjdk8.tar.gz \
 && mv $(ls | grep -i jdk) bootjdk8

# Set JAVA_HOME, and prepend $JAVA_HOME/bin to PATH.
ENV JAVA_HOME=/root/bootjdk8
ENV PATH="$JAVA_HOME/bin:$PATH"

RUN apt-get update \
 && apt-get install -qq -y --no-install-recommends \
    automake \
    curl \
    g++-7 \
    gcc-7 \
    libssl-dev \
    libtool \
    build-essential \ 
    autoconf \
    pkg-config \
 && rm -rf /var/lib/apt/lists/*

# JITaaS needs to be built with a newer GCC
RUN ln -sf /usr/bin/g++ /usr/bin/c++ \
 && ln -sf /usr/bin/g++-7 /usr/bin/g++ \
 && ln -sf /usr/bin/gcc /usr/bin/cc \
 && ln -sf /usr/bin/gcc-7 /usr/bin/gcc

# grpc 1.10.1 and protobuf 3.5.1 install
RUN git clone -b $(curl -L https://grpc.io/release) https://github.com/grpc/grpc \
 && cd grpc \
 && git checkout v1.10.1 \
 && git submodule update --init \
 && cd /grpc/third_party/protobuf \
 && git checkout v3.5.1 \
 && cd /grpc/ \
 && make \
 && make install \
 && cd /grpc/third_party/protobuf \
 && make install \
 && rm -rf /grpc

# Grab sources from openj9-openjdk-jdk8 and JITaaS repos
# Then builds JITaaS
RUN if [ $use_grpc = 1 ] ; \
    then export JITAAS_USE_GRPC=1 ; \
    else echo "Building with raw socket implementation" ; \
    fi

WORKDIR /root
RUN git clone https://github.com/ibmruntimes/openj9-openjdk-jdk8 \
 && cd openj9-openjdk-jdk8 \
 && bash ./get_source.sh -openj9-repo=$openj9_repo -openj9-branch=$openj9_branch -omr-repo=$omr_repo -omr-branch=$omr_branch -omr-sha=$omr_sha \
 && bash ./configure --with-freemarker-jar=/root/freemarker.jar \
 && export UMA_SUPPRESS_WARNINGS_AS_ERRORS=1 \
 && make all

# Clean up
RUN mv /root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image /root/ \
 && mv /root/openj9-openjdk-jdk8/openj9/test /root/ \
 && rm -rf /root/openj9-openjdk-jdk8 \
 && rm -rf /root/bootjdk8 \
 && rm -rf /root/freemarker.jar

RUN apt-get update \
 && apt-get install -qq -y --no-install-recommends \
    ant \
    ant-contrib \
    perl \
    vim \
 && rm -rf /var/lib/apt/lists/*

# Install Docker module to run test framework
RUN echo yes | cpan install JSON Text::CSV

ENV JAVA_VERSION=SE80
ENV JAVA_BIN=/root/j2sdk-image/jre/bin
ENV SPEC=linux_x86-64_cmprssptrs
ENV JAVA_HOME=/root/j2sdk-image

# Builds tests
RUN cd /root/test/TestConfig \
 && make -f run_configure.mk \
 && make compile

COPY ./dockerfile/jitaas-openj9-sanity.sh /test.sh

ENTRYPOINT ["/bin/bash", "/test.sh"]
