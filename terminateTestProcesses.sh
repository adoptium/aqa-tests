#!/bin/sh


set -euxo pipefail

# If Windows use PowerShell
if [ "$OSTYPE" = "cygwin" ]; then
  echo "Windows machine, using powershell queries..."

  # Match anything that most likely test job or process related
  match_str="(CommandLine like '%java%jck%' or \
             CommandLine like '%openjdkbinary%java%' or \
             CommandLine like '%java%javatest%' or \
             CommandLine like '%java%-Xfuture%' or \
             CommandLine like '%X%vfb%' or \
             CommandLine like '%rmid%' or \
             CommandLine like '%rmiregistry%' or \
             CommandLine like '%tnameserv%' or \
             CommandLine like '%make.exe%')"

  # Ignore Jenkins agent and grep cmd
  ignore_str="not CommandLine like '%remoting.jar%' and \
              not CommandLine like '%agent.jar%' and \
              not CommandLine like '%grep%'"

  C=`powershell -c "(Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}} | measure).count" | tr -d "\\\\r"`
  
  if [ $C -gt 0 ]; then
      echo Windows rogue processes detected, attempting to stop them..
      powershell -c "Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}}"
      powershell -c "(Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}}).Terminate()"
      uname | grep CYGWIN 2>/dev/null || uptime
      sleep 10
      uname | grep CYGWIN 2>/dev/null || uptime
      C=`powershell -c "(Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}} | measure).count" | tr -d "\\\\r"`
      if [ $C -gt 0 ]; then
        echo "Cleanup failed, processes still remain..."
        exit 127
      fi 
      echo "Processes stopped successfully"
  else
      echo Woohoo - no rogue processes detected
  fi
else
  echo "Unix type machine.."

  # Match anything that most likely jck test job or process related
  # Note: On Solaris "other user" ps output trauncated to 80 chars, so this list
  #       needs best attempt to match first part of command line
  match_str="java.*jck|openjdkbinary.*java|java.*javatest|java.*-Xfuture|X.*vfb|rmid|rmiregistry|tnameserv|make"

  # Ignore Jenkins agent and grep cmd
  ignore_str="remoting.jar|agent.jar|grep"

  # Fix For Issue https://github.com/adoptium/infrastructure/issues/2442 - Solaris PS Command Truncation
  if [ `uname` = "SunOS" ]; then
      PSCOMMAND="/usr/ucb/ps -uxww"
  else
      PSCOMMAND="ps -fu jenkins"
  fi

  if $PSCOMMAND | egrep "${match_str}" | egrep -v "${ignore_str}"; then
      echo Boooo - there are rogue processes kicking about
      echo Issuing a kill to all processes shown above - though this will not terminate vfb processes on AIX as we are trying to debug source of leftovers there
      $PSCOMMAND | egrep "${match_str}" | egrep -v "${ignore_str}" | awk '{print $2}' | xargs -n1 kill
      uname | grep CYGWIN 2>/dev/null || uptime
      sleep 10
      uname | grep CYGWIN 2>/dev/null || uptime
      if $PSCOMMAND | egrep "${match_str}" | egrep -v "${ignore_str}"; then
        echo Still processes left going to remove those with kill -KILL ...
        $PSCOMMAND | egrep "${match_str}" | egrep -v "${ignore_str}" | awk '{print $2}' | xargs -n1 kill -KILL
        echo DONE - One final check ...
        if $PSCOMMAND | egrep "${match_str}" | egrep -v "${ignore_str}"; then
          echo "Cleanup failed, processes still remain..."
          exit 127
        fi
      fi
      echo "Processes stopped successfully"
  else
      echo Woohoo - no rogue processes detected
  fi
fi

exit 0

