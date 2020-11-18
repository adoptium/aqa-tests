#!/usr/bin/env bash
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
######
case `uname` in
  AIX | SunOS | *BSD )
    MAKE=gmake
    ;;
  *)
    MAKE=make
    ;;
esac

if [ "$OSTYPE" = "cygwin" ]; then
  export JOBSTARTTIME=$(date -R)
  echo PROCESSCATCH: Java processes which are currently on the machine:
  powershell -c "Get-Process java | select id,processname,starttime"
fi

if [ "$#" -eq 1 ];then
	cd $1/TKG
	$MAKE compile
else
	cd $1/TKG
	shift
	$MAKE $@
fi

# For now (while this code is being prototyped) I only want this to be
# done while running in jenkins, to avoid a potentially destabilising
# change if people are running other java processes on their local system
set -x
if [ ! -z "$EXECUTOR_NUMBER" ]; then
  if [ "$OSTYPE" = "cygwin" ]; then
    # Windows code based on https://github.com/AdoptOpenJDK/openjdk-infrastructure/issues/1669#issuecomment-727863096
    echo PROCESSCATCH: Showing java processes - one will be the jenkins agent - removing any of these created since $JOBSTARTTIME:
    powershell -c "Get-Process java"
    C=`powershell -c "(Get-Process java | select id,processname,starttime | where {\\$_.StartTime -gt (Get-Date -Date \"$JOBSTARTTIME\")} | measure).count" | tr -d \\\\r`
    if [ $C -ne 0 ]; then
      echo PROCESSCATCH: Attempting to stop the following processes:
      powershell -c "Get-Process java | select id,processname,starttime | where {\$_.StartTime -gt (Get-Date -Date \"$JOBSTARTTIME\")}"
      powershell -c "Get-Process java | select id,processname,starttime | where {\$_.StartTime -gt (Get-Date -Date \"$JOBSTARTTIME\")} | stop-process"
      echo PROCESSCATCH: Removed processes - here is what is remaining:
      powershell -c "Get-Process java"
    fi
  else
    # Non-Windows code from https://ci.adoptopenjdk.net/job/SXA-processCheck
    echo PROCESSCATCH: Checking for any leftover java processes on the machine
    if ps -fu $USER | grep java | egrep -v "remoting.jar|agent.jar|grep"; then
      echo PROCESSCATCH: There are rogue processes still on the machine as per the list above
      # TODO: We could kill them, but SXA-processCheck covers this in the adoptopenjdk jenkins
      # And this is likely to be replaced as part of https://github.com/AdoptOpenJDK/TKG/issues/45
    fi
  fi
fi
