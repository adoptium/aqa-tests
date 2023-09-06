# Overview 

The jckupdater.sh script automates the process of obtaining JCK materials from https://eu.artifactory.swg-devops.com/artifactory/jim-jck-generic-local site and pushing them in the IBM internal repository.

# How to run the script

The following instructions may be used to run jckupdater.sh to make a JCK update

- Create a fork of the internal JCK repository (if it doesn't already exist) to create the pull request. 	
- Run the script with following arguments: 

```
JCK_VERSION - The JCK version for which the update is being performed (e.g. 8, 11, 12 etc)
JCK_GIT_REPO - The JCK GIT repo to update.(e.g. git@github.ibm.com:runtimes/JCK8-unzipped.git)
ARTIFACTORY_TOKEN - Artifactory token to download JCK resources from https://eu.artifactory.swg-devops.com/artifactory/jim-jck-generic-local
ARTIFACTORY_DOWNLOAD_URL - Artifactory server URL to download JCK resources. Default value is https://eu.artifactory.swg-devops.com/artifactory/jim-jck-generic-local
GIT_TOKEN - GIT user's API Token to create PR.
GIT_DEV_BRANCH - Branch to Pull and merge resources. Default value autoBranch.
JAVA_HOME - Path to JDK on system. Optional.

```

# What the script automates: 

```
1) Checks if latest update is available in artifcatory. If not available then exits without proceeding.
2) If available then downloads the JCK material Jars on local disc.
3) Installs JCK material Jars on local disc.
4) Initializes Git at the local machine, and configures the remotes and origins.
5) Checks out existing JCK materials from the internal Git repository.
6) If changes are present then Commit to local repo and push it to Git branch.
7) Creates PR for review from origin to Base repo ie. runtime.
```


# Functions 
The script contains the following function calls: 

```
	setup
	isLatestUpdate
	getJCKSources
	extract
	gitClone
	copyFilestoGITRepo
	checkChangesAndCommit
	createPR
	cleanup
```


