# Vendor-Specific Testing

## Build Tests

### 1) Contents
1) Contents
2) Purpose
3) Usage
4) Current Tests
5) Adding new tests
6) FAQ

### 2) Purpose

The purpose of the Vendor Specific "Build" tests are to verify specific aspects of an OpenJDK build from a  
specific vendor (like Adopt).  

Specifically, tests which only verify quality when testing a build from that vendor. A build from a different  
vendor cannot be said to be good or bad as a result of passing or failing these tests.  

E.g. Adopt may decline to bundle an optional library with its builds. Builds from other providers can be  
perfectly ok both with and without this library, but an Adopt build could only be said to be "good" if  
the example lib was successfully excluded. So an adopt-specific test is created to test for the lib's absence 
 
### 3) Usage

The commands needed to run these tests locally can be found inside the playlist.xml file in this directory.

When run on a regular basis, these should be run alongside the existing functional tests as part of
a standard suite of post-build functional validation testing.

### 4) Current Tests

- **CUDA enabled test (AdoptOpenJDK)**: Tests that the CUDA functionality has been enabled in JDKs on supported platforms.
  
- **Freetype Absence test (AdoptOpenJDK)**: Tests for the absence of a bundled freetype library in any Linux JDK.

### 5) Adding new tests

To add a new test, follow these steps:

1) Add a title and brief description of your test to the "Current Tests" section above, including
the name/s of any vendors whose JDK builds you intend to test with this.

2) Add the test source (ideally testng) to:
```./src/net/\<vendor name\>/test/build/\<test name \(dir\)\>/```
Note: A test template is available for reference or copying, and can be found at:
```./src/net/adoptopenjdk/test/build/TestTemplate.java```

3) (Testng tests only) Add an entry to testng.xml for your test.

3) (Non-testng tests) If not a testng test, add a test execution command to the playlist.xml file in this directory, using other \<test\> elements as reference.

4) (Optional) If the test requires special treatment for building, modify the build.xml file as needed. Most java-based tests should require no changes.

### 5) FAQ

Q1) What happens if I run one of these tests against a non-Adopt build, or a build too old for what it's testing?  
A1) The test (if appropriate) should automatically pass if:  
- The build was not produced by the relevant vendor.  
- The test is being run on the wrong JDK major version.  
- The test is being run on a build that's too old for what we're testing.  
Note:  from: ./src/net/adoptopenjdk/test/build/common/BuildIs.java  

Q2) Can I put tests for non-AdoptOpenJDK vendor builds in here?  
A2) Yes, though you should ensure the vendor name in the source path is set correctly.

Q3) Where can I ask questions?
A3) The #testing Slack channel at AdoptOpenJDK is a good place to ask questions.
