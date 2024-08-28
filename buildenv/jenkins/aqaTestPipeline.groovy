#!groovy

def JDK_VERSIONS = params.JDK_VERSIONS.trim().split("\\s*,\\s*")
def PLATFORMS = params.PLATFORMS.trim().split("\\s*,\\s*")
def TARGETS = params.TARGETS ?: "Grinder"
TARGETS = TARGETS.trim().split("\\s*,\\s*")

def PARALLEL = params.PARALLEL ? params.PARALLEL : "Dynamic"

def NUM_MACHINES = ""
if (params.NUM_MACHINES) {
    NUM_MACHINES = params.NUM_MACHINES
} else if (!params.TEST_TIME && PARALLEL == "Dynamic") {
    // set default NUM_MACHINES to 3 if params.NUM_MACHINES and params.TEST_TIME are not set and PARALLEL is Dynamic 
    NUM_MACHINES = 3
}
def SDK_RESOURCE = params.SDK_RESOURCE ? params.SDK_RESOURCE : "releases"
def TIME_LIMIT = params.TIME_LIMIT ? params.TIME_LIMIT : 10
def AUTO_AQA_GEN = params.AUTO_AQA_GEN ? params.AUTO_AQA_GEN.toBoolean() : false
def TRSS_URL = params.TRSS_URL ? params.TRSS_URL : "https://trss.adoptium.net/"
def TEST_FLAG = (params.TEST_FLAG) ?: ""
def LIGHT_WEIGHT_CHECKOUT = params.LIGHT_WEIGHT_CHECKOUT ?: false

// Use BUILD_USER_ID if set and jdk-JDK_VERSIONS
def DEFAULT_SUFFIX = (env.BUILD_USER_ID) ? "${env.BUILD_USER_ID} - jdk-${params.JDK_VERSIONS}" : "jdk-${params.JDK_VERSIONS}"
def PIPELINE_DISPLAY_NAME = (params.PIPELINE_DISPLAY_NAME) ? "#${currentBuild.number} - ${params.PIPELINE_DISPLAY_NAME}" : "#${currentBuild.number} - ${DEFAULT_SUFFIX}"

// Set the AQA_TEST_PIPELINE Jenkins job displayName
currentBuild.setDisplayName(PIPELINE_DISPLAY_NAME)

def defaultTestTargets = "sanity.functional,extended.functional,special.functional,sanity.openjdk,extended.openjdk,sanity.system,extended.system,sanity.perf,extended.perf"
def defaultFipsTestTargets = "sanity.functional,extended.functional,sanity.openjdk,extended.openjdk,sanity.jck,extended.jck,special.jck"

JOBS = [:]
fail = false

JDK_VERSIONS.each { JDK_VERSION ->
    if (params.PLATFORMS == "release") {
        def configJson = []
        node("worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)") {
            checkout scm
            dir (env.WORKSPACE) {
                def filePath = "./aqa-tests/buildenv/jenkins/config/${params.VARIANT}/"
                filePath = filePath + "default.json"
                if (fileExists(filePath + "jdk${JDK_VERSION}.json")) {
                    filePath = filePath + "jdk${JDK_VERSION}.json"
                }
                configJson = readJSON(file: filePath)
            }
        }

        configJson.each { item ->
            def releaseTestFlag = ""
            releaseTestFlag = item.TEST_FLAG
            item.PLATFORM_TARGETS.each { pt ->
                pt.each{ p, t ->
                    def releasePlatform = p
                    def releaseTargets = ""
                    if (t.contains("defaultFipsTestTargets")) {
                        releaseTargets = t.replace("defaultFipsTestTargets","${defaultFipsTestTargets}")
                    } else {
                        releaseTargets = t.replace("defaultTestTargets","${defaultTestTargets}")
                    }
                    String[] releasePlatformArray = releasePlatform.split("\\s*,\\s*")
                    String[] releaseTargetsArray = releaseTargets.split("\\s*,\\s*")
                    generateJobs(JDK_VERSION, releaseTestFlag, releasePlatformArray, releaseTargetsArray)
                }
            }
        }
    } else {
        generateJobs(JDK_VERSION, TEST_FLAG, PLATFORMS, TARGETS)
    }
}
parallel JOBS
if (fail) {
    currentBuild.result = "FAILURE"
}

def generateJobs(jobJdkVersion, jobTestFlag, jobPlatforms, jobTargets) {
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
                // example: <jenkins_url>/job/build-scripts/job/openjdk11-pipeline/123/artifact/target/linux/aarch64/openj9/*_aarch64_linux_*.tar.gz/*zip*/openj9.zip
                download_url = params.TOP_LEVEL_SDK_URL + "artifact/target/${os}/${arch}/${params.VARIANT}/${filter}/*zip*/${params.VARIANT}.zip"
            }
        } else if (SDK_RESOURCE == "releases") {
            if (params.VARIANT == "openj9") {
                // get IBM Semeru CE
                sdk_resource_value = "customized"
                download_url="https://ibm.com/semeru-runtimes/api/v3/binary/latest/${jobJdkVersion}/ga/${os}/${arch}/jdk/openj9/normal/ibm_ce https://ibm.com/semeru-runtimes/api/v3/binary/latest/${jobJdkVersion}/ga/${os}/${arch}/testimage/openj9/normal/ibm_ce"
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
            if (params.VARIANT == "openj9") {
                // default rerunIterations is 3 for openj9
                rerunIterations = params.RERUN_ITERATIONS ? params.RERUN_ITERATIONS.toInteger() : 3
                if (TARGET.contains('external')) {
                    PARALLEL = "None"
                    rerunIterations = 0
                } else if (TARGET.contains('functional')) {
                    VENDOR_TEST_REPOS = 'git@github.ibm.com:runtimes/test.git'
                    VENDOR_TEST_BRANCHES = params.ADOPTOPENJDK_BRANCH ?: 'master'
                    VENDOR_TEST_DIRS = 'functional'
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

                if (TARGET.contains("FIPS") || (TARGET.contains("dev"))) {
                    rerunIterations = 0
                }
            } else if (params.VARIANT == "temurin") {
                if (TARGET.contains("functional") || TARGET.contains("perf")) {
                    PARALLEL = "None"
                }
            }
            echo "AUTO_AQA_GEN: ${AUTO_AQA_GEN}"
            // Grinder job has special settings and should be regenerated specifically, not via aqaTestPipeline
            if (AUTO_AQA_GEN && !TEST_JOB_NAME.contains("Grinder")) {
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
                        childParams << string(name: param.key, value: PARALLEL)
                    } else if (param.key == "NUM_MACHINES") {
                       childParams << string(name: param.key, value: NUM_MACHINES.toString())
                    } else if (param.key == "LIGHT_WEIGHT_CHECKOUT") {
                        childParams << booleanParam(name: param.key, value: LIGHT_WEIGHT_CHECKOUT.toBoolean())
                    } else if (param.key == "TIME_LIMIT") {
                        childParams << string(name: param.key, value: TIME_LIMIT.toString())
                    } else if (param.key == "TEST_FLAG") {
                        childParams << string(name: param.key, value: jobTestFlag)
                    } else if (param.key == "KEEP_REPORTDIR") {
                        childParams << booleanParam(name: param.key, value: keep_reportdir.toBoolean())
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
                childParams << string(name: "JDK_IMPL", value: jdk_impl)
                childParams << string(name: "JDK_VERSION", value: jobJdkVersion)
                childParams << string(name: "PLATFORM", value: PLATFORM)
                childParams << string(name: "RERUN_ITERATIONS", value: rerunIterations.toString())
                childParams << string(name: "VENDOR_TEST_BRANCHES", value: VENDOR_TEST_BRANCHES)
                childParams << string(name: "VENDOR_TEST_DIRS", value: VENDOR_TEST_DIRS)
                childParams << string(name: "VENDOR_TEST_REPOS", value: VENDOR_TEST_REPOS)

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