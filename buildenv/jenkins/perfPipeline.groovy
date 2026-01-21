#!groovy

def testParams = []
def baselineParams = []
boolean RUN_BASELINE = (params.RUN_BASELINE != null) ? params.RUN_BASELINE.toBoolean() : true
int PERF_ITERATIONS = params.PERF_ITERATIONS ? params.PERF_ITERATIONS.toInteger() : 4
boolean PROCESS_METRICS = (params.PROCESS_METRICS != null) ? params.PROCESS_METRICS.toBoolean() : false 
boolean EXIT_EARLY = (params.EXIT_EARLY != null) ? params.EXIT_EARLY.toBoolean() : false 

if (params.SETUP_LABEL) {
    SETUP_LABEL = params.SETUP_LABEL
} else {
    if (PROCESS_METRICS && EXIT_EARLY) {
       SETUP_LABEL = "test-rhibmcloud-rhel9-x64-1" //machine needs python
   } else {
       SETUP_LABEL = "ci.role.test&&hw.arch.x86&&sw.os.linux"
   }
}

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

node (SETUP_LABEL) {
        timestamps {
                try {
                        def metrics = [:]
                        def testList = []
                        def testNames = null 
                        def testRun = null
                        def baseRun = null 
                        def runBase = "runBase.json"
                        def aggrBase = "aggrBase.json"

                        ["BUILD_LIST", "PLATFORM", "LABEL"].each { key ->
                                [testParams, baselineParams].each { list ->
                                        list << string(name: key, value: params."${key}")
                                }
                        }

                        if (PROCESS_METRICS) { //convert BenchmarkMetric.js to a JSON file optimized for metric processing 
                                def owner = params.ADOPTOPENJDK_REPO.tokenize('/')[2]
                                getPythonDependencies(owner, params.ADOPTOPENJDK_BRANCH) 
                                sh "curl -Os  https://raw.githubusercontent.com/adoptium/aqa-test-tools/refs/heads/master/TestResultSummaryService/parsers/BenchmarkMetric.js"
                                sh "python3 metricConfig2JSON.py --metricConfig_js BenchmarkMetric.js"
                                sh "python3 initBenchmarkMetrics.py --metricConfig_json metricConfig.json --testNames ${params.TARGET.split("=")[1]} --runBase ${runBase} --aggrBase ${aggrBase}"
                                testList = params.TARGET.split("=")[1].tokenize(",")
                                metrics = readJSON file: aggrBase
                        }
                        
                        if (!EXIT_EARLY) {
                                testParams << string(name: "TARGET", value: params.TARGET) 
                                baselineParams << string(name: "TARGET", value: params.TARGET)
                        }
                        
                        echo "starting to trigger build..."
                        lock(resource: params.LABEL) {
                                for (int i = 0; i < PERF_ITERATIONS; i++) {
                                        //clone to avoid mutation
                                        def thisTestParams = testParams.collect()
                                        def thisBaselineParams = baselineParams.collect()
                                        if (EXIT_EARLY) {     
                                                //update TARGET, testlist should hold metrics that were not exited early
                                                testNames = testList.join(",")
                                                def TARGET = params.TARGET.replaceFirst(/(?<=TESTLIST=)[^ ]+/, testNames)
                                                thisTestParams << string(name: "TARGET", value: TARGET)
                                                thisBaselineParams << string(name: "TARGET", value: TARGET)
                                        }

                                        // test
                                        testParams << string(name: "TEST_NUM", value: "TEST_NUM" + i.toString())
                                        testRun = triggerJob(params.BENCHMARK, params.PLATFORM, thisTestParams, "test")

                                        // baseline
                                        if (RUN_BASELINE) {
                                                baselineParams << string(name: "BASELINE_NUM", value: "BASELINE_NUM_" + i.toString())
                                                baseRun = triggerJob(params.BENCHMARK, params.PLATFORM, thisBaselineParams, "baseline")
                        
                                        } else {
                                                echo "Skipping baseline run since RUN_BASELINE is set to false"
                                        }

                                        if (PROCESS_METRICS) {
                                                aggregateLogs(testRun, testNames, testList, runBase, metrics, "test")
                                                aggregateLogs(baseRun, testNames, testList, runBase, metrics, "baseline")
                                                writeJSON file: "metrics.json", json: metrics, pretty: 4
                                                archiveArtifacts artifacts: "metrics.json" 

                                                //if we are on the final iteration, or we have executed enough iterations to decide likelihood of regression and have permission to exit early 
                                                if (i == PERF_ITERATIONS-1 || (EXIT_EARLY && i >= PERF_ITERATIONS * 0.8)) {
                                                        if (i == PERF_ITERATIONS-1) {
                                                                echo "All iterations completed"
                                                        } else {
                                                                echo "Attempting early exit"
                                                        }
                                                        echo "checking for regressions"
                                                        checkRegressions(metrics, testList) //compute relevant performance stats, check for regression
                                                        if (testList.size() == 0) break //if all tests have been exited early we can end testing
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

def aggregateLogs(run, testNames, testList, templateName, aggregateMetrics, testType) {
        node("ci.role.test&&hw.arch.x86&&sw.os.linux") {
                def json
                def buildId  = run.getNumber()
                def name = run.getProjectName()
                def result = run.getCurrentResult()
                def fname = "${name}_${buildId}.json"

        echo "${name} #${buildId} completed with status ${result}, retrieving console log..."
        writeFile file : 'console.txt', text: run.getRawBuild().getLog() 
        sh "python3 benchmarkMetric.py --benchmarkMetricsTemplate_json ${templateName} --console console.txt --fname ${fname} --testNames ${testNames}"

        try {
                archiveArtifacts artifacts: fname, fingerprint: true, allowEmptyArchive: false
        } catch (Exception e) {
                echo "Cannot copy/process ${name}_${buildId}.json from ${name}: ${e}"
        }

        def runMetrics = readJSON file: fname 

        testList.each { test ->
                aggregateMetrics[test].each { metric -> 
                        def value = runMetrics[test][metric.key]["value"]
                        if (value != null) metric.value[testType]["values"] << value
                }
        } }
}

def checkRegressions(aggregateMetrics, testList) { 
testloop: for (test in testList.clone()) {
                for (metric in aggregateMetrics[test].entrySet()) {
                        def testMetrics = metric.value["test"]["values"]
                        def baselineMetrics = metric.value["baseline"]["values"]
                        if (testMetrics.size() > 0 && baselineMetrics.size() > 0) {
                                def testStats = getStats(testMetrics) 
                                def baselineStats = getStats(baselineMetrics)

                                echo "testStats: ${testStats}"
                                echo "baselineStats: ${baselineStats}"

                                def score = (metric.value["higherbetter"]) ? testStats.mean/baselineStats.mean : baselineStats.mean/testStats.mean
                                score *= 100 

                                echo "score: ${score}"

                                if (score <= 98) {
                                        currentBuild.result = 'UNSTABLE'
                                        echo "Possible ${metric.key} regression for ${test}, set build result to UNSTABLE."
                                        continue testloop
                                }
                        }
                        else {
                                currentBuild.result = 'UNSTABLE'
                                echo "${metric.key} metric for ${test} not found across all iterations. Set build result to UNSTABLE."
                                continue testloop
                        }
                }
                echo "Perf iteration for ${test} completed."
                testList.remove(test) //no metrics had regression or errors, we can EXIT_EARLY this test 
        }
}

def getStats (values) { 
        def n = values.size()
        def mid = n.intdiv(2)
        def sorted = values.sort()
        def mean = values.sum()/n
        def median = (n % 2 == 1) ? sorted[mid] : (sorted[mid-1]+sorted[mid])/2
        def variance = values.collect{(it-mean)**2}.sum()/n
        def stdev = Math.sqrt(variance as double)
        [mean: mean, max: sorted[-1], min: sorted[0], median: median, std: stdev]
}

def getPythonDependencies (owner, branch) {
        def pythonScripts = ["benchmarkMetric.py", "initBenchmarkMetrics.py", "metricConfig2JSON.py"]
        pythonScripts.each { pythonScript ->
                sh "curl -Os https://raw.githubusercontent.com/${owner}/aqa-tests/refs/heads/${branch}/buildenv/jenkins/${pythonScript}"
        }
}