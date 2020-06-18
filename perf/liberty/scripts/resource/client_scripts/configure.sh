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

case $SCENARIO in

DayTrader*)
        # Allow either EJB or JDBC mode to be specified when configuring DayTrader
        DT3_RUNTIME_MODE=$1
        if [ -z "$DT3_RUNTIME_MODE" ]; then
            echo "Runtime mode (JDBC or EJB) not specified, defaulting to EJB"
            RUNTIME_VALUE=0
	else
            case $DT3_RUNTIME_MODE in
            EJB|ejb)
		echo "Configuring DayTrader runtime mode to 'EJB'"
                RUNTIME_VALUE=0
                ;;
            JDBC|jdbc)
		echo "Configuring DayTrader runtime mode to 'JDBC'"
                RUNTIME_VALUE=1
                ;;
            *)
                echo "Error - unknown runtime mode '$DT3_RUNTIME_MODE'"
                ;;
	    esac
	fi
	URL=/daytrader/config
	LOGDIR=log
	LOG=${LOGDIR}/config.out
	RESPONSE_DOC=log/config_response.html
	#POST_DATA="action=updateConfig&RunTimeMode=1&OrderProcessingMode=0&AcessMode=0&WorkloadMix=0&WebInterface=0&CachingType=2&MaxUsers=500&MaxQuotes=1000&primIterations=1&EnableLongRun=true"
	POST_DATA="action=updateConfig&RunTimeMode=${RUNTIME_VALUE}&OrderProcessingMode=0&SOAP_URL=http://localhost:${WAS_PORT}/daytrader/services/TradeWSServices&WorkloadMix=0&WebInterface=0&CachingType=0&MaxUsers=15000&MaxQuotes=10000&marketSummaryInterval=20&primIterations=1&EnablePublishQuotePriceChange=on&EnableLongRun=on"

	if [ -z "${WAS_HOST}" ]; then
	  echo "WAS_HOST is not set - try running setenv"
	  exit
	fi

	unset HTTP_PROXY
	unset HTTPS_PROXY
	unset FTP_PROXY

	unset http_proxy
	unset https_proxy
	unset ftp_proxy

	echo Script to configure WAS daytrader server instance - ${WAS_HOST}:${WAS_PORT}
	echo Now setting new configuration
	mkdir -p ${LOGDIR}
	wget -o ${LOG} -O ${RESPONSE_DOC} --post-data=${POST_DATA} http://${WAS_HOST}:${WAS_PORT_CONFIG}${URL}
	grep 'Updated' ${RESPONSE_DOC}
	;;

TradeLite)
	URL=/tradelite/config
	LOGDIR=log
	LOG=${LOGDIR}/config.out
	RESPONSE_DOC=log/config_response.html
	POST_DATA="action=updateConfig&RunTimeMode=1&OrderProcessingMode=0&AcessMode=0&SOAP_URL=http://localhost/trade/services/TradeWSServices?wsdl&WorkloadMix=0&WebInterface=0&CachingType=2&MaxUsers=500&MaxQuotes=1000&primIterations=1&EnableLongRun=true"

	if [ -z "${WAS_HOST}" ]; then
	  echo "WAS_HOST is not set - try running setenv"
	  exit
	fi

	unset HTTP_PROXY
	unset HTTPS_PROXY
	unset FTP_PROXY

	unset http_proxy
	unset https_proxy
	unset ftp_proxy

	echo Script to configure WAS daytrader server instance - ${WAS_HOST}:${WAS_PORT}
	echo Now setting new configuration
	mkdir -p ${LOGDIR}
	wget -o ${LOG} -O ${RESPONSE_DOC} --post-data=${POST_DATA} http://${WAS_HOST}:${WAS_PORT_CONFIG}${URL}
	grep 'Updated' ${RESPONSE_DOC}
	;;
esac
