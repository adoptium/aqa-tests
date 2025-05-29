#!groovy

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
        childParams << string(name: "PLATFORM", value: PLATFORM)
        childParams << string(name: "BENCHMARK", value: BENCHMARK)

        def jobName = "Perf_openjdk21_j9_sanity.perf_${PLATFORM}_${BENCHMARK}"
        JOBS[jobName] = {
            build job: jobName, parameters: childParams, propagate: true
        }
    }
}

parallel JOBS
