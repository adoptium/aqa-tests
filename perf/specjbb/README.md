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

## SPECjbb 2015

SPECjbb is a Java Client/Server performance testing benchmark. 
Please visit its product page for more information, including a user guide: https://www.spec.org/jbb2015/

## Prerequisites
Since SPECjbb requires a license to run, this aqa-test config requires that a licensed copy of the benchmark already exists on the machine that you are running on.

## Setup

Set the `SPECJBB_SRC` environment variable to the absolute path of your SPECjbb benchmark directory

```bash
export SPECJBB_SRC=/Users/aqa-tester/SPECjbb2015-1.03
```

## Run

This test target is part of the `dev` level, and the test target name is, `SPECjbb2015-multijvm-simple`. You can use the following commands to run it:

```bash
make _SPECjbb2015-multijvm-simple
```