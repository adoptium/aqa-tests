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

			"--checkFailed" | "-cf" )
				checkFailed; exit 0;;

			"--checkTAP" | "-ct" )
				checkTAP; exit 0;;

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

	echo "List and remove the above *_special.openjdk_* ..."
	find . -name "*_special.openjdk_*"
	find . -name "*_special.openjdk_*" -exec rm -f {} ';'

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

checkFailed() {
	echo "============================"
	echo "List failed tests in $(pwd)"
	echo "============================"
	if grep -R 'not ok ' . ; then
		echo "[ERROR]: There are failed tests"
		exit 1
	else
		echo "All Tests Passed!"
	fi
}

listRerun() {
	echo "============================"
	echo "list rerun in Grinder..."
	echo "============================"
	find . -name "*Grinder_*" | xargs grep 'ok '
	echo "============================"
	echo "list rerun in rerun build..."
	echo "============================"
	find . -name "*_rerun*" | xargs grep 'ok '
}

findCmd() {
	fileName=$2
	echo "----------------------------"
	echo "$1. List $fileName ..."
	if [ "$(find . -name "$fileName" ! -name "*_rerun*.tap")" ]; then
		find . -name "$fileName" ! -name "*_rerun*.tap" | sort
		numFiles=$(find . -name "$fileName" -type f ! -name "*_rerun*.tap" -print | wc -l)
		echo "Total num of Files:$numFiles"
		fileNameWithoutExt=${fileName//".tap"/""}
		if [ $numFiles == 1 ]; then
			if [ "$(find . -name "${fileNameWithoutExt}_testList*" ! -name "*_rerun*.tap")"  ]; then
				find . -name "${fileNameWithoutExt}_testList*" ! -name "*_rerun*.tap"
				echo "Found 1 testList file. Looks like this is a parallel run, so multiple testList files are expected."
				echo "[ERROR]: Missing testList TAP files"
				exit 1
			fi
		else
			for (( i=0; i < $numFiles; ++i ))
			do
				if [ "$(find . -name "${fileNameWithoutExt}_testList_${i}*" ! -name "*_rerun*.tap")" == "" ]; then
					echo "[ERROR]: Missing ${fileNameWithoutExt}_testList_${i}* TAP file"
					exit 1
				fi
			done
		fi
	else
		echo "[ERROR]: File not found"
		exit 1
	fi
}

checkTAP() {
	echo "============================"
	echo "check AQAvit TAP files in $(pwd)"
	echo "============================"

	findCmd 1 "*sanity.openjdk*.tap"
	findCmd 2 "*extended.openjdk*.tap"
	findCmd 3 "*sanity.functional*.tap"
	findCmd 4 "*extended.functional*.tap"
	findCmd 5 "*special.functional*.tap"
	findCmd 6 "*sanity.system*.tap"
	findCmd 7 "*extended.system*.tap"
	findCmd 8 "*sanity.perf*.tap"
	findCmd 9 "*extended.perf*.tap"
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
	echo "7. List extended.system ..."
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
