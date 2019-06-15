
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

# AdoptOpenJDK Performance Testing

We are in the process of adding more microbenchmarks (both bumblebench and jmh formats) and full open-source benchmarks to this directory. New perftest jobs are added to the [Test_perf](https://ci.adoptopenjdk.net/view/Test_perf/) view.  While we will run these benchmarks regularly at AdoptOpenJDK, the intention is to make it easy for developers to run performance testing locally.  

You can follow the [same manual testing instructions](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/doc/userGuide.md#local-testing-via-make-targets-on-the-commandline) to run these tests, as you do for all of the other tests we run.  The top-level make target for tests contained in this directory (openjdk-tests/perf) is "perf".  So, once you compile test material, running "make perf" will run all of the testcases defined in playlist.xml files in this directory and its subdirectories.  Each unique benchmark is housed in a 
subdirectory and given a meaningful name.  Once the reorganization of this directory is complete, the directory structure will look like:

```
//./
//├── micro
//├────── idle
//├────── bumblebench
//├────── jmh
//├── odm
//├── acme_air
//├── liberty_daytrader
```
Each subdirectory requires a build.xml file describing where to pull the benchmark suite from, and how to build and run it.  Each subdirectory also requires a playlist.xml file which describes what commands to run in order to execute a particular benchmark run.
### Microbenchmarks
#### idle_micro
Currently we run a single micro-benchmark called idle_micro against OpenJDK8 builds (both hotspot and openj9 variants).  

#### bumblebench  
In plan, we intend to add other microbenchmarks found in the [bumblebench repo](https://github.com/AdoptOpenJDK/bumblebench).  There are already a good variety of microbenchmarks for evaluating performance of various aspects of code, such as string, lambda, gpu, math, crypto, etc.

### Full Benchmark Suites
Transparency and the ability to see how the binaries are being exercised is important to us.  We will focus on running fully open-sourced benchmarks at AdoptOpenJDK so that developers have full-access to see the benchmarking code.  

#### odm 
Added, but not running regularly in builds, as some of the test materials are not yet available in the open.  

#### acme_air 
In plan, benchmark material to be cloned from the [acme-air git repo](https://github.com/blueperf/acmeair-monolithic-java).

#### liberty_daytrader
In plan
