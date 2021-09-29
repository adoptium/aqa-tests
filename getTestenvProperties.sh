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

REPO_DIR=""
OUTPUT_FILE="../SHA.txt"


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
	{ echo "================================================"; echo "timestamp: $(timestamp)"; echo "repo dir: $REPO_DIR"; echo "git repo: "; git remote show origin -n | grep "Fetch URL:"; echo "sha:"; git rev-parse HEAD; }  2>&1 | tee -a $OUTPUT_FILE
}
parseCommandLineArgs "$@"
getSHA
