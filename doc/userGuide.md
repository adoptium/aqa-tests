# Running AdoptOpenJDK Tests

If you have immediate test-related questions, please post them to the [AdoptOpenJDK testing Slack channel](https://adoptopenjdk.slack.com/messages/C5219G28G).

Platform: x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux

Java Version: SE80 | SE90 | SE100

Set up your test machine with this [set of prerequisites](https://github.com/eclipse/openj9/blob/master/test/docs/Prerequisites.md).

## Jenkins setup and running
While you can [run all the tests manually](#local-testing-via-make-targets-on-the-commandline) via the make targets on the command line, you may also run the tests in Jenkins. As part of the AdoptOpenJDK continuous integration (CI), AdoptOpenJDK runs test builds against the release and nightly SDK builds.

You can set up your own Jenkins-based test builds using the AdoptOpenJDK openjdk-tests Jenkinsfiles by:
	
- Configure a [Jenkins job with a Customized URL](#jenkins-configuration-with-customized-url)
- Ensure your Jenkins machines are configured properly (see the [openjdk-infrastructure playbooks](https://github.com/AdoptOpenJDK/openjdk-infrastructure/blob/master/ansible/README.md) for details)
- Ensure machines are labeled following the [AdoptOpenJDK labeling scheme](https://github.com/smlambert/openjdk-infrastructure/blob/labels/docs/jenkinslabels.md).  Minimally, your Jenkins nodes should have hw.arch.xxxx and sw.os.xxxx labels (for example, hw.arch.x86 and sw.os.linux for an x86_linux machine).

### Jenkins Configuration with Customized URL

1. Create Pipeline test build job using Pipeline script from SCM  
- Repository url - :https://github.com/AdoptOpenJDK/openjdk-tests.git
- Branches to build - */master
- Script path - buildenv/jenkins/fileToMatchVersionAndPlatformToTest, example openjdk8_x86-64_linux
![pipeline from SCM](/doc/pipelineFromSCM.jpg)

2. Create necessary parameters

* TARGET - relates to the test target you wish to run (system, openjdk, perf, external, jck, functional are the top-level targets, but you can also add any of the sub-targets, including those defined in playlist.xml files in test directories)
* JVM_VERSION - depending on what SDK you are testing against (some possible values are: openjdk8, openjdk8-openj9, openjdk9, openjdk9-openj9, openjdk10, openjdk10-openj9, openjdk10-sap)
* CUSTOMIZED_SDK_URL - the URL for where to pick up the SDK to test (if you are picking up builds from AdoptOpenJDK, please refer to the [openjdk-api README](https://github.com/AdoptOpenJDK/openjdk-api/blob/master/README.md) for more details) 

![jenkins parameters](/doc/jenkinsParameters.jpg)

## Local testing via make targets on the commandline

#### Clone the repo and pick up the dependencies
``` bash
git clone https://github.com/AdoptOpenJDK/openjdk-tests.git
cd openjdk-tests
get.sh -t openjdk-testsDIR -p platform -v jvmversion [-s downloadBinarySDKDIR] [-r SDK_RESOURCE] [-c CUSTOMIZED_SDK_URL]
```

Where possible values of get.sh script are:
```
Usage : get.sh  --testdir|-t openjdktestdir
                --platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix

                --jvmversion|-v openjdk8 | openjdk8-openj9 | openjdk9 | openjdk9-openj9 | openjdk10 | openjdk10-sap

                [--sdkdir|-s binarySDKDIR] : if do not have a local sdk available, specify preferred directory

                [--sdk_resource|-r ] : indicate where to get sdk - releases, nightly , upstream or customized

                [--customizedURL|-c ] : indicate sdk url if sdk source is set as customized
```

#### Set environment variables, configure, build and run tests

You can use the same approach as described in the [OpenJ9 functional tests README file]( https://github.com/eclipse/openj9/blob/master/test/README.md).  In the case of the tests run at AdoptOpenJDK, instead of using a make target called _sanity.functional, you can provide the appropriate make target to run the tests of interest to you. 

##### Top-level test targets:
- openjdk 
- system
- external
- perf
- jck

##### Sub-targets by level:
- _sanity.openjdk, _sanity.system, _sanity.external, _sanity.perf, etc.
- _extended.openjdk, _extended.system, _extended.external, _extended.perf, etc.

##### Sub-targets by directory:
Refer to these instructions for how to [run tests by directory](https://github.com/eclipse/openj9/blob/master/test/README.md#5-how-to-execute-a-directory-of-tests)

##### Sub-targets by test name:
In each playlist.xml file in each test directory, there are tests defined.  Test targets are generated from the ```<testCaseName>``` tag, so you can use the test case name as a make target.

For example, for this excerpt from a playlist:
```
<test>
		<testCaseName>scala_test</testCaseName> 
		...
```
you will be able to run 'make scala_test' to execute the test.
