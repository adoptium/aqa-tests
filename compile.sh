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
    openj9_repo=JDK${JDK_VERSION}_OPENJ9_REPO
    openj9_branch=JDK${JDK_VERSION}_OPENJ9_BRANCH
    repo=JDK${JDK_VERSION}_REPO
    branch=JDK${JDK_VERSION}_BRANCH
    eval openj9_repo2='$'$openj9_repo
    eval openj9_branch2='$'$openj9_branch
    eval repo2='$'$repo
    eval branch2='$'$branch

    if [ $JDK_IMPL == "openj9" ]
    then
        export JDK_REPO=$openj9_repo2
        export JDK_BRANCH=$openj9_branch2
        
    else
        export eval JDK_REPO=$repo2
        export eval JDK_BRANCH=$branch2
    fi

fi
cd ./TKG;
$MAKE compile