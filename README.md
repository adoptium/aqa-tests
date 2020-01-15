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

In the event you do not find the answer to your AdoptOpenJDK Quality Assurance (AQA) questions in this documentation, please post questions in the [#testing Slack channel](https://adoptopenjdk.slack.com/messages/C5219G28G).

- [1. AdoptOpenJDK Quality Assurance (AQA)](#1-adoptopenjdk-quality-assurance-aqa)
    - [1.1. AQA Quick Start](#12-quick-start) 
        - [1.1.1. General Steps to run locally](#111-general-steps-to-run-locally)
        - [1.1.2. Examples](#112-examples)
    - [1.2. AQA Goals and Definition](#12-aqa-goals-and-definition)   
        - [1.2.1. AQA Overview](#121-aqa-overview)
        - [1.2.2. AQA Manifesto](#122-aqa-manifesto)        
        - [1.2.3. Additional Reference material](#123-additional-reference-material)   
      
    - [1.3. Where we test](#13-where-we-test)    
    - [1.4. How can you help?](#14-how-can-you-help?)   
    - [1.5. How to view Test Results](#15-how-to-view-test-results)
        - [1.5.1. How to triage a release pipeline](#151-how-to-triage-a-release-pipeline)
    - [1.6. How to setup Jenkins test environment](#16-how-to-setup-jenkins-test-environment)

## 1.1. AQA Quick Start

To support development teams, AQA tests can be run locally.  These instructions assume you have set up your machine with the [test prereqs](https://github.com/eclipse/openj9/blob/master/test/docs/Prerequisites.md) and have built or downloaded the SDK you want to test.

### 1.1.1. General steps to run locally

We run a diverse set of tests that use various underlying test frameworks.  In order to standardize them, we use TestKitGen ([TKG](https://github.com/AdoptOpenJDK/TKG)) which wraps them to provide a common look and feel.  These general steps are common to all test groups:

1. State location of SDK you want to test (for example in /jdkHome)
    - export TEST_JDK_HOME=/jdkHome 
1. Clone openjdk-tests repo
    - git clone https://github.com/AdoptOpenJDK/openjdk-tests.git (into /testLocation)
    - cd openjdk-tests
1. Fetch TKG and set appropriate environment variables 
    - ./get.sh -t /testLocation/openjdk-tests
    - cd TestConfig
1. Export environment variables appropriate for tests you want to run  
    - see [1.1.2. Examples](#112-examples)
1. Generate applicable test targets (uses playlist.xml in test subdirectories)
    - make -f run_configure.mk   
1. Compile test material (uses build.xml files in test subdirectories)
    - make compile 
1. Execute test targets (by level/group/testCaseName defined in playlist.xml)
    - make < _someTestTarget >    

Note: Steps 4-7 can be repeated many times defining different environment variables to cumulatively incorporate different or more testing in the same workspace.

### 1.1.2 Examples

These examples are a small sample of possibilities.  JDK_VERSION, JDK_IMPL and SPEC/Platform are auto-detected by our test framework (based on querying the SDK under test defined by the required environment variable, TEST_JDK_HOME).  

You need only to know what test target you wish to run and what test group to which it belongs (so that you can set BUILD_LIST and minimize the amount of test material that is built).  Top-level test targets are available for each group, while individual test targets are defined in the playlist.xml files in the test subdirectories.  Follow the [1.1.1. General Steps](#111-general-steps) setting the environment variables in step 4 and executing the test target in step 7.

| Test group  | Test Target | Env variables | 
|----------|-----------|--------|
| [functional](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/functional/README.md#how-to-build-and-run)    | _special.functional |export TEST_JDK_HOME=/jdkHome <br/>export BUILD_LIST=functional  | 
| [system](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/system/README.md#how-to-build-and-run)          | _extended.system| export TEST_JDK_HOME=/jdkHome <br/>export BUILD_LIST=system   |
| [openjdk](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/openjdk/README.md#how-to-build-and-run)  | _sanity.openjdk |export TEST_JDK_HOME=/jdkHome <br/>export BUILD_LIST=openjdk |
| [openjdk](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/openjdk/README.md#how-to-build-and-run)   | _jdk_custom | export TEST_JDK_HOME=/jdkHome <br/>export BUILD_LIST=openjdk <br/>export JDK_CUSTOM_TARGET=java/lang/Class/GetModuleTest.java |
| [external](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/external/README.md#how-to-build-and-run)   | _sanity.external | export TEST_JDK_HOME=/jdkHome <br/>export BUILD_LIST=external|
| [perf](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/perf/README.md#how-to-build-and-run)   | _dacapo-eclipse | export TEST_JDK_HOME=/jdkHome <br/>export BUILD_LIST=perf/dacapo|


## 1.2. AQA Goals and Definition
This [openjdk-tests repository](https://github.com/AdoptOpenJDK/openjdk-tests) is the central location for the AdoptOpenJDK Quality Assurance (AQA) effort.  While we pull test material and tools from a variety of repositories, this repo defines the structure in which the test material is organized, built and executed.  

Our main goal is to test OpenJDK binaries built by the AdoptOpenJDK project (build scripts reside in the [openjdk-build repository](https://github.com/AdoptOpenJDK/openjdk-build)) to "make quality certain to happen". We additionally strive to support JVM developers by making it easier for them to run tests against their changes.  

[Issue 965](https://github.com/AdoptOpenJDK/openjdk-tests/issues/965) summarizes our goals and definition.  

### 1.2.1. AQA Overview
We are interested in verifying that the OpenJDK binaries we test are:
 
- functionally correct
- secure
- performant
- scalable
- durable

In order to address these requirements, our test material is categorized into different logical groups which is also reflected in the directory structure of this repository:

```
//./
//├── external
//├── functional
//├── openjdk
//├── perf
//├── system
```

For details on these groups of tests, please refer to the corresponding README files within each subdirectory.  Subdirectories contain instructions on how to build test material (in build.xml) and definition of the test targets to run (in playlist.xml).  For test material from other projects, we generally wrap its underlying test framework with our [testkitgen (TKG)](https://github.com/AdoptOpenJDK/TKG) framework, to make all tests look and feel the same (to both automation tooling and to developers).

Playlists offer various further ways of 'slicing and dicing' tests (by JDK version, by VM implementation, and by levels.  Test targets are divided into 3 levels, sanity, extended and special to help optimize test automation and scheduling.  For more information, please refer to the wiki called [Graduated Testing and Test Numbers](https://github.com/AdoptOpenJDK/openjdk-tests/wiki/Graduated-Testing-&-Test-Numbers) outlining the number of test targets in each group, and the schedule upon which they are currently run.

### 1.2.2. AQA Manifesto
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

### 1.2.3. Additional Reference material

Several additional documents and articles have been written to describe AQA. [AdoptOpenJDK Quality Assurance (AQA) 1.0](doc/AQAv1.0.pdf) was the first cut of the community-driven discussion on open verification.  [The First Drop: AdoptOpenJDK Quality Assurance (AQA) v1.0](https://medium.com/adoptopenjdk/the-first-drop-introducing-adoptopenjdk-quality-assurance-aqa-v1-0-fe09f10ced80) also summarizes the goals and definition of this quality bar.

## 1.3. Where we test

## 1.4. How can you help?
You can:
- browse through the [openjdk-tests issues list](https://github.com/AdoptOpenJDK/openjdk-tests/issues), select one, add a comment to claim it and ask questions
- browse through the [openjdk-systemtest issues](https://github.com/AdoptOpenJDK/openjdk-systemtest/issues) or [stf issues](https://github.com/AdoptOpenJDK/stf/issues), claim one with a comment and dig right in
- triage live test jobs at [ci.adoptopenjdk.net](https://ci.adoptopenjdk.net), check out the [triage doc](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/doc/Triage.md) for guidance
- ask questions in the [#testing channel](https://adoptopenjdk.slack.com/messages/C5219G28G) 

## 1.5. How to View Test Results

### 1.5.1. How to Triage a Release Pipeline

## 1.6. How to setup Jenkins test environment



