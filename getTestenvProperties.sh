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

declare -A map

REPO_DIR=""
OUTPUT_FILE="../../testenv.properties"
map[SDKDIR]=""
map[TESTDIR]="$(pwd)"
map[PLATFORM]=""
map[JVMVERSION]=""
map[SDK_RESOURCE]="nightly"
map[CUSTOMIZED_SDK_URL]=""
map[CUSTOMIZED_SDK_SOURCE_URL]=""
map[CLONE_OPENJ9]="true"
map[OPENJ9_REPO]="https://github.com/eclipse-openj9/openj9.git"
map[OPENJ9_SHA]=""
map[OPENJ9_BRANCH]=""
map[TKG_REPO]="https://github.com/adoptium/TKG.git"
map[TKG_BRANCH]="master"
map[VENDOR_REPOS]=""
map[VENDOR_SHAS]=""
map[VENDOR_BRANCHES]=""
map[VENDOR_DIRS]=""
map[JDK_VERSION]="8"
map[JDK_IMPL]="openj9"
map[RELEASES]="latest"
map[TYPE]="jdk"
map[TEST_IMAGES_REQUIRED]=true
map[DEBUG_IMAGES_REQUIRED]=true


usage ()
{
	echo 'This script use git command to get sha in the provided REPO_DIR HEAD and write the info into the OUTPUT_FILE'
	echo 'Usage : '
	echo '                --repo_dir: local git repo dir'
	echo '                --output_file: the file to write the sha info to. Default is to ../SHA.txt'

}

parseCommandLineArgs()
{
	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--repo_dir" | "-d" )
				REPO_DIR="$1"; shift;;

			"--output_file" | "-o" )
				OUTPUT_FILE="$1"; shift;;

			"--sdkdir" | "-s" )
				map[SDKDIR]="$1"; shift;;

			"--platform" | "-p" )
				map[PLATFORM]="$1"; shift;;

			"--jdk_version" | "-j" )
				map[JDK_VERSION]="$1"; shift;;

			"--jdk_impl" | "-i" )
				map[JDK_IMPL]="$1"; shift;;

			"--releases" | "-R" )
				map[RELEASES]="$1"; shift;;

			"--type" | "-T" )
				map[TYPE]="$1"; shift;;

			"--sdk_resource" | "-r" )
				map[SDK_RESOURCE]="$1"; shift;;

			"--customizedURL" | "-c" )
				map[CUSTOMIZED_SDK_URL]="$1"; shift;;

			"--customized_sourceURL" | "-S" )
				map[CUSTOMIZED_SDK_SOURCE_URL]="$1"; shift;;

			"--username" )
				map[USERNAME]="$1"; shift;;

			"--password" )
				map[PASSWORD]="$1"; shift;;

			"--clone_openj9" )
				map[CLONE_OPENJ9]="$1"; shift;;

			"--openj9_repo" )
				map[OPENJ9_REPO]="$1"; shift;;

			"--openj9_sha" )
				map[OPENJ9_SHA]="$1"; shift;;

			"--openj9_branch" )
				map[OPENJ9_BRANCH]="$1"; shift;;

			"--tkg_repo" )
				map[TKG_REPO]="$1"; shift;;

			"--tkg_branch" )
				map[TKG_BRANCH]="$1"; shift;;

			"--vendor_repos" )
				map[VENDOR_REPOS]="$1"; shift;;

			"--vendor_shas" )
				map[VENDOR_SHAS]="$1"; shift;;

			"--vendor_branches" )
				map[VENDOR_BRANCHES]="$1"; shift;;

			"--vendor_dirs" )
				map[VENDOR_DIRS]="$1"; shift;;

			"--test_images_required" )
				map[TEST_IMAGES_REQUIRED]="$1"; shift;;

			"--debug_images_required" )
				map[DEBUG_IMAGES_REQUIRED]="$1"; shift;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
	if [ -z "$REPO_DIR" ] || [ -z "$OUTPUT_FILE" ] || [ ! -d "$REPO_DIR" ]; then
		echo "Error, please see the usage and also check if $REPO_DIR is existing"
		usage
		exit 1
	fi
}

timestamp() {
  date +"%Y%m%d-%H%M%S"
}

getSHA()
{
	echo "Check sha in $REPO_DIR and store the info in $OUTPUT_FILE"
	if [ ! -e ${OUTPUT_FILE} ]; then
		echo "touch $OUTPUT_FILE"
		touch $OUTPUT_FILE
	fi

	cd $REPO_DIR
	# append the info into $OUTPUT_FILE
	for i in "${!map[@]}"
	do
		echo "$i" | tee -a $OUTPUT_FILE;
	done


	{ echo "================================================"; echo "timestamp: $(timestamp)"; echo "repo dir: $REPO_DIR"; echo "git repo: "; git remote show origin -n | grep "Fetch URL:"; echo "sha:"; git rev-parse HEAD; }  2>&1 | tee -a $OUTPUT_FILE
}
parseCommandLineArgs "$@"
getSHA
