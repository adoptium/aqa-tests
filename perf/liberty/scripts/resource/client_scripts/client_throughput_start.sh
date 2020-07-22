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

# Check environment variables
if [ -z "${NET_PROTOCOL}" ]; then
	echo "NET_PROTOCOL not set"
	#TODO : Fix "echo $USAGE" to properly echo USAGE by exporting function from throughput_bencmhark.sh
	echo ${USAGE}
	exit
fi
if [ -z "${CLIENT_MACHINE_USER}" ]; then
	echo "CLIENT_MACHINE_USER not set"
	echo ${USAGE}
	exit
fi
if [ -z "${HOST_MACHINE_USER}" ]; then
	echo "HOST_MACHINE_USER not set"
	echo ${USAGE}
	exit
fi
if [ -z "${DB_MACHINE_USER}" ]; then
	echo "DB_MACHINE_USER not set"
	echo ${USAGE}
	exit
fi
if [ -z "${MEASURES}" ]; then
	echo "MEASURES not set"
	echo ${USAGE}
	exit
fi
if [ -z "${WARMUPS}" ]; then
	echo "WARMUPS not set"
	echo ${USAGE}
	exit
fi
if [ -z "${MEASURE_TIME}" ]; then
	echo "MEASURE_TIME not set"
	echo ${USAGE}
	exit
fi
if [ -z "${WARMUP_TIME}" ]; then
	echo "WARMUP_TIME not set"
	echo ${USAGE}
	exit
fi
if [ -z "${DB_HOST}" ]; then
	echo "DB_HOST not set"
	echo ${USAGE}
	exit
fi
if [ -z "${DB2_HOME}" ]; then
	echo "DB2_HOME not set"
	echo ${USAGE}
	exit
fi
if [ -z "${DB_NAME}" ]; then
	echo "DB_NAME not set"
	echo ${USAGE}
	exit
fi
if [ -z "${WAS_HOST}" ]; then
	echo "WAS_HOST not set"
	echo ${USAGE}
	exit
fi
if [ -z "${WAS_PORT}" ]; then
	echo "WAS_PORT not set"
	echo ${USAGE}
	exit
fi
if [ -z "${WAS_PORT_CONFIG}" ]; then
	echo "WAS_PORT_CONFIG not set. Setting to 9080"
	export WAS_PORT_CONFIG=9080
fi

if [ -z "${CLIENT}" ]; then
	echo "CLIENT not set"
	echo ${USAGE}
	exit
fi
if [ -z "${SERVER_WORKDIR}" ]; then
	echo "SERVER_WORKDIR not set"
	echo ${USAGE}
	exit
fi

if [ -z "${SCENARIO}" ]; then
	echo "SCENARIO not set"
	echo ${USAGE}
	exit
fi

if [ -z "${SSL_CLIENTS}" ]; then
	echo "SSL_CLIENTS not set"
	echo "setting to 30"
	SSL_CLIENTS=30
fi

if [ -z "${THROUGHPUT_DRIVER}" ]; then
	echo "THROUGHPUT_DRIVER not set"
	echo ${USAGE}
	exit
else
	export THROUGHPUT_DRIVER=`echo $THROUGHPUT_DRIVER| awk '{print tolower($0)}'`
fi

case $THROUGHPUT_DRIVER in

IWL)
	which iwlengine > /dev/null
	if [ $? -gt 0 ]; then
		echo "iwlengine can't be found"
		exit 5
	fi
	;;
JMeter)
	ls -l ${JMETER_LOC} > /dev/null
		if [ $? -gt 0 ]; then
		echo "JMeter can't be found at ${JMETER_LOC}"
		exit 5
	fi
	;;
esac	

echo "cleaning up left over files"

datestamp=`date +%Y%m%d-%H:%M:%S`

for file in `ls *tmp resultstmp* jmeter.log results_bkup.txt *xml`
do
mkdir -p leftoverResults/${datestamp}
mv $file leftoverResults/${datestamp}
done

echo "WAS profile and server names are defined as follows:"
echo ""
echo "WAS_HOST=${WAS_HOST}"
echo "WAS_PORT=${WAS_PORT}"
echo "DB_HOST=${DB_HOST}"
echo "DB_NAME=${DB_NAME}"
echo "DB2_HOME=${DB2_HOME}"
echo ""

if [ -e results.xml ]; then
	echo "Removing previous results.xml"
	rm -f results.xml
fi

case $SCENARIO in

DayTrader7 | DayTrader7JDBC)
	SCRIPT_JMeter=daytrader7.jmx
	SCRIPT_IWL="daytrader7.jxs -f TradeApp.conf"
	;;
DayTrader | DayTraderJDBC)
	SCRIPT_JMeter=daytrader3.jmx
	SCRIPT_IWL="daytrader3.jxs -f TradeApp.conf"
	;;
TradeApp)
	SCRIPT_IWL="TradeApp.jxs -f TradeApp.conf"
	;;

TradeAppDerby)
	SCRIPT_IWL="TradeApp.jxs -f TradeApp.conf"
	;;

Primitive) 
	SCRIPT_IWL="Primitive.jxs -f Primitive.conf"
	EXTRA_IWL="--define url=/tradelite/servlet/${PRIMITIVE}"

	;;

DayTraderCrypto) 
	SCRIPT_JMeter=daytrader3.jmx
	SCRIPT_IWL="ssl_daytrader3.jxs -f TradeApp.conf"
	;;

DayTraderRU) 
	SCRIPT_JMeter=daytrader3.jmx
	SCRIPT_IWL="daytrader3.jxs -f TradeApp.conf"
	;;

DayTraderSSL)
	SCRIPT_JMeter=daytrader3.jmx
	SCRIPT_IWL="ssl_daytrader3.jxs -f TradeApp.conf"
	;;

DayTrader7SSL)
	SCRIPT_JMeter=daytrader7_ssl.jmx
	SCRIPT_IWL="ssl_daytrader7.jxs -f TradeApp.conf"
	;;
JMS)
	SCRIPT_JMeter=jms.jmx
	;;

esac

# Configure the application in either JDBC or EJB mode depending on scenario
case $SCENARIO in
DayTraderJDBC | DayTrader7JDBC)
	DT3_RUNTIME_MODE="JDBC"
	;;
*)
	DT3_RUNTIME_MODE="EJB"
	;;
esac

echo "DT3_RUNTIME_MODE=${DT3_RUNTIME_MODE}"

# Some scenarios create new connections on each request - adjust client TCP params to cope with this
case $SCENARIO in
DayTraderSSL | DayTrader7SSL | JMS)
	echo "Setting SSL Specific values:"
	tcp_tw_reuse=`cat  /proc/sys/net/ipv4/tcp_tw_reuse`
	echo 'sudo su -c " echo 1 > /proc/sys/net/ipv4/tcp_tw_reuse"'
	sudo su -c " echo 1 > /proc/sys/net/ipv4/tcp_tw_reuse"
	tcp_fin_timeout=`cat /proc/sys/net/ipv4/tcp_fin_timeout`
	echo 'sudo su -c " echo 30 > /proc/sys/net/ipv4/tcp_fin_timeout"'
	sudo su -c " echo 30 > /proc/sys/net/ipv4/tcp_fin_timeout"	
	;;
*)
	# Use defaults
	;;
esac

PERM_CMD="chmod +x *.sh"
echo ${PERM_CMD}
${PERM_CMD}

echo ""

# Pre-run configuration

case $SCENARIO in
    JMS)
        # Nothing to do
        ;;
    *)
        echo ""
        echo "Configuring server..."
        echo ""

        MAX_WAIT=30
        WAIT_TIME=5
		
		#reqired for to run the configure script
		cd ${CLIENT_WORK_DIR}

        while [[ "`. ./configure.sh ${DT3_RUNTIME_MODE} | grep -c -i \"Configuration Updated\"`" == "0" ]]; do
            let WAIT_TOTAL=WAIT_TOTAL+WAIT_TIME

            # Have we waited long enough already?
            if [ ${WAIT_TOTAL} -gt ${MAX_WAIT} ]; then
                # TODO cleanup
                echo "!!! ERROR !!! Configuration failed after ${WAIT_TOTAL}s. Exiting"
                exit
            fi
            
            # Keep waiting
            echo "Waiting for configuration to succeed... (${WAIT_TOTAL})"
            sleep ${WAIT_TIME}
        done

        echo "Configuration successful."

        echo ""
        ;;
esac

# Start run
if [ "$SR" = "false" ]; then
	sed -i 's/https.use.cached.ssl.context=false/https.use.cached.ssl.context=true/g' jmeter_ssl.properties
	echo "setting to reuse session for warmups"
	echo "sed -i 's/https.use.cached.ssl.context=false/https.use.cached.ssl.context=true/g' jmeter_ssl.properties"
fi
runNo=0
results="results.xml"
case $SCENARIO in

        DayTraderSSL | DayTrader7SSL)
                for(( clientIterator=1; clientIterator<=${JMETER_INSTANCES}; clientIterator++ ))
                do
                        echo "<iteration server=\"${WAS_HOST}\">" >>  client${clientIterator}.txt
                done
                ;;
        *)
	        echo "<iteration server=\"${WAS_HOST}\">" > ${results}
		;;
esac

bash cpu.sh start ${SERVER_WORKDIR} ${WAS_HOST} ${DB_HOST} ${NET_PROTOCOL} ${HOST_MACHINE_USER} ${CLIENT_MACHINE_USER} ${DB_MACHINE_USER} ${DB_SERVER_WORKDIR}


for (( i=0; i<${SINGLE_CLIENT_WARMUPS}; i++ ))
do
	let runNo=runNo+1
	echo "Running 1 client warmup"
	./reset.sh
	
	# CPU
	bash cpu.sh 60 warmup ${runNo} ${CLIENT} ${SERVER_WORKDIR} ${WAS_HOST} ${DB_HOST} ${NET_PROTOCOL} ${HOST_MACHINE_USER} ${CLIENT_MACHINE_USER} ${DB_MACHINE_USER} ${DB_SERVER_WORKDIR} &
	
	CPU_PID=$!
	
	# trade
	. ./trade.sh warmup warmup >> ${results}

	
	wait ${CPU_PID}
	
	sleep 1
done

echo "Running ${WARMUPS} warmups, ${WARMUP_TIME}s each"
for(( i=0; i<${WARMUPS}; i++ ))
do
	let runNo=runNo+1
	#./reset.sh no reset in 16_01 and later
	
	# CPU
	bash cpu.sh ${WARMUP_TIME} warmup ${runNo} ${CLIENT} ${SERVER_WORKDIR} ${WAS_HOST} ${DB_HOST} ${NET_PROTOCOL} ${HOST_MACHINE_USER} ${CLIENT_MACHINE_USER} ${DB_MACHINE_USER} ${DB_SERVER_WORKDIR} &
	
	CPU_PID=$!
	
	# trade

	. ./trade.sh  ${WARMUP_TIME} warmup >> ${results}
	
	wait ${CPU_PID}
	
	sleep 1
done

if [ "$SR" = "false" ]; then
	sed -i 's/https.use.cached.ssl.context=true/https.use.cached.ssl.context=false/g' jmeter_ssl.properties
	echo "Replaced values back to no session reuse"
	echo "sed -i 's/https.use.cached.ssl.context=true/https.use.cached.ssl.context=false/g' jmeter_ssl.properties"
fi

echo "Running ${MEASURES} measures, ${MEASURE_TIME}s each"
for(( i=0; i<${MEASURES}; i++ ))
do
	let runNo=runNo+1
	#./reset.sh no reset in 16_01 and later
	
	# CPU
	bash cpu.sh ${MEASURE_TIME} measure ${runNo} ${CLIENT} ${SERVER_WORKDIR} ${WAS_HOST} ${DB_HOST} ${NET_PROTOCOL} ${HOST_MACHINE_USER} ${CLIENT_MACHINE_USER} ${DB_MACHINE_USER} ${DB_SERVER_WORKDIR} &
	CPU_PID=$!

	if [ ! -z "$PROFILING_TOOL" ]; then
		let PROFILING_PROFILE_TIME=MEASURE_TIME*2/3

		echo "Running Profiler for 2/3 of the measure time (${MEASURE_TIME}). PROFILING_PROFILE_TIME=${PROFILING_PROFILE_TIME}"
		. ./profile.sh
		runProfile ${WAS_HOST} ${PROFILING_PROFILE_TIME} ${SERVER_WORKDIR} &
		PROFILING_TOOL_PID=$!
	fi

	# trade
	. ./trade.sh  ${MEASURE_TIME} measure >> ${results}
	if [ ! -z "$PROFILING_TOOL" ]; then
		wait ${PROFILING_TOOL_PID}
	fi
	wait ${CPU_PID}
	
	sleep 1
done

# Restore TCP params back to previous values
case $SCENARIO in
DayTraderSSL | DayTrader7SSL | JMS)
	echo "restoring SSL specific values to default:"
	echo "sudo su -c  echo $tcp_tw_reuse > /proc/sys/net/ipv4/tcp_tw_reuse"
	sudo su -c " echo $tcp_tw_reuse > /proc/sys/net/ipv4/tcp_tw_reuse"
	echo "sudo su -c  echo $tcp_fin_timeout > /proc/sys/net/ipv4/tcp_fin_timeout"
	sudo su -c " echo $tcp_fin_timeout > /proc/sys/net/ipv4/tcp_fin_timeout"
	;;
*)
	# Nothing to do
	;;
esac

case $SCENARIO in
	DayTraderSSL | DayTrader7SSL)
		for(( clientIterator=1; clientIterator<=${JMETER_INSTANCES}; clientIterator++ ))
		do
			echo "</iteration>" >>  client${clientIterator}.txt 
		done
		echo "restoring SSL specific values to default:"
		echo "sudo su -c  echo $tcp_tw_reuse > /proc/sys/net/ipv4/tcp_tw_reuse"
		sudo su -c " echo $tcp_tw_reuse > /proc/sys/net/ipv4/tcp_tw_reuse"
		echo "sudo su -c  echo $tcp_fin_timeout > /proc/sys/net/ipv4/tcp_fin_timeout"
		sudo su -c " echo $tcp_fin_timeout > /proc/sys/net/ipv4/tcp_fin_timeout"
		;;
	*)
		echo "</iteration>" >> ${results}
		;;
esac
bash cpu.sh end ${SERVER_WORKDIR} ${WAS_HOST} ${DB_HOST} ${NET_PROTOCOL} ${HOST_MACHINE_USER} ${CLIENT_MACHINE_USER} ${DB_MACHINE_USER} ${DB_SERVER_WORKDIR}

echo "Client scripts complete"
