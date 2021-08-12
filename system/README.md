# Adoptium System Tests

System tests help verify that the adoptium binaries are *good* by running a variety of load tests. 

## Related repositories: 
- System tests that run both on adoptium and OpenJ9 SDKs are located at:  github.com/adoptium/aqa-systemtest.git
- System tests specific to OpenJ9 are located at: github.com/eclipse-openj9/openj9-systemtest.git
- STF framework that runs system tests are located at: github.com/adoptium/stf.git

## How it works
The [build.xml](https://github.com/adoptium/aqa-tests/blob/master/system/build.xml) file defines steps to pull test materials from the above 3 repositories, and compile them. The [playlist.xml](https://github.com/adoptium/aqa-tests/blob/master/system/playlist.xml) file defines a set of test we want to run. 

For detailed documentation, please see 
- aqa-systemtest [build instruction](https://github.com/adoptium/aqa-systemtest/blob/master/openjdk.build/docs/build.md) 
- openj9-systemtest [build instruction](https://github.com/eclipse-openj9/openj9-systemtest/blob/master/openj9.build/docs/build.md)

Please direct questions to the [#testing Slack channel](https://adoptium.slack.com/archives/C5219G28G).
