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

setupLinuxEnv() {
    # Set environment variables, makes the assumption that JDK21 is the default java in /usr/bin/java
    export LC_ALL=POSIX
    export ARCTIC_JDK=/usr/bin/java

    echo "Fonts:"
	echo "==========================================="
	find /usr/share/fonts -name "*" -type d
	echo "==========================================="

	cp /etc/X11/twm/system.twmrc $HOME/.twmrc
	echo 'RightTitleButton "xlogo11" = f.delete' >> $HOME/.twmrc
	echo 'Button3 = : root : f.menu "windowops"' >> $HOME/.twmrc
	echo 'RandomPlacement'                       >> $HOME/.twmrc

	# Ensure fonts match recording vs playback
	sed -i 's/MenuFont.*$/MenuFont    "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
	sed -i 's/TitleFont .*$/TitleFont   "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
	sed -i 's/IconFont .*$/IconFont    "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
	sed -i 's/ResizeFont .*$/ResizeFont    "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
	sed -i 's/IconManagerFont .*$/IconManagerFont    "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc

	# Ensure consistent colors
	sed -i 's/BorderColor.*$/BorderColor "slategrey"/g' $HOME/.twmrc
	sed -i 's/DefaultBackground.*$/DefaultBackground "rgb:2\\/a\\/9"/g' $HOME/.twmrc
	sed -i 's/DefaultForeground.*$/DefaultForeground "gray85"/g' $HOME/.twmrc
	sed -i 's/TitleBackground.*$/TitleBackground "rgb:2\\/a\\/9"/g' $HOME/.twmrc
	sed -i 's/TitleForeground.*$/TitleForeground "gray85"/g' $HOME/.twmrc
	sed -i 's/MenuBackground.*$/MenuBackground "rgb:2\\/a\\/9"/g' $HOME/.twmrc
	sed -i 's/MenuForeground.*$/MenuForeground "gray85"/g' $HOME/.twmrc
	sed -i 's/MenuBorderColor.*$/MenuBorderColor "slategrey"/g' $HOME/.twmrc
	sed -i 's/MenuTitleBackground.*$/MenuTitleBackground "gray70"/g' $HOME/.twmrc
	sed -i 's/MenuTitleForeground.*$/MenuTitleForeground "rgb:2\\/a\\/9"/g' $HOME/.twmrc
	sed -i 's/IconBackground.*$/IconBackground "rgb:2\\/a\\/9"/g' $HOME/.twmrc
	sed -i 's/IconForeground.*$/IconForeground "gray85"/g' $HOME/.twmrc
	sed -i 's/IconBorderColor.*$/IconBorderColor "gray85"/g' $HOME/.twmrc
	sed -i 's/IconManagerBackground.*$/IconManagerBackground "rgb:2\\/a\\/9"/g' $HOME/.twmrc
	sed -i 's/IconManagerForeground.*$/IconManagerForeground "gray85"/g' $HOME/.twmrc

	# Start twm
	twm &
	twm_pid=$!
	echo "Started twm process $twm_pid"
}

setupMacEnv() {

}

setupWindowsEnv() {

}

JOPTIONS="-Djava.net.preferIPv4Stack=true -Djdk.attach.allowAttachSelf=true -Dsun.rmi.activation.execPolicy=none -Djdk.xml.maxXMLNameLimit=4000"

if [ $(uname) = Linux ]; then
    JENKINS_HOME=/home/jenkins
    PPROP_LINE='s#arctic.common.repository.json.path.*$#arctic.common.repository.json.path = /home/jenkins/jck_run/arctic/mac/arctic_tests#g'
    setupLinuxEnv

elif [ $(uname) = Darwin ]; then
    JENKINS_HOME = "/Users/jenkins"
    PPROP_LINE='s#arctic.common.repository.json.path.*\$#arctic.common.repository.json.path = /Users/jenkins/jck_run/arctic/mac/arctic_tests#g'
    setupMacEnv

elif [ $(uname) = Windows_NT ]; then
    JENKINS_HOME = "c:/Users/jenkins"
    PPROP_LINE='s#arctic.common.repository.json.path.*$#arctic.common.repository.json.path = c:/Users/jenkins/jck_run/arctic/mac/arctic_tests#g'
    setupWindowsEnv

fi

# Verify that the contents are present in jck_run
TEST_GROUP=$1
PLATFORM=$2
VERSION=$3
TEST_SUB_DIR=$4
JCK_VERSION_NUMBER=$5
OSNAME=${PLATFORM%_*}
STARTING_SCOPE=$VERSION
if [ $VERSION -eq 8 ]; then
    STARTING_SCOPE="default"
fi

TEST_DIR=$JENKINS_HOME/jck_run/arctic/$OSNAME/arctic_tests/$STARTING_SCOPE/$TEST_SUB_DIR/interactive
echo "TEST_DIR is $TEST_DIR"
ls -al "$TEST_DIR"
echo "TEST_GROUP is $TEST_GROUP, OSNAME is $OSNAME, VERSION is $VERSION", STARTING_SCOPE is $STARTING_SCOPE, TEST_SUB_DIR is $TEST_SUB_DIR

echo "Running testcases in $TEST_GROUP on $OSNAME"
if [ $OSNAME = osx ]; then
    $OSNAME = "mac"
fi

cp $JENKINS_HOME/jck_run/arctic/$OSNAME/player.properties .
echo "Player properties line is $PPROP_LINE"
sed -i "$PPROP_LINE" player.properties

if [ ! -f ${LIB_DIR}/arctic.jar ]; then
    echo "arctic.jar not present"
    ls -al ${LIB_DIR}
fi

echo "Starting player in background with RMI..."
# $ARCTIC_JDK -Darctic.logLevel=TRACE -jar ${LIB_DIR}/arctic.jar -p &
rc=$?
if [ $rc -ne 0 ]; then
   echo "Unable to start Arctic player, rc=$rc"
   exit $rc
fi

echo "Java under test: $TEST_JDK_HOME"
twm &

# Loop through files in the target directory
for testcase in $TEST_DIR/* 
do
echo "testcase is $testcase"
# if $TEST_DIR "ends with .html":
#   run -TestCaseID ALL
#else:
#   run <parent folder.html> -TestCaseID <folder>
   if [ -d $testcase ]; then
   # Look for Test.json file & Test.link file

      echo "Starting testcase... $testcase"
      tcase=${testcase##*/}
      tcase=${tcase%.html}
      echo "tcase is $tcase"
      tgroup=${TEST_GROUP//_/\.} 
      echo $tgroup

      # $TEST_JDK_HOME/bin/java --enable-preview --add-modules java.xml.crypto,java.sql $JOPTIONS -classpath :$JENKINS_HOME/jck_root/JCK$VERSION-unzipped/JCK-runtime-$JCK_VERSION_NUMBER/classes: -Djava.security.policy=$JENKINS_HOME/jck_root/JCK$VERSION-unzipped/JCK-runtime-$JCK_VERSION_NUMBER/lib/jck.policy javasoft.sqe.tests.api.$tgroup.interactive.$tcase -TestCaseID ALL &
      
      sleep 10
      echo "Running testcase $testcase"
      # $ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test start "api/$TEST_GROUP" "$tcase"
      rc=$?
      
      if [[ $rc -ne 0 ]]; then
        echo "Unable to start playback for testcase $TEST_GROUP/$tcase, rc=$rc"
        exit $rc
      fi

      sleep 10
      echo "$TEST_GROUP/$tcase"
      result="testing"
      # result=$($ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test list $TEST_GROUP/$tcase)
      rc=$?
      status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
      echo "==>" $status
      while [[ $rc -eq 0 ]] && { [[ "$status" == "RUNNING" ]] || [[ "$status" == "STARTING" ]]; };
        do
            sleep 3
            result="testing"
            # result=$($ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test list $TEST_GROUP/$tcase)
            rc=$?
            status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
            echo "==>" $status
        done

        echo "Terminating Arctic CLI..."
        # $ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c terminate
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