#!/usr/bin/env bash
usage() {
    echo '                Please export USE_TESTENV_PROPERTIES, JDK_VERSION and JDK_IMPL before running the script locally.'

}
if [ $(uname) = AIX ] || [ $(uname) = SunOS ] || [ $(uname) = *BSD ]; then
    MAKE=gmake
else
    MAKE=make
fi
if [ $USE_TESTENV_PROPERTIES == true ]; then
    testenv_file="./testenv/testenv.properties"
    if [[ "$PLATFORM" == *"zos"* ]]; then
        testenv_file="./testenv/testenv_zos.properties"
    fi
    if [[ "$PLATFORM" == *"arm"* ]] && [[ "$JDK_VERSION" == "8" ]]; then
        testenv_file="./testenv/testenv_arm32.properties"
    fi
    while read line; do
        export $line
    done <$testenv_file
    if [ $JDK_IMPL == "openj9" ] || [ $JDK_IMPL == "ibm" ]; then
        repo=JDK${JDK_VERSION}_OPENJ9_REPO
        branch=JDK${JDK_VERSION}_OPENJ9_BRANCH
    else
        repo=JDK${JDK_VERSION}_REPO
        branch=JDK${JDK_VERSION}_BRANCH
    fi

    eval repo2='$'$repo
    eval branch2='$'$branch

    export JDK_REPO=$repo2
    export JDK_BRANCH=$branch2
    echo "Set values based on ${testenv_file}:"
    cat $testenv_file
    echo ""
    echo "JDK_REPO=${JDK_REPO}"
    echo "JDK_BRANCH=${JDK_BRANCH}"

fi
cd ./TKG
$MAKE compile
