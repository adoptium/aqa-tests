#!groovy

def JDK_VERSIONS = params.JDK_VERSIONS.trim().split("\\s*,\\s*")
def PLATFORMS = params.PLATFORMS.trim().split("\\s*,\\s*")
def TARGETS = params.TARGETS ?: "Grinder"
TARGETS = TARGETS.trim().split("\\s*,\\s*")
def TEST_FLAG = (params.TEST_FLAG) ?: ""

def PARALLEL = params.PARALLEL ? params.PARALLEL : "Dynamic"

NUM_MACHINES = ""
if (params.NUM_MACHINES) {
    NUM_MACHINES = params.NUM_MACHINES
} else if (!params.TEST_TIME && PARALLEL == "Dynamic") {
    // set default NUM_MACHINES to 3 if params.NUM_MACHINES and params.TEST_TIME are not set and PARALLEL is Dynamic 
    NUM_MACHINES = 3
}

SDK_RESOURCE = params.SDK_RESOURCE ? params.SDK_RESOURCE : "releases"
TIME_LIMIT = params.TIME_LIMIT ? params.TIME_LIMIT : 10
AUTO_AQA_GEN = params.AUTO_AQA_GEN ? params.AUTO_AQA_GEN.toBoolean() : false
TRSS_URL = params.TRSS_URL ? params.TRSS_URL : "https://trss.adoptium.net/"
LIGHT_WEIGHT_CHECKOUT = params.LIGHT_WEIGHT_CHECKOUT ?: false

// Use BUILD_USER_ID if set and jdk-JDK_VERSIONS
def DEFAULT_SUFFIX = (env.BUILD_USER_ID) ? "${env.BUILD_USER_ID} - jdk-${params.JDK_VERSIONS}" : "jdk-${params.JDK_VERSIONS}"
def PIPELINE_DISPLAY_NAME = (params.PIPELINE_DISPLAY_NAME) ? "#${currentBuild.number} - ${params.PIPELINE_DISPLAY_NAME}" : "#${currentBuild.number} - ${DEFAULT_SUFFIX}"

// Set the AQA_TEST_PIPELINE Jenkins job displayName
currentBuild.setDisplayName(PIPELINE_DISPLAY_NAME)

defaultTestTargets = "sanity.functional,extended.functional,special.functional,sanity.openjdk,extended.openjdk,special.openjdk,sanity.system,extended.system,special.system,sanity.perf,extended.perf,sanity.jck,extended.jck,special.jck"
defaultFipsTestTargets = "extended.functional,sanity.openjdk,extended.openjdk,sanity.jck,extended.jck,special.jck"
// There is no applicable tests for FIPS140-2 extended.functional atm, so temporarily disable FIPS140-2 extended.functional
defaultFips140_2TestTargets = defaultFipsTestTargets.replace("extended.functional,", "")

if (params.BUILD_TYPE == "nightly") {
    defaultTestTargets = "sanity.functional,extended.functional,sanity.openjdk,extended.openjdk,sanity.perf,sanity.jck,sanity.system,special.system"
}

JOBS = [:]
fail = false

timestamps {
    JDK_VERSIONS.each { JDK_VERSION ->
        if (params.BUILD_TYPE == "release" || params.BUILD_TYPE == "nightly" || params.BUILD_TYPE == "weekly") {
            def configJson = []
            if (params.CONFIG_JSON) {
                echo "Read JSON from CONFIG_JSON parameter..."
                configJson = readJSON text: "${params.CONFIG_JSON}"
            } else {
                node("worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)") {
                    checkout scm
                    dir (env.WORKSPACE) {
                        def filePath = "./aqa-tests/buildenv/jenkins/config/${params.VARIANT}/${params.BUILD_TYPE}/"
                        filePath = filePath + "default.json"
                        if (fileExists(filePath + "jdk${JDK_VERSION}.json")) {
                            filePath = filePath + "jdk${JDK_VERSION}.json"
                        }
                        echo "Read JSON from file ${filePath}..."
                        configJson = readJSON(file: filePath)
                    }
                }
            }

            configJson.each { item ->
                def releaseTestFlag = item.TEST_FLAG
                item.PLATFORM_TARGETS.each { pt ->
                    pt.each { p, t ->
                        // When the AQA Test Pipeline is triggered by an upstream pipeline at runtime, we only receive the SDK URL for a single platform at a time.
                        // if params.PLATFORMS is set, only trigger testing for the platform that is specified
                        if (params.PLATFORMS) {
                            if (params.PLATFORMS.contains(p)) {
                                echo "Only triggering test builds specified in PLATFORMS: ${params.PLATFORMS}..."
                                generateJobs(JDK_VERSION, releaseTestFlag, p, t, PARALLEL)
                            }
                        } else {
                            generateJobs(JDK_VERSION, releaseTestFlag, p, t, PARALLEL)
                        }
                    }
                }
            }
        } else {
            generateJobs(JDK_VERSION, TEST_FLAG, PLATFORMS, TARGETS, PARALLEL)
        }
    }
    parallel JOBS
    if (fail) {
        currentBuild.result = "FAILURE"
    }
}

def generateJobs(jobJdkVersion, jobTestFlag, jobPlatforms, jobTargets, jobParallel) {
    if (jobTargets instanceof String) {
        if (jobTargets.contains("defaultFips")) {
            jobTargets = jobTargets.replace("defaultFipsTestTargets","${defaultFipsTestTargets}")
            jobTargets = jobTargets.replace("defaultFips140_2TestTargets","${defaultFips140_2TestTargets}")
        } else {
            jobTargets = jobTargets.replace("defaultTestTargets","${defaultTestTargets}")
        }
       jobTargets = jobTargets.split("\\s*,\\s*")
    }
    if (jobPlatforms instanceof String) {
        jobPlatforms = jobPlatforms.split("\\s*,\\s*")
    }

    echo "jobJdkVersion: ${jobJdkVersion}, jobTestFlag: ${jobTestFlag}, jobPlatforms: ${jobPlatforms}, jobTargets: ${jobTargets}, jobParallel: ${jobParallel}"
    if (jobTestFlag == "NONE") {
        jobTestFlag = ""
    }
    jobPlatforms.each { PLATFORM ->
        String[] tokens = PLATFORM.split('_')
        def os = tokens[1];
        def arch = tokens[0];
        if (arch.contains("x86-64")){
            arch = "x64"
        } else if (arch.contains("x86-32")) {
            arch ="x86-32"
        }

        def filter = "*.tar.gz"
        if (os.contains("windows")) {
            filter = "*.zip"
        }
        def short_name = "hs"
        def jdk_impl = "hotspot"
        if (params.VARIANT == "openj9") {
            short_name = "j9"
            jdk_impl = params.VARIANT
        }
        def download_url = params.CUSTOMIZED_SDK_URL ? params.CUSTOMIZED_SDK_URL : ""
        def sdk_resource_value = SDK_RESOURCE

        if (SDK_RESOURCE == "customized" ) {
            if (params.TOP_LEVEL_SDK_URL) {
                def url = params.TOP_LEVEL_SDK_URL
                if (!url.endsWith("/")) {
                    url = "${params.TOP_LEVEL_SDK_URL}/"
                }
                // example: <jenkins_url>/job/build-scripts/job/openjdk11-pipeline/123/artifact/target/linux/aarch64/openj9/*_aarch64_linux_*.tar.gz/*zip*/openj9.zip
                download_url = "${url}artifact/target/${os}/${arch}/${params.VARIANT}/${filter}/*zip*/${params.VARIANT}.zip"
            }
        }
        echo "download_url: ${download_url}"

        jobTargets.each { TARGET ->
            def TEST_JOB_NAME = "Grinder"
            if (TARGET.contains("Grinder")) {
                TEST_JOB_NAME = TARGET
            } else {
                def suffix = ""
                if (jobTestFlag) {
                    suffix = "_" + jobTestFlag.toLowerCase().trim()
                }
                TEST_JOB_NAME = "Test_openjdk${jobJdkVersion}_${short_name}_${TARGET}_${PLATFORM}${suffix}"
            }
            echo "TEST_JOB_NAME: ${TEST_JOB_NAME}"

            def keep_reportdir = false
            if (TARGET.contains("functional") || TARGET.contains("jck") || TARGET.contains("openjdk") || TARGET.contains("osb")) {
                keep_reportdir = true
            }

            def DYNAMIC_COMPILE = false
            if (!params.DYNAMIC_COMPILE) {
                if (("${TARGET}".contains('functional')) || ("${TARGET}".contains('external'))) {
                    DYNAMIC_COMPILE = true
                } else {
                    DYNAMIC_COMPILE = false
                }
            } else {
                DYNAMIC_COMPILE = params.DYNAMIC_COMPILE ? params.DYNAMIC_COMPILE.toBoolean() : false
            }

            def VENDOR_TEST_REPOS = ''
            def VENDOR_TEST_BRANCHES = ''
            def VENDOR_TEST_DIRS = ''
            int rerunIterations = params.RERUN_ITERATIONS ? params.RERUN_ITERATIONS.toInteger() : 0
            def buildList = params.BUILD_LIST ?: ""
            if (params.VARIANT == "openj9") {
                // default rerunIterations is 3 for openj9
                rerunIterations = params.RERUN_ITERATIONS ? params.RERUN_ITERATIONS.toInteger() : 3
                if (TARGET.contains('external')) {
                    jobParallel = "None"
                    rerunIterations = 0
                } else if (TARGET.contains('functional')) {
                    if (jobTestFlag.contains("FIPS")) {
                        if (!buildList) {
                            buildList = "functional/OpenJcePlusTests,functional/security"
                        }
                    } else {
                        VENDOR_TEST_REPOS = 'git@github.ibm.com:runtimes/test.git'
                        // Default VENDOR_TEST_BRANCHES is master. 
                        // If offical adoptium repo is used, set VENDOR_TEST_BRANCHES to match with params.ADOPTOPENJDK_BRANCH.
                        VENDOR_TEST_BRANCHES = 'master'
                        if (params.ADOPTOPENJDK_REPO && params.ADOPTOPENJDK_REPO.contains("adoptium/aqa-tests")) {
                            VENDOR_TEST_BRANCHES = params.ADOPTOPENJDK_BRANCH ?: 'master'
                        }
                        VENDOR_TEST_DIRS = 'functional'
                    }
                } else if (TARGET.contains('jck')) {
                    VENDOR_TEST_REPOS = 'git@github.ibm.com:runtimes/jck.git'
                    VENDOR_TEST_BRANCHES = "main"
                    VENDOR_TEST_DIRS = 'jck'
                } else if (TARGET.contains('openjdk')) {
                    // only use osb repo for regular testing
                    if (TARGET.contains('special') && jobTestFlag == "") {
                        VENDOR_TEST_REPOS = 'git@github.ibm.com:runtimes/osb-tests.git'
                        VENDOR_TEST_BRANCHES = "ibm_tlda"
                        VENDOR_TEST_DIRS = 'openjdk'
                    }
                }

                if (jobTestFlag.contains("FIPS") || (TARGET.contains("dev"))) {
                    rerunIterations = 0
                }
            } else if (params.VARIANT == "temurin") {
                if (TARGET.contains("functional") || TARGET.contains("perf")) {
                    jobParallel = "None"
                }
            }
            // Grinder job has special settings and should be regenerated specifically, not via aqaTestPipeline
            if (AUTO_AQA_GEN.toBoolean() && !TEST_JOB_NAME.contains("Grinder")) {
                String[] targetTokens = TARGET.split("\\.")
                def level = targetTokens[0];
                def group = targetTokens[1];
                def parameters = [
                    string(name: 'TEST_JOB_NAME', value: TEST_JOB_NAME),
                    string(name: 'LEVELS', value: level),
                    string(name: 'GROUPS', value: group),
                    string(name: 'JDK_VERSIONS', value: jobJdkVersion),
                    string(name: 'ARCH_OS_LIST', value: PLATFORM),
                    string(name: 'JDK_IMPL', value: jdk_impl),
                    booleanParam(name: 'LIGHT_WEIGHT_CHECKOUT', value: LIGHT_WEIGHT_CHECKOUT)
                ]
                build job: 'Test_Job_Auto_Gen', parameters: parameters, propagate: true
            }

            def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper
            if (JobHelper.jobIsRunnable(TEST_JOB_NAME as String)) {
                def childParams = []
                // loop through all the params and change the parameters if needed
                params.each { param ->
                    if ( param.key == "PLATFORMS" || param.key == "TARGETS" || param.key == "TOP_LEVEL_SDK_URL" 
                    || param.key == "AUTO_AQA_GEN" || param.key == "JDK_VERSIONS" || param.key == "VARIANT" || param.key == "PIPELINE_DISPLAY_NAME") {
                        // do not need to pass the param to child jobs
                    } else if (param.key == "SDK_RESOURCE") {
                        childParams << string(name: param.key, value: sdk_resource_value)
                    } else if (param.key == "CUSTOMIZED_SDK_URL") {
                        childParams << string(name: param.key, value: download_url)
                    } else if (param.key == "PARALLEL") {
                        childParams << string(name: param.key, value: jobParallel)
                    } else if (param.key == "NUM_MACHINES") {
                       childParams << string(name: param.key, value: NUM_MACHINES.toString())
                    } else if (param.key == "LIGHT_WEIGHT_CHECKOUT") {
                        childParams << booleanParam(name: param.key, value: LIGHT_WEIGHT_CHECKOUT.toBoolean())
                    } else if (param.key == "TIME_LIMIT") {
                        childParams << string(name: param.key, value: TIME_LIMIT.toString())
                    } else {
                        def value = param.value.toString()
                        if (value == "true" || value == "false") {
                            childParams << booleanParam(name: param.key, value: value.toBoolean())
                        } else {
                            childParams << string(name: param.key, value: value)
                        }
                    }
                }
                childParams << booleanParam(name: "DYNAMIC_COMPILE", value: DYNAMIC_COMPILE.toBoolean())
                childParams << booleanParam(name: "GENERATE_JOBS", value: AUTO_AQA_GEN.toBoolean())
                childParams << booleanParam(name: "KEEP_REPORTDIR", value: keep_reportdir.toBoolean())
                childParams << string(name: "JDK_IMPL", value: jdk_impl)
                childParams << string(name: "JDK_VERSION", value: jobJdkVersion)
                childParams << string(name: "PLATFORM", value: PLATFORM)
                childParams << string(name: "RERUN_ITERATIONS", value: rerunIterations.toString())
                childParams << string(name: "TEST_FLAG", value: jobTestFlag)
                childParams << string(name: "VENDOR_TEST_BRANCHES", value: VENDOR_TEST_BRANCHES)
                childParams << string(name: "VENDOR_TEST_DIRS", value: VENDOR_TEST_DIRS)
                childParams << string(name: "VENDOR_TEST_REPOS", value: VENDOR_TEST_REPOS)
                if (buildList) {
                    childParams << string(name: "BUILD_LIST", value: buildList)
                }

                int jobNum = JOBS.size() + 1
                JOBS["${TEST_JOB_NAME}_${jobNum}"] = {
                    def downstreamJob = build job: TEST_JOB_NAME, parameters: childParams, propagate: false, wait: true
                    def downstreamJobResult = downstreamJob.getResult()
                    echo "${TEST_JOB_NAME} result is ${downstreamJobResult}"
                    if (downstreamJobResult == 'SUCCESS' || downstreamJobResult == 'UNSTABLE') {
                        echo "[NODE SHIFT] MOVING INTO CONTROLLER NODE..."
                        node("worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)") {
                            cleanWs disableDeferredWipeout: true, deleteDirs: true
                            try {
                                timeout(time: 2, unit: 'HOURS') {
                                    copyArtifacts(
                                        projectName: TEST_JOB_NAME,
                                        selector:specific("${downstreamJob.getNumber()}"),
                                        filter: "**/${TEST_JOB_NAME}*.tap",
                                        fingerprintArtifacts: true,
                                        flatten: true
                                    )
                                }
                            } catch (Exception e) {
                                echo 'Exception: ' + e.toString()
                                echo "Cannot run copyArtifacts from job ${TEST_JOB_NAME}. Skipping copyArtifacts..."
                            }
                            try {
                                timeout(time: 1, unit: 'HOURS') {
                                    archiveArtifacts artifacts: "*.tap", fingerprint: true
                                }
                            } catch (Exception e) {
                                echo 'Exception: ' + e.toString()
                                echo "Cannot archiveArtifacts from job ${TEST_JOB_NAME}. "
                            }
                        }
                    }
                    if (downstreamJobResult != "SUCCESS") {
                        fail = true
                    }
                }
            } else {
                println "Requested test job that does not exist or is disabled: ${TEST_JOB_NAME}. \n To generate the job, pelase set AUTO_AQA_GEN = true"
                fail = true
            }
        }
    }
}