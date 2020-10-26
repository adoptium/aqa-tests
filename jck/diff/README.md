# Overview 

The diff.sh script is used when a new JCK repo is created or an existing JCK repo is updated, to check if new test folders have been added. It helps to ensure we are not missing any jck test targets in our playlists.

# How to run the script
diff.sh can be run by providing two jck repositories where one corresponds to an existing JCK repo (for which we know we have all test targets), and the other corresponds to a new / updated JCK repo. 


# What the script does 

Given two JCK repos: 


```
1. Git clone the two JCK given repositories (if they do not already exist in the local  workspace location). 
2. From the two JCK repos, compare the top level folders (e.g. for jck8c, top level folders would be the ones under <JCK8-REPO>/tree/master/JCK-runtime-8c/tests), as well as the tree inside each of the second level folders (e.g. a second level folder would be <JCK8-REPO>/tree/master/JCK-runtime-8c/tests/api). 
3. If the folders are identical, print output in console to indicate that. 
4. If any mismatch is detected, generate logs to indicate the differences for users to check. 

```

Prerequisite: 

```
If any SSH keys are required to access the given JCK repositories, they should already be set up prior to invoking this script. 

```
