#!groovy

def testParams = []
def baselineParams = []
int PERF_ITERATIONS = params.PERF_ITERATIONS ? params.PERF_ITERATIONS.toInteger() : 4
boolean RUN_BASELINE = (params.RUN_BASELINE != null) ? params.RUN_BASELINE.toBoolean() : true
// loop through all the params and change the parameters if needed
params.each { param ->
    if (param.key == "BASELINE_SDK_RESOURCE") {
        baselineParams << string(name: "SDK_RESOURCE", value: "${BASELINE_SDK_RESOURCE}")
    } else if (param.key == "BASELINE_SDK_URL" ) {
        baselineParams << string(name: "CUSTOMIZED_SDK_URL", value: "${BASELINE_SDK_URL}")
    } else if (param.key == "BASELINE_SDK_URL_CREDENTIAL_ID") {
        baselineParams << string(name: "CUSTOMIZED_SDK_URL_CREDENTIAL_ID", value: "${BASELINE_SDK_URL_CREDENTIAL_ID}")
    } else if (param.key == "TEST_SDK_RESOURCE") {
        testParams << string(name: "SDK_RESOURCE", value: "${TEST_SDK_RESOURCE}")
    } else if (param.key == "TEST_SDK_URL" ) {
        testParams << string(name: "CUSTOMIZED_SDK_URL", value: "${TEST_SDK_URL}")
    } else if (param.key == "TEST_SDK_URL_CREDENTIAL_ID") {
        testParams << string(name: "CUSTOMIZED_SDK_URL_CREDENTIAL_ID", value: "${TEST_SDK_URL_CREDENTIAL_ID}")
    } else if (param.key == "CUSTOMIZED_SDK_URL_CREDENTIAL_ID") {
        // do nothing
    } else {
        def value = param.value.toString()
        if (value == "true" || value == "false") {
            testParams << booleanParam(name: param.key, value: value.toBoolean())
            baselineParams << booleanParam(name: param.key, value: value.toBoolean())
        } else {
            testParams << string(name: param.key, value: value)
            baselineParams << string(name: param.key, value: value)
        }
    }
}

// read JSON from perfConfig file
def perfConfigJson = []
node("ci.role.test&&hw.arch.x86&&sw.os.linux") {
    checkout scm

    dir (env.WORKSPACE) {
        def subdir = params.JDK_IMPL ?: "hotspot"
        if (params.JDK_IMPL == "ibm") {
            subdir = "openj9"
        }
        def filePath = "./aqa-tests/perf/config/${subdir}/"
        // If vendor repo and branch is set, use vendor repo perfConfigJson file.
        if (params.VENDOR_TEST_REPOS && params.VENDOR_TEST_BRANCHES) {
            def vendorRepoDir = "vendorRepo"
            def statusCode = -1
            sshagent (credentials: ["$params.USER_CREDENTIALS_ID"], ignoreMissing: true) {
                statusCode =  sh returnStatus: true, script: """
                    git clone -q --depth 1 -b ${params.VENDOR_TEST_BRANCHES} ${params.VENDOR_TEST_REPOS} ${vendorRepoDir}
                """
            }
            if (statusCode == 0) {
                filePath = "./${vendorRepoDir}/perf/config/${subdir}/"
            } else {
                assert false: "Cannot git clone -b ${params.VENDOR_TEST_BRANCHES} ${params.VENDOR_TEST_REPOS}. Status code: ${statusCode}"
            }
        }
        filePath = filePath + "perfConfig.json"
        echo "Read JSON from file ${filePath}..."
        perfConfigJson = readJSON(file: "${filePath}")
    }
}

// loop throught the json config and update the parameters
timestamps {
    perfConfigJson.each { item ->
        if (params.BENCHMARK == item.BENCHMARK){
            def target = item.TARGET
            def buildList = item.BUILD_LIST

            testParams << string(name: "TARGET", value:"${target}")
            baselineParams << string(name: "TARGET", value:"${target}")

            testParams << string(name: "BUILD_LIST", value:"${buildList}")
            baselineParams << string(name: "BUILD_LIST", value:"${buildList}")

            item.PLAT_MACHINE_MAP.each { kv ->
                kv.each{ p, m ->
                    def platform = p
                    def machine = m

                    testParams << string(name: "PLATFORM", value:"${platform}")
                    baselineParams << string(name: "PLATFORM", value:"${platform}")

                    testParams << string(name: "LABEL", value:"${machine}")
                    baselineParams << string(name: "LABEL", value:"${machine}")

                    if (params.PLATFORM == platform) {
                        echo "starting to trigger build..."
                        lock(resource: "${machine}") {
                            for (int i = 0; i < PERF_ITERATIONS; i++) {
                                // test
                                testParams << string(name: "TEST_NUM", value: "TEST_NUM" + i.toString())
                                triggerJob("${item.BENCHMARK}", "${platform}", testParams, "test")

                                // baseline
                                if (RUN_BASELINE) {
                                    baselineParams << string(name: "BASELINE_NUM", value: "BASELINE_NUM_" + i.toString())
                                    triggerJob("${item.BENCHMARK}", "${platform}", baselineParams, "baseline")
                                } else {
                                    echo "Skipping baseline run since RUN_BASELINE is set to false"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

def triggerJob(benchmarkName, platformName, buildParams, jobSuffix) {
    def buildJobName = "${JOB_NAME}_${jobSuffix}"
    def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper
    def jobIsRunnable = JobHelper.jobIsRunnable("${buildJobName}")
    if (!jobIsRunnable) {
        echo "Child job ${buildJobName} doesn't exist, set child job ${buildJobName} params for generating the job"
        generateChildJobViaAutoGen(buildJobName)
    }
    build job: buildJobName, parameters: buildParams, propagate: true
}

def generateChildJobViaAutoGen(newJobName) {
    def jobParams = []
    jobParams << string(name: 'TEST_JOB_NAME', value: newJobName)
    jobParams << string(name: 'ARCH_OS_LIST', value: params.PLATFORM)
    jobParams << booleanParam(name: 'LIGHT_WEIGHT_CHECKOUT', value: false)
    jobParams << string(name: 'LEVELS', value: "sanity") // ToDo: hard coded for now from line 79
    jobParams << string(name: 'GROUPS', value: "perf") // ToDo: hard coded for now from line 79
    jobParams << string(name: 'JDK_VERSIONS', value: params.JDK_VERSION)
    jobParams << string(name: 'JDK_IMPL', value: params.JDK_IMPL)

    build job: 'Test_Job_Auto_Gen', parameters: jobParams, propagate: true
}
