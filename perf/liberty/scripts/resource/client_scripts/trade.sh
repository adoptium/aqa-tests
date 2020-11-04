#!/usr/bin/env bash

################################################################################
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
################################################################################

CLIENTS=50
TIMELIMIT=$1
runType=$2

echo "${SCENARIO} Run against ${WAS_HOST}:${WAS_PORT}"

if [ -z "$1" ]; then
  echo "Usage: trade.sh <warmup | n> run_type"
  echo "  - warmup runs a warmup with 1 client to allow the database to warm up gently."
  echo "  - n (a number in seconds) runs with $CLIENTS clients for that number of seconds."
  echo "  - run_type - text for the type fof run - warmup or measure."
  exit
fi


rm resultstmp.txt > /dev/null 2>&1
case $SCENARIO in

JMS)
	# JMS scenario starts with 1 client, then 5, then 20 
	CLIENTS=20
	if [ "${runNo}" = "2" ]; then
	    CLIENTS=5
	fi
	echo "<data machine=\"${CLIENT}\" runType=\"${runType}\" runNo=\"${runNo}\">" 
	if [ "$TIMELIMIT" == "warmup" ]; then
	  echo "Warming up with 1 client for 60 seconds..."
	  TIMELIMIT=60
	  ${JMETER_LOC} -n -t ${SCRIPT_JMeter} -p jmeter_nonssl.properties -JTHREADS=1 -JDURATION=60 -JHOST=${WAS_HOST} -JPORT=9128 -JURL=JMSApp/2.0/P2P?ACTION=NonPersistent -JPAYLOAD=${PWD}/jms1KB >>resultstmp.txt  
	else
	  echo "Running $CLIENTS clients for $TIMELIMIT seconds..."
	  ${JMETER_LOC} -n -t ${SCRIPT_JMeter} -p jmeter_nonssl.properties -JTHREADS=${CLIENTS} -JDURATION=$TIMELIMIT -JHOST=${WAS_HOST} -JPORT=9128 -JURL=JMSApp/2.0/P2P?ACTION=NonPersistent -JPAYLOAD=${PWD}/jms1KB >>resultstmp.txt
	fi
	summary=`cat resultstmp.txt|sed 's%<summary>%%g'|grep "summary = "|tail -1`
	throughput=`echo $summary|awk '{print $7}' | sed 's%/s%%g'`
	responsetime=`echo $summary|awk '{print $9}' | sed 's%/s%%g'`
	weberrors=`echo $summary|awk '{print $15}' | sed 's%/s%%g'`
	pages=`echo $summary|awk '{print $3}' | sed 's%/s%%g'`
	echo "Page throughput = ${throughput} /s" 
	echo "HTTP avg. page element response time = ${responsetime}" 
	echo "Web server errors = ${weberrors}" 
	echo "Network I/O errors = ${weberrors}" 
	echo "Num of pages retrieved = ${pages}"  
	echo "Initial number of clients = $CLIENTS"
	echo "</data>" 
	# Sleep, to try to reduce socket exhaustion errors
	sleep 30
	cat resultstmp.txt >> results_bkup.txt 2>&1
	;;

DayTraderSSL | DayTrader7SSL)
	if [ "$TIMELIMIT" == "warmup" ]; then
		echo "Warming up with 1 client for 60 seconds..."
		TIMELIMIT=60
		CLIENTS=1
	else
		CLIENTS=$SSL_CLIENTS
	fi
	for(( clientIterator=0; clientIterator<${JMETER_INSTANCES}; clientIterator++ ))
	do
		SPLIT_UID=`echo "9000/${JMETER_INSTANCES}"|bc`
		BOTUID=`echo "${clientIterator}*${SPLIT_UID}"|bc`
		TOPUID=`echo "$BOTUID+${SPLIT_UID}-1"|bc`
		realClientIterator=`echo "${clientIterator} + 1"|bc`
		echo "<data machine=\"${CLIENT}.${realClientIterator}\" runType=\"$runType\" runNo=\"${runNo}\">" >> client${realClientIterator}.txt 

		${JMETER_LOC} -n -t ${SCRIPT_JMeter} -JTHREADS=$CLIENTS -p jmeter_ssl.properties -JDURATION=$TIMELIMIT -JHOST=${WAS_HOST} -JPROTO=https -JPORT=9443 -JBOTUID=${BOTUID} -JTOPUID=${TOPUID} |tee -a client${realClientIterator}.txt.tmp &
		PIDS="${PIDS} $!"
	done
	for PID in `ps -ef|grep jmeter|grep -v grep|grep -v jar|awk {'print $2'}`
        do
       		wait $PID
	done

	for(( clientIterator=1; clientIterator<=${JMETER_INSTANCES}; clientIterator++ ))
	do
		summary=`cat client${clientIterator}.txt.tmp|sed 's%<summary>%%g'|grep "summary = "|tail -1`
		throughput=`echo $summary|awk '{print $7}' | sed 's%/s%%g'`
		responsetime=`echo $summary|awk '{print $9}' | sed 's%/s%%g'`
		weberrors=`echo $summary|awk '{print $15}' | sed 's%/s%%g'`
		pages=`echo $summary|awk '{print $3}' | sed 's%/s%%g'`
		echo "Page throughput = ${throughput} /s" >> client${clientIterator}.txt
		echo "HTTP avg. page element response time = ${responsetime}" >> client${clientIterator}.txt 
		echo "Web server errors = ${weberrors}" >> client${clientIterator}.txt
		echo "Network I/O errors = ${weberrors}" >> client${clientIterator}.txt
		echo "Num of pages retrieved = ${pages}"  >> client${clientIterator}.txt
		echo "Initial number of clients = 1" >> client${clientIterator}.txt	
		echo "</data>" >>  client${clientIterator}.txt
		echo $clientIterator >> results_bkup.txt
		cat client${clientIterator}.txt >> results_bkup.txt 2>&1
	done
	;;
*)
	echo "<data machine=\"${CLIENT}\" runType=\"${runType}\" runNo=\"${runNo}\">" 
	case $THROUGHPUT_DRIVER in
		iwl)
			if [ "$TIMELIMIT" == "warmup" ]; then
			  echo "Warming up with 1 client for 60 seconds..."
			  iwlengine -s ${SCRIPT_IWL} --define hostname=${WAS_HOST}:${WAS_PORT} --define botClient=0 --define topClient=14999 ${EXTRA_IWL} -c 1 --timelimit 60
			else
			  echo "Running $CLIENTS clients for $TIMELIMIT seconds..."
			  iwlengine -s ${SCRIPT_IWL} --define hostname=${WAS_HOST}:${WAS_PORT} --define botClient=0 --define topClient=14999 ${EXTRA_IWL} -c $CLIENTS --timelimit $TIMELIMIT
			fi
		;;
		jmeter)
			if [ "$TIMELIMIT" == "warmup" ]; then
			  echo "Warming up with 1 client for 60 seconds..."
			  TIMELIMIT=60
			  ${JMETER_LOC} -n -t ${SCRIPT_JMeter} -p jmeter_nonssl.properties -JTHREADS=1 -JDURATION=60 -JHOST=${WAS_HOST} >>resultstmp.txt  
			else
			  echo "Running $CLIENTS clients for $TIMELIMIT seconds..."
			  ${JMETER_LOC} -n -t ${SCRIPT_JMeter} -p jmeter_nonssl.properties -JTHREADS=${CLIENTS} -JDURATION=$TIMELIMIT -JHOST=${WAS_HOST} >>resultstmp.txt
			fi
			
			summary=`cat resultstmp.txt|sed 's%<summary>%%g'|grep "summary = "|tail -1`
	                throughput=`echo $summary|awk '{print $7}' | sed 's%/s%%g'`
		        responsetime=`echo $summary|awk '{print $9}' | sed 's%/s%%g'`
		        weberrors=`echo $summary|awk '{print $15}' | sed 's%/s%%g'`
		        pages=`echo $summary|awk '{print $3}' | sed 's%/s%%g'`
		        echo "Page throughput = ${throughput} /s" 
		        echo "HTTP avg. page element response time = ${responsetime}" 
		        echo "Web server errors = ${weberrors}" 
		        echo "Network I/O errors = ${weberrors}" 
		        echo "Num of pages retrieved = ${pages}"  
		        echo "Initial number of clients = $CLIENTS"
			cat resultstmp.txt >> results_bkup.txt 2>&1

		;;
	esac
	echo "</data>" 
	;;
esac
