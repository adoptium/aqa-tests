---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
---

# How to run a Grinder test job at Adoptium

<a name="quickStart"></a>
## Quick Start Documentation
There are far too many input parameters to the AQAvit Jenkins jobs.  Luckily almost all of them are optional and can be ignored for some common test scenarios.

<a name="rerunOpenJDK"></a>
### Rerun a failing OpenJDK regression test
You can find "**Rerun in Grinder**" links near the top of the Jenkins job that failed.  See Section A in the diagram below.  Section B shows the test target summary for a particular job.  Section C shows individual testcases, if the type of testing run produces a standard format (NUnit style output).

<img width="484" alt="rerunLinks" src="https://user-images.githubusercontent.com/2836948/220449801-35cec3ee-ef65-41fc-b24a-6528d8ad55c6.png">

*  "**Rerun in Grinder**" link opens a Grinder with pre-populated parameters, but it sets the TARGET as the entire target used by the job (example, TARGET=sanity.openjdk).
*  "**Rerun in Grinder on same machine**" does the same as Rerun in Grinder, but also sets LABEL=`hostname` of the machine where it failed
    * In the case you want to rerun on a particular machine that the test failed on, because the test seems to only fail on particular machines, you could change LABEL=`hostname`, example, LABEL=test-godaddy-centos7-x64-2
*  "**Rerun in Grinder with failed targets**" does the same as Rerun in Grinder, but changes TARGET=`list of failed targets`
*  "**Rerun failed jdk test cases in Grinder**" does the same as Rerun in Grinder, but changes TARGET=`jdk_custom` and CUSTOM_TARGET=`space-separated list of failed testcases to rerun`
    * To rerun a single test case in the `openjdk` group (for example the test case java/math/BigInteger/LargeValueExceptions.java, group jdk\_math for example), click on either rerun link described above (so that the most of the other parameters used in the failing tests are pre-populated and then edit any parameters you want to be able to run a particular variation of the previous run.  In this case, you can set TARGET=jdk\_custom and CUSTOM_TARGET=java/math/BigInteger/LargeValueExceptions.java to rerun just that particular test class.
    *   If you want to run a specific list of testcases, you can set TARGET=jdk\_custom and space-separated list of test classes for CUSTOM_TARGET=`java/math/BigInteger/LargeValueExceptions.java java/net/Inet6Address/B6206527.java` to rerun that particular test class
    *   If you want to run a directory of tests, TARGET=jdk\_custom and CUSTOM_TARGET=jdk/test/java/math/BigInteger to run the test cases within that directory

<a name="runSystemCustom"></a>
### Run `system_custom` in a Grinder 
* For running STF based system tests using `system_custom` target in a Grinder, please ensure the following is set:
    * BUILD_LIST=system
    * TARGET=system_custom
    * CUSTOM_TARGET=-test=<stf_test_class_name>
              - Optionally, if test requires arguments, CUSTOM_TARGET=-test=<stf_test_class_name>  -test-args="x=a,y=b" (e.g. CUSTOM_TARGET=-test=MathLoadTest -test-args="workload=math,timeLimit=5m")

* Note : `<stf_test_class_name>` should be the name of the STF class to run, not a Playlist target name. For example, if you want to re-run [ClassLoadingTest_5m](https://github.com/adoptium/aqa-tests/blob/517467de209aae47db938d4d2b58f45727912322/system/otherLoadTest/playlist.xml#L57), using system_custom, you can simply copy the last portion of the command line from the playlist (e.g. `-test=ClassloadingLoadTest -test-args="timeLimit=5m"`) for CUSTOM_TARGET.
