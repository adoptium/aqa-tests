# AdoptOpenJDK System Tests

System tests help verify that the AdoptOpenJDK binaries are *good* by running a variety of load tests. 

## Related repositories: 
- System tests that run both on AdoptOpenJDK and AdoptOpenJDK-OpenJ9 SDKs are located at:  github.com/AdoptOpenJDK/openjdk-systemtest.git
- System tests specific to AdoptOpenJDK-OpenJ9 are located at: github.com/eclipse-openj9/openj9-systemtest.git
- STF framework that runs system tests are located at: github.com/AdoptOpenJDK/stf.git

## How it works
The [build.xml](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/system/build.xml) file defines steps to pull test materials from the above 3 repositories, and compile them. The [playlist.xml](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/system/playlist.xml) file defines a set of test we want to run. 

For detailed documentation, please see 
- openjdk-systemtest [build instruction](https://github.com/AdoptOpenJDK/openjdk-systemtest/blob/master/openjdk.build/docs/build.md) 
- openj9-systemtest [build instruction](https://github.com/eclipse-openj9/openj9-systemtest/blob/master/openj9.build/docs/build.md)

Please direct questions to the [#testing Slack channel](https://adoptopenjdk.slack.com/messages/C5219G28G).
