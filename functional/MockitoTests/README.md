# Mockito Tests
There are two OpenJ9 user issues where a crash with Mockito are reported:

    - Issue: https://github.com/eclipse-openj9/openj9/issues/18750 and 

    - Issue: https://github.com/eclipse-openj9/openj9/issues/18781

    - Reported crashed with Java 21 0.42, and set testing jdk version to 17+ as required for now

Added MockitoMockTest test case for Java 11+ in functional MockitoTests folder.


# This MockitoMockTest test cases will help identify the above crashes in our builds.
Test case file added: test.java.MockitoMockTest which will working on JDK11+, it'a not avaiable (disabled) for JDK23+, nor zos platform.
    - https://github.com/eclipse-openj9/openj9/issues/19331
    - https://github.com/eclipse-openj9/openj9/issues/19354
    For JDK8, it is having mockito compile problems with a different jdk version match from the Mockito side, refering to this issue.
    - https://github.com/adoptium/infrastructure/issues/3043

# Dependencies Required:
    - mockito-core
    - byte-buddy
    - byte-buddy-agent
    - objenesis
