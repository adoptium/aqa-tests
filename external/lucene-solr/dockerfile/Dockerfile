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

# This Dockerfile in external/lucene-solr/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-lucene-solr-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version> -t adoptopenjdk-lucene-solr-test .`

ARG SDK=openjdk11
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=latest

FROM $IMAGE_NAME:$IMAGE_VERSION

# Lucene Branch/Tag
ARG LUCENE_SOLR_TAG=releases/lucene-solr/8.3.1
ARG ANT_VERSION=1.10.6
ARG IVY_VERSION=2.5.0

# Install test dependent executable files
RUN apt-get update; \
    apt-get install -y --no-install-recommends \
        git \
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

#apache ivy setup 
ENV IVY_VERSION=$IVY_VERSION
RUN wget --no-check-certificate --no-cookies https://archive.apache.org/dist/ant/ivy/${IVY_VERSION}/apache-ivy-${IVY_VERSION}-bin.tar.gz \
    && wget --no-check-certificate --no-cookies https://archive.apache.org/dist/ant/ivy/${IVY_VERSION}/apache-ivy-${IVY_VERSION}-bin.tar.gz.sha512 \
    && echo "$(cat apache-ivy-${IVY_VERSION}-bin.tar.gz.sha512) apache-ivy-${IVY_VERSION}-bin.tar.gz" | sha512sum -c \
    && tar -zvxf apache-ivy-${IVY_VERSION}-bin.tar.gz apache-ivy-${IVY_VERSION}/ivy-${IVY_VERSION}.jar -C ${ANT_HOME}/lib/ \
    && rm -f apache-ivy-${IVY_VERSION}-bin.tar.gz \
    && rm -f apache-ivy-${IVY_VERSION}-bin.tar.gz.sha512

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY ./dockerfile/lucene-solr-test.sh /lucene-solr-test.sh

ENV LUCENE_SOLR_HOME ${WORKDIR}

# Clone the repo where the Lucen-Solr code and tests reside.
ENV LUCENE_SOLR_TAG=$LUCENE_SOLR_TAG
RUN git clone https://github.com/apache/lucene-solr.git
WORKDIR /lucene-solr/
RUN git checkout ${LUCENE_SOLR_TAG}
WORKDIR /

ENTRYPOINT ["/bin/bash", "/lucene-solr-test.sh"]