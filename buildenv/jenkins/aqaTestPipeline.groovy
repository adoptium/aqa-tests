#!groovy

import groovy.transform.Field

def JDK_VERSIONS = params.JDK_VERSIONS.trim().split("\\s*,\\s*")
def PLATFORMS = params.PLATFORMS ? params.PLATFORMS.trim().split("\\s*,\\s*") : ""
def TARGETS = params.TARGETS ?: "Grinder"
TARGETS = TARGETS.trim().split("\\s*,\\s*")
def TEST_FLAG = (params.TEST_FLAG) ?: ""

def PARALLEL = params.PARALLEL ? params.PARALLEL : "Dynamic"
def MODE = params.MODE ? params.MODE : "ENTRYPOINT"


@Field String SDK_RESOURCE
@Field String TIME_LIMIT
@Field Boolean AUTO_AQA_GEN
@Field Boolean LIGHT_WEIGHT_CHECKOUT

SDK_RESOURCE = params.SDK_RESOURCE ? params.SDK_RESOURCE : "releases"
TIME_LIMIT = params.TIME_LIMIT ? params.TIME_LIMIT : 10
AUTO_AQA_GEN = params.AUTO_AQA_GEN ? params.AUTO_AQA_GEN.toBoolean() : false
LIGHT_WEIGHT_CHECKOUT = params.LIGHT_WEIGHT_CHECKOUT ?: false

// Use BUILD_USER_ID if set and jdk-JDK_VERSIONS
def DEFAULT_SUFFIX = (env.BUILD_USER_ID) ? "${env.BUILD_USER_ID} - jdk-${params.JDK_VERSIONS}" : "jdk-${params.JDK_VERSIONS}"
def PIPELINE_DISPLAY_NAME = (params.PIPELINE_DISPLAY_NAME) ? "#${currentBuild.number} - ${params.PIPELINE_DISPLAY_NAME}" : "#${currentBuild.number} - ${DEFAULT_SUFFIX}"

// Set the AQA_TEST_PIPELINE Jenkins job displayName
currentBuild.setDisplayName(PIPELINE_DISPLAY_NAME)

// Default test targets - will be loaded from config JSON files
@Field String defaultTestTargets = ""
@Field String defaultFipsTestTargets = ""
@Field String defaultFips140_2TestTargets = ""

@Field Map JOBS = [:]

timestamps {
    currentBuild.description = (currentBuild.description) ? currentBuild.description + "<br>" : ""
    JDK_VERSIONS.each { JDK_VERSION ->
        if ( params.MODE != 'RELAY' && (params.BUILD_TYPE == "release" || params.BUILD_TYPE == "nightly" || params.BUILD_TYPE == "weekly" )) {
            def configJson = []
            if (params.CONFIG_JSON) {
                echo "Read JSON from CONFIG_JSON parameter..."
                configJson = readJSON text: "${params.CONFIG_JSON}"
            } else {
                node("worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)") {
                    checkout scm
                    dir (env.WORKSPACE) {
                        // 2-Level Configuration Hierarchy:
                        // Level 1: config/{variant}/default.json (variant-wide base)
                        // Level 2: config/{variant}/{build_type}/jdk{version}.json OR
                        //          config/{variant}/{build_type}/default.json (fallback)
                        
                        def variantDir = "./aqa-tests/buildenv/jenkins/config/${params.VARIANT}/"
                        
                        // Fallback: if variant is 'ibm' and config doesn't exist, use 'openj9' config
                        if (params.VARIANT == "ibm" && !fileExists("${variantDir}default.json")) {
                            echo "Config for variant 'ibm' not found, falling back to 'openj9' config..."
                            variantDir = "./aqa-tests/buildenv/jenkins/config/openj9/"
                        }
                        
                        def buildTypeDir = "${variantDir}${params.BUILD_TYPE}/"
                        
                        // Level 1: Read variant-wide base configuration
                        def level1File = "${variantDir}default.json"
                        def baseConfig = [:]
                        if (fileExists(level1File)) {
                            echo "Reading Level 1 config from ${level1File}..."
                            def level1Array = readJSON(file: level1File)
                            baseConfig = level1Array[0] // Extract first element from array
                        } else {
                            echo "Warning: Level 1 config file not found: ${level1File}"
                        }
                        
                        // Level 2: Try version-specific file first, fallback to build-type default
                        def level2File = "${buildTypeDir}jdk${JDK_VERSION}.json"
                        if (!fileExists(level2File)) {
                            level2File = "${buildTypeDir}default.json"
                        }
                        
                        echo "Reading Level 2 config from ${level2File}..."
                        def level2Array = readJSON(file: level2File)
                        def level2Config = level2Array[0] // Extract first element from array
                        
                        // Merge: Level 2 overrides Level 1
                        if (baseConfig && baseConfig.size() > 0) {
                            echo "Merging configurations: Level 2 overrides Level 1..."
                            // Inline deep merge to avoid DSL method resolution issues
                            def merged = [:]
                            baseConfig.each { k, v ->
                                if (v instanceof Map) {
                                    merged[k] = [:]
                                    v.each { k2, v2 ->
                                        if (v2 instanceof Map) {
                                            merged[k][k2] = [:]
                                            v2.each { k3, v3 -> merged[k][k2][k3] = v3 }
                                        } else {
                                            merged[k][k2] = v2
                                        }
                                    }
                                } else if (v instanceof List) {
                                    merged[k] = v.clone()
                                } else {
                                    merged[k] = v
                                }
                            }
                            echo "DEBUG: level2Config keys: ${level2Config.keySet()}"
                            level2Config.each { key, value ->
                                echo "DEBUG: Processing key '${key}', value type: ${value.getClass().simpleName}"
                                if (value instanceof List) {
                                    // Lists override completely
                                    echo "DEBUG: Adding List '${key}'"
                                    merged[key] = value
                                } else if (value instanceof Map && merged[key] instanceof Map) {
                                    // Merge nested maps
                                    echo "DEBUG: Merging Map '${key}'"
                                    value.each { k2, v2 ->
                                        if (v2 instanceof Map && merged[key][k2] instanceof Map) {
                                            // Merge 2 levels deep
                                            v2.each { k3, v3 -> merged[key][k2][k3] = v3 }
                                        } else {
                                            merged[key][k2] = v2
                                        }
                                    }
                                } else {
                                    echo "DEBUG: Adding/overriding '${key}'"
                                    merged[key] = value
                                }
                            }
                            configJson = [merged]
                            echo "DEBUG: Merged config keys: ${merged.keySet()}"
                            if (merged.PLATFORM_TARGETS) {
                                echo "DEBUG: PLATFORM_TARGETS found: ${merged.PLATFORM_TARGETS}"
                            } else {
                                echo "DEBUG: PLATFORM_TARGETS is missing!"
                            }
                        } else {
                            // If no Level 1 config, use Level 2 only (backward compatibility)
                            configJson = [level2Config]
                            echo "DEBUG: Using Level 2 only, keys: ${level2Config.keySet()}"
                        }
                    }
                }
            }

            configJson.each { item ->
                def releaseTestFlag = item.TEST_FLAG
                def globalBuildConfig = item.GLOBAL_BUILD_CONFIG ?: [:]
                def targetSpecificConfig = item.TARGET_SPECIFIC_CONFIG ?: [:]
                def platformSpecificConfig = item.PLATFORM_SPECIFIC_CONFIG ?: [:]
                def platformAdditionalTestLabels = item.PLATFORM_ADDITIONAL_TEST_LABELS ?: [:]
                def platformAdditionalTestParams = item.PLATFORM_ADDITIONAL_TEST_PARAMS ?: [:]
                
                // Load default test targets from config
                if (item.DEFAULT_TEST_TARGETS) {
                    defaultTestTargets = item.DEFAULT_TEST_TARGETS
                }
                if (item.DEFAULT_FIPS_TEST_TARGETS) {
                    defaultFipsTestTargets = item.DEFAULT_FIPS_TEST_TARGETS
                }
                if (item.DEFAULT_FIPS140_2_TEST_TARGETS) {
                    defaultFips140_2TestTargets = item.DEFAULT_FIPS140_2_TEST_TARGETS
                }
                
                item.PLATFORM_TARGETS.each { pt ->
                    pt.each { p, t ->
                        // When the AQA Test Pipeline is triggered by an upstream pipeline at runtime, we only receive the SDK URL for a single platform at a time.
                        // if params.PLATFORMS is set, only trigger testing for the platform that is specified
                        if (params.PLATFORMS) {
                            if (params.PLATFORMS.contains(p)) {
                                echo "Only triggering test builds specified in PLATFORMS: ${params.PLATFORMS}..."
                                generateJobs(JDK_VERSION, releaseTestFlag, p, t, globalBuildConfig, targetSpecificConfig, platformSpecificConfig, platformAdditionalTestLabels, platformAdditionalTestParams)
                            }
                        } else {
                            generateJobs(JDK_VERSION, releaseTestFlag, p, t, globalBuildConfig, targetSpecificConfig, platformSpecificConfig, platformAdditionalTestLabels, platformAdditionalTestParams)
                        }
                    }
                }
            }
        } else {
            if ( params.MODE == 'RELAY' && params.VARIANT == "temurin" ) {
                remoteTriggerTemurinJCK(JDK_VERSION, PLATFORMS)
            } else {
                generateJobs(JDK_VERSION, TEST_FLAG, PLATFORMS, TARGETS, [:], [:], [:], [:], [:])
            }
        }
    }
    parallel JOBS
}

def generateJobs(jobJdkVersion, jobTestFlag, jobPlatforms, jobTargets, globalBuildConfig = [:], targetSpecificConfig = [:], platformSpecificConfig = [:], platformAdditionalTestLabels = [:], platformAdditionalTestParams = [:]) {
    if (jobTargets instanceof String) {
        jobTargets = jobTargets.replace("defaultFipsTestTargets","${defaultFipsTestTargets}")
        jobTargets = jobTargets.replace("defaultFips140_2TestTargets","${defaultFips140_2TestTargets}")
        jobTargets = jobTargets.replace("defaultTestTargets","${defaultTestTargets}")
        jobTargets = jobTargets.split("\\s*,\\s*")
    }
    if (jobPlatforms instanceof String) {
        jobPlatforms = jobPlatforms.split("\\s*,\\s*")
    }

    echo "jobJdkVersion: ${jobJdkVersion}, jobTestFlag: ${jobTestFlag}, jobPlatforms: ${jobPlatforms}, jobTargets: ${jobTargets}"
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
        } else if (params.VARIANT == "ibm") {
            short_name = "ibm"
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
            // Helper function to merge configs with special handling for LABEL_ADDITION
            def mergeConfig = { target, source ->
                source.each { key, value ->
                    if (key == "LABEL_ADDITION" && target.containsKey("LABEL_ADDITION") && target["LABEL_ADDITION"]) {
                        // Append to existing LABEL_ADDITION instead of replacing
                        target["LABEL_ADDITION"] = "${target['LABEL_ADDITION']}&&${value}"
                    } else {
                        target[key] = value
                    }
                }
            }
            
            // Apply configuration hierarchy: global -> target-specific -> platform-specific -> params
            def buildConfig = [:]
            
            // Start with global build config from JSON
            if (globalBuildConfig) {
                mergeConfig(buildConfig, globalBuildConfig)
            }
            
            // Apply target-specific config (e.g., functional, openjdk, jck)
            if (targetSpecificConfig) {
                // Check for exact match first (e.g., "dev.openjdk")
                if (targetSpecificConfig.containsKey(TARGET)) {
                    mergeConfig(buildConfig, targetSpecificConfig[TARGET])
                }
                
                // Apply test flag-only config (e.g., "FIPS", "OpenJCEPlus") - applies to any target
                if (jobTestFlag && targetSpecificConfig.containsKey(jobTestFlag)) {
                    mergeConfig(buildConfig, targetSpecificConfig[jobTestFlag])
                }
                
                // Check for target + test flag combination (e.g., "functional.FIPS")
                if (jobTestFlag && targetSpecificConfig.containsKey("${TARGET}.${jobTestFlag}")) {
                    mergeConfig(buildConfig, targetSpecificConfig["${TARGET}.${jobTestFlag}"])
                }
                
                // Apply configs in hierarchical order: base configs first, then specific overrides
                // This allows "openjdk" to apply to all openjdk variants, but "sanity.openjdk" to override
                
                // First pass: Apply base configs (simple keys that match via substring)
                targetSpecificConfig.each { key, value ->
                    if (!key.contains('.') && TARGET.contains(key)) {
                        // Simple keys use substring match (e.g., "functional" matches "sanity.functional")
                        // This allows base category configs to apply to all variants
                        if (!['FIPS', 'OpenJCEPlus'].contains(key)) {
                            mergeConfig(buildConfig, value)
                        }
                    }
                }
                
                // Second pass: Apply specific compound configs (exact match overrides)
                targetSpecificConfig.each { key, value ->
                    if (key.contains('.')) {
                        def parts = key.split('\\.')
                        if (parts.size() == 2) {
                            // Check if this is a test flag key (e.g., "functional.FIPS")
                            def isTestFlagKey = ['FIPS', 'OpenJCEPlus'].contains(parts[1])
                            
                            if (isTestFlagKey) {
                                // Test flag keys are handled above, skip here
                                return
                            } else if (TARGET == key) {
                                // Exact match for compound keys (e.g., "special.openjdk" matches "special.openjdk")
                                // This overrides any base config applied in first pass
                                // Only apply if jobTestFlag is empty
                                if (!jobTestFlag || jobTestFlag == "") {
                                    mergeConfig(buildConfig, value)
                                }
                            }
                        }
                    }
                }
            }
            
            // Apply platform-specific config for this target
            if (platformSpecificConfig && platformSpecificConfig.containsKey(TARGET)) {
                def platformConfig = platformSpecificConfig[TARGET]
                if (platformConfig.containsKey(PLATFORM)) {
                    mergeConfig(buildConfig, platformConfig[PLATFORM])
                }
            }
            
            // Apply platform-specific additional test labels
            // This appends to any LABEL_ADDITION already set by target-specific config
            if (platformAdditionalTestLabels && platformAdditionalTestLabels.containsKey(PLATFORM)) {
                def additionalLabel = platformAdditionalTestLabels[PLATFORM]
                if (buildConfig.LABEL_ADDITION) {
                    buildConfig.LABEL_ADDITION = "${buildConfig.LABEL_ADDITION}&&${additionalLabel}"
                } else {
                    buildConfig.LABEL_ADDITION = additionalLabel
                }
            }
            
            // Apply platform-specific additional test params
            if (platformAdditionalTestParams && platformAdditionalTestParams.containsKey(PLATFORM)) {
                def additionalParams = platformAdditionalTestParams[PLATFORM]
                if (!buildConfig.ADDITIONAL_TEST_PARAMS) {
                    buildConfig.ADDITIONAL_TEST_PARAMS = [:]
                }
                mergeConfig(buildConfig.ADDITIONAL_TEST_PARAMS, additionalParams)
            }
            // Build config is now fully determined by JSON config files.
            // Pipeline-level parameters (JDK_VERSIONS, PLATFORMS, TARGETS) are separate and not in JSON.
            
            
            def TEST_JOB_NAME = "Grinder"
            if (TARGET.contains("Grinder")) {
                TEST_JOB_NAME = TARGET
            } else {
                def suffix = ""
                if (jobTestFlag) {
                    // Use abbreviated FIPS suffixes in job names to keep naming consistent
                    // across platforms while still passing the original jobTestFlag unchanged
                    // to the tests via the TEST_FLAG child parameter.
                    if (jobTestFlag.contains("FIPS")) {
                        if (jobTestFlag == "FIPS140_2") {
                            suffix = "_f2"
                        } else if (jobTestFlag == "FIPS140_3_OpenJCEPlusFIPS.FIPS140-3-Strongly-Enforced") {
                            suffix = "_f3_strong"
                        } else if (jobTestFlag == "FIPS140_3_OpenJCEPlusFIPS.FIPS140-3") {
                            suffix = "_f3_strict"
                        } else if (jobTestFlag == "FIPS140_3_OpenJCEPlusFIPS") {
                            suffix = "_f3_weak"
                        } else {
                            suffix = "_" + jobTestFlag.toLowerCase().trim()
                        }
                    } else {
                        suffix = "_" + jobTestFlag.toLowerCase().trim()
                    }
                }
                TEST_JOB_NAME = "Test_openjdk${jobJdkVersion}_${short_name}_${TARGET}_${PLATFORM}${suffix}"
            }
            echo "TEST_JOB_NAME: ${TEST_JOB_NAME}"
            echo "Applied buildConfig: ${buildConfig}"

            // Extract values from buildConfig with fallbacks
            def keep_reportdir = buildConfig.KEEP_REPORTDIR ? buildConfig.KEEP_REPORTDIR.toBoolean() : false
            def DYNAMIC_COMPILE = buildConfig.DYNAMIC_COMPILE ? buildConfig.DYNAMIC_COMPILE.toBoolean() : false
            // Override config values with params if provided
            def VENDOR_TEST_REPOS = params.VENDOR_TEST_REPOS ?: (buildConfig.VENDOR_TEST_REPOS ?: '')
            def VENDOR_TEST_BRANCHES = params.VENDOR_TEST_BRANCHES ?: (buildConfig.VENDOR_TEST_BRANCHES ?: '')
            def VENDOR_TEST_DIRS = params.VENDOR_TEST_DIRS ?: (buildConfig.VENDOR_TEST_DIRS ?: '')
            int rerunIterations = buildConfig.RERUN_ITERATIONS ? buildConfig.RERUN_ITERATIONS.toString().toInteger() : 0
            def buildList = buildConfig.BUILD_LIST ?: ""
            def testLabel = buildConfig.LABEL ?: ""
            def additionalTestLabel = buildConfig.LABEL_ADDITION ?: ""
            
            // Apply variant-specific logic for special cases not covered by config
            if (params.VARIANT == "openj9" || params.VARIANT == "ibm") {
                // Handle VENDOR_TEST_BRANCHES override for adoptium repo
                if (TARGET.contains('functional') && !jobTestFlag.contains("FIPS")) {
                    if (params.ADOPTOPENJDK_REPO && params.ADOPTOPENJDK_REPO.contains("adoptium/aqa-tests")) {
                        VENDOR_TEST_BRANCHES = params.ADOPTOPENJDK_BRANCH ?: 'master'
                    }
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
                        childParams << string(name: param.key, value: buildConfig.PARALLEL ?: "Dynamic")
                    } else if (param.key == "NUM_MACHINES") {
                        childParams << string(name: param.key, value: buildConfig.NUM_MACHINES ?: (params.NUM_MACHINES ?: ""))
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
                
                // Add build-specific parameters from config
                if (buildConfig.JDK_REPO) {
                    childParams << string(name: "JDK_REPO", value: buildConfig.JDK_REPO)
                }
                if (buildConfig.JDK_BRANCH) {
                    childParams << string(name: "JDK_BRANCH", value: buildConfig.JDK_BRANCH)
                }
                if (buildConfig.OPENJ9_BRANCH) {
                    childParams << string(name: "OPENJ9_BRANCH", value: buildConfig.OPENJ9_BRANCH)
                }
                if (testLabel) {
                    childParams << string(name: "LABEL", value: testLabel)
                }
                if (additionalTestLabel) {
                    childParams << string(name: "LABEL_ADDITION", value: additionalTestLabel)
                }
                if (buildConfig.ACTIVE_NODE_TIMEOUT) {
                    childParams << string(name: "ACTIVE_NODE_TIMEOUT", value: buildConfig.ACTIVE_NODE_TIMEOUT.toString())
                }
                if (buildConfig.USE_TESTENV_PROPERTIES != null) {
                    childParams << booleanParam(name: "USE_TESTENV_PROPERTIES", value: buildConfig.USE_TESTENV_PROPERTIES.toBoolean())
                }
                if (buildConfig.ADOPTOPENJDK_BRANCH) {
                    childParams << string(name: "ADOPTOPENJDK_BRANCH", value: buildConfig.ADOPTOPENJDK_BRANCH)
                }
                if (buildConfig.RERUN_FAILURE != null) {
                    childParams << booleanParam(name: "RERUN_FAILURE", value: buildConfig.RERUN_FAILURE.toBoolean())
                }
                if (buildList) {
                    childParams << string(name: "BUILD_LIST", value: buildList)
                }
                
                // Add any additional test params from config
                if (buildConfig.ADDITIONAL_TEST_PARAMS && buildConfig.ADDITIONAL_TEST_PARAMS instanceof Map) {
                    buildConfig.ADDITIONAL_TEST_PARAMS.each { key, value ->
                        def valueStr = value.toString()
                        if (valueStr == "true" || valueStr == "false") {
                            childParams << booleanParam(name: key, value: valueStr.toBoolean())
                        } else {
                            childParams << string(name: key, value: valueStr)
                        }
                    }
                }

                int jobNum = JOBS.size() + 1
                JOBS["${TEST_JOB_NAME}_${jobNum}"] = {
                    def downstreamJob = build job: TEST_JOB_NAME, parameters: childParams, propagate: false, wait: true
                    def downstreamJobResult = downstreamJob.getResult()
                    echo "${TEST_JOB_NAME} result is ${downstreamJobResult}"
                    def buildId = downstreamJob.getNumber()
                    def childBuildUrl = "${env.JENKINS_URL}job/${TEST_JOB_NAME}/${buildId}"
                    def badgeUrl = "${childBuildUrl}/badge/icon"
                    currentBuild.description += """
                        <p>${TEST_JOB_NAME}/${buildId}:
                        <a href="${childBuildUrl}">
                            <img src="${badgeUrl}" />
                        </a>
                        </p>
                    """
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
                    currentBuild.result = downstreamJobResult
                }
            } else {
                println "Requested test job that does not exist or is disabled: ${TEST_JOB_NAME}. \n To generate the job, pelase set AUTO_AQA_GEN = true"
                currentBuild.result = "FAILURE"
            }
        }
    }
}

def remoteTriggerTemurinJCK (jobJdkVersion, jobPlatforms) {
    // Load JCK configuration from JSON files
    def jckConfig = loadJCKConfig(jobJdkVersion)
    
    if (!jckConfig) {
        error "Failed to load JCK configuration for JDK ${jobJdkVersion}"
    }
    
    // Determine if this is a release build (for SETUP_JCK_RUN)
    def isReleaseBuild = (params.BUILD_TYPE == "release")
    
    def globalConfig = jckConfig.GLOBAL_BUILD_CONFIG ?: [:]
    def targetSpecificConfig = jckConfig.TARGET_SPECIFIC_CONFIG ?: [:]
    def platformSpecificConfig = jckConfig.PLATFORM_SPECIFIC_CONFIG ?: [:]
    def platformApplicationOptions = jckConfig.PLATFORM_APPLICATION_OPTIONS ?: [:]
    def platformAdditionalTestLabels = jckConfig.PLATFORM_ADDITIONAL_TEST_LABELS ?: [:]
    def jckGitRepoTemplate = jckConfig.JCK_GIT_REPO_TEMPLATE ?: "git@github.com:temurin-compliance/JCK\${JDK_VERSION}-unzipped.git"
    
    // Get platform targets from config to determine which tests run on which platforms
    def platformTargets = jckConfig.PLATFORM_TARGETS ?: []
    
    jobPlatforms.each { platform ->
        // Find the targets for this platform from PLATFORM_TARGETS
        def platformTargetEntry = platformTargets.find { it.containsKey(platform) }
        if (!platformTargetEntry) {
            echo "No test targets defined for platform ${platform}, skipping"
            return
        }
        
        def targetsForPlatform = platformTargetEntry[platform].split(',').collect { it.trim() }
        
        targetsForPlatform.each { target ->
            // Create a unique job name for this JDK version+platform+target combination
            int jobNum = JOBS.size() + 1
            def jobName = "JCK_jdk${jobJdkVersion}_${platform}_${target}_${jobNum}"
            
            JOBS[jobName] = {
                // Build configuration by merging: global -> target-specific -> platform-specific
                def config = [:]
                
                // Start with global config
                globalConfig.each { k, v -> config[k] = v }
                
                // Apply target-specific config
                def targetConfig = targetSpecificConfig[target] ?: [:]
                targetConfig.each { k, v -> config[k] = v }
                
                // Apply platform-specific config for this target
                def platformConfig = (platformSpecificConfig[target] && platformSpecificConfig[target][platform]) ? platformSpecificConfig[target][platform] : [:]
                platformConfig.each { k, v ->
                    if (k == 'ADDITIONAL_TEST_PARAMS' && config[k]) {
                        // Merge ADDITIONAL_TEST_PARAMS
                        config[k] = config[k] + v
                    } else {
                        config[k] = v
                    }
                }
                
                // Apply platform-specific config for all jck targets
                def jckPlatformConfig = (platformSpecificConfig['jck'] && platformSpecificConfig['jck'][platform]) ? platformSpecificConfig['jck'][platform] : [:]
                jckPlatformConfig.each { k, v ->
                    if (k == 'ADDITIONAL_TEST_PARAMS') {
                        // Merge ADDITIONAL_TEST_PARAMS
                        if (!config[k]) config[k] = [:]
                        v.each { k2, v2 -> config[k][k2] = v2 }
                    } else if (!config.containsKey(k)) {
                        config[k] = v
                    }
                }
                
                // Get platform-specific APPLICATION_OPTIONS (customJtx path)
                def appOptions = platformApplicationOptions[platform] ?: ""
                appOptions = appOptions.replace('${JDK_VERSION}', jobJdkVersion)
                
                // Add any extra APPLICATION_OPTIONS from config
                def extraAppOptions = config.ADDITIONAL_TEST_PARAMS?.APPLICATION_OPTIONS ?: ""
                if (extraAppOptions) {
                    appOptions = appOptions ? "${appOptions} ${extraAppOptions}" : extraAppOptions
                }
                
                // Get EXTRA_OPTIONS
                def extraOptions = config.ADDITIONAL_TEST_PARAMS?.EXTRA_OPTIONS ?: ""
                
                // Get LABEL and LABEL_ADDITION
                def label = config.LABEL ?: ""
                def labelAddition = config.LABEL_ADDITION ?: ""
                
                // Add platform-specific additional labels
                def platformLabel = platformAdditionalTestLabels[platform] ?: ""
                if (platformLabel) {
                    labelAddition = labelAddition ? "${labelAddition}&&${platformLabel}" : platformLabel
                }
                
                // Build JCK_GIT_REPO from template
                def jckGitRepo = jckGitRepoTemplate.replace('${JDK_VERSION}', jobJdkVersion)
                
                // Get parallel and num_machines settings
                def parallel = config.PARALLEL ?: "None"
                def numMachines = config.NUM_MACHINES ?: "1"
                
                // Get other settings
                def rerunIterations = config.RERUN_ITERATIONS ?: "1"
                def rerunFailure = config.RERUN_FAILURE ? "true" : "false"
                // SETUP_JCK_RUN is true for release builds
                def setupJckRun = isReleaseBuild ? "true" : (config.SETUP_JCK_RUN ? "true" : "false")
                def autoAqaGen = config.AUTO_AQA_GEN ? "true" : "false"
                
                // Build display name
                def displayName = params.PIPELINE_DISPLAY_NAME ?: "${params.BUILD_TYPE} : jdk${jobJdkVersion} : ${platform} : ${target}"
                
                echo "Triggering ${target} on ${platform} with JDK ${jobJdkVersion}"
                echo "  PARALLEL: ${parallel}, NUM_MACHINES: ${numMachines}"
                echo "  JCK_GIT_REPO: ${jckGitRepo}"
                echo "  APPLICATION_OPTIONS: ${appOptions}"
                echo "  EXTRA_OPTIONS: ${extraOptions}"
                echo "  LABEL: ${label}, LABEL_ADDITION: ${labelAddition}"
                
                // Build parameter list
                def paramList = [
                    MapParameter(name: 'SDK_RESOURCE', value: 'customized'),
                    MapParameter(name: 'TARGETS', value: target),
                    MapParameter(name: 'JCK_GIT_REPO', value: jckGitRepo),
                    MapParameter(name: 'CUSTOMIZED_SDK_URL', value: params.CUSTOMIZED_SDK_URL),
                    MapParameter(name: 'JDK_VERSIONS', value: jobJdkVersion),
                    MapParameter(name: 'PARALLEL', value: parallel),
                    MapParameter(name: 'NUM_MACHINES', value: numMachines),
                    MapParameter(name: 'PLATFORMS', value: platform),
                    MapParameter(name: 'PIPELINE_DISPLAY_NAME', value: displayName),
                    MapParameter(name: 'APPLICATION_OPTIONS', value: appOptions),
                    MapParameter(name: 'LABEL_ADDITION', value: labelAddition),
                    MapParameter(name: 'AUTO_AQA_GEN', value: autoAqaGen),
                    MapParameter(name: 'RERUN_ITERATIONS', value: rerunIterations),
                    MapParameter(name: 'RERUN_FAILURE', value: rerunFailure),
                    MapParameter(name: 'EXTRA_OPTIONS', value: extraOptions),
                    MapParameter(name: 'SETUP_JCK_RUN', value: setupJckRun)
                ]
                
                // Add LABEL if specified
                if (label) {
                    paramList.add(MapParameter(name: 'LABEL', value: label))
                }
                
                // Trigger remote job
                def handle = triggerRemoteJob abortTriggeredJob: true,
                    blockBuildUntilComplete: true,
                    pollInterval: 240,
                    connectionRetryLimit: 5,
                    job: 'AQA_Test_Pipeline',
                    parameters: MapParameters(parameters: paramList),
                    remoteJenkinsName: 'temurin-compliance',
                    shouldNotFailBuild: true,
                    token: 'RemoteTrigger',
                    useCrumbCache: true,
                    useJobInfoCache: true
                    
                echo "Remote job ${displayName} Status: ${handle.getBuildResult().toString()}"
                
                // Update build result if any test fails
                if (handle.getBuildResult().toString() != 'SUCCESS') {
                    currentBuild.result = handle.getBuildResult().toString()
                }
            }
        }
    }
}

def loadJCKConfig(jdkVersion) {
    def jckConfig = [:]
    
    node("worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)") {
        checkout scm
        dir (env.WORKSPACE) {
            def jckDir = "./aqa-tests/buildenv/jenkins/config/temurin/jck/"
            
            // Load default.json (JDK 24+ settings)
            def defaultFile = "${jckDir}default.json"
            if (!fileExists(defaultFile)) {
                error "JCK default configuration not found: ${defaultFile}"
            }
            
            echo "Loading JCK default config from ${defaultFile}..."
            def defaultArray = readJSON(file: defaultFile)
            jckConfig = defaultArray[0] // Extract first element from array
            
            // Load version-specific override file
            // Priority: jdk${VERSION}.json > jdk21.json (for 17-21) > jdk11.json (for 8-11)
            // JDK 25+ uses default.json only (no override needed)
            def versionFile = null
            def jdkVersionInt = jdkVersion.toInteger()
            
            // Try exact version match first
            def exactVersionFile = "${jckDir}jdk${jdkVersion}.json"
            if (fileExists(exactVersionFile)) {
                versionFile = exactVersionFile
            } else if (jdkVersionInt >= 17 && jdkVersionInt <= 24) {
                // JDK 17-24 use jdk21.json
                versionFile = "${jckDir}jdk21.json"
            } else if (jdkVersionInt >= 8 && jdkVersionInt <= 11) {
                // JDK 8-11 use jdk11.json
                versionFile = "${jckDir}jdk11.json"
            }
            // JDK 25+ uses default.json only (no override)
            
            if (versionFile && fileExists(versionFile)) {
                echo "Loading JDK ${jdkVersion} specific config from ${versionFile}..."
                def versionArray = readJSON(file: versionFile)
                def versionConfig = versionArray[0]
                
                // Merge version-specific config (overrides default)
                versionConfig.each { key, value ->
                    if (value instanceof Map && jckConfig[key] instanceof Map) {
                        // Merge nested maps
                        value.each { k2, v2 ->
                            if (v2 instanceof Map && jckConfig[key][k2] instanceof Map) {
                                // Merge 2 levels deep
                                v2.each { k3, v3 -> jckConfig[key][k2][k3] = v3 }
                            } else {
                                jckConfig[key][k2] = v2
                            }
                        }
                    } else {
                        jckConfig[key] = value
                    }
                }
            } else {
                echo "No version-specific config found for JDK ${jdkVersion}, using default only"
            }
        }
    }
    
    return jckConfig
}
