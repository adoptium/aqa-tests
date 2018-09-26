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

# This Dockerfile in thirdparty_containers/elasticsearch/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-elasticsearch-test .`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version >-t adoptopenjdk-elasticsearch-test .`
 
ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=latest

FROM $IMAGE_NAME:$IMAGE_VERSION

RUN groupadd -g 1000 elasticsearch && useradd elasticsearch -u 1000 -g 1000

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
	wget

#Install Gradle
RUN mkdir -p /usr/share/gradle \
         && cd /usr/share/gradle \
         && wget https://services.gradle.org/distributions/gradle-4.5-bin.zip \
         && unzip gradle-4.5-bin.zip \
         && rm -rf gradle-4.5-bin.zip \
         && ln -s /usr/share/gradle/gradle-4.5/bin/gradle /usr/bin/gradle \
         && cd /

VOLUME ["/java"]
ENV JAVA_HOME=/java \
    PATH=/java/bin:$PATH \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

# This is the main script to run Elasticsarch tests.
COPY ./dockerfile/elasticsearch-test.sh /elasticsearch-test.sh

# Clone the Elasticsearch test repo
WORKDIR /
RUN pwd

RUN git clone https://github.com/elastic/elasticsearch.git

RUN cd elasticsearch \
&& git checkout v6.1.3

RUN chown -R elasticsearch /elasticsearch
RUN chown -R elasticsearch /elasticsearch-test.sh
RUN chown -R elasticsearch /usr/share/gradle/gradle-4.5

RUN chmod -R a+x /elasticsearch
RUN chmod -R a+x /usr/share/gradle/gradle-4.5

USER elasticsearch
WORKDIR /
ENV PATH="/usr/share/gradle/gradle-4.5/bin:${PATH}"

#ENTRYPOINT ["/bin/bash", "/example-test.sh"]
ENTRYPOINT ["/bin/bash", "/elasticsearch-test.sh"]
CMD ["--version"]
