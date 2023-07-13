
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

# Yahoo! Cloud Serving Benchmark (YCSB)

YCSB is an [open-source](https://github.com/brianfrankcooper/YCSB/) database benchmark suite. AQA performance testing currently only supports YCSB with Azure Cosmos DB, but other databases may be added in the future.

## Running YCSB with Azure Cosmos DB

Refer to [the source](https://github.com/brianfrankcooper/YCSB/tree/master/azurecosmos) for an indepth explanation of running YCSB with Azure Cosmos DB, or follow the instructions below.

## Setup

This benchmark expects you to have pre-created the database "ycsb" and collection "usertable" before running the commands. When prompted for a Partition Key, use "id". For [Request Units (RUs)](https://learn.microsoft.com/en-us/azure/cosmos-db/request-units), select a value you want to benchmark. RUs are the measure of provisioned thoughput that Azure Cosmos DB defines. The higher the RUs, the more throughput you will get.

## Properties

Configuration parameters for the benchmark should be set in the azurecosmos.properties file, which should be created in the ycsb directory (the current directory). A template version of this file azurecosmos-template.properties is included for reference.

There are two required parameters for connecting to the database:

azurecosmos.uri=< uri string >
* Path to your Azure Cosmos DB account which can be obtained from the portal. It will look like the following: https://accountname.documents.azure.com:443/

azurecosmos.primaryKey=< key string >
* Obtained from the portal. The primary key is used to allow both read & write operations. If you are doing read-only workloads you can substitute the readonly key from the portal.

The other optional parameters and their default values are defined in the template configuration file azurecosmos-template.properties.