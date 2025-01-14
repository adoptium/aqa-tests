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

def UPSTREAM_REPO = params.UPSTREAM_REPO.trim()
def UPSTREAM_BRANCH = params.UPSTREAM_BRANCH.trim()
def VERSION = params.VERSION.trim()
def BUILD_TYPES = params.BUILD_TYPES ? params.BUILD_TYPES : "release"
// BUILD_TYPES = BUILD_TYPES.trim().split("\\s*,\\s*")

def TEST_TARGETS = params.TEST_TARGETSTARGETS ?: "sanity.openjdk,extended.openjdk"
// TEST_TARGETS = TEST_TARGETS.trim().split("\\s*,\\s*")
def USE_PR_BUILD = params.USE_PR_BUILD ?: false
def EXTRA_OPTIONS = params.EXTRA_OPTIONS.trim()

def PIPELINE_DISPLAY_NAME = "${env.BUILD_USER_ID} - ${VERSION} - ${UPSTREAM_BRANCH} "
currentBuild.setDisplayName(PIPELINE_DISPLAY_NAME)

JOBS = [:]
fail = false
generateJobs(VERSION, UPSTREAM_REPO, UPSTREAM_BRANCH, PLATFORMS, TEST_TARGETS)
parallel JOBS
if (fail) {
    currentBuild.result = "FAILURE"
}

def generateJobs(jobJdkVersion, upstreamRepo, upstreamBranch, jobPlatforms, testTargets) {
    echo "jobJdkVersion: ${jobJdkVersion}, upstreamRepo: ${upstreamRepo}, upstreamBranch: ${upstreamBranch}, jobPlatforms: ${jobPlatforms}, testTargets: ${testTargets}"
    def JOB_NAME = "trestle-openjdk${jobJdkVersion}-pipeline"   
    echo "JOB_NAME: ${JOB_NAME}"

    // add some code to convert the comma-separated list of PLATFORMS into the json style targetConfigurations parameter

    // add code to update weeklyDefault in defaultsJson with testTargets info

    // add code to pass through the BUILD_TYPES - trestle pipelines need to be updated, only handle release type atm

    // add code to pull VERSION from upstreamRepo if VERSION not supplied (if supplied, check if its compatible with repo version)

    def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper
          if (JobHelper.jobIsRunnable(JOB_NAME as String)) {
               def childParams = []
               // most parameters have defaults, we customize only the following ones
               // childParams << string(name: "targetConfigurations", value: configJson)
               // childParams << string(name: "defaultsJson", value: ourDefaults)
               def addBldArgs = "-r ${upstreamRepo} -b ${upstreamBranch} --disable-adopt-branch-safety"
               echo "additionalBuildArgs: ${addBldArgs}"
               childParams << string(name: "additionalBuildArgs", value: ${addBldArgs})
               
               JOBS["${JOB_NAME}"] = {
                    def trestleJob = build job: JOB_NAME, parameters: childParams, propagate: false, wait: true
                    def trestleJobResult = trestleJob.getResult()
                    echo "${JOB_NAME} result is ${trestleJobResult}"
                    if (trestleJobResult == 'SUCCESS' || trestleJobResult == 'UNSTABLE') {
                        echo "[NODE SHIFT] MOVING INTO CONTROLLER NODE..."
                        node("worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)") {
                            cleanWs disableDeferredWipeout: true, deleteDirs: true
                            //try {
                            //    timeout(time: 2, unit: 'HOURS') {
                            //        copyArtifacts(
                            //            projectName: JOB_NAME,
                            //            selector:specific("${trestleJob.getNumber()}"),
                            //            filter: "**/${JOB_NAME}*.tap",
                            //            fingerprintArtifacts: true,
                            //            flatten: true
                            //        )
                            //    }
                            //} catch (Exception e) {
                            //    echo 'Exception: ' + e.toString()
                            //    echo "Cannot run copyArtifacts from job ${JOB_NAME}. Skipping copyArtifacts..."
                            //}
                            //try {
                            //    timeout(time: 1, unit: 'HOURS') {
                            //        archiveArtifacts artifacts: "*.tap", fingerprint: true
                            //    }
                            //} catch (Exception e) {
                            //    echo 'Exception: ' + e.toString()
                            //    echo "Cannot archiveArtifacts from job ${JOB_NAME}. "
                            //}
                        }
                    }
                    if (trestleJobResult != "SUCCESS") {
                        fail = true
                    }
                }
            } else {
                println "Requested test job that does not exist or is disabled: ${JOB_NAME}. \n To generate the job, pelase set AUTO_AQA_GEN = true"
                fail = true
            }
}

