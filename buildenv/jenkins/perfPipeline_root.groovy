#!groovy
def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper
def JOBS =[:]

// read JSON from perfConfig file
def perfConfigJson = []
if (params.PERFCONFIG_JSON) { 
        echo "Read JSON from PERFCONFIG_JSON parameter..." 
        perfConfigJson = readJSON text: "${params.PERFCONFIG_JSON}"
} else { 
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
}

def childParams = []
// update all parameters to strings except for booleans
params.each { param ->
        if ( param.key == "PERFCONFIG_JSON" ) {
                // do not need to pass the param to child jobs
        } else {
                def value = param.value.toString()
                if (value == "true" || value == "false") {
                        childParams << booleanParam(name: param.key, value: value.toBoolean())
                } else {
                        childParams << string(name: param.key, value: value)
                }
        }

}
node("worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)") {
        perfConfigJson.each { item ->
                def BENCHMARK = item.BENCHMARK 
                def TARGET = item.TARGET
                def BUILD_LIST = item.BUILD_LIST
                def PLATMACHINE_MAP = item.PLAT_MACHINE_MAP
                def baseParams = childParams.collect()
                baseParams << string(name: "BENCHMARK", value: item.BENCHMARK)
                baseParams << string(name: "TARGET", value: item.TARGET)
                baseParams << string(name: "BUILD_LIST", value: item.BUILD_LIST)
                
                item.PLAT_MACHINE_MAP.each { kv -> 
                        kv.each {p, m -> 
                                // Clone baseParams to avoid mutation
                                def thisChildParams = baseParams.collect()
                                thisChildParams << string(name: "PLATFORM", value: p)
                                thisChildParams << string(name: "LABEL", value: m)

                                def shortName = (params.JDK_IMPL && params.JDK_IMPL == "hotspot") ? "hs" : "j9"
                                def jobName = "Perf_openjdk${params.JDK_VERSION}_${shortName}_sanity.perf_${p}_${item.BENCHMARK}"
                                if (params.GENERATE_JOBS) {
                                        echo "Generating downstream job '${jobName}' from perfL2JobTemplate …"
                                        createPerfL2Job(jobName, p, item.BENCHMARK)
                                }
                                else {
                                        def jobIsRunnable = JobHelper.jobIsRunnable(jobName)
                                        echo "jobName ${jobName} params: ${thisChildParams}"
                                        if (!jobIsRunnable) {
                                                echo "Generating downstream job '${jobName}' from perfL2JobTemplate …"
                                                createPerfL2Job(jobName, p, item.BENCHMARK)
                                        }
                                }
                                JOBS[jobName] = {
                                        build job: jobName, parameters: thisChildParams, propagate: true
                                }
                        }
                }
        }
}

parallel JOBS

def createPerfL2Job(String jobName, String platform, String benchmark) {
        def jobParams = [:] 
        jobParams.put('TEST_JOB_NAME', jobName)
        jobParams.put('PLATFORM', platform) 
        jobParams.put('BENCHMARK', benchmark)
        def templatePath = 'aqa-tests/buildenv/jenkins/perf/perfL2JobTemplate'
        if (!fileExists(templatePath)) {
                sh 'curl -Os https://raw.githubusercontent.com/adoptium/aqa-tests/master/buildenv/jenkins/perfL2JobTemplate'
                templatePath = 'perfL2JobTemplate'
        }
        jobDsl targets: templatePath, ignoreExisting: false, additionalParameters: jobParams
}

