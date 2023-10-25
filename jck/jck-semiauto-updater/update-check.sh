#!/bin/bash

# File to store the previous state
PREVIOUS_STATE_FILE="/home/jenkins/previous_state.txt"

function list_folders_depth {
    local folder_url="$1"
    local depth="$2"
    if [ "$depth" -eq 0 ]; then
        return
    fi
    
    local children=$(curl -X GET -ks -H X-JFrog-Art-Api:${ARTIFACTORY_TOKEN} $folder_url | grep -o '"uri" : "[^"]*"' | awk -F': "' '{print $2}' | sed 's/"$//' | grep -v "$folder_url$")
    for child in $children; do
        if [ "$depth" -eq 1 ]; then
            echo "$folder_url$child"
        else
            local child_url="${folder_url}${child}"
            list_folders_depth "$child_url" "$((depth - 1))"
        fi
    done
}

function compare_states {

    # Read the previous state from the file, or create an empty file if it doesn't exist
    if [ -e "$PREVIOUS_STATE_FILE" ]; then
        PREVIOUS_STATE=$(cat "$PREVIOUS_STATE_FILE")
    else
        PREVIOUS_STATE=""
    fi

    # Retrieve the current state of the repository
    CURRENT_STATE=$(list_folders_depth "$ARTIFACTORY_API_URL" 3)
    # Calculate the difference between the initial state and current state
    added_folders=$(comm -13 <(echo "$PREVIOUS_STATE" | sort) <(echo "$CURRENT_STATE" | sort))
    
    if [ -n "$added_folders" ]; then
        
        JDK_LIST=()
        IFS=$'\n'
        for folder_entry in $added_folders; do
            JDK=$(echo "$folder_entry" | awk -F'/' '{print $(NF-2)}')
            UPDATE=$(echo "$folder_entry" | awk -F'/' '{print $(NF)}')
            JDK_LIST+=("$JDK $UPDATE")
        done
        jdk_values=$(IFS=,; echo "${JDK_LIST[*]}")
        echo "Update available for JDK= $jdk_values"
    else
        echo "No new folders detected."
    fi

    # Store the current state as the new previous state
    echo "$CURRENT_STATE" > "$PREVIOUS_STATE_FILE"
}

parseCommandLineArgs()
{
	while [ $# -gt 0 ] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			
			"--artifactory_token" | "-at")
				ARTIFACTORY_TOKEN="$1"; shift;;
			
			"--artifactory_url" | "-au")
				ARTIFACTORY_API_URL="$1"; shift;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
}

parseCommandLineArgs "$@"

if [ "$ARTIFACTORY_TOKEN" != "" ] && [ "$ARTIFACTORY_API_URL" != "" ]  ; then
    compare_states
else
	echo "Please provide missing arguments"
	exit 1
fi