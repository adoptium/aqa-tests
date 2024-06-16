# AQA jcstress runner

This playlist runs by default all jcstress tests in minimal `time budget`.  Currently  it is just `-tb 1h` which is trying to squeeze all 11543 tests from jcstress-20240222 to 1hour.  That is not going to work, but the suite should try to squeeze itself to absolute minimum. Another switch which can minimize runtime is `-c` - number of cores to use, but that was not recommended by upstream.   

Other targets are generated subgroups to run targeted groups of tests in case that the affected area could do cause an issue.  Dont forget you have to prefix such target by `disabled...` keyword. All those targets are run with all cores and no forced time budget. 

Both main tests and generated subgroups can take `$JC_TIME_BUDGET` variable to set the time budget and `$JC_CORES` to set number of cores.  In addition standard `$APPLICATION_OPTIONS` is honoured.

# AQA jcstress playlist generator and tester
The generator is slightly over-engineered but do its job quite well. It takes only one argument - the file to jcstress.jar. The name of file maters - it is used in the command.

**It is necessary that default usage of the generator always prints the playlist.xml as it shuold be**

Except the generation the class can also print final standalone statistics or regexes for future research. It can also calculate estimated time of all groups by forking and killing jcstress jar (of course this feature is useless with `-tb`). It can also run all the jcstress groups, and calculate precise times. Current implementation looks like:
<details>
<summary>Setup</summary>
<pre>
Limit is 100; no group with more then 100 of tests should be merged to bigger ones. Exclude list is of length of 4
Small groups will not be created. Intentional?
Huge groups will NOT be split to more subsets. Intentional?
Max count of natural grouping iterations is 3
Only N from FQN will be used. This saves space, but risks duplicate matches
Cores limit for final playlist is not used
Time budget is not used. Intentional?
Output is set TEST
Total test cases: 11543
total tests files: 4374
Natural groups round 1 : 251
Natural groups round 2 : 112
Natural groups round 3 : 82
</pre>
</details>
<details>
<summary>Times</summary>
<pre>
Results gathered: 82 of expected 82; 100% time of longest group, n% time of ideal group from really run results
org.openjdk.jcstress.tests.seqcst.volatiles with 2131tests took 3928320s [45d+11:12:00] (100%)(+3494%)
org.openjdk.jcstress.tests.seqcst.sync with 2131tests took 3928320s [45d+11:12:00] (100%)(+3494%)
org.openjdk.jcstress.tests.volatiles with 39tests took 71135s [0d+19:45:35] (1%)(-35%)
org.openjdk.jcstress.tests.locks.stamped.StampedLockPairwiseTests with 450tests took 64799s [0d+17:59:59] (1%)(-41%)
org.openjdk.jcstress.tests.causality with 43tests took 42911s [0d+11:55:11] (1%)(-61%)
org.openjdk.jcstress.tests.fences.varHandles with 196tests took 28223s [0d+07:50:23] (0%)(-75%)
org.openjdk.jcstress.tests.memeffects.basic.atomicupdaters.AtomicIntegerFieldUpdater with 192tests took 27648s [0d+07:40:48] (0%)(-75%)
org.openjdk.jcstress.tests.memeffects.basic.atomicupdaters.AtomicLongFieldUpdater with 192tests took 27647s [0d+07:40:47] (0%)(-75%)
org.openjdk.jcstress.tests.memeffects.basic.atomic.AtomicLong with 192tests took 27647s [0d+07:40:47] (0%)(-75%)
org.openjdk.jcstress.tests.memeffects.basic.atomic.AtomicInteger with 192tests took 27647s [0d+07:40:47] (0%)(-75%)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.heap.little with 168tests took 24192s [0d+06:43:12] (0%)(-78%)
org.openjdk.jcstress.tests.acqrel.varHandles.byteArray.big with 168tests took 24192s [0d+06:43:12] (0%)(-78%)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.heap.big with 168tests took 24191s [0d+06:43:11] (0%)(-78%)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.direct.little with 168tests took 24191s [0d+06:43:11] (0%)(-78%)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.direct.big with 168tests took 24191s [0d+06:43:11] (0%)(-78%)
org.openjdk.jcstress.tests.acqrel.varHandles.byteArray.little with 168tests took 24191s [0d+06:43:11] (0%)(-78%)
org.openjdk.jcstress.tests.locks.mutex with 160tests took 23039s [0d+06:23:59] (0%)(-79%)
org.openjdk.jcstress.tests.memeffects.basic with 156tests took 22463s [0d+06:14:23] (0%)(-80%)
org.openjdk.jcstress.tests.oota with 13tests took 19440s [0d+05:24:00] (0%)(-83%)
org.openjdk.jcstress.tests.acqrel.varHandles.fields.volatiles with 126tests took 18144s [0d+05:02:24] (0%)(-84%)
org.openjdk.jcstress.tests.acqrel.varHandles.arrays.volatiles with 126tests took 18144s [0d+05:02:24] (0%)(-84%)
org.openjdk.jcstress.tests.acqrel.varHandles.arrays.acqrel with 126tests took 18144s [0d+05:02:24] (0%)(-84%)
org.openjdk.jcstress.tests.acqrel.fields.volatiles with 126tests took 18144s [0d+05:02:24] (0%)(-84%)
org.openjdk.jcstress.tests.acqrel.fields.sync with 126tests took 18144s [0d+05:02:24] (0%)(-84%)
org.openjdk.jcstress.tests.atomicity with 126tests took 18143s [0d+05:02:23] (0%)(-84%)
org.openjdk.jcstress.tests.acqrel.varHandles.fields.acqrel with 126tests took 18143s [0d+05:02:23] (0%)(-84%)
org.openjdk.jcstress.tests.atomics.longs.AtomicLongFieldUpdaterPairwiseTests with 111tests took 15840s [0d+04:24:00] (0%)(-86%)
org.openjdk.jcstress.tests.atomics.integer.AtomicIntegerPairwiseTests with 111tests took 15840s [0d+04:24:00] (0%)(-86%)
org.openjdk.jcstress.tests.atomics.integer.AtomicIntegerArrayPairwiseTests with 111tests took 15840s [0d+04:24:00] (0%)(-86%)
org.openjdk.jcstress.tests.atomics.longs.AtomicLongPairwiseTests with 111tests took 15839s [0d+04:23:59] (0%)(-86%)
org.openjdk.jcstress.tests.atomics.longs.AtomicLongArrayPairwiseTests with 111tests took 15839s [0d+04:23:59] (0%)(-86%)
org.openjdk.jcstress.tests.atomics.integer.AtomicIntegerFieldUpdaterPairwiseTests with 111tests took 15839s [0d+04:23:59] (0%)(-86%)
org.openjdk.jcstress.tests.atomicity.varHandles.byteBuffer.heap with 162tests took 15552s [0d+04:19:12] (0%)(-86%)
org.openjdk.jcstress.tests.atomicity.varHandles.byteBuffer.direct with 162tests took 15552s [0d+04:19:12] (0%)(-86%)
org.openjdk.jcstress.tests.atomicity.varHandles.byteArray with 162tests took 15551s [0d+04:19:11] (0%)(-86%)
org.openjdk.jcstress.tests.coherence.varHandles with 108tests took 15551s [0d+04:19:11] (0%)(-86%)
org.openjdk.jcstress.tests.accessAtomic.varHandles with 108tests took 15551s [0d+04:19:11] (0%)(-86%)
org.openjdk.jcstress.tests.atomicity.varHandles.arrays with 141tests took 13536s [0d+03:45:36] (0%)(-88%)
org.openjdk.jcstress.tests.atomicity.varHandles.fields with 141tests took 13535s [0d+03:45:35] (0%)(-88%)
org.openjdk.jcstress.tests.atomicity.varHandles.fields.WeakCASContendStrongTest with 108tests took 10368s [0d+02:52:48] (0%)(-91%)
org.openjdk.jcstress.tests.atomicity.varHandles.arrays.WeakCASTest with 108tests took 10368s [0d+02:52:48] (0%)(-91%)
org.openjdk.jcstress.tests.accessAtomic.varHandles.byteBuffer.heap with 72tests took 10368s [0d+02:52:48] (0%)(-91%)
org.openjdk.jcstress.tests.accessAtomic.varHandles.byteArray with 72tests took 10368s [0d+02:52:48] (0%)(-91%)
org.openjdk.jcstress.tests.atomicity.varHandles.fields.WeakCASTest with 108tests took 10367s [0d+02:52:47] (0%)(-91%)
org.openjdk.jcstress.tests.atomicity.varHandles.arrays.WeakCASContendStrongTest with 108tests took 10367s [0d+02:52:47] (0%)(-91%)
org.openjdk.jcstress.tests.coherence.varHandles.byteBuffer.heap with 72tests took 10367s [0d+02:52:47] (0%)(-91%)
org.openjdk.jcstress.tests.coherence.varHandles.byteBuffer.direct with 72tests took 10367s [0d+02:52:47] (0%)(-91%)
org.openjdk.jcstress.tests.coherence.varHandles.byteArray with 72tests took 10367s [0d+02:52:47] (0%)(-91%)
org.openjdk.jcstress.tests.accessAtomic.varHandles.byteBuffer.direct with 72tests took 10367s [0d+02:52:47] (0%)(-91%)
org.openjdk.jcstress.tests.countdownlatch with 24tests took 9791s [0d+02:43:11] (0%)(-92%)
org.openjdk.jcstress.tests.tearing with 87tests took 8352s [0d+02:19:12] (0%)(-93%)
org.openjdk.jcstress.tests.copy.manual.arrays with 56tests took 8064s [0d+02:14:24] (0%)(-93%)
org.openjdk.jcstress.tests.copy.arraycopy.arrays with 56tests took 8064s [0d+02:14:24] (0%)(-93%)
org.openjdk.jcstress.tests.copy.copyof.arrays with 56tests took 8063s [0d+02:14:23] (0%)(-93%)
org.openjdk.jcstress.tests.copy.clone.arrays with 56tests took 8063s [0d+02:14:23] (0%)(-93%)
org.openjdk.jcstress.tests.coherence with 54tests took 7776s [0d+02:09:36] (0%)(-93%)
org.openjdk.jcstress.tests.accessAtomic with 54tests took 7775s [0d+02:09:35] (0%)(-93%)
org.openjdk.jcstress.tests.init with 52tests took 7488s [0d+02:04:48] (0%)(-94%)
org.openjdk.jcstress.tests.initClass.arrays with 36tests took 5184s [0d+01:26:24] (0%)(-96%)
org.openjdk.jcstress.tests.init.arrays with 36tests took 5184s [0d+01:26:24] (0%)(-96%)
org.openjdk.jcstress.tests.defaultValues with 36tests took 5184s [0d+01:26:24] (0%)(-96%)
org.openjdk.jcstress.tests.tearing.arrays with 54tests took 5183s [0d+01:26:23] (0%)(-96%)
org.openjdk.jcstress.tests.accessAtomic.fields with 54tests took 5183s [0d+01:26:23] (0%)(-96%)
org.openjdk.jcstress.tests.locks with 36tests took 5183s [0d+01:26:23] (0%)(-96%)
org.openjdk.jcstress.tests.initLen.arrays with 36tests took 5183s [0d+01:26:23] (0%)(-96%)
org.openjdk.jcstress.tests.initClass with 36tests took 5183s [0d+01:26:23] (0%)(-96%)
org.openjdk.jcstress.tests.defaultValues.arrays with 36tests took 5183s [0d+01:26:23] (0%)(-96%)
org.openjdk.jcstress.tests.singletons with 28tests took 4032s [0d+01:07:12] (0%)(-97%)
org.openjdk.jcstress.tests.copy.clone with 28tests took 4032s [0d+01:07:12] (0%)(-97%)
org.openjdk.jcstress.tests.copy.manual with 28tests took 4031s [0d+01:07:11] (0%)(-97%)
org.openjdk.jcstress.tests.atomics with 28tests took 3743s [0d+01:02:23] (0%)(-97%)
org.openjdk.jcstress.tests.fences with 16tests took 2303s [0d+00:38:23] (0%)(-98%)
org.openjdk.jcstress.tests.strings with 17tests took 2016s [0d+00:33:36] (0%)(-99%)
org.openjdk.jcstress.tests.unsafe with 10tests took 1439s [0d+00:23:59] (0%)(-99%)
org.openjdk.jcstress.tests.varhandles with 6tests took 864s [0d+00:14:24] (0%)(-100%)
org.openjdk.jcstress.tests.executors with 6tests took 576s [0d+00:09:36] (0%)(-100%)
org.openjdk.jcstress.tests.future with 5tests took 575s [0d+00:09:35] (0%)(-100%)
org.openjdk.jcstress.tests.interrupt with 15tests took 540s [0d+00:09:00] (0%)(-100%)
org.openjdk.jcstress.tests.sample with 3tests took 288s [0d+00:04:48] (0%)(-100%)
org.openjdk.jcstress.tests.collections with 3tests took 287s [0d+00:04:47] (0%)(-100%)
org.openjdk.jcstress.tests.threadlocal with 2tests took 287s [0d+00:04:47] (0%)(-100%)
org.openjdk.jcstress.tests.mxbeans with 2tests took 287s [0d+00:04:47] (0%)(-100%)
Total time: 149338 minutes [103d+16:58:38]
Ideal avg time: 1821 minutes [1d+06:21:12] (100%)
Max seen  time: 65472 minutes [45d+11:12:00] (3594%)
Min seen  time: 4 minutes [0d+00:04:47] (0%)
Avg differecne from longest: 2%
Avg differecne from ideal: -70%
</pre>
</details>
<details>
<summary>Number of tests</summary>
<pre>
org.openjdk.jcstress.tests.mxbeans: classes 1/tests 2(ac/ar:2/0)
org.openjdk.jcstress.tests.threadlocal: classes 1/tests 2(ac/ar:2/0)
org.openjdk.jcstress.tests.collections: classes 1/tests 3(ac/ar:2/1)
org.openjdk.jcstress.tests.sample: classes 1/tests 3(ac/ar:2/1)
org.openjdk.jcstress.tests.future: classes 2/tests 5(ac/ar:4/1)
org.openjdk.jcstress.tests.executors: classes 2/tests 6(ac/ar:4/2)
org.openjdk.jcstress.tests.varhandles: classes 3/tests 6(ac/ar:6/0)
org.openjdk.jcstress.tests.unsafe: classes 5/tests 10(ac/ar:10/0)
org.openjdk.jcstress.tests.oota: classes 5/tests 13(ac/ar:13/0)
org.openjdk.jcstress.tests.interrupt: classes 15/tests 15(ac/ar:15/0)
org.openjdk.jcstress.tests.fences: classes 8/tests 16(ac/ar:16/0)
org.openjdk.jcstress.tests.strings: classes 7/tests 17(ac/ar:14/3)
org.openjdk.jcstress.tests.countdownlatch: classes 10/tests 24(ac/ar:24/0)
org.openjdk.jcstress.tests.atomics: classes 13/tests 28(ac/ar:26/2)
org.openjdk.jcstress.tests.copy.clone: classes 14/tests 28(ac/ar:28/0)
org.openjdk.jcstress.tests.copy.manual: classes 14/tests 28(ac/ar:28/0)
org.openjdk.jcstress.tests.singletons: classes 14/tests 28(ac/ar:28/0)
org.openjdk.jcstress.tests.defaultValues: classes 18/tests 36(ac/ar:36/0)
org.openjdk.jcstress.tests.defaultValues.arrays: classes 18/tests 36(ac/ar:36/0)
org.openjdk.jcstress.tests.init.arrays: classes 18/tests 36(ac/ar:36/0)
org.openjdk.jcstress.tests.initClass: classes 18/tests 36(ac/ar:36/0)
org.openjdk.jcstress.tests.initClass.arrays: classes 18/tests 36(ac/ar:36/0)
org.openjdk.jcstress.tests.initLen.arrays: classes 18/tests 36(ac/ar:36/0)
org.openjdk.jcstress.tests.locks: classes 18/tests 36(ac/ar:36/0)
org.openjdk.jcstress.tests.volatiles: classes 15/tests 39(ac/ar:39/0)
org.openjdk.jcstress.tests.causality: classes 18/tests 43(ac/ar:43/0)
org.openjdk.jcstress.tests.init: classes 26/tests 52(ac/ar:52/0)
org.openjdk.jcstress.tests.accessAtomic: classes 27/tests 54(ac/ar:54/0)
org.openjdk.jcstress.tests.accessAtomic.fields: classes 18/tests 54(ac/ar:36/18)
org.openjdk.jcstress.tests.coherence: classes 27/tests 54(ac/ar:54/0)
org.openjdk.jcstress.tests.tearing.arrays: classes 18/tests 54(ac/ar:36/18)
org.openjdk.jcstress.tests.copy.arraycopy.arrays: classes 28/tests 56(ac/ar:56/0)
org.openjdk.jcstress.tests.copy.clone.arrays: classes 28/tests 56(ac/ar:56/0)
org.openjdk.jcstress.tests.copy.copyof.arrays: classes 28/tests 56(ac/ar:56/0)
org.openjdk.jcstress.tests.copy.manual.arrays: classes 28/tests 56(ac/ar:56/0)
org.openjdk.jcstress.tests.accessAtomic.varHandles.byteArray: classes 36/tests 72(ac/ar:72/0)
org.openjdk.jcstress.tests.accessAtomic.varHandles.byteBuffer.direct: classes 36/tests 72(ac/ar:72/0)
org.openjdk.jcstress.tests.accessAtomic.varHandles.byteBuffer.heap: classes 36/tests 72(ac/ar:72/0)
org.openjdk.jcstress.tests.coherence.varHandles.byteArray: classes 36/tests 72(ac/ar:72/0)
org.openjdk.jcstress.tests.coherence.varHandles.byteBuffer.direct: classes 36/tests 72(ac/ar:72/0)
org.openjdk.jcstress.tests.coherence.varHandles.byteBuffer.heap: classes 36/tests 72(ac/ar:72/0)
org.openjdk.jcstress.tests.tearing: classes 29/tests 87(ac/ar:58/29)
org.openjdk.jcstress.tests.accessAtomic.varHandles: classes 54/tests 108(ac/ar:108/0)
org.openjdk.jcstress.tests.atomicity.varHandles.arrays.WeakCASContendStrongTest: classes 36/tests 108(ac/ar:72/36)
org.openjdk.jcstress.tests.atomicity.varHandles.arrays.WeakCASTest: classes 36/tests 108(ac/ar:72/36)
org.openjdk.jcstress.tests.atomicity.varHandles.fields.WeakCASContendStrongTest: classes 36/tests 108(ac/ar:72/36)
org.openjdk.jcstress.tests.atomicity.varHandles.fields.WeakCASTest: classes 36/tests 108(ac/ar:72/36)
org.openjdk.jcstress.tests.coherence.varHandles: classes 54/tests 108(ac/ar:108/0)
org.openjdk.jcstress.tests.atomics.integer.AtomicIntegerArrayPairwiseTests: classes 55/tests 111(ac/ar:110/1)
org.openjdk.jcstress.tests.atomics.integer.AtomicIntegerFieldUpdaterPairwiseTests: classes 55/tests 111(ac/ar:110/1)
org.openjdk.jcstress.tests.atomics.integer.AtomicIntegerPairwiseTests: classes 55/tests 111(ac/ar:110/1)
org.openjdk.jcstress.tests.atomics.longs.AtomicLongArrayPairwiseTests: classes 55/tests 111(ac/ar:110/1)
org.openjdk.jcstress.tests.atomics.longs.AtomicLongFieldUpdaterPairwiseTests: classes 55/tests 111(ac/ar:110/1)
org.openjdk.jcstress.tests.atomics.longs.AtomicLongPairwiseTests: classes 55/tests 111(ac/ar:110/1)
org.openjdk.jcstress.tests.acqrel.fields.sync: classes 63/tests 126(ac/ar:126/0)
org.openjdk.jcstress.tests.acqrel.fields.volatiles: classes 63/tests 126(ac/ar:126/0)
org.openjdk.jcstress.tests.acqrel.varHandles.arrays.acqrel: classes 63/tests 126(ac/ar:126/0)
org.openjdk.jcstress.tests.acqrel.varHandles.arrays.volatiles: classes 63/tests 126(ac/ar:126/0)
org.openjdk.jcstress.tests.acqrel.varHandles.fields.acqrel: classes 63/tests 126(ac/ar:126/0)
org.openjdk.jcstress.tests.acqrel.varHandles.fields.volatiles: classes 63/tests 126(ac/ar:126/0)
org.openjdk.jcstress.tests.atomicity: classes 63/tests 126(ac/ar:126/0)
org.openjdk.jcstress.tests.atomicity.varHandles.arrays: classes 47/tests 141(ac/ar:94/47)
org.openjdk.jcstress.tests.atomicity.varHandles.fields: classes 47/tests 141(ac/ar:94/47)
org.openjdk.jcstress.tests.memeffects.basic: classes 78/tests 156(ac/ar:156/0)
org.openjdk.jcstress.tests.locks.mutex: classes 80/tests 160(ac/ar:160/0)
org.openjdk.jcstress.tests.atomicity.varHandles.byteArray: classes 54/tests 162(ac/ar:108/54)
org.openjdk.jcstress.tests.atomicity.varHandles.byteBuffer.direct: classes 54/tests 162(ac/ar:108/54)
org.openjdk.jcstress.tests.atomicity.varHandles.byteBuffer.heap: classes 54/tests 162(ac/ar:108/54)
org.openjdk.jcstress.tests.acqrel.varHandles.byteArray.big: classes 84/tests 168(ac/ar:168/0)
org.openjdk.jcstress.tests.acqrel.varHandles.byteArray.little: classes 84/tests 168(ac/ar:168/0)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.direct.big: classes 84/tests 168(ac/ar:168/0)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.direct.little: classes 84/tests 168(ac/ar:168/0)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.heap.big: classes 84/tests 168(ac/ar:168/0)
org.openjdk.jcstress.tests.acqrel.varHandles.byteBuffer.heap.little: classes 84/tests 168(ac/ar:168/0)
org.openjdk.jcstress.tests.memeffects.basic.atomic.AtomicInteger: classes 96/tests 192(ac/ar:192/0)
org.openjdk.jcstress.tests.memeffects.basic.atomic.AtomicLong: classes 96/tests 192(ac/ar:192/0)
org.openjdk.jcstress.tests.memeffects.basic.atomicupdaters.AtomicIntegerFieldUpdater: classes 96/tests 192(ac/ar:192/0)
org.openjdk.jcstress.tests.memeffects.basic.atomicupdaters.AtomicLongFieldUpdater: classes 96/tests 192(ac/ar:192/0)
org.openjdk.jcstress.tests.fences.varHandles: classes 98/tests 196(ac/ar:196/0)
org.openjdk.jcstress.tests.locks.stamped.StampedLockPairwiseTests: classes 225/tests 450(ac/ar:450/0)
org.openjdk.jcstress.tests.seqcst.sync: classes 489/tests 2131(ac/ar:1642/489)
org.openjdk.jcstress.tests.seqcst.volatiles: classes 489/tests 2131(ac/ar:1642/489)
</pre>
</details>

*Each rutime generator is run, it first verify, that all tests will be run, and that each will be run only once.*

## Control variables
In order of importance and reasonability
 * OUTPUT - one of  
   * do - will run all groups as jcstress (takes ages!).  If interrupted, will print at least what was calculated up to that time
   * test - will fork and kill `java jcstress.jar` and estimate time of all groups. If interrupted, will print at least what was calculated up to that time
   * stats - will print amount of tests in each group
   * regexes - will print just final regexes
   * **generate - default, will print playlist.xml to stdout**
 * LIMIT - number. **default is 100**. Every group smaller then LIMIT will be merged to bigger subset.
 * SMALL_GROUPS - true/false. **default is false**. After natural grouping is done, all remaining groups smaller then LIMIT are merged to artificial groups
 * SPLIT_BIG_BASES - true/false. **default is false**. Each natural  group group bigger then LIMIT is split once it reaches limit.
 * MAX_NATURAL_ITERATIONS - number, usually 1-10, how many namespaces can be cut for natural grouping.
 <details>
<summary>eg:</summary>
<pre>
org.openjdk.jcstress.tests.atomicity.varHandles.arrays.WeakCASContendStrongTest
org.openjdk.jcstress.tests.atomicity.varHandles.arrays.WeakCASTest
org.openjdk.jcstress.tests.atomicity.varHandles.fields.WeakCASContendStrongTest
org.openjdk.jcstress.tests.atomicity.varHandles.fields.WeakCASTest:

Are not mixed, because MAX_NATURAL_ITERATIONS was 3. If it would be 4, one more level would be  cut (if LIMIT allows), to:
org.openjdk.jcstress.tests.atomicity.varHandles.arrays
org.openjdk.jcstress.tests.atomicity.varHandles.fields

If it would be 5, and LIMIT would allow, it would cut one more: to
org.openjdk.jcstress.tests.atomicity.varHandles
</pre>
</details>

 * CORES - number, **null by default (thus all cores)**. Number of cores used to generate list and and run jcstress.
 * TIME_BUDGET - time budget, **null by default (thus no limit)**. Applies only to generated groups. The default target `all` have hardcoded value
 * FQN - **false by default**. If true, no prefix would be cut from tests in `|` expressions
 * SPLIT_ALL - **true by default**, if false, some hardcoded groups, which are known to have a lot of tests, but run surprisingly quickly are excluded from splitting if `SPLIT_BIG_BASES` is true.
 * VERBOSE - prints some debugging info. **false by default**. Enable byu setting to true.


 It is obvious, that the `Generate.java` can create pretty balanced groups by number of tests, but it proved to be not exactly useful, as single jcsrtress tests do not have constant duration. The `-tb` is usually not honoured, it is common to exceed it by 200% but also by 20000% - https://bugs.openjdk.org/browse/CODETOOLS-7903750

 Algorithm:
  * the natural groups are finished first. They are creating by grouping by packages.
    * this runs in iterations, and by defualt breaks when iteration merges nothing
    * if `MAX_NATURAL_ITERATIONS` is enabled, then it stops merging after MAX_NATURAL_ITERATIONS of iterations.
    * each group with less then `LIMIT` of members is merged to another with shares namesapce
    * if `SPLIT_BIG_BASES` is enabled, then each namespaces will be split so it do not exceed LIMIT.
  * if `SMALL_GROUPS` are enabled then all leftovers smaller then LIMIT are merged to unnatural groups at the end.
    * Again, once this artificial group reaches limit no more tests is added 

Before this playlist was created, many experiments were run:
<details>
<pre>

 1 core : Total time: 9 minutes [0d+00:09:35]
 2 cores: Total time: 16458 minutes [11d+10:18:34]
 3 cores: Total time: 33695 minutes [23d+09:35:21]
 4 cores: Total time: 149339 minutes [103d+16:59:21]
 8 cores: Total time: 149339 minutes [103d+16:59:20] 

 split_exl Limit 10 - 603 groups, from those  7 "small groups" (0.5hours each. %like longest/ideal %17%/? (6m-2.5h)
 split_all Limit 10 - 603 groups, from those  7 "small groups" (0.5hours each. %like longest/ideal %68%/81 (26m-38m)
 split_exl Limit 50 - 128 groups, from those  6 "small groups" (~2.5hhours each. %like longest/ideal %60/85% (45m-3.5h)
 split_all Limit 50 - 206 groups, from those  7 "small groups" (~1.1 hours each. %like longest/ideal %37/27% (6s-3.5h)   
   (there was an error (eg for rg.openjdk.jcstress.tests.seqcst.sync-028) 3 actors:   No scheduling is possible, these tests would not run. Which I need to investiagte and maybe fall back to simple more simple class counting, or run also the -l listing with -c (which seems most correct, as -l is indeed counting with -c)
 the real min time would be some 1hour.
 split_exl Limit 100 - 60 groups, from those  7 "small groups" (~4.5hours each. %like longest/ideal %63/79% (2.5h-7h)
 split_all Limit 100 - 99 groups, from those  7 "small groups" (~2.5hours each . %like longest/ideal %38/21% (7s-7h)
   (same error, so real min time would be again some 2.5 hours)
 
  The estimated times are highly CORES sensitive. Some tests do not even run with CORES=1!
  Some groups, eg org.openjdk.jcstress.tests.seqcst.sync and org.openjdk.jcstress.tests.seqcst.volatiles are highly affected by cores (2->2hours 4=>45days!)
  other groups are slightly less affected by cores, but still are.
  
  This table was genrated with CORES=2 in TEST mode (thus with aprox 75% accuracy).
  jcstress20240202  4400classes with 11500 tests.
  Note, that `%like longest/ideal` is better closer to bigger/bigger.
  all: 2 cores and org.openjdk.jcstress.tests.seqcst.sync and org.openjdk.jcstress.tests.seqcst.volatiles not split:
  all: MAX_NATURAL_ITERATIONS=Integer.max_value SMALL_GROUPS=true SPLIT_BIG_BASES=true
  Limit 5 - 1207 groups, from those  8 "small groups" (not tried... yet... to long...)
  split_exl Limit 10 - 603 groups, from those  7 "small groups" (0.5hours each. %like longest/ideal %17%/? (6m-2.5h)
  split_all Limit 10 - 603 groups, from those  7 "small groups" (0.5hours each. %like longest/ideal %68%/81? (26m-38mm)
  Limit 50 - 128 groups, from those  6 "small groups" (~2.5hhours each. %like longest/ideal %60/85% (45m-3.5h)
  Limit 100 - 60 groups, from those  7 "small groups" (~4.5hours each. %like longest/ideal %63/79% (2.5h-7h)
  Limit 250 - 25 groups, from those  4 "small groups" (~11hours each. %like longest/ideal 63%/77% (2.5h-17h)
  Limit 500 - 14 groups, from those  4 "small groups" (~20hours each. %like longest/ideal 59%/60% (1.5h-1d 9h)
  Limit 1000 - 9 groups, from those  5 "small groups" (~1day 6hours each. %like longest/ideal 42%/41% (2.5h-3d)
  Limit 2000 - 6 groups, from those  4 "small groups" (~2day each, %like longest/ideal 41%/9% (2.5h-4d)
  Limit 5000 - 3 groups, from those  3 "small groups" (unknown, selector argument to long for one of groups)
  Limit 50000 - 1 groups, from those 1 "small groups" (unknown, selector argument to long)
  all tests in batch ~11.5 of day
  The minimal 2.5 which is invalidating huge groups a bit, are  the two excluded gorg.openjdk.jcstress.tests.seqcst.sync and org.openjdk.jcstress.tests.seqcst.volatiles,
 
  Note, that LIMIT is not strictly honored. It is jsut saying, that if there LIMIT of testes or more, it wil not be grouped.
  So in worse scenario, LIMIT-1+LIMIT-1 will join to group of size of (2*LIMIT)-2, but it is very rare,
  and in addition the time of one test is very far from being constant, so this deviation in size of grtoup (LIMIT+1, <2*LIMIT)-2> is minimal.
  If small groups are enagetOutputSbled, and they should be, there wil nearly always be some leftover group with size <= LIMIT
</pre>
</details>

So any regenerator, be warned, it seems it is impossible to group jcstress tests in really balanced way. In addition, it omitting any si a no go, all jcstress test are equally important.