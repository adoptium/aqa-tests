#!/bin/sh
################################################################################
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
################################################################################

BASE=`dirname $0`

CP="-cp ${BASE}/codepoint.jar"

java ${CP} UnicodeDataTest ${BASE}/data/UnicodeData-10.0.0.txt 2> err.txt
java ${CP} UnihanCodePoint ${BASE}/data/UnicodeData-10.0.0.txt ${BASE}/data/Unihan_IRGSources-10.0.0.txt 2>>err.txt

if [ -s err.txt ]; then
  echo Test Failed
  exit 1
else
  echo Test Passed
  exit 0
fi
