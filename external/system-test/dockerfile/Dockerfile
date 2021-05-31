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
# Build system test: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version > -t adoptopenjdk-system-test .`


ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=nightly

FROM $IMAGE_NAME:$IMAGE_VERSION

# Install test dependent executable files
RUN apt-get update \
	&& apt-get -y install \
	ant \
	ant-contrib \
	apt-transport-https \
	ca-certificates \
	dirmngr \
	git \
	make \
	unzip \
	wget \
	cpio \
	gcc

# This is the main script to run system tests.
COPY ./dockerfile/system-test.sh /system-test.sh

# Clone the repo where the 3rd party application code and tests reside.
WORKDIR /

# Clone adoptium aqa-tests repo
RUN git clone https://github.com/adoptium/aqa-tests.git

ENTRYPOINT ["/bin/bash", "/system-test.sh"]
CMD ["_sanity.system"]
