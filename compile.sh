#!/usr/bin/env bash
usage ()
{
	echo '                [--USE_TESTENV_PROPERTIES ]: use testenv.properties or not'
    echo '                [--JDK_VERSION ]: JDK Version'
    echo '                [--JDK_IMPL ]: JDK implementation'

}
if [ `uname` = AIX ] || [ `uname` = SunOS ] || [ `uname` = *BSD ]; then MAKE=gmake; else MAKE=make; fi
if [ $USE_TESTENV_PROPERTIES == true ];then
    while read line; do
        export $line
    done < ./testenv/testenv.properties
    openj9_repo="JDK${JDK_VERSION}_OPENJ9_REPO"
    openj9_branch="JDK${JDK_VERSION}_OPENJ9_BRANCH"
    repo="JDK{$JDK_VERSION}_REPO"
    branch="JDK{$JDK_VERSION}_BRANCH"
    if [ $JDK_IMPL == "openj9" ]
    then
        export JDK_REPO=${!openj9_repo}
        export JDK_BRANCH=${!openj9_branch}
        
    else
        export JDK_REPO=${!repo}
        export JDK_BRANCH=${!branch}
    fi

fi
cd ./TKG;
$MAKE compile