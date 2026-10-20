#!groovy
/*
 * TapArtifactCollector.groovy
 *
 * NEW STRATEGY — Reverse-engineering the trigger chain
 * =====================================================
 * Traditional TAP collection tools (e.g. TapCollection.groovy) work by tracing
 * FORWARD: starting from a known upstream pipeline run, following its child jobs
 * down to the test jobs, and collecting results.  That approach requires direct
 * knowledge of the job topology and is tightly coupled to how the upstream
 * pipeline fans out its work.
 *
 * This pipeline takes a completely NEW approach: REVERSE ENGINEERING.
 * Instead of tracing forward from the upstream pipeline, it inspects each
 * AQA_Test_Pipeline_RELEASE build and traces BACKWARD through its cause chain
 * to determine whether it was ultimately triggered by the specified upstream
 * pipeline run.  This decouples the collector from the upstream topology — it
 * does not need to know how the release pipeline fans out; it simply asks each
 * test pipeline build "were you caused by this release?".
 *
 * This strategy is made possible by the new test job implementation in which
 * AQA_Test_Pipeline_RELEASE records full cause provenance through the
 * intermediate build-scripts jobs, allowing the origin pipeline and build
 * number to be recovered by walking up the cause chain via the Jenkins REST API.
 *
 * Pipeline overview
 * -----------------
 * Stage 0 – Clean Workspace
 *   Wipes the workspace before every run to prevent artifacts from a previous
 *   run polluting the current one.
 *
 * Stage 1 – Collect Jenkins Artifacts  [REVERSE-ENGINEERING]
 *   Scans all builds of TEST_PIPELINE_JOB (default: AQA_Test_Pipeline_RELEASE)
 *   and identifies every build whose cause chain traces back to the specified
 *   PIPELINE_NAME + BUILD_NUMBERS.  Matching works in two steps:
 *
 *   Step A — check direct cause (no auth required):
 *     Each AQA_Test_Pipeline_RELEASE build records its immediate upstream cause
 *     (an intermediate build-scripts per-platform job).  If that job itself
 *     matches PIPELINE_NAME + build number, we are done.
 *
 *   Step B — climb one level (auth required):
 *     The immediate upstream is normally a per-platform build-scripts job
 *     (e.g. jdk8u-release-linux-arm-temurin), not the top-level release pipeline.
 *     The pipeline fetches that intermediate build via the Jenkins REST API
 *     (using upstreamUrl for the correct path) and checks ITS cause, which
 *     contains the original release-openjdk*-pipeline trigger.
 *
 *   For each matched build, the platform name is read from that build's own
 *   displayName (last colon-separated token, e.g. "s390x_linux") and its full
 *   artifact tree is downloaded as <platform>.zip.
 *
 * Stage 2 – Collect GitHub TAP Attachments
 *   Given a GitHub triage issue URL, downloads all .tap.txt file attachments
 *   from the main issue and each child issue linked in its description.
 *   Child issues follow the naming convention
 *     "Triage Automated Tests for <release> <jdk> - <platform>"
 *   so the platform is extracted from the title (or body "Platform:" line) and
 *   attachments are grouped per platform as grinder_<platform>.zip.
 *
 * Stage 3 – Merge Artifacts
 *   Merges the Jenkins artifacts (<platform>.zip) and GitHub attachments
 *   (grinder_<platform>.zip) for each platform into a single <platform>.tar.gz.
 *   Platforms that only have one source are still converted to .tar.gz.
 *   Only the final <platform>.tar.gz files are archived as build artifacts.
 *
 * Parameters
 * ----------
 *   PIPELINE_NAME        Upstream release pipeline name to match.
 *                        Example: release-openjdk8-pipeline
 *   BUILD_NUMBERS        Comma-separated build numbers of that pipeline.
 *                        Example: 130,131
 *   TEST_PIPELINE_JOB    Jenkins job to scan.  Default: AQA_Test_Pipeline_RELEASE
 *   GITHUB_ISSUE_URL     Main GitHub triage issue URL.
 *                        Example: https://github.com/adoptium/aqa-tests/issues/7612
 *   GITHUB_CREDENTIAL    Secret text credential for the GitHub API token.
 */

pipeline {
    agent { label 'ci.role.test&&hw.arch.x86&&sw.os.linux' }

    parameters {
        string(
            name: 'PIPELINE_NAME',
            defaultValue: '',
            description: 'Upstream pipeline name to match (e.g. release-openjdk8-pipeline)'
        )
        string(
            name: 'BUILD_NUMBERS',
            defaultValue: '',
            description: 'Comma-separated upstream build numbers to match (e.g. 130,131)'
        )
        string(
            name: 'TEST_PIPELINE_JOB',
            defaultValue: 'AQA_Test_Pipeline_RELEASE',
            description: 'Jenkins job to scan for matching builds'
        )
        string(
            name: 'GITHUB_ISSUE_URL',
            defaultValue: '',
            description: 'GitHub issue URL (e.g. https://github.com/adoptium/aqa-tests/issues/7612)'
        )
        credentials(
            name: 'GITHUB_CREDENTIAL',
            defaultValue: 'github-bot-token',
            description: 'Secret text credential containing the GitHub personal access token',
            credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
            required: false
        )
    }

    environment {
        // Bind JENKINS_AUTH (user:token) at pipeline level so it is available
        // to all sh steps including those called from helper functions.
        // The value is masked in logs by Jenkins automatically.
        JENKINS_AUTH = credentials('eclipse_temurin_bot_email_and_token')
    }

    options {
        timestamps()
        skipDefaultCheckout()
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        // -----------------------------------------------------------------------
        // Stage 1: Collect TAP artifacts from matching Jenkins builds
        // -----------------------------------------------------------------------
        stage('Collect Jenkins Artifacts') {
            steps {
                script {
                    def pipelineName  = params.PIPELINE_NAME?.trim()
                    def buildNumbers  = params.BUILD_NUMBERS?.trim()
                    def testJob       = params.TEST_PIPELINE_JOB?.trim() ?: 'AQA_Test_Pipeline_RELEASE'

                    if (!pipelineName) { error "PIPELINE_NAME parameter must be set." }
                    if (!buildNumbers) { error "BUILD_NUMBERS parameter must be set." }

                    def targetBuildNums = buildNumbers.split(/\s*,\s*/).collect { it.trim() }.findAll { it } as Set

                    echo "=== Stage 1: scanning '${testJob}' for builds triggered by '${pipelineName}' #${targetBuildNums} ==="

                    def jobPath = testJob.split('/').join('/job/')
                    // Fetch the list of all build numbers for the job.
                    def allBuildsJson = fetchJson(
                        "${env.JENKINS_URL}job/${jobPath}/api/json?tree=builds%5Bnumber%5D",
                        "build list of '${testJob}'"
                    )
                    if (!allBuildsJson || !allBuildsJson.builds) {
                        echo "No builds found for '${testJob}'. Skipping Stage 1."
                        return
                    }

                    // Each entry: [number: N, platform: 'x86-64_windows']
                    // One upstream build number fans out to many AQA_Test_Pipeline_RELEASE
                    // builds, one per platform. Each zip is named <platform>.zip.
                    def matchedBuilds = []

                    allBuildsJson.builds.each { b ->
                        def num = b.number as int
                        def buildApiUrl = "${env.JENKINS_URL}job/${jobPath}/${num}/api/json" +
                            "?tree=number,displayName,description,actions%5Bcauses%5BupstreamProject,upstreamBuild,upstreamUrl,shortDescription%5D%5D"
                        // AQA_Test_Pipeline_RELEASE is public — no auth needed here.
                        def info = fetchJson(buildApiUrl, "'${testJob}' #${num}", false)
                        if (!info) return

                        // Gate: cause chain must match PIPELINE_NAME + one of BUILD_NUMBERS.
                        // useAuth=true so the parent build-scripts fetch uses $JENKINS_AUTH from env.
                        if (!isBuildTriggeredBy(info, pipelineName, targetBuildNums, 3, true)) return

                        // Platform always comes from the build's own display name / description,
                        // since each build is for exactly one platform regardless of which
                        // upstream build number triggered it.
                        // Use jsonStr() to safely handle net.sf.json.JSONNull values.
                        def platform = extractPlatformFromDisplayName(
                            jsonStr(info.displayName),
                            jsonStr(info.description)
                        )
                        echo "  Matched build #${num} — platform: '${platform}'"
                        matchedBuilds << [number: num, platform: platform]
                    }

                    if (matchedBuilds.isEmpty()) {
                        echo "No matching builds found in '${testJob}'."
                        return
                    }

                    echo "=== Found ${matchedBuilds.size()} matching build(s) ==="
                    matchedBuilds.each { entry ->
                        def buildUrl = "${env.JENKINS_URL}job/${jobPath}/${entry.number}/"
                        echo "  #${entry.number} — platform: '${entry.platform}' — ${buildUrl}"
                    }
                    echo "Downloading artifacts..."

                    matchedBuilds.each { entry ->
                        def bNum     = entry.number
                        def platform = entry.platform ?: "unknown"
                        def zipName  = "${platform}.zip"
                        echo "  Downloading artifacts from '${testJob}' #${bNum} → ${zipName} ..."
                        try {
                            def artifactZipUrl = "${env.JENKINS_URL}job/${jobPath}/${bNum}/artifact/*zip*/archive.zip"
                            sh "curl -sf -o '${zipName}' '${artifactZipUrl}'"
                        } catch (Exception e) {
                            echo "  WARNING: Failed to download artifacts for build #${bNum}: ${e.message}"
                        }
                    }

                    // platform.zip files are intermediate — not archived here; merged into *.tar.gz in Stage 3.
                }
            }
        }

        // -----------------------------------------------------------------------
        // Stage 2: Collect .tap.txt attachments from GitHub issue + child issues
        // -----------------------------------------------------------------------
        stage('Collect GitHub TAP Attachments') {
            steps {
                script {
                    def issueUrl    = params.GITHUB_ISSUE_URL?.trim()
                    def credId      = params.GITHUB_CREDENTIAL?.trim() ?: 'github-bot-token'

                    if (!issueUrl) {
                        echo "GITHUB_ISSUE_URL not set — skipping Stage 2."
                        return
                    }

                    // Parse  https://github.com/<owner>/<repo>/issues/<number>
                    def m = issueUrl =~ /github\.com\/([^\/]+\/[^\/]+)\/issues\/(\d+)/
                    if (!m) { error "Cannot parse GITHUB_ISSUE_URL: '${issueUrl}'" }
                    def repoSlug    = m[0][1]
                    def issueNumber = m[0][2]

                    echo "=== Stage 2: collecting .tap.txt attachments for ${repoSlug}#${issueNumber} ==="

                    withCredentials([string(credentialsId: credId, variable: 'GITHUB_TOKEN')]) {

                        // Helper: fetch JSON from GitHub API
                        def ghFetch = { url, label ->
                            def outFile = "gh_${label}.json"
                            sh """curl -sf -H "Authorization: token \${GITHUB_TOKEN}" \
                                -H "Accept: application/vnd.github.v3+json" \
                                "${url}" -o "${outFile}" """
                            return readJSON(file: outFile)
                        }

                        // Helper: download all .tap.txt attachments from a Markdown body
                        // into destDir. Mirrors TapCollection.groovy URL extraction logic.
                        def downloadTapTxt = { body, destDir ->
                            if (!body) return
                            body.split(/\r?\n/).each { line ->
                                if (!line.endsWith(')')) return
                                if (!line.contains('https://github.com/user-attachments/files/') &&
                                    !line.contains("https://github.com/${repoSlug}/files/")) return
                                def urlStart = line.indexOf('(https://github.com/user-attachments/files/')
                                if (urlStart < 0) urlStart = line.indexOf("(https://github.com/${repoSlug}/files/")
                                if (urlStart < 0) return
                                def url      = line.substring(urlStart + 1, line.lastIndexOf(')'))
                                if (!url.endsWith('.tap.txt')) return
                                def filename = url.split('/').last()
                                echo "    Downloading ${filename} → ${destDir}/"
                                sh "curl -Lsf -o '${destDir}/${filename}' '${url}'"
                            }
                        }

                        def apiBase = "https://api.github.com/repos/${repoSlug}"

                        // --- Main issue: download attachments into TAPs/main/ ---
                        def issue    = ghFetch("${apiBase}/issues/${issueNumber}", "main_issue")
                        def comments = ghFetch("${apiBase}/issues/${issueNumber}/comments", "main_comments")
                        sh "mkdir -p TAPs/main"
                        downloadTapTxt(issue.body, 'TAPs/main')
                        comments.each { c -> downloadTapTxt(c.body, 'TAPs/main') }

                        // --- Child issues: each gets its own platform subdir ---
                        def childNums = extractChildIssueNumbers(issue.body ?: '', repoSlug)
                        echo "Found ${childNums.size()} child issue(s): ${childNums}"

                        childNums.eachWithIndex { childNum, idx ->
                            echo "  Processing child issue #${childNum} ..."
                            try {
                                def childIssue    = ghFetch("${apiBase}/issues/${childNum}", "child_${idx}_issue")
                                def childComments = ghFetch("${apiBase}/issues/${childNum}/comments", "child_${idx}_comments")

                                // Extract platform from child issue title ("... - x86-64_linux")
                                // or fall back to body "Platform: x86-64_linux" line.
                                def platform = extractPlatformFromIssueTitle(childIssue.title ?: '')
                                if (!platform) platform = extractPlatformFromIssueBody(childIssue.body ?: '')
                                if (!platform) platform = "child_${childNum}"
                                echo "    Platform: '${platform}'"

                                def destDir = "TAPs/${platform}"
                                sh "mkdir -p '${destDir}'"
                                downloadTapTxt(childIssue.body, destDir)
                                childComments.each { c -> downloadTapTxt(c.body, destDir) }

                                // Zip all downloaded .tap.txt files as grinder_<platform>.zip
                                def tapCount = sh(script: "ls '${destDir}'/*.tap.txt 2>/dev/null | wc -l", returnStdout: true).trim().toInteger()
                                if (tapCount > 0) {
                                    sh "cd '${destDir}' && zip -j '../../grinder_${platform}.zip' *.tap.txt"
                                    echo "    Created grinder_${platform}.zip (${tapCount} file(s))"
                                } else {
                                    echo "    No .tap.txt files found for platform '${platform}'"
                                }
                            } catch (Exception e) {
                                echo "  WARNING: Failed to process child issue #${childNum}: ${e.message}"
                            }
                        }
                    }

                    // grinder_*.zip files are intermediate — not archived here; merged into *.tar.gz in Stage 3.
                }
            }
        }

        // -----------------------------------------------------------------------
        // Stage 3: Merge Jenkins artifacts + GitHub attachments per platform
        // -----------------------------------------------------------------------
        stage('Merge Artifacts') {
            steps {
                script {
                    // Collect all platforms from both sources:
                    //   - grinder_<platform>.zip  → GitHub attachments (Stage 2)
                    //   - <platform>.zip           → Jenkins artifacts  (Stage 1)
                    def platforms = [] as LinkedHashSet

                    findFiles(glob: 'grinder_*.zip').each { f ->
                        platforms << f.name.replaceFirst(/^grinder_/, '').replaceFirst(/\.zip$/, '')
                    }
                    findFiles(glob: '*.zip').findAll { !it.name.startsWith('grinder_') }.each { f ->
                        platforms << f.name.replaceFirst(/\.zip$/, '')
                    }

                    if (platforms.isEmpty()) {
                        echo "No zip files found — skipping merge."
                        return
                    }

                    platforms.each { platform ->
                        def grinderZip  = "grinder_${platform}.zip"
                        def jenkinsZip  = "${platform}.zip"
                        def mergeDir    = "merge_${platform}"
                        def haGrinder   = sh(script: "test -f '${grinderZip}'",  returnStatus: true) == 0
                        def hasJenkins  = sh(script: "test -f '${jenkinsZip}'",  returnStatus: true) == 0

                        sh "mkdir -p '${mergeDir}'"

                        if (haGrinder) {
                            sh "unzip -o '${grinderZip}' -d '${mergeDir}'"
                            echo "  Unpacked grinder attachments for '${platform}'"
                        }
                        if (hasJenkins) {
                            sh "unzip -o '${jenkinsZip}' -d '${mergeDir}'"
                            echo "  Unpacked Jenkins artifacts for '${platform}'"
                        }

                        // Pack everything into <platform>.tar.gz
                        sh "tar -czf '${platform}.tar.gz' -C '${mergeDir}' ."
                        echo "  Created ${platform}.tar.gz (grinder=${haGrinder}, jenkins=${hasJenkins})"
                    }

                    archiveArtifacts artifacts: '*.tar.gz', allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Safely convert a JSON field value to a plain Groovy String.
 * Handles Groovy null, net.sf.json.JSONNull, and normal values.
 * Returns empty string for null/JSONNull so callers never get a crash on .trim() etc.
 */
def jsonStr(def value) {
    if (value == null) return ''
    def s = value.toString()
    return (s == 'null') ? '' : s.trim()
}

/**
 * Fetch a URL with curl and return a parsed JSON map.
 * @param useAuth  When true, passes -u "$JENKINS_AUTH" to curl. JENKINS_AUTH must already
 *                 be bound in the shell environment via withCredentials — it is never
 *                 interpolated into the Groovy string, eliminating the insecure-interpolation warning.
 * Returns null on failure.
 */
def fetchJson(String url, String label, boolean useAuth = false) {
    try {
        def script = useAuth
            ? "curl -sf --connect-timeout 10 -u \"\$JENKINS_AUTH\" '${url}'"
            : "curl -sf --connect-timeout 10 '${url}'"
        def json = sh(script: script, returnStdout: true).trim()
        if (!json) { echo "Empty response for ${label}"; return null }
        return readJSON(text: json)
    } catch (Exception e) {
        echo "Failed to fetch ${label}: ${e.message}"
        return null
    }
}

/**
 * Check whether a build was triggered (directly or indirectly) by the given
 * upstream pipeline name + one of the target build numbers.
 *
 * AQA_Test_Pipeline_RELEASE is not triggered directly by release-openjdk*-pipeline;
 * there is at least one intermediate build-scripts job in between.  This function
 * therefore walks the cause chain upward (up to maxDepth levels) by fetching each
 * intermediate upstream build via the REST API until it either finds a match or
 * exhausts the chain.
 *
 * At each level two match forms are checked:
 *   • Structured: upstreamProject contains pipelineName AND upstreamBuild is in targetBuildNums.
 *   • Text fallback (shortDescription): "Started by upstream project … build number 130"
 *
 * @param useAuth  When true, fetches of parent builds use $JENKINS_AUTH (must be bound via withCredentials).
 */
def isBuildTriggeredBy(def buildInfo, String pipelineName, Set targetBuildNums, int maxDepth = 3, boolean useAuth = false) {
    def current = buildInfo
    for (int depth = 0; depth < maxDepth; depth++) {
        def actions = current?.actions ?: []
        def allCauses = []
        actions.each { action -> allCauses.addAll(action?.causes ?: []) }

        if (allCauses.isEmpty()) break

        for (cause in allCauses) {
            // Use jsonStr() to guard against net.sf.json.JSONNull on any field.
            def proj      = jsonStr(cause?.upstreamProject)
            def upNumStr  = jsonStr(cause?.upstreamBuild)
            def shortDesc = jsonStr(cause?.shortDescription)

            // Direct structured match
            if (proj && proj.contains(pipelineName) && targetBuildNums.contains(upNumStr)) {
                return true
            }
            // Text fallback in shortDescription
            if (shortDesc && shortDesc.contains(pipelineName)) {
                for (n in targetBuildNums) {
                    if (shortDesc.contains("build number ${n}") || shortDesc.contains("#${n}")) {
                        return true
                    }
                }
            }
        }

        // No match at this level — climb one level up using upstreamUrl (the correct
        // job path as Jenkins knows it) + upstreamBuild number.
        // Guard: upstreamUrl and upstreamBuild must be non-null, non-"null" strings.
        def parentCause = allCauses.find { jsonStr(it?.upstreamUrl) && jsonStr(it?.upstreamBuild) }
        if (!parentCause) break

        def parentProj     = jsonStr(parentCause.upstreamProject)
        def parentBuildNum = jsonStr(parentCause.upstreamBuild)
        def parentJobUrl   = jsonStr(parentCause.upstreamUrl).replaceAll('/$', '')
        if (!parentJobUrl || !parentBuildNum) break
        def parentApiUrl   = "${env.JENKINS_URL}${parentJobUrl}/${parentBuildNum}/api/json" +
            "?tree=actions%5Bcauses%5BupstreamProject,upstreamBuild,upstreamUrl,shortDescription%5D%5D"
        def parentInfo = fetchJson(parentApiUrl, "'${parentProj}' #${parentBuildNum}", useAuth)
        if (!parentInfo) break
        current = parentInfo
    }
    return false
}

/**
 * Extract the platform name from a Jenkins build's display name and/or description.
 *
 * Display name convention:
 *   "#346 - jdk8 : jdk8u504-b01_adopt_RELEASE : x86-64_windows"
 *   → last colon-separated token trimmed → "x86-64_windows"
 *
 * Description convention (HTML, last segment after final ' : ' or last colon):
 *   Same format, so same rule applies.
 *
 * Falls back to the last two underscore-delimited tokens from the description
 * if neither source has a colon (e.g. "Test_openjdk8_hs_special.functional_arm_linux"
 * → "arm_linux").
 *
 * @param displayName  value of buildInfo.displayName
 * @param description  value of buildInfo.description (may contain HTML)
 * @return platform string, or empty string if not determinable
 */
def extractPlatformFromDisplayName(String displayName, String description) {
    // Try display name first (most reliable — always set by Jenkins)
    if (displayName) {
        def parts = displayName.split(':')
        if (parts.size() >= 2) {
            return parts.last().trim()
        }
    }
    // Try description (strip HTML tags first)
    if (description) {
        def plain = description.replaceAll(/<[^>]+>/, '').trim()
        def parts = plain.split(':')
        if (parts.size() >= 2) {
            return parts.last().trim()
        }
        // Last two underscore tokens as final fallback
        def tokens = plain.tokenize('_')
        if (tokens.size() >= 2) {
            return "${tokens[-2]}_${tokens[-1]}"
        }
    }
    return ''
}

/**
 * Parse child issue numbers from a GitHub issue body.
 *
 * Recognises lines containing:
 *   - https://github.com/<owner>/<repo>/issues/<N>
 *   - - #<N>  (task list items)
 *
 * Returns a de-duplicated list of issue number strings.
 */
def extractChildIssueNumbers(String body, String repoSlug) {
    def nums = [] as LinkedHashSet
    if (!body) return nums as List

    body.split(/\r?\n/).each { line ->
        // Full URL form
        def urlM = (line =~ /github\.com\/${repoSlug}\/issues\/(\d+)/)
        urlM.each { nums << it[1] }
        // Short form: "- #123" or "closes #123" or "#123"
        def shortM = (line =~ /(?:^|\s|-)#(\d+)/)
        shortM.each { nums << it[1] }
    }
    return nums as List
}

/**
 * Extract platform from a child issue title.
 * Convention: "Triage Automated Tests for August 2026 JDK8 - x86-64_linux"
 * → last token after " - " → "x86-64_linux"
 */
def extractPlatformFromIssueTitle(String title) {
    if (!title) return ''
    def idx = title.lastIndexOf(' - ')
    if (idx < 0) return ''
    return title.substring(idx + 3).trim()
}

/**
 * Extract platform from a child issue body.
 * Looks for a line matching "Platform: <platform>"
 */
def extractPlatformFromIssueBody(String body) {
    if (!body) return ''
    for (line in body.split(/\r?\n/)) {
        def m = (line =~ /^Platform:\s*(\S+)/)
        if (m) return m[0][1].trim()
    }
    return ''
}
