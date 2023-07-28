## Applications of AQAvit in Adoptium Quality Assurance (AQA)

AQAvit is a critical component of the Adoptium Quality Assurance (AQA) process, designed to ensure the reliability and excellence of OpenJDK builds distributed by the Eclipse Adoptium project. AQAvit serves various applications in the AQA workflow, contributing to the delivery of high-quality Java runtimes for developers and users. Let's explore the specific applications of AQAvit:

### 1. AQAvit Verification:
AQAvit is primarily used for verification testing of the OpenJDK builds. This application involves a comprehensive suite of automated tests that assess different aspects of the builds to verify their correctness and adherence to Java SE specifications. Key aspects covered by AQAvit verification include:

- Functional Correctness: - AQAvit tests the functional correctness of the OpenJDK builds, ensuring that they execute Java applications and libraries as expected and according to the Java SE standard.

- Stability and Reliability: - AQAvit assesses the stability and reliability of the OpenJDK builds, verifying that they operate without crashes or unexpected behavior under various conditions.

- Performance Benchmarking: - AQAvit includes performance tests to benchmark the OpenJDK builds, ensuring that they meet acceptable performance criteria for a wide range of applications.

- Security Testing: AQAvit verifies OpenJDK builds against several security test suites and for known vulnerabilities.

AQAvit verification is focussed on testing for 'quality'.  Compatibility testing is done as part of the [Eclipse Temurin Compliance project](https://projects.eclipse.org/projects/adoptium.temurin-compliance) and is outside of the scope of AQAvit.

### 2. Developer Support:
AQAvit plays a vital role in providing support to development teams.  It supports both OpenJDK developers (those working on the upstream OpenJDK project) and Java developers (those writing Java applications that are built and run on the distributed OpenJDK binaries). This application involves publicly visible regular test runs, parameterization and bug tracking to assist developers in the following ways:

- Publicly available test results: - AQAvit's automated tests run regularly at the Eclipse [Adoptium Jenkins server](https://ci.adoptium.net/). 
 Test results are visible through the Jenkins server directly and also through the [public TRSS instance](https://trss.adoptium.net/).  These regular results provide quick feedback on newly committed code changes, enabling developers to identify and address issues promptly.

- Continuous Integration (CI) Pipelines and Pull Request testing: AQA tests are integrated into the Temurin build pipelines and run automatically with each new Temurin build.  In addition to the build pipeline, AQA tests can be triggered via a standalone AQA Test Pipeline or Grinder job, both of which can pull the OpenJDK binary they are intended to test from either the [Adoptium API](https://api.adoptium.net/), a previously run Jenkins job or from any public URL that may be hosting a JDK binary available for testing.  

Since AQA tests can be run via the [run-aqa Github action](https://github.com/adoptium/run-aqa), Pull request (PR) testing can be incorporated into a development workflow and tuned to specific developer needs.

This ensures that developers have access to up-to-date and customized builds for their development and testing needs.

- Bug Tracking and Reporting: - AQAvit contributes to the bug tracking and reporting system in the AQA process, allowing developers to log and track issues that arise during testing or are reported by the community.

- Issue Resolution: - By identifying and highlighting potential problems, AQAvit supports the iterative bug-fixing process, leading to the continuous improvement of the OpenJDK builds.

