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

/**
 * A template that defines a test job.
 */

if (!binding.hasVariable('ARCH_OS')) ARCH_OS = "x86-64_linux"
if (!binding.hasVariable('SUFFIX')) SUFFIX = '_personal'
if (!binding.hasVariable('JDK_VERSION')) JDK_VERSION = '8'
if (!binding.hasVariable('BUILDS_TO_KEEP')) {
	BUILDS_TO_KEEP = 10
} else {
	BUILDS_TO_KEEP = BUILDS_TO_KEEP.toInteger()
}

ROOTFOLDER = "test${SUFFIX}build"
folder("$ROOTFOLDER")
folder("$ROOTFOLDER/jobs")
folder("$ROOTFOLDER/jobs/$JDK_VERSION") {
	description('Automatically generated test jobs.')
}

pipelineJob("$ROOTFOLDER/jobs/$JDK_VERSION/$JOB_NAME") {
	description('<h1>THIS IS AN AUTOMATICALLY GENERATED JOB. PLEASE DO NOT MODIFY, IT WILL BE OVERWRITTEN.</h1><p>This job is defined in JobTemplate.groovy in the https://github.com/AdoptOpenJDK/openjdk-tests repo. If you wish to change the job, please modify JobTemplate.groovy script.</p>')
	definition {
		parameters {
			stringParam('ADOPTOPENJDK_REPO', "https://github.com/AdoptOpenJDK/openjdk-tests.git", "AdoptOpenJDK git repo. Please use ssh for zos.")
			stringParam('ADOPTOPENJDK_BRANCH', "master", "AdoptOpenJDK branch")
			stringParam('OPENJ9_REPO', "https://github.com/eclipse/openj9.git", "OpenJ9 git repo. Please use ssh for zos.")
			stringParam('OPENJ9_BRANCH', "master", "OpenJ9 branch")
			stringParam('OPENJ9_SHA', "", "OpenJ9 sha")
			stringParam('JDK_VERSION', "8", "JDK version. i.e., 8, 11")
			stringParam('JDK_IMPL', "openj9", "JDK implementation, e.g. hotspot, openj9, sap")
			stringParam('BUILD_LIST', "openjdk", "Specific test directory to compile, set blank for all projects to be compiled")
			choiceParam('SDK_RESOURCE', ['nightly', 'upstream', 'customized', 'releases'], "Where to get sdk?")
			choiceParam('TEST_FLAG', ['', 'JITAAS', 'AOT'], "Optional. Only set to JITAAS/AOT for JITAAS/AOT feature testing.")
			stringParam('EXTRA_OPTIONS', "", "Use this to append options to the test command")
			stringParam('JVM_OPTIONS', "", "Use this to replace the test original command line options")
			stringParam('ITERATIONS', "1", "Number of times to repeat execution of test target")
			stringParam('JCK_GIT_REPO', "", "For JCK test only")
			stringParam('SSH_AGENT_CREDENTIAL', "", "Optional. Only use when ssh credentials are needed")
			booleanParam('KEEP_WORKSPACE', false, "Keep workspace on the machine")
			stringParam('ARTIFACTORY_SERVER', "", "Optional. Default is to upload test output (failed build) onto artifactory only. By unset this value, test output will be archived to Jenkins")
			stringParam('ARTIFACTORY_REPO', "", "Optional. It should be used with ARTIFACTORY_SERVER")
			stringParam('ARTIFACTORY_ROOT_DIR', "", "Optional. It should be used with ARTIFACTORY_SERVER and ARTIFACTORY_REPO. Default is to set root dir to be the same as the current Jenkins domain")
			booleanParam('PERSONAL_BUILD', true, "Is this a personal build?")
			booleanParam('IS_PARALLEL', false, "Should tests run in parallel?")
			stringParam('USER_CREDENTIALS_ID', "", "Optional. User credential ID")
			stringParam('VENDOR_TEST_REPOS', "", "Optional. Addtional test repos")
			stringParam('VENDOR_TEST_BRANCHES', "", "Optional. Addtional test branches")
			stringParam('VENDOR_TEST_SHAS', "", "Optional. Addtional test shas")
			stringParam('VENDOR_TEST_DIRS', "", "Optional. Addtional test dirs")
			booleanParam('KEEP_REPORTDIR', true, "Keep the test report dir if the test passes?")
			stringParam('BUILD_IDENTIFIER', "", "build identifier")
			booleanParam('AUTO_DETECT', true, "Optional. Default is to enable AUTO_DETECT")
			stringParam('TIME_LIMIT', "", "time limit")
		}
		cpsScm {
			scm {
				git {
					remote {
						url("${ADOPTOPENJDK_REPO}")
					}
					branch("${ADOPTOPENJDK_BRANCH}")
					extensions {
						relativeTargetDirectory('openjdk-tests')
						cleanBeforeCheckout()
						pruneStaleBranch()
					}
				}
				scriptPath("buildenv/jenkins/openjdk_${ARCH_OS}")
				lightweight(true)
			}
		}
		logRotator {
			numToKeep(BUILDS_TO_KEEP)
			artifactNumToKeep(BUILDS_TO_KEEP)
		}
	}
}

listView("test$SUFFIX") {
	jobs {
		name("$ROOTFOLDER/jobs/$JDK_VERSION")
		regex("Test_openjdk.+$SUFFIX")
	}
	recurse(true)
	columns {
		status()
		weather()
		name()
		lastSuccess()
		lastFailure()
		lastDuration()
		buildButton()
	}
}
