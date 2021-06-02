
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

You can follow the [same manual testing instructions](https://github.com/adoptium/aqa-tests/blob/master/doc/userGuide.md#local-testing-via-make-targets-on-the-commandline) to run these tests, as you do for all of the other tests we run.  The top-level make target for tests contained in this directory (openjdk-tests/perf) is "perf".  So, once you compile test material, running "make perf" will run all of the testcases defined in playlist.xml files in this directory and its subdirectories.  Each unique benchmark is housed in a 
subdirectory and given a meaningful name.  Once the reorganization of this directory is complete, the directory structure will look like:

```
//./
//├── bumbleBench
//├── idle_micro
//├── dacapo
//├── renaissance
//├── liberty

```
Each subdirectory requires a build.xml file describing where to pull the benchmark suite from, and how to build and run it.  Each subdirectory also requires a playlist.xml file which describes 1 or more benchmarks and what commands to run in order to execute a particular benchmark run.

### Microbenchmarks
#### idle_micro
Currently we run a single micro-benchmark called idle_micro against OpenJDK8 builds (both hotspot and openj9 variants).  

#### bumblebench  
Microbenchmarks found in the [bumblebench repo](https://github.com/AdoptOpenJDK/bumblebench).  There are already a good variety of microbenchmarks for evaluating performance of various aspects of code, such as string, lambda, gpu, math, crypto, etc.  Microbenches include bumbleBench-ArrayListForEachConsumerAnonymousBench, bumbleBench-ArrayListForEachConsumerFinalBench, bumbleBench-ArrayListForEachLambdaBench, bumbleBench-ArrayListForEachTradBench, bumbleBench-ArrayListRemoveIfLambdaBench, bumbleBench-ArrayListRemoveIfTradBench, bumbleBench-ArrayListSortCollectionsBench, bumbleBench-ArrayListSortComparatorBench, bumbleBench-ArrayListSortLambdaBench, bumbleBench-HashMapForEachBiConsumerAnonymousBench, bumbleBench-HashMapForEachBiConsumerFinalBench, bumbleBench-HashMapForEachLambdaBench, bumbleBench-HashMapForEachTradBench, bumbleBench-HashMapReplaceAllLambdaBench, bumbleBench-HashMapReplaceAllTradBench, bumbleBench-CipherBench, bumbleBench-DigestBench, bumbleBench-EllipticCurveBench, bumbleBench-GCMBench, bumbleBench-HMACBench, bumbleBench-KeyExchangeBench, bumbleBench-RSABench, bumbleBench-SignatureBench, bumbleBench-SSLSocketBench, bumbleBench-BitonicBench-CPU, bumbleBench-BitonicBench-GPULambda, bumbleBench-DoitgenBench-CPU, bumbleBench-DoitgenBench-GPULambda, bumbleBench-KmeansBench-CPU, bumbleBench-KmeansBench-GPULambda, bumbleBench-MatMultBench-CPU, bumbleBench-MatMultBench-GPULambda, bumbleBench-DistinctStringsEqualsBench, bumbleBench-NewStringBufferWithCapacityBench, bumbleBench-NewStringBuilderWithCapacityBench, bumbleBench-SameStringsEqualsBench, bumbleBench-DispatchBench-InnerClasses, bumbleBench-FibBench-Vanilla, bumbleBench-FibBench-InnerClass, bumbleBench-FibBench-Lambda, bumbleBench-FibBench-LocalLambda, bumbleBench-FibBench-DynamicLambda, bumbleBench-FibBench-LocalMethodReferences, bumbleBench-GroupingBench-Serial, bumbleBench-GroupingBench-Parallel, bumbleBench-SieveBench, bumbleBench-ExactBench, bumbleBench-SIMDDoubleMaxMinBench, bumbleBench-StringConversionBench, bumbleBench-StringHashBench, bumbleBench-StringIndexOfBench and bumbleBench-StringIndexOfStringBench.

### Full Benchmark Suites
Transparency and the ability to see how the binaries are being exercised is important to us.  We will focus on running fully open-sourced benchmarks at AdoptOpenJDK so that developers have full-access to see the benchmarking code.  

#### dacapo
Dacapo benchmarks from https://github.com/dacapobench/dacapobench - including dacopo-eclipse, dacapo-avrora, dacapo-fop, dacapo-h2, dacapo-jython, dacapo-luindex, dacapo-lusearch-fix, dacapo-pmd, dacapo-sunflow, dacapo-tomcat and dacapo-xalan.

#### liberty
Liberty benchmarks from https://github.com/OpenLiberty - including liberty-dt7-startup and liberty-dt7-throughput

#### renaissance
Renaissance benchmarks from https://github.com/renaissance-benchmarks/renaissance - including renaissance-akka-uct, renaissance-als, renaissance-chi-square, renaissance-db-shootout, renaissance-dec-tree, renaissance-finagle-chirper, renaissance-finagle-http, renaissance-fj-kmeans, renaissance-future-genetic, renaissance-gauss-mix, renaissance-log-regression, renaissance-mnemonics, renaissance-movie-lens, renaissance-naive-bayes, renaissance-par-mnemonics, renaissance-philosophers and renaissance-scala-kmeans

Additional benchmarks are being reviewed for addition and if you wish to include more, please comment in the open performance benchmarks [issue 1112](https://github.com/adoptium/aqa-tests/issues/1112).
