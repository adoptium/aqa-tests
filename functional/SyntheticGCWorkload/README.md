# A Multi-Threaded Tunable Synthetic GC Workload

IBM Center for Advanced Studies Atlantic
Sub-Project: Performance of Benchmarks
October, 2014

## 1 Motivation and Approach

Percolate collections and aborts are very costly occurrences in the J9 GC. Unfortunately they are sometimes seen on production systems running J9 and may lead to significant performance issues.

Due to the inherent difficulties in reproducing the real-world conditions under which these issues often occur, they can be very difficult to debug. It is therefore desirable to find an alternate means by which we can replicate similar conditions in a controlled test environment.

A synthetic workload was therefore devised in order to facilitate the debugging of these problems and the formulation of solutions. In order to find a simple set of parameters which can reproduce a given issue, a synthetic workload was devised, allowing the developer to specify a allocation and reference change patterns.

The results can then be monitored using an included verbose GC parser which monitors the effects of the workload on the garbage collector. Comparing these results to results from the customer’s system allows
the development of a workload that closely replicates the customer’s problem and allows the development of a targeted and tested solution.

## 2 Design

The Synthetic GC Workload provides a framework for the construction of custom allocation and reference change scenarios, specified by an XML configuration file. Each configuration specifies one or more workloads which run sequentially. Each workload has a hard runtime, after which it will terminate. Repetition is also
supported.

Payloads are specified within each workload, each having a size, a finite lifespan and constituting a certain proportion of allocations. Payloads also have a type and may be either arrays of certain primitives or instances of runtime-generate reflexive classes.

Payloads are grouped into payload sets, each set having a start time, an end time and a data rate. The data rate is divided among any directly nested payloads according to their proportion fields and is inherited by any subsets. Subsets may override any parameters specified in parent sets. Payloads may inherit certain
parameters from their parent set including their type.

## 3 Implementation

The implementation of the workload follows a producer-consumer design. An atomic linked list of payloads serves as a container. The list is walked at a fixed interval by a maintenance thread. The maintenance thread removes any expired payloads.

The producer side consists of a payer thread which distributes tokens to (or pays) builders. A producer thread pool performs the allocations themselves. Builders initially begin with zero balance in order to avoid any initial burst of allocation. Builders are paid based on the time elapsed during the last payment cycle.

#### 1


Cycle payment is adjusted by a small factor if the average allocation rate differs from the target rate.

Each builder will spawn one or more allocation tasks on the thread pool when its balance (the ”number” of tokens it possesses) is sufficient. This algorithm is similar to the token bucket-style algorithm, often used for network bandwidth allocation.

When the payment interval is small, the payment and allocation rates are consistent. Additionally, short maintenance intervals allow for more precise payload lifespans. Both come at the cost of additional CPU usage.

## 4 Usage

The majority of configuration is done via the XML configuration file, specified on the command line at runtime. Table 2 lists other command line options which may be specified at runtime.

Table 1 gives an example configuration file. This configuration contains one workload which runs for 10 minutes. It uses a 16-thread producer pool with a 100 item queue and a container that may grow to occupy all free space on the heap.

The payer thread and maintenance thread are both set to run at an interval of one microsecond, allowing for high resolution timing of allocation and reference changes. Output is printed by the status thread every 500ms, providing a running tally of payment rate, allocation rate, live-set size, free heap space and elapsed
time.

`<?xml version="1.0" encoding="ISO-8859-1"?>
<configuration maxDuration="15h" numRepetitions="1">
<workload duration="10m" numProducerThreads="16" workQueueSize="100"
containerSize="100%"
paymentPeriod="1us" maintenancePeriod="1us" statusUpdatePeriod="500ms"
numRepetitions="1">
<payloadSet startTime="0" endTime="20s" dataRate="20MB/s" payloadType="auto">
<payload proportionOfAllocation="50%" size="1kB" lifespan="1h" />
<payload proportionOfAllocation="50%" size="1kB" lifespan="10s" />
</payloadSet>
<payloadSet startTime="15s">
<payloadSet dataRate="75MB/s" payloadType="reflexive">
<payload proportionOfAllocation="100%" size="256B" lifespan="1ms" />
</payloadSet>
<payloadSet dataRate="30MB/s" payloadType="reflexive">
<payload proportionOfAllocation="80%" size="1.5kB" lifespan="10s" />
<payload proportionOfAllocation="20%" size="48MB" lifespan="10s" payloadType="byte_array" />
</payloadSet>
</payloadSet>
</workload>
</configuration>`

```
Table 1: An example workload configuration
```
#### 2


The workload in Table 1 contains 2 top level payloadSet tags. Payment to the first set starts at t=0 and runs for 20s. Payment to the second starts at 15s and runs until completion.

The first set also specifies the payloadType as auto. Auto will use runtime-generated reflexive payloads up to a predetermined point where their source is unreasonably long. For any larger payloads, one of the array types will be used.

```
Short Long Description Example
-l --logfile Specify a file to send workload output to --logfile log.txt
-s --silent Don’t print output to the console -ls log.txt
(Sends output to file instead of the console)
-h --help Print usage information
```
```
Table 2: Command line options supported by the Synthetic GC Workload
```
The second stop level set contains 2 subsets. The first allocates a small reflexive payload with a short lifespan at a high data rate. The second allocates a medium-sized and a very large payload at a moderate data rate. Note that the payloadType in the payload tag itself overrides that of the set. Sets can also be
further nested as needed.

### 4.1 Output

Table 3 provides an example of the output generated by running the configuration listed in Table 1 for a few minutes. The last line is continually updated throughout the run at the time interval specified by the statusUpdatePeriod parameter in the workload. It provides elapsed time, total average pay rate for active sets, average allocation rate, container usage, total throughput and free heap information. It also displays a readout of the number of queued allocation tasks.

For each payload, we can see additional information such as the period (time between two allocations), the peak live set size of that payload and the time at which it is expected to reach its peak size. For each set, we see the set’s expect peak usage and the time at which it is expected to peak.

It should be noted that the maintenance interval is taken into account when making these calculations, as a payload is not expected to live for less than one maintenance interval. There are, however, two cases where this may occur: first, when the container is full, an interrupt will trigger a maintenance cycle; second, if the payload expires before it can be added to the container, it will not be added but instead will be released prior to addition.

#### 3


#### =========================================================

J9 Synthetic Garbage Collection Workload
=========================================================
---------------------------------------------------
| BENCHMARK REPETITION | 1 of 1 |
| Free Heap | 1.75GB/2.00GB |
| Maximum duration | 15.0h |
---------------------------------------------------
| WORKLOAD | 1 of 1 |
| Duration | 10.0m |
| Container Size | 1.75GB |
| Producer threads | 16 |
| Work queue depth | 100 |
| Sataus update period | 500ms |
| Maintenance period | 1.00us |
| Pay period | 1.00us |
---------------------------------------------------
| PAYLOAD SET | 1 |
| Pay rate | 20.0MB/s(20480tr/s) |
| Expected peak usage | 300MB at 20.0s |
| Payment interval | 0-20.0s(20.0s) |
----------------------------------------------------------------------------------------
| ID | Type | % | Size | Lifespan | Period | Peak | ...at |
----------------------------------------------------------------------------------------
| 1 | REFLEXIVE | 50.0% | 1.00kB | 1.00h | 97.7us | 200MB | 20.0s |
| 2 | REFLEXIVE | 50.0% | 1.00kB | 10.0s | 97.7us | 100MB | 10.0s |
----------------------------------------------------------------------------------------
| PAYLOAD SET | 2.1 |
| Pay rate | 75.0MB/s(307200tr/s) |
| Expected peak usage | 77.0kB at 15.0s |
| Payment interval | 15.0s-10.0m(9.75m) |
----------------------------------------------------------------------------------------
| ID | Type | % | Size | Lifespan | Period | Peak | ...at |
----------------------------------------------------------------------------------------
| 1 | REFLEXIVE | 100% | 256B | 1.00ms | 3.26us | 77.0kB | 15.0s |
----------------------------------------------------------------------------------------
| PAYLOAD SET | 2.2 |
| Pay rate | 30.0MB/s(16384tr/s) |
| Expected peak usage | 336MB at 25.0s |
| Payment interval | 15.0s-10.0m(9.75m) |
----------------------------------------------------------------------------------------
| ID | Type | % | Size | Lifespan | Period | Peak | ...at |
----------------------------------------------------------------------------------------
| 1 | REFLEXIVE | 80.0% | 1.50kB | 10.0s | 61.0us | 240MB | 25.0s |
| 2 | BYTE_ARRAY | 20.0% | 48.0MB | 10.0s | 8.00s | 96.0MB | 25.0s |
----------------------------------------------------------------------------------------
| WORKLOAD REPETITION 1 of 1
-----------------------------------------------------------------------------
| | Pay | Alloca... | Container | Thro... | Free... | Q |
-----------------------------------------------------------------------------
| 6.29m | 105MB/s | 102MB/s | 506MB/1.75GB | 37.5GB | 665MB | 0 |

```
Table 3: An example run of the configuration given in Table 1
```
#### 4


