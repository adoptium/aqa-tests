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

set +e

active_versions=("24" "21" "17" "11" "default")
SLEEP_TIME=5
export LC_ALL=POSIX

setupLinuxEnv() {
    echo "Setup Linux Environment"
    # Fetch the prebuilt arctic jar (will be pulled from prereq build eventually)
    # wget -q https://ci.adoptium.net/job/Build_Arctic/5/artifact/upload/arctic-0.8.1.jar

    # Set environment variables, makes the assumption that JDK21 is the default java in /usr/bin/java
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
    export AWT_FORCE_HEADFUL=true
    echo "Setup Mac Environment"

    export ARCTIC_JDK=/usr/bin/java

    cat <<EOF > JMinWindows.java
		import java.awt.Robot;
		import java.awt.event.KeyEvent;
		import java.awt.Desktop;
		import java.io.File;
		public class JMinWindows {
		public static void main(String... args) throws Exception {
                    System.out.println("JMinWindows: Opening Finder on home folder so Finder becomes active front window and will get closed by Alt-Cmd-M");
		    Desktop.getDesktop().open(new File(System.getProperty("user.home")));
		    System.out.println("JMinWindows: Issuing Alt-Cmd-H to minimize all 'other' Windows");
		    Robot r = new Robot();
		    r.keyPress(KeyEvent.VK_META);
		    r.delay(250);
		    r.keyPress(KeyEvent.VK_ALT);
		    r.delay(250);
		    r.keyPress(KeyEvent.VK_H);
		    r.delay(250);
		    r.keyRelease(KeyEvent.VK_H);
		    r.delay(250);
		    r.keyRelease(KeyEvent.VK_ALT);
		    r.delay(250);
		    r.keyRelease(KeyEvent.VK_META);
		    r.delay(250);
		    System.out.println("JMinWindows: Issuing Alt-Cmd-M to minimize all Finder Windows");
                    r.keyPress(KeyEvent.VK_META);
                    r.delay(250);
                    r.keyPress(KeyEvent.VK_ALT);
                    r.delay(250);
                    r.keyPress(KeyEvent.VK_M);
                    r.delay(250);
                    r.keyRelease(KeyEvent.VK_M);
                    r.delay(250);
                    r.keyRelease(KeyEvent.VK_ALT);
                    r.delay(250);
                    r.keyRelease(KeyEvent.VK_META);
                    r.delay(250);
		  }
		}
EOF

		javac JMinWindows.java
		java -Djava.awt.headless=false JMinWindows

        echo "Running java ListJavaFonts..."

cat <<EOF > ListJavaFonts.java
                import java.awt.GraphicsEnvironment;
                public class ListJavaFonts {
                    public static void main( String[] args ) {
                        String java_fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                        for(String font : java_fonts) System.out.println( font );
                    }
                }
EOF

        javac ListJavaFonts.java
        echo "================================================"
        java ListJavaFonts
        echo "================================================"

}

setupWindowsEnv() {
    echo "Setup Windows Environment"
    ARCTIC_JDK=$(cygpath -u "C:/Users/jenkins/jck_run/${TEST_JDK_PATH}/bin/java")
    echo "Copying Arctic.jar Into Place"
    cp -rf /cygdrive/c/temp/arctic_jars/arctic-0.8.1.jar ${LIB_DIR}/arctic.jar
    cp -rf /cygdrive/c/temp/arctic_jars/JNativeHook-0.8.1.x86_64.dll ${LIB_DIR}/JNativeHook-0.8.1.x86_64.dll
    echo "Working directory: $(pwd)"
}

JOPTIONS="-Djava.net.preferIPv4Stack=true -Djdk.attach.allowAttachSelf=true -Dsun.rmi.activation.execPolicy=none -Djdk.xml.maxXMLNameLimit=4000"

if [ $(uname) = Linux ]; then
    JENKINS_HOME_DIR=/home/jenkins
    PPROP_LINE='s#arctic.common.repository.json.path.*$#arctic.common.repository.json.path = /home/jenkins/jck_run/arctic/linux/arctic_tests#g'
    setupLinuxEnv

elif [ $(uname) = Darwin ]; then
    JENKINS_HOME_DIR="/Users/jenkins"
    PPROP_LINE='s#arctic.common.repository.json.path.*$#arctic.common.repository.json.path = /Users/jenkins/jck_run/arctic/mac/arctic_tests#g'
    setupMacEnv

elif [ $(uname) = Windows_NT ]; then
    JENKINS_HOME_DIR="c:/Users/jenkins"
    PPROP_LINE='s#arctic.common.repository.json.path.*$#arctic.common.repository.json.path = c:/Users/jenkins/jck_run/arctic/windows/arctic_tests#g'
    setupWindowsEnv

fi

# Verify that the contents are present in jck_run
TEST_GROUP=$1
PLATFORM=$2
VERSION=$3
JCK_VERSION_NUMBER=$4
TEST_SUB_DIR=$5
OSNAME=${PLATFORM%_*}
STARTING_SCOPE=$VERSION
if [ $VERSION -eq 8 ]; then
    STARTING_SCOPE="default"
fi

if [ $OSNAME = "osx" ]; then
    OSNAME="mac"
fi

ARCTIC_GROUP="${TEST_SUB_DIR}"
if [ $TEST_GROUP = "custom" ]; then
    if [[ -z $TEST_SUB_DIR ]]; then
        echo "Custom: No custom Arctic groups specified, skipping."
        exit 0
    fi

    if [[ $TEST_SUB_DIR == api/java_awt/* ]]; then
        ARCTIC_GROUP="api/java_awt"
    elif [[ $TEST_SUB_DIR == api/javax_swing/* ]]; then    
        ARCTIC_GROUP="api/javax_swing"
    else
        echo "ERROR: custom Arctic target $TEST_SUB_DIR, is not a known group (api/java_awt, api/javax_swing)"
        exit 1
    fi
    # Strip ARCTIC_GROUP/ from front
    CUSTOM_ARCTIC_TESTCASE=${TEST_SUB_DIR/$ARCTIC_GROUP/}
    CUSTOM_ARCTIC_TESTCASE=${CUSTOM_ARCTIC_TESTCASE:1}
    echo "Running custom target: $ARCTIC_GROUP $CUSTOM_ARCTIC_TESTCASE"
fi

JCK_MATERIAL="$JENKINS_HOME_DIR/jck_root/JCK${VERSION}-unzipped/JCK-runtime-${JCK_VERSION_NUMBER}"

if [ $PLATFORM = "ppc64le_linux" ]; then
    wget -q https://ci.adoptium.net/job/Build_Arctic_ppc64le_linux/lastSuccessfulBuild/artifact/upload/arctic-0.8.1.jar
elif [ $PLATFORM = "s390x_linux" ]; then
    wget -q https://ci.adoptium.net/job/Build_Arctic_s390x_linux/lastSuccessfulBuild/artifact/upload/arctic-0.8.1.jar
else
    wget -q https://ci.adoptium.net/job/Build_Arctic/lastSuccessfulBuild/artifact/upload/arctic-0.8.1.jar
fi

mv arctic-0.8.1.jar ${LIB_DIR}/arctic.jar

cp $JENKINS_HOME_DIR/jck_run/arctic/$OSNAME/player.properties .
echo "Player properties line is $PPROP_LINE"
if [ $OSNAME = "mac" ]; then
  sed -i '' "$PPROP_LINE" player.properties
else
  sed -i "$PPROP_LINE" player.properties
fi

if [ ! -f ${LIB_DIR}/arctic.jar ]; then
    echo "arctic.jar not present"
    ls -al ${LIB_DIR}
fi

echo "==========================================================================="
cat player.properties
echo "==========================================================================="

echo "Starting player in background with RMI..."
$ARCTIC_JDK -Darctic.scope=$VERSION -Darctic.logLevel=TRACE -jar ${LIB_DIR}/arctic.jar -p &
rc=$?
if [ $rc -ne 0 ]; then
   echo "Unable to start Arctic player, rc=$rc"
   if [[ -n $twm_pid ]]; then
     kill $twm_pid 2>/dev/null
   fi
   exit $rc
fi

# Sleep longer for Arctic RMI to start up...
sleep $SLEEP_TIME
sleep $SLEEP_TIME

echo "Java under test: $TEST_JDK_HOME"
# twm &
TOP_DIR=$JENKINS_HOME_DIR/jck_run/arctic/$OSNAME/arctic_tests
echo "TEST_GROUP is $TEST_GROUP, OSNAME is $OSNAME, VERSION is $VERSION, STARTING_SCOPE is $STARTING_SCOPE"

overallSuccess=true
FOUND_TESTS=()
PASSED_TESTS=()
FAILED_TESTS=()
for i in "${active_versions[@]}"; do
  if [[ "$i" == "default" ]] || [[ "$i" -le "${VERSION}" ]]; then
    START_DIR="${TOP_DIR}/${i}/${ARCTIC_GROUP}"
    # Remove any double slashes
    START_DIR=$(echo "$START_DIR" | sed 's#//#/#g')

    TEST_JSON_FILES=$(find ${START_DIR} -type f -name 'Test.json' -o -name 'Test.link')
    for f in $TEST_JSON_FILES
    do
      f=$(echo "$f" | sed 's#//#/#g')
      echo "Test file: ${f}"

      # Determine Arctic testcase name from folder
      test_name="$(dirname "$f")"
      test_basename="$(basename "$f")"
      ARCTIC_TESTCASE=${test_name/$START_DIR//}
      ARCTIC_TESTCASE=${ARCTIC_TESTCASE/\/\//}

      # Is testcase recording ending in .html (ALL), or not(TESTCASE_ID)
      if [[ "$ARCTIC_TESTCASE" == *.html ]]; then
        JCK_TESTCASE="${ARCTIC_TESTCASE}"
        JCK_TEST="ALL"
      elif [[ "$ARCTIC_TESTCASE" == *.html/* ]]; then
        JCK_TESTCASE="$(dirname "$ARCTIC_TESTCASE")"
        JCK_TEST="$(basename "$ARCTIC_TESTCASE")"
      else
        JCK_TESTCASE=""
      fi

      # Is this a recording link? in which case point at target recording within Test.link file
      if [[ "$test_basename" == "Test.link" ]]; then
        ARCTIC_TESTCASE=$(cat $f)
        # Update JCK_TEST for link
        if [[ "$ARCTIC_TESTCASE" == *.html ]]; then
          JCK_TEST="ALL"
        elif [[ "$ARCTIC_TESTCASE" == *.html/* ]]; then
          JCK_TEST="$(basename "$ARCTIC_TESTCASE")"
        fi
      fi

      if [[ -n "${JCK_TESTCASE}" ]]; then
        HTML_FILE="${JCK_MATERIAL}/tests/${ARCTIC_GROUP}/${JCK_TESTCASE}"
        # Does JCK testcase exist for this VERSION ?
        if [[ -e "${HTML_FILE}" ]]; then
          FOUND=false
          for test in "${FOUND_TESTS[@]}"
          do
            if [[ "$test" == "$ARCTIC_TESTCASE" ]]; then
              FOUND=true
            fi
          done

          if [[ $FOUND == false ]]; then
            FOUND_TESTS+=("${ARCTIC_TESTCASE}")

            # Get class name from JCK .html
            TEST_CLASS=$(grep "javasoft.sqe.tests" ${HTML_FILE} | head -1 | sed -e 's/ //g' -e 's/<[^>]*>//g')

            echo "==> JCK Test exists: $HTML_FILE"
            echo "    Running:"
            echo "       Testcase ${JCK_TEST} of scope ${i} ${ARCTIC_GROUP} ${ARCTIC_TESTCASE}"
            echo "         JCK class: ${TEST_CLASS}"

            TEST_CMDLINE="${TEST_JDK_HOME}/bin/java -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel -Dmultitest.testcaseOrder=sorted -classpath :${JCK_MATERIAL}/classes: ${TEST_CLASS} -TestDirURL file:${JCK_MATERIAL}/tests/${ARCTIC_GROUP}/${JCK_TESTCASE} -TestCaseID ${JCK_TEST}"

            # Certain tests require extra options
            if [[ "${ARCTIC_TESTCASE}" =~ .*PageDialog.* ]] || [[ "${ARCTIC_TESTCASE}" =~ .*Print.* ]]; then
              TEST_CMDLINE="${TEST_CMDLINE} -platform.hasPrinter true"
            fi
            if [[ "${ARCTIC_TESTCASE}" =~ .*Robot.* ]]; then
              TEST_CMDLINE="${TEST_CMDLINE} -platform.robotAvailable true"
            fi
            echo "EXECUTING: ${TEST_CMDLINE}"

            # Custom check
            if [[ "${TEST_GROUP}" == "custom" ]] && [[ "${ARCTIC_TESTCASE}" != "${CUSTOM_ARCTIC_TESTCASE}" ]]; then
              test_pid=-1
              skipped=true
              echo "Skipping: $ARCTIC_GROUP $ARCTIC_TESTCASE"
            else
              skipped=false
              # Start TESTCASE...
              ${TEST_CMDLINE} &
              test_pid=$!
              echo "Testcase started process $test_pid"
            fi

            if [[ $skipped == false ]]; then
              sleep $SLEEP_TIME
            fi

            # Check testcase started successfully.
            ps -p $test_pid -o pid 2>/dev/null 1>&2
            if [[ $? != 0 ]]; then
              if [[ $skipped == false ]]; then
                echo "ERROR: Test class failed prior to playback."
                overallSuccess=false
              fi
            else
              # Testcase started, start Arctic playback...
              sleep $SLEEP_TIME

              echo "Starting Arctic: testcase $ARCTIC_GROUP $ARCTIC_TESTCASE"
              $ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test start "$ARCTIC_GROUP" "$ARCTIC_TESTCASE"
              rc=$?

              if [[ $rc -ne 0 ]]; then
                  echo "Unable to start playback for testcase $ARCTIC_GROUP/$ARCTIC_TESTCASE, rc=$rc"
              else
                sleep $SLEEP_TIME

                echo "$ARCTIC_GROUP/$ARCTIC_TESTCASE"

                result=$($ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test list $ARCTIC_GROUP/$ARCTIC_TESTCASE)
                rc=$?
                # status is "test path" left justified to 50 characters with no space from status
                status=$(echo $result | sed 's#'${ARCTIC_GROUP}/${ARCTIC_TESTCASE}'#TEST #' | tr -s ' '| cut -d' ' -f2) 
                echo "==>" $status
                loop_counter=360 # 30 mins
                while [[ $rc -eq 0 ]] && { [[ "$status" == "RUNNING" ]] || [[ "$status" == "STARTING" ]]; };
                do
                  sleep $SLEEP_TIME
                  loop_counter=$((loop_counter - 1))
                  if [[ $loop_counter -eq 0 ]]; then
                    echo "Arctic process has timed out. Tidying up processes and failing job."
                    status="ABORTED"
                    rc=1
                  else
                    result=$($ARCTIC_JDK -jar ${LIB_DIR}/arctic.jar -c test list $ARCTIC_GROUP/$ARCTIC_TESTCASE)
                    rc=$?
                    status=$(echo $result | sed 's#'${ARCTIC_GROUP}/${ARCTIC_TESTCASE}'#TEST #' | tr -s ' '| cut -d' ' -f2)
                  fi
                  echo "==>" $status
                done

                sleep $SLEEP_TIME

                echo "Testcase process $test_pid should have finished if successfully automated, getting test process completion status..."
                if ps -p $test_pid -o pid; then
                  echo "ERROR: Testcase process $test_pid is still running... terminating!"
                  kill -9 $test_pid
                fi
                wait $test_pid
                test_exit_status=$?
                echo "Testcase exited with completion status = ${test_exit_status}"
                echo "Testcase Arctic status = ${status}"

                # Finish Arctic TESTCASE session
                # NOTE: PASSED == 95 for jtharness test status, javatest CLI will be "0" !
                success=false
                if [[ $status == "UNCONFIRMED" ]] && [[ $test_exit_status == 95 ]]; then
                  ${ARCTIC_JDK} -jar ${LIB_DIR}/arctic.jar -c test finish "${ARCTIC_GROUP}" "${ARCTIC_TESTCASE}" true
                  success=true
                else
                  ${ARCTIC_JDK} -jar ${LIB_DIR}/arctic.jar -c test finish "${ARCTIC_GROUP}" "${ARCTIC_TESTCASE}" false
                fi

                # Get final Arctic status
                result=$(${ARCTIC_JDK} -jar ${LIB_DIR}/arctic.jar -c test list ${ARCTIC_GROUP}/${ARCTIC_TESTCASE})
                status=$(echo $result | tr -s ' ' | cut -d' ' -f2)
                echo "Arctic final completion status ==>" $status

                echo "Saving Arctic session..."
                session_file=$(echo ${ARCTIC_GROUP}/${ARCTIC_TESTCASE}.session | tr "/" "_")
                ${ARCTIC_JDK} -jar ${LIB_DIR}/arctic.jar -c session save ${session_file}

                echo "Printing Arctic session info..."
                ${ARCTIC_JDK} -jar ${LIB_DIR}/arctic.jar -c session print

                echo "Completed playback of ${ARCTIC_GROUP}/${ARCTIC_TESTCASE} status: ${status} success: ${success}"

                # Clean processes before exit...
                kill $test_pid 2>/dev/null

                if [[ $success != true ]]; then
                    FAILED_TESTS+=("${ARCTIC_GROUP}/${ARCTIC_TESTCASE}")
                    overallSuccess=false
                else
                    PASSED_TESTS+=("${ARCTIC_GROUP}/${ARCTIC_TESTCASE}")
                fi
              fi
            fi
          fi
        fi
      fi
    done
  fi
done

echo "Terminating Arctic CLI..."
${ARCTIC_JDK} -jar ${LIB_DIR}/arctic.jar -c terminate

if [[ -n $twm_pid ]]; then
  kill $twm_pid 2>/dev/null
fi

echo "======================================================================================="
echo "PASSED Testcases:"
for test in "${PASSED_TESTS[@]}"
do
  echo "$test : PASSED"
done
echo "======================================================================================="

if [[ $overallSuccess != true ]]; then
  echo "FAILED Testcases:"
  for test in "${FAILED_TESTS[@]}"
  do
    echo "$test : FAILED"
  done
  echo "======================================================================================="
fi

echo "Finished running testcases, overallSuccess = $overallSuccess"

if [[ $overallSuccess != true ]]; then
    echo "Arctic playback failed"
    exit 1
else
    echo "Arctic playback successful"
    exit 0
fi

