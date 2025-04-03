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

export LC_ALL=POSIX

wget -q https://ci.adoptium.net/view/Test_grinder/job/Build_Arctic/lastSuccessfulBuild/artifact/upload/Arctic.jar
mv Arctic.jar ${LIB_DIR}

wget -q https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz
tar -xf OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz

export PATH=$PWD/jdk-17.0.9+9/bin:$PATH
export JAVA_HOME=$PWD/jdk-17.0.9+9

disp=":36"
Xvfb $disp -screen 0 1024x768x24 -nolisten tcp &
xvfb_pid=$!
echo "Started Xvfb process $xvfb_pid on DISPLAY $disp"
export DISPLAY=$disp

cp /etc/X11/twm/system.twmrc $HOME/.twmrc
echo 'RightTitleButton "xlogo11" = f.delete' >> $HOME/.twmrc
echo 'Button3 = : root : f.menu "windowops"' >> $HOME/.twmrc
echo 'RandomPlacement'                       >> $HOME/.twmrc
# Ensure fonts match recording vs playback
sed -i 's/MenuFont.*$/MenuFont      "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
sed -i 's/TitleFont .*$/TitleFont   "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
sed -i 's/IconFont .*$/IconFont     "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc
sed -i 's/ResizeFont .*$/ResizeFont "-misc-fixed-bold-r-normal--15-140-75-75-c-90-iso8859-1"/g' $HOME/.twmrc

echo "Starting player in background with RMI..."

if [ ! -f ${LIB_DIR}/Arctic.jar ]; then
    echo "Arctic.jar not present"
    ls -al ${LIB_DIR}
fi

java -Darctic.logLevel=TRACE -jar ${LIB_DIR}/Arctic.jar -p &
rc=$?
if [ $rc -ne 0 ]; then
   echo "Unable to start Arctic player, rc=$rc"
   exit $rc
fi

# Allow 3 seconds for RMI server to start...
sleep 3

testdir=$1
testgroup="java_awt" # eventually strip off name from testdir variable 

ls -al "$testdir"

# Loop through files in the target directory
for testcase in "$testdir"/*; do
    tcase="api/$testgroup/interactive/$testcase.html"
    # Start testcase...
    echo "Starting testcase... $tcase"
    twm &

    # replace with variable representing JDK under test
    /home/jenkins/jck_run/jdk21/jdk/jdk-21.0.7+5/bin/java --enable-preview --add-modules java.xml.crypto,java.sql -Djava.net.preferIPv4Stack=true -Djdk.attach.allowAttachSelf=true -Dsun.rmi.activation.execPolicy=none -Djdk.xml.maxXMLNameLimit=4000 -classpath :/home/jenkins/jck_root/JCK21-unzipped/JCK-runtime-21/classes: -Djava.security.policy=/home/jenkins/jck_root/JCK21-unzipped/JCK-runtime-21/lib/jck.policy javasoft.sqe.tests.api.java.awt.interactive.ListTests -TestCaseID ALL &
    echo "Testcase started"

    if [ -f "$testcase" ]; then
        # Allow 3 seconds for RMI server to start...
        sleep 3
        echo "Running testcase $testcase"
        java -jar ${LIB_DIR}/Arctic.jar -c test start "${testgroup}" "${testcase}"
        rc=$?
    
        if [[ $rc -ne 0 ]]; then
            echo "Unable to start playback for testcase ${testgroup}/${testcase}, rc=$rc"
            exit $rc
        fi

        # Allow 3 seconds for RMI server to start...
        sleep 3
        result=$(java -jar ${LIB_DIR}/Arctic.jar -c test list ${testgroup}/${testcase})
        rc=$?
        status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
        echo "==>" $status
        while [[ $rc -eq 0 ]] && { [[ "$status" == "RUNNING" ]] || [[ "$status" == "STARTING" ]]; };
        do
            sleep 3
            result=$(java -jar ${LIB_DIR}/Arctic.jar -c test list ${testgroup}/${testcase})
            rc=$?
            status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
            echo "==>" $status
        done

        echo "Terminating Arctic CLI..."
        java -jar ${LIB_DIR}/Arctic.jar -c terminate
        echo "Completed playback of ${testgroup}/${testcase} status: ${status}"
    fi
done

kill $xvfb_pid

echo "Finished running $testdir testcases!"

if [[ $status != "UNCONFIRMED" ]]; then
    echo "Arctic playback failed"
    exit 1
else
    echo "Arctic playback successful"
    exit 0
fi