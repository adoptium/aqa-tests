# Mockito Tests
There are two OpenJ9 user issues where a crash with Mockito is reported:

    - Crash with Java 21 0.42 

    - Segmentation error when using Mockito and Byte-buddy-agent with Java 21

# This Mockito test cases will help identify the above crashes in our builds.
Test case 1: main.java.test.MainTest.java
Test case 2: main.java.test.SegmentationErrorDemo 
Dependencies:
    - Mockito
    - Bytebuddy
    - Maven
