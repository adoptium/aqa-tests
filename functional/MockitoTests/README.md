# Mockito Tests
There are two OpenJ9 user issues where a crash with Mockito is reported:

    - Issue: https://github.com/eclipse-openj9/openj9/issues/18750 and 

    - Issue: https://github.com/eclipse-openj9/openj9/issues/18781

    - Crash with Java 21 0.42, and set testing jdk version to 17+ as required for now

    - Segmentation error when using Mockito with Java 21

    - More Testcases would be added later as required

# This Mockito test cases will help identify the above crashes in our builds.
Test case 1: test.java.MainTest.java
Test case 2: test.java.SegmentationErrorDemo 

# Dependencies Required:
    - mockito-core
    - junit-platform-console-standalone
    - junit-jupiter-api
