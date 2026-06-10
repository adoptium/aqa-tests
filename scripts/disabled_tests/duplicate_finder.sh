#!/usr/bin/env bash

################################################################################
# Duplicate Test Exclusion Finder Script
# 
# This script takes a file containing a list of exclude files, and determines
# whether any of the uncommented tests appear more than once in any one file.
#
# Usage:
#   ./duplicate_finder.sh file_containing_a_list_of_exclude_files
################################################################################

set -e  # Exit on error
set -o pipefail # Exit on pipe failure

if [[ $# -ne 1 ]]; then
  echo "Error: Missing mandatory argument."
  exit 1
fi

if [[ ! -r "$1" ]]; then
  echo "Error: Argument file either does not exist or cannot be read."
  exit 1
fi

list_of_exclude_files="$1"

duplicates=""
while IFS= read -r exclude_file_raw; do
  exclude_file="$exclude_file_raw"
  if [[ ! -r "$exclude_file" ]]; then
    exclude_file="$(pwd)/$exclude_file_raw"
    if [[ ! -r "$exclude_file" ]]; then
      echo "Cannot locate exclude file $exclude_file_raw"
      exit 1
    fi
  fi
  echo "Processing: $exclude_file"
  exclude_file_contents=$(grep -E '^[[:space:]]*[^#[:space:]]' "$exclude_file" | awk '{print $1}' | sort || true)
  if [[ "$exclude_file_contents" == "" ]]; then
    echo "Warning: Skipping exclude file. No tests found. File name: $exclude_file"
    continue
  fi
  dup_tests_in_problemlist="$(printf '%s\n' "$exclude_file_contents" | uniq -d -c)"
  if [[ "${dup_tests_in_problemlist}" != '' ]]; then
    while IFS= read -r single_dup; do
      duplicates+="${exclude_file}: $(echo ${single_dup} | sed 's/^ *//')\n";
    done <<< "$dup_tests_in_problemlist"
  fi
done < "$list_of_exclude_files"

if [[ $duplicates ]]; then
  echo ""
  echo "---- Processing complete. Duplicate tests found. ----"
  echo ""
  echo -e "$duplicates"
  echo "Script failed."
  exit 1
fi

echo "Script passed."
exit 0