#!/bin/bash
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
######
if [ `uname` = AIX ]; then
  MAKE=gmake
else
  MAKE=make
fi

if [ "$#" -eq 1 ];then
	# temporarily removing DDR_Test for macos run
	rm -rf $1/functional/DDR_Test
	cd $1/TestConfig
	$MAKE -f run_configure.mk
	if [ $? -ne 0 ]; then
		exit 1
	fi
	$MAKE compile
else
	testDir=$1
	shift
	# check if TARGET is comma-separated list of targets
	# if so, parse and run each target, CUSTOMIZED_TEST_TARGET will be ignored
	if [[ $1 == *[,]* ]]
	then
  	echo "TARGET is list of sub_targets\n"
		subtargets=$(echo $1 | tr "," "\n")
		for sub_target in $subtargets
		do
    	echo "> [$sub_target]"
			$MAKE -C $testDir -f autoGen.mk $sub_target
		done
	else
	  echo "Normal setup, no multiple targets passed"
		$MAKE -C $testDir -f autoGen.mk $@
	fi
fi

