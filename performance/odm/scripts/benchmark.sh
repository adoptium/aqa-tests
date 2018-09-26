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

export PRODUCT=pxa6480sr6
export BM_DIR_NAME=ilog_wodm881

#Just for testing purposes: to be removed later
currentDir="$(pwd)"
echo "$currentDir"
echo "Current Dir: $PWD"
ls -lart
echo "$PERFMAN_PASSWORD"

#This is where all the benchmark packages are downloaded. All downloaded SDKs exist under ./sdks
RUN_DIR=$1
export RUN_DIR=$RUN_DIR

#TODO: Remove these once the use of STAF has been eliminated from all the benchmark scripts
export PATH=/usr/local/staf/bin:$PATH
export LD_LIBRARY_PATH=/usr/local/staf/lib:$LD_LIBRARY_PATH

benchmark_dir="$RUN_DIR/${BM_DIR_NAME}"
echo "benchmark_dir: $benchmark_dir"

cd $benchmark_dir
echo "Current Dir: $(pwd)"

#Temporarily setting the SDK to this since Perfxposh doesn't have Espresso access so we have use the one that's already
#there on the machine.
LAST_BUILD=`cat "$WORKSPACE/JDK_LAST_BUILD.txt"`
#export JDK_DIR="$WORKSPACE/../../sdks"
#export JDK="${PRODUCT}-$LAST_BUILD"
JDK_DIR=$JAVA_BIN/..
export JDK_DIR=${JDK_DIR}

######### Generated Script #########

export CLASS="com.ibm.rules.bench.segmentation.RuleEngineRunner"
export CLSPATH="lib/asm-3.1.jar:lib/asm-analysis-3.1.jar:lib/asm-commons-3.1.jar:lib/asm-tree-3.1.jar:lib/asm-util-3.1.jar:lib/dom4j-1.6.1.jar:lib/j2ee_connector-1_5-fr.jar:lib/jrules-engine.jar:lib/jrules-res-execution.jar:lib/log4j-1.2.8.jar:lib/openxml4j-beta.jar:lib/sam.jar:lib/sizing-xom.jar:bin:bin/ra.xml"
export RULEUSAGE="false"
export BENCHMARKARGS="ruleset=F_JAVAXOM_Segmentation5_DE javaXOM sizeparam=1 warmup=240 timeout=400 stateful 100000000 reportPath=j2se-perf-report-87.csv jrulesVersion=8.7 DT_or_Rules=DT"
export WORKDIR="$(pwd)"
export WORKLOAD="ILOG_WODM"
export TIMEOUT="900000"
export GCPOLICY="-Xgcpolicy:optthruput"
export JDK_OPTIONS="-Xdump:system:defaults:file={perffarm_dump}/core.%Y%m%d.%H%M%S.%pid.%seq.dmp -Xdump:nofailover -Xcompressedrefs"
export MINHEAPSIZE="-Xms1024m"
export MAXHEAPSIZE="-Xmx1024m"
export TENUREDSIZE=""
export NURSERYSIZE=""
export MULTITHREAD="4"
export CPUAFFINITY="numactl --physcpubind=0,1,36,37 --membind=0"

echo "********** START OF NEW TESTCI BENCHMARK JOB **********"
echo "Benchmark Name: ILOG_WODM Benchmark Variant: 881-4way-Seg5FastpathRVEJB"
echo "Benchmark Product: $JDK"

bash ./run_ilog_with_gcmv.sh