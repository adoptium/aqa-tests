#!/usr/bin/env bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Example script for running ./run_test.sh 
#
#
# Option 1: Run the test for each size:
# ./run_test.sh 1k
# ./run_test.sh 10k
# ./run_test.sh 100k
# ./run_test.sh 1M
# ./run_test.sh 10M
#
# OR 
#
# Option 2: Iterate over them:
#
for a in "1k" "10k" "100k" "1M" "10M"
do 
	./run_test.sh $a	
done 
#
# Success if the test exits with status 0 or prints "success". 
# Failure if it exits with status 1 or prints "failure". 
# And exception has occurred if it exits with status not equal to 0 or 1. 
#
