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

##################################
# Usage
#
# There are 2 large commands that set environment variables:
#  - setServerDBLoadAffinities
#  - setServerLoadAffinities
# ```
# setServerDBLoadAffinities --server-physcpu-num 2 --smt true
# ```
# This will calculate and initialize environment variables such that 2 cores
# are allocated for the server and the rest are evenly shared between DB and Load.
# setServerLoadAffinities behaves similarly but allocates the rest to Load.
#
# Users can then use the environment variables to create commands to allocate
# cpus:
# ```
# generate_cpu_set_command --physcpubind $LOAD_CPUS --membind $LOAD_NODE
# ```
# This will generate a command that allocates the LOAD_CPUS and LOAD_NODE.
# i.e. numactl --physcpubind=18-23,42-47 --membind=1
#
# ```
# get_cpus --node 0 --cpu-offset 0 --physcpu-num 2 --smt true
# ```
# This will generate cpu numbers for the current platform so that it can be used
# with docker or other applications.
# i.e. 0-7,14-22 on linux
# i.e. ff on windows

#########################
# Dependencies
#
# These are the programs that need to be installed in order for this script to run
#
# Common:
#  - bash
#    - expr
#    - printf
#  - seq
#    - an in-house solution exists but a system seq is recommended
#
# Linux:
#  - lscpu
#  - numactl
# AIX:
#  - lssrad
#  - lsdev
#  - execrset
# Windows:
#  - wmic
# z/OS:
#  - oeconsol
# Mac:
#  - sysctl

# Given, the number of cpus for the server, set up a
# 3-way cpu split allocation between a server, a database and a load.
# This function sets the following environment variables:
#
# SERVER_CPU_NUM - number of cpus allocated to the server
# SERVER_CPUS - specific cpus allocated to the server, formatted to
# be consumed by docker or numactl commands
# SERVER_NODE - the node server cpus will be on
# LOAD_DB_CPU_NUM - number of cpus allocated to the load or database
# DB_CPUS - specific cpus allocated to the database, formatted to
# be consumed by docker or numactl commands
# DB_NODE - the node db cpus will be on
# LOAD_CPUS - specific cpus allocated to the load, formatted to
# be consumed by docker or numactl commands
# LOAD_NODE - the node load cpus will be on
# SERVER_AFFINITY_CMD - command used to bind a process to the server cpus
# LOAD_AFFINITY_CMD - command used to bind a proess to the load cpus
# DB_AFFINITY_CMD - command used to bind a proess to the database cpus
#
# args:
# --server-physcpu-num i: allocate i number of physical cpus for the server
# --smt: take into account logical cores
function setServerDBLoadAffinities() {
	echo "##### Setting Affinities: Start"

	unset SERVER_CPUS
	unset LOAD_CPUS
	unset DB_CPUS

	unset SERVER_AFFINITY_CMD
	unset LOAD_AFFINITY_CMD
	unset DB_AFFINITY_CMD

	local __ARG_SERVER_PHY_CPU_NUM=0
	local __ARG_SMT=true
	while [ -n "$1" ]; do
		case $1 in
			--server-physcpu-num)
				shift
				__ARG_SERVER_PHY_CPU_NUM=$1
				;;
			--smt)
				shift
				__ARG_SMT=$1
				;;
			*)
				echo "Unknown parameter: $1" >&2
				;;
		esac
		shift
	done

	SERVER_NODE=0
	SERVER_CPU_NUM=$__ARG_SERVER_PHY_CPU_NUM

	COMMAND="get_cpus --node $SERVER_NODE --cpu-offset 0 --physcpu-num ${SERVER_CPU_NUM} --smt $__ARG_SMT"
	echo "${COMMAND}"
	SERVER_CPUS=$(${COMMAND})

	local NODE_COUNT=$(get_node_count)

	if [ $NODE_COUNT -gt 1 ]; then
		LOAD_NODE=1
		DB_NODE=1
		DB_CPU_START=0
		LOAD_CPU_NUM=$(expr $(get_phys_cores_per_socket) / 2)
		DB_CPU_NUM=$LOAD_CPU_NUM
		LOAD_CPU_START=$LOAD_CPU_NUM
	else
		LOAD_NODE=0
		DB_NODE=0
		DB_CPU_START=$SERVER_CPU_NUM
		LOAD_CPU_NUM=$(expr $(expr $(get_phys_cores_per_socket) - $SERVER_CPU_NUM) / 2)
		DB_CPU_NUM=$LOAD_CPU_NUM
		LOAD_CPU_START=$(expr $SERVER_CPU_NUM + $LOAD_CPU_NUM)
	fi
	COMMAND="get_cpus --node $DB_NODE --cpu-offset $DB_CPU_START --physcpu-num $LOAD_CPU_NUM --smt $__ARG_SMT"
	echo "${COMMAND}"
	DB_CPUS=$(${COMMAND})
	COMMAND="get_cpus --node $LOAD_NODE --cpu-offset $LOAD_CPU_START --physcpu-num $LOAD_CPU_NUM --smt $__ARG_SMT"
	echo "${COMMAND}"
	LOAD_CPUS=$(${COMMAND})

	SERVER_AFFINITY_CMD=$(generate_cpu_set_command --physcpubind $SERVER_CPUS --membind $SERVER_NODE)
	LOAD_AFFINITY_CMD=$(generate_cpu_set_command --physcpubind $LOAD_CPUS --membind $LOAD_NODE)
	DB_AFFINITY_CMD=$(generate_cpu_set_command --physcpubind $DB_CPUS --membind $DB_NODE)

	echo "====Server affinity"
	echo "Server will now be using CPUs: SERVER_CPUS=${SERVER_CPUS}"
	echo "Server number of physical CPUs is: SERVER_CPU_NUM=${SERVER_CPU_NUM}"
	echo "Server will be using node: SERVER_NODE=${SERVER_NODE}"
	echo "Server affinity command is: SERVER_AFFINITY_CMD=${SERVER_AFFINITY_CMD}"

	echo "====DB affinity"
	echo "DB will now be using CPUs: DB_CPUS=${DB_CPUS}"
	echo "DB will be using node: DB_NODE=${DB_NODE}"
	echo "DB number of physical CPUs is: DB_CPU_NUM=${DB_CPU_NUM}"
	echo "DB affinity command is: DB_AFFINITY_CMD=${DB_AFFINITY_CMD}"

	echo "====Load affinity"
	echo "Load will now be using CPUs: LOAD_CPUS=${LOAD_CPUS}"
	echo "Load will be using node: LOAD_NODE=${LOAD_NODE}"
	echo "Load number of physical CPUs is: LOAD_CPU_NUM=${LOAD_CPU_NUM}"
	echo "Load affinity command is: LOAD_AFFINITY_CMD=${LOAD_AFFINITY_CMD}"

	affinity_tool_install_check
	echo "##### Setting Affinities: Done"
}

# Given, the number of cpus for the server, set up a
# 2-way cpu split allocation between a server and a load.
# This function sets the following environment variables:
#
# SERVER_CPU_NUM - number of cpus allocated to the server
# SERVER_CPUS - specific cpus allocated to the server, formatted to
# be consumed by docker or numactl commands
# SERVER_NODE - the node server cpus will be on
# LOAD_CPU_NUM - number of cpus allocated to the load
# LOAD_CPUS - specific cpus allocated to the load, formatted to
# LOAD_NODE - the node load cpus will be on
# be consumed by docker or numactl commands
# SERVER_AFFINITY_CMD - command used to bind a process to the server cpus
# LOAD_AFFINITY_CMD - command used to bind a proess to the load cpus
# 
# args:
# --server-physcpu-num i: allocate i number of physical cpus for the server
# --smt: take into account logical cores
function _setServerLoadAffinities() {
	unset SERVER_CPUS
	unset LOAD_CPUS
	unset DB_CPUS

	unset SERVER_AFFINITY_CMD
	unset LOAD_AFFINITY_CMD
	unset DB_AFFINITY_CMD

	local __ARG_SERVER_PHY_CPU_NUM=0
	local __ARG_SMT=true
	while [ -n "$1" ]; do
		case $1 in
			--server-physcpu-num)
				shift
				__ARG_SERVER_PHY_CPU_NUM=$1
				;;
			--smt)
				shift
				__ARG_SMT=$1
				;;
			*)
				echo "Unknown parameter: $1" >&2
				;;
		esac
		shift
	done

	SERVER_NODE=0
	SERVER_CPU_NUM=$__ARG_SERVER_PHY_CPU_NUM

	COMMAND="get_cpus --node ${SERVER_NODE} --cpu-offset 0 --physcpu-num ${SERVER_CPU_NUM} --smt $__ARG_SMT"
	echo "${COMMAND}"
	SERVER_CPUS=$(${COMMAND})

	local NODE_COUNT=$(get_node_count)

	if [ $NODE_COUNT -gt 1 ]; then
		LOAD_NODE=1
		LOAD_CPU_START=0
		LOAD_CPU_NUM=$(get_phys_cores_per_socket)
	else
		LOAD_NODE=0
		LOAD_CPU_START=$SERVER_CPU_NUM
		LOAD_CPU_NUM=$(expr $(get_phys_cores_per_socket) - $SERVER_CPU_NUM)
	fi

	COMMAND="get_cpus --node $LOAD_NODE --cpu-offset $LOAD_CPU_START --physcpu-num $LOAD_CPU_NUM --smt $__ARG_SMT"
	echo "${COMMAND}"
	LOAD_CPUS=$(${COMMAND})

	SERVER_AFFINITY_CMD=$(generate_cpu_set_command --physcpubind $SERVER_CPUS --membind $SERVER_NODE)
	LOAD_AFFINITY_CMD=$(generate_cpu_set_command --physcpubind $LOAD_CPUS --membind $LOAD_NODE)
}

# Wrapper around _setServerLoadAffinities function so setServerDBAffinities
# can reuse code
function setServerLoadAffinities() {
	echo "##### Setting Affinities: Start"

	_setServerLoadAffinities $@

	echo "====Server affinity"
	echo "Server will now be using CPUs: SERVER_CPUS=${SERVER_CPUS}"
	echo "Server number of physical CPUs is: SERVER_CPU_NUM=${SERVER_CPU_NUM}"
	echo "Server will be using node: SERVER_NODE=${SERVER_NODE}"
	echo "Server affinity command is: SERVER_AFFINITY_CMD=${SERVER_AFFINITY_CMD}"

	echo "====Load affinity"
	echo "Load will now be using CPUs: LOAD_CPUS=${LOAD_CPUS}"
	echo "Load will be using node: LOAD_NODE=${LOAD_NODE}"
	echo "Load number of physical CPUs is: LOAD_CPU_NUM=${LOAD_CPU_NUM}"
	echo "Load affinity command is: LOAD_AFFINITY_CMD=${LOAD_AFFINITY_CMD}"

	affinity_tool_install_check
	echo "##### Setting Affinities: Done"
}

# Given, the number of cpus for the server, set up a
# 2-way cpu split allocation between a server and a database.
# This function sets the following environment variables:
#
# SERVER_CPU_NUM - number of cpus allocated to the server
# SERVER_CPUS - specific cpus allocated to the server, formatted to
# be consumed by docker or numactl commands
# SERVER_NODE - the node server cpus will be on
# DB_CPU_NUM - number of cpus allocated to the database
# DB_CPUS - specific cpus allocated to the database, formatted to
# DB_NODE - the node database cpus will be on
# be consumed by docker or numactl commands
# SERVER_AFFINITY_CMD - command used to bind a process to the server cpus
# DB_AFFINITY_CMD - command used to bind a proess to the database cpus
# 
# args:
# --server-physcpu-num i: allocate i number of physical cpus for the server
# --smt: take into account logical cores
function setServerDBAffinities() {
	echo "##### Setting Affinities: Start"

	_setServerLoadAffinities $@

	DB_CPU_NUM="$LOAD_CPU_NUM"
	DB_NODE="$LOAD_NODE"
	DB_CPUS="$LOAD_CPUS"
	DB_AFFINITY_CMD="$LOAD_AFFINITY_CMD"

	unset LOAD_CPU_NUM
	unset LOAD_NODE
	unset LOAD_CPUS
	unset LOAD_AFFINITY_CMD

	echo "====Server affinity"
	echo "Server will now be using CPUs: SERVER_CPUS=${SERVER_CPUS}"
	echo "Server number of physical CPUs is: SERVER_CPU_NUM=${SERVER_CPU_NUM}"
	echo "Server will be using node: SERVER_NODE=${SERVER_NODE}"
	echo "Server affinity command is: SERVER_AFFINITY_CMD=${SERVER_AFFINITY_CMD}"

	echo "====DB affinity"
	echo "DB will now be using CPUs: DB_CPUS=${DB_CPUS}"
	echo "DB number of physical CPUs is: DB_CPU_NUM=${DB_CPU_NUM}"
	echo "DB will be using node: DB_NODE=${DB_NODE}"
	echo "DB affinity command is: DB_AFFINITY_CMD=${DB_AFFINITY_CMD}"

	affinity_tool_install_check
	echo "##### Setting Affinities: Done"
}

if [ -z "$AFFINITY_VERBOSE" ]; then
	AFFINITY_VERBOSE=""
fi

if [ hash seq 2> /dev/null ]; then
	echo "Warning: no system seq detected. Using in-house solution" >&2

	# TODO: z/OS currently does not have support for seq command.
	# So we will maintain our own basic seq command for now.
	function seq() {
		local sep='\n'
		local start=1
		local end=1
		local inc=1

		while [ -n "$1" ]; do
			case $1 in
				-s)
					shift
					sep=$1
					shift
					break
					;;
				*)
					break
					;;
			esac
			shift
		done

		if [ $# -eq 2 ]; then
			start=$1
			end=$2
		elif [ $# -eq 3 ]; then
			start=$1
			end=$3
			inc=$2
		fi

		local i=$start
		local result="$i"
		i=$(expr $i + $inc)
		while [ $i -le "$end" ]; do
			result="${result}${sep}${i}"
			i=$(expr $i + $inc)
		done
		echo $result
	}
fi

# get how many numa nodes are on this system
function get_node_count() {
	echo "$__AFFINITY_NUMA_NODES"
}

function get_os() {
	echo "$__AFFINITY_UNAME"
}

# i.e. x86, x86_64
function get_cpu_arch() {
	echo "$__AFFINITY_CPU_ARCH"
}

# i.e. Linux, AIX
function get_platform() {
	echo "$__AFFINITY_PLATFORM"
}

# get the cpus assigned to a specific numa node (starting from 0)
function get_numa_node_cpus() {
	local index=$1
	echo "${__AFFINITY_NUMA_NODE_ARR[${index}]}"
}

# get number of physical cores per socket
function get_phys_cores_per_socket() {
	echo "$__AFFINITY_CORES_PER_SOCKET"
}

# get number of smt per core
function get_threads_per_core() {
	echo "$__AFFINITY_THREAD_PER_CORE"
}

# get total amount of cpus (SMT included)
function get_cpu_count() {
	echo "$__AFFINITY_THREAD_COUNT"
}

function echo_and_run() {
	if [ -z "$1" ]; then
		return
	fi

	if [ -n "$AFFINITY_VERBOSE" ]; then
		echo "$1"
	fi
	$1
}

# outputs cpu information depending on the OS
function __get_cpu_info {
	local os=$(get_os)
	case "$os" in
	Linux)
		lscpu
		;;
	AIX)
		prtconf
		;;
	Windows)
		wmic cpu get DeviceID, NumberOfCores, NumberOfLogicalProcessors, SocketDesignation
		;;
	OS/390*)
		oeconsol 'd m=core'
		;;
	Darwin)
		sysctl -a | grep machdep.cpu
		;;
	*)
		echo "Unsupported platform: $os" >&2
		;;
	esac
}

# initializes environment variables for platform detection.
# this function needs to be called for other functions to work
function __init_platform() {
	__AFFINITY_UNAME=$(uname)
	__AFFINITY_PLATFORM=""
	case "$__AFFINITY_UNAME" in
	Linux)
		# ie. x86, x86_64, ppcle64, ppc64, s390, s390x
		__AFFINITY_CPU_ARCH=$(uname -m)
		__AFFINITY_PLATFORM="${__AFFINITY_UNAME}-${__AFFINITY_CPU_ARCH}"
		;;
	AIX)
		__AFFINITY_CPU_ARCH=$(uname -m)
		__AFFINITY_PLATFORM="${__AFFINITY_UNAME}-${__AFFINITY_CPU_ARCH}"
		;;
	OS/390*)
		__AFFINITY_CPU_ARCH=$(uname -m)
		__AFFINITY_PLATFORM="${__AFFINITY_UNAME}-${__AFFINITY_CPU_ARCH}"
		;;
	Darwin)
		__AFFINITY_CPU_ARCH=$(uname -m)
		__AFFINITY_PLATFORM="${__AFFINITY_UNAME}-${__AFFINITY_CPU_ARCH}"
		;;
	CYGWIN*)
		__AFFINITY_UNAME=Windows
		__AFFINITY_CPU_ARCH=$(uname -m)
		# ie. Windows-x86_64
		__AFFINITY_PLATFORM=${__AFFINITY_UNAME}-${__AFFINITY_CPU_ARCH}
		;;
	*)
		echo "Unknown platform: ${__AFFINITY_PLATFORM}" >&2
		exit 1
		;;
	esac
}

# initializes environment variables for cpu configuration.
# this function needs to be called for other functions to work
function __init_cpus() {
	case $(get_os) in
	Linux)
		__init_cpus_linux
		;;
	AIX)
		__init_cpus_aix
		;;
	Windows)
		__init_cpus_windows
		;;
	OS/390*)
		__init_cpus_zos
		;;
	Darwin)
		__init_cpus_macos
		;;
	*)
		echo "Unsupported os: $os" >&2
		exit 1
		;;
	esac
}

# initializes cpu values for Linux platform
function __init_cpus_linux() {
	# used to hold all cpu info
	__AFFINITY_CPU_INFO_OUTPUT=$(__get_cpu_info)

	# total number of cpus (physical and logical) ie. 4, 8, 16, 32, 48
	__AFFINITY_THREAD_COUNT=$(echo $(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'CPU(s):' | head -n 1 | cut -d ':' -f 2))

	# threads per core. ie. 2, 8
	__AFFINITY_THREAD_PER_CORE=$(echo $(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'Thread(s) per core:' | cut -d ':' -f 2))

	# cores per socket. ie. 2, 4, 8, 12, 24
	__AFFINITY_CORES_PER_SOCKET=$(echo $(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'Core(s) per socket:' | cut -d ':' -f 2))

	# number of sockets on the system. ie. 1, 2, 3
	__AFFINITY_SOCKETS=$(echo $(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'Socket(s):' | cut -d ':' -f 2))

	# in case the platform does not support NUMA, just fallback to on-line cpu list
	if ! echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'NUMA node(s):' >/dev/null; then
		__AFFINITY_NUMA_NODES=1

		# ie. 0-31
		local tmp=$(echo $(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'On-line CPU(s) list:' | cut -d ':' -f 2))
		# put it into an array
		__AFFINITY_NUMA_NODE_ARR=($tmp)
	else
		__AFFINITY_NUMA_NODES=$(echo $(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'NUMA node(s):' | cut -d ':' -f 2))
		__AFFINITY_NUMA_SEQ=$(expr $__AFFINITY_NUMA_NODES - 1)

		__AFFINITY_NUMA_NODE_ARR=()
		for (( i=0; i<$__AFFINITY_NUMA_NODES; i++ )); do
			local tmp=$(echo $(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep "NUMA node${i} CPU(s):" | cut -d ':' -f 2))
			__AFFINITY_NUMA_NODE_ARR+=($tmp)
		done
	fi
}

# initializes cpu values for Windows platform
function __init_cpus_windows() {
	__AFFINITY_CPU_INFO_OUTPUT=$(__get_cpu_info)

	# physical cores per socket. ie. 2, 4, 8, 12, 24
	__AFFINITY_CORES_PER_SOCKET=$(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'CPU0' | awk '{ print $2 }')

	# logical cores per socket. ie. 2, 4, 8, 12, 24
	__AFFINITY_LOGIC_CORE_PER_SOCKET=$(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'CPU0' | awk '{ print $3 }')

	# get thread number by dividing logical core by physical cores
	# ie. 16 / 8 = 2
	__AFFINITY_THREAD_PER_CORE=$(expr $__AFFINITY_LOGIC_CORE_PER_SOCKET / $__AFFINITY_CORES_PER_SOCKET)

	# get total cpu count and join them with ' + '
	# ie.
	#  16
	#  16 -> 16 16 16 -> 16 + 16 + 16 +
	#  16
	__AFFINITY_THREAD_COUNT=$(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep CPU | awk '{ print $3 }' | tr '\n' ' ' | sed 's/ / + /g')

	# expr 16 + 16 + 16 + 0
	# giving us 48 cpus, the total cpu count
	__AFFINITY_THREAD_COUNT=$(expr $__AFFINITY_THREAD_COUNT 0)

	# get how many numa nodes there are
	__AFFINITY_NUMA_NODES=$(expr $(wmic cpu get SocketDesignation | wc -l) - 2)

	__AFFINITY_NUMA_NODE_ARR=()
	for (( i=0; i<$__AFFINITY_NUMA_NODES; i++ )); do
		local cpu_max=$(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep "CPU$i" | awk '{ print $3 }')
		local start=$(expr $i \* $cpu_max)
		local end=$(expr $start + $cpu_max - 1)
		__AFFINITY_NUMA_NODE_ARR+=(${start}-${end})
	done
}

# initializes cpu values for AIX platform
function __init_cpus_aix() {
	# get how many nodes there are
	__AFFINITY_NUMA_NODES=$(expr $(lssrad -a | wc -l) - 1)

	# add the nodes to an array
	__AFFINITY_NUMA_NODE_ARR=()
	for (( i=0; i<$__AFFINITY_NUMA_NODES; i++ )); do
		local numa_node=$(lssrad -vs $i | tail -1 | awk '{print $4}')
		__AFFINITY_NUMA_NODE_ARR+=($numa_node)
	done

	# total number of physical cores
	local phys_cpus_count=$(lsdev -Cc processor | grep Available | wc -l)

	# get total number of cpus
	local cpu_max=$(bindprocessor -q | tr ' ' '\n' | tail -1)
	__AFFINITY_CPUS=$(expr $cpu_max + 1)

	# get threads per core
	__AFFINITY_THREAD_PER_CORE=$(expr $__AFFINITY_CPUS / $phys_cpus_count)

	# get physical cores per socket
	__AFFINITY_CORES_PER_SOCKET=$(expr $phys_cpus_count / $__AFFINITY_NUMA_NODES)
}

# initializes cpu values for z/OS platform
function __init_cpus_zos() {
	__AFFINITY_CPU_INFO_OUTPUT=$(__get_cpu_info)

	# get threads per core
	__AFFINITY_THREAD_PER_CORE=$(echo "$__AFFINITY_CPU_INFO_OUTPUT" | grep 'CORE STATUS: ' | cut -d ':' -f 2 | awk '{ print $2 }' | cut -d '=' -f 2)

	# cpu id counter: 0, 1, 2, ..., 9, 10, 11, ...,
	local i=0
	# cpu id counter formatted as hex: 0, 1, 2, ..., 9, A, B, ...
	local ix=$(printf '%X\n' "$((${i}))")
	# cpu id counter as hex with 4 0s padded: 0001, 0002, ..., 0009, 000A
	local ix4=$(printf '%04X\n' $(expr $i \* $__AFFINITY_THREAD_PER_CORE))

	# loop that keeps checking if more cores exists
	local cpus=$(oeconsol "d m=core($ix)" | grep "$ix4" | awk '{ print $3 }')
	while [ "$cpus" != "" ]; do
		local start=$(printf "%d\n" 0x$(echo "$cpus" | cut -d "-" -f 1))
		local   end=$(printf "%d\n" 0x$(echo "$cpus" | cut -d "-" -f 2))

		# local cpu=$start-$end

		i=$(expr $i + 1)
		ix=$(printf '%X\n' "${i}")
		ix4=$(printf '%04X\n' $(expr $i \* $__AFFINITY_THREAD_PER_CORE))

		cpus=$(oeconsol "d m=core($ix)" | grep "$ix4" | awk '{ print $3 }')
	done

	# number of physical cores per socket
	__AFFINITY_CORES_PER_SOCKET=$i

	# total number of cores
	__AFFINITY_THREAD_COUNT=$(expr $__AFFINITY_CORES_PER_SOCKET \* $__AFFINITY_THREAD_PER_CORE)

	# set to just 1 node
	__AFFINITY_NUMA_NODE_ARR=(0-$(expr $__AFFINITY_THREAD_COUNT - 1))
	__AFFINITY_NUMA_NODES=1
}

# initializes cpu values for MacOS platform
function __init_cpus_macos() {
	__AFFINITY_THREAD_COUNT=$(sysctl -n machdep.cpu.thread_count)
	__AFFINITY_CORES_PER_SOCKET=$(sysctl -n machdep.cpu.core_count)

	__AFFINITY_THREAD_PER_CORE=$(expr $__AFFINITY_THREAD_COUNT / $__AFFINITY_CORES_PER_SOCKET)

	__AFFINITY_NUMA_NODE_ARR=(0-$(expr $__AFFINITY_THREAD_COUNT - 1))
	__AFFINITY_NUMA_NODES=1
}

# shrinks the cpus into its shortened form as well as format them for their
# specific platform.
function __shrink_format_cpu() {
	local os=$(get_os)
	case "$os" in
	Linux*)
		__shrink_format_cpu_linux $@
		;;
	Windows*)
		__shrink_format_cpu_windows $@
		;;
	AIX*)
		__shrink_format_cpu_linux $@
		;;
	OS/390*)
		__shrink_format_cpu_zos $@
		;;
	Darwin)
		__shrink_format_cpu_linux $@
		;;
	*)
		echo "Unsupported os: $os" >&2
		exit 1
		;;
	esac
}

# Function that shrinks and format of cpus for Linux systems
# ie.
#     0,7,6,5,1,3,4,2 -> 0-7
function __shrink_format_cpu_linux() {
	# Function that formats cpu cores together
	function __format_cpu() {
		local LOW=$1
		local HIGH=$2

		if [ "$LOW" -eq "$HIGH" ]; then
			echo $LOW
			return
		fi

		local TMP=$(expr "$LOW" + 1)

		if [ "$TMP" -eq "$HIGH" ]; then
			echo "$LOW,$HIGH"
		else
			echo "$LOW-$HIGH"
		fi
	}

	local CPUS=$1

	local prev=""
	local start=""
	local end=""
	local RESULT=""
	for i in $(echo $CPUS | tr ',' '\n'); do
		if [ -z "$prev" ]; then
			start=$i
			end=$i
		else
			local tmp=$(expr $prev + 1)
			if [ $tmp -eq $i ]; then
				end=$i
			else
				RESULT="$RESULT "$(__format_cpu $start $end)
				start=$i
				end=$i
			fi
		fi
		prev=$i
	done
	RESULT="$RESULT "$(__format_cpu $start $end)
	RESULT=$(echo $RESULT | tr ' ' ',')
	echo $RESULT
}

# Function that shrinks and format of cpus for Windows systems
# ie.
#     0,7,6,5,1,3,4,2 -> ff
#
# 0xff -> 0b'1111 1111
#            ^cpu7   ^cpu0
function __shrink_format_cpu_windows() {
	local CPUS=$1
	local result="0"
	for cpu in $(echo "$CPUS" | tr ',' '\n'); do
		tmp="$((1 << $cpu))"
		result=$(($result | $tmp))
	done
	result=$(printf '%x\n' "$((${result}))")

	echo $result
}

# Function that shrinks and format of cpus for z/OS systems
# ie.
#     0,7,6,5,1,3,4,2 -> 00,01,02,03,04,05,06,07
function __shrink_format_cpu_zos() {
	local CPUS=$1
	local result=""
	for cpu in $(echo "$CPUS" | tr ',' '\n'); do
		tmp=$(printf "%02x" $cpu)
		result="$result $tmp"
	done
	result=$(echo $result | tr ' ' ',')

	echo $result
}

# Get the start of physical cpu count.
# i.e. if the cpus we want are 12-16, this function computes 12.
# i.e. if the physical cpus we want are 2-4 with smt 2, this function will
# compute 4.
# core0: 0,1
# core1: 2,3
# core2: 4,5
#
# args:
# --node n: use node n for calculating cpu allocation
# --cpu-offset n: start at cpu n
function get_physical_core_start() {
	local __ARG_NUMA_NODE=0
	local __ARG_CPU_START=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--cpu-offset)
				shift
				__ARG_CPU_START=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				;;
		esac
		shift
	done

	# get the cpus associated with given numa node index
	local NODE_CPUS=$(get_numa_node_cpus $__ARG_NUMA_NODE)
	# in cases where cpus are like: 0-63,64-127
	# 0-63 are physical cores
	# 64-127 are logical cores
	if [[ "$NODE_CPUS" == *,* ]]; then
		# we only want the physical cores (0-63)
		NODE_CPUS=$(echo $NODE_CPUS | cut -d ',' -f 1)
		# get the starting cpu index and offset it by __ARG_CPU_START
		local PHY_CPU_START=$(echo $NODE_CPUS | cut -d '-' -f 1)
		local RESULT=$(expr $PHY_CPU_START + $__ARG_CPU_START)

		echo $RESULT
		return
	fi

	# in cases where cpus are like: 0-127
	# get the number of threads per core and 
	local THREADS_PER_CORE=$(get_threads_per_core)

	# get the starting cpu index and offset it by
	# __ARG_CPU_START * THREADS_PER_CORE
	local PHY_CPU_START=$(echo $NODE_CPUS | cut -d '-' -f 1)
	local RESULT=$(expr $PHY_CPU_START + $__ARG_CPU_START \* $THREADS_PER_CORE)

	# if RESULT exceeds number of cpus available, then set it the max index
	# cpu
	local PHY_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
	if [ $RESULT -gt $PHY_CPU_MAX ]; then
		RESULT=$PHY_CPU_MAX
	fi

	echo $RESULT
}

# Get the end of physical cpu count.
# i.e. if we wanted cores 0-7, this function computes to get 7
#
# args:
# --node n: use node n for calculating cpu allocation
# --physcpu-num n: calculate n physical cpus
# --cpu-offset n: start at cpu n
function get_physical_core_end() {
	local __ARG_NUMA_NODE=0
	local __ARG_CPU_START=0
	local __ARG_CPU_NUM=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--cpu-offset)
				shift
				__ARG_CPU_START=$1
				;;
			--physcpu-num)
				shift
				__ARG_CPU_NUM=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				exit 1
				;;
		esac
		shift
	done

	local CPU_START=$(get_physical_core_start --node $__ARG_NUMA_NODE --cpu-offset $__ARG_CPU_START)

	# get the cpus associated with given numa node index
	local NODE_CPUS=$(get_numa_node_cpus $__ARG_NUMA_NODE)
	# in cases where cpus are like: 0-63,64-127
	# 0-63 are physical cores
	# 64-127 are logical cores
	if [[ "$NODE_CPUS" == *,* ]]; then
		# we only want the physical cores (0-63)
		NODE_CPUS=$(echo $NODE_CPUS | cut -d ',' -f 1)
		# subtract 1 because we count CPU_START as a cpu too
		# e.g. assuming we are starting at cpu 0
		#   12-15 -> 0 + 12 + 4 = 16
		#   16 - 1 = 15
		local RESULT=$(expr $CPU_START + $__ARG_CPU_NUM - 1)

		echo $RESULT
		return
	fi

	# in cases where cpus are like: 0-127
	local THREADS_PER_CORE=$(get_threads_per_core)

	# when asked for more cpu than the computer has, fallback to the max
	# number of cpus the computer has
	local RESULT=$(expr $CPU_START + $(expr $__ARG_CPU_NUM - 1) \* $THREADS_PER_CORE)

	local PHY_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
	if [ $RESULT -gt $PHY_CPU_MAX ]; then
		echo "Warning: asked for $RESULT cpus but only have $PHY_CPU_MAX cpus" >&2
		RESULT=$PHY_CPU_MAX
	fi
	echo $RESULT
}

# Creates a string of comma separated cpu numbers that we specified.
# i.e. generate_physical_cores --node 0 --start 0 --end 7
# where smt is 2.
# output: 0,2,4,6
# i.e. generate_physical_cores --node 0 --start 2 --end 4
# where smt is 2.
# output: 2,4
#
# args:
# --node n: allocate cpus on node n
# --start n: start at cpu n when allocating
# --end n: end at cpu n when allocating
function generate_physical_cores() {
	local __ARG_NUMA_NODE=0
	local __ARG_CPU_FIRST=0
	local __ARG_CPU_LAST=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--start)
				shift
				__ARG_CPU_FIRST=$1
				;;
			--end)
				shift
				__ARG_CPU_LAST=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				exit 1
				;;
		esac
		shift
	done

	local THREADS_PER_CORE=$(get_threads_per_core)

	# get the cpus associated with given numa node index
	local NODE_CPUS=$(get_numa_node_cpus $__ARG_NUMA_NODE)
	# in cases where cpus are like: 0-63,64-127
	# 0-63 are physical cores
	# 64-127 are logical cores
	if [[ "$NODE_CPUS" == *,* ]]; then
		# we only want the physical cores (0-63)
		NODE_CPUS=$(echo $NODE_CPUS | cut -d ',' -f 1)

		# in case we ask for more cpus than the machine has
		local PHY_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
		if [ $__ARG_CPU_LAST -gt $PHY_CPU_MAX  ]; then
			echo "Warning: asked for $__ARG_CPU_LAST cpus, but only have $PHY_CPU_MAX cpus" >&2
			$__ARG_CPU_LAST=$PHY_CPU_MAX
		fi

		local RESULT=$(seq -s ',' $__ARG_CPU_FIRST $__ARG_CPU_LAST)
		echo $RESULT
		return
	fi

	local PHY_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
	if [ $__ARG_CPU_LAST -gt $PHY_CPU_MAX  ]; then
		echo "Warning: asked for $__ARG_CPU_LAST cpus, but only have $PHY_CPU_MAX cpus" >&2
		$__ARG_CPU_LAST=$PHY_CPU_MAX
	fi
	CPUS=$(seq -s ' ' $__ARG_CPU_FIRST $THREADS_PER_CORE $__ARG_CPU_LAST)

	local RESULT=$(echo $CPUS | tr ' ' ',')
	echo $RESULT
}

# Creates a string of comma separated cpu numbers that we specified.
# i.e. get_physical_cores --node 0 --cpus 2 --cpu-offset 1
# where smt is 2.
# output: 2,4
# i.e. get_physical_cores --node 0 --cpus 2 --cpu-offset 2
# where smt is 2.
# output: 4,6
#
# args:
# --node n: allocate cpus on node n
# --cpus n: allocate n number of physical cpus
# --cpu-offset n: start allocating cpus at n
function get_physical_cores() {
	# which numa node to use
	local __ARG_NUMA_NODE=0
	# how many cpus we want
	local __ARG_CPU_NUM=0
	# cpu offset in the numa node
	local __ARG_CPU_START=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--cpus)
				shift
				__ARG_CPU_NUM=$1
				;;
			--cpu-offset)
				shift
				__ARG_CPU_START=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				exit 1
				;;
		esac
		shift
	done

	local CPU_FIRST=$(get_physical_core_start --node $__ARG_NUMA_NODE --cpu-offset $__ARG_CPU_START)
	local CPU_LAST=$(get_physical_core_end --node $__ARG_NUMA_NODE --cpu-offset $__ARG_CPU_START --physcpu-num $__ARG_CPU_NUM)

	local RESULT=$(generate_physical_cores --node $__ARG_NUMA_NODE --start $CPU_FIRST --end $CPU_LAST)
	echo $RESULT
}

# Get the start of logical cpu count.
# i.e. if the cpus we want are 0-16, with smt > 1, this function computes 1
#
# args:
# --node n: use node n for calculating cpu allocation
# --physcpu-num n: calculate n physical cpus worth of logical cpus
# --cpu-offset n: start at cpu n
function get_logical_core_start() {
	# if there is only 1 thread per core,
	# then this system doesn't support SMT
	local THREADS_PER_CORE=$(get_threads_per_core)
	if [ $THREADS_PER_CORE -eq 1 ]; then
		echo "Warning: This CPU does not support SMT" >&2
		return
	fi

	local __ARG_NUMA_NODE=0
	local __ARG_CPU_START=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--cpu-offset)
				shift
				__ARG_CPU_START=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				;;
		esac
		shift
	done

	local NODE_CPUS=$(get_numa_node_cpus $__ARG_NUMA_NODE)
	# in cases where cpus are like: 0-63,64-127
	# 0-63 are physical cores
	# 64-127 are logical cores
	if [[ "$NODE_CPUS" == *,* ]]; then
		# we only want the logical cores (64-127)
		NODE_CPUS=$(echo $NODE_CPUS | cut -d ',' -f 2)
		local LOG_CPU_START=$(echo $NODE_CPUS | cut -d '-' -f 1)

		# get the starting cpu index and offset it by __ARG_CPU_START
		local RESULT=$(expr $LOG_CPU_START + $__ARG_CPU_START)

		# in case we ask for more cpus than the machine has
		local LOG_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
		if [ $RESULT -gt $LOG_CPU_MAX ]; then
			echo "Warning: asked for $RESULT cpus, but only have $LOG_CPU_MAX cpus" >&2
			RESULT=$LOG_CPU_MAX
		fi

		echo $RESULT
		return
	fi

	# count the physical core out
	local LOG_CPU_START=$(echo $NODE_CPUS | cut -d '-' -f 1)
	local RESULT=$(expr $LOG_CPU_START + $__ARG_CPU_START \* $THREADS_PER_CORE + 1)

	# in case we ask for more cpus than the machine has
	local LOG_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
	if [ $RESULT -gt $LOG_CPU_MAX ]; then
		echo "Warning: asked for $RESULT cpus, but only have $LOG_CPU_MAX cpus" >&2
		RESULT=$LOG_CPU_MAX
	fi

	echo $RESULT
}

# Get the end of logical cpu count.
# i.e. if we wanted cores 0-7, with smt 4, this function computes to get 7
#
# args:
# --node n: use node n for calculating cpu allocation
# --physcpu-num n: calculate n physical cpus worth of logical cpus
# --cpu-offset n: start at cpu n
function get_logical_core_end() {
	# if there is only 1 thread per core,
	# then this system doesn't support SMT
	local THREADS_PER_CORE=$(get_threads_per_core)
	if [ $THREADS_PER_CORE -eq 1 ]; then
		echo "Warning: This CPU does not support SMT" >&2
		return
	fi

	local __ARG_NUMA_NODE=0
	local __ARG_CPU_NUM=0
	local __ARG_CPU_START=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--physcpu-num)
				shift
				__ARG_CPU_NUM=$1
				;;
			--cpu-offset)
				shift
				__ARG_CPU_START=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				;;
		esac
		shift
	done

	local CPU_START=$(get_logical_core_start --node $__ARG_NUMA_NODE --cpu-offset $__ARG_CPU_START)

	# gets the cpus assigned to this numa node
	local NODE_CPUS=$(get_numa_node_cpus $__ARG_NUMA_NODE)
	# in cases where cpus are like: 0-63,64-127
	# 0-63 are physical cores
	# 64-127 are logical cores
	if [[ "$NODE_CPUS" == *,* ]]; then
		# we only want the physical cores (64-127)
		NODE_CPUS=$(echo $NODE_CPUS | cut -d ',' -f 2)
		# subtract 1 because we count CPU_START as a cpu too
		# e.g. assuming we are starting at cpu 0
		#   12-15 -> 0 + 12 + 4 = 16
		#   16 - 1 = 15
		local RESULT=$(expr $CPU_START + $__ARG_CPU_NUM - 1)

		local LOG_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
		if [ $RESULT -gt $LOG_CPU_MAX ]; then
			echo "Warning: asked for $RESULT cpus, but only have $LOG_CPU_MAX cpus" >&2
			RESULT=$LOG_CPU_MAX
		fi

		echo $RESULT
		return
	fi

	local RESULT=$(expr $CPU_START - 1 + $__ARG_CPU_NUM \* $THREADS_PER_CORE - 1)

	local LOG_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
	if [ $RESULT -gt $LOG_CPU_MAX ]; then
		echo "Warning: asked for $RESULT cpus, but only have $LOG_CPU_MAX cpus" >&2
		RESULT=$LOG_CPU_MAX
	fi

	echo $RESULT
}

# Creates a string of comma separated logical cpu numbers that we specified.
# i.e. generate_logical_cores --node 0 --start 1 --end 7
# where smt is 2.
# output: 1,3,5,7
# i.e. generate_logical_cores --node 0 --start 1 --end 16
# where smt is 8.
# output: 1,2,3,4,5,6,7,9,10,11,12,13,14,15,16
#
# args:
# --node n: allocate cpus on node n
# --start n: start at cpu n when allocating
# --end n: end at cpu n when allocating
function generate_logical_cores() {
	# if there is only 1 thread per core,
	# then this system doesn't support SMT
	local THREADS_PER_CORE=$(get_threads_per_core)
	if [ $THREADS_PER_CORE -eq 0 ]; then
		echo "Warning: This CPU does not support SMT" >&2
		return
	fi

	local __ARG_NUMA_NODE=0
	local __ARG_CPU_FIRST=0
	local __ARG_CPU_LAST=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--start)
				shift
				__ARG_CPU_FIRST=$1
				;;
			--end)
				shift
				__ARG_CPU_LAST=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				exit 1
				;;
		esac
		shift
	done

	local CPU_NUM=$(expr $__ARG_CPU_LAST - $__ARG_CPU_FIRST)

	local NODE_CPUS=$(get_numa_node_cpus $__ARG_NUMA_NODE)

	if [[ "$NODE_CPUS" == *,* ]]; then
		# we only want the physical cores (64-127)
		NODE_CPUS=$(echo $NODE_CPUS | cut -d ',' -f 2)

		# in case we ask for more cpus than the machine has
		local LOG_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
		if [ $__ARG_CPU_LAST -gt $LOG_CPU_MAX  ]; then
			echo "Warning: asked for $__ARG_CPU_LAST cpus, but only have $LOG_CPU_MAX cpus" >&2
			$__ARG_CPU_LAST=$LOG_CPU_MAX
		fi

		local RESULT=$(seq -s ',' $__ARG_CPU_FIRST $__ARG_CPU_LAST)
		echo $RESULT
		return
	fi

	# in case we ask for more cpus than the machine has
	local LOG_CPU_MAX=$(echo $NODE_CPUS | cut -d '-' -f 2)
	if [ $__ARG_CPU_LAST -gt $LOG_CPU_MAX  ]; then
		echo "Warning: asked for $__ARG_CPU_LAST cpus, but only have $LOG_CPU_MAX cpus" >&2
		$__ARG_CPU_LAST=$LOG_CPU_MAX
	fi

	local RESULT=""
	local CPU_SEQ=$(seq -s ' ' $__ARG_CPU_FIRST $THREADS_PER_CORE $__ARG_CPU_LAST)
	for start in $CPU_SEQ; do
		local threads_left=$(expr $THREADS_PER_CORE - 1)
		local end=$(expr $start + $threads_left - 1)

		RESULT="$RESULT "$(seq -s ' ' $start $end)
	done
	RESULT=$(echo $RESULT | tr ' ' ',')
	echo $RESULT
}

# Creates a string of comma separated logical cpu numbers that we specified.
# i.e. get_logical_cores --node 0 --cpus 2 --cpu-offset 1
# where smt is 2.
# output: 3,5
# i.e. get_logical_cores --node 0 --cpus 2 --cpu-offset 2
# where smt is 2.
# output: 5,7
#
# args:
# --node n: allocate cpus on node n
# --cpus n: allocate n number of cpu worth of logical cpus
# --cpu-offset n: start allocating cpus at n
function get_logical_cores() {
	# if there is only 1 thread per core,
	# then this system doesn't support SMT
	local THREADS_PER_CORE=$(get_threads_per_core)
	if [ $THREADS_PER_CORE -eq 0 ]; then
		echo "Warning: This CPU does not support SMT" >&2
		return
	fi

	local __ARG_NUMA_NODE=0
	local __ARG_CPU_NUM=0
	local __ARG_CPU_START=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--cpus)
				shift
				__ARG_CPU_NUM=$1
				;;
			--cpu-offset)
				shift
				__ARG_CPU_START=$1
				;;
			*)
				echo "$0: Unknown parameter: $1" >&2
				exit 1
				;;
		esac
		shift
	done

	local CPU_FIRST=$(get_logical_core_start --node $__ARG_NUMA_NODE --cpu-offset $__ARG_CPU_START)
	local CPU_LAST=$(get_logical_core_end --node $__ARG_NUMA_NODE --cpu-offset $__ARG_CPU_START --physcpu-num $__ARG_CPU_NUM)

	local RESULT=$(generate_logical_cores --node $__ARG_NUMA_NODE --start $CPU_FIRST --end $CPU_LAST)
	echo $RESULT
}

# Returns a shortened and formatted version of the cpus we want to allocate
#
# args:
# --node i: allocate cpus on node i
# --physcpu-num n: allocate n number of physical cpus
# --smt true/false: takes into account logical cpus
# --cpu-offset n: start allocating from physical cpu n
function get_cpus() {
	local __ARG_NUMA_NODE=0
	local __ARG_CPU_NUM=0
	local __ARG_SMT=1
	local __ARG_CPU_START=0
	while [ -n "$1" ]; do
		case $1 in
			--node)
				shift
				__ARG_NUMA_NODE=$1
				;;
			--physcpu-num)
				shift
				__ARG_CPU_NUM=$1
				;;
			--smt)
				shift
				case $1 in
					true)
						__ARG_SMT=1
						;;
					false)
						__ARG_SMT=0
						;;
					*)
						__ARG_SMT=1
						;;
				esac
				;;
			--cpu-offset)
				shift
				__ARG_CPU_START=$1
				;;
			*)
				echo "Unknown parameter: $1" >&2
				;;
		esac
		shift
	done

	local PHY_CORES=$(get_physical_cores --node $__ARG_NUMA_NODE --cpus $__ARG_CPU_NUM --cpu-offset $__ARG_CPU_START)

	local THREADS_PER_CORE=$(get_threads_per_core)
	local RESULT=$PHY_CORES
	if [ $__ARG_SMT -eq 1 ]; then
		if [ $THREADS_PER_CORE -eq 1 ]; then
			echo "Warning: Platform does not support SMT" >&2
		else
			local LOG_CORES=$(get_logical_cores --node $__ARG_NUMA_NODE --cpus $__ARG_CPU_NUM --cpu-offset $__ARG_CPU_START)

			RESULT=$RESULT,$LOG_CORES
		fi
	fi
	RESULT=$(echo $RESULT | tr ',' '\n' | sort -n | tr '\n' ',')
	RESULT=$(__shrink_format_cpu $RESULT)

	echo $RESULT
}

# Checks whether the affinity tool is intalled/supported
# If the tool is not installed/supported, it unsets all the affinity variables
# This helps in avoiding errors while running some java command with affinity when affinity tool is missing

function affinity_tool_install_check() {

	${SERVER_AFFINITY_CMD} java -version > /dev/null 2>&1
	if [ $? -eq 0 ]; then
	    echo "Affinity tool is intalled/supported."
	else
	    echo "Warning!!! Affinity tool is NOT installed/supported. Unsetting affinity variables: SERVER_AFFINITY_CMD, LOAD_AFFINITY_CMD, & DB_AFFINITY_CMD"
	    unset SERVER_AFFINITY_CMD LOAD_AFFINITY_CMD DB_AFFINITY_CMD
	fi
}

# Given the cpus and node, generate a command for the specific platform
# e.g. generate_cpu_set_command --physcpubind 0-3 --membind 0
# output: numactl --physcpubind=0-3 --membind=0
#
# args:
# --physcpubind s: the cpus to allocate
# --membind n: the node to allocate for
function generate_cpu_set_command() {
	local __ARG_CPU_BIND=0
	local __ARG_MEM_BIND=0
	while [ -n "$1" ]; do
		case $1 in
			--physcpubind)
				shift
				__ARG_CPU_BIND=$1
				;;
			--membind)
				shift
				__ARG_MEM_BIND=$1
				;;
			*)
				echo "Unknown parameter: $1" >&2
				;;
		esac
		shift
	done

	local PLATFORM=$(get_platform)
	case "$PLATFORM" in
		Linux-s390x)
			echo "taskset -c $__ARG_CPU_BIND"
			;;
		Linux-*)
			echo "numactl --physcpubind=$__ARG_CPU_BIND --membind=$__ARG_MEM_BIND"
			;;
		Windows-*)
			echo "cmd /C start /B /WAIT /AFFINITY $__ARG_CPU_BIND /NODE $__ARG_MEM_BIND"
			;;
		AIX-*)
			echo "execrset -c $__ARG_CPU_BIND -e"
			;;
		OS/390*)
			echo "oeconsol \"cf cpu($__ARG_CPU_BIND),online\""
			;;
#		Darwin*)
#			TODO: No tools found for setting affinity yet
#			;;
		*)
			echo "Unsupported platform: $PLATFORM" >&2
			;;
	esac
}

function printMachineCPUInfo {
	HOSTNAME=$(hostname -s)

	echo "HOSTNAME=${HOSTNAME}"
	echo "Printing Machine Info"
	echo "Platform:" $(get_platform)
	echo "Operating System:" $(get_os)
	echo "CPU Architecture:" $(get_cpu_arch)
	echo "Number of CPUs:" $(get_cpu_count)
	echo "Threads Per Core:" $(get_threads_per_core)
	echo "Cores Per Socket:" $(get_phys_cores_per_socket)
	echo "Nodes:" $(get_node_count)
	local i=0
	for cpu in ${__AFFINITY_NUMA_NODE_ARR[@]}; do
		echo "Node($i) CPUs:" $cpu
		i=$(expr $i + 1)
	done

	if [ ! -z "$AFFINITY_VERBOSE" ]; then
		local os=$(get_os)
		case "$os" in
		Linux)
			echo_and_run "lscpu"
			;;
		AIX)
			echo_and_run "prtconf"
			echo_and_run "lssrad -a"
			local lssrad_wc=$(expr $(lssrad -a | wc -l) - 1)
			for (( i=0; i<$lssrad_wc; i++ )); do
				echo_and_run "lssrad -vs $i"
			done
			;;
		Windows)
			echo_and_run "wmic cpu get DeviceID, NumberOfCores, NumberOfLogicalProcessors, SocketDesignation"
			;;
		OS/390*)
			echo "oeconsol 'd m=core'"
			oeconsol 'd m=core'
			;;
		Darwin)
			echo_and_run "sysctl -a | grep machdep.cpu"
			;;
		*)
			echo "Unsupported platform: $os" >&2
			;;
		esac
	fi
}

function main() {
	return
}

__init_platform
__init_cpus
printMachineCPUInfo

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
	main "$@"
fi
