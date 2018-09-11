<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[1]http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
-->

# OpenJDK regression tests
This group of tests are the set that comes from the openjdk project, often referred to as jtreg tests, as the jtreg framework is the underlying executable used to execute them.  The entire set of openjdk regression tests is quite large.  For our nightly builds, we run only a subset of these tests (typically, those suites of tests that we 'tag' as sanity in the [playlist.xml](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/openjdk_regression/playlist.xml) file).  For release builds, we intend to run the suites tagged as sanity and extended in the playlist.  

For more details on how the underlying jtreg harness works, you can refer to the ["How to Use the JTreg harness" guide](https://adoptopenjdk.gitbooks.io/adoptopenjdk-getting-started-kit/en/intermediate-steps/how_to_use_jtreg_-_java_regression_test_harness.html).  

## Add a sub group test
We already have a large set of tests defined in the playlist.xml file, but not all of the openjdk regression test groups are presented there.  If you wish to define more, add a `<test></test>` in playlist.xml and specify:

* testCaseName
* command (how to run the test) - essentially the command that invokes the underlying jtreg harness
* subset: sdk version
* levels: sanity, extended
* groups: openjdk

## Exclude a testcase
Update ProblemList_(JVM_VERSION).txt to exclude testcases which fails in adoptopenjdk regression test build.

List items  are testnames followed by labels, all MUST BE commented
as to why they are here and use a label:

* generic-all   Problems on all platforms
* generic-ARCH  Where ARCH is one of: x64, s390x, ppc64le, sparc, sparcv9, i586, etc.
* OSNAME-all    Where OSNAME is one of: solaris, linux, windows, macosx, aix
* OSNAME-ARCH   Specific on to one OSNAME and ARCH, e.g. solaris-amd64
* OSNAME-REV    Specific on to one OSNAME and REV, e.g. solaris-5.8

	
**Note:** If the test will be run more than once ( more than one annotation @test in test source code) need to append specific testcase number something like following:

	* java/util/concurrent/tck/JSR166TestCase.java#id0  0000 generic-all

## Fixing the tests:
Some tests just may need to be run with "othervm", and that can easily be
done by adding a @run line (or modifying any existing @run):
	
	* @run main/othervm NameOfMainClass
Make sure this @run follows any use of @library.
Otherwise, if the test is a samevm possibility, make sure the test is
cleaning up after itself, closing all streams, deleting temp files, etc.
Keep in mind that the bug could be in many places, and even different per
platform, it could be a bug in any one of:

* the testcase
* the jdk (jdk classes, native code, or hotspot)
* the native compiler
* the javac compiler
* the OS (depends on what the testcase does)

If you managed to really fix one of these tests, here is how you can
remove tests from this list:

* Make sure test passes on all platforms with samevm, or mark it othervm
* Make sure test passes on all platforms when run with it's entire group
* Make sure both VMs are tested, -server and -client, if possible
* Make sure you try the -d64 option on Solaris
* Use a tool like JPRT or something to verify these results
* Delete lines in this file, include the changes with your test changes

You may need to repeat your testing 2 or even 3 times to verify good
results, some of these samevm failures are not very predictable.
