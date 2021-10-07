#!/usr/bin/env bash
if [ $USE_TESTENV_PROPERTIES == true ];then
    while read line; do
        export $line
    done < testenv.properties
    make compile
fi