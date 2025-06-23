#!groovy
def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper

def PLATFORMS = params.PLATFORMS.trim().split("\\s*,\\s*")
def BENCHMARKS = params.BENCHMARKS.trim().split("\\s*,\\s*")

def JOBS =[:]
PLATFORMS.each { PLATFORM ->
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
        if (!childParams.contains("TEST_IMAGES_REQUIRED")) {
            childParams << booleanParam(name: "TEST_IMAGES_REQUIRED", value: false)
        }
        if (!childParams.contains("DEBUG_IMAGES_REQUIRED")) {
            childParams << booleanParam(name: "DEBUG_IMAGES_REQUIRED", value: false)
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
                createJob(jobName, PLATFORM)
        }
        JOBS[jobName] = {
            build job: jobName, parameters: childParams, propagate: true
        }
    }
}

parallel JOBS

def createJob( TEST_JOB_NAME, ARCH_OS ) {

	def jobParams = [:]
	jobParams.put('TEST_JOB_NAME', TEST_JOB_NAME)
	jobParams.put('ARCH_OS_LIST', ARCH_OS)

	if (params.DAYS_TO_KEEP) {
		jobParams.put('DAYS_TO_KEEP', DAYS_TO_KEEP)
	}

	if (params.BUILDS_TO_KEEP) {
		jobParams.put('BUILDS_TO_KEEP', BUILDS_TO_KEEP)
	}

	def templatePath = 'aqa-tests/buildenv/jenkins/testJobTemplate'
	if (!fileExists(templatePath)) {
		sh "curl -Os https://raw.githubusercontent.com/adoptium/aqa-tests/master/buildenv/jenkins/testJobTemplate"
		templatePath = 'testJobTemplate'
	}

	if (env.LIGHT_WEIGHT_CHECKOUT) {
		jobParams.put('LIGHT_WEIGHT_CHECKOUT', env.LIGHT_WEIGHT_CHECKOUT)
	}

	def create = jobDsl targets: templatePath, ignoreExisting: false, additionalParameters: jobParams
	return create
}