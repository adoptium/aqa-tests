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
#

source $(dirname "$0")/test_base_functions.sh
#Set up Java to be used by the kafka test
echo_setup

TEST_TARGET="${1:-smoke}"

set -e
echo "Building kafka using gradle"
./gradlew -q jar
echo "Kafka Build - Completed"
set +e

if [ "$TEST_TARGET" = "full" ]; then
	echo "Running (ALL) Kafka tests :"
	./gradlew -q test
	test_exit_code=$?
	echo "Kafka tests - Completed"
	exit $test_exit_code
else
	echo "Probing Kafka"
	bin/kafka-run-class.sh kafka.Kafka
	test_exit_code=$?
	# kafka.Kafka exits 1 without a config file - class loaded successfully
	[ $test_exit_code -eq 1 ] && exit 0
	exit $test_exit_code
fi
