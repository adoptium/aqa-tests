---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
---

# How-To Contribute your first test suite to the aqa-tests repository

This is a quick guide of adding a testsuite to Adoptium AQAvit. This is guidance for contributions into the functional test group.  Some of the other test groups might feature different prerequisites.

## Requirements:

An actual OpenJDK compatible testsuite that you want to contribute.

## Actual steps:

1. First you need to decide what kind of testsuite you are adding. Create an [issue](https://github.com/adoptium/aqa-tests/issues) about contribution to aqa-tests.  Here you can discuss with AQAvit committers about where to situate your test suite. Make sure to explain briefly what is the purpose of your test suite and what specific part of an OpenJDK implementation it tests. Also describe special requirements that your test needs to work (containers, sudo access, graphical display, etc..).

2. ***Fork the aqa-tests*** repository to your own personal fork. We will be executing the testsuite from this fork later.

3. Once you determine in what test group your testsuite is going to be added to, there are at least two files you need to prepare.

    1. First one is the ***build.xml*** file that sets up the environment for your test, clones the repository of your test material if from a separate repository, and builds the test material. A good approach is to look for build.xml files within other testsuite folders and get inspired there, a simple example is [functional/testHeadlessComponents/build.xml](https://github.com/adoptium/aqa-tests/blob/master/functional/testHeadlessComponents/build.xml).
   
    2. The second file is the ***playlist.xml***. This one defines the execution of the testsuite itself as well as what platforms it will be scheduled for. Like with the build.xml files it is advisable to consult other playlists in already existing testsuites.  Another useful resource is the [playlist.xsd](https://github.com/adoptium/TKG/blob/master/resources/playlist.xsd) file which can help guide what elements are required in a playlist.xm file.

       There is a `<command>` element inside each `<test>` target definition in the playlist.xml file to indicate what commands are to be run to execute the testsuite.  One thing that is not immediately obvious but is ***mandatory*** is that the command needs to be followed by a ***$(TEST_STATUS)*** statement for the correct reporting of the test result and associated test status and result color to be made.

4. Once you think you have both the build.xml and playlist.xml you can ***test your integration*** with [Grinder JOB](https://ci.adoptium.net/view/Test_grinder/job/Grinder/).
   ![BuildWithParameters](../../diagrams/BuildWithParameters.png)
   
Click the ***"Build with Parameters" button*** in the left panel (if there is no such option, make sure you are logged in, and have appropriate permissions set for your account).
Here you can configure your run. Main two parameters that are of interest to us are ADOPTOPENJDK_REPO, where you can put URL of your forked aqa-tests repository and ADOPTOPENJDK_BRANCH that specified the branch your integration is on.
The documentation in the Grinder configurator is pretty descriptive, so let's just mention a few other key paremeters you might find useful and their usage.

| Parameter | Purpose |
| --- | --- |
| BUILD_LIST | is the relative path to the folder containing your build.xml and playlist.xml files |
| TARGET | refers to the name you specified in "testCaseName" section in your playlist.xml file |
| KEEP_WORKSPACE | after testrun the workspace is saved for a while for the developer to check it |
| ARCHIVE_TEST_RESULTS | after testrun the test results are by default discarded for passing tests, check for keeping them |

It is advised to check KEEP_WORKSPACE and ARCHIVE_TEST_RESULTS checkboxes for debuggin purposes. Adoptium infrastructure by default does not keep workspaces at all and results in case the tests were         successful. In some cases runs can report as successful when it is not even executed, therefore nothing had a chance of failing there. It is a good practice to not only check that the test finishes         "green" but also that the tests that were supposed to run actually did.

5. Final step is creating a ***pull request*** for your hard work into main branch.. don't forget to ***link the original issue*** and a ***passing grinder run*** as well.

   ***Happy Hacking! :-)***
