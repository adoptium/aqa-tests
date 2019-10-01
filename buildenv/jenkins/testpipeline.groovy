#!groovy

/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

// Initialize all PARAMETERS (params) to Groovy Variables even if they are not passed
echo "Initialize all PARAMETERS..."

// TO DO use ARCH_OS instead of Jenkinsfiles params.ARCH_OS? params.ARCH_OS : 'x86-64_linux'
ADOPTOPENJDK_REPO = params.ADOPTOPENJDK_REPO ? params.ADOPTOPENJDK_REPO : "https://github.com/AdoptOpenJDK/openjdk-tests.git"
ADOPTOPENJDK_BRANCH = params.ADOPTOPENJDK_BRANCH ? params.ADOPTOPENJDK_BRANCH : "master"
OPENJ9_REPO = params.OPENJ9_REPO ? params.OPENJ9_REPO : "https://github.com/eclipse/openj9.git"
OPENJ9_BRANCH = params.OPENJ9_BRANCH ? params.OPENJ9_BRANCH : "master"
CUSTOM_TARGET = params.CUSTOM_TARGET ? params.CUSTOM_TARGET : ""
JCK_GIT_REPO = params.JCK_GIT_REPO ? params.JCK_GIT_REPO : ""
SSH_AGENT_CREDENTIAL = params.SSH_AGENT_CREDENTIAL ? params.SSH_AGENT_CREDENTIAL : ""
OPENJ9_SHA = params.OPENJ9_SHA ? params.OPENJ9_SHA : ""
PERSONAL_BUILD = params.PERSONAL_BUILD? params.PERSONAL_BUILD : true
USER_CREDENTIALS_ID = params.USER_CREDENTIALS_ID ? params.USER_CREDENTIALS_ID : ""
//env.TEST_JDK_HOME = "$WORKSPACE/openjdkbinary/j2sdk-image"
JRE_IMAGE = params.JRE_IMAGE ? params.JRE_IMAGE : ""
JDK_VERSIONS = params.JDK_VERSIONS ? params.JDK_VERSIONS : "8"
JVM_OPTIONS = params.JVM_OPTIONS ? params.JVM_OPTIONS : ""
EXTRA_OPTIONS = params.EXTRA_OPTIONS ? params.EXTRA_OPTIONS : ""
TARGET = params.TARGET ? params.TARGET : 'jdk_custom'
EXTRA_DOCKER_ARGS = params.EXTRA_DOCKER_ARGS ? params.EXTRA_DOCKER_ARGS : ""
//env.SPEC = "${SPEC}"
TEST_FLAG = params.TEST_FLAG ? params.TEST_FLAG : ''
KEEP_REPORTDIR = params.KEEP_REPORTDIR ? params.KEEP_REPORTDIR : true
SDK_RESOURCE = params.SDK_RESOURCE ? params.SDK_RESOURCE : "nightly"
AUTO_DETECT = params.AUTO_DETECT ? params.AUTO_DETECT : true
PERF_ROOT = params.PERF_ROOT ? params.PERF_ROOT : ''
ITERATIONS = params.ITERATIONS ? params.ITERATIONS : "1"
JCK_ROOT = params.JCK_ROOT ? params.JCK_ROOT : ''
JCK_GIT_REPO = params.JCK_GIT_REPO ? params.JCK_GIT_REPO : ''
DOCKERIMAGE_TAG = params.DOCKERIMAGE_TAG ? params.DOCKERIMAGE_TAG : ''
//JENKINS_FILE = params.JenkinsFile ? params.JenkinsFile : ""
IS_PARALLEL = params.IS_PARALLEL? params.IS_PARALLEL : false
JENKINSFILES = params.JENKINSFILES? params.JENKINSFILES : 'openjdk_x86-64_linux'
BUILD_LIST = params.BUILD_LIST? params.BUILD_LIST : 'openjdk'
JDK_IMPLS = params.JDK_IMPLS? params.JDK_IMPLS : 'hotspot'
//ARCH_OS_LIST = ARCH_OS_LIST.split(',')
echo "jenkinsfile is ${JENKINSFILES}"
JENKINSFILE_LIST = JENKINSFILES.split(',')
def getARCH_OS = { s -> s.replace('openjdk_','') }
ARCH_OS_LIST = JENKINSFILE_LIST.collect{getARCH_OS it}
echo " arch_os is ${ARCH_OS_LIST.toString()}"
JDK_VERSIONS = JDK_VERSIONS.split(',')
JDK_IMPLS = JDK_IMPLS.split(',')
TestJobs = [:]

TIME_LIMIT =  params.TIME_LIMIT ? params.TIME_LIMIT.toInteger() : 10

node('master') {
	timeout(time: TIME_LIMIT, unit: 'HOURS') {
		timestamps {
			if (PERSONAL_BUILD) {
				// update build name if personal build
				wrap([$class: 'BuildUser']) {
					currentBuild.displayName = "#${BUILD_NUMBER} - ${BUILD_USER_EMAIL}"
				}
				//SUFFIX = nightly, releases, personal, prtest
				SUFFIX = '_personal'
			}
			checkout scm
			ARCH_OS_LIST.each { ARCH_OS ->
				JDK_VERSIONS.each { JDK_VERSION ->
					JDK_IMPLS.each { JDK_IMPL ->
						def TEST_JOB_NAME = "Test_openjdk${JDK_VERSION}_${JDK_IMPL}_${ARCH_OS}${SUFFIX}"
						TestJobs["${TEST_JOB_NAME}"] = {
							echo "create the job ${TEST_JOB_NAME}"
							createJob(TEST_JOB_NAME, JDK_VERSION, ARCH_OS, SUFFIX)
							echo "runing the job $TEST_JOB_NAME}"
							buildJob(TEST_JOB_NAME, ADOPTOPENJDK_REPO, ADOPTOPENJDK_BRANCH, OPENJ9_REPO, 
							OPENJ9_BRANCH, CUSTOM_TARGET, JCK_GIT_REPO, SSH_AGENT_CREDENTIAL, 
							OPENJ9_SHA, PERSONAL_BUILD, USER_CREDENTIALS_ID, 
							JRE_IMAGE, JDK_VERSION, JDK_IMPL, JVM_OPTIONS, EXTRA_OPTIONS, 
							EXTRA_DOCKER_ARGS, TEST_FLAG, KEEP_REPORTDIR, SDK_RESOURCE, 
							AUTO_DETECT, ITERATIONS, JCK_ROOT, DOCKERIMAGE_TAG, 
							IS_PARALLEL, BUILD_LIST, TARGET)
						}
					}
				}
			}
			parallel TestJobs
		}
	}
}

def createJob( JOB_NAME, JDK_VERSION, ARCH_OS, SUFFIX ) {
    def params = [:]
	params.put('ARCH_OS', ARCH_OS)
	params.put('JOB_NAME', JOB_NAME)
	params.put('SUFFIX', SUFFIX)
	params.put('JDK_VERSION', JDK_VERSION)
    def templatePath = 'openjdk-tests/buildenv/jenkins/JobTemplate.groovy'

    create = jobDsl targets: templatePath, ignoreExisting: false, additionalParameters: params
    return create
}

def buildJob(JOB_NAME, ADOPTOPENJDK_REPO, ADOPTOPENJDK_BRANCH, OPENJ9_REPO, 
								OPENJ9_BRANCH, CUSTOM_TARGET, JCK_GIT_REPO, SSH_AGENT_CREDENTIAL, 
								OPENJ9_SHA, PERSONAL_BUILD, USER_CREDENTIALS_ID, 
								JRE_IMAGE, JDK_VERSION, JDK_IMPL, JVM_OPTIONS, EXTRA_OPTIONS, 
								EXTRA_DOCKER_ARGS, TEST_FLAG, KEEP_REPORTDIR, SDK_RESOURCE, 
								AUTO_DETECT, ITERATIONS, JCK_ROOT, DOCKERIMAGE_TAG, IS_PARALLEL, 
								BUILD_LIST, TARGET) 
	{
    stage ("${JOB_NAME}") {
        JOB = build job: "test${SUFFIX}build/jobs/$JDK_VERSION/$JOB_NAME",
                parameters: [
                    string(name: 'ADOPTOPENJDK_REPO', value: ADOPTOPENJDK_REPO),
                    string(name: 'ADOPTOPENJDK_BRANCH', value: ADOPTOPENJDK_BRANCH),
                    string(name: 'OPENJ9_REPO', value: OPENJ9_REPO),
                    string(name: 'OPENJ9_BRANCH', value: OPENJ9_BRANCH),
                    string(name: 'CUSTOM_TARGET', value: CUSTOM_TARGET),
                    string(name: 'JCK_GIT_REPO', value: JCK_GIT_REPO),
                    string(name: 'SSH_AGENT_CREDENTIAL', value: SSH_AGENT_CREDENTIAL),
                    string(name: 'OPENJ9_SHA', value: OPENJ9_SHA),
                    booleanParam(name: 'PERSONAL_BUILD', value: PERSONAL_BUILD),
                    string(name: 'USER_CREDENTIALS_ID', value: USER_CREDENTIALS_ID),
                    string(name: 'JRE_IMAGE', value: JRE_IMAGE),
                    string(name: 'JDK_VERSION', value: JDK_VERSION),
                    string(name: 'JDK_IMPL', value: JDK_IMPL),
                    string(name: 'JVM_OPTIONS', value: JVM_OPTIONS),
                    string(name: 'EXTRA_OPTIONS', value: EXTRA_OPTIONS),
                    string(name: 'EXTRA_DOCKER_ARGS', value: EXTRA_DOCKER_ARGS),
                    string(name: 'TEST_FLAG', value: TEST_FLAG),
                    booleanParam(name: 'KEEP_REPORTDIR', value: KEEP_REPORTDIR),
                    string(name: 'SDK_RESOURCE', value: SDK_RESOURCE),
                    booleanParam(name: 'AUTO_DETECT', value: AUTO_DETECT),
                    string(name: 'ITERATIONS', value: ITERATIONS),
                    string(name: 'JCK_ROOT', value: JCK_ROOT),
                    booleanParam(name: 'IS_PARALLEL', value: IS_PARALLEL),
                    string(name: 'BUILD_LIST', value: BUILD_LIST),
                    string(name: 'TARGET', value: TARGET)]
        return JOB
    }
}
