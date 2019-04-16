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

echo "***** Running Benchmark Script *****"

echo "Current Dir: $(pwd)"

#TODO: Remove these once the use of STAF has been eliminated from all the benchmark scripts
export PATH=/usr/local/staf/bin:$PATH
export LD_LIBRARY_PATH=/usr/local/staf/lib:$LD_LIBRARY_PATH


echo "JDK_VERSION=${JDK_VERSION}"

export JDK="j2sdk-image"
echo "JDK=${JDK}"

export JDK_DIR="${TEST_JDK_HOME}/.."
echo "JDK_DIR=${JDK_DIR}"

######### Generated Script #########

#TODO: Need to do some cleanup and restructure some files for adding other configs
echo ""
echo "********** START OF NEW TESTCI BENCHMARK JOB **********"
echo "Benchmark Name: LibertyStartupDT Benchmark Variant: 17dev-4way-0-256-qs"
echo "Benchmark Product: ${JDK}"
echo ""

#TODO: Need to tune these options. Keeping them simple for now 
export JDK_OPTIONS="-Xmx256m"
export COLD="0"
export WARMUP="0"
export NO_SETUP="false"
export SETUP_ONLY="false"
export WARM="1"
export INSTALL_DIR=""
export LIB_PATH=""
export HEALTH_CENTRE=""
export COGNOS_WAIT=""
export REQUEST_CORE=""
export SCENARIO="DayTrader7"
export SERVER_NAME="LibertySUDTServer-$JDK"
export PETERFP="false"
export RESULTS_MACHINE="lowry1"
export RESULTS_DIR="libertyResults"
export LIBERTY_HOST="$(hostname)"
export LAUNCH_SCRIPT="server"
export LIBERTY_BINARIES_DIR="$1/libertyBinaries"
export LIBERTY_VERSION="openliberty-19.0.0.4"
export APP_VERSION="daytrader-ee7"
export WLP_SKIP_MAXPERMSIZE="1"

#TODO: Need to soft-code these configs. Need to add various affinity tools in the perf pre-reqs ()
export AFFINITY="numactl --physcpubind=0-3 --membind=0"

bash ${1}/scripts/bin/sufp_benchmark.sh 