# Overview 
The splitter.sh script can be used to split long running tck targets into smaller groups of sub-tests. These groups can then be used to compose playlist targets. 

# How to run the script
splitter.sh accepts two parameters - (1) Path to the target to split, (2) Number of groups in which to break down the sub-tests. For example, the following command will split the lang/CLSS tests into 10 groups: 

```
 $ splitter.sh <LOCAL_TCK_ROOT>/JCK-compiler-11/tests/lang/CLSS 10

```


# Prerequisite 
TCK materials should already be checked out locally so that a valid path to a test (to split) can be supplied to the script. 

