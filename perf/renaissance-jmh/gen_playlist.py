'''
This script generates the playlist.xml file necessary for running benchmarks in the Renaissance JHM benchmark suite.
See https://github.com/renaissance-benchmarks/renaissance/.
'''

# This is the list of benchmarks in the Renaissance suite that we're interested in.
benchmarks= [  "AkkaUct", "Als", "ChiSquare", "DecTree", "Dotty", "FinagleChirper", "FinagleHttp", "FjKmeans", "FutureGenetic", 
            "GaussMix", "LogRegression", "Mnemonics", "MovieLens", "NaiveBayes", "PageRank", "ParMnemonics", 
            "Philosophers", "Reactors", "RxScrabble", "ScalaDoku", "ScalaKmeans", "ScalaStmBench7", "Scrabble"     
         ]

# Forks are used to specify the number of times a benchmark should be run
# Note that benchmarks already have a default # of measurement interations - forks will multiply those
forks= [8, 17, 7, 4, 5, 5, 8, 8, 7, 8, 7, 4, 10, 10, 2, 2, 4, 8, 7, 7, 4, 2, 12]

# Create the file
file = open("playlist.xml", "w")

# Generate the header
file.write("<?xml version='1.0' encoding='UTF-8'?>")
file.write("""
<!--
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
-->
""")

# Generate the body
file.write("<playlist xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../TKG/playlist.xsd\">\n")
file.write("\t<include>../perf.mk</include>\n")

# For each of the benchmarks in the list, generate a test
# The additional JVM options were chosen by experimentation in order to reduce variation in the performance of the benchmarks
for idx, bench in enumerate(benchmarks):
    file.write("\t<test>\n")

    file.write("\t\t<testCaseName>renaissance-jmh-" + bench + "</testCaseName>\n")
    file.write("\t\t<command>\n")
    file.write("\t\t\t$(JAVA_COMMAND) $(ADD_OPENS_CMD) $(JVM_OPTIONS) -Xlog:gc*:file=" + bench + "_gc.log -XX:+UseG1GC -Xms12G -Xmx12G -XX:ThreadPriorityPolicy=1 -XX:+AlwaysPreTouch -XX:+UseLargePages -XX:+UseTransparentHugePages")
    file.write(" -jar $(Q)$(TEST_RESROOT)$(D)renaissance-jmh.jar$(Q) -f " + str(forks[idx]) + " -prof gc -bm avgt -rf json -rff $(Q)$(REPORTDIR)$(D)" + bench + ".json$(Q) " + bench + "; $(TEST_STATUS)\n")
    file.write("\t\t</command>\n")
    file.write("\t\t<levels>\n\t\t\t<level>extended</level>\n\t\t</levels>\n\t\t<groups>\n\t\t\t<group>perf</group>\n\t\t</groups>\n")

    file.write("\t</test>\n")

file.write("</playlist>")