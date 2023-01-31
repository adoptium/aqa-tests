
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

# TechEmpower Framework Benchmarks (TFB)

The TechEmpower Framework Benchmarks is an [open source](https://github.com/TechEmpower/FrameworkBenchmarks) performance comparison of many web application frameworks executing tasks such as JSON serialization, database access, and server-side template composition. AQA performance testing currently only supports the [spring](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Java/spring) and [jetty](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Java/jetty) benchmarks, but other benchmarks may be added in the future.

## Running TechEmpower

The current implementation in the AQA test framework assumes that Docker is installed on the test machine and the Docker daemon is running before TFB tests are run. 

A subsequent PR will add the ability to configure the number of CPUs and memory given to the test container.