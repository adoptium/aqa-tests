#!groovy
def testStats = [:]
def baselineStats = [:]
def testRuntimes = []
def baselineRuntimes = []
def testParams = []
def baselineParams = []
int PERF_ITERATIONS = params.PERF_ITERATIONS ? params.PERF_ITERATIONS.toInteger() : 4
boolean RUN_BASELINE = (params.RUN_BASELINE != null) ? params.RUN_BASELINE.toBoolean() : true

def EXIT_EARLY = (params.EXIT_EARLY) ? true : false 

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

node ("ci.role.test&&hw.arch.x86&&sw.os.linux") {
        timestamps {
                try {
                        ["TARGET", "BUILD_LIST", "PLATFORM", "LABEL"].each { key ->
                                [testParams, baselineParams].each { list ->
                                        list << string(name: key, value: params."${key}")
                                }
                        }
                        
                        echo "starting to trigger build..."
                        lock(resource: params.LABEL) {
                                for (int i = 0; i < PERF_ITERATIONS; i++) {
                                        // test
                                        testParams << string(name: "TEST_NUM", value: "TEST_NUM" + i.toString())
                                        def testRun = triggerJob(params.BENCHMARK, params.PLATFORM, testParams, "test")
                                        aggregateLogs(testRun, testRuntimes)

                                        // baseline
                                        if (RUN_BASELINE) {
                                                baselineParams << string(name: "BASELINE_NUM", value: "BASELINE_NUM_" + i.toString())
                                                def baseRun = triggerJob(params.BENCHMARK, params.PLATFORM, baselineParams, "baseline")
                                                aggregateLogs(baseRun, baselineRuntimes)
                                        } else {
                                                echo "Skipping baseline run since RUN_BASELINE is set to false"
                                        }
                                        if (params.TARGET && params.TARGET.contains('dacapo')) {
                                                testStats = stats(testRuntimes)
                                                baselineStats = stats(baselineRuntimes)
                                                def score = (testStats.mean/baselineStats.mean) * 100

                                                echo "testRuntimes: ${testRuntimes}" 
                                                echo "baselineRuntimes: ${baselineRuntimes}"
                                                echo "score: ${score} %"

                                                if (i == PERF_ITERATIONS || (EXIT_EARLY && i >= PERF_ITERATIONS * 0.8)) {
                                                        if (score <= 98) {
                                                                currentBuild.result = 'UNSTABLE'
                                                                echo "Possible regression, set build result to UNSTABLE."
                                                        } else {
                                                                echo "Perf iteration completed. EXIT_EARLY: ${EXIT_EARLY}. PERF_ITERATIONS: ${PERF_ITERATIONS}. Actual iterations: ${i}."
                                                                break
                                                        }
                                                }
                                        }
                                }
                        }
                } finally {
                        cleanWs disableDeferredWipeout: true, deleteDirs: true
                }
        }
}

def triggerJob(benchmarkName, platformName, buildParams, jobSuffix) {
    def buildJobName = "${JOB_NAME}_${jobSuffix}"
    def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper
    if (params.GENERATE_JOBS) {
        echo "Child job ${buildJobName} doesn't exist, set child job ${buildJobName} params for generating the job"
        generateChildJobViaAutoGen(buildJobName)
    }
    else {
        def jobIsRunnable = JobHelper.jobIsRunnable("${buildJobName}")
        if (!jobIsRunnable) {
                echo "Child job ${buildJobName} doesn't exist, set child job ${buildJobName} params for generating the job"
                generateChildJobViaAutoGen(buildJobName)
        }
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

def aggregateLogs(run, runtimes) {
        def json 
        node("ci.role.test&&hw.arch.x86&&sw.os.linux") {
                def buildId  = run.getNumber()
                def name = run.getProjectName()
                def result = run.getCurrentResult()

                echo "${name} #${buildId} completed with status ${result}, copying JSON logs..."

                try {
                        timeout(time: 1, unit: 'HOURS') {
                                copyArtifacts(
                                        projectName: name,
                                        selector: specific("${buildId}"),
                                        filter: "**/${name}_${buildId}.json",
                                        target: "."
                                )
                                
                        }   
                        json = readJSON file: "${name}_${buildId}.json"
                        archiveArtifacts artifacts: "${name}_${buildId}.json", fingerprint: true, allowEmptyArchive: false
                        def metricList = json.metrics['dacapo-h2']
                        def runtimeMap = metricList.find{ it.containsKey('value') }
                        if (runtimeMap) {
                                runtimes << (runtimeMap.value as double)
                        } else { 
                                echo "No runtime in ${name}_${buildId}.json" 
                        }
                } catch (Exception e) {
                        echo "Cannot copy/process ${name}_${buildId}.json from ${name}: ${e}"
                }
        }
}

def stats (List nums) { 
        def n = nums.size()
        def mid = n.intdiv(2)

        def sorted = nums.sort()
        def mean = nums.sum()/n
        def median = (n % 2 == 1) ? sorted[mid] : (sorted[mid-1]+sorted[mid])/2
        def variance = nums.collect{(it-mean)**2}.sum()/n
        def stdev = Math.sqrt(variance as double)
        [mean: mean, max: sorted[-1], min: sorted[0], median: median, std: stdev]
}

