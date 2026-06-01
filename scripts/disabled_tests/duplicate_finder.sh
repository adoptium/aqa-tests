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
  exclude_file_contents=$(grep -o '^[^#].[^[:space:]]*' "$exclude_file" || true)
  if [[ "$exclude_file_contents" == "" ]]; then
    echo "Warning: Skipping exclude file. No tests found. File name: $exclude_file"
    continue
  fi
  while IFS= read -r test; do
    test_regex="$(echo "$test" | sed 's/[^a-zA-Z0-9]/./g')"
    num_of_test_mentions="$(echo "$exclude_file_contents" | grep -c "^${test_regex}$" || true)"
    if [[ "$num_of_test_mentions" != "1" ]]; then
      [[ $duplicates != *"$test"* ]] && duplicates+="$exclude_file - $test - $num_of_test_mentions instances\n";
    fi
  done <<< "$exclude_file_contents"
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