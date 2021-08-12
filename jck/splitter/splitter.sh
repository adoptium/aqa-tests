#!/bin/bash
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

split() 
{ 
	path=$1
	numOfGrps=$2
	
	if [[ $numOfGrps == 0 ]]; then
		echo "Too few sub-tests to divide. Please use this script for tests that have a set of sub-tests > 20"
		return; 
	fi 
	
	testPathPrefix=$3
    numOfIndices=numOfGrps+1; 
    
	# Read in the sub-tests into an array 
	
	i=0
	while read line
	do
		subTests[$i]="$line"        
	    ((i++))
	done < <(ls $path)
	
	totalTests=${#subTests[@]}
	minGrpSize=totalTests/numOfGrps 
	totalCounted=0
	
	# Prepare indices where to split the set of sub-tests  
	
	indices[0]=0
	for ((i=1; i<($numOfIndices-1); i++));
	do
	    indices[$i]=$((${indices[$i-1]}+$minGrpSize))
	done
	indices[$numOfIndices-1]=$totalTests

	# Split the sub-tests into groups 
	
	for ((i=0; i<$numOfGrps; i++));
	do 
	   for ((j=${indices[$i]}; j<${indices[$i+1]}; j++));
	   do 
	      if [[ ${subTests[$j]} != "index.html" ]]; then 
	         groups[$i]+="$testPathPrefix"/"${subTests[$j]};"
			 totalCounted=$(($totalCounted+1))
	      fi 
	   done
	done
	
	# Verify that we included all sub-tests 
	
	success=1
	for ((i=0; i<$totalTests; i++));
	do
		aTest=${subTests[$i]}
		found=0
		for ((j=0; j<$numOfGrps; j++)); 
		do
			if [[ ${groups[$j]} == *"$aTest"* ]]; then
				found=1 
				break
			fi
		done
		if [[ $found == 0 ]]; then  
			if [[ $aTest != "index.html" ]]; then 
				echo "Uncounted : $aTest"
				success=0
				break
			fi
		fi
	done
	
	if [[ $success == 1 ]]; then  
		for ((i=0; i<$numOfGrps; i++));
		do
			value=${groups[$i]}
			printf "%s\n\n" "GROUP ($i) = ${value%?}"
		done
		printf "%s\n\n" "Total subtests processed = $totalCounted"
	else 
		echo "There was some error in splitting the test. Please check your input and try again.."
	fi
}

targetTestPath=$1 
numOfGrps=$2
pathPrefix=$(echo "$1" | rev | cut -d"/" -f1 -f2 | rev)

split $targetTestPath $numOfGrps $pathPrefix

