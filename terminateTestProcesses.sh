#!/bin/sh

# Arg S1 : Unix current env.User

# If Windows uses PowerShell
if [ "$OS" = "Windows_NT" ]; then
  echo "Windows machine, using powershell queries..."

  # Match anything that is most likely a test job or process related
  match_str="(CommandLine like '%java%jck%' or \
             CommandLine like '%openjdkbinary%java%' or \
             CommandLine like '%java%javatest%' or \
             CommandLine like '%java%-Xfuture%' or \
             CommandLine like '%rmid%' or \
             CommandLine like '%rmiregistry%' or \
             CommandLine like '%tnameserv%' or \
             CommandLine like '%make.exe%')"

  # Ignore Jenkins agent and grep cmd
  ignore_str="not CommandLine like '%remoting.jar%' and \
              not CommandLine like '%agent.jar%' and \
              not CommandLine like '%grep%'"

  count=`powershell -c "(Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}} | measure).count" | tr -d "\\\\r"`
  
  if [ $count -gt 0 ]; then
      echo Windows rogue processes detected, attempting to stop them..
      powershell -c "Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}}"
      powershell -c "(Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}}).Terminate()"
      echo Sleeping for 10 seconds...
      sleep 10
      count=`powershell -c "(Get-WmiObject Win32_Process -Filter {${ignore_str} and ${match_str}} | measure).count" | tr -d "\\\\r"`
      if [ $count -gt 0 ]; then
        echo "Cleanup failed, ${count} processes still remain..."
        exit 127
      fi 
      echo "Processes stopped successfully"
  else
      echo Woohoo - no rogue processes detected!
  fi
else
  echo "Unix type machine.."

  # Match anything that is most likely a jck test job or process related
  match_str="java.*jck|openjdkbinary.*java|java.*javatest|java.*-Xfuture|X.*vfb|rmid|rmiregistry|tnameserv|make"

  # Ignore Jenkins agent and grep cmd
  ignore_str="remoting.jar|agent.jar|grep"

  if [ `uname` = "SunOS" ]; then
      PSCOMMAND="/usr/ucb/ps -uxww"
  else
      PSCOMMAND="ps -fu $1"
  fi

  LINUX_DOCKER_FILTER=""
  if [ `uname` = "Linux" ]; then
      if egrep "\/docker\/" /proc/1/cgroup >nul; then
          echo Running in a Linux docker container
      else
          echo Running on a Linux host
          # Filter any possible docker container processes by cgroup containing "/docker"
          PSCOMMAND="ps -o cgroup,pid,state,tname,time,command -u $1"
          LINUX_DOCKER_FILTER="| egrep -v '^[^[:space:]]+\/docker'"
      fi
  fi

  if eval "$PSCOMMAND | egrep '${match_str}' | egrep -v '${ignore_str}' ${LINUX_DOCKER_FILTER}"; then
      echo Boooo - there are rogue processes kicking about
      echo Issuing a kill to all processes shown above
      eval "$PSCOMMAND | egrep '${match_str}' | egrep -v '${ignore_str}' ${LINUX_DOCKER_FILTER} | awk '{print \$2}' | xargs -n1 kill"
      echo Sleeping for 10 seconds...
      sleep 10
      if eval "$PSCOMMAND | egrep '${match_str}' | egrep -v '${ignore_str}' ${LINUX_DOCKER_FILTER}"; then
        echo Still processes left going to remove those with kill -KILL ...
        eval "$PSCOMMAND | egrep '${match_str}' | egrep -v '${ignore_str}' ${LINUX_DOCKER_FILTER} | awk '{print \$2}' | xargs -n1 kill -KILL"
        echo DONE - One final check ...
        if eval "$PSCOMMAND | egrep '${match_str}' | egrep -v '${ignore_str}' ${LINUX_DOCKER_FILTER}"; then
          echo "Cleanup failed, processes still remain..."
          exit 127
        fi
      fi
      echo "Processes stopped successfully"
  else
      echo Woohoo - no rogue processes detected!
  fi
fi

exit 0

