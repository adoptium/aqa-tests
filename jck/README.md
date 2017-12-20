
# How-to Run JCK Tests

* Prerequisites:
  * JCK test materials (JCK test source): jck8b or jck9
  * ant 1.10.1 or above with ant-contrib.jar



1. Put unarchived jck test materials (jck8b or jck9) into an empty folder, for example:
* `/jck/jck8b/` and `/jck/jck9`

2. Export `JCK_ROOT=/jck` as an environment variable or pass it in makefile when run make command

3. Export `JAVA_HOME=<your_JDK_root>` as an environment variable

4. The other steps will stay the same as instructed in `openj9/test/README.md`


This test directory contains:
  * build.xml file - that clones AdoptOpenJDK/stf repo to pick up a test framework
  * playlist.xml - to allow easy inclusion of JCK tests into automated builds
