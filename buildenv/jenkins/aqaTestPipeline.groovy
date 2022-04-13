#!groovy

def JDK_VERSIONS = params.JDK_VERSIONS.trim().split("\\s*,\\s*");
def PLATFORMS = params.PLATFORMS.trim().split("\\s*,\\s*");
def TARGETS = params.TARGETS.trim().split("\\s*,\\s*");

def USE_TESTENV_PROPERTIES = params.USE_TESTENV_PROPERTIES ? params.USE_TESTENV_PROPERTIES : true
def PARALLEL = params.PARALLEL ? params.PARALLEL : "Dynamic"
def NUM_MACHINES = params.NUM_MACHINES ? params.NUM_MACHINES : 3
def SDK_RESOURCE = params.SDK_RESOURCE ? params.SDK_RESOURCE : "releases"
def TIME_LIMIT = params.TIME_LIMIT ? params.TIME_LIMIT : 10
def AUTO_AQA_GEN = params.AUTO_AQA_GEN ? params.AUTO_AQA_GEN : false
def TRSS_URL = params.TRSS_URL ? params.TRSS_URL : "https://trss.adoptium.net/"

def JOBS = [:]

JDK_VERSIONS.each { JDK_VERSION ->
    PLATFORMS.each { PLATFORM ->
        String[] tokens = PLATFORM.split('_')
        def os = tokens[1];
        def arch = tokens[0];
        if (arch.contains("x86-64")){
            arch = "x64"
        } else if (arch.contains("x86-32")) {
            arch ="x32"
        }

        def short_name = "hs"
        def jdk_impl = "hotspot"
        if (params.VARIANT == "openj9") {
            short_name = "j9"
            jdk_impl = params.VARIANT
        }

        def download_url = params.CUSTOMIZED_SDK_URL ? params.CUSTOMIZED_SDK_URL : ""
        if (SDK_RESOURCE == "customized" ) {
            if (params.TOP_LEVEL_SDK_URL) {
                // example: <jenkins_url>/job/build-scripts/job/openjdk11-pipeline/123/artifact/target/linux/aarch64/openj9/*_aarch64_linux_*.tar.gz/*zip*/openj9.zip 
                download_url = params.TOP_LEVEL_SDK_URL + "/artifact/target/${os}/${arch}/${params.VARIANT}/*.tar.gz/*zip*/${params.VARIANT}.zip"
            }
        } else if (SDK_RESOURCE == "releases") {
            if (params.VARIANT == "openj9") {
                // get IBM Semeru CE
                SDK_RESOURCE = "customized"
                download_url="https://ibm.com/semeru-runtimes/api/v3/binary/latest/${JDK_VERSION}/ga/${os}/${arch}/jdk/openj9/normal/ibm_ce https://ibm.com/semeru-runtimes/api/v3/binary/latest/${JDK_VERSION}/ga/${os}/${arch}/testimage/openj9/normal/ibm_ce"
            }
        }
        echo "download_url: ${download_url}"

        TARGETS.each { TARGET ->
            def TEST_JOB_NAME = "Test_openjdk${JDK_VERSION}_${short_name}_${TARGET}_${PLATFORM}"
            echo "TEST_JOB_NAME: ${TEST_JOB_NAME}"

            def keep_reportdir = false
            if (TARGET.contains("jck") || TARGET.contains("openjdk")) {
                keep_reportdir = true
            }
            if (AUTO_AQA_GEN) {
                String[] targetTokens = TARGET.split("\\.")
                def level = targetTokens[0];
                def group = targetTokens[1];
                def parameters = [
                    string(name: 'LEVELS', value: level),
                    string(name: 'GROUPS', value: group),
                    string(name: 'JDK_VERSIONS', value: JDK_VERSION),
                    string(name: 'ARCH_OS_LIST', value: PLATFORM),
                    string(name: 'JDK_IMPL', value: jdk_impl),
                    booleanParam(name: 'LIGHT_WEIGHT_CHECKOUT', value: false)
                ]
                build job: 'Test_Job_Auto_Gen', parameters: parameters, propagate: true
            }

            def JobHelper = library(identifier: 'openjdk-jenkins-helper@master').JobHelper
            if (JobHelper.jobIsRunnable(TEST_JOB_NAME as String)) {
                JOBS["${TEST_JOB_NAME}"] = {
                    build job: TEST_JOB_NAME, parameters: [
                        string(name: 'ADOPTOPENJDK_REPO', value: params.ADOPTOPENJDK_REPO),
                        string(name: 'ADOPTOPENJDK_BRANCH', value: params.ADOPTOPENJDK_BRANCH),
                        booleanParam(name: 'USE_TESTENV_PROPERTIES', value: USE_TESTENV_PROPERTIES),
                        string(name: 'SDK_RESOURCE', value: params.SDK_RESOURCE),
                        string(name: 'CUSTOMIZED_SDK_URL',  value: download_url),
                        string(name: 'CUSTOMIZED_SDK_URL_CREDENTIAL_ID',  value: params.CUSTOMIZED_SDK_URL_CREDENTIAL_ID),
                        string(name: 'PARALLEL', value: PARALLEL),
                        string(name: 'NUM_MACHINES', value: NUM_MACHINES.toString()),
                        booleanParam(name: 'GENERATE_JOBS', value: AUTO_AQA_GEN),
                        booleanParam(name: 'LIGHT_WEIGHT_CHECKOUT', value: false),
                        string(name: 'TIME_LIMIT', value: TIME_LIMIT.toString()),
                        string(name: 'TRSS_URL', value: TRSS_URL),
                        booleanParam(name: 'KEEP_REPORTDIR', value: keep_reportdir)
                    ]
                }
            } else {
	            println "[WARNING] Requested test job that does not exist or is disabled: ${TEST_JOB_NAME}"
            }
        }
    }

}
parallel JOBS
