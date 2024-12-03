#!/bin/bash
set -euo pipefail
# an experimental script to try all external tests on different osses/jdks

if [ -z ${EXTERNALS_DIR:-} ] ; then
  EXTERNALS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi
EXTERNALS_DIR="$(cd "${EXTERNALS_DIR}" && pwd)"

if [ -z ${RESULTS_DIR:-} ] ; then
  RESULTS_DIR="${HOME}/externalsMatrix"
fi
RESULTS_DIR="$(cd "${RESULTS_DIR}" && pwd)"

if [ -z ${JDKS_DIR:-} ] ; then
  JDKS_DIR="${HOME}/externalJdks"
fi
if [ -d ${JDKS_DIR} ] ; then 
  JDKS_DIR="$(cd "${JDKS_DIR}" && pwd)"
fi

if [ -z ${IMAGES:-} ] ; then
  IMAGES="DEFAULT fedora:40 centos:stream8"
fi
if [ -z ${TEST_EXCLUDE_LIST:-} ] ; then
  # those tests donot seem to run with external JDK
  TEST_EXCLUDE_LIST="\(criu\|quarkus\|openliberty\|tck-ubi-testplaylist\|openjdk\)"
fi
if [ -z ${TEST_INCLUDE_LIST:-} ] ; then
  TEST_INCLUDE_LIST=".*"
fi
if [ -z ${SKIP_GET:-} ] ; then
  SKIP_GET="true"
fi

# warning, all functions operate with global variables

function checkIncludesExcludes() {
  echo " ** included by $TEST_INCLUDE_LIST:"
  find "${EXTERNALS_DIR}" | sort | grep playlist.xml | grep -e "${TEST_INCLUDE_LIST}"
  echo " ** excluded by $TEST_EXCLUDE_LIST: **"
  find "${EXTERNALS_DIR}" | sort | grep playlist.xml | grep -e "${TEST_EXCLUDE_LIST}"
}

function useDirIfPossible() {
  if [ -d "${jdk}" ] ; then
    if [ "${jdk}" == "." ] ; then
      jdk=$(cd "${jdk}" && pwd);
    fi
    if [ -e "${jdk}/bin/java" ] ; then
      echo "${jdk} is java, using"
    else
      echo ""${jdk}" is not java, skipping"
      return 0
    fi
  fi
  return 1
}

function useTarballIfPossible() {
  if [ -f "${jdk}" ] ; then
    futureJdk=$(mktemp -d )
    echo "Unpacking ${jdk}"
    tar -xf "${jdk}" -C "${futureJdk}"
    extractedJavaCmd=$(find "${futureJdk}" | grep "bin/java" | head -n 1)
    jdk=$(dirname $(dirname "${extractedJavaCmd}"))
    echo "Using ${jdk}"
  fi
}

function cleanUnpackedJdk() {
  if [ ! "${futureJdk}" == "itWasDir" ] ; then
    echo "Cleaning ${jdk}"
    rm -rf ${futureJdk}
  fi
}

function runExternalTest() {
  echo $jdkName $image $externalTest $testCase
  local fileName="${finalPath}/${externalTest}:${testCase}"
  rm -f "${fileName}"
  echo "cat ${fileName}* for details"
  export TEST_JDK_HOME="${jdk}"
  export EXTRA_DOCKER_ARGS="-v $TEST_JDK_HOME:/opt/java/openjdk" ;
  export BUILD_LIST=external/${externalTest} ; 
  if [ "${image}" == "DEFAULT" ] ; then
    unset EXTERNAL_AQA_IMAGE;
  else
    export EXTERNAL_AQA_IMAGE="${image}";
  fi
  pushd "${EXTERNALS_DIR}/.." > /dev/null
    if [ ! "${SKIP_GET:-}" == "true" ] ; then
      sh ./get.sh
    fi
    SKIP_GET=true
    cd TKG
    local cresult=0
    make compile > "${fileName}" 2>&1 || cresult=$?
    local rresult=0
    make "_${testCase}" >> "${fileName}" 2>&1 || rresult=$?
    echo "compilation: $cresult" >> "${fileName}"
    echo "runtime: $rresult" >> "${fileName}"
    if [ $cresult -gt 0 ] ; then
      mv "${fileName}" "${fileName}-ERROR"
      echo "error compiling"
    elif [ $rresult -gt 0 ] ; then
      mv "${fileName}" "${fileName}-FAILED"
      echo "failed"
    else
      mv "${fileName}" "${fileName}-PASSED"
      echo "passed"
    fi
  popd > /dev/null
}

rm -rf "${RESULTS_DIR}"
mkdir "${RESULTS_DIR}"
checkIncludesExcludes
for jdk in $(find  "${JDKS_DIR}"  -maxdepth 1 | sort -V ); do
  if useDirIfPossible ; then continue ; fi
  jdkName=$(basename "${jdk}")
  futureJdk="itWasDir"
  useTarballIfPossible
  mkdir "${RESULTS_DIR}/${jdkName}"
  for image in $IMAGES ; do 
  finalPath="${RESULTS_DIR}/${jdkName}/$image"
  mkdir "${finalPath}"
    for playlist in $(find "${EXTERNALS_DIR}" | grep playlist.xml | sort | grep -e "${TEST_INCLUDE_LIST}" | grep -ve "${TEST_EXCLUDE_LIST}" ) ; do 
      externalTest=$(basename $(dirname "${playlist}"))
      for testCaseTag in $(cat $playlist | grep -e "<testCaseName>") ; do
        testCase=$(echo "${testCaseTag}" | sed "s;.*<testCaseName>;;g" | sed "s;</testCaseName.*;;g")
        echo "for $playlist"
        runExternalTest
      done
    done
  done
  cleanUnpackedJdk
done
if which tree > /dev/null 2>&1 ; then
  tree "${RESULTS_DIR}"
else
  ls -R "${RESULTS_DIR}"
fi
