#!/usr/bin/env bash
usage ()
{
	echo '                [--USE_TESTENV_PROPERTIES ]: use testenv.properties or not'
}
RESOLVED_MAKE = "if [ `uname` = AIX ] || [ `uname` = SunOS ] || [ `uname` = *BSD ]; then MAKE=gmake; else MAKE=make; fi"

if [ $USE_TESTENV_PROPERTIES == true ];then
    while read line; do
        export $line
    done < ./testenv/testenv.properties
fi
$RESOLVED_MAKE
cd ./TKG;
$MAKE compile