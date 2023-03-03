#!/usr/bin/env bash

set -eo pipefail

TAPDIR="AQAvitTapFiles"
DOWNLOAD_URL=""
PLATFORM="unknown"

usage ()
{
	echo 'Usage : aqaTap.sh --url <link to download TAP files> --dir <dir to store TAP files>'
}

parseArgs() {
	while [ $# -gt 0 ] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--url" | "-u" )
				DOWNLOAD_URL="$1"; shift;;

			"--dir" | "-d" )
				TAPDIR="$1"; shift;;

			"--platform" | "-p" )
				PLATFORM="$1"; shift;;

			"--username" )
				USERNAME="$1"; shift;;

			"--password" )
				PASSWORD="$1"; shift;;

			"--clean" | "-c" )
				clean; exit 0;;

			"--deleteGrinder" | "-dg" )
				deleteGrinder; exit 0;;

			"--listAll" | "-l" )
				listAll; exit 0;;

			"--listFailed" | "-lf" )
				listFailed; exit 0;;

			"--listRerun" | "-lr" )
				listRerun; exit 0;;

			"--listTAP" | "-lt" )
				listTAP; exit 0;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done

	echo "TAPDIR: $TAPDIR"
	echo "PLATFORM: $PLATFORM"
}

downloadTAP() {
	rm -rf "$PLATFORM"
	mkdir "$PLATFORM"
	rm -rf tmp
	mkdir tmp
	cd tmp
	if [ "$USERNAME" != "" ] && [ "$PASSWORD" != "" ]; then
		curl_options="--user $USERNAME:$PASSWORD"
	fi
	echo "curl -OLJSks ${curl_options} ${DOWNLOAD_URL}"
	curl -OLJSks ${curl_options} "${DOWNLOAD_URL}"
	jar_file=$(ls ./*.zip)
	jar_file_array=("${jar_file//\\n/ }")
	unzip -q "${jar_file_array[0]}" -d .
	jar_dirs=$(ls -d ./*/)
	jar_dir_array=("${jar_dirs//\\n/ }")
	mv "${jar_dir_array[0]}" "$PLATFORM"
	mv "$PLATFORM" ../
	cd ..
	rm -rf tmp
}

clean() {
	echo "============================"
	echo "clean files in $(pwd)"
	echo "============================"
	echo "List and remove the above *.jck_* ..."
	find . -name "*.jck_*"
	find . -name "*.jck_*" -exec rm -f {} ';'

	echo "List and remove the above *_special.system_* ..."
	find . -name "*_special.system_*"
	find . -name "*_special.system_*" -exec rm -f {} ';'

	echo "List and remove the above *.external_* ..."
	find . -name "*.external_*"
	find . -name "*.external_*" -exec rm -f {} ';'

	echo "List and remove the above *_dev.* ..."
	find . -name "*_dev.*"
	find . -name "*_dev.*" -exec rm -f {} ';'

	echo "List and remove the above .DS_Store ..."
	find . -name ".DS_Store"
	find . -name ".DS_Store" -exec rm -f {} ';'

	echo "List and remove the above *_fips* ..."
	find . -name "*_fips*"
	find . -name "*_fips*" -exec rm -f {} ';'

	echo "Print empty dir ..."
	find . -type d -empty -print
	echo "Delete empty dir ..."
	find . -type d -empty -delete
}

deleteGrinder() {
	echo "============================"
	echo "List and remove *Grinder_* ..."
	echo "============================"
	find . -name "*Grinder_*"
	find . -name "*Grinder_*" -exec rm -f {} ';'
}

listAll() {
	echo "============================"
	echo "list all files in $(pwd)"
	echo "============================"
	find . -type f | sort
}

listFailed() {
	echo "============================"
	echo "List failed tests in $(pwd)"
	echo "============================"
	grep -R 'not ok ' . || true
}

listRerun() {
	echo "============================"
	echo "list rerun in Grinder..."
	echo "============================"
	find . -name "*Grinder_*" | xargs grep 'ok '
}

listTAP() {
	echo "============================"
	echo "list AQAvit TAP files in $(pwd)"
	echo "============================"
	echo "1. List sanity.openjdk ..."
	printf '%s\n' *"sanity.openjdk"*.tap
	echo "2. List extended.openjdk ..."
	printf '%s\n' *"extended.openjdk"*.tap
	echo "3. List sanity.functional ..."
	printf '%s\n' *"sanity.functional"*.tap
	echo "4. List extended.functional ..."
	printf '%s\n' *"extended.functional"*.tap
	echo "5. List special.functional ..."
	printf '%s\n' *"special.functional"*.tap
	echo "6. List sanity.system ..."
	printf '%s\n' *"sanity.system"*.tap
	echo "7. List extended.openjdk ..."
	printf '%s\n' *"extended.system"*.tap
	echo "8. List sanity.perf ..."
	printf '%s\n' *"sanity.perf"*.tap
	echo "9. List extended.perf ..."
	printf '%s\n' *"extended.perf"*.tap
}

parseArgs "$@"
mkdir -p "$TAPDIR"
cd "$TAPDIR"
if [ "$DOWNLOAD_URL" ]; then
	downloadTAP
fi
cd "$PLATFORM"
listAll
clean
listTAP
listFailed
