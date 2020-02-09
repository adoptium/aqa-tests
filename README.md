<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[1]https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
-->

# 1. AdoptOpenJDK Quality Assurance (AQA)

If you have a questions not answered by this documentation, please ask questions in the [#testing Slack channel](https://adoptopenjdk.slack.com/messages/C5219G28G).

- [1. AdoptOpenJDK Quality Assurance (AQA)](#1-adoptopenjdk-quality-assurance-aqa)
    - [1.0. What is AQA](#10-what-is-aqa)
        - [1.0.0. Test Organization](#100-test-organization)
        - [1.0.1. AQA Goals](#101-aqa-goals)
        - [1.0.2. AQA Manifesto](#102-aqa-manifesto)
    - [1.1. Ways to Use AQA](#11-ways-to-use-aqa) 
        - [1.1.1. Running locally](#111-running-locally)
        - [1.1.2. Specific Examples of Targets and Env Vars](#112-specific-examples-of-targets-and-env-vars)
        - [1.1.3. Running in Jenkins](#113-running-in-jenkins)
        - [1.1.4. Running in Azure Devops](#114-running-in-azure-devops)
        - [1.1.5 Running via Github Action](#115-running-via-github-action)  
    - [1.2. How to view Test Results](#12-how-to-view-test-results)
        - [1.2.0. How to triage a release pipeline](#120-how-to-triage-a-release-pipeline)
    - [1.3. How can you help?](#13-how-can-you-help?) 
    - [1.4. Additional Reference material](#14-additional-reference-material)     


## 1.0. What is AQA

AQA is a very good set of curated tests to best assure that AdoptOpenJDK binaries fulfill the requirements of our enterprise consumers, validating the binaries are:
- functionally correct
- performant
- secure
- scalable
- durable

### 1.0.0 Test Organization
This [openjdk-tests repository](https://github.com/AdoptOpenJDK/openjdk-tests) is the central location for the AdoptOpenJDK Quality Assurance (AQA) effort.  While we pull test material and tools from a variety of repositories, this repo defines the structure in which the test material is organized, built and executed.  

In order to address requirements of our enterprise consumers, our test material is categorized into different logical groups which is also reflected in the directory structure of this repository:

```
//./
//├── external  
//├── functional 
//├── openjdk 
//├── perf
//├── system
```

- external: microprofile TCKs from popular frameworks and application functional tests
- functional: functional tests from Eclipse OpenJ9 and other contributors
- openjdk: functional/regression tests from OpenJDK
- perf: from various open-source performance benchmark projects
- system: various system level tests including stress/load tests contributed to AdoptOpenJDK

For details on our groups of tests, you can refer to the README files within each corresponding directory.  Test directories contain instructions on how to build test material (in build.xml) and definition of the test targets to run (in playlist.xml).  For test material from other projects, we generally wrap its underlying test framework with our [testkitgen (TKG)](https://github.com/AdoptOpenJDK/TKG) framework, to make all tests look and feel the same (to both automation tooling and to developers).

Playlists offer various further ways of 'slicing and dicing' tests (by JDK version, by VM implementation, and by levels.  Test targets are divided into 3 levels, sanity, extended and special to help optimize test automation and scheduling.  For more information, please refer to the wiki called [Graduated Testing and Test Numbers](https://github.com/AdoptOpenJDK/openjdk-tests/wiki/Graduated-Testing-&-Test-Numbers) outlining the number of test targets in each group, and the schedule upon which they are currently run.

### 1.0.1 AQA Goals
Our main goal is to test OpenJDK binaries built by the AdoptOpenJDK project (build scripts reside in the [openjdk-build repository](https://github.com/AdoptOpenJDK/openjdk-build)) to "make quality certain to happen". We additionally strive to support JVM developers by making it easier for them to run tests against their changes.  

[Issue 965](https://github.com/AdoptOpenJDK/openjdk-tests/issues/965) describes our AQA goals and definition and links to a more detailed description of AQA. 

Summary of Goals:
- **Assure quality of binaries and images** produced at the AdoptOpenJDK project by verifying against enterprise grade requirements
- **Empower open runtime developers.**  Give them a way to verify their contributions and see their test results and artifacts.  Allow them to rerun test failures and debug issues locally prior to final contribution
- **Engage application and framework communities.**  Test the major frameworks and applications in our CI environment and provide a way for any application community to easily test out their applications with AdoptOpenJDK binaries 
- **Support any other OpenJDK distributors** by providing a quality kit to use within their own environments to ensure a high quality distributions 

### 1.0.2. AQA Manifesto
Verification of AdoptOpenJDK binaries should:

- be open & transparent
- consist of a diverse & robust set of test suites
- evolve alongside implementations
- have continual investment
- have process to modify
- be measured by codecov & other metrics
- allow for comparative analysis
- be portable
- get tagged & published alongside binaries 

To be guided by this criteria, we help build confidence and provide results transparency to consumers of the binaries produced by the AdoptOpenJDK project.

## 1.1. Ways to Use AQA

As per our goals, we support a variety of methods to use AQA test material:
- Running via command-line locally
- Running as a Github Action
- Running in various CI environments like Jenkins, Azure Devops

### 1.1.1 Running locally

To support development teams, AQA tests can be run locally.  These instructions assume you have set up your machine with the [test prereqs](https://github.com/eclipse/openj9/blob/master/test/docs/Prerequisites.md) and have built or downloaded the SDK you want to test.

We run a diverse set of tests that use various underlying test frameworks.  In order to standardize them, we use TestKitGen ([TKG](https://github.com/AdoptOpenJDK/TKG)) which wraps them to provide a common look and feel.  These general steps are common to all test groups:

1. State location of SDK under test (example: in /jdk8u242-b06/Contents/Home)
    - export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home
1. Clone openjdk-tests repo
    - git clone https://github.com/AdoptOpenJDK/openjdk-tests.git 
    - cd openjdk-tests
1. Fetch TKG 
    - ./get.sh -t ${PWD}
    - cd TKG
1. Set appropriate env vars (see [1.1.2. Specific Examples](#112-examples))
    - export BUILD_LIST=`[system|openjdk|functional|perf|external|jck|commaSeparatedTestDirs]`
1. Compile test material (uses build.xml files in test subdirectories)
    - make compile 
1. Execute test targets (by level/group/testCaseName defined in playlist.xml)
    - make `[sanity|extended|special|testCaseNameFromPlaylistFiles]`   

Note: Steps 1-3 only need to be done once in the workspace.  Steps 4-6 can be repeated many times defining different environment variables and test targets to cumulatively incorporate different testing in the same workspace.

### 1.1.2 Specific Examples of Targets and Env Vars

Follow the [1.1.1. General Steps](#111-general-steps) setting the environment variables in step 4 and executing the test target in step 6.  These examples are a small sample of possibilities.  

| Test group  | Test Target | Env variables | 
|----------|-----------|--------|
| [functional](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/functional/README.md#how-to-build-and-run)    | _special.functional |export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home <br/>export BUILD_LIST=functional  | 
| [system](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/system/README.md#how-to-build-and-run)          | _extended.system| export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home <br/>export BUILD_LIST=system   |
| [openjdk](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/openjdk/README.md#how-to-build-and-run)  | _sanity.openjdk |export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home <br/>export BUILD_LIST=openjdk |
| [openjdk](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/openjdk/README.md#how-to-build-and-run)   | _jdk_custom | export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home <br/>export BUILD_LIST=openjdk <br/>export JDK_CUSTOM_TARGET=java/lang/Class/GetModuleTest.java |
| [external](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/external/README.md#how-to-build-and-run)   | _sanity.external | export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home <br/>export BUILD_LIST=external|
| [external](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/external/README.md#how-to-build-and-run)   | _external_custom | export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home <br/>export BUILD_LIST=external/custom|
| [perf](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/perf/README.md#how-to-build-and-run)   | _dacapo-eclipse | export TEST_JDK_HOME=/jdk8u242-b06/Contents/Home <br/>export BUILD_LIST=perf/dacapo|

### 1.1.3. Running in Jenkins

### 1.1.4. Running in Azure Devops

### 1.1.5 Running via Github Action

## 1.2. How to View Test Results

### 1.2.0. How to Triage a Release Pipeline

## 1.3. How can you help?
You can:
- browse through the [openjdk-tests issues list](https://github.com/AdoptOpenJDK/openjdk-tests/issues), select one, add a comment to claim it and ask questions
- browse through the [openjdk-systemtest issues](https://github.com/AdoptOpenJDK/openjdk-systemtest/issues) or [stf issues](https://github.com/AdoptOpenJDK/stf/issues), claim one with a comment and dig right in
- triage live test jobs at [ci.adoptopenjdk.net](https://ci.adoptopenjdk.net), check out the [triage doc](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/doc/Triage.md) for guidance
- ask questions in the [#testing channel](https://adoptopenjdk.slack.com/messages/C5219G28G) 


### 1.4. Additional Reference material

Several additional documents and articles have been written to describe AQA. [AdoptOpenJDK Quality Assurance (AQA) 1.0](doc/AQAv1.0.pdf) was the first cut of the community-driven discussion on open verification.  [The First Drop: AdoptOpenJDK Quality Assurance (AQA) v1.0](https://medium.com/adoptopenjdk/the-first-drop-introducing-adoptopenjdk-quality-assurance-aqa-v1-0-fe09f10ced80) also summarizes the goals and definition of this quality bar.


