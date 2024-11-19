#!/usr/bin/env bash
usage() {
    echo '                Please export USE_TESTENV_PROPERTIES, JDK_VERSION and JDK_IMPL before running the script locally.'

}
if [ $(uname) = AIX ] || [ $(uname) = SunOS ] || [ $(uname) = *BSD ]; then
    MAKE=gmake
else
    MAKE=make
fi

source ./scripts/testenv/testenvSettings.sh

cd ./TKG
$MAKE compile

if [ $(uname) = SunOS ]; then
    if [ -f ../openjdk/openjdk-jdk/jdk/test/sun/management/jmxremote/bootstrap/SSLConfigFilePermissionTest.sh ]; then
      chmod -R 744 ../openjdk/openjdk-jdk/jdk/test/sun/management/jmxremote/bootstrap/SSLConfigFilePermissionTest.sh
    fi
fi
