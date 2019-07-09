# (C) Copyright IBM Corporation 2017.
#
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

# This Dockerfile in buildenv/docker dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-test .`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version >-t adoptopenjdk-test .`

ARG IMAGE_NAME=adoptopenjdk/openjdk8
ARG IMAGE_VERSION=latest

FROM ${IMAGE_NAME}:${IMAGE_VERSION}

# Use this when there is a need for input during docker image building process
ENV DEBIAN_FRONTEND noninteractive

# Install required OS and testing tools
RUN apt-get update \
  && apt-get install -qq -y --no-install-recommends \
    ant \
    ant-contrib \
    autoconf \
    bash \
    build-essential \
    ca-certificates \
    git \
    make \
    perl \
    unzip \
    wget \
    vim \
    zip \
    xserver-xorg-core \
    xserver-xorg \
    xauth \
    xorg \
    openbox \
    x11vnc \
    xvfb \
    ftpd \
    krb5-kdc \
    krb5-admin-server \
    pwgen \
    tomcat8 \
    libxml-parser-perl \
  && rm -rf /var/lib/apt/lists/*

# Install Docker module to run test framework
RUN echo yes | cpan install JSON Text::CSV

# These two folders are used testing purpose.
# In the `docker run` command, user also need mount <JDK_ROOT>
# and <openjdk-test_root_dir> to the Docker image by using cmd:
#   `docker run -it -v <path_to_JDK_root>:/java -v <path_to_openjdk_test_root_dir>:/test <your_image_name>`
VOLUME ["/test","/java"]

# Shell script to automate test execution
COPY ./mktest.sh /mktest.sh

CMD /bin/bash /mktest.sh