#!/usr/bin/env bash


SUCCESS=0
ERROR=1

exists() {
    local specjbb_dir=$1
    return $(test -f "${specjbb_dir}/${SPECJBB_JAR}")
}

isRunnable() {
    local specjbb_dir=$1
    result=$(${JAVA_BIN} -jar ${specjbb_dir}/${SPECJBB_JAR} -v | grep -q "SPECjbb2015") > /dev/null 2>&1
    return $result
}

dirIsValid(){
    local specjbb_dir=$1

    if exists $specjbb_dir; then
        if isRunnable $specjbb_dir; then
            return $SUCCESS
        fi
    fi

    return $ERROR
}

dstIsValid(){
    echo "##### Validating destination suite"
    return $(dirIsValid $SPECJBB_DEST)
}

srcIsValid(){
    echo "##### Validating source suite"
    return $(dirIsValid $SPECJBB_SRC)
}

syncSrcAndDst(){
    echo "##### Syncing destination with source"
    rsync -a $SPECJBB_SRC/ $SPECJBB_DEST
}

succeed(){
    echo "##### SUCCESS"
    exit $SUCCESS
}

fail(){
    echo "##### FAIL"
    exit $ERROR
}

main(){
    if dstIsValid; then
        succeed
    fi

    echo "##### Destination suite is not valid"

    if srcIsValid; then
        syncSrcAndDst  
        if dstIsValid; then
            succeed
        fi
    else
        echo "##### Source suite is not valid"
    fi    

    fail
}

printInput(){
    echo "++ SPECJBB_SRC ${SPECJBB_SRC}"
    echo "++ SPECJBB_DEST ${SPECJBB_DEST}"
    echo "++ SPECJBB_JAR ${SPECJBB_JAR}"
    echo "++ JAVA_BIN ${JAVA_BIN}"
}

printInput
main

