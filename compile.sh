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
    repo_j9=JDK${JDK_VERSION}_OPENJ9_REPO
    branch_j9=JDK${JDK_VERSION}_OPENJ9_BRANCH
    repo=JDK${JDK_VERSION}_REPO
    branch=JDK${JDK_VERSION}_BRANCH
    eval repo_j9_2='$'$repo_j9
    eval branch_j9_2='$'$branch_j9
    eval repo2='$'$repo
    eval branch2='$'$branch

    if [ $JDK_IMPL == "openj9" ]; then
        export JDK_REPO=$repo_j9_2
        export JDK_BRANCH=$branch_j9_2

    else
        export JDK_REPO=$repo2
        export JDK_BRANCH=$branch2
    fi
    echo "Set values based on ./testenv/testenv.properties:"
    cat ./testenv/testenv.properties
    echo "JDK_REPO=${JDK_REPO}"
    echo
    echo "JDK_BRANCH=${JDK_BRANCH}"

fi
cd ./TKG
$MAKE compile
