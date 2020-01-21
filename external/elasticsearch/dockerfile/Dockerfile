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

# This Dockerfile in external/elasticsearch/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-elasticsearch-test -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version >-t adoptopenjdk-elasticsearch-test .`
 
ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=nightly
FROM $IMAGE_NAME:$IMAGE_VERSION

RUN groupadd -g 1000 elasticsearch && useradd elasticsearch -u 1000 -g 1000

# Install test dependent executable files
RUN apt-get update \
	&& apt-get -y install \
	git \
	unzip \
	wget

ENV  JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

# This is the main script to run Elasticsarch tests.
COPY ./dockerfile/elasticsearch-test.sh /elasticsearch-test.sh

RUN git clone -q https://github.com/elastic/elasticsearch.git

#default is HEAD
ARG ELASTIC_VERSION=.
WORKDIR elasticsearch
RUN git checkout -q $ELASTIC_VERSION

WORKDIR /
RUN mkdir testResults
RUN chown -R elasticsearch /elasticsearch
RUN chown -R elasticsearch /elasticsearch-test.sh
RUN chown -R elasticsearch /testResults
RUN chmod -R a+x /elasticsearch

USER elasticsearch


#ENTRYPOINT ["/bin/bash", "/example-test.sh"]
ENTRYPOINT ["/bin/bash", "/elasticsearch-test.sh"]
CMD [""]
