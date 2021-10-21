#!/usr/bin/env bash
usage ()
{
	echo '                [--USE_TESTENV_PROPERTIES ]: use testenv.properties or not'
    echo '                [--MAKE ]: make command'
    echo '                [--JDK_VERSION ]: JDK Version'
    echo '                [--JDK_IMPL ]: JDK implementation'

}
if [ `uname` = AIX ] || [ `uname` = SunOS ] || [ `uname` = *BSD ]; then MAKE=gmake; else MAKE=make; fi
if [ $USE_TESTENV_PROPERTIES == true ];then
    while read line; do
        export $line
    done < ./testenv/testenv.properties
    if [ $JDK_VERSION == "8" ];then
        if [ $JDK_IMPL == "openj9" ] 
        then
            export JDK_REPO = $JDK8_OPENJ9_REPO
            export JDK_BRANCH = $JDK8_OPENJ9_BRANCH
           
        else
            export JDK_REPO = $JDK8_REPO
            export JDK_BRANCH = $JDK8_BRANCH
        fi
    fi

fi
cd ./TKG;
$MAKE compile