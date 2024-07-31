---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
---

# How-To Contribute your first test suite to the aqa-tests repository

This is a quick guide of adding a testsuite to adoptium aqa-vit. The author of this quick guide has so far only made a contribution into functional tests and some of the other test groups might feature different prerequisites.

## Requirements:

An actual java/temurin compatible testsuite that you want to contribute.

## Actual steps:

1. First you need to decide what kind of testsuite you are adding. Create an [issue](https://github.com/adoptium/aqa-tests/issues) about contribution to aqa-tests.. here you can discuss with responsible engineers on where to situate your testsuite. Make sure to explain briefly what is the purpose of your testsuite and what specific part of java implementation it tests. Also describe special requirements that your test needs to work (containers, sudo access, graphical display etc..).

2. ***Fork the aqa-tests*** repository to your own personal fork. We will be executing the testsuite from this fork later.

3. Once you determine in what testgroup your testsuite is gonna end up in, there are now at least two files you need to prepare.

    1. First one is the ***build.xml*** file that sets up the environment for your test. It also clones the repository of your testsuite to the testing folder. A good approach is to look for build.xml files within other testsuite folders and get inspired there. A good simple example of such build.xml is within functional/testHeadlessComponents.
   
    2. The second file we need to discuss is the ***playlist.xml***. This one manages the execution of the testsuite itself as well as what platforms it will be scheduled for. Like with the build.xml files it is advisable to consult other playlists in already existing testsuites.

       The one thing that is not immediately obvious but is ***mandatory*** after every execution is the ***usage of $(TEST_STATUS)*** statement after test execution in the ***playlist.xml*** . This is where the decision on how the test is colored is made.

5. Once you think you have both the build.xml and playlist.xml you can ***test your integration*** with [Grinder JOB](https://ci.adoptium.net/view/Test_grinder/job/Grinder/). 
Click the ***"Build with Parameters" button*** in the left panel (if there is no such option, make sure you are logged in, and have appropriate permissions set for your account).
Here you can configure your run. Main two parameters that are of interest to us are ADOPTOPENJDK_REPO, where you can put URL of your forked aqa-tests repository and ADOPTOPENJDK_BRANCH that specified the branch your integration is on.
The documentation in the grinder configurator is pretty descriptive, so lets just mention a few other key paremeters you might find useful and their usage.

| Parameter | Purpose |
| --- | --- |
| BUILD_LIST | is the relative path to the folder containing your build.xml and playlist.xml files |
| TARGET | refers to the name you specified in "testCaseName" section in your playlist.xml file |
| KEEP_WORKSPACE | after testrun the workspace is saved for a while for the developer to check it |
| ARCHIVE_TEST_RESULTS | after testrun the test results are by default discarded for passing tests, check for keeping them |

It is advised to check KEEP_WORKSPACE and ARCHIVE_TEST_RESULTS checkboxes for debuggin purposes. Adoptium infrastructure by default does not keep workspaces at all and results in case the tests were successful. In some cases runs can report as successful when it is not even executed, therefore nothing had a chance of failing there. It is a good practice to not only check that the test finishes "green" but also that the tests that were supposed to run actually did.
