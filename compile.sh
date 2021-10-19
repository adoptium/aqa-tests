#!/usr/bin/env bash
usage ()
{
	echo '                [--USE_TESTENV_PROPERTIES ]: use testenv.properties or not'
}

if [ $USE_TESTENV_PROPERTIES == true ];then
    while read line; do
        export $line
    done < ./testenv/testenv.properties
fi
cd ./TKG;
make compile