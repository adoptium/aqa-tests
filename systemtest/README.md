# AdoptOpenJDK System Tests

System tests help verify that the AdoptOpenJDK binaries are *good* by running a variety of load tests. 

## Related repositories: 
- System tests that run both on AdoptOpenJDK and AdoptOpenJDK-OpenJ9 SDKs are located at:  github.com/AdoptOpenJDK/openjdk-systemtest.git
- System tests specific to AdoptOpenJDK-OpenJ9 are located at: github.com/eclipse/openj9-systemtest.git
- STF framework that runs system tests are located at: github.com/AdoptOpenJDK/stf.git

## How it works
The [build.xml](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/systemtest/build.xml) file defines steps to pull test materials from the above 3 repositories, and compile them. The [playlist.xml](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/systemtest/playlist.xml) file defines a set of test we want to run. 

For detailed documentation, please see 
- openjdk-systemtest [build instruction](https://github.com/AdoptOpenJDK/openjdk-systemtest/blob/master/openjdk.build/docs/build.md) 
- openj9-systemtest [build instruction](https://github.com/eclipse/openj9-systemtest/blob/master/openj9.build/docs/build.md)

##How to manually run system test using TestConfig 
(*Note:* You need Ant version 1.84 or later installed on your local machine. For full prereq details, see [build instruction](https://github.com/AdoptOpenJDK/openjdk-systemtest/blob/master/openjdk.build/docs/build.md))

- Export necessary environment variables: 
	
		export SPEC=[linux_x86-64|linux_x86-64_cmprssptrs|...] (platform on which to test)
		export JAVA_HOME=<path to the JDK directory you wish to test>
		export JAVA_BIN=<path to JDK bin directory that you wish to test>
		export BUILD_LIST=systemtest
		export JDK_VERSION=[8|9|10|11|..]
		export JDK_IMPL=[hotspot|openj9|..]
- git clone https://github.com/AdoptOpenJDK/openjdk-tests.git
-  cd openjdk-tests
- ./get.sh -t ../openjdk-tests -p x64_linux -i [openj9|hotspot]  
- cd TestConfig
- make -f run_configure.mk 
- make compile (This gets all the system test prereqs and builds system test)
- make _test (*Note*: test = "testCaseName" of any test you want to run, as specified in [systemtest playlist](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/systemtest/playlist.xml))
 

Please direct questions to the [#testing Slack channel](https://adoptopenjdk.slack.com/messages/C5219G28G).