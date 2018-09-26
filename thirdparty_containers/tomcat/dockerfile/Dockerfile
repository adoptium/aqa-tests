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

# This Dockerfile in thirdpart_containers/tomcat/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build tomcat: `docker build -t adoptopenjdk-tomcat-test .`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version > -t adoptopenjdk-tomcat-test .`
 
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

# Install OpenSSL
RUN git clone https://github.com/openssl/openssl.git && cd /openssl && git checkout OpenSSL_1_0_2k \ 
	&& ./config --prefix=/usr --openssldir=/usr/local/openssl shared \
	&& make \
	&& make install

VOLUME ["/java"]
ENV JAVA_HOME=/java \
    PATH=/java/bin:$PATH \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

# This is the main script to run tomcat tests.  
COPY ./dockerfile/tomcat-test.sh /tomcat-test.sh

# Clone the repo where the 3rd party application code and tests reside. 
WORKDIR /
RUN pwd

# Clone tomcat to /tomcat 
RUN git clone https://github.com/apache/tomcat85.git

ENTRYPOINT ["/bin/bash", "/tomcat-test.sh"]
CMD ["--version"]
