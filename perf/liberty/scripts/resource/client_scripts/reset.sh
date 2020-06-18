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
	URL=/daytrader/config?action=resetTrade
	;;

TradeLite)
	URL=/tradelite/config?action=resetTrade
	;;
JMS)
	# Do nothing
	;;
*)
	exit 3
	;;
esac

LOGDIR=log
LOG=${LOGDIR}/reset.out
RESPONSE_DOC=log/reset_response.html

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

echo Script to reset DayTrader on WAS daytrader server instance - ${WAS_HOST}:${WAS_PORT}
mkdir -p ${LOGDIR}
wget -o ${LOG} -O ${RESPONSE_DOC} http://${WAS_HOST}:${WAS_PORT}${URL}
grep 'successfully' ${RESPONSE_DOC}
