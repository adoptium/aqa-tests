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

# This Dockerfile in external/wycheproof/dockerfile dir is used to create an image with
# AdoptOpenJDK jdk binary installed. Basic test dependent executions
# are installed during the building process.
#
# Build example: `docker build -t adoptopenjdk-wycheproof -f Dockerfile ../.`
#
# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest.
# If you want to build image based on other images, please use
# `--build-arg list` to specify your base image
#
# Build example: `docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version> -t adoptopenjdk-wycheproof .`

ARG SDK=openjdk8
ARG IMAGE_NAME=adoptopenjdk/$SDK
ARG IMAGE_VERSION=nightly

FROM $IMAGE_NAME:$IMAGE_VERSION

ARG WYCHEPROOF_TAG=master
ARG BAZEL_VERSION=1.2.1

# Install test dependent executable files
RUN apt-get update; \
    apt-get install -y --no-install-recommends \
        git \
        wget \
        unzip \
        g++ \
        zip \
    ; \
    rm -rf /var/lib/apt/lists/*


ENV BAZEL_VERSION=$BAZEL_VERSION
ENV BAZEL_HOME=/opt/bazel

# Install BAZEL
RUN wget --no-check-certificate --no-cookies https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh \
    && wget --no-check-certificate --no-cookies https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh.sha256 \
    && echo "$(cat bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh.sha256)" | sha256sum -c \
    && chmod +x bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh \
    && ./bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh --prefix=${BAZEL_HOME} \
    && rm -f bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh \
    && rm -f bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh.sha256

# Add BAZEL to PATH
ENV PATH ${PATH}:${BAZEL_HOME}/bin

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY ./dockerfile/wycheproof.sh /wycheproof.sh

ENV WYCHEPROOF_HOME $WORKDIR
RUN mkdir testResults

# Clone Wycheproof
ENV WYCHEPROOF_TAG=$WYCHEPROOF_TAG
RUN git clone https://github.com/google/wycheproof.git
WORKDIR /wycheproof/
RUN git checkout ${WYCHEPROOF_TAG}
WORKDIR /

ENTRYPOINT ["/bin/bash", "/wycheproof.sh"]