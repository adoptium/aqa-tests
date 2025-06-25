#!groovy
def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper

def PLATFORM = params.PLATFORMS.trim() //one platform
def BENCHMARKS = params.BENCHMARKS.trim().split("\\s*,\\s*")

def JOBS =[:]
BENCHMARKS.each { BENCHMARK ->
        def childParams = []
        // loop through all the params and change the parameters if needed
        params.each { param ->
                // Exclude unnecessary parameters for downstream jobs
                if (param == "PLATFORMS" || param == "BENCHMARKS") {
                // do nothing
                }
                def value = param.value.toString()
                if (value == "true" || value == "false") {
                        childParams << booleanParam(name: param.key, value: value.toBoolean())
                } else {
                        childParams << string(name: param.key, value: value)
                }
        }
        childParams << string(name: "PLATFORM", value: PLATFORM)
        childParams << string(name: "BENCHMARK", value: BENCHMARK)
        def shortName = "j9"
        if (params.JDK_IMPL) {
                if (params.JDK_IMPL == "hotspot") {
                shortName = "hs"
                }
        }
        def jobName = "Perf_openjdk21_${shortName}_sanity.perf_${PLATFORM}_${BENCHMARK}"
        def jobIsRunnable = JobHelper.jobIsRunnable(jobName)
        if (!jobIsRunnable) {
                echo "Generating downstream job '${jobName}' from template …"
                createPerfL2Job(jobName, PLATFORM)
        }
        JOBS[jobName] = {
                build job: jobName, parameters: childParams, propagate: true
        }
}

parallel JOBS

def createPerfL2Job(String jobName, String platform) {
        def jobParams = [:] 
        jobParams.put('TEST_JOB_NAME', jobName)
        jobParams.put('PLATFORM', platform) 
        def templatePath = 'aqa-tests/buildenv/jenkins/perf/perfL2JobTemplate'
        if (!fileExists(templatePath)) {
                sh 'curl -Os https://raw.githubusercontent.com/adoptium/aqa-tests/master/buildenv/jenkins/perfL2JobTemplate'
                templatePath = 'perfL2JobTemplate'
        }
        jobDsl targets: templatePath, ignoreExisting: false, additionalParameters: jobParams
}

