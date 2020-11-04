
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

# Liberty Benchmarks

**TARGET** | Description |
-- | -- | 
liberty-dt7-startup | Benchmark to measure startup time and footprint of server running Liberty with DayTrader7 | 
liberty-dt7-throughput | Benchmark to measure throughput of server running Open Liberty with DayTrader7, connected to Apache Derby database, by driving load with Apache JMeter |

### How to use the Liberty Framework?

Please see `usage()` function in various scripts: `sufp_benchmark.sh`, `throughput_benchmark.sh`, and `configure_liberty.sh`.

### Prerequisites

The following prerequisites are needed in order to measure various metrics like footprint. Without these prerequisites, the benchmarks should run, but they won't produce values for some metrics, which require those prerequisites to be installed.

Platform | Prerequisites |
-- | -- | 
All | wget, bc (only required for throughput) |
Linux | Huge Pages (Optional) |
Windows | vmmap |

