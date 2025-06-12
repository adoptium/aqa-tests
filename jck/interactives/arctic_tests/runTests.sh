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

# Fetch the prebuilt arctic jar (will be pulled from prereq build eventually)
wget -q https://ci.adoptium.net/job/Build_Arctic/5/artifact/upload/arctic-0.8.1.jar
mv arctic-0.8.1.jar ${LIB_DIR}/arctic.jar

JENKINS_HOME=/home/jenkins
PPROP_LINE='s#arctic.common.repository.json.path.*\$#arctic.common.repository.json.path = /home/jenkins/jck_run/arctic/mac/arctic_tests#g'
if [ $(uname) = SunOS ]; then
    JENKINS_HOME = "/export/home/jenkins"
    PPROP_LINE='s#arctic.common.repository.json.path.*\$#arctic.common.repository.json.path = /export/home/jenkins/jck_run/arctic/mac/arctic_tests#g'
elif [ $(uname) = Darwin ]; then
    JENKINS_HOME = "/Users/jenkins"
    PPROP_LINE='s#arctic.common.repository.json.path.*\$#arctic.common.repository.json.path = /Users/jenkins/jck_run/arctic/mac/arctic_tests#g'
elif [ $(uname) = Windows_NT ]; then
    JENKINS_HOME = "c:/Users/jenkins"
    PPROP_LINE='s#arctic.common.repository.json.path.*\$#arctic.common.repository.json.path = c:/Users/jenkins/jck_run/arctic/mac/arctic_tests#g'
fi

# Verify that the contents are present in jck_run
TEST_GROUP=$1
PLATFORM=$2
VERSION=$3
JCK_VERSION_NUMBER=$4
OSNAME=${PLATFORM%_*}

TEST_DIR=$JENKINS_HOME/jck_run/arctic/$OSNAME/arctic_tests/default/api/$TEST_GROUP/interactive
echo "TEST_DIR is $TEST_DIR"
ls -al "$TEST_DIR"
echo "TEST_GROUP is $TEST_GROUP, OSNAME is $OSNAME, VERSION is $VERSION"

# Set environment variables, makes the assumption that JDK21 is the default java in /usr/bin/java
export LC_ALL=POSIX
export ARCTIC_JDK=/usr/bin/java

cp /etc/X11/twm/system.twmrc $HOME/.twmrc
echo 'RightTitleButton "xlogo11" = f.delete' >> $HOME/.twmrc
echo 'Button3 = : root : f.menu "windowops"' >> $HOME/.twmrc
echo 'RandomPlacement'                       >> $HOME/.twmrc
# Ensure fonts match recording vs playback
sed -i 's/MenuFont.*$/MenuFont      "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
sed -i 's/TitleFont .*$/TitleFont   "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
sed -i 's/IconFont .*$/IconFont     "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
sed -i 's/ResizeFont .*$/ResizeFont "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc

cp $JENKINS_HOME/jck_run/arctic/mac/player.properties .
sed -i $PPROP_LINE player.properties

if [ ! -f ${LIB_DIR}/arctic.jar ]; then
    echo "arctic.jar not present"
    ls -al ${LIB_DIR}
fi

echo "Starting player in background with RMI..."
$ARCTIC_JDK -Darctic.logLevel=TRACE -jar ${LIB_DIR}/arctic.jar -p &
rc=$?
if [ $rc -ne 0 ]; then
   echo "Unable to start Arctic player, rc=$rc"
   exit $rc
fi

# Allow 3 seconds for RMI server to start...
sleep 3

echo "Running testcases in $TEST_GROUP on $OSNAME"
echo "Java under test: $TEST_JDK_HOME"
twm &

# Loop through files in the target directory
for testcase in $TEST_DIR/* 
do
echo "testcase is $testcase"
   if [ -d $testcase ]; then
      echo "Starting testcase... $testcase"
      tcase=${testcase##*/}
      tcase=${tcase%.html}
      echo "tcase is $tcase"
      tgroup=${TEST_GROUP//_/\.} 
      echo $tgroup

      $TEST_JDK_HOME/bin/java --enable-preview --add-modules java.xml.crypto,java.sql -Djava.net.preferIPv4Stack=true -Djdk.attach.allowAttachSelf=true -Dsun.rmi.activation.execPolicy=none -Djdk.xml.maxXMLNameLimit=4000 -classpath :$JENKINS_HOME/jck_root/JCK$VERSION-unzipped/JCK-runtime-$JCK_VERSION_NUMBER/classes: -Djava.security.policy=$JENKINS_HOME/jck_root/JCK$VERSION-unzipped/JCK-runtime-$JCK_VERSION_NUMBER/lib/jck.policy javasoft.sqe.tests.api.$tgroup.interactive.$tcase -TestCaseID ALL &
      
      # Allow 3 seconds for RMI server to start...
      sleep 10
      echo "Running testcase $testcase"
      $ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test start "api/$TEST_GROUP" "$tcase"
      rc=$?
      
      if [[ $rc -ne 0 ]]; then
        echo "Unable to start playback for testcase $TEST_GROUP/$tcase, rc=$rc"
        exit $rc
      fi

        # Allow 3 seconds for RMI server to start...
        sleep 10
        result=$($ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test list $TEST_GROUP/$tcase)
        rc=$?
        status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
        echo "==>" $status
        while [[ $rc -eq 0 ]] && { [[ "$status" == "RUNNING" ]] || [[ "$status" == "STARTING" ]]; };
        do
            sleep 3
            result=$($ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test list $TEST_GROUP/$tcase)
            rc=$?
            status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
            echo "==>" $status
        done

        echo "Terminating Arctic CLI..."
        $ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c terminate
        echo "Completed playback of $TEST_GROUP/$tcase status: ${status}"
    fi
done

echo "Finished running $TEST_DIR testcases!"

if [[ $status != "UNCONFIRMED" ]]; then
    echo "Arctic playback failed"
    exit 1
else
    echo "Arctic playback successful"
    exit 0
fi