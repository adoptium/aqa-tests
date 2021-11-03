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
    while read line; do
        export $line
    done <./testenv/testenv.properties
    if [ $JDK_IMPL == "openj9" ]; then
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
    echo "Set values based on ./testenv/testenv.properties:"
    cat ./testenv/testenv.properties
    echo ""
    echo "JDK_REPO=${JDK_REPO}"
    echo "JDK_BRANCH=${JDK_BRANCH}"

fi
cd ./TKG
$MAKE compile
