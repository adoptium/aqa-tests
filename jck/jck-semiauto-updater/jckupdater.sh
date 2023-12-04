#!/usr/bin/env bash

#set -eo pipefail


WORKSPACE="$(pwd)"/workspace
GIT_REPO="$(pwd)"/pr
JCK_VERSION=""
JCK_UPDATE_NUMBER=""
JCK_GIT_REPO=""
ARTIFACTORY_DOWNLOAD_URL=""
ARTIFACTORY_API_URL=""
ARTIFACTORY_EXC_URL=""
ARTIFACTORY_TOKEN=""
JCK_REPO_NAME=""
GIT_USER=""
JCK_GIT_BRANCH="autoBranch"
GIT_TOKEN=""
JCK_FOLDER_SUFFIX=""
GIT_EXCLUDE_API_URL="https://api.github.ibm.com/repos/runtimes"
JAVA_SDK_PATH="$WORKSPACE/../../../../jdkbinary/j2sdk-image"

usage ()
{
	echo 'Usage : jckupdater.sh  [--jck_version|-j ] : Indicate JCK version to update.'
	echo '                [--artifactory_token|-at] : Token to access JCK artifactory: https://eu.artifactory.swg-devops.com/artifactory/jim-jck-generic-local.'
	echo '                [--artifactory_url|-au] : Artifactory server URL to download JCK material'
	echo '                [--artifactory_api_url|-ap] : Artifactory API URL to fetch details on JCK material'
	echo '                [--jck-repo|-repo] : JCK GIT repo to update.'
	echo '                [--git_token|-gt] : Git API Token to create PR.'
	echo '                [--jck_git_branch|-gb ] :  Optional. JCK_GIT_BRANCH name to clone repo. Default is autoBranch'
	echo '                [--java_sdk|-java] : Optional. JAVA_SDK_PATH path. '

}


parseCommandLineArgs()
{
	while [ $# -gt 0 ] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--jck_version" | "-j" )
				JCK_VERSION="$1"; shift;;

			"--artifactory_token" | "-at")
				ARTIFACTORY_TOKEN="$1"; shift;;
			
			"--jck-repo" | "-repo")
				JCK_GIT_REPO="$1"; shift;;

			"--java_sdk" | "-java" )
				JAVA_SDK_PATH="$1"; shift;;

			"--git_token" | "-gt" )
			 	GIT_TOKEN="$1"; shift;;

			"--jck_git_branch" | "-gb")
				JCK_GIT_BRANCH="$1"; shift;;

			"--artifactory_url" | "-au")
				ARTIFACTORY_DOWNLOAD_URL="$1"; shift;;

			"--artifactory_api_url" | "-ap")
				ARTIFACTORY_API_URL="$1"; shift;;

			"--git_exclude_api_url" | "-eu")
				GIT_EXCLUDE_API_URL="$1"; shift;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
}

##Initial setup and print values provided
setup(){

	echo ""
	echo "JAVA_SDK_PATH=$JAVA_SDK_PATH"
	echo "WORKSPACE=$WORKSPACE"
	echo "GIT_REPO=$GIT_REPO"
	echo "JCK_VERSION=$JCK_VERSION"
	echo "JCK_GIT_REPO=$JCK_GIT_REPO"

	#Extract GIT_USER and JCK_REPO_NAME  from JCK_GIT_REPO
	IFS=":" read -ra parts <<< "$JCK_GIT_REPO"
	IFS="/" read -ra userAndRepo <<< "${parts[1]}"
	GIT_USER="${userAndRepo[0]}"
	JCK_REPO_NAME="${userAndRepo[1]}"

	echo "JCK_REPO_NAME=$JCK_REPO_NAME"
	echo "GIT_USER=$GIT_USER"
	echo "JCK_GIT_BRANCH=$JCK_GIT_BRANCH"
	
	JCK=$JCK_VERSION
	JCK_FOLDER_SUFFIX=$JCK_VERSION
	## Only for JCK 8 and 11 
	if [[ $JCK_VERSION == "8" ]]; then 
		JCK=1.8d
		JCK_FOLDER_SUFFIX="$JCK_VERSION"d
	elif [[ $JCK_VERSION == "11" ]]; then
		JCK=11a
		JCK_FOLDER_SUFFIX="$JCK_VERSION"a
	fi
	echo "JCK_FOLDER_SUFFIX=" $JCK_FOLDER_SUFFIX
	ARTIFACTORY_JCK_DOWNLOAD_URL="${ARTIFACTORY_DOWNLOAD_URL}/${JCK}/"
	ARTIFACTORY_EXC_URL="${ARTIFACTORY_API_URL}/${JCK}/exc/"
	echo "ARTIFACTORY_JCK_DOWNLOAD_URL=$ARTIFACTORY_JCK_DOWNLOAD_URL"
	echo "ARTIFACTORY_EXC_URL=$ARTIFACTORY_EXC_URL"
	echo "GIT_EXCLUDE_API_URL=$GIT_EXCLUDE_API_URL"
	echo ""

	mkdir $WORKSPACE
	mkdir $GIT_REPO
	mkdir $WORKSPACE/unpackjck
	mkdir $WORKSPACE/jckmaterial
}

list() {
	url="$1"
	file_list=$(curl -ks -H X-JFrog-Art-Api:${ARTIFACTORY_TOKEN} "${url}")

	# Use grep to filter out the content within <a href=""> tags
	file_names=$(echo "$file_list" | grep -o '<a href="[^"]*">' | sed 's/<a href="//;s/">//')	
}

isLatestUpdate() {
	cd $WORKSPACE/jckmaterial
	
	list "${ARTIFACTORY_JCK_DOWNLOAD_URL}tck/" # get the JCK update number from artifactory
	last_file=""
	for file in $file_names; do
		last_file="$file"
	done
	
	JCK_UPDATE_NUMBER=$last_file

	JCK_WITHOUT_BACKSLASH="${JCK_UPDATE_NUMBER%/*}"
	#remove any "ga" associated with JCK_BUILD_ID
	JCK_WITHOUT_BACKSLASH=$(echo "$JCK_WITHOUT_BACKSLASH" | sed 's/-ga//g')
	echo "JCK_BUILD_ID - $JCK_WITHOUT_BACKSLASH"
	GIT_URL="https://raw.github.ibm.com/runtimes/JCK$JCK_VERSION-unzipped/main/JCK-compiler-$JCK_FOLDER_SUFFIX/build.txt"

	curl -o "build.txt" $GIT_URL -H "Authorization: token $GIT_TOKEN"
	echo -e "JCK version in build.txt:\n$(cat build.txt)\n\n"
	
	#check if latest exclude file is available
	getExcludeFiles
	isExcludeUpdateNeeded=$?

	#Check if material update is needed.
	if grep -q "$JCK_WITHOUT_BACKSLASH" build.txt; then
		isTestMaterialUpdateNeeded=1
	else
		isTestMaterialUpdateNeeded=0
	fi

	if [[ $isTestMaterialUpdateNeeded -eq 1 &&  $isExcludeUpdateNeeded -eq 1 ]]; then
		echo " JCK$JCK_VERSION material is $JCK_WITHOUT_BACKSLASH in the repo $GIT_URL. It is up to date. No need to pull changes"
		cleanup
		exit 2
	else
		if [ $isTestMaterialUpdateNeeded -eq 0 ]; then
			echo " JCK$JCK_VERSION $JCK_WITHOUT_BACKSLASH is latest and not in the repo $GIT_URL... Please proceed with download"
		else
			echo " Latest exclude files are available for JCK$JCK_VERSION"
		fi
		getJCKSources
	fi
}

getLastModifiedForFile() {
    file="$1"
    curl -s -H "X-JFrog-Art-Api:$ARTIFACTORY_TOKEN" "$ARTIFACTORY_EXC_URL$file" | awk -F'"' '/lastModified/ {print $4}'
}

getExcludeFiles() {
	github_last_modified=$(curl -s -H "Authorization: token $GIT_TOKEN" "${GIT_EXCLUDE_API_URL}/JCK$JCK_VERSION-unzipped/commits?path=excludes/jck$JCK_FOLDER_SUFFIX.jtx" | awk -F'"date":' '{if(NF>1){gsub(/[",]/,"",$2); print $2; exit}}')
	#check if file is present in github.
	if [[ -n "${github_last_modified}" ]]; then
		github_date_only=$(date -u -d "${github_last_modified}" "+%Y-%m-%d")
	fi
	artifactory_file_list=$(curl -s -H "X-JFrog-Art-Api:$ARTIFACTORY_TOKEN" "$ARTIFACTORY_EXC_URL" -s | \
    awk -F'"uri" : "' '/"uri" : "\// { gsub(/".*$/, "", $2); print $2 }' | \
    awk '{gsub(/^\//, ""); print}' | \
    grep -v '/$' )

	#if no exclude file is present
	if [[ -z "$artifactory_file_list" ]]; then
		echo "No exclude files present in Artifactory to update"
		return 1
	else
		declare -a last_modified_dates
		declare -a file_last_modified

		echo "List of exclude files in Artifactory:"
		echo "$artifactory_file_list"

		#store all files one by one
		files=()
		while IFS= read -r line; do
			files+=("$line")
		done <<< "$artifactory_file_list"

		for file in "${files[@]}"; do
			artifactory_date=$(date -d "$(getLastModifiedForFile "$file")" "+%Y-%-m-%d")
			last_modified_dates+=("$artifactory_date")
			file_last_modified["$artifactory_date"]=$file
		done

		artifactory_latest_modified=$(printf "%s\n" "${last_modified_dates[@]}" | sort -rV | head -1)
		latest_file_name="${file_last_modified[$artifactory_latest_modified]}"
		formatted_date=$(date -d "$artifactory_latest_modified" "+%Y-%m-%d") # reconvert month to value with leading zero for comparison
		echo "Latest exclude file: $latest_file_name"
		echo "Last Modified Date in GitHub: ${github_date_only}"
		echo "Last Modified Date in Artifactory: ${formatted_date}"

		# Compare and find the latest last modified date only if file is present in github. Else update exclude file
		if [[ -n "${github_last_modified}" && ("${github_date_only}" > "${formatted_date}" || "${github_date_only}" == "${formatted_date}" ) ]]; then
			echo "No need to update exclude file in GitHub"
			return 1
		else
			echo "Update exclude file with $latest_file_name from Artifactory"
			return 0
		fi
	fi

}

## Download directly from given URL under current folder
getJCKSources() {
	cd $WORKSPACE/jckmaterial
	echo "remove build.txt file after comparison"
	rm -rf build.txt
	echo "download jck materials..."
	
	#download latest exclude file
	if [ $isExcludeUpdateNeeded -eq 0 ]; then
		curl -OLJSk -H X-JFrog-Art-Api:${ARTIFACTORY_TOKEN} ${ARTIFACTORY_JCK_DOWNLOAD_URL}exc/$latest_file_name && unzip -o "$latest_file_name"
	fi

	if [ $isTestMaterialUpdateNeeded -eq 0 ]; then
		ARTIFACTORY_JCK_UPDATE_NUMBER_URL=${ARTIFACTORY_JCK_DOWNLOAD_URL}tck/$JCK_UPDATE_NUMBER
		echo $ARTIFACTORY_JCK_UPDATE_NUMBER_URL

		list "$ARTIFACTORY_JCK_UPDATE_NUMBER_URL" #get list of files to download

		IFS=$'\n' read -r -d '' -a file_names_array <<< "$file_names"

		if [ "${ARTIFACTORY_JCK_UPDATE_NUMBER_URL}" != "" ]; then
			for file in "${file_names_array[@]:1}"; do
				url="$ARTIFACTORY_JCK_UPDATE_NUMBER_URL$file"
				_ENCODE_FILE_NEW=UNTAGGED curl -OLJSk -H X-JFrog-Art-Api:${ARTIFACTORY_TOKEN} $url
				rt_code=$?
				if [ $rt_code != 0 ]; then
					echo "curl error code: $rt_code"
					echo "Failed to retrieve $file. This is what we received of the file and MD5 sum:"
					ls -ld $file

					exit 1
				fi
			done
		fi
	fi
}

#Unpack downloaded jar files 
extract() {
	cd $WORKSPACE/jckmaterial
	echo "install downloaded resources"
	for f in $WORKSPACE/jckmaterial/*.jar; do
		echo "Unpacking $f:"
		$JAVA_SDK_PATH/bin/java -jar $f -install shell_scripts -o $WORKSPACE/unpackjck
	done
	cd $WORKSPACE/unpackjck
	ls -la
	echo "completed unpack of jck resources"
}

#Clone GIT branch.
gitClone() {
	echo "Checking out git@github.ibm.com:$GIT_USER/$JCK_REPO_NAME and reset to git@github.ibm.com:runtimes/$JCK_REPO_NAME main branch in dir $GIT_REPO"
	cd $GIT_REPO
	git init
	git remote add origin git@github.ibm.com:$GIT_USER/"$JCK_REPO_NAME"
	git remote add upstream git@github.ibm.com:runtimes/"$JCK_REPO_NAME"
	git remote -v
	git checkout -b $JCK_GIT_BRANCH
	git fetch upstream main
	git reset --hard upstream/main
	git branch -v
}

#Move unpacked files to GIT repository
copyFilestoGITRepo() {
	cd $GIT_REPO
	if [ $isTestMaterialUpdateNeeded -eq 0 ]; then
		rm -rf JCK-compiler-$JCK_FOLDER_SUFFIX
		rm -rf JCK-runtime-$JCK_FOLDER_SUFFIX
		rm -rf JCK-devtools-$JCK_FOLDER_SUFFIX
		rm -rf headers
		rm -rf natives
		echo "copy unpacked JCK files from $WORKSPACE/unpackjck to local GIT dir $GIT_REPO"

		cd $WORKSPACE/unpackjck
		for file in $WORKSPACE/unpackjck/*; do
			echo $file
			cp -rf $file $GIT_REPO
		done
	fi

	#copy remaining file like .jtx, .kfl .html to GIT Repo
	for file in $WORKSPACE/jckmaterial/*; do
		if [[ "$file" != *.zip ]] && [[ "$file" != *.gz* ]] && [[ "$file" != *.jar ]];then
			if [[ "$file" == *.kfl* ]]  || [[ "$file" == *.jtx* ]]; then
				echo "Copy $file to $GIT_REPO"
				mkdir -p $GIT_REPO/excludes && cp -rf $file $GIT_REPO/excludes
			else
	 			cp -rf $file $GIT_REPO
			fi
		fi
	done
	find $GIT_REPO -name .DS_Store -exec rm -rf {} \;
}


#Commit changes to local repo and Push changes to GIT branch.
checkChangesAndCommit() {
	cd $GIT_REPO
	# Check if there are any changes in the working directory
	if git diff --quiet  &&  git status --untracked-files=all | grep -q "nothing to commit" ; then
	   	echo "No changes to commit."
	else
		git config --global user.email "$GIT_USER@in.ibm.com"
		git config --global user.name "$GIT_USER"
		git add -A .
		#Commit and push to origin
		echo "Pushing the changes to git branch: $JCK_GIT_BRANCH..."
		git commit -am "Initial commit for JCK$JCK_FOLDER_SUFFIX $JCK_WITHOUT_BACKSLASH update"
		git push origin $JCK_GIT_BRANCH
		echo "Changes committed successfully."
		#Call only if there are any changes to commit.
		createPR
	fi
}


#Create PR from JCK_GIT_BRANCH to main branch. PR will not be merged.
#Call only if there are any changes to commit.
createPR(){
	cd $GIT_REPO

	# Add the base repository as a remote

	## Create a PR from JCK_GIT_BRANCH to main branch.
	## Will not merge PR and wait for review.

	title="JCK$JCK_VERSION $JCK_WITHOUT_BACKSLASH udpate"
	body="This is a new pull request for the JCK$JCK_VERSION $JCK_WITHOUT_BACKSLASH udpate"
	echo " Creating PR from $JCK_GIT_BRANCH to main branch"
	url="https://api.github.ibm.com/repos/$GIT_USER/JCK$JCK_VERSION-unzipped/pulls"

	response=$(curl -X POST \
	-H "Authorization: token $GIT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"title\":\"$title\",\"body\":\"$body\",\"head\":\"$GIT_USER:$JCK_GIT_BRANCH\",\"base\":\"main\"}" \
        "$url")
	
	# Assuming $response contains the JSON response
	PR_NUMBER=$(echo "$response" | grep -o '"number": *[0-9]*' | awk -F':' '{print $2}' | tr -d ' ,"')

	# $PR_NUMBER now contains the PR number
	echo "PR_NUMBER=$PR_NUMBER"
}

cleanup() {
	echo "Starting clean up dir $WORKSPACE and $GIT_REPO ........"
	rm -rf $WORKSPACE
	rm -rf $GIT_REPO
	echo "Clean up successfull"
}

date
echo "Starting autojckupdater script.."
begin_time="$(date -u +%s)"

parseCommandLineArgs "$@"

if [ "$JCK_VERSION" != "" ] && [ "$JCK_GIT_REPO" != "" ] && [ "$GIT_TOKEN" != "" ] && [ "$ARTIFACTORY_TOKEN" != "" ] && [ "$ARTIFACTORY_DOWNLOAD_URL" != "" ]  ; then
	cleanup
	setup
	isLatestUpdate
	if [ $isTestMaterialUpdateNeeded -eq 0 ]; then
		extract
	fi
	gitClone
	copyFilestoGITRepo
	checkChangesAndCommit
	cleanup
else 
	echo "Please provide missing arguments"
	usage; exit 1
fi

end_time="$(date -u +%s)"
date 
elapsed="$(($end_time-$begin_time))"
echo "Complete JCK update process took ~ $elapsed seconds."
echo ""
