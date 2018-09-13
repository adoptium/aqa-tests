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

# AdoptOpenJDK Testing

#### Guide to the Test Jobs at AdoptOpenJDK

For nightly and release builds, there are test jobs running as part of the AdoptOpenJDK continuous delivery pipelines.  There is a [blog post and brief presentation](https://blog.adoptopenjdk.net/2017/12/testing-java-help-count-ways) that explains what testing we run and how they fit into the overall delivery pipeline.  As the world of testing at AdoptOpenJDK is evolving and improving quickly, some documentation may fall behind the march of progress.  Please let us know and help us keep it up-to-date, and ask questions at the [AdoptOpenJDK testing Slack channel](https://adoptopenjdk.slack.com/messages/C5219G28G)!

![CI pipeline view](doc/ciPipeline.jpg)

See the different test jobs here:

- [openjdk](https://ci.adoptopenjdk.net/view/Test_openjdk/) - OpenJDK regression tests (from OpenJDK)

- [system](https://ci.adoptopenjdk.net/view/Test_system/) - System and load tests (from the AdoptOpenJDK/openjdk-systemtest repo)

- [external](https://ci.adoptopenjdk.net/view/Test_external/) - 3rd party application tests run in Docker containers (from different application projects such as scala, tomcat, etc)

- [perf](https://ci.adoptopenjdk.net/view/Test_perf/) - Open-source benchmark suites (from different open-source projects such as Acme-Air)

- [jck](https://ci.adoptopenjdk.net/view/Test_jck/) - TCK tests (under the OpenJDK Community TCK License Agreement), in compliance with the license agreement, are run on a private Jenkins server (jobs linked here are merely remote triggers to the private server)


#### Test 'Inventory'

The directory structure in this openjdk-tests repository is meant to reflect the different types of test we run (and pull from lots of other locations).  The diagram below shows the test make target for each of the types, along with in-plan, upcoming additions (denoted by dotted line grey boxes).

![Overview of Tests](doc/overviewOfAdoptTests.svg)

#### Guide to Running the Tests Yourself
For more details on how to run the same tests that we run at AdoptOpenJDK on your laptop or in your build farm, please consult our [User Guide](doc/userGuide.md) (work in progress).

#### What is our motivation?
We want:
- better, more flexible tests, with the ability to apply certain types of testing to different builds
- a common way to easily add, edit, group, include, exclude and execute tests on AdoptOpenJDK builds
- the latitude to use a variety of tests that use many different test frameworks
- test results to have a common look & feel for easier viewing and comparison

There are a great number of tests available to test a JVM, starting with the OpenJDK regression tests.  In addition to running the OpenJDK regression tests, we will increase the amount of testing and coverage by pulling in other open tests.  These new tests are not necessarily written using the jtreg format.

Why the need for other testing?  The OpenJDK regression tests are a great start, but eventually you may want to be able to test how performant is your code, and whether some 3rd party applications still work.  We will begin to incorporate more types of testing, including:
- additional API and functional tests
- stress/load tests
- system level tests such as 3rd party application tests
- performance tests
- TCK tests

The test infrastructure in this repository allows us to lightly yoke a great variety of tests together to be applied to testing the AdoptOpenJDK binaries.  By using an intentionally thin wrapper around a varied set of tests, we can more easily run all types of tests via make targets and as stages in our Jenkins CI pipeline builds.





