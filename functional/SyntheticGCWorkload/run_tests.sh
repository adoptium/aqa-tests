#!/bin/bash
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