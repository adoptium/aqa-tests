#!/usr/bin/env bash
#
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

declare -A vars

vars["SDKDIR"]=""
vars["TESTDIR"]="$(pwd)"
vars["PLATFORM"]=""
vars["JVMVERSION"]=""
vars["SDK_RESOURCE"]="nightly"
vars["CUSTOMIZED_SDK_URL"]=""
vars["CUSTOMIZED_SDK_SOURCE_URL"]=""
vars["CLONE_OPENJ9"]="true"
vars["OPENJ9_REPO"]="https://github.com/eclipse-openj9/openj9.git"
vars["OPENJ9_SHA"]=""
vars["OPENJ9_BRANCH"]=""
vars["TKG_REPO"]="https://github.com/adoptium/TKG.git"
vars["TKG_BRANCH"]="master"
vars["VENDOR_REPOS"]=""
vars["VENDOR_SHAS"]=""
vars["VENDOR_BRANCHES"]=""
vars["VENDOR_DIRS"]=""
vars["JDK_VERSION"]="8"
vars["JDK_IMPL"]="openj9"
vars["RELEASES"]="latest"
vars["TYPE"]="jdk"
vars["TEST_IMAGES_REQUIRED"]=true
vars["DEBUG_IMAGES_REQUIRED"]=true


usage ()
{
	echo 'Usage : get.sh -platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix'
	echo '                [--jdk_version|-j ]: optional. JDK version'
	echo '                [--jdk_impl|-i ]: optional. JDK implementation'
	echo '                [--releases|-R ]: optional. Example: latest, jdk8u172-b00-201807161800'
	echo '                [--type|-T ]: optional. jdk or jre'
	echo '                [--sdkdir|-s binarySDKDIR] : if do not have a local sdk available, specify preferred directory'
	echo '                [--sdk_resource|-r ] : indicate where to get sdk - releases, nightly , upstream or customized'
	echo '                [--customizedURL|-c ] : indicate sdk url if sdk source is set as customized.  Multiple urls can be passed with space as separator'
	echo '                [--customized_sourceURL|-S ] : indicate sdk source url if sdk source is set as customized.'
	echo '                [--username ] : indicate username required if customized url requiring authorization is used'
	echo '                [--password ] : indicate password required if customized url requiring authorization is used'
	echo '                [--clone_openj9 ] : optional. ture or false. Clone openj9 if this flag is set to true. Default to true'
	echo '                [--openj9_repo ] : optional. OpenJ9 git repo. Default value https://github.com/eclipse-openj9/openj9.git is used if not provided'
	echo '                [--openj9_sha ] : optional. OpenJ9 pull request sha.'
	echo '                [--openj9_branch ] : optional. OpenJ9 branch.'
	echo '                [--tkg_repo ] : optional. TKG git repo. Default value https://github.com/adoptium/TKG.git is used if not provided'
	echo '                [--tkg_branch ] : optional. TKG branch.'
	echo '                [--vendor_repos ] : optional. Comma separated Git repository URLs of the vendor repositories'
	echo '                [--vendor_shas ] : optional. Comma separated SHAs of the vendor repositories'
	echo '                [--vendor_branches ] : optional. Comma separated vendor branches'
	echo '                [--vendor_dirs ] : optional. Comma separated directories storing vendor test resources'
}
parseCommandLineArgs()
{
	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--sdkdir" | "-s" )
				vars["SDKDIR"]="$1"; shift;;

			"--testdir" | "-t" )
				vars["TESTDIR"]="$1"; shift;;

			"--platform" | "-p" )
				vars["PLATFORM"]="$1"; shift;;

			"--jdk_version" | "-j" )
				vars["JDK_VERSION"]="$1"; shift;;

			"--jdk_impl" | "-i" )
				vars["JDK_IMPL"]="$1"; shift;;

			"--releases" | "-R" )
				vars["RELEASES"]="$1"; shift;;

			"--type" | "-T" )
				vars["TYPE"]="$1"; shift;;

			"--sdk_resource" | "-r" )
				vars["SDK_RESOURCE"]="$1"; shift;;

			"--customizedURL" | "-c" )
				vars["CUSTOMIZED_SDK_URL"]="$1"; shift;;

			"--customized_sourceURL" | "-S" )
				vars["CUSTOMIZED_SDK_SOURCE_URL"]="$1"; shift;;

			"--username" )
				vars["USERNAME"]="$1"; shift;;

			"--password" )
				vars["PASSWORD"]="$1"; shift;;

			"--clone_openj9" )
				vars["CLONE_OPENJ9"]="$1"; shift;;

			"--openj9_repo" )
				vars["OPENJ9_REPO"]="$1"; shift;;

			"--openj9_sha" )
				vars["OPENJ9_SHA"]="$1"; shift;;

			"--openj9_branch" )
				vars["OPENJ9_BRANCH"]="$1"; shift;;

			"--tkg_repo" )
				vars["TKG_REPO"]="$1"; shift;;

			"--tkg_branch" )
				vars["TKG_BRANCH"]="$1"; shift;;

			"--vendor_repos" )
				vars["VENDOR_REPOS"]="$1"; shift;;

			"--vendor_shas" )
				vars["VENDOR_SHAS"]="$1"; shift;;

			"--vendor_branches" )
				vars["VENDOR_BRANCHES"]="$1"; shift;;

			"--vendor_dirs" )
				vars["VENDOR_DIRS"]="$1"; shift;;

			"--test_images_required" )
				vars["TEST_IMAGES_REQUIRED"]="$1"; shift;;

			"--debug_images_required" )
				vars["DEBUG_IMAGES_REQUIRED"]="$1"; shift;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done

	echo "TESTDIR: $TESTDIR"
}

timestamp() {
  date +"%Y%m%d-%H%M%S"
}

writeTestenvProperties()
{
	OUTPUT_FILE="testenv.properties"
	echo "Check sha in $REPO_DIR and store the info in $OUTPUT_FILE"
	if [ ! -e ${OUTPUT_FILE} ]; then
		echo "touch $OUTPUT_FILE"
		touch $OUTPUT_FILE
	fi

	# append the info into $OUTPUT_FILE
	for key in "${!vars[@]}"
	do
		echo "$key=${vars[$key]}" | tee -a $OUTPUT_FILE
	done
}
parseCommandLineArgs "$@"
writeTestenvProperties
