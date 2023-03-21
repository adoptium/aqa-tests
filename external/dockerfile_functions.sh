#!/usr/bin/env bash
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
#

source $(dirname "$0")/common_functions.sh

# Generate the common license and copyright header
print_legal() {
    local file=$1

    echo -e "# ------------------------------------------------------------------------------" \
          "\n#               NOTE: THIS DOCKERFILE IS GENERATED VIA \"build_image.sh\" or \"build_all.sh\"" \
          "\n#" \
          "\n#" \
          "\n#                       PLEASE DO NOT EDIT IT DIRECTLY." \
          "\n# ------------------------------------------------------------------------------" \
          "\n#" \
          "\n# Licensed under the Apache License, Version 2.0 (the \"License\");" \
          "\n# you may not use this file except in compliance with the License." \
          "\n# You may obtain a copy of the License at" \
          "\n#" \
          "\n#      https://www.apache.org/licenses/LICENSE-2.0" \
          "\n#" \
          "\n# Unless required by applicable law or agreed to in writing, software" \
          "\n# distributed under the License is distributed on an \"AS IS\" BASIS," \
          "\n# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied." \
          "\n# See the License for the specific language governing permissions and" \
          "\n# limitations under the License." \
          "\n" > ${file}
}

# Generate the Adopt Test header
print_adopt_test() {
    local file=$1
    local test=$2

    echo -e "# This Dockerfile in external/${test}/dockerfile dir is used to create an image with" \
          "\n# Adoptium jdk binary installed. Basic test dependent executions" \
          "\n# are installed during the building process." \
          "\n#" \
          "\n# Build example: \`docker build -t adoptopenjdk-${test}-test -f ${file} .\`" \
          "\n#" \
          "\n# This Dockerfile builds image based on adoptopenjdk/openjdk8:latest." \
          "\n# If you want to build image based on other images, please use" \
          "\n# \`--build-arg list\` to specify your base image" \
          "\n#" \
          "\n# Build example: \`docker build --build-arg IMAGE_NAME=<image_name> --build-arg IMAGE_VERSION=<image_version> -t adoptopenjdk-${test}-test .\`" \
          "\n" >> ${file}
}

sanitize_test_names() {
    local test=$1

    mp_tck=$(echo ${test} | awk -F'-' '{print $2"-"$3}')

    if [[ "${mp_tck}" == "mp-tck" ]]; then
        echo "$(echo ${test} | awk -F'-' '{print $1}')"
    elif [[ "${mp_tck}" == "solr-" || "${mp_tck}" == "test-" ]]; then
        echo "$(echo ${test} | sed 's/-/_/g')"
    else
        echo "${test}"
    fi
}

print_image_args() {
    local file=$1
    local os=$2
    local version=$3
    local vm=$4
    local package=$5
    local build=$6

    image_name="eclipse-temurin"
    tag=""
    if [[ "${package}" == "jre" ]]; then
        tag="${version}-jre"
    else 
        tag="${version}-jdk"
    fi
    if [[ "${vm}" == "openj9" ]]; then
        if  [[ "${os}" == "ubuntu" ]]; then
            image_name="docker.io/ibm-semeru-runtimes"
            tag=open-${tag}
        else
            # os is ubi
            image_name="registry.access.redhat.com/ubi8/ubi"
            tag="8.6"
        fi
    fi
    image="${image_name}:${tag}"

    echo -e "ARG IMAGE=${image}" \
          "\nARG OS=${os}" \
          "\nARG IMAGE_VERSION=nightly" \
          "\nARG TAG=${tag}" \
          "\nFROM \$IMAGE\n" >> ${file}
}

print_test_tag_arg() {
    local file=$1
    local test=$2
    local tag=$3

    # Cause Test name to be capitalized
    test="$(sanitize_test_names ${test} | tr a-z A-Z)_TAG"

    echo -e "ARG ${test}=${tag}\n" >> ${file}
}


# Select the ubuntu OS packages
print_ubuntu_pkg() {
    local file=$1
    local packages=$2

    echo -e "RUN apt-get update \\" \
            "\n\t&& apt-get install -qq -y --no-install-recommends software-properties-common \\" \
            "\n\t&& apt-get install -qq -y --no-install-recommends gnupg \\" \
            "\n\t&& add-apt-repository ppa:ubuntu-toolchain-r/test \\" \
            "\n\t&& apt-get update \\" \
            "\n\t&& apt-get install -y --no-install-recommends ${packages} \\" \
            "\n\t&& rm -rf /var/lib/apt/lists/*" \
            "\n" >> ${file}
}

# Select the ubuntu OS packages
print_ubi_pkg() {
    local file=$1
    local packages=$2

    echo -e "RUN dnf install -y ${packages} \\" \
            "\n\t&& dnf clean all " >> ${file}
    echo -e "\nENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'" >> ${file}
}

# Install JDK
print_jdk_install() {
    local file=$1
    local os=$2
    local platform=$3

    # JDK install is only needed for ubi image, and the installed jdk requires criu support, and will be replaced by mounted jdk during test
    echo -e "\nRUN set -eux; \\" \
            "\n\t case \"${platform}\" in \\" \
            "\n\t  *x86-64*) \\" \
            "\n\t     BUILD_ID_LAST_SUCCESS=\$(wget -qO- https://openj9-jenkins.osuosl.org/job/Build_JDK11_x86-64_linux_criu_Nightly/lastSuccessfulBuild/buildNumber); \\" \
            "\n\t     BINARY_URL=\$(wget -qO- https://openj9-jenkins.osuosl.org/job/Build_JDK11_x86-64_linux_criu_Nightly/lastSuccessfulBuild/consoleText | grep -Po \"(?<=Deploying artifact: )https://openj9-artifactory.osuosl.org/artifactory/ci-openj9/Build_JDK11_x86-64_linux_criu_Nightly/\${BUILD_ID_LAST_SUCCESS}/OpenJ9-JDK11-x86-64_linux_criu.*tar.gz\"); \\" \
            "\n\t     ;; \\" \
            "\n\t  *) \\" \
            "\n\t     echo \"Unsupported platform \"; \\" \
            "\n\t     exit 1; \\" \
            "\n\t     ;; \\" \
            "\n\t esac; \\" \
            "\n\t curl -LfsSo /tmp/openjdk.tar.xz \${BINARY_URL}; \\" \
            "\n\t mkdir /tmp/jdk-extract; \\" \
            "\n\t cd /tmp/jdk-extract; \\" \
            "\n\t tar -xf /tmp/openjdk.tar.xz --strip-components=1; \\" \
            "\n\t ./bin/jlink --no-header-files --no-man-pages --compress=2 --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.se,java.security.jgss,java.security.sasl,java.smartcardio,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.accessibility,jdk.attach,jdk.charsets,jdk.compiler,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.dynalink,jdk.editpad,jdk.httpserver,jdk.internal.ed,jdk.internal.jvmstat,jdk.internal.le,jdk.internal.opt,jdk.jartool,jdk.javadoc,jdk.jcmd,jdk.jconsole,jdk.jdeps,jdk.jdi,jdk.jdwp.agent,jdk.jlink,jdk.jshell,jdk.jsobject,jdk.localedata,jdk.management,jdk.management.agent,jdk.naming.dns,jdk.naming.ldap,jdk.naming.rmi,jdk.net,jdk.pack,jdk.rmic,jdk.scripting.nashorn,jdk.scripting.nashorn.shell,jdk.sctp,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom,jdk.zipfs,openj9.criu,openj9.cuda,openj9.dataaccess,openj9.dtfj,openj9.dtfjview,openj9.gpu,openj9.jvm,openj9.sharedclasses,openj9.traceformat,openj9.zosconditionhandling --output /opt/java/openjdk; \\" \
            "\n\t rm -rf /tmp/jdk-extract; \\" \
            "\n\t rm -rf /tmp/openjdk.tar.xz; " \
             "\n" >> ${file}

    echo -e "\nENV JAVA_HOME=/opt/java/openjdk \\" \
            "\n\t PATH=\"/opt/java/openjdk/bin:\$PATH\" " \
            "\n" >> ${file}

    echo -e "\nENV JAVA_TOOL_OPTIONS=\"-XX:+IgnoreUnrecognizedVMOptions -XX:+IdleTuningGcOnIdle\" " \
            "\n" >> ${file}

    echo -e "\nENV RANDFILE=/tmp/.rnd  \\" \
            "\n\t OPENJ9_JAVA_OPTIONS=\"-XX:+IgnoreUnrecognizedVMOptions -XX:+IdleTuningGcOnIdle -Dosgi.checkConfiguration=false\" " \
            "\n" >> ${file}

}

# Install Ant
print_ant_install() {
    local file=$1
    local ant_version=$2
    local os=$3

    echo -e "ARG ANT_VERSION=${ant_version}" \
          "\nENV ANT_VERSION=\$ANT_VERSION" \
          "\nENV ANT_HOME=/opt/ant" \
          "\n\n# Install Ant" >> ${file}

    if [[ "${os}" == *"ubi"* ]]; then
        echo -e "\nRUN cd /tmp \\" \
            "\n\t&& wget --progress=dot:mega -O ant.zip https://archive.apache.org/dist/ant/binaries/apache-ant-\${ANT_VERSION}-bin.zip \\" \
            "\n\t&& unzip -q ant.zip -d /opt \\" \
            "\n\t&& ln -s /opt/apache-ant-\${ANT_VERSION} /opt/ant \\" \
            "\n\t&& ln -s /opt/ant/bin/ant /usr/bin/ant " \
            "\n" >> ${file}
    else
        echo -e "\nRUN wget --no-verbose --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-\${ANT_VERSION}-bin.tar.gz \\" \
            "\n\t&& wget --no-verbose --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-\${ANT_VERSION}-bin.tar.gz.sha512 \\" >> ${file}

        echo -e "\t&& echo \"\$(cat apache-ant-\${ANT_VERSION}-bin.tar.gz.sha512) apache-ant-\${ANT_VERSION}-bin.tar.gz\" | sha512sum -c \\" >> ${file}

        echo -e "\t&& tar -zvxf apache-ant-\${ANT_VERSION}-bin.tar.gz -C /opt/ \\" \
                "\n\t&& ln -s /opt/apache-ant-\${ANT_VERSION} /opt/ant \\" \
                "\n\t&& rm -f apache-ant-\${ANT_VERSION}-bin.tar.gz \\" \
                "\n\t&& rm -f apache-ant-\${ANT_VERSION}-bin.tar.gz.sha512" \
                "\n\n# Add Ant to PATH" \
                "\nENV PATH \${PATH}:\${ANT_HOME}/bin" \
                "\n" >> ${file}
    fi
}

# Install Ant Contrib
print_ant_contrib_install() {
    local file=$1
    local ant_contrib_version=$2

    echo -e "ARG ANT_CONTRIB_VERSION=${ant_contrib_version}" \
        "\nENV ANT_CONTRIB_VERSION=\$ANT_CONTRIB_VERSION" \
        "\n\n# Install Ant Contrib" \
        "\nRUN wget --no-verbose --no-check-certificate --no-cookies https://sourceforge.net/projects/ant-contrib/files/ant-contrib/\${ANT_CONTRIB_VERSION}/ant-contrib-\${ANT_CONTRIB_VERSION}-bin.tar.gz \\" \
        "\n\t&& wget --no-verbose --no-check-certificate --no-cookies https://sourceforge.net/projects/ant-contrib/files/ant-contrib/\${ANT_CONTRIB_VERSION}/ant-contrib-\${ANT_CONTRIB_VERSION}-bin.tar.gz.md5 \\" >> ${file}

    echo -e "\t&& echo \"\$(cat ant-contrib-\${ANT_CONTRIB_VERSION}-bin.tar.gz.md5) ant-contrib-\${ANT_CONTRIB_VERSION}-bin.tar.gz\" | md5sum -c \\" >> ${file}

    echo -e "\t&& tar -zvxf ant-contrib-\${ANT_CONTRIB_VERSION}-bin.tar.gz -C /tmp/ \\" \
            "\n\t&& mv /tmp/ant-contrib/ant-contrib-\${ANT_CONTRIB_VERSION}.jar \${ANT_HOME}/lib/ant-contrib.jar \\" \
            "\n\t&& rm -rf /tmp/ant-contrib \\" \
            "\n\t&& rm -f ant-contrib-\${ANT_CONTRIB_VERSION}-bin.tar.gz \\" \
            "\n\t&& rm -f ant-contrib-\${ANT_CONTRIB_VERSION}-bin.tar.gz.md5" \
            "\n" >> ${file}
}

# Install SBT
print_sbt_install() {
    local file=$1
    local sbt_version=$2

    echo -e "ARG SBT_VERSION=${sbt_version}" \
          "\nENV SBT_VERSION=\$SBT_VERSION" \
          "\nENV SBT_HOME=/opt/sbt" \
          "\n\n# Install SBT" \
          "\nRUN wget --no-verbose --no-check-certificate --no-cookies https://github.com/sbt/sbt/releases/download/v\${SBT_VERSION}/sbt-\${SBT_VERSION}.tgz \\" \
          "\n\t&& wget --no-verbose --no-check-certificate --no-cookies https://github.com/sbt/sbt/releases/download/v\${SBT_VERSION}/sbt-\${SBT_VERSION}.tgz.asc \\" \
          "\n\t&& curl \"https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823\" | gpg --import \\" \
          "\n\t&& gpg --verify sbt-\${SBT_VERSION}.tgz.asc sbt-\${SBT_VERSION}.tgz \\" \
          "\n\t&& tar -zvxf sbt-\${SBT_VERSION}.tgz -C /opt/ \\" \
          "\n\t&& rm -f sbt-\${SBT_VERSION}.tgz \\" \
          "\n\t&& rm -f sbt-\${SBT_VERSION}.tgz.asc" \
          "\n\n# Add SBT to PATH" \
          "\nENV PATH \${PATH}:\${SBT_HOME}/bin" \
          "\n" >> ${file}
}

# Install Ivy
print_ivy_install() {
    local file=$1
    local ivy_version=$2

    echo -e "ARG IVY_VERSION=${ivy_version}" \
          "\nENV IVY_VERSION=\$IVY_VERSION" \
          "\n\n# Install Ivy" \
          "\nRUN wget --no-verbose --no-check-certificate --no-cookies https://archive.apache.org/dist/ant/ivy/\${IVY_VERSION}/apache-ivy-\${IVY_VERSION}-bin.tar.gz \\" \
          "\n\t&& wget --no-verbose --no-check-certificate --no-cookies https://archive.apache.org/dist/ant/ivy/\${IVY_VERSION}/apache-ivy-\${IVY_VERSION}-bin.tar.gz.sha512 \\" >> ${file}

    echo -e "\t&& echo \"\$(cat apache-ivy-\${IVY_VERSION}-bin.tar.gz.sha512) apache-ivy-\${IVY_VERSION}-bin.tar.gz\" | sha512sum -c \\" >> ${file}

    echo -e "\t&& tar -zvxf apache-ivy-\${IVY_VERSION}-bin.tar.gz apache-ivy-\${IVY_VERSION}/ivy-\${IVY_VERSION}.jar -C \${ANT_HOME}/lib/ \\" \
            "\n\t&& rm -f apache-ivy-\${IVY_VERSION}-bin.tar.gz \\" \
            "\n\t&& rm -f apache-ivy-\${IVY_VERSION}-bin.tar.gz.sha512" \
            "\n" >> ${file}
}

# Install OpenSSL
print_openssl_install() {
    local file=$1
    local openssl_version=$2

    echo -e "ARG OPENSSL_VERSION=${openssl_version}" \
          "\nENV OPENSSL_VERSION=\$OPENSSL_VERSION" \
          "\nENV OPENSSL_HOME /opt/openssl" \
          "\n\n# Install OpenSSL" \
          "\nRUN  wget --no-verbose --no-check-certificate --no-cookies https://www.openssl.org/source/openssl-\${OPENSSL_VERSION}.tar.gz \\" \
          "\n\t&& wget --no-verbose --no-check-certificate --no-cookies https://www.openssl.org/source/openssl-\${OPENSSL_VERSION}.tar.gz.sha256 \\" >> ${file}

    echo -e "\t&& echo \"\$(cat openssl-\${OPENSSL_VERSION}.tar.gz.sha256) openssl-\${OPENSSL_VERSION}.tar.gz\" | sha256sum -c \\" >> ${file}
    echo -e "\t&& tar -zvxf openssl-\${OPENSSL_VERSION}.tar.gz -C /opt/ \\" \
            "\n\t&& ln -s /opt/openssl-\${OPENSSL_VERSION} /opt/openssl \\" \
            "\n\t&& rm -f openssl-\${OPENSSL_VERSION}.tar.gz \\" \
            "\n\t&& rm -f openssl-\${OPENSSL_VERSION}.tar.gz.sha256 \\" \
            "\n\t&& cd \${OPENSSL_HOME} \\" \
            "\n\t&& ./config -Wl,--enable-new-dtags,-rpath,'\$(LIBRPATH)' \\" \
            "\n\t&& make \\" \
            "\n\t&& make install" \
            "\n" >> ${file}
}

# Install Bazel
print_bazel_install() {
    local file=$1
    local bazel_version=$2

    echo -e "ARG BAZEL_VERSION=${bazel_version}" \
          "\nENV BAZEL_VERSION=\$BAZEL_VERSION" \
          "\nENV BAZEL_HOME /opt/bazel" \
          "\n\n# Install Bazel" \
          "\nRUN  wget --no-verbose --no-check-certificate --no-cookies https://github.com/bazelbuild/bazel/releases/download/\${BAZEL_VERSION}/bazel-\${BAZEL_VERSION}-installer-linux-x86_64.sh \\" \
          "\n\t&&  wget --no-verbose --no-check-certificate --no-cookies https://github.com/bazelbuild/bazel/releases/download/\${BAZEL_VERSION}/bazel-\${BAZEL_VERSION}-installer-linux-x86_64.sh.sha256 \\" >> ${file}

    echo -e "\t&& echo \"\$(cat bazel-\${BAZEL_VERSION}-installer-linux-x86_64.sh.sha256)\" | sha256sum -c \\" >> ${file}

    echo -e "\t&& chmod +x bazel-\${BAZEL_VERSION}-installer-linux-x86_64.sh \\" \
            "\n\t&& ./bazel-\${BAZEL_VERSION}-installer-linux-x86_64.sh --prefix=\${BAZEL_HOME} \\" \
            "\n\t&& rm -f bazel-\${BAZEL_VERSION}-installer-linux-x86_64.sh \\" \
            "\n\t&& rm -f bazel-\${BAZEL_VERSION}-installer-linux-x86_64.sh.sha256" \
            "\n\n# Add Bazel to PATH" \
            "\nENV PATH \${PATH}:\${BAZEL_HOME}/bin" \
            "\n" >> ${file}
}

print_python_install() {
    local file=$1
    local python_version=$2

    echo -e "ARG PYTHON_VERSION=${python_version}" \
          "\nENV PYTHON_VERSION=\$PYTHON_VERSION" \
          "\n\n# Install python" \
          "\nRUN wget --progress=dot:mega -O python.tar.xz https://www.python.org/ftp/python/\${PYTHON_VERSION}/Python-\${PYTHON_VERSION}.tar.xz \\" >> ${file}
    
    echo -e "\t&& tar -xJf python.tar.xz \\" \
            "\n\t&& cd Python-\${PYTHON_VERSION}  \\" \
            "\n\t&& ./configure --prefix=/usr/local \\" \
            "\n\t&& make \\" \
            "\n\t&& make install \\" \
            "\n\t&& cd .. \\" \
            "\n\t&& rm -rf python.tar.xz Python-\${PYTHON_VERSION}" \
            "\n" >> ${file}
}

print_criu_install() {
    local file=$1
    local criu_version=$2
    local os=$3
    local platform=$4

    if [[ "${os}" == *"ubi"* ]]; then
        if [[ "${platform}" == *"x86-64"* ]]; then
            echo -e "\nRUN wget -O /usr/sbin/criu https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/criu-build/b6/criu \\" \
                    "\n\t&& wget -O /usr/lib64/libcriu.so.2.0 https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/criu-build/b6/libcriu.so.2.0 " \
                    "\n" >> ${file}

            echo -e "\nRUN chmod a+x /usr/sbin/criu \\" \
                    "\n\t&& setcap cap_checkpoint_restore,cap_sys_ptrace,cap_setpcap=eip /usr/sbin/criu \\" \
                    "\n\t&& export GLIBC_TUNABLES=glibc.pthread.rseq=0:glibc.cpu.hwcaps=-XSAVEC,-XSAVE,-AVX2,-ERMS,-AVX,-AVX_Fast_Unaligned_Load \\" \
                    "\n\t&& cd /usr/lib64 \\" \
                    "\n\t&& ln -s libcriu.so.2.0 libcriu.so \\" \
                    "\n\t&& ln -s libcriu.so.2.0 libcriu.so.2 \\" \
                    "\n\t&& cd / "  >> ${file}
        else
            echo "Criu binaries are not available for Platform ${platform}!"
            exit 1
        fi
    else # for ubuntu
        # Method 1: Install from package repo
        # echo -e "\n# Install criu and set capabilities" \
        #         "\nRUN add-apt-repository ppa:criu/ppa \\" \
        #         "\n\t&& apt-get update \\" \
        #         "\n\t&& apt-get install -y --no-install-recommends criu \\" \
        #         "\n\t&& criu -V \\" \
        #         "\n\t&& setcap cap_chown,cap_dac_override,cap_dac_read_search,cap_fowner,cap_fsetid,cap_kill,cap_setgid,cap_setuid,cap_setpcap,cap_net_admin,cap_sys_chroot,cap_sys_ptrace,cap_sys_admin,cap_sys_resource,cap_sys_time,cap_audit_control=eip /usr/sbin/criu" \
        #         "\n\t&& export GLIBC_TUNABLES=glibc.pthread.rseq=0:glibc.cpu.hwcaps=-XSAVEC,-XSAVE,-AVX2,-ERMS,-AVX,-AVX_Fast_Unaligned_Load" \
        #         "\n" >> ${file}

        # Method 2: build from source code
        echo -e "\n# Install dependent packages for criu" \
                "\nRUN apt-get update \\" \
                "\n\t&& apt-get install -y --no-install-recommends iptables libbsd-dev libcap-dev libdrm-dev libnet1-dev libgnutls28-dev libgnutls30 libnftables-dev libnl-3-dev libprotobuf-dev python3-distutils protobuf-c-compiler protobuf-compiler xmlto libssl-dev python3-future libxt-dev libfontconfig1-dev python3-protobuf nftables libcups2-dev libasound2-dev libxtst-dev libexpat1-dev libfontconfig libaio-dev libffi-dev libx11-dev libprotobuf-c-dev libnuma-dev libfreetype6-dev libxrandr-dev libxrender-dev libelf-dev libxext-dev libdwarf-dev" \
                "\n" >> ${file}

        echo -e "\n# Build criu and set capabilities" \
                "\nRUN mkdir -p /tmp \\" \
                "\n\t&& cd /tmp \\" \
                "\n\t&& git clone https://github.com/ibmruntimes/criu.git \\" \
                "\n\t&& cd criu \\" \
                "\n\t&& git fetch origin \\" \
                "\n\t&& git reset --hard origin/march_ea_23 \\" \
                "\n\t&& make PREFIX=/usr install \\" \
                "\n\t&& criu -V \\" \
                "\n\t&& setcap cap_checkpoint_restore,cap_chown,cap_dac_override,cap_dac_read_search,cap_fowner,cap_fsetid,cap_kill,cap_setgid,cap_setuid,cap_setpcap,cap_net_admin,cap_sys_chroot,cap_sys_ptrace,cap_sys_admin,cap_sys_resource,cap_sys_time,cap_audit_control=eip /usr/sbin/criu \\" \
                "\n\t&& export GLIBC_TUNABLES=glibc.pthread.rseq=0:glibc.cpu.hwcaps=-XSAVEC,-XSAVE,-AVX2,-ERMS,-AVX,-AVX_Fast_Unaligned_Load" \
                "\n" >> ${file}
    fi
}


print_maven_install() {
    local file=$1
    local maven_version=$2

    echo -e "ARG MAVEN_VERSION=${maven_version}" \
          "\nENV MAVEN_VERSION=\$MAVEN_VERSION" \
          "\nENV MAVEN_HOME /opt/maven" \
          "\n\n# Install Maven" \
          "\nRUN  wget --no-verbose --no-check-certificate --no-cookies https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/\${MAVEN_VERSION}/apache-maven-\${MAVEN_VERSION}-bin.tar.gz \\" >> ${file}
    
    echo -e "\t&& tar -zvxf apache-maven-\${MAVEN_VERSION}-bin.tar.gz -C /opt/ \\" \
            "\n\t&& ln -s /opt/apache-maven-\${MAVEN_VERSION} /opt/maven \\" \
            "\n\t&& rm -f apache-maven-\${MAVEN_VERSION}-bin.tar.gz" \
            "\nENV PATH \${PATH}:\${MAVEN_HOME}/bin" \
            "\n" >> ${file}
}
# Prints Java Tool Options
print_java_tool_options() {
    local file=$1

    echo -e "ENV JAVA_TOOL_OPTIONS=\"-Dfile.encoding=UTF8 -Djava.security.egd=file:/dev/./urandom\"\n" >> ${file}
}

print_environment_variable() {
    local file=$1
    local environment_variable=$2

    echo -e "ENV ${environment_variable}\n" >> ${file}
}

print_home_path() {
    local file=$1
    local github_url=$2

    # Get Github folder name
    local folder="$(echo ${github_url} | awk -F'/' '{print $NF}' | sed 's/.git//g')"
    echo -e "ENV TEST_HOME /${folder}\n" >> ${file}
}

print_test_results() {
    local file=$1

    echo -e "RUN mkdir testResults\n" >> ${file}
}

print_test_files() {
    local file=$1
    local test=$2
    local localPropertyFile=$3

    if [[ ${check_external_custom_test} -eq 1 ]]; then 
        echo -e "# This is the main script to run ${test} tests" \
                "\nCOPY external_custom/test.sh /test.sh" \
                "\nCOPY test_base_functions.sh test_base_functions.sh\n" >> ${file}
    else
        echo -e "# This is the main script to run ${test} tests" \
                "\nCOPY ${test}/test.sh /test.sh" \
                "\nCOPY test_base_functions.sh test_base_functions.sh\n" >> ${file}
    fi
    if [[ "${test}" == *"portable"* ]]; then
        echo -e "# This is the script used to restore portable test later." \
            "\nCOPY ${test}/test_restore.sh /test_restore.sh" >> ${file}
    fi
    if [[ ! -z ${localPropertyFile} ]]; then
        echo -e "# This local property file is needed to set up user preferred properties." \
            "\nCOPY ${test}/${localPropertyFile} \${TEST_HOME}/${localPropertyFile}\n" >> ${file}
    fi
}

print_testInfo_env() {
    local test=$1
    local test_tag=$2
    local OS=$3
    local version=$4
    local vm=$5
    echo -e "ENV APPLICATION_NAME=${test}" \
            "\nENV APPLICATION_TAG=${test_tag}" \
            "\nENV OS_TAG=${OS}" \
            "\nENV JDK_VERSION=${version}" \
            "\nENV JDK_IMPL=${vm}" \
            "\n" >> ${file}
}

print_clone_project() {
    local file=$1
    local test=$2
    local github_url=$3

    # Cause Test name to be capitalized
    test_tag="$(sanitize_test_names ${test} | tr a-z A-Z)_TAG"
    git_branch_tag="master"
    if [[ "$test_tag" != *"PORTABLE"* ]]; then
        git_branch_tag=$test_tag
    fi

    # Get Github folder name
    folder="$(echo ${github_url} | awk -F'/' '{print $NF}' | sed 's/.git//g')"

    echo -e "# Clone ${test} source" \
            "\nENV ${test_tag}=\$${test_tag}" \
            "\nRUN git clone ${github_url}" \
            "\nWORKDIR /${folder}/" \
            "\nRUN git checkout \$${git_branch_tag}" \
            "\nWORKDIR /" \
            "\n" >> ${file}
}

print_external_custom_parameters(){
    local file=$1

    echo -e "ARG EXTERNAL_CUSTOM_PARAMETERS" \
            "\nENV EXTERNAL_CUSTOM_REPO ${EXTERNAL_CUSTOM_REPO}" \
            "\nENV EXTERNAL_TEST_CMD ${EXTERNAL_TEST_CMD}" \
            "\nENV EXTERNAL_REPO_BRANCH ${EXTERNAL_REPO_BRANCH}" \
            "\n" >> ${file}

}

print_entrypoint() {
    local file=$1
    echo -e "ENTRYPOINT [\"/bin/bash\", \"/test.sh\"]" >> ${file}
}

print_workdir() {
    local file=$1
    echo -e "\nWORKDIR \${TEST_HOME}\n" >> ${file}
}

print_cmd() {
    local file=$1
    local cmd=$2

    echo -e "\nCMD [\"${cmd}\"]" >> ${file}
}

remove_trailing_spaces() {
    local file=$1

    # Make the Dockerfile after we set the base image
    if [[ "$(uname)" == 'Darwin' ]]; then
        sed -i '' 's/[[:space:]]*$//g' ${file}
    else
        sed -i 's/[[:space:]]*$//g' ${file}
    fi
}

# Generate the dockerfile for a given build
generate_dockerfile() {
    file=$1
    test=$2
    version=$3
    vm=$4
    os=$5
    package=$6
    build=$7
    platform=$8
    check_external_custom_test=$9


    if [[ ${check_external_custom_test} -eq 1 ]]; then
        tag_version=${EXTERNAL_REPO_BRANCH}
    fi

    if [[ ${check_external_custom_test} -eq 1 ]]; then
        set_external_custom_test_info ${test} ${check_external_custom_test}
    else
        set_test_info ${test} ${check_external_custom_test}
    fi
    packages=$(echo ${os}_packages | sed 's/-/_/')
    jhome="/opt/java/openjdk"

    mkdir -p `dirname ${file}` 2>/dev/null
    echo
    echo -n "Writing ${file} ... "
    print_legal ${file};
    print_adopt_test ${file} ${test};
    print_image_args ${file} ${os} ${version} ${vm} ${package} ${build};
    print_test_tag_arg ${file} ${test} ${tag_version};
    print_${os}_pkg ${file} "${!packages}";

    if [[ ! -z ${ant_version} ]]; then
        print_ant_install ${file} ${ant_version} ${os};
    fi

    if [[ ! -z ${ant_contrib_version} ]]; then
        print_ant_contrib_install ${file} ${ant_contrib_version};
    fi

    if [[ ! -z ${ivy_version} ]]; then
        print_ivy_install ${file} ${ivy_version};
    fi

    if [[ ! -z ${sbt_version} ]]; then
        print_sbt_install ${file} ${sbt_version};
    fi

    if [[ ! -z ${openssl_version} ]]; then
        print_openssl_install ${file} ${openssl_version};
    fi

    if [[ ! -z ${bazel_version} ]]; then
        print_bazel_install ${file} ${bazel_version};
    fi

    if [[ ! -z ${python_version} ]]; then
        print_python_install ${file} ${python_version};
    fi

    if [[ ! -z ${criu_version} ]]; then
        print_criu_install ${file} ${criu_version} ${os} ${platform};
    fi

    if [[ ! -z ${jdk_install} ]]; then
        print_jdk_install ${file} ${os} ${platform};
    fi
    
    if [[ ! -z ${maven_version} ]]; then
        print_maven_install ${file} ${maven_version};
    fi
    print_java_tool_options ${file};

    if [[ ! -z ${environment_variable} ]]; then
        print_environment_variable ${file} ${environment_variable};
    fi

    if [[ ! -z ${test_results} ]]; then
        print_test_results ${file};
    fi

    print_home_path ${file} ${github_url};
    print_testInfo_env ${test} ${tag_version} ${os} ${version} ${vm}
    print_clone_project ${file} ${test} ${github_url};
    print_test_files ${file} ${test} ${localPropertyFile};

    if [[ ${check_external_custom_test} -eq 1 ]]; then
        print_external_custom_parameters ${file}
    fi
    print_workdir ${file};
    print_entrypoint ${file};

    if [[ ! -z ${test_options} ]]; then
        print_cmd ${file} "${test_options}";
    fi

    remove_trailing_spaces ${file};

    echo "done"
    echo
}
