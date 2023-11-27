#!/bin/bash
# Now you can use these variables in your script
JCK_ROOT_USED="$1"
JCK_GIT_REPO_USED="$2"
jck_branch="$3"
isZOS="$4"

echo "JCK_ROOT_USED is: $JCK_ROOT_USED"
echo "JCK_GIT_REPO_USED is: $JCK_GIT_REPO_USED"
echo "jck_branch is: $jck_branch"
echo "isZOS is: $isZOS"

global_error=0

# Set up an error handler function
handle_error() {
	echo "Removing test material and git clone again"
	git_clone
}

set -o errtrace
trap 'handle_error' ERR

delete_native_dirs() {
	natives_directory="${JCK_ROOT_USED}/natives"
	# Check if the directory exists
	if [ -d "$natives_directory" ]; then
		echo "Deleting $natives_directory..."
		rm -r "$natives_directory"
		if [ $? -eq 0 ]; then
			echo "Directory deleted successfully."
		fi
	fi
}

# Function to perform git clone
git_clone() {
	rm -rf "$JCK_ROOT_USED"
	# Set the working directory before cloning
	cd "$(dirname "$JCK_ROOT_USED")" || exit 1
	git clone --single-branch -b "${jck_branch}" "${JCK_GIT_REPO_USED}" "${JCK_ROOT_USED}"
	# Check if the clone was successful
	if [ $? -eq 0 ]; then
		echo "Repository cloned successfully into $JCK_ROOT_USED"
		exit 0
	else
		echo "Failed to clone repository"
		exit 1
	fi
}

# Function to perform hard reset and pull
perform_hard_reset() {
	echo "Performing hard reset..."
	# Fetch changes from the remote repository
	git fetch "$JCK_GIT_REPO_USED"
	# Reset the local branch to match the remote branch
	git -C $JCK_ROOT_USED reset --hard "FETCH_HEAD"
	# Clean untracked files and directories
	git -C $JCK_ROOT_USED clean -fd
	# Check if isZOS property is set
	if [ -n "$isZOS" ] && [ "$isZOS" = "true" ]; then
		# Check if local-material-uptodate property is set
		if [ -n "$local_material_uptodate" ]; then
			echo "zOS Local JCK materials up-to-date. Skipping hard reset"
		else
			# Perform hard reset for z/OS
			echo "Performing hard reset of ${JCK_ROOT_USED} using .gitattributes.zos for file conversions..."
			rm -rf "${JCK_ROOT_USED}/.git/info"
			mkdir -p "${JCK_ROOT_USED}/.git/info"
			mv "${JCK_ROOT_USED}/.gitattributes.zos" "${JCK_ROOT_USED}/.git/info/attributes"
			git rm --cached -r -q .
			git reset --hard
		fi
	fi
}

# Function to check for updates
check_and_handle_updates() {
	cd $JCK_ROOT_USED
	# Print a message indicating the change of the working directory
	local localSHA=$(git rev-parse HEAD)
	localSHATrimmed=$(echo "${localSHA}" | tr -d '[:space:]')
	local remoteSHA=$(git ls-remote "$JCK_GIT_REPO_USED" "$jck_branch" | cut -f1)
	echo "Current Working Directory: $(pwd)"
	echo "LocalSHA = --$localSHA--"
	echo "RemoteSHA = --$remoteSHA--"
	# Check if SHA match
	if [ "$localSHA" != "$remoteSHA" ]; then
		# Update materials
		delete_native_dirs
		perform_hard_reset
	else
		# Set the property indicating that local materials are up-to-date
		echo "Local JCK materials up-to-date. Skipping update"
		# Discard unstaged files
		git restore .
		# Clean untracked files and directories
		git -C $JCK_ROOT_USED clean -fd
		# Discard staged changes
		git checkout -- .
		# Pull changes from the remote repository
		git pull $JCK_GIT_REPO_USED $jck_branch
		local_material_uptodate="true"
	fi
	# If SHA match or update succeeded
   echo "Local JCK materials updated"
}

# Try block
if [ -d "$JCK_ROOT_USED" ] && { [ -d "$JCK_ROOT_USED/.git" ] || git -C "$JCK_ROOT_USED" rev-parse --is-inside-work-tree &>/dev/null; }; then
	echo "The directory is a valid Git repository."
	# Call the function and capture its exit status
	check_and_handle_updates
	check_status=$?
	# Print the exit status for debugging
	echo "Exit Status: $check_status"
	if [ $check_status -ne 0 ]; then
		# Set the global error variable to 1
		global_error=1
	fi
else
	# JCK materials don't exist, git clone
	echo "JCK materials don't exist."
	git_clone
fi
# Catch block
if [ "$global_error" -eq 1 ]; then
	handle_error
fi
