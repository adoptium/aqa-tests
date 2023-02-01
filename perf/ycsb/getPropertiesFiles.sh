#!/usr/bin/env bash
# Check if .properties files exist in the current directory
# If they do, print them and copy them to arg $ycsb_dir

true=0
false=1

propFilesExist() {
    propFiles=($(find . -type f -name "*.properties"))
    if [ ${#propFiles[@]} -gt 0 ]; then 
        echo "Found ${#propFiles[@]} properties files:"
        echo "${propFiles[*]}"
        return $true
    else
        echo "No properties files found."
        return $false
    fi
}

ycsb_dir=$1
if propFilesExist; then
    echo "Copying properties files to $ycsb_dir"
    cp ./*.properties "$ycsb_dir"
fi
