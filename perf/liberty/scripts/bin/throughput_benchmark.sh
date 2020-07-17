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

## Print out the help information if all mandatory environment variables are not set
usage()
{
echo "
Usage:

To customize the use of this script use the following environment variables:

VERBOSE_MODE        - Prints all commands before they are executed.

NO_SETUP            - Liberty server creation is skipped if set to true.
                      If false, a new server will be created and the corresponding customized server.xml
                      and customized bootstrap.properties (if exists) files will overwrite the server's default.

SETUP_ONLY          - Only Liberty server creation and replacement of Liberty server default server.xml and bootstrap.properties (if available) is done.

JDK                 - Name of JDK build to use

JDK_DIR             - absolute path to directory where JDK builds are located.\n
                      Ex: if the path to a JDK's bin directory is /tmp/all_jdks/jdk8/sdk/bin\n
                      Then JDK=jdk8 and JDK_DIR=/tmp/all_jdks

JDK_OPTIONS         - JVM command line options

PROFILING_TOOL      - Profiling Tool to Use (Example: 'jprof tprof', 'perf stat')

LIBERTY_HOST        - Hostname for Liberty host machine

LIBERTY_VERSION     - Liberty application's root directory

LIBERTY_PORT        - Listening port on Liberty host machine

LAUNCH_SCRIPT       - Liberty script that can create, start, stop a server

SERVER_NAME         - The given name will be used to identify the Liberty server.

SERVER_XML          - XML File

AFFINITY            - CPU pinning command prefixed on the Liberty server start command.

LARGE_THREAD_POOL   - Sets specified CORE_THREADS, MAX_THREADS, keepAlive=60s, STEAL_POLICY,
                      rejectedWorkPolicy=CALLER_RUNS

CORE_THREADS        - Starting number of core threads in the pool used by server

MAX_THREADS         - Max number of threads in the pool used by server

STEAL_POLICY        - Steal policy defaults to LOCAL if not set

SCENARIO            - Supported scenarios: AcmeAir, TradeApp, TradeAppDerby, Primitive,
                      DayTrader, DayTrader7, DayTraderJDBC, DayTrader7JDBC, DayTraderCrypto, DayTraderRU,
                      DayTraderSSL, DayTrader7SSL, JMS 

APP_VERSION
                    - The specific DayTrader 3 application version used for all scenarios
                      involving this particular benchmark. (Default: daytrader3.0.10.1-ee6-src)

CLEAN_RUN           - adds the --clean option suffix when starting the Liberty server

GCMV_ENABLED        - The GCMV tool will run

RESULTS_MACHINE     - Hostname for results storage machine

RESULTS_DIR         - Name of directory on Liberty host where results are temporarily stored

ROOT_RESULTS_DIR    - Absolute path of Liberty results directory on remote storage machine

CLIENT              - Hostname for client machine

CLIENT_WORK_DIR     - Directory on client machine used to store the client script

THROUGHPUT_DRIVER   - Client side throughput driver

MEASURES            - number of measure runs client will perform

WARMUPS             - number of warmup runs client will perform

SINGLE_CLIENT_WARMUP
                    - number of warmup runs with 1 simulated client

MEASURE_TIME        - number of seconds each measure run will last

WARMUP_TIME         - number of seconds each warmup run will last

DB_MACHINE          - Hostname for database achine

DB2_HOME            - Home directory for db2 user (ex. /home/db2inst1/)

DB_NAME             - Name of database (ex. day30r)

DB_PORT             - Listening port for database (ex. 50000)

DB_USR_NAME         - Database login username

DB_PASSWORD         - Database login password (Defaults to DB_USR_NAME)

DB_SERVER_WORKDIR   - Work directory for database (Defaults to /java/perffarm/liberty)

JMETER_LOC			- Location of Load Driver: JMeter (/java/perffarm/JMeter/apache-jmeter-2.12/bin/jmeter)

Specific to the DayTraderSSL & DayTrader7SSL Scenarios:

CIPHER_SUITE        - Configured in Liberty server.xml

KEY_TYPE            - Configured in Liberty server.xml

KEY_LENGTH          - Configured in Liberty server.xml

SSL_PASSWORD        - Keypass value for Java keytool (Default: Liberty)
                      Also configured in JMeter system.properties file

PROTOCOL            - Configured in Liberty server.xml and JMeter jmeter.properties

SSL_SESSION_REUSE   - Configured in JMeter jmeter.properties

JMETER_INSTANCES    - Number of JMeter clients. Defaults to 1

HOST_MACHINE_USER   - HOST Machine username to be used for ssh logins ( for STAF it will be automatically set to User that STAF runs on as dummy var)

DB_MACHINE_USER     - DB Machine username to be used for ssh logins ( for STAF it will be automatically set to User that STAF runs on as dummy var)

CLIENT_MACHINE_USER - Client Machine username to be used for ssh logins ( for STAF it will be automatically set to User that STAF runs on as dummy var)

NET_PROTOCOL        - Intermachine communication protocol decider variable
                      User may declaire variable as STAF, SSH or LOCAL else, exit
                      If LOCAL is specified when more than one machines used, use STAF instead
                      If not specificed, STAF is used as default


Specific to the Primitive Scenario:

PRIMITIVE           - The primitive servlet type
"
}

#######################################################################################
#	THROUGHPUT UTILS - Helper methods that are throughput specific
#######################################################################################

SSH_CMD=""
STAF_CMD=""
LOCAL_CMD=""

function runNetProtocolCmd()
{
    case ${NET_PROTOCOL} in
        "STAF")
            echoAndRunCmd "${STAF_CMD}" "${1}" 
        ;; 
        "SSH") 
            echo "${SSH_CMD}"
            echoAndRunCmd "${SSH_CMD}" "${1}"
        ;;
        "LOCAL")
            echoAndRunCmd "${LOCAL_CMD}" "${1}"
        ;;
esac
}
##
## Environment variables setup. Terminate script if a mandatory env variable is not set
checkAndSetThroughputEnvVars()
{
    printf '%s\n' "
.--------------------------
| Throughput Environment Setup
"
    if [ -z "$NET_PROTOCOL" ]; then
        echo "NET_PROTOCOL not set"
        if [ "${DB_MACHINE}" == "${CLIENT}" ] && [ "${LIBERTY_HOST}" == "${CLIENT}" ]; then
            echo "NET_PROTOCOL set to LOCAL as all 3 machines are same"
            export NET_PROTOCOL="LOCAL"
        else
            echo "NET_PROTOCOL set to STAF"
            export NET_PROTOCOL="STAF"
        fi
    elif [ $NET_PROTOCOL == "LOCAL" ] && [ "${DB_MACHINE}" != "${CLIENT}" ] && [ "${LIBERTY_HOST}" != "${CLIENT}" ]; then
        echo "DB Machine:" ${DB_MACHINE} "Client Machine:"${CLIENT} "Liberty Host Machine:"${LIBERTY_HOST}
        echo "All 3 machines must be same for Local run. Setting NET_PROTOCOL to STAF"
        export NET_PROTOCOL="STAF"
    fi
    echo "NET_PROTOCOL=${NET_PROTOCOL}"
    
    if [ -z "${CLIENT_MACHINE_USER}" ]; then
        echo "CLIENT MACHNINE_USER not set, setting it to jbench"
        #Using jbecnh for tempoary measure as Client Credential is not given in current state
        export CLIENT_MACHINE_USER="jbench"
    fi
    if [ -z "${DB_MACHINE_USER}" ]; then
        echo "DB MACHNINE_USER not set, setting it to jbench"
        #Using jbecnh for tempoary measure as Client Credential is not given in current state
        export DB_MACHINE_USER="jbench"
    fi
    if [ -z "${HOST_MACHINE_USER}" ]; then
        echo "HOST MACHNINE_USER not set, setting it to current user : `whoami`"
        export HOST_MACHINE_USER=`whoami`
    fi
    if [ ${NET_PROTOCOL} != "STAF" ] && [ "${PLATFORM}" = "CYGWIN" ]; then
        echo "Net protocol is not STAF, Setting Windows path of Benchmark & work dir to appropriate unix path"
        BENCHMARK_ORIGINAL_DIR=${BENCHMARK_DIR}
        echo ${BENCHMARK_ORIGINAL_DIR}
        BENCHMARK_DIR=`cygpath -u ${BENCHMARK_DIR}`
        CLIENT_WORK_DIR=`cygpath -u ${CLIENT_WORK_DIR}`
    fi
    
    if [ -z "$GCMV_ENABLED" ]; then
        echo "GCMV_ENABLED not set. Defaulting to true"
        GCMV_ENABLED="true"
        
    fi

    if [ -z "$MEASURES" ]; then
        echo "MEASURES not set"
        usage
        exit
    fi

    if [ -z "$WARMUPS" ]; then
        echo "WARMUPS not set"
        usage
        exit
    fi
    
    if [ -z "$SINGLE_CLIENT_WARMUP" ]; then
        echo "SINGLE_CLIENT_WARMUP not set. Defaulting to 1"
        export SINGLE_CLIENT_WARMUP=1
    fi

    if [ -z "$MEASURE_TIME" ]; then
        echo "MEASURE_TIME not set"
        usage
        exit
    fi

    if [ -z "$WARMUP_TIME" ]; then
        echo "WARMUP_TIME not set"
        usage
        exit
    fi

    if [ -z "$CLIENT" ]; then
        echo "CLIENT not set"
        usage
        exit
    fi

    if [ -z "$CLIENT_WORK_DIR" ]; then
        echo "CLIENT_WORK_DIR not set"
        usage
        exit
    fi

	if [ -z "$JMETER_LOC" ]; then
        echo "Optional JMETER_LOC not set. Setting to '/java/perffarm/JMeter/apache-jmeter-2.12/bin/jmeter'"
        JMETER_LOC="/java/perffarm/JMeter/apache-jmeter-2.12/bin/jmeter"
    fi
    
    if [ -z "$DB_MACHINE" ]; then
        echo "DB_MACHINE not set"
        usage
        exit
    fi

    if [ -z "$DB2_HOME" ]; then
        echo "DB2_HOME not set"
        usage
        exit
    fi

    if [ -z "$DB_NAME" ]; then
        echo "DB_NAME not set"
        usage
        exit
    fi
    
    if [ -z "$DB_PORT" ]; then
        echo "DB_PORT not set"
        usage
        exit
    fi

    if [ -z "$DB_USR_NAME" ]; then
        echo "DB_USR_NAME not set"
        usage
        exit
    fi

    if [ -z "$DB_PASSWORD" ]; then
        echo "Optional DB_PASSWORD not set. Setting to match DB_USR_NAME"
        DB_PASSWORD=${DB_USR_NAME}
    fi

    if [ -z "$THROUGHPUT_DRIVER" ]; then
        echo "Optional THROUGHPUT_DRIVER not set. Setting to IWL"
        THROUGHPUT_DRIVER="IWL"
    fi

    if [ -z "$LIBERTY_PORT" ]; then
        echo "LIBERTY_PORT not set"
        usage
        exit
    fi

    if [ -z "$LARGE_THREAD_POOL" ]; then
        echo "Optional LARGE_THREAD_POOL not set. Setting to 'false'"
        LARGE_THREAD_POOL="false"
    fi

    if [ -z "$CLEAN_RUN" ]; then
        echo "Optional CLEAN_RUN not set. Setting to 'true'"
        CLEAN_RUN="true"
    fi

    if [ -z "$SSL_PASSWORD" ]; then
        echo "SSL_PASSWORD not set. Defaulting to 'Liberty'"
        export SSL_PASSWORD="Liberty"
    fi

    if [ -z "$DB_SERVER_WORKDIR" ]; then
        echo "Optional DB_SERVER_WORKDIR not set. Setting to '/java/perffarm/liberty/'"
        DB_SERVER_WORKDIR="/java/perffarm/liberty"
    fi

    RESULTS="results.xml"
    CLIENT_CPU_RESULTS="client_CPU_results.xml"
    SERVER_CPU_RESULTS="server_CPU_results.xml"
    DB_CPU_RESULTS="db_CPU_results.xml"
}

##
## Scenario dependent environment variable setup
scenarioSpecificEnvSetup()
{
    printf '%s\n' "
.--------------------------
| Scenario Specific Environment Setup
"
    case ${SCENARIO} in
        DayTraderSSL|DayTrader7SSL)
            #check scenario SSL specific env vars
            if [ -z "$CIPHER_SUITE" ]; then
                echo "CIPHER_SUITE must be defined for SSL runs"
                echo "Some supported ones are : TLS_RSA_WITH_AES_128_CBC_SHA256"
                exit
            fi
            if [ -z "$KEY_TYPE" ]; then
                echo "KEY_TYPE must be defined for SSL runs. e.g RSA"
                exit
            fi
            if [ -z "$KEY_LENGTH" ]; then
                echo "KEY_LENGTH must be defined for SSL runs"
                exit
            fi
            if [ -z "$PROTOCOL" ]; then
                echo "PROTOCOL must be defined for SSL runs"
                exit
            fi
            if [ -z "$SSL_SESSION_REUSE" ]; then
                echo "SSL_SESSION_REUSE must be defined for SSL runs"
                exit
            fi
            if [ -z "$JMETER_INSTANCES" ]; then
                echo "JMETER_INSTANCES is not defined. Defaulting to 1"
                export JMETER_INSTANCES=1
            fi
            if [ "$CIPHER_SUITE" = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256" ]; then
                if [ "$KEY_TYPE" = "RSA" ]; then
                    export KEY_TYPE="ECDSA"
                fi
                if [ "$KEY_LENGTH" = "2048" ]; then
                    export KEY_LENGTH="256"
                fi
            fi

            # generate SSL keys and copy them to client
            if [ "$PLATFORM" = "CYGWIN" ]; then
                local tmpSERVER_DIR=$WIN_SERVER_DIR
            else
                local tmpSERVER_DIR=$SERVER_DIR
            fi
            
            if [ ! -e $tmpSERVER_DIR/resources/security ]; then
                mkdir -p $tmpSERVER_DIR/resources/security
            fi
            echo "server dir is $tmpSERVER_DIR"
            echo "Creating Key file"
            echo "${JAVA_HOME}/bin/keytool -genkey -alias RSA_2048 -keyalg RSA -keysize 2048 -dname CN=example -keystore ${tmpSERVER_DIR}/resources/security/key.jks -storepass Liberty -keypass ${SSL_PASSWORD}"
            ${JAVA_HOME}/bin/keytool -genkey -alias RSA_2048 -keyalg RSA -keysize 2048 -dname CN=example -keystore ${tmpSERVER_DIR}/resources/security/key.jks -storepass Liberty -keypass ${SSL_PASSWORD}

            echo "${JAVA_HOME}/bin/keytool -genkey -alias RSA_3072 -keyalg RSA -keysize 3072 -dname CN=example -keystore ${tmpSERVER_DIR}/resources/security/key.jks -storepass Liberty -keypass ${SSL_PASSWORD}"
            ${JAVA_HOME}/bin/keytool -genkey -alias RSA_3072 -keyalg RSA -keysize 3072 -dname CN=example -keystore ${tmpSERVER_DIR}/resources/security/key.jks -storepass Liberty -keypass ${SSL_PASSWORD}

            echo "${JAVA_HOME}/bin/keytool -genkey -alias ECDSA_256 -sigalg SHA256withECDSA -keyalg EC -keysize 256 -dname CN=example -keystore ${tmpSERVER_DIR}/resources/security/key.jks -storepass Liberty -keypass ${SSL_PASSWORD}"
            ${JAVA_HOME}/bin/keytool -genkey -alias ECDSA_256 -sigalg SHA256withECDSA -keyalg EC -keysize 256 -dname CN=example -keystore ${tmpSERVER_DIR}/resources/security/key.jks -storepass Liberty -keypass ${SSL_PASSWORD}  
            
            STAF_CMD="STAF local fs copy file ${tmpSERVER_DIR}/resources/security/key.jks tomachine ${CLIENT} todirectory ${CLIENT_WORK_DIR}"
            SSH_CMD="scp ${tmpSERVER_DIR}/resources/security/key.jks ${CLIENT_MACHINE_USER}@${CLIENT}:${CLIENT_WORK_DIR}"
            LOCAL_CMD="cp ${tmpSERVER_DIR}/resources/security/key.jks ${CLIENT_WORK_DIR}"
            runNetProtocolCmd 
            ;;
        AcmeAir)
            if [ -z "$MONGODB_DIR" ]; then
                echo "MONGODB_DIR is not defined. Defaulting to /java/perffarm/nodejs/mongo3"
                export MONGODB_DIR=/java/perffarm/nodejs/mongo3
            fi
            export ACMEAIR_CLIENT_WORK_DIR=${CLIENT_WORK_DIR}/AcmeAir
            ;;       
        Primitive)
            if [ -z "${PRIMITIVE}" ]; then
                echo "Primitive type not set - required for the Primitive scenario."
                exit
            fi
            ;;
        *)
            ;;
    esac
}

##
## Set client script environment variables and transfer the client scripts to the client
clientScriptSetup()
{
    printf '%s\n' "
.--------------------------
| Client Throughput Script Setup
"
  # set the path where client scripts are stored on host
  if [ -z "${CLIENT_SCRIPT_DIR}" ]; then
    case ${PLATFORM} in
      # Because staf runs under windows not cygwin, need to convert paths on windows
      CYGWIN)
        if [ ${NET_PROTOCOL} == "STAF" ]; then
            local CLIENT_SCRIPT_PATH=$(cygpath -m "${BENCHMARK_DIR}/resource/client_scripts")
        else 
            local CLIENT_SCRIPT_PATH=$(cygpath -u "${BENCHMARK_DIR}/resource/client_scripts")
        fi
        PATH_SEP=";"
        ;;

      *)
        local CLIENT_SCRIPT_PATH="${BENCHMARK_DIR}/resource/client_scripts"
        PATH_SEP=":"
        ;;
    esac
  else
    local CLIENT_SCRIPT_PATH=${CLIENT_SCRIPT_DIR}
    PATH_SEP=":"
  fi

  echoAndRunCmd "mkdir -p ${CLIENT_WORK_DIR}"

  echo "CLIENT_SCRIPT_PATH=${CLIENT_SCRIPT_PATH}"
  echo "CLIENT_WORK_DIR=${CLIENT_WORK_DIR}"
  echo "PATH_SEP=${PATH_SEP}"

  # transfer client scripts
  STAF_CMD="STAF local FS COPY DIRECTORY ${CLIENT_SCRIPT_PATH} TOMACHINE ${CLIENT} TODIRECTORY ${CLIENT_WORK_DIR} RECURSE"
  SSH_CMD="scp -r ${CLIENT_SCRIPT_PATH}/* ${CLIENT_MACHINE_USER}@${CLIENT}:${CLIENT_WORK_DIR}"
  LOCAL_CMD="cp -r ${CLIENT_SCRIPT_PATH}/* ${CLIENT_WORK_DIR}"
  runNetProtocolCmd -alwaysEcho
  STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND chmod -R 755 ${CLIENT_WORK_DIR}"
  SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} sudo chmod -R 755 ${CLIENT_WORK_DIR}"
  LOCAL_CMD="sudo chmod -R 755 ${CLIENT_WORK_DIR}"
  runNetProtocolCmd -alwaysEcho
  
  # check for successful transfer
  if [ "`echo $? | grep Done`" = "0" ]; then
    echo "!!! ERROR !!! Couldn\'t copy client scripts across."
    exit
  fi
}

# Configure the Liberty server's server.xml and optionally bootstrap.properties depending on scenario
# TODO: Once verified, not all scenarios set SERVER_XML_TMP?
scenarioSpecificServerSetup() {
    printf '%s\n' "
.--------------------------
| Scenario Specific Liberty Server Setup
"
    if [[ "${SCENARIO}" = DayTrader* ]]; then

        # set the default scenario specific server.xml script the Liberty server will use
        case ${SCENARIO} in
            DayTraderCrypto)
                export CLIENT_SERVERXML="dt3cryptoserver.xml"
                ;;

            DayTraderSSL)
                export CLIENT_SERVERXML="ssl_dt3server.xml"
                ;;

            DayTrader7|DayTrader7JDBC)
                export CLIENT_SERVERXML="dt7server.xml"
                echo "CLIENT_SERVERXML=dt7server.xml"
                ;;

            DayTrader7SSL)
                export CLIENT_SERVERXML="ssl_dt7server.xml"
                echo "CLIENT_SERVERXML=ssl_dt7server.xml"
                ;;
            *)
                export CLIENT_SERVERXML="dt3server.xml"
                ;;
        esac

		if [ "${DATABASE}" = "derby" ]; then
		
			echo "DATABASE=${DATABASE}"
			export CLIENT_SERVERXML="sufpdt7server.xml"
		
		fi
	
        # use default scenario specific server.xml if not given
        if [ -z "${SERVER_XML}" ]; then
            echo "Using ${CLIENT_SERVERXML} for Server xml"
        else
            echo "Server XML defined in job. Using ${SERVER_XML} instead of ${CLIENT_SERVERXML}"
            export CLIENT_SERVERXML="${SERVER_XML}"
        fi
        
        # replace server's default created server.xml with our custom one, along with adding bootstrap.properties
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_SERVERXML} ${SERVER_DIR}/server.xml"
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/db2bootstrap.properties ${SERVER_DIR}/bootstrap.properties"

        # replace the placeholders in server.xml
        echo "Setting the following values in server.xml:"

        if [[ ! "${SCENARIO}" = DayTrader7 ]]; then
            echo "APP_VERSION=${APP_VERSION}"
        fi

        echo "DB_NAME=${DB_NAME}"
        echo "DB_MACHINE=${DB_MACHINE}"
        echo "DB_PORT=${DB_PORT}"
        echo "DB_USR_NAME=${DB_USR_NAME}"
        echo "DB_PASSWORD"
        
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh ${SERVER_DIR}/server.xml
        fi
        
        local SERVER_XML_TMP=`sed "s/DB_NAME_HERE/${DB_NAME}/g" ${SERVER_DIR}/server.xml | sed "s/DB_MACHINE_HERE/${DB_MACHINE}/g" | sed "s/DB_PORT_HERE/${DB_PORT}/g" | sed "s/DB_USR_HERE/${DB_USR_NAME}/g" | sed "s/DB_PASSWORD_HERE/${DB_PASSWORD}/g"`

        # replace DayTrader3 App version
        if [[ ! "${SCENARIO}" = DayTrader7* ]]; then
            SERVER_XML_TMP=`echo "${SERVER_XML_TMP}" | sed "s/APP_VERSION_HERE/${APP_VERSION}/g"`
        fi

        # replace large thread pool placeholders in server.xml
        if [ "${LARGE_THREAD_POOL}" = "true" ]; then
            echo "Running with large thread pool, coreThreads=${CORE_THREADS}, maxThreads=${MAX_THREADS}, stealPolicy=${STEAL_POLICY}"
            local LARGE_THREAD_POOL_LINE="<executor name=\"LargeThreadPool\" id=\"default\" coreThreads=\"${CORE_THREADS}\" maxThreads=\"${MAX_THREADS}\" keepAlive=\"60s\" stealPolicy=\"${STEAL_POLICY}\" rejectedWorkPolicy=\"CALLER_RUNS\" />"
            SERVER_XML_TMP=`echo "${SERVER_XML_TMP}" | sed "s%</server>%${LARGE_THREAD_POOL_LINE}\n</server>%"`
        fi

        ###############
        #CHECK IF CORRECTLY REPLACED PLACEHOLDERS IN SERVER.XML
        ###############

        if [ ! -z "${SERVER_XML_TMP}" ]; then
            echo ${SERVER_XML_TMP} | tee ${SERVER_DIR}/server.xml
        else
            echo "Could not match required placeholders in server.xml. File before sed replace:"
            cat  ${SERVER_DIR}/server.xml
            exit 1
        fi
        
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh -toascii ${SERVER_DIR}/server.xml
        fi

        ###############
        #REPLACE CRYPTO PLACEHOLDERS
        ###############
        
        if [ "$SCENARIO" = "DayTraderSSL" ] || [ "$SCENARIO" = "DayTrader7SSL" ]; then
            local newXML=`cat ${SERVER_DIR}/server.xml | \
            sed "s/TARGET_PROTOCOL/${PROTOCOL}/g" | \
            sed "s/TARGET_CIPHER_SUITE/${CIPHER_SUITE}/g" | \
            sed "s/TARGET_KEY_ALIAS/${KEY_TYPE}_${KEY_LENGTH}/g"`
            echo "$newXML" | tee ${SERVER_DIR}/server.xml	
            
            # jmeter_ssl.properties is used in the client run.sh script
            cat ${BENCHMARK_DIR}/resource/template_jmeter.properties | \
            sed "s/TARGET_PROTOCOL/${PROTOCOL}/g" | \
            sed "s/SSL_SESSION_REUSE/${SSL_SESSION_REUSE}/g" >  ${BENCHMARK_DIR}/resource/client_scripts/jmeter_ssl.properties
            
            cat ${BENCHMARK_DIR}/resource/template_system.properties | \
            sed "s%KEYSTORE_LOC_HERE%${CLIENT_WORK_DIR}/key.jks%g" | \
            sed "s/PASSWORD_HERE/${SSL_PASSWORD}/g" >  ${BENCHMARK_DIR}/tmp/system.properties
            
        ###############
        #SEND JMETER SYSTEM PROP TO CLIENT MACHINE
        ###############
            #TODO: Looks like these 2 lines could be shared with DayTrader (no ssl). Leaving it for now. Need to test before doing so.
            local JMETER_BIN_DIR=`echo $JMETER_LOC|sed 's%/jmeter%%g'`
            STAF_CMD="STAF local fs copy file ${BENCHMARK_DIR}/tmp/system.properties tomachine ${CLIENT} todirectory ${JMETER_BIN_DIR}"
            SSH_CMD="scp ${BENCHMARK_DIR}/tmp/system.properties ${CLIENT_MACHINE_USER}@${CLIENT}:${JMETER_BIN_DIR}"
            LOCAL_CMD="cp ${BENCHMARK_DIR}/tmp/system.properties ${JMETER_BIN_DIR}"
            runNetProtocolCmd

        else
            if [ $THROUGHPUT_DRIVER = "jmeter" ]; then
                local JMETER_BIN_DIR=`echo $JMETER_LOC|sed 's%/jmeter%%g'`
                cp ${BENCHMARK_DIR}/resource/template_nosslsystem.properties ${BENCHMARK_DIR}/tmp/system.properties
                STAF_CMD="staf local fs copy file ${BENCHMARK_DIR}/tmp/system.properties tomachine ${CLIENT} todirectory ${JMETER_BIN_DIR}"
                SSH_CMD="scp ${BENCHMARK_DIR}/tmp/system.properties ${CLIENT_MACHINE_USER}@${CLIENT}:${JMETER_BIN_DIR}"
                LOCAL_CMD="cp ${BENCHMARK_DIR}/tmp/system.properties ${JMETER_BIN_DIR}"
                runNetProtocolCmd
            fi
        fi
    elif [ "${SCENARIO}" = "TradeApp" ]; then

        ###############
        #REPLACE DEFAULT SERVER.XML AND BOOTSTRAP.PROPERTIES WITH CUSTOM FILES
        ###############

        # make db2 server xml - overwrite always to ensure correct values
        export CLIENT_SERVERXML="db2server.xml"
        if [ -z "${SERVER_XML}" ]; then
            echo "Using ${CLIENT_SERVERXML} for Server xml"
        else
            echo "Server XML defined in job. Using ${SERVER_XML} instead of ${CLIENT_SERVERXML}"
            export CLIENT_SERVERXML = "${SERVER_XML}"
        fi
        
        
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_SERVERXML} ${SERVER_DIR}/server.xml"
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/db2bootstrap.properties ${SERVER_DIR}/bootstrap.properties"
        
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh ${SERVER_DIR}/server.xml
        fi
        
        ###############
        #REPLACE DATABASE INFO IN SERVER.XML
        ###############

        # replace the relevant details
        echo "Setting the following values in server.xml:"
        echo "DB_NAME=${DB_NAME}"
        echo "DB_MACHINE=${DB_MACHINE}"
        echo "DB_PORT=${DB_PORT}"
        echo "DB_USR_NAME=${DB_USR_NAME}"
        echo "DB_PASSWORD"
        local SERVER_XML_TMP=`sed "s/DB_NAME_HERE/${DB_NAME}/g" ${SERVER_DIR}/server.xml | sed "s/DB_MACHINE_HERE/${DB_MACHINE}/g" | sed "s/DB_PORT_HERE/${DB_PORT}/g" | sed "s/DB_USR_HERE/${DB_USR_NAME}/g" | sed "s/DB_PASSWORD_HERE/${DB_PASSWORD}/g"`

        # if we are running java 80 we want to use the newer DB2 jars as the old ones get sun io errors.
        if [ `echo "${JDK}" | grep -E "p.{4}80"` ]; then
            SERVER_XML_TMP=`echo "${SERVER_XML_TMP}" | sed "s%\\\${shared.resource.dir}/db2%\\\${shared.resource.dir}/db2new%"`
        fi

        ###############
        #REPLACE LARGE THREAD POOL INFO IN SERVER.XML
        ###############

        if [ "${LARGE_THREAD_POOL}" = "true" ]; then
            echo "Running with large thread pool, coreThreads=${CORE_THREADS}, maxThreads=${MAX_THREADS}, stealPolicy=${STEAL_POLICY}"
            local LARGE_THREAD_POOL_LINE="<executor name=\"LargeThreadPool\" id=\"default\" coreThreads=\"${CORE_THREADS}\" maxThreads=\"${MAX_THREADS}\" keepAlive=\"60s\" stealPolicy=\"${STEAL_POLICY}\" rejectedWorkPolicy=\"CALLER_RUNS\" />"
            SERVER_XML_TMP=`echo "${SERVER_XML_TMP}" | sed "s%</server>%${LARGE_THREAD_POOL_LINE}\n</server>%"`
        fi

        ###############
        #CHECK IF CORRECTLY REPLACED PLACEHOLDERS IN SERVER.XML
        ###############

        if [ ! -z "${SERVER_XML_TMP}" ]; then
            echo ${SERVER_XML_TMP} | tee ${SERVER_DIR}/server.xml
        else
            echo "Could not match required placeholders in server.xml. File before sed replace:"
            cat  ${SERVER_DIR}/server.xml
            exit
        fi
        
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh -toascii ${SERVER_DIR}/server.xml
        fi
        
    elif [ "${SCENARIO}" = "TradeAppDerby" ]; then

        ###############
        #REPLACE DEFAULT SERVER.XML AND BOOTSTRAP.PROPERTIES WITH CUSTOM FILES
        ###############

        # nothing to replace in the server.xml template for TradeAppDerby scenario
        export CLIENT_SERVERXML="derbyServer.xml"
        if [ -z "${SERVER_XML}" ]; then
            echo "Using ${CLIENT_SERVERXML} for Server xml"
        else
            echo "Server XML defined in job. Using ${SERVER_XML} instead of ${CLIENT_SERVERXML}"
            export CLIENT_SERVERXML = "${SERVER_XML}"
        fi
        
        
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_SERVERXML} ${SERVER_DIR}/server.xml"
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/derbyBootstrap.properties ${SERVER_DIR}/bootstrap.properties"
        
        
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh ${SERVER_DIR}/server.xml
        fi

        ###############
        #REPLACE LARGE THREAD POOL INFO IN SERVER.XML
        ###############

        if [ "${LARGE_THREAD_POOL}" = "true" ]; then
            echo "Running with large thread pool, coreThreads=${CORE_THREADS}, maxThreads=${MAX_THREADS}, stealPolicy=${STEAL_POLICY}"
            local LARGE_THREAD_POOL_LINE="<executor name=\"LargeThreadPool\" id=\"default\" coreThreads=\"${CORE_THREADS}\" maxThreads=\"${MAX_THREADS}\" keepAlive=\"60s\" stealPolicy=\"${STEAL_POLICY}\" rejectedWorkPolicy=\"CALLER_RUNS\" />"
            local SERVER_XML_TMP=`sed "s%</server>%${LARGE_THREAD_POOL_LINE}\n</server>%" ${SERVER_DIR}/server.xml`
        fi

        ###############
        #CHECK IF CORRECTLY REPLACED PLACEHOLDERS IN SERVER.XML
        ###############

        if [ ! -z "${SERVER_XML_TMP}" ]; then
            echo ${SERVER_XML_TMP} | tee ${SERVER_DIR}/server.xml
        else
            echo "Could not match required placeholders in server.xml. File before sed replace:"
            cat  ${SERVER_DIR}/server.xml
            exit
        fi


        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh -toascii ${SERVER_DIR}/server.xml
        fi
        
    elif [ "${SCENARIO}" = "Primitive" ]; then

        ###############
        #REPLACE DEFAULT SERVER.XML AND BOOTSTRAP.PROPERTIES WITH CUSTOM FILES
        ###############

        # nothing to replace in the server.xml template for Primitive scenario
        export CLIENT_SERVERXML="primativeServer.xml"
        if [ -z "${SERVER_XML}" ]; then
            echo "Using ${CLIENT_SERVERXML} for Server xml"
        else
            echo "Server XML defined in job. Using ${SERVER_XML} instead of ${CLIENT_SERVERXML}"
            export CLIENT_SERVERXML="${SERVER_XML}"
        fi
        
        
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_SERVERXML} ${SERVER_DIR}/server.xml"
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/primitiveBootstrap.properties  ${SERVER_DIR}/bootstrap.properties"
        
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh ${SERVER_DIR}/server.xml
        fi
        
        ###############
        #REPLACE LARGE THREAD POOL INFO IN SERVER.XML
        ###############

        if [ "${LARGE_THREAD_POOL}" = "true" ]; then
            echo "Running with large thread pool, coreThreads=${CORE_THREADS}, maxThreads=${MAX_THREADS}, stealPolicy=${STEAL_POLICY}"
            local LARGE_THREAD_POOL_LINE="<executor name=\"LargeThreadPool\" id=\"default\" coreThreads=\"${CORE_THREADS}\" maxThreads=\"${MAX_THREADS}\" keepAlive=\"60s\" stealPolicy=\"${STEAL_POLICY}\" rejectedWorkPolicy=\"CALLER_RUNS\" />"
            local SERVER_XML_TMP=`sed "s%</server>%${LARGE_THREAD_POOL_LINE}\n</server>%" ${SERVER_DIR}/server.xml`
        fi

        ###############
        #CHECK IF CORRECTLY REPLACED PLACEHOLDERS IN SERVER.XML
        ###############

        if [ ! -z "${SERVER_XML_TMP}" ]; then
            echo ${SERVER_XML_TMP} | tee ${SERVER_DIR}/server.xml
        else
            echo "Could not match required placeholders in server.xml. File before sed replace:"
            cat  ${SERVER_DIR}/server.xml
            exit
        fi
        if [ "$PLATFORM" = "OS/390" ]; then
            ${BENCHMARK_DIR}/bin/encode.sh -toascii ${SERVER_DIR}/server.xml
        fi

    elif [ "${SCENARIO}" = "JMS" ]; then

        ###############
        #REPLACE DEFAULT SERVER.XML AND BOOTSTRAP.PROPERTIES WITH CUSTOM FILES
        ###############

        # nothing to replace in the server.xml template for JMS scenario
        export CLIENT_SERVERXML="jmsServer.xml"
        if [ -z "${SERVER_XML}" ]; then
            echo "Using ${CLIENT_SERVERXML} for Server xml"
        else
            echo "Server XML defined in job. Using ${SERVER_XML} instead of ${CLIENT_SERVERXML}"
            export CLIENT_SERVERXML="${SERVER_XML}"
        fi
        
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_SERVERXML} ${SERVER_DIR}/server.xml"
        # not actually used for anything, for now
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/jmsBootstrap.properties  ${SERVER_DIR}/bootstrap.properties"

        ###############
        #ADD JMS APP TO SERVER'S DROPINS DIR
        ###############

        # Copy JMS app into dropins folder
        echoAndRunCmd "mkdir ${SERVER_DIR}/dropins"
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/JMSApp.war ${SERVER_DIR}/dropins/JMSApp.war"

    elif [ "${SCENARIO}" = "AcmeAir" ]; then

        ###############
        #REPLACE DEFAULT SERVER.XML AND BOOTSTRAP.PROPERTIES WITH CUSTOM FILES
        ###############

        export CLIENT_SERVERXML="acmeair_server.xml"
        if [ -z "${SERVER_XML}" ]; then
            echo "Using ${CLIENT_SERVERXML} for Server xml"
        else
            echo "Server XML defined in job. Using ${SERVER_XML} instead of ${CLIENT_SERVERXML}"
            export CLIENT_SERVERXML="${SERVER_XML}"
        fi
        
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/${CLIENT_SERVERXML} ${SERVER_DIR}/server.xml"
        
        echoAndRunCmd "cp ${BENCHMARK_DIR}/resource/acmeairBootstrap.properties  ${SERVER_DIR}/bootstrap.properties"
        
        ###############
        #REPLACE DATABASE INFO IN SERVER.XML
        ###############

        local SERVER_XML_TMP=`sed "s%MONGO_SERVER_HERE%$DB_MACHINE%" ${SERVER_DIR}/server.xml`

        ###############
        #CHECK IF CORRECTLY REPLACED PLACEHOLDERS IN SERVER.XML
        ###############

        if [ ! -z "${SERVER_XML_TMP}" ]; then
            echo ${SERVER_XML_TMP} | tee ${SERVER_DIR}/server.xml
        else
            echo "Could not match required placeholders in server.xml. File before sed replace:"
            cat  ${SERVER_DIR}/server.xml
            exit
        fi

        ###############
        #START MONGODB
        ###############

        # TODO: Once verified, use staf instead?
        ssh $DB_MACHINE ${ACMEAIR_CLIENT_WORK_DIR}/mongodb.sh start ${MONGODB_DIR}
    fi	
}
##
## Set the variables to be exported for client thorughput script, database restoration script
## This is required when NET_PROTOCOL is set to LOCAL
localSpecificEnvSetup() 
{
    printf '%s\n' "
.--------------------------
| exporting Local Specific Environment variables
"   
    export NET_PROTOCOL=${NET_PROTOCOL}; 
    export PROFILING_TOOL=${PROFILING_TOOL}; 
    export LIBERTY_SERVERDIR=${SERVER_DIR};
    export THROUGHPUT_DRIVER=${THROUGHPUT_DRIVER}; 
    export SERVER_PID=${SERVER_PID};
    export MEASURES=${MEASURES}; 
    export WARMUPS=${WARMUPS};
    export SINGLE_CLIENT_WARMUPS=${SINGLE_CLIENT_WARMUP}; 
    export MEASURE_TIME=${MEASURE_TIME}; 
    export WARMUP_TIME=${WARMUP_TIME};
    export DB_HOST=${DB_MACHINE}; 
    export DB2_HOME=${DB2_HOME}; 
    export DB_NAME=${DB_NAME}; 
    export WAS_HOST=${LIBERTY_HOST}; 
    export WAS_PORT=${LIBERTY_PORT}; 
    export CLIENT=${CLIENT}; 
    export DB_SERVER_WORKDIR=${DB_SERVER_WORKDIR}; 
    export SERVER_WORKDIR=${BENCHMARK_DIR}/tmp; 
    export PRIMITIVE=${PRIMITIVE}; 
    export SCENARIO=${SCENARIO}; 
    export NUM_CPUS=${NUM_CPUS}; 
    export JMETER_LOC=${JMETER_LOC}; 
    export JMETER_INSTANCES=${JMETER_INSTANCES}; 
    export SR=${SSL_SESSION_REUSE}; 
    export CLIENT_MACHINE_USER=${CLIENT_MACHINE_USER}; 
    export HOST_MACHINE_USER=${HOST_MACHINE_USER}; 
    export DB_MACHINE_USER=${DB_MACHINE_USER}; 
    export CLIENT_WORK_DIR=${CLIENT_WORK_DIR};
}
##
## Set the options relating to large thread pool based on scenario given
largeThreadPoolSetup()
{
    printf '%s\n' "
.--------------------------
| Large Thread Pool Setup
"
    case ${SCENARIO} in
        TradeApp)
            if [ -z "${CORE_THREADS}" ]; then
                echo "Optional CORE_THREADS not set. Setting to '40'"
                CORE_THREADS="40"
            fi
            if [ -z "${MAX_THREADS}" ]; then
                echo "Optional MAX_THREADS not set. Setting to '50'"
                MAX_THREADS="50"
            fi
            if [ -z "${STEAL_POLICY}" ]; then
                echo "Optional STEAL_POLICY not set. Setting to 'LOCAL'"
                STEAL_POLICY="LOCAL"
            fi
            ;;

        DayTraderSSL|DayTrader7SSL)
            if [ -z "${CORE_THREADS}" ]; then
                echo "Optional CORE_THREADS not set. Setting to '40'"
                CORE_THREADS="40"
            fi
            if [ -z "${MAX_THREADS}" ]; then
                echo "Optional MAX_THREADS not set. Setting to '50'"
                MAX_THREADS="50"
            fi
            if [ -z "${STEAL_POLICY}" ]; then
                echo "Optional STEAL_POLICY not set. Setting to 'LOCAL'"
                STEAL_POLICY="LOCAL"
            fi
            ;;

        DayTrader|DayTraderJDBC|DayTrader7|DayTrader7JDBC)
            if [ -z "${CORE_THREADS}" ]; then
                echo "Optional CORE_THREADS not set. Setting to '40'"
                CORE_THREADS="40"
            fi
            if [ -z "${MAX_THREADS}" ]; then
                echo "Optional MAX_THREADS not set. Setting to '50'"
                MAX_THREADS="50"
            fi
            if [ -z "${STEAL_POLICY}" ]; then
                echo "Optional STEAL_POLICY not set. Setting to 'LOCAL'"
                STEAL_POLICY="LOCAL"
            fi
            ;;
            
        DayTraderRU)
            if [ -z "${CORE_THREADS}" ]; then
                echo "Optional CORE_THREADS not set. Setting to '40'"
                CORE_THREADS="40"
            fi
            if [ -z "${MAX_THREADS}" ]; then
                echo "Optional MAX_THREADS not set. Setting to '50'"
                MAX_THREADS="50"
            fi
            if [ -z "${STEAL_POLICY}" ]; then
                echo "Optional STEAL_POLICY not set. Setting to 'LOCAL'"
                STEAL_POLICY="LOCAL"
            fi
            ;;

        DayTraderCrypto)
            if [ -z "${CORE_THREADS}" ]; then
                echo "Optional CORE_THREADS not set. Setting to '40'"
                CORE_THREADS="40"
            fi
            if [ -z "${MAX_THREADS}" ]; then
                echo "Optional MAX_THREADS not set. Setting to '50'"
                MAX_THREADS="50"
            fi
            if [ -z "${STEAL_POLICY}" ]; then
                echo "Optional STEAL_POLICY not set. Setting to 'LOCAL'"
                STEAL_POLICY="LOCAL"
            fi
            ;;

        TradeAppDerby)
            if [ -z "${CORE_THREADS}" ]; then
                echo "Optional CORE_THREADS not set. Setting to '40'"
                CORE_THREADS="40"
            fi
            if [ -z "${MAX_THREADS}" ]; then
                echo "Optional MAX_THREADS not set. Setting to '50'"
                MAX_THREADS="50"
            fi
            if [ -z "${STEAL_POLICY}" ]; then
                echo "Optional STEAL_POLICY not set. Setting to 'LOCAL'"
                STEAL_POLICY="LOCAL"
            fi
            ;;

        Primitive)
            if [ -z "${CORE_THREADS}" ]; then
                echo "Optional CORE_THREADS not set. Setting to '10'"
                CORE_THREADS="10"
            fi
            if [ -z "${MAX_THREADS}" ]; then
                echo "Optional MAX_THREADS not set. Setting to '15'"
                MAX_THREADS="15"
            fi
            if [ -z "${STEAL_POLICY}" ]; then
                echo "Optional STEAL_POLICY not set. Setting to 'LOCAL'"
                STEAL_POLICY="LOCAL"
            fi
            ;;

        JMS)
            # Nothing specific to do
            ;;

        *)
            # ?? Shouldn't get here
            ;;
    esac
    echo "CORE_THREADS=${CORE_THREADS}"
    echo "MAX_THREADS=${MAX_THREADS}"
    echo "STEAL_POLICY=${STEAL_POLICY}"
}

##
## Start the client script on the client machine. This will initiate client process (ex. start jmeter testing)
# TODO: Once verified why client needs tprof set
runClientScript()
{
    printf '%s\n' "
.--------------------------
| Running Client Throughput Script
"
  if [ "$SCENARIO" = "DayTraderSSL" ] || [ "$SCENARIO" = "DayTrader7SSL" ]; then
    STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND sh ${CLIENT_WORK_DIR}/client_throughput_start.sh ENV DB_MACHINE_USER=${DB_MACHINE_USER} ENV HOST_MACHINE_USER=${HOST_MACHINE_USER} ENV CLIENT_MACHINE_USER=${CLIENT_MACHINE_USER} ENV NET_PROTOCOL=${NET_PROTOCOL} ENV PROFILING_TOOL=${PROFILING_TOOL} ENV LIBERTY_SERVERDIR=${SERVER_DIR} ENV THROUGHPUT_DRIVER=${THROUGHPUT_DRIVER} ENV SERVER_PID=${SERVER_PID} ENV MEASURES=${MEASURES} ENV WARMUPS=${WARMUPS} ENV SINGLE_CLIENT_WARMUPS=${SINGLE_CLIENT_WARMUP} ENV MEASURE_TIME=${MEASURE_TIME} ENV WARMUP_TIME=${WARMUP_TIME} ENV DB_HOST=${DB_MACHINE} ENV DB2_HOME=${DB2_HOME} ENV DB_NAME=${DB_NAME} ENV WAS_HOST=${LIBERTY_HOST} ENV WAS_PORT=${LIBERTY_PORT} ENV CLIENT=${CLIENT} ENV CLIENT_WORK_DIR=${CLIENT_WORK_DIR} ENV DB_SERVER_WORKDIR=${DB_SERVER_WORKDIR} ENV SERVER_WORKDIR=${BENCHMARK_DIR}/tmp ENV PRIMITIVE=${PRIMITIVE} ENV SCENARIO=${SCENARIO} ENV NUM_CPUS=${NUM_CPUS} ENV JMETER_LOC=${JMETER_LOC} ENV JMETER_INSTANCES=${JMETER_INSTANCES} ENV SR=${SSL_SESSION_REUSE} WORKDIR ${CLIENT_WORK_DIR} STDERRTOSTDOUT RETURNSTDOUT WAIT ${MAX_CLIENT_WAIT}"
    SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} sh -c \"export DB_MACHINE_USER=${DB_MACHINE_USER}; export HOST_MACHINE_USER=${HOST_MACHINE_USER}; export CLIENT_MACHINE_USER=${CLIENT_MACHINE_USER}; export NET_PROTOCOL=${NET_PROTOCOL}; export PROFILING_TOOL=${PROFILING_TOOL}; export LIBERTY_SERVERDIR=${SERVER_DIR}; export THROUGHPUT_DRIVER=${THROUGHPUT_DRIVER}; export SERVER_PID=${SERVER_PID}; export MEASURES=${MEASURES}; export WARMUPS=${WARMUPS}; export SINGLE_CLIENT_WARMUPS=${SINGLE_CLIENT_WARMUP}; export MEASURE_TIME=${MEASURE_TIME}; export WARMUP_TIME=${WARMUP_TIME}; export DB_HOST=${DB_MACHINE}; export DB2_HOME=${DB2_HOME}; export DB_NAME=${DB_NAME}; export WAS_HOST=${LIBERTY_HOST}; export WAS_PORT=${LIBERTY_PORT}; export CLIENT=${CLIENT}; export CLIENT_WORK_DIR=${CLIENT_WORK_DIR} export DB_SERVER_WORKDIR=${DB_SERVER_WORKDIR}; export SERVER_WORKDIR=${BENCHMARK_DIR}/tmp; export PRIMITIVE=${PRIMITIVE}; export SCENARIO=${SCENARIO}; export NUM_CPUS=${NUM_CPUS}; export JMETER_LOC=${JMETER_LOC}; export JMETER_INSTANCES=${JMETER_INSTANCES}; export SR=${SSL_SESSION_REUSE}; timeout ${MAX_CLIENT_WAIT} ${CLIENT_WORK_DIR}/client_throughput_start.sh \" 2>&1 & wait"
    LOCAL_CMD="${CLIENT_WORK_DIR}/client_throughput_start.sh 2>&1"
    runNetProtocolCmd -alwaysEcho    
  elif [ "$SCENARIO" = "AcmeAir" ]; then
    
    # Load the database with customers 
    STAF_CMD="STAF ${DB_MACHINE} PROCESS START SHELL COMMAND sh ${ACMEAIR_CLIENT_WORK_DIR}/loaddb.sh ${LIBERTY_HOST} ${LIBERTY_PORT} true STDERRTOSTDOUT RETURNSTDOUT WAIT"
	SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_MACHINE} ${ACMEAIR_CLIENT_WORK_DIR}/loaddb.sh ${LIBERTY_HOST} ${LIBERTY_PORT} true 2>&1 & wait"
    LOCAL_CMD="${ACMEAIR_CLIENT_WORK_DIR}/loaddb.sh ${LIBERTY_HOST} ${LIBERTY_PORT} true 2>&1"
    runNetProtocolCmd -alwaysEcho

    STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND rm ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log STDERRTOSTDOUT RETURNSTDOUT WAIT" 
    SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} rm ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log 2>&1 & wait"
    LOCAL_CMD="rm ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log 2>&1"
    runNetProtocolCmd -alwaysEcho

    STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND ${JMETER_LOC} -Jdrivers=10 -Jhost=${LIBERTY_HOST} -Jport=${LIBERTY_PORT} -n -t ${ACMEAIR_CLIENT_WORK_DIR}/AcmeAir_java.jmx -p ${ACMEAIR_CLIENT_WORK_DIR}/acmeair.properties -l ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log > jmeter.out 2>&1 STDERRTOSTDOUT RETURNSTDOUT WAIT"
    SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} ${JMETER_LOC} -Jdrivers=10 -Jhost=${LIBERTY_HOST} -Jport=${LIBERTY_PORT} -n -t ${ACMEAIR_CLIENT_WORK_DIR}/AcmeAir_java.jmx -p ${ACMEAIR_CLIENT_WORK_DIR}/acmeair.properties -l ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log > jmeter.out 2>&1 & wait"
    LOCAL_CMD="/java/perffarm/Jmeter/bin/jmeter -Jdrivers=10 -Jhost=${LIBERTY_HOST} -Jport=${LIBERTY_PORT} -n -t ${ACMEAIR_CLIENT_WORK_DIR}/AcmeAir_java.jmx -p ${ACMEAIR_CLIENT_WORK_DIR}/acmeair.properties -l ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log > jmeter.out 2>&1"
	runNetProtocolCmd -alwaysEcho
    
    STAF_CMD="STAF ${CLIENT} FS COPY FILE ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log TOMACHINE ${LIBERTY_HOST} TODIRECTORY ${BENCHMARK_DIR}/tmp"
    SSH_CMD="scp ${CLIENT_MACHINE_USER}@${CLIENT}:${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log ${HOST_MACHINE_USER}@${LIBERTY_HOST}:${BENCHMARK_DIR}/tmp"
    LOCAL_CMD="cp ${ACMEAIR_CLIENT_WORK_DIR}/jmeter.log ${BENCHMARK_DIR}/tmp"
    runNetProtocolCmd -alwaysEcho
  else
    STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND bash ${CLIENT_WORK_DIR}/client_throughput_start.sh ENV DB_MACHINE_USER=${DB_MACHINE_USER} ENV HOST_MACHINE_USER=${HOST_MACHINE_USER} ENV CLIENT_MACHINE_USER=${CLIENT_MACHINE_USER} ENV NET_PROTOCOL=${NET_PROTOCOL} ENV JMETER_LOC=${JMETER_LOC} ENV LIBERTY_SERVERDIR=${SERVER_DIR} ENV PROFILING_TOOL=${PROFILING_TOOL} ENV THROUGHPUT_DRIVER=${THROUGHPUT_DRIVER} ENV SCENARIO=${SCENARIO} ENV SERVER_PID=${SERVER_PID} ENV MEASURES=${MEASURES} ENV WARMUPS=${WARMUPS} ENV SINGLE_CLIENT_WARMUPS=${SINGLE_CLIENT_WARMUP} ENV MEASURE_TIME=${MEASURE_TIME} ENV WARMUP_TIME=${WARMUP_TIME} ENV DB_HOST=${DB_MACHINE} ENV DB2_HOME=${DB2_HOME} ENV DB_NAME=${DB_NAME} ENV WAS_HOST=${LIBERTY_HOST} ENV WAS_PORT=${LIBERTY_PORT} ENV CLIENT=${CLIENT} ENV CLIENT_WORK_DIR=${CLIENT_WORK_DIR} ENV DB_SERVER_WORKDIR=${DB_SERVER_WORKDIR} ENV SERVER_WORKDIR=${BENCHMARK_DIR}/tmp ENV PRIMITIVE=${PRIMITIVE} ENV NUM_CPUS=${NUM_CPUS} WORKDIR ${CLIENT_WORK_DIR} STDERRTOSTDOUT RETURNSTDOUT WAIT ${MAX_CLIENT_WAIT}"
    SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} bash -c \"export DB_MACHINE_USER=${DB_MACHINE_USER}; export HOST_MACHINE_USER=${HOST_MACHINE_USER}; export CLIENT_MACHINE_USER=${CLIENT_MACHINE_USER}; export NET_PROTOCOL=${NET_PROTOCOL}; export JMETER_LOC=${JMETER_LOC}; export LIBERTY_SERVERDIR=${SERVER_DIR}; export PROFILING_TOOL=${PROFILING_TOOL}; export THROUGHPUT_DRIVER=${THROUGHPUT_DRIVER} ; export SCENARIO=${SCENARIO}; export SERVER_PID=${SERVER_PID}; export MEASURES=${MEASURES}; export WARMUPS=${WARMUPS}; export SINGLE_CLIENT_WARMUPS=${SINGLE_CLIENT_WARMUP}; export MEASURE_TIME=${MEASURE_TIME}; export WARMUP_TIME=${WARMUP_TIME}; export DB_HOST=${DB_MACHINE}; export DB2_HOME=${DB2_HOME}; export DB_NAME=${DB_NAME}; export WAS_HOST=${LIBERTY_HOST}; export WAS_PORT=${LIBERTY_PORT}; export CLIENT=${CLIENT}; export CLIENT_WORK_DIR=${CLIENT_WORK_DIR} export DB_SERVER_WORKDIR=${DB_SERVER_WORKDIR}; export SERVER_WORKDIR=${BENCHMARK_DIR}/tmp; export PRIMITIVE=${PRIMITIVE}; export NUM_CPUS=${NUM_CPUS}; timeout ${MAX_CLIENT_WAIT} ${CLIENT_WORK_DIR}/client_throughput_start.sh \" 2>&1 & wait"
    LOCAL_CMD="timeout ${MAX_CLIENT_WAIT} ${CLIENT_WORK_DIR}/client_throughput_start.sh 2>&1"
    runNetProtocolCmd -alwaysEcho
  fi
}

## Start the database process on the database machine and restore the original database used for testing
runDatabaseRestoreScript()
{
    printf '%s\n' "
.--------------------------
| Restoring Database
"
    STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND sh ${CLIENT_WORK_DIR}/database_start_and_restore.sh ENV NET_PROTOCOL=${NET_PROTOCOL} ENV DB_HOST=${DB_MACHINE} ENV DB2_HOME=${DB2_HOME} ENV DB_NAME=${DB_NAME} ENV DB_SERVER_WORKDIR=${DB_SERVER_WORKDIR} STDERRTOSTDOUT RETURNSTDOUT WAIT ${MAX_CLIENT_WAIT}"
    SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} sh -c \"export MACHINE_USER=root; export NET_PROTOCOL=${NET_PROTOCOL}; export DB_HOST=${DB_MACHINE}; export DB2_HOME=${DB2_HOME}; export DB_NAME=${DB_NAME}; export DB_SERVER_WORKDIR=${DB_SERVER_WORKDIR}; timeout ${MAX_CLIENT_WAIT} ${CLIENT_WORK_DIR}/database_start_and_restore.sh\" 2>&1 & wait"
    LOCAL_CMD="timeout ${MAX_CLIENT_WAIT} ${CLIENT_WORK_DIR}/database_start_and_restore.sh 2>&1"
    runNetProtocolCmd -alwaysEcho
}

##
## Use the resulting files from client and database to produce throughput results
parseResults()
{
    printf '%s\n' "
.--------------------------
| Parsing Throughput Results from Client and Database
"
  # parse the data
  #	Store the parsed results in a file, which we can then test to see if the throughput parsing worked.

  # Patch code to match previous codework when SSH & LOCAL pathway is used for windows
  if [ "${NET_PROTOCOL}" != "STAF" ] && [ "${PLATOFRM}" = "CYGWIN" ]; then
    BENCHMARK_DIR=${BENCHMARK_ORIGINAL_DIR}
  fi

  # For windows need to convert the Java path to a cygwin path (could do other way around, they just need to match)
  local UNIX_PATH
  case ${PLATFORM} in
    CYGWIN)
      UNIX_PATH=$(cygpath -m "${BENCHMARK_DIR}")
      ;;
    *)
      UNIX_PATH="${BENCHMARK_DIR}"
      ;;
  esac
  
  if [ "$PLATFORM" = "OS/390" ]; then
    ${BENCHMARK_DIR}/bin/encode.sh -toascii ${BENCHMARK_DIR}/tmp/${SERVER_CPU_RESULTS} ${BENCHMARK_DIR}/tmp/${CLIENT_CPU_RESULTS} ${BENCHMARK_DIR}/tmp/${DB_CPU_RESULTS} ${BENCHMARK_DIR}/tmp/${RESULTS}
  fi

  #TODO: maybe for jenkins this is not needed. What is the raw format?
  #how jenkins is storing it, maybe write own parser
  
  local PARSE_CMD
  case $SCENARIO in
    TradeApp)
      PARSE_CMD="${JAVA_HOME}/bin/java -cp ${UNIX_PATH}/resource/parser/liberty_parser.jar${PATH_SEP}${UNIX_PATH}/resource/parser/combined_parser.jar com.ibm.libertyperf.parsers.TradeLiteParser ${UNIX_PATH}/tmp/${RESULTS} ${UNIX_PATH}/tmp/${CLIENT_CPU_RESULTS} ${UNIX_PATH}/tmp/${SERVER_CPU_RESULTS} ${UNIX_PATH}/tmp/${DB_CPU_RESULTS}"
      ;;
    DayTrader|DayTrader7|DayTraderJDBC|DayTrader7JDBC|DayTraderRU)
      PARSE_CMD="${JAVA_HOME}/bin/java -cp ${UNIX_PATH}/resource/parser/liberty_parser.jar${PATH_SEP}${UNIX_PATH}/resource/parser/combined_parser.jar com.ibm.libertyperf.parsers.TradeLiteParser ${BENCHMARK_DIR}/tmp/${RESULTS} ${BENCHMARK_DIR}/tmp/${CLIENT_CPU_RESULTS} ${BENCHMARK_DIR}/tmp/${SERVER_CPU_RESULTS} ${BENCHMARK_DIR}/tmp/${DB_CPU_RESULTS}"
      ;;
    DayTraderSSL|DayTrader7SSL)
      PARSE_CMD="${JAVA_HOME}/bin/java -cp ${UNIX_PATH}/resource/parser/liberty_parser.jar${PATH_SEP}${UNIX_PATH}/resource/parser/combined_parser.jar com.ibm.libertyperf.parsers.TradeLiteParser ${dt_ssl_result_files} ${UNIX_PATH}/tmp/${CLIENT_CPU_RESULTS} ${UNIX_PATH}/tmp/${SERVER_CPU_RESULTS} ${UNIX_PATH}/tmp/${DB_CPU_RESULTS}"
      ;;
    DayTraderCrypto)
      PARSE_CMD="${JAVA_HOME}/bin/java -cp ${UNIX_PATH}/resource/parser/liberty_parser.jar${PATH_SEP}${UNIX_PATH}/resource/parser/combined_parser.jar com.ibm.libertyperf.parsers.TradeLiteParser ${UNIX_PATH}/tmp/${RESULTS} ${UNIX_PATH}/tmp/${CLIENT_CPU_RESULTS} ${UNIX_PATH}/tmp/${SERVER_CPU_RESULTS} ${UNIX_PATH}/tmp/${DB_CPU_RESULTS}"
      ;;
    TradeAppDerby|Primitive|JMS)
      PARSE_CMD="${JAVA_HOME}/bin/java -cp ${UNIX_PATH}/resource/parser/liberty_parser.jar${PATH_SEP}${UNIX_PATH}/resource/parser/combined_parser.jar com.ibm.libertyperf.parsers.TradeLiteParser ${UNIX_PATH}/tmp/${RESULTS} ${UNIX_PATH}/tmp/${CLIENT_CPU_RESULTS} ${UNIX_PATH}/tmp/${SERVER_CPU_RESULTS}"
      ;;
    AcmeAir)
      local ACMEAIRTP=`cat ${BENCHMARK_DIR}/tmp/jmeter.log|awk -f resource/acmeair_score.awk`
      if [ "${ACMEAIRTP%.*}" -gt 0 ]; then
        echo "Run Successful"
      fi
      echo "Throughput: ${ACMEAIRTP}"
      PARSE_CMD="echo 'metric type=\"throughput\" '"
      ;;
  esac
	   

if [ -e "${UNIX_PATH}/resource/parser" ]; then
	echo "${PARSE_CMD}"
	# We do it like this, because otherwise the parse program also tries to parse "> parsedResuls.xml"
	echo "$(${PARSE_CMD})" > ${BENCHMARK_DIR}/tmp/parsedResults.xml
	if [ "${PLATFORM}" = "OS/390" ]; then
	  ${BENCHMARK_DIR}/bin/encode.sh ${RESULTS} results_bkup.txt
	  chmod u+x perSecondResults.sh.ebcdic
	  #PER_SECOND_CMD="perSecondResults.sh ${RESULTS}"
	  #echo "$(${PER_SECOND_CMD})"
	fi
	
	###############
	#CHECK PARSED RESULTS
	###############
	
	# Check that the results contain throughput data
	local RESULTS_CHECK=`grep -c 'metric type="throughput"' ${BENCHMARK_DIR}/tmp/parsedResults.xml`
	if [ "${RESULTS_CHECK}" = "0" ]; then
	  echo "Throughput results missing. Please check server logs for signs of an error."
	  echo "Storing logs directory"
	  echoAndRunCmd "cat ${SERVER_DIR}/logs/console.log"
	  storeFiles -d ${SERVER_DIR}/logs
	else
	  cat ${BENCHMARK_DIR}/tmp/parsedResults.xml
	fi
 
 else
 	echo "Printing all results files since there are no parsers binaries."
	echoAndRunCmd "cat ${BENCHMARK_DIR}/tmp/${RESULTS}"
	echoAndRunCmd "cat ${BENCHMARK_DIR}/tmp/${CLIENT_CPU_RESULTS}"
	echoAndRunCmd "cat ${BENCHMARK_DIR}/tmp/${SERVER_CPU_RESULTS}"
	echoAndRunCmd "cat ${BENCHMARK_DIR}/tmp/${DB_CPU_RESULTS}"
 fi

}


##
## Print scenario specific stats calculated from RESULTS
calculateScenarioSpecificStats()
{
    printf '%s\n' "
.--------------------------
| Calculating Scenario Specific Statistics
"
  echo "Scenario is ${SCENARIO}"
  if [[ "${SCENARIO}" = DayTrader* ]]; then
    local results
    echo "WARNING: The times quoted below do NOT include the 60 second 1 client warmup"
    if [ "$THROUGHPUT_DRIVER" = "iwl" ]; then	
      results=`cat ${BENCHMARK_DIR}/tmp/${RESULTS} |grep "clients 50/50"|awk {'print $28}'`
    else
      results=`cat ${BENCHMARK_DIR}/tmp/results_bkup.txt |grep "summary +"|awk {'print $7'}|sed 's%/s%%g'`
    fi

    ###############
    #CALCULATE AND PRINT MAX THROUGHPUT AND TIME TAKEN TO REACH
    ###############

    local i=0
    local max=0
    for result in $results
    do
        i=`echo $i+1|bc`
        if [[ ${result%.*} -ge $max ]]; then
            max=${result%.*}
            #TODO: why are we multiplying by 6? The answer will be found when looking at the client run.sh script
            local maxtime=`echo "$i*6"|bc`
        fi	

    done
    local totalrun=`echo "$i*6"|bc`
    #Percentage=`echo "(${maxtime}/${totalrun})*100"|bc`
    #TODO: can be made more efficient. If target gets to 0.9 but its on the 0.5 iteration, then will have to happen 4 more times. But we can eliminate these repeat calculations.
    echo "Max throughput of ${max} reached in ${maxtime} seconds total run=${totalrun}" #${Percentage}%"
    
    ###############
    #CALCULATE AND PRINT THROUGHPUT TARGETS AND TIME TAKEN TO REACH
    ###############
    
    for target in 0.5 0.6 0.7 0.8 0.9 0.95 0.98
    do
        local targetnumber=`echo "${target}*${max}"|bc`
        i=0
        for result in $results
        do
        i=`echo $i+1|bc`
            if [[ ${result%.*} -ge ${targetnumber%.*} ]]; then
                local timetarget=`echo "$i*6"|bc`
                local targetPerc=`echo "$target*100"|bc`
                echo "Target of ${targetPerc%.*}% - ${targetnumber%.*} - was reached in ${timetarget} seconds"
                break
            fi

        done
    done

    local currentTime
    if [[ "${SCENARIO}" = "DayTraderRU" ]]; then
      local pages=$results
      for limit in 60 120 300 600 1200
      do
          i=0
          for page in $pages
          do
          i=`echo $i+1|bc`
          currentTime=`echo "$i*6"|bc`
              if [[ ${currentTime%.*} -ge ${limit%.*} ]]; then
                  echo "Pages transfered at ${limit} seconds was ${page}"
                  break
              fi
    
          done
      done
      for target in 300000 600000 1000000 5000000 10000000
      do
          i=0
          for page in $pages
          do
          i=`echo $i+1|bc`
          currentTime=`echo "$i*6"|bc`
              if [[ ${page%.*} -ge ${target%.*} ]]; then
                  echo "Target of ${target} pages reached in ${currentTime}"
                  break
              fi
          done
      done
    else
      echo "This is not a rampup run. Page stats not easily determinable"
    fi
    local output=""
    for result in $results
        do		
        if [[ "$output" = "" ]]; then			#check to see if we're on the first iteration - if so, then don't add a comma
          output=${result%.*}
        else
          output="${output},${result%.*}"
        fi
        done
    echo "CSV: ${output}"

  else
    echo "Nothing to do"
  fi
}

##
## Delete the results generated from this benchmark run on the client machine
removeResultsFromClient()
{
    printf '%s\n' "
.--------------------------
| Remove Result Files from Client and Database
"
  # Remove these results files from the machines they came from
  echo "Removing old data"
  # delete the client results set

  STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND rm PARMS \"-f ${CLIENT_WORK_DIR}/${RESULTS}\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
  SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} rm -f ${CLIENT_WORK_DIR}/${RESULTS} 2>&1 & wait"
  LOCAL_CMD="rm -f ${CLIENT_WORK_DIR}/${RESULTS} 2>&1"
  runNetProtocolCmd -alwaysEcho
  # server data moved, was created in this dir so no copy left elsewhere to clean up
  if [ "$SCENARIO" = "DayTraderSSL" ] || [ "$SCENARIO" = "DayTrader7SSL" ]; then
        for clientIterator in 1 2 3
        do
          STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND rm PARMS \" -f ${CLIENT_WORK_DIR}/client${clientIterator}.txt\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
          SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} rm -f ${CLIENT_WORK_DIR}/client${clientIterator}.txt"
          LOCAL_CMD="rm -f ${CLIENT_WORK_DIR}/client${clientIterator}.txt"
          runNetProtocolCmd -alwaysEcho
        done
  fi
  # delete the client CPU data
  STAF_CMD="STAF ${CLIENT} PROCESS START SHELL COMMAND rm PARMS \"-f ${CLIENT_WORK_DIR}/${CLIENT_CPU_RESULTS}\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
  SSH_CMD="ssh ${CLIENT_MACHINE_USER}@${CLIENT} rm -f ${CLIENT_WORK_DIR}/${CLIENT_CPU_RESULTS} 2>&1 & wait"
  LOCAL_CMD="rm -f ${CLIENT_WORK_DIR}/${CLIENT_CPU_RESULTS} 2>&1"
  runNetProtocolCmd -alwaysEcho
  ###############
  #REMOVE CPU RESULTS DATA FROM DB MACHINE
  ###############

  for i in TradeApp  DayTrader DayTrader7 DayTraderJDBC DayTrader7JDBC DayTraderCrypto DayTraderHC DayTraderRU DayTraderSSL DayTrader7SSL
  do
    if [ "${SCENARIO}" = "${i}" ]; then
      # delete the db host CPU data
      STAF_CMD="STAF ${DB_MACHINE} PROCESS START SHELL COMMAND rm PARMS \"-f ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS}\" STDERRTOSTDOUT RETURNSTDOUT WAIT"
      SSH_CMD="ssh ${DB_MACHINE_USER}@${DB_MACHINE} rm -f ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} 2>&1 & wait"
      LOLAL_CMD="rm -f ${DB_SERVER_WORKDIR}/db_CPU_results.xml 2>&1"
      runNetProtocolCmd -alwaysEcho
      break
    fi
  done
}


##
## All files that will be stored are added to FILES_TO_STORE
includeFilesToStore()
{
    printf '%s\n' "
.--------------------------
| Adding Client and Database Result Files to Store
"
  # include raw data
  FILES_TO_STORE="${FILES_TO_STORE} ${SERVER_DIR}/logs/${LOG_FILE} ${BENCHMARK_DIR}/tmp/${RESULTS} ${BENCHMARK_DIR}/tmp/${CLIENT_CPU_RESULTS} ${BENCHMARK_DIR}/tmp/${SERVER_CPU_RESULTS} ${SERVER_DIR}/logs/messages*.log ${SERVER_DIR}/verbosejit*.txt* ${BENCHMARK_DIR}/tmp/results_bkup.txt ${BENCHMARK_DIR}/tmp/gcmvData.txt parsedResults.xml ${SERVER_DIR}/verbosegc*.xml*"
  if [ "$SCENARIO" = "DayTraderSSL" ] || [ "$SCENARIO" = "DayTrader7SSL" ]; then
      FILES_TO_STORE="${FILES_TO_STORE} ${dt_ssl_result_files}"
  fi

  case ${SCENARIO} in
        TradeApp|DayTrader|DayTrader7|DayTraderJDBC|DayTrader7JDBC|DayTraderCrypto|DayTraderHC|DayTraderRU|DayTraderSSL|DayTrader7SSL)
      FILES_TO_STORE="${FILES_TO_STORE} ${BENCHMARK_DIR}/tmp/${DB_CPU_RESULTS}"
      ;;

    AcmeAir)
      FILES_TO_STORE="$FILES_TO_STORE ${BENCHMARK_DIR}/tmp/jmeter.log"
      ;;
    
    *)
      ;;
  esac

  # include trace files
  if [[ "${PROFILING_TOOL}" = jprof* ]]; then
    pushd ${BENCHMARK_DIR}/tmp
    local TPROF_BACKUP_FILE=jprof_`date +%d%m%y-%H:%M:%S`.zip
    echo "zip $TPROF_BACKUP_FILE swtrace* *a2n* tprof* a2n.err *${SERVER_PID}* *log-* *post.msg*"
    zip $TPROF_BACKUP_FILE swtrace* *a2n* tprof* a2n.err *${SERVER_PID}* *log-* *post.msg*
    echo "Zipped up TPROF files in ${TPROF_BACKUP_FILE}"
    export FILES_TO_STORE="${FILES_TO_STORE} ${BENCHMARK_DIR}/tmp/${TPROF_BACKUP_FILE}"
    popd
  elif [[ "${PROFILING_TOOL}" = perf* ]]; then
	pushd ${BENCHMARK_DIR}/tmp
	echo "Adding perf files to the list of files to archive"
	
	export FILES_TO_STORE="${FILES_TO_STORE} ${BENCHMARK_DIR}/tmp/perf.data ${BENCHMARK_DIR}/tmp/perf_stat.txt /tmp/perf-${SERVER_PID}.map ${BENCHMARK_DIR}/tmp/perf_debug.tar.gz"
	popd
  fi
  
}


##
## Transfer all result files from client and database machines to the Liberty host machine
copyAllResultsToHost()
{
    printf '%s\n' "
.--------------------------
| Copying Result Files from Client and Database to Host
"
  # get the throughput data
  if [ "$SCENARIO" = "DayTraderSSL" ] || [ "$SCENARIO" = "DayTrader7SSL" ]; then
    dt_ssl_result_files=""
    for clientIterator in 1 2 3
      do
      STAF_CMD="STAF ${CLIENT} FS COPY FILE ${CLIENT_WORK_DIR}/client${clientIterator}.txt TOMACHINE ${LIBERTY_HOST} TODIRECTORY ${BENCHMARK_DIR}/tmp"
      SSH_CMD="scp ${CLIENT_MACHINE_USER}@${CLIENT}:${CLIENT_WORK_DIR}/client${clientIterator}.txt ${BENCHMARK_DIR}/tmp"
      LOCAL_CMD="cp ${CLIENT_WORK_DIR}/client${clientIterator}.txt ${BENCHMARK_DIR}/tmp"
      runNetProtocolCmd -alwaysEcho
      dt_ssl_result_files="${dt_ssl_result_files} ${BENCHMARK_DIR}/tmp/client${clientIterator}.txt"
    done
  else
    STAF_CMD="STAF ${CLIENT} FS COPY FILE ${CLIENT_WORK_DIR}/${RESULTS} TOMACHINE ${LIBERTY_HOST} TODIRECTORY ${BENCHMARK_DIR}/tmp"
    SSH_CMD="scp ${CLIENT_MACHINE_USER}@${CLIENT}:${CLIENT_WORK_DIR}/${RESULTS} ${BENCHMARK_DIR}/tmp"
    LOCAL_CMD="cp ${CLIENT_WORK_DIR}/${RESULTS} ${BENCHMARK_DIR}/tmp"
    runNetProtocolCmd -alwaysEcho
  fi

  # get the client CPU data
  STAF_CMD="STAF ${CLIENT} FS COPY FILE ${CLIENT_WORK_DIR}/${CLIENT_CPU_RESULTS} TOMACHINE ${LIBERTY_HOST} TODIRECTORY ${BENCHMARK_DIR}/tmp"
  SSH_CMD="scp ${CLIENT_MACHINE_USER}@${CLIENT}:${CLIENT_WORK_DIR}/${CLIENT_CPU_RESULTS} ${BENCHMARK_DIR}/tmp"
  LOCAL_CMD="cp ${CLIENT_WORK_DIR}/${CLIENT_CPU_RESULTS} ${BENCHMARK_DIR}/tmp"
  runNetProtocolCmd -alwaysEcho
  # server one is created in the working dir

  if [ "${SCENARIO}" = "TradeApp" ]; then
    # get the db host CPU data
    STAF_CMD="STAF ${DB_MACHINE} FS COPY FILE ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} TOMACHINE ${LIBERTY_HOST} TODIRECTORY ${BENCHMARK_DIR}/tmp"
    SSH_CMD="scp ${DB_MACHINE_USER}@${DB_MACHINE}:${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} ${BENCHMARK_DIR}/tmp"
    LOCAL_CMD="cp ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} ${BENCHMARK_DIR}/tmp"
    runNetProtocolCmd -alwaysEcho
  fi
  if [[ "${SCENARIO}" = DayTrader* ]]; then
    # get the db host CPU data
    STAF_CMD="STAF ${DB_MACHINE} FS COPY FILE ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} TOMACHINE ${LIBERTY_HOST} TODIRECTORY ${BENCHMARK_DIR}/tmp"
    SSH_CMD="scp ${DB_MACHINE_USER}@${DB_MACHINE}:${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} ${BENCHMARK_DIR}/tmp"
    LOCAL_CMD="cp ${DB_SERVER_WORKDIR}/${DB_CPU_RESULTS} ${BENCHMARK_DIR}/tmp"
    runNetProtocolCmd -alwaysEcho
  fi
  local tp_drivertmp=`echo $THROUGHPUT_DRIVER|awk {'print tolower($0)'}`
  if [[ "${tp_drivertmp}" = "jmeter" ]]; then
    STAF_CMD="STAF ${CLIENT} FS COPY FILE ${CLIENT_WORK_DIR}/results_bkup.txt TOMACHINE ${LIBERTY_HOST} TODIRECTORY ${BENCHMARK_DIR}/tmp"
    SSH_CMD="scp ${CLIENT_MACHINE_USER}@${CLIENT}:${CLIENT_WORK_DIR}/results_bkup.txt ${BENCHMARK_DIR}/tmp"
    LOCAL_CMD="cp ${CLIENT_WORK_DIR}/results_bkup.txt ${BENCHMARK_DIR}/tmp"
    runNetProtocolCmd -alwaysEcho
  fi
}


#######################################################################################
# BEGIN THROUGHPUT BENCHMARK
#######################################################################################

# Handle any script arguments given when running the benchmark
while [ ! $1 = "" ]; do
    case $1 in
        -help|--help)
            usage
            exit
            ;;
        *)
            echo "Invalid benchmark argument: ${1}"
            usage
            exit
            ;;
    esac
done

BENCHMARK_TYPE=THROUGHPUT

# Import the common utilities needed to run this benchmark
. "$(dirname $0)"/database_utils.sh

# Import the common utilities needed to run this benchmark
. "$(dirname $0)"/common_utils.sh

# Print Liberty header for script identification
printLibertyHeader

# Print Throughput header with script version
echo "Throughput Benchmark Launch Script for Liberty
###########################################################################

"

# echo the local time, helps match logs up if local time not correct
echo "Local date: `date`"
echo "Local time: $(perl $(dirname $0)/time.pl)"

setMachinePlatform
setBenchmarkTopLevelDir

if [ ! -z "$PROFILING_TOOL" ]; then
	supportedProfilingToolsValidation
fi

checkAndSetCommonEnvVars
checkAndSetThroughputEnvVars

###############
# Liberty Host Environment Setup
###############

# remove results files from previous runs
removeResultsFromClient
removeAllResultFilesOnHost

# Tmp folder stores any files created during the running of the benchmark
if [ ! -d "${BENCHMARK_DIR}/tmp" ]; then
  mkdir ${BENCHMARK_DIR}/tmp
fi

VALID_SCENARIOS=(
    "AcmeAir"
    "TradeApp"
    "TradeAppDerby"
    "Primitive"
    "DayTrader"
    "DayTrader7"
    "DayTraderJDBC"
    "DayTrader7JDBC"
    "DayTraderCrypto"
    "DayTraderRU"
    "DayTraderSSL"
    "DayTrader7SSL"
    "JMS"
)
scenarioValidation

jdkEnvSetup

if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
    # set shared class cache (SCC) name from given JVM args
    getSharedClassCacheName
    if [ -z "$SCCNAME" ]; then
        case ${SCENARIO} in
            AcmeAir)
                SCCNAME=liberty-acmeair-throughput-${JDK}
                ;;
            TradeApp)
                SCCNAME=liberty-tradeapp-throughput-${JDK}
                ;;
            TradeAppDerby)
                SCCNAME=liberty-tradeappderby-throughput-${JDK}
                ;;
            Primitive)
                SCCNAME=liberty-primitive-throughput-${JDK}
                ;;
            DayTrader)
                SCCNAME=liberty-dt-throughput-${JDK}
                ;;
            DayTrader7)
                SCCNAME=liberty-dt7-throughput-${JDK}
                ;;
            DayTraderJDBC)
                SCCNAME=liberty-dtjdbc-throughput-${JDK}
                ;;
            DayTrader7JDBC)
                SCCNAME=liberty-dt7jdbc-throughput-${JDK}
                ;;
            DayTraderCrypto)
                SCCNAME=liberty-dtcrypto-throughput-${JDK}
                ;;
            DayTraderRU)
                SCCNAME=liberty-dtru-throughput-${JDK}
                ;;
            DayTraderSSL)
                SCCNAME=liberty-dtssl-throughput-${JDK}
                ;;
            DayTrader7SSL)
                SCCNAME=liberty-dt7ssl-throughput-${JDK}
                ;;
            JMS)
                SCCNAME=liberty-jms-throughput-${JDK}
                ;;
            *)
                SCCNAME=liberty-throughput-${JDK}
                ;;
        esac
        echo "Couldn't work out SCCNAME - setting to ${SCCNAME}"
    else
        echo "SCCNAME worked out to be ${SCCNAME} from given JVM_ARGS"
    fi
    echo "SCCNAME: ${SCCNAME} will be used for destroying and printing the SCC"
fi



if [ "${LARGE_THREAD_POOL}" = "true" ] ; then
    largeThreadPoolSetup
fi

setLibertyServerDirs
scenarioSpecificEnvSetup
platformSpecificSetup

# set environment vars for this shell
setLocalEnv
printSensorInfo

###############
# Liberty Server Setup
###############

# perform server setup only if user has requested it
if [ "${NO_SETUP}" != "true" ]; then
  # Only create if it doesn't exist
  createLibertyServer
  scenarioSpecificServerSetup
  setBootstrapPropsLibertyPort
else
    echo "No setup - not changing server"
fi

# exit if user requested setup only
if [ "${SETUP_ONLY}" = "true" ]; then
    echo ""
    echo "SETUP_ONLY flag set. Setup complete. Exiting."
    exit
fi

configureDB

terminateRunningJavaProcs

# tprof will be done for Liberty server java process
if [ ! -z "$PROFILING_TOOL" ]; then
  addProfilingOptions
fi

# print liberty and jdk version
printAllApplicationVersionInfo

# display jvm.options
if [ -e ${SERVER_DIR}/jvm.options ]; then
  echo "jvm.options file exists. Contains:"
  cat ${SERVER_DIR}/jvm.options
fi

###############
# Pre Benchmark Cleanup
###############

# any server(s) from previous runs should not be running
terminateRunningLibertyServer

# only destroy SCC for cold (aka clean) runs, otherwise print SCC stats
if [ "${CLEAN_RUN}" = "true" ]; then
  setLibertyCleanRunOptions
  if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
    destroySharedClassCache
  fi
else
  echo "CLEAN_RUN=false. Workarea and shared class caches will be warm"
  CLEAN=""
  if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
    printSharedClassCacheInfo
  fi
fi
###############
# Env Variable setup for LOCAL run
# Setup 
##############

if [ ${NET_PROTOCOL} == "LOCAL" ]; then
    localSpecificEnvSetup
fi
###############
# Client Script Setup
###############

# The staf call that runs the client side script will wait for this number of seconds
let MAX_CLIENT_WAIT=MEASURES*MEASURE_TIME+MEASURES*10+WARMUPS*WARMUP_TIME+WARMUPS*10
let MAX_CLIENT_WAIT=MAX_CLIENT_WAIT+500
let MAX_CLIENT_WAIT=MAX_CLIENT_WAIT*1000
    
clientScriptSetup

###############
# Start and Restore Database
###############

if [ "${DATABASE}" != "derby" ]; then
	runDatabaseRestoreScript
fi
###############
# Pre Benchmark Measurement
###############

# get current HP being used for post benchmark analysis. Currenly only done for Linux platforms.
#TODO: Once verified, zos restartRMF enabled or disabled?
getPreBenchmarkHugePagesInUse

###############
# Start Liberty Server
###############

enableJitThreadUtilizationInfo

# call the startup function, allow 2 attempts
echoAndRunCmd "cd ${BENCHMARK_DIR}/tmp"
startLibertyServer 1
echoAndRunCmd "cd -"

# unset Jit stat variables so that future use of Java, like when using it for GCMV and other tools does not
# give ThreadUtilization output. (GCMV creates an error file which captures stderr. This ThreadUtilization output
# gives a false positive that a GCMV error has occurred.)
disableJitThreadUtilizationInfo

getRunningServerPID
libertyServerRunningCheck
printLibertyServerProcessCommand

# Must wait for liberty to finish starting up. 10s should be fine, since target is 5s
echoAndRunCmd "sleep 10"

###############
# Run Client Side Script
###############

if [ "$PLATFORM" = "OS/390" ]; then
  NUM_CPUS=`oeconsol "d m=cpu" | grep '[0-9][0-9A-F]  +' | tail -n 1 | awk '{print ($1 + 1)}'`
  echo "AppServer is running with $NUM_CPUS CPUs online"
  restartRMF ${MAX_CLIENT_WAIT}
fi

# Add extra time just in case

# By default, STAF uses milliseconds as time input for the wait command. Convert to ms.

#TODO: Once verified, if IWL throughput driver is twas specific remove from the client run.sh script
runClientScript
if [ "${SDK_SUPPORTS_SCC}" = "true" ]; then
    printSharedClassCacheInfo
fi
printSensorInfo

###############
# Parse Result Data
###############

# copy result data fom client (and db) to Liberty host
copyAllResultsToHost

parseResults
calculateScenarioSpecificStats

###############
# Post Benchmark Measurement
###############

calculateFootprint

###############
# Stop Liberty Server
###############

# Remove jvm args to stop the jit/verbose gc log files being overwritten.
# Order of precedence (from highest/rightmost to lowest/leftmost) given within Liberty "Launch Script" (Server):
#       OPENJ9_JAVA_OPTIONS, JVM_ARGS, JVM_OPTIONS_QUOTED (those within the jvm.options file) 
if [ `echo ${JVM_ARGS}|grep -c "verbosegclog"` -eq 0 ]; then
  echo "no verbosegc defined. Not changing args"
else	
  export JVM_ARGS="-Xverbosegclog:stopSerververbosegc.xml"
fi

###############
#REMOVE JVM.OPTIONS (OR RESTORE ORIGINAL IF PRESENT BEFORE)
###############

if [ "${OPTIONS_EXISTED}" = true ]; then
    if [ -e "${BENCHMARK_DIR}/tmp/backup_jvmoptions" ]; then
        mv ${BENCHMARK_DIR}/tmp/backup_jvmoptions ${SERVER_DIR}/jvm.options
    else
        echo "*** Error *** File ${BENCHMARK_DIR}/tmp/backup_jvmoptions should exist but it was not found"
    fi
else
    # TODO: Once verified why we delete it
    if [ -e "${SERVER_DIR}/jvm.options" ]; then
        rm ${SERVER_DIR}/jvm.options
    else
        echo "File ${SERVER_DIR}/jvm.options not found. Nothing to remove"
    fi
fi

echoAndRunCmd "cd ${BENCHMARK_DIR}/tmp"
stopLibertyServer
echoAndRunCmd "cd -"


# Run the Garbage Collection and Memory Visualization (GCMV) tool.
# should really be run on a seperate machine so perf machine can run other jobs
if [[ "$GCMV_ENABLED" = "true" ]]; then
  runGCMVTool
else
  echo "Skipping GCMV"
fi

calculateJitCpuUtilization

echo ""
echo "Throughput Benchmark has completed successfully"

###############
# Cleanup
###############
zipPerfProfileFiles ${BENCHMARK_DIR}/tmp
includeFilesToStore

#TODO: Once verified if zOS files on webber are ascii
# convert backup files to ASCII for os390
if [[ "$PLATFORM" = "OS/390" ]]; then
  echo "Making sure all files are in ascii"
  ${BENCHMARK_DIR}/bin/encode.sh -toascii ${FILES_TO_STORE}
fi

# store files locally, then clone the whole results dir to a remote results machine
storeFiles ${FILES_TO_STORE}
FILES_TO_STORE=""
moveResultsToRemoteMachine -z
# TODO: Once verified, check for successful transfer to remote before deleting files on host and client
removeResultsFromClient
removeAllResultFilesOnHost