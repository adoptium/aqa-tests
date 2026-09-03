#!/usr/bin/env groovy
/*
 * rerunJobs.groovy
 *
 * Standalone pipeline that triggers a rerun of the last build of a given Jenkins
 * job, mirroring the rerun logic in JenkinsfileBase (triggerRerunJob /
 * addFailedTestsGrinderLink).
 *
 * Uses the Jenkins REST API (JSON) to inspect builds — no Jenkins.instance calls,
 * fully compatible with the script security sandbox.
 *
 * Parameters
 * ----------
 *   JOB_NAME                  (required) Full Jenkins job name to inspect.
 *                             If the job does not exist, no rerun is triggered
 *                             and a message is logged.
 *   SCM_REFERENCE              (optional) When non-empty, checks that the last
 *                             build's CUSTOMIZED_SDK_URL parameter contains this
 *                             substring; if it does not match, no rerun is
 *                             triggered and a message is logged.
 *                             When empty, the check is skipped entirely.
 *   MODE                      (optional) When set to 'RELAY', this pipeline acts
 *                             as a relay: it forwards the rerun request to the
 *                             same rerunJobs pipeline on the remote private Jenkins
 *                             (temurin-compliance) via triggerRemoteJob, instead of
 *                             inspecting and triggering jobs locally.
 *
 * Pre-conditions (all checked before any rerun is triggered — local mode only)
 * ----------------------------------------------------------------------------
 *   • Job does not exist          → FAILURE, log and stop.
 *   • Last build is still running → FAILURE, log and stop.
 *   • SCM_REFERENCE set but does not match CUSTOMIZED_SDK_URL → FAILURE, log and stop.
 *   • Last build result is SUCCESS → SUCCESS, log "passed, no rerun needed" and stop.
 *
 * Rerun rules — always re-triggers <JOB_NAME> itself, never a separate _rerun job
 * ----------------------------------------------------------------------------------
 *   FAILURE / ABORTED  → re-trigger <JOB_NAME> with identical parameters (full rebuild).
 *   UNSTABLE           → parse the build description for rerun links produced by
 *                        addFailedTestsGrinderLink(). TARGET and CUSTOM_TARGET are
 *                        extracted from the parambuild URL and used to re-trigger
 *                        <JOB_NAME> with overridden parameters:
 *                          • _custom links present → re-trigger once per _custom link
 *                            in parallel; skip "failed test targets" (redundant).
 *                          • Only "failed test targets" → re-trigger once with
 *                            TARGET=testList TESTLIST=<csv> override only.
 *                          • No links found → fall back to full rebuild.
 *
 * Job status: after all rerun tasks complete this pipeline's result is set to
 * the worst status among them (ABORTED > FAILURE > UNSTABLE > SUCCESS).
 */

pipeline {
    agent { label 'worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)' }

    parameters {
        string(
            name: 'JOB_NAME',
            defaultValue: '',
            description: 'Full Jenkins job name to rerun (e.g. Test_openjdk21_hs_sanity.functional_x86-64_linux)'
        )
        string(
            name: 'SCM_REFERENCE',
            defaultValue: '',
            description: '(Optional) Substring that must appear in the last build\'s CUSTOMIZED_SDK_URL parameter. ' +
                         'When empty, the check is skipped entirely.'
        )
        string(
            name: 'MODE',
            defaultValue: '',
            description: '(Optional) Set to RELAY to forward the rerun request to the remote private Jenkins ' +
                         '(temurin-compliance) instead of running locally.'
        )
    }

    options {
        timestamps()
        skipDefaultCheckout()
    }

    stages {
        stage('Rerun') {
            steps {
                script {
                    def jobName      = params.JOB_NAME?.trim()
                    def scmReference = params.SCM_REFERENCE?.trim() ?: ''
                    def mode         = params.MODE?.trim() ?: ''

                    if (!jobName) {
                        error "JOB_NAME parameter must be set."
                    }

                    // --- RELAY mode: forward to remote private Jenkins ---
                    if (mode == 'RELAY') {
                        echo "=== rerunJobs RELAY: forwarding rerun of '${jobName}' to temurin-compliance ==="
                        def remoteParamList = [
                            MapParameter(name: 'JOB_NAME',      value: jobName),
                            MapParameter(name: 'SCM_REFERENCE', value: scmReference)
                        ]
                        def handle = triggerRemoteJob(
                            abortTriggeredJob:      true,
                            blockBuildUntilComplete: true,
                            pollInterval:           60,
                            job:                    'rerunJobs',
                            parameters:             MapParameters(parameters: remoteParamList),
                            remoteJenkinsName:      'temurin-compliance',
                            shouldNotFailBuild:     false,
                            token:                  'RemoteTrigger',
                            useCrumbCache:          true,
                            useJobInfoCache:        true
                        )
                        def remoteResult = handle.getBuildResult().toString()
                        def remoteBuildNum = handle.getBuildNumber()
                        echo "Remote rerunJobs #${remoteBuildNum} result: ${remoteResult}"
                        currentBuild.description = (currentBuild.description ?: '') +
                            "<br><a href='https://ci.eclipse.org/temurin-compliance/job/rerunJobs/${remoteBuildNum}'>" +
                            "Remote rerunJobs #${remoteBuildNum}: ${remoteResult}</a>"
                        setWorstResult(remoteResult)
                        return
                    }

                    echo "=== rerunJobs: inspecting last build of '${jobName}'" +
                         (scmReference ? " (SCM_REFERENCE='${scmReference}')" : '') + " ==="

                    // --- Fetch last build info via Jenkins REST API ---
                    def buildInfo = getLastBuildInfo(jobName)
                    if (!buildInfo) {
                        echo "Job '${jobName}' not found or has no builds. No rerun triggered."
                        currentBuild.result = 'FAILURE'
                        return
                    }

                    def buildNum = buildInfo.number as int
                    def building = buildInfo.building as boolean
                    def result   = buildInfo.result ?: 'UNKNOWN'

                    // --- Check if still running ---
                    if (building) {
                        echo "Last build #${buildNum} of '${jobName}' is still running. No rerun triggered."
                        currentBuild.result = 'FAILURE'
                        return
                    }

                    echo "Last build: #${buildNum}, result: ${result}"

                    // --- Optional SCM_REFERENCE filter check against CUSTOMIZED_SDK_URL ---
                    if (scmReference) {
                        def sdkUrl = getBuildParamFromInfo(buildInfo, 'CUSTOMIZED_SDK_URL') ?: ''
                        if (!sdkUrl.contains(scmReference)) {
                            echo "CUSTOMIZED_SDK_URL ('${sdkUrl}') does not contain SCM_REFERENCE '${scmReference}'. No rerun triggered."
                            currentBuild.result = 'FAILURE'
                            return
                        }
                        echo "CUSTOMIZED_SDK_URL matches SCM_REFERENCE — proceeding."
                    }

                    // --- Check result ---
                    if (result == 'SUCCESS') {
                        echo "Last build #${buildNum} passed. No rerun needed."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    // --- Build and run rerun tasks ---
                    def rerunTasks = buildRerunTasks(jobName, buildInfo, result)

                    if (rerunTasks.isEmpty()) {
                        echo "Nothing to rerun for '${jobName}' (result: ${result})."
                        currentBuild.result = 'FAILURE'
                        return
                    }

                    echo "Triggering ${rerunTasks.size()} rerun task(s) in parallel ..."
                    def rerunResults = parallel rerunTasks
                    // Set this pipeline's result to the worst status among all rerun jobs.
                    rerunResults.each { key, res ->
                        setWorstResult(res?.toString())
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Jenkins REST API helpers
// ---------------------------------------------------------------------------

/**
 * Fetch the last build JSON for a job via the Jenkins REST API using curl.
 * Returns the parsed JSON map, or null if the job/build does not exist.
 * Folder-style job names (with '/') are converted to /job/A/job/B paths.
 */
def getLastBuildInfo(String jobName) {
    def jobPath = jobName.split('/').join('/job/')
    def apiUrl  = "${env.JENKINS_URL}job/${jobPath}/lastBuild/api/json?tree=number,building,result,description,actions%5Bparameters%5Bname,value%5D%5D"
    return fetchBuildJson(apiUrl, "last build of '${jobName}'")
}

/**
 * Internal helper: curl a Jenkins API URL and parse the JSON response.
 * Returns the parsed map, or null on HTTP error or missing content.
 */
def fetchBuildJson(String apiUrl, String label) {
    try {
        def json = sh(
            script: "curl -sf --connect-timeout 10 '${apiUrl}'",
            returnStdout: true
        ).trim()
        if (!json) {
            echo "No response from Jenkins API for ${label}."
            return null
        }
        return readJSON(text: json)
    } catch (Exception e) {
        echo "Could not fetch ${label}: ${e.message}"
        return null
    }
}

/**
 * Extract a named parameter value from a build info JSON map (from REST API).
 * Returns null if not found.
 */
def getBuildParamFromInfo(def buildInfo, String paramName) {
    def actions = buildInfo?.actions ?: []
    for (action in actions) {
        def parameters = action?.parameters ?: []
        for (p in parameters) {
            if (p?.name == paramName) return p?.value?.toString()
        }
    }
    return null
}

/**
 * Collect all build parameters from a build info JSON map as a List of plain
 * Groovy maps [name: k, value: v, type: 'bool'|'string'].
 * Converted to Jenkins parameter objects only when passed to `build` via
 * toJenkinsParams().
 */
def collectParamsFromInfo(def buildInfo) {
    def result  = []
    def actions = buildInfo?.actions ?: []
    for (action in actions) {
        def parameters = action?.parameters ?: []
        for (p in parameters) {
            if (p?.name) {
                def val = p?.value?.toString() ?: ''
                result << [name: p.name, value: val, type: (val == 'true' || val == 'false') ? 'bool' : 'string']
            }
        }
    }
    return result
}

/**
 * Convert a list of plain param maps [name, value, type] to Jenkins parameter
 * objects suitable for `build job:…, parameters:`.
 */
def toJenkinsParams(List paramMaps) {
    return paramMaps.collect { p ->
        p.type == 'bool' ? booleanParam(name: p.name, value: p.value.toBoolean())
                         : string(name: p.name, value: p.value ?: '')
    }
}

// ---------------------------------------------------------------------------
// Core rerun task builder
// ---------------------------------------------------------------------------

/**
 * Inspect buildInfo and return a map of parallel closures that trigger the
 * appropriate rerun(s) of jobName.
 *
 * For UNSTABLE: parse rerun links from the top-level description first.
 * Additionally, if the description contains badge links to child jobs whose
 * name ends with '_rerun', those children are also inspected and their rerun
 * links are collected (these are _rerun children that already have a targeted
 * rerun link in their own description).
 * Regular _testList_ children are ignored.
 */
def buildRerunTasks(String jobName, def buildInfo, String result) {
    def tasks          = [:]
    def originalParams = collectParamsFromInfo(buildInfo)

    if (result == 'FAILURE' || result == 'ABORTED') {
        // Re-trigger the same job with identical parameters (full rebuild).
        tasks[jobName] = makeRerunClosure(jobName, originalParams, null, null)
        return tasks
    }

    if (result == 'UNSTABLE') {
        def description = buildInfo?.description ?: ''

        // Parse rerun links from the top-level description.
        def topTasks = rerunTasksFromLinks(jobName, originalParams, description)
        tasks.putAll(topTasks)

        // Also inspect any _rerun child jobs found in the description.
        def childEntries = parseRerunChildJobEntries(description)
        childEntries.each { childName, childBuildNum ->
            def jobPath    = childName.split('/').join('/job/')
            def apiUrl     = "${env.JENKINS_URL}job/${jobPath}/${childBuildNum}/api/json?tree=number,building,result,description,actions%5Bparameters%5Bname,value%5D%5D"
            def childInfo  = fetchBuildJson(apiUrl, "build #${childBuildNum} of '${childName}'")
            if (!childInfo) return
            def childResult = childInfo?.result ?: 'UNKNOWN'
            echo "  _rerun child '${childName}' #${childBuildNum}: ${childResult}"
            if (childResult == 'UNSTABLE') {
                def childParams = collectParamsFromInfo(childInfo)
                def childTasks  = rerunTasksFromLinks(childName, childParams, childInfo?.description ?: '')
                tasks.putAll(childTasks)
            } else if (childResult == 'FAILURE' || childResult == 'ABORTED') {
                def childParams = collectParamsFromInfo(childInfo)
                tasks[childName] = makeRerunClosure(childName, childParams, null, null)
            }
        }
    }

    return tasks
}

/**
 * Build the rerun task map for an UNSTABLE job from its description links:
 *   - _custom links present   → trigger jobName once per _custom link (in parallel);
 *                               skip "failed test targets" link (redundant).
 *   - Only "failed targets"   → trigger jobName once with TARGET override only.
 *   - No links found          → fall back to full rebuild of jobName.
 */
def rerunTasksFromLinks(String jobName, List originalParams, String description) {
    def tasks    = [:]
    def allLinks = parseRerunLinks(description)

    if (allLinks.isEmpty()) {
        // No links — fall back to full rebuild of the same job.
        tasks["${jobName}_rebuild"] = makeRerunClosure(jobName, originalParams, null, null)
        return tasks
    }

    def customLinks = allLinks.findAll { it.isCustom }
    def activeLinks = customLinks ?: allLinks   // prefer _custom; fall back to failed-targets

    activeLinks.eachWithIndex { entry, idx ->
        tasks["${jobName}_${idx}"] = makeRerunClosure(jobName, originalParams, entry.target, entry.customTarget)
    }
    return tasks
}

// ---------------------------------------------------------------------------
// Closure factory
// ---------------------------------------------------------------------------

/**
 * Re-triggers jobName with overridden TARGET/CUSTOM_TARGET (null = keep original)
 * and PARALLEL/NUM_MACHINES/TEST_TIME reset.
 */
def makeRerunClosure(String jobName, List baseParams, String target, String customTarget) {
    return {
        def label = target ? "TARGET=${target}" : "(full rebuild)"
        echo "--- Triggering: ${jobName} ${label} ---"

        def jobParams       = toJenkinsParams(overrideParams(baseParams, target, customTarget))
        def downstreamBuild = build job: jobName, parameters: jobParams, propagate: false, wait: true
        def rerunResult     = downstreamBuild.getResult()?.toString() ?: 'UNKNOWN'
        def rerunBuildNum   = downstreamBuild.getNumber()

        echo "${jobName} #${rerunBuildNum} → ${rerunResult}"
        currentBuild.description = (currentBuild.description ?: '') +
            "<br><a href='${env.JENKINS_URL}job/${jobName.replace('/', '/job/')}/${rerunBuildNum}'>" +
            "${jobName} #${rerunBuildNum}: ${rerunResult}</a>"

        setWorstResult(rerunResult)
    }
}

// ---------------------------------------------------------------------------
// Parameter helpers
// ---------------------------------------------------------------------------

/**
 * Clone baseParams (plain maps), overriding TARGET / CUSTOM_TARGET (when non-null)
 * and resetting PARALLEL / NUM_MACHINES / TEST_TIME as JenkinsfileBase does.
 * Returns a list of plain maps; call toJenkinsParams() before passing to `build`.
 */
def overrideParams(List baseParams, String target, String customTarget) {
    def result = []
    baseParams.each { p ->
        def key = p.name
        if (key == 'TARGET' && target != null) {
            result << [name: 'TARGET', value: target, type: 'string']
        } else if (key == 'CUSTOM_TARGET' && customTarget != null) {
            result << [name: 'CUSTOM_TARGET', value: customTarget, type: 'string']
        } else if (key == 'PARALLEL') {
            result << [name: 'PARALLEL', value: 'None', type: 'string']
        } else if (key == 'NUM_MACHINES') {
            result << [name: 'NUM_MACHINES', value: '', type: 'string']
        } else if (key == 'TEST_TIME') {
            result << [name: 'TEST_TIME', value: '', type: 'string']
        } else {
            result << p
        }
    }
    return result
}

// ---------------------------------------------------------------------------
// Description parsers
// ---------------------------------------------------------------------------

/**
 * Parse rerun links written by addFailedTestsGrinderLink() from a build description.
 * Extracts TARGET and CUSTOM_TARGET from the parambuild query string.
 *
 * Returns a list of maps:
 *   { target: String, customTarget: String|null, isCustom: boolean }
 *
 *   isCustom=false  → "Rerun in Grinder with failed test targets"
 *                     TARGET=testList TESTLIST=<csv>, CUSTOM_TARGET=null
 *   isCustom=true   → "Rerun failed <x> test cases … <x>_custom target"
 *                     TARGET=<x>_custom, CUSTOM_TARGET=<cases>
 */
def parseRerunLinks(String description) {
    def links = []
    if (!description) return links

    // Match both link forms and capture the full href URL.
    def m = (description =~
        /href=['"]?([^'">\s]*parambuild[^'">\s]*)['"]?[^>]*>\s*(Rerun in Grinder with failed test targets|Rerun failed \S+ test cases in Grinder with \S+_custom target)/)
    m.each {
        def fullUrl  = it[1].replace('&amp;', '&')
        def isCustom = it[2].contains('_custom target')

        // Parse TARGET and CUSTOM_TARGET out of the query string.
        def queryString = fullUrl.contains('?') ? fullUrl.split('\\?', 2)[1] : ''
        def qParams = [:]
        queryString.split('&').findAll { it }.each { pair ->
            def kv    = pair.split('=', 2)
            def key   = java.net.URLDecoder.decode(kv[0], 'UTF-8')
            def value = kv.size() > 1 ? java.net.URLDecoder.decode(kv[1].replace('+', ' '), 'UTF-8') : ''
            qParams[key] = value
        }

        def target       = qParams['TARGET'] ?: ''
        def customTarget = isCustom ? (qParams['CUSTOM_TARGET'] ?: '') : null

        if (target) {
            links << [target: target, customTarget: customTarget, isCustom: isCustom]
        }
    }

    return links
}

/**
 * Extract (childJobName → buildNumber) pairs from a build description,
 * only for child jobs whose name contains '_testList_' AND ends with '_rerun'
 * (e.g. Test_openjdk25_hs_..._testList_2_rerun).
 * Plain _testList_N jobs and plain _rerun jobs are both ignored.
 * Looks for badge-icon links: /job/<childName>/<buildNum>/badge/icon
 * Returns a Map of childJobName → buildNumber (int). Empty map if none found.
 */
def parseRerunChildJobEntries(String description) {
    def entries = [:]
    if (!description) return entries

    def m = (description =~ /\/job\/([A-Za-z0-9_.\-]+)\/(\d+)\/badge\/icon/)
    m.each {
        def childName = it[1]
        if (childName.contains('_testList_') && childName.endsWith('_rerun')) {
            entries[childName] = it[2].toInteger()
        }
    }
    return entries
}

// ---------------------------------------------------------------------------
// Status helper
// ---------------------------------------------------------------------------

/**
 * Update this pipeline's result to the worst of its current result and newResult.
 * Priority: ABORTED > FAILURE > UNSTABLE > SUCCESS.
 */
def setWorstResult(String newResult) {
    if (!newResult) return
    def priority = ['SUCCESS': 1, 'UNSTABLE': 2, 'FAILURE': 3, 'ABORTED': 4]
    def current  = currentBuild.result ?: 'SUCCESS'
    if ((priority[newResult] ?: 0) > (priority[current] ?: 0)) {
        currentBuild.result = newResult
    }
}
