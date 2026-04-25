---
layout: default
title: Adding AQAvit Support for a New Platform
parent: Tutorials
nav_order: 1
---

<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Adding AQAvit Support for a New Platform

## Basic changes (required)

If the platform is quite similar to other platforms, this is a relatively straight-forward task. This will cover basic running of many tests. Changes need to be made in the following files:

1. [aqa-tests/buildenv/jenkins/openjdk_tests](https://github.com/adoptium/aqa-tests/blob/master/buildenv/jenkins/openjdk_tests) file, by adding the new platform to the PLATFORM_MAP (see this [PR](https://github.com/adoptium/aqa-tests/pull/3670) as an example)
2. [TKG/resources/buildPlatformMap.properties](https://github.com/adoptium/TKG/blob/master/resources/buildPlatformMap.properties) (see [PR](https://github.com/adoptium/TKG/pull/322) as example)

## Potential other changes (possibly optional)

### Machine information detection

TKG queries the machine for information about the platform and resources available. If there are warnings during setup relating to CPU detection or other queries, you may have to make updates to [MachineInfo.java](https://github.com/adoptium/TKG/blob/master/src/org/openj9/envInfo/MachineInfo.java) (see [PR](https://github.com/adoptium/TKG/pull/211) as an example).

### Compiling native test material

For compiling native test material (which exists in openjdk and system test material), a few additional changes may be needed, if the platform needs specific compiler args or configuration.

TODO: share links to PRs for examples of these changes (in aqa-systemtest/STF), essentially looking at other similar changes to native makefiles, such as [openjdk.test.modularity/src/tests/com.test.jlink/native/makefile](https://github.com/adoptium/aqa-systemtest/blob/master/openjdk.test.modularity/src/tests/com.test.jlink/native/makefile). See also [STF PR](https://github.com/adoptium/STF/pull/117).

## Other Complications

See [https://github.com/adoptium/aqa-tests/issues/2224](https://github.com/adoptium/aqa-tests/issues/2224) for an example of an unsolved 'complication'.
