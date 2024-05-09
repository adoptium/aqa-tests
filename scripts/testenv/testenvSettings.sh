#!/usr/bin/env bash
set +x
set -e

if [ $USE_TESTENV_PROPERTIES = true ]; then
    testenv_file="./testenv/testenv.properties"
    case "$PLATFORM" in
        *zos*)
            testenv_file="./testenv/testenv_zos.properties"
            ;;
        *)
            ;;
    esac
    case "$PLATFORM" in
        *arm*)
            if [ "$JDK_VERSION" = "8" ]; then
                testenv_file="./testenv/testenv_arm32.properties"
            fi
            ;;
        *)
            ;;
    esac
    while read line; do
        export $line
    done <$testenv_file
    if [ $JDK_IMPL = "openj9" ] || [ $JDK_IMPL = "ibm" ]; then
        repo=JDK${JDK_VERSION}_OPENJ9_REPO
        branch=JDK${JDK_VERSION}_OPENJ9_BRANCH

        openjceplus_repo=JDK${JDK_VERSION}_OPENJCEPLUS_GIT_REPO
        openjceplus_branch=JDK${JDK_VERSION}_OPENJCEPLUS_GIT_BRANCH
        eval openjceplus_repo2='$'$openjceplus_repo
        eval openjceplus_branch2='$'$openjceplus_branch
        export OPENJCEPLUS_GIT_REPO=$openjceplus_repo2
        export OPENJCEPLUS_GIT_BRANCH=$openjceplus_branch2

    else
        repo=JDK${JDK_VERSION}_REPO
        branch=JDK${JDK_VERSION}_BRANCH
    fi

    eval repo2='$'$repo
    eval branch2='$'$branch

    export JDK_REPO=$repo2
    export JDK_BRANCH=$branch2
    echo "Set values based on ${testenv_file}:"
    echo "========="
    cat $testenv_file
    echo ""
    echo "========="
    echo ""
    echo "JDK_REPO=${JDK_REPO}"
    echo "JDK_BRANCH=${JDK_BRANCH}"
    echo "OPENJCEPLUS_GIT_REPO=${OPENJCEPLUS_GIT_REPO}"
    echo "OPENJCEPLUS_GIT_BRANCH=${OPENJCEPLUS_GIT_BRANCH}"

fi
