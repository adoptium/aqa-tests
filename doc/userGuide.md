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
![pipeline from SCM](/doc/diagrams/pipelineFromSCM.jpg)

2. Create necessary parameters

* TARGET - relates to the test target you wish to run (system, openjdk, perf, external, jck, functional are the top-level targets, but you can also add any of the sub-targets, including those defined in playlist.xml files in test directories)
* JVM_VERSION - depending on what SDK you are testing against (some possible values are: openjdk8, openjdk8-openj9, openjdk9, openjdk9-openj9, openjdk10, openjdk10-openj9, openjdk10-sap)
* CUSTOMIZED_SDK_URL - the URL for where to pick up the SDK to test (if you are picking up builds from AdoptOpenJDK, please refer to the [openjdk-api README](https://github.com/AdoptOpenJDK/openjdk-api/blob/master/README.md) for more details) 

![jenkins parameters](/doc/diagrams/jenkinsParameters.jpg)

## How to pass in environment variables

This is the guide on how to pass in environment variables when making builds with Jenkins. For the directions on this, we will be using the example variable below: TR_Options='verbose,vlog=testExample1.log' There are three ways to do this, and there will be screenshot demonstrations for each step. Keep in mind that this operation is not typically necessary, and is usually used for debugging purposes.

#### Method 1: Write it in as part of the playlist file

This method is typically used if that environment variable is to be used in that specific test target. 

1.	Find the folder that your test is in

 ![test_folder](/doc/diagrams/testFolder.jpg)
 
2.	Open the playlist.xml file

 ![playlist_file](/doc/diagrams/playListFile.jpg)
 
3.	Find the testCaseName matching with the test you want to run

 ![test_case_name](/doc/diagrams/testCaseName.jpg)
 
4.	In the corresponding command section, at the beginning, add the key word `export`, your environment variable, followed by a semicolon, just as you might do if you were running this set of commands locally

 ![export](/doc/diagrams/commandSection.jpg)
 
5.	Save it, git add, commit, push

 ``` bash
git add --all
git commit -m "Added TR_Options as an environment variable in the playlist"
git push origin env_var
```
 
6.	Go to the Jenkins page, and open up the Grinders

 ![open_grinders](/doc/diagrams/openGrinders.jpg)
 
7.	Click “Build with Parameters” on the left side of the page, third down from the top

8.	In the ADOPTOPENJDK_REPO section, put in the repository you were working from when you made those changes

 ![repo](/doc/diagrams/repo.jpg)
 
9.	In the ADOPTOPENJDK_BRANCH section, put in the branch you were on

 ![branch](/doc/diagrams/branch.jpg)
 
10.	In the BUILD_LIST and TARGET sections, put in the corresponding information

 ![build_list_target](/doc/diagrams/buildListTarget.jpg)
 
11.	Scroll to the bottom and hit the Build button

 ![build](/doc/diagrams/build.jpg)


#### Method 2: Put it in the .mk file of the test that you want to run 

This method is to be used when the objective is to set that environment variable for all test targets in the group being run. For this example, we will be looking at the systemtest.mk file. 

1.	Open the openjdk-tests/system folder

 ![system_folder](/doc/diagrams/systemFolder.jpg)
 
2.	Open the .mk file corresponding to your test

 ![system_test](/doc/diagrams/systemtest.jpg)
 
3.	Find the last line of the file with the RESROOT name, the line that says SYSTEMTEST_RESROOT=$(TEST_RESROOT)/../ in this example 

 ![resroot_line](/doc/diagrams/resrootLine.jpg)
 
4.	Insert the key word `export`, followed by your environment variable, without any single or double quotation marks, in the line above it

 ![export](/doc/diagrams/export.jpg)
 
5.	Save it, git add, commit, push

  ``` bash
git add --all
git commit -m "Added TR_Options as an environment variable in the playlist"
git push origin env_var
```
 
6.	Go to the Jenkins page, and open up the Grinders

 ![open_grinders](/doc/diagrams/openGrinders.jpg)
 
7.	Click “Build with Parameters” on the left side of the page, third down from the top

8.	In the ADOPTOPENJDK_REPO section, put in the repository you were working from when you made those changes

 ![repo](/doc/diagrams/repo.jpg)
 
9.	In the ADOPTOPENJDK_BRANCH section, put in the branch you were on

 ![branch](/doc/diagrams/branch.jpg)
 
10.	In the BUILD_LIST and TARGET sections, put in the corresponding information

 ![build_list_target](/doc/diagrams/buildListTarget.jpg)
 
11.	Scroll to the bottom and hit the Build button

 ![build](/doc/diagrams/build.jpg)
 
 
#### Method 3: Put it in the testEnv.mk file 

This method is to be used when the objective is to set that environment variable for a more generic case.

1.	Fork https://github.com/AdoptOpenJDK/TKG  

 ![test_config](/doc/diagrams/testConfig.jpg)
 
2.	Edit the [testEnv.mk](https://github.com/AdoptOpenJDK/TKG/blob/master/testEnv.mk) file
 
 ![test_env](/doc/diagrams/testEnv.jpg)
 
3.	Insert the key word export, followed by your environment variable, without any single or double quotation marks, or spaces
 
 ![export](/doc/diagrams/otherExport.jpg)
 
5.	Save it, git add, commit, push
```
git add --all
git commit -m "Added TR_Options as an environment variable in testEnv"
git push origin env_var
```
6.	Go to the Jenkins page, and open up the Grinder_TKG job

 ![open_grinders](/doc/diagrams/openGrinders.jpg)
 
7.	Click “Build with Parameters” on the left side of the page, third down from the top
 
8.	Use your TKG_REPO and TKG_BRANCH where you have made your changes for those parameters instead of the default values

9. 	Scroll to the bottom and hit the Build button

 ![open_grinders](/doc/diagrams/build.jpg)

## Local testing via make targets on the commandline

#### Clone the repo and pick up the dependencies
``` bash
git clone https://github.com/AdoptOpenJDK/openjdk-tests.git
cd openjdk-tests
get.sh -t openjdk-testsDIR -p platform [-j SE80] [-i hotspot] [-R latest] [-T jdk] [-s downloadBinarySDKDIR] [-r SDK_RESOURCE] [-c CUSTOMIZED_SDK_URL]
```

Where possible values of get.sh script are:
```
Usage : get.sh  --testdir|-t openjdktestdir
                --platform|-p x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix

                [--jdk_version|-j ] : optional. JDK version

                [--jdk_impl|-i ] : optional. JDK implementation

                [--releases|-R ] : optional. Example: latest, jdk8u172-b00-201807161800

                [--type|-T ] : optional. jdk or jre

                [--sdkdir|-s binarySDKDIR] : optional.  Used if do not have a local sdk available and wish to download one to specify preferred directory to download into

                [--sdk_resource|-r ] : optional. Indicate where to download an sdk from - releases, nightly, upstream or customized

                [--customizedURL|-c ] : optional. If downloading an sdk and if sdk source is set as customized, indicates sdk url 
                [--clone_openj9 ] : optional. ture or false. Clone openj9 if this flag is set to true. Default to true
                [--openj9_repo ] : optional. OpenJ9 git repo. Default value https://github.com/eclipse/openj9.git is used if not provided
                [--openj9_sha ] : optional. OpenJ9 pull request sha
                [--openj9_branch ] : optional. OpenJ9 branch
                [--tkg_repo ] : optional. TKG git repo. Default value https://github.com/AdoptOpenJDK/TKG.git is used if not provided
                [--tkg_sha ] : optional. TkG pull request sha
                [--tkg_branch ] : optional. TKG branch
                [--vendor_repos ] : optional. Comma separated Git repository URLs of the vendor repositories
                [--vendor_shas ] : optional. Comma separated SHAs of the vendor repositories
                [--vendor_branches ] : optional. Comma separated vendor branches
                [--vendor_dirs ] : optional. Comma separated directories storing vendor test resources
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

### Examples

#### Sanity check an upstream JDK 8u patch

Consider you are an upstream OpenJDK developer on Linux x86_64, and you'd like to run OpenJDK sanity tests locally on a patch for OpenJDK 8. Let the OpenJDK checkout be at `openjdk-jdk8u`, you've produced
a *fastdebug* build and would like to run the regression test suite, `_sanity.openjdk` on it.

```
$ OPENJDK_SOURCES="$(pwd)/openjdk-jdk8u"
$ OPENJDK_BUILD=$OPENJDK_SOURCES/build/linux-x86_64-normal-server-fastdebug/images/j2sdk-image
$ tmpdir=$(mktemp -d)
$ pushd $tmpdir
$ git clone https://github.com/AdoptOpenJDK/openjdk-tests
$ cd openjdk-tests
$ TOP_DIR=$(pwd)
$ TEST_DIR="$TOP_DIR"
$ pushd openjdk
$ ln -s $OPENJDK_SOURCES openjdk-jdk
$ popd
$ export BUILD_LIST=openjdk
$ export BUILD_ROOT=$TOP_DIR/test-results
$ export JRE_IMAGE=$OPENJDK_BUILD/../j2re-image
$ export TEST_JDK_HOME=$OPENJDK_BUILD
$ ./get.sh -t $TEST_DIR
$ ./maketest.sh $TEST_DIR
$ OPENJDK_DIR=$OPENJDK_SOURCES ./maketest.sh $TEST_DIR _sanity.openjdk
$ popd
$ echo "openjdk-tests located at $tmpdir/openjdk-tests"

```

If all goes well, this should run sanity JTREG OpenJDK tests on your hotspot JDK 8 build and all of them should be passing. Output will then look like this:

```
[...]

TEST TARGETS SUMMARY
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
PASSED test targets:
	jdk_io_0
	jdk_lang_0
	jdk_math_0
	jdk_math_jre_0
	jdk_net_0
	jdk_nio_0
	jdk_security1_0
	jdk_util_0
	jdk_rmi_0

TOTAL: 9   EXECUTED: 9   PASSED: 9   FAILED: 0   SKIPPED: 0
ALL TESTS PASSED
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

_sanity.openjdk done

[...]
```

Additional test output can be found in the following folders:

```
openjdk-tests/test-results/openjdk/TKG/test_output_<timestamp>`
openjdk-tests/test-results/openjdk/work
openjdk-tests/test-results/openjdk/report
```

The JTREG report HTML summary file is then located at `openjdk-tests/test-results/openjdk/report/html/index.html`

## Exclude a test target

#### Automatically exclude a test target
Instead of having to manually create a PR to disable test targets, they can now be automatically disabled via Github workflow (see autoTestPR.yml). In the issue that describes the test failure, add a comment with the following format:

```auto exclude test <testName>```

If the testName matches the testCaseName defined in ```<testCaseName>``` element of playlist.xml, the entire test suite will be excluded. If the testName is testCaseName followed by _n, only the (n+1)th variation will be excluded. 

For example:

```
<test>
  <testCaseName>jdk_test</testCaseName> 
    <variations>
      <variation>NoOptions</variation>
      <variation>-Xmx1024m</variation>
    <variations>
    ...
```
To exclude the entire suite:

```auto exclude test jdk_test```

To exclude the test case with variation ```-Xmx1024m```:

```auto exclude test jdk_test_1```

After the comment is left, there will be a auto PR created with the exclude change in the playlist.xml. The PR will be linked to issue. If the testName can not be found in the repo, no PR will be created and there will be a comment left in the issue linking to the failed workflow run for more details.

#### Manually exclude a test target
Search the test name to find its playlist.xml file. Add a ```<disabled>``` element after ```<testCaseName>``` element. The ```<disabled>``` element should always contain a ```<comment>``` element to specify the related issue url (or issue comment url).

For example:

```
<test>
  <testCaseName>jdk_test</testCaseName> 
    <disabled>
      <comment>https://github.com/AdoptOpenJDK/openjdk-tests/issues/123456</comment>
    </disabled>
    ...
```

This will disable the entire test suite. The following section describes how to disable the specific test cases.

##### Exclude a specific test variation:
Add a ```<variation>``` element in the ```<disabled>``` element to specify the variation. The ```<variation>``` element must match an element defined in the ```<variations>``` element.

For example, to exclude the test case with variation ```-Xmx1024m```:

```
<test>
  <testCaseName>jdk_test</testCaseName> 
    <disabled>
      <comment>https://github.com/AdoptOpenJDK/openjdk-tests/issues/123456</comment>
      <variation>-Xmx1024m</variation>
    </disabled>
    ...
    <variations>
      <variation>NoOptions</variation>
      <variation>-Xmx1024m</variation>
    <variations>
    ...
```

##### Exclude multiple test variations:
Define multiple ```<disabled>``` elements, each with a single ```<variation>``` element inside. Multiple ```<variation>``` elements inside one ```<disabled>``` element is not allowed.