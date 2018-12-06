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
OS=`uname`
LOC=`locale charmap`
FULLLANG=${OS}_${LANG%.*}.${LOC}

. ${BASE}/../data/test_${FULLLANG}

cp ${BASE}/*.java .

javac CheckValidData.java

TS=`java CheckValidData "${TEST_STRING}"`

echo "creating source file..."
sed "s/TEST_STRING/${TS}/g" DefineAnnotation_org.java > DefineAnnotation.java
sed "s/TEST_STRING/${TS}/g"  AnnotatedTest_org.java >  AnnotatedTest.java

echo "compiling..."
javac DefineAnnotation.java AnnotationProcessor.java AnnotatedTest.java AnnotationProcessor7.java AnnotationProcessor8.java AnnotationProcessor11.java

echo "execute javap"
javap AnnotatedTest

echo "execute javac with processor option with RELEASE_6"
javac -processor AnnotationProcessor AnnotatedTest.java

echo "execute javac with processor option with RELEASE_7"
javac -processor AnnotationProcessor7 AnnotatedTest.java

echo "execute javac with processor option with RELEASE_8"
javac -processor AnnotationProcessor8 AnnotatedTest.java

echo "execute javac with processor option with RELEASE_11"
javac -processor AnnotationProcessor11 AnnotatedTest.java

