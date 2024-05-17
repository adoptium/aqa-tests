# Guidance for Creating OpenJDK Test Defects

If this is an openjdk test failure, there are a couple of steps to creating a useful test defect that we would like people to follow:

1. **Check whether a defect already exists in JBS (Java Bug System) and link to that defect, if it does. Steps to check for duplicate bugs:**
   1. Open JBS by opening the URL [https://bugs.openjdk.java.net](https://bugs.openjdk.java.net) in a web browser.
   2. Go to search by selecting Issues/Search for Issues.
   3. Switch to advanced, search by selecting “Advanced” link.
   4. Use this query: `text ~ "test name"`. This will show all bugs where a particular test is mentioned whether in summary, description or comments. Example: `text ~ "runtime/NMT/NMTWithCDS.java"` would give [https://bugs.openjdk.java.net/browse/JDK-8055814?jql=text%20~%20runtime%2FNMT%2FNMTWithCDS.java%22](https://bugs.openjdk.java.net/browse/JDK-8055814?jql=text%20~%20runtime%2FNMT%2FNMTWithCDS.java%22).
   5. Make sure to look through:
      - All unresolved bugs in the view
      - All recently updated bugs
      - Make sure to review bug description and comments.
   6. Review bugs to see if the test failure symptoms look similar to the reported failure:
      - Check exception messages, stack traces
      - Check test debug output.

2. **Using a bug template when filling out the details of the defect, to help others triage and fix the defect. As some of the issues in this repo are for enhancements, we will not at this time provide this as the default template. For now, when reporting a test failure, cut and paste this text into the issue and provide as much of the details as possible (in future, we can update the test reporting system to supply this information in this format as output of the test):**
```
Test category:
Testsuite name:
Test name(s):
Product(s) tested:
OS/architecture:
Platform specific: [yes|no]

Reproducibility:[always|intermittent],[single-run|group-run]
Reproducible on machine: [machineInfo, if reproducible on a particular machine]
Regression: yes|no|unsure [notes, if yes, what build introduced]

Exception/Error from Log:
Error log file (if available): link to the file

Steps to reproduce
```
**An example of this template filled out would then be:**
```
Test category: openjdk
Testsuite name: jdk_rmi
Test name(s): java/rmi/activation/rmidViaInheritedChannel/InheritedChannelNotServerSocket.java
Product(s) tested: JDK 8u144 b01
OS/architecture: Ubuntu16.04 x64
Platform specific: yes

Reproducibility: always
Reproducible on machine: machinename1
Regression: yes, introduced in JDK 8u144 b01

Exception/Error from Log:
some output from log file
Error log file (if available): link to file, something like: https://ci.adoptopenjdk.net/view/OpenJDK%20tests/job/[JVM_VERSION]_test_x86-64_linux/[BUILD_NUMBER]/artifact/

Steps to reproduce:
1) Re-include test.  (Search the Test class name in ProblemList.txt find the exclude line and comment the line
In this case looking for a group called jdk_rmi and an entry like this, "java/rmi/activation/rmidViaInheritedChannel/InheritedChannelNotServerSocket.java 154 macosx-all")
2) make jdk_rmi_SE80_0  (assuming you are running with the test material already setup, if not follow openjdk-tests/README.md instructions to set up test material first, then run the make target).
```


