### Demo Script

## Running External tests locally

    To run any AQA tests locally,
        Ensure your test machine is set up with test prereqs. For external tests, you do need Docker installed.

## Prerequisites:

# install ant 1.9.6 or above with ant-contrib (1.0b2 or above) following the following steps :

- Visit the `http://ant-contrib.sourceforge.net/ `
- From there visit `https://ant.apache.org/bindownload.cgi` to download any binary listed out there.
- I would suggest installing ant manually. Follow this article `https://girishkr.medium.com/install-apache-ant-1-10-on-ubuntu-16-04-7e249765e1bc` for a proper guide if you are on Ubuntu. Remember that if you are not familiar with vim you might replace the sudo vim /etc/profile.d/ant.sh with sudo nano /etc/profile.d/ant.sh. It gives an easy to understand interface.
- For any other OS you might be using, refer to `https://ant.apache.org/manual/install.html`

  - Assume Ant is installed in c:\ant\. The following sets up the environment:

  set ANT_HOME=c:\ant
  set JAVA_HOME=c:\jdk11.0.8_10
  set PATH=%PATH%;%ANT_HOME%\bin

- To check whether you have installed Ant properly or not, you may run the command `ant`. You should get the following output:
  Buildfile: build.xml does not exist!
  Build failed
- If you don't get this then you may log out and log in into your system again to check if you got it right!

# Installing ant-contrib 1.03b or above

- Visit `https://sourceforge.net/projects/ant-contrib/files/`
- Download version ant-contrib-1.03b or above
- Unzip the folder in your desired location
- To install the ant-contrib file simply copy the ant-contrib-0.x.jar to the lib directory of the Ant installation

# make 4.1 or above

- Installing make 4.1
- Please Install from `ftp://ftp.gnu.org/gnu/make/ `

# perl 5.10.1 or above\*\*

- Installing perl 5.10.1 or above
- Please Install from `https://www.perl.org/get.html`

# curl 7.20.0 or above (needs -J/--remote-header-name support)

- Install curl 7.20.0 or above
- Please install it from `https://help.ubidots.com/en/articles/2165289-learn-how-to-install-run-curl-on-windows-macosx-linux`

# docker (needed if you wish to run docker-based third-party application tests in the /external directory)

- Please Follow the guide `https://docs.docker.com/get-docker/`

**NOTE:** for Windows testing, cygwin is also required.

---

# Done with Prerequesties , please follow the following steps

- Download/unpack the SDK that you want to test to your test machine
  - Please use the following link : `https://adoptopenjdk.net/ `.
- Remember to set the path environment variable for Java.

  - export TEST_JDK_HOME=</pathToWhereYouInstalledSDK>
    # set test JDK home,
    the windows path format is expected. (i.e., TEST_JDK_HOME=C:\locationTest )

- git clone https://github.com/AdoptOpenJDK/openjdk-tests.git to `(/locationTest)`

- cd openjdk-tests

- ./get.sh

- cd TKG

- export environment variables suitable for the SDK under test (i.e., export BUILD_LIST=functional )

- make compile // fetches test material and compiles it, based on build.xml files in the test directories

- make \_< someTestTarget > // executes the test target (can be test group, level, level.group or specific test). i.e., openjdk (all tests in openjdk group), sanity.functional (all functional tests labelled at sanity level), extended.system (all system tests labelled at extended level), jdk_math (the specific jdk_math target defined as part of openjdk group),

- MauveMultiThreadLoadTest_0 (the first variation of the specific system test called MauveMultiThreadLoadTest), etc

# follow this link for more information `https://www.jenkins.io/doc/developer/testing/` .
