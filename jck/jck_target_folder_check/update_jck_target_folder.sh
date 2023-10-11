#!/usr/bin/env bash

directory=$1
sourceFilePath=$2

while IFS= read -r line; do
    # Check for the start of a match
    if [[ $line == "New test folder detected:"* ]]; then
        match="$line"
    elif [[ $line == "</test>" ]]; then
        match="$match"$'\n\t'"$line"
        matches+=("$match")
        match=""
    elif [[ -n $match ]]; then
        match="$match"$'\n\t'"$line"
    fi
done < "$sourceFilePath"

for ((i=0; i<${#matches[@]}; i++)); do
    match="${matches[i]}"
    path=$(echo "$match" | grep -o "New test folder detected: .*" |awk -F'/' '{print $2}')
    if [[ $match == *"COMPILER"* ]]; then
        targetFilePath="compiler.$path/playlist.xml"
    elif [[ $match == *"RUNTIME"* ]]; then
        targetFilePath="runtime.$path/playlist.xml"
    elif [[ $match == *"DEVTOOLS"* ]]; then
        targetFilePath="devtools.$path/playlist.xml"
    else
        echo "No target file mapping found for path: $path "
    fi

    targetMatch=""
    if [[ $match =~ "<test>"(.*)"</test>" ]]; then 
        targetMatch=$'\t<test>'"${BASH_REMATCH[1]}</test>"
    fi

    tempfile=$(mktemp)
    filePath=$directory/$targetFilePath
    lastLine=$(tail -n 1 "$filePath")
    sed '$d' "$filePath" > "$tempfile"
    echo "$targetMatch" >> "$tempfile"
    echo "$lastLine" >> "$tempfile"
    mv "$tempfile" "$filePath"
done

