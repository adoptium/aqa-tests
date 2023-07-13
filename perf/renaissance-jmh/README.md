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

# Renaissance Benchmark Suite (JMH)

The [Renaissance Benchmark Suite](https://github.com/renaissance-benchmarks/renaissance/#) is an open-source benchmark suite for testing the performance of the JVM. This suite also [supports](https://github.com/renaissance-benchmarks/renaissance/#jmh-support) running these benchmarks using [JMH](https://github.com/openjdk/jmh). This is the method used to run the benchmarks in this directory.

## Generating the playlist.xml

To enable quick iterations on the configurations used for this benchmarking, a `gen_playlist.py` script has been provided. Edit this script and use Python to execute it when you want to generate a new `playlist.xml` with different configurations.  

The script will generate a `test` for each benchmark in the Renaissance Benchmark Suite (JMH), with specific GC and benchmark options. These options may change over time and/or be moved and set at different stages of the testing pipeline.