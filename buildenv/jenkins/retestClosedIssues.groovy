#!groovy

// =============================================================================
// retestClosedIssues.groovy
//
// Re-runs excluded tests whose associated issue tracker bug is now closed.
//
// Stage 1: Parse exclude files inline (inline equivalent of parse-issues.yml).
// Stage 2: Fan out to Grinder (max 100 jobs, deduplicated by test+platform+JDK).
// Stage 3: Collect results and produce a human-readable summary.
// Stage 4: Open a GitHub PR removing passing tests; assign reviewer adamfarley.
// =============================================================================

// ---------------------------------------------------------------------------
// Pipeline-level parameters (configure these in the Jenkins job definition)
// ---------------------------------------------------------------------------
// JDK_VERSIONS  - comma-separated, e.g. "11,17,21,25"  (default: all found)
// JDK_IMPL      - controls which vendor exclude files are scanned in Stage 1
//                 (e.g. "openj9" adds vendor-specific ProblemList files).
//                 Only hotspot entries are ever processed regardless of this value.
// MAX_FIXED     - passed to issue_status.py --max-fixed; 0 = no limit (default).
//                 Set to a small number (e.g. 3) for quick smoke-runs.
// AQA_GITHUB_USER  - GitHub username for issue_status.py API auth
// AQA_GITHUB_TOKEN - Credential ID for the GitHub token used by issue_status.py
// GITHUB_TOKEN  - Credential ID for the GitHub API token (PR creation / HEAD checks)
// GITHUB_USER   - git author name for the automated commit (default: "jenkins-bot")
// GRINDER_JOB   - name of the upstream Grinder job (default: "Grinder")
// MAX_GRINDERS  - hard cap on parallel Grinder jobs (default: 100)
// ---------------------------------------------------------------------------

def GRINDER_JOB  = params.GRINDER_JOB  ?: 'Grinder'
def MAX_GRINDERS = (params.MAX_GRINDERS ?: '100').toInteger()
def GITHUB_USER  = params.GITHUB_USER  ?: 'jenkins-bot'
def JDK_IMPL_ARG = params.JDK_IMPL     ?: ''
def MAX_FIXED    = (params.MAX_FIXED   ?: '0').toInteger()

// Results accumulated across Stage 2, consumed by Stages 3 and 4.
// Key: "jdk{VER}|{PLATFORM}|{FULL_TEST_PATH}|{EXCLUDE_FILE}"  Value: 'PASS' | 'FAIL' | 'ERROR'
Map<String, String> jobResults = [:]
// Parallel closures map for Stage 2
Map<String, Closure> parallelJobs = [:]
// Errors from TARGET resolution (Stage 2); appended to the Stage 4 PR body.
List<String> targetErrors = []

// ---------------------------------------------------------------------------
// Build the Stage 1 shell script at script level (outside any node/stage/
// withCredentials block) so the CPS parser never sees shell glob patterns,
// bracket characters, or control-flow constructs inside a string literal.
//
// Rules learned from the working pipelines in this repo:
//   - Never put shell if/fi or glob patterns inside sh"" or sh'' blocks.
//   - Build script content as a Groovy string variable; pass as sh(script: var).
//   - Ternaries with GStrings are safe at script level only.
//   - '[' in any string context inside a node block confuses the CPS parser.
// ---------------------------------------------------------------------------
def globPat   = '[0-9]'   // stored as a variable so '[' never appears inline below
def flattenFn = 'flatten(1)'
def maxFixedFlag = MAX_FIXED > 0 ? "--max-fixed ${MAX_FIXED}" : ''

// issue_status.py is our stdlib rewrite (no pip required).  It lives alongside
// this Groovy file in buildenv/jenkins/ and is copied into the aqa-tests working
// directory at runtime so it sits next to exclude_parser.py and common/.
def s1Script = "set -e\n" +
    "cd \"\$WORKSPACE/aqa-tests\"\n" +
    "cp \"\$S1_ISSUE_STATUS_SRC\" scripts/disabled_tests/issue_status.py\n" +
    "find openjdk/excludes -maxdepth 1 -name 'ProblemList_openjdk${globPat}*.txt' > exclude_files.txt\n" +
    "find openjdk/excludes/alpine -name 'ProblemList_openjdk${globPat}*.txt' >> exclude_files.txt\n" +
    "if [ -n \"\$S1_JDK_IMPL\" ] && [ -d \"openjdk/excludes/vendors/\$S1_JDK_IMPL\" ]; then\n" +
    "    find \"openjdk/excludes/vendors/\$S1_JDK_IMPL\" -name 'ProblemList_openjdk${globPat}*.txt' >> exclude_files.txt\n" +
    "fi\n" +
    "if [ \"\$S1_JDK_IMPL\" = \"openj9\" ]; then\n" +
    "    find openjdk/excludes -maxdepth 1 -name 'ProblemList_openjdk${globPat}*-openj9.txt' >> exclude_files.txt\n" +
    "    find openjdk/excludes -maxdepth 1 -name 'ProblemList_openjdkvalhalla-openj9.txt' >> exclude_files.txt\n" +
    "fi\n" +
    "cat exclude_files.txt | python scripts/disabled_tests/exclude_parser.py > exclude.json\n" +
    "jq -s '${flattenFn}' exclude.json > all.json\n" +
    "cat all.json | python scripts/disabled_tests/issue_status.py \\\n" +
    "    --github-user \"\$S1_AQA_USER\" \\\n" +
    "    --github-token \"\$AQA_GH_TOKEN\" \\\n" +
    "    \$S1_MAX_FIXED_ARG > output.json\n"

// ============================================================================
// STAGE 1 — Parse exclude files (inline, aqa-tests source only)
// ============================================================================
List<Map<String, Object>> candidates = []

node('worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)') {
    stage('Parse disabled tests') {
        checkout scm

        sh "if [ ! -d \"\$WORKSPACE/aqa-tests/.git\" ]; then git clone --depth 1 https://github.com/adoptium/aqa-tests.git \"\$WORKSPACE/aqa-tests\"; else git -C \"\$WORKSPACE/aqa-tests\" fetch --depth 1 origin && git -C \"\$WORKSPACE/aqa-tests\" reset --hard origin/HEAD; fi"

        env.S1_MAX_FIXED_ARG     = maxFixedFlag
        env.S1_AQA_USER          = params.AQA_GITHUB_USER ?: env.AQA_GITHUB_USER ?: ''
        env.S1_JDK_IMPL          = JDK_IMPL_ARG
        // Resolve the path to our stdlib issue_status.py relative to this Groovy file.
        // scm checkout places the repo at WORKSPACE; the file lives in buildenv/jenkins/.
        env.S1_ISSUE_STATUS_SRC  = "${env.WORKSPACE}/buildenv/jenkins/issue_status.py"

        def aqaCredId = params.AQA_GITHUB_TOKEN ?: 'aqa-github-token'
        withCredentials([string(credentialsId: aqaCredId, variable: 'AQA_GH_TOKEN')]) {
            sh(script: s1Script)
        }

        def all = readJSON(file: "${env.WORKSPACE}/aqa-tests/output.json") as List

        def jdkFilter = params.JDK_VERSIONS ? params.JDK_VERSIONS.split(',').collect { it.trim() } : []

        all.each { entry ->
            if (entry['ISSUE_TRACKER_STATUS'] != 'closed') return
            if (entry['JDK_IMPL'].toString() != 'hotspot') return
            if (jdkFilter && !jdkFilter.contains(entry['JDK_VERSION'].toString())) return

            def fixedJdks = entry['ISSUE_TRACKED_FIXED_JDKS'] ? entry['ISSUE_TRACKED_FIXED_JDKS'].toString().trim() : ''
            if (fixedJdks) {
                def jdkVer = entry['JDK_VERSION'].toString()
                if (jdkVer.isInteger()) {
                    def ver = jdkVer.toInteger()
                    def covered = fixedJdks.split(',').any { token ->
                        token = token.trim()
                        token.endsWith('+') ? ver >= token[0..-2].toInteger() : (token.isInteger() ? ver == token.toInteger() : false)
                    }
                    if (!covered) return
                }
            }
            candidates << entry
        }

        echo "Found ${candidates.size()} closed-issue candidates after filtering."
    }
}

// ============================================================================
// TARGET resolution helpers
// ============================================================================

def stripTestSuffix(String path) {
    def m = path =~ /^(.*\.[a-z]+)/
    return m ? m[0][1] : path
}

def githubFileExists(String url, String ghToken) {
    def rawUrl = url
        .replace('https://github.com/', 'https://raw.githubusercontent.com/')
        .replace('/blob/', '/')
    def authFlag = ghToken ? "-H \"Authorization: token ${ghToken}\"" : ''
    def result = sh(script: "curl -sf --head --max-time 10 ${authFlag} '${rawUrl}' -o /dev/null && echo YES || echo NO", returnStdout: true).trim()
    return result == 'YES'
}

def resolveTarget(String testPath, String jdkVersion, String ghToken) {
    def ver = jdkVersion.isInteger() ? jdkVersion.toInteger() : -1
    if (ver == -1) return [target: null, url: null]

    def candidateList = []
    if (ver == 8) {
        def base = 'https://github.com/openjdk/jdk8u/blob/master'
        candidateList = [
            [url: "${base}/jdk/test/${testPath}",       target: 'jdk_custom'],
            [url: "${base}/hotspot/test/${testPath}",   target: 'hotspot_custom'],
            [url: "${base}/langtools/test/${testPath}", target: 'langtools_custom'],
        ]
    } else {
        ["jdk${ver}u", "jdk${ver}"].each { repo ->
            def base = "https://github.com/openjdk/${repo}/blob/master"
            candidateList << [url: "${base}/test/jdk/${testPath}",       target: 'jdk_custom']
            candidateList << [url: "${base}/test/hotspot/${testPath}",   target: 'hotspot_custom']
            candidateList << [url: "${base}/test/langtools/${testPath}", target: 'langtools_custom']
        }
    }

    def hit = candidateList.find { githubFileExists(it.url, ghToken) }
    return hit ? [target: hit.target, url: hit.url] : [target: null, url: null]
}

// ============================================================================
// STAGE 2 — Deduplicate and fan out to Grinder (cap at MAX_GRINDERS)
// ============================================================================
stage('Launch Grinder jobs') {
    node('worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)') {

        def PLATFORM_MAP = [
            's390x_linux'    : ['s390x_linux'],
            'ppc64le_linux'  : ['ppc64le_linux'],
            'x86-64_linux'   : ['x86-64_linux'],
            'aarch64_linux'  : ['aarch64_linux'],
            'arm_linux'      : ['arm_linux'],
            'riscv64_linux'  : ['riscv64_linux'],
            'x86-64_windows' : ['x86-64_windows'],
            'x86-32_windows' : ['x86-32_windows'],
            'aarch64_windows': ['aarch64_windows'],
            'x86-64_mac'     : ['x86-64_mac'],
            'aarch64_mac'    : ['aarch64_mac'],
            'ppc64_aix'      : ['ppc64_aix'],
            'sparcv9_solaris': ['sparcv9_solaris'],
            'x86-64_solaris' : ['x86-64_solaris'],
            's390x_zos'      : ['s390x_zos'],
            'all_linux'   : ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux'],
            'all_windows' : ['x86-64_windows', 'aarch64_windows'],
            'all_mac'     : ['x86-64_mac', 'aarch64_mac'],
            'all_aix'     : ['ppc64_aix'],
            'all'         : ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux',
                             'x86-64_windows', 'aarch64_windows',
                             'x86-64_mac', 'aarch64_mac',
                             'ppc64_aix'],
        ]

        List expanded = []
        candidates.each { entry ->
            List concretePlatforms = []
            entry['PLATFORM'].toString().split(',').each { rawToken ->
                def token = rawToken.trim().toLowerCase().replace(' ', '_')
                def resolved = PLATFORM_MAP[token]
                if (resolved) {
                    concretePlatforms.addAll(resolved)
                } else {
                    concretePlatforms << token
                }
            }
            concretePlatforms.each { concretePlatform ->
                def record = [:] + entry
                record['PLATFORM'] = concretePlatform
                expanded << record
            }
        }

        Map targetCache = [:]
        List resolved = []

        def ghCredId2 = params.GITHUB_TOKEN ?: 'github-token'
        withCredentials([string(credentialsId: ghCredId2, variable: 'GH_TOKEN_STAGE2')]) {
            expanded.each { entry ->
                def rawPath    = (entry['CUSTOM_TARGET'] ?: entry['TARGET']).toString()
                def cleanPath  = stripTestSuffix(rawPath)
                def jdkVersion = entry['JDK_VERSION'].toString()
                def cacheKey   = "${cleanPath}|${jdkVersion}"

                if (!targetCache.containsKey(cacheKey)) {
                    targetCache[cacheKey] = resolveTarget(cleanPath, jdkVersion, env.GH_TOKEN_STAGE2)
                }
                def result = targetCache[cacheKey]

                if (!result.target) {
                    if (result.target == null) {
                        targetCache[cacheKey] = [target: '', url: '']
                        targetErrors << "Could not locate test file for ${cleanPath} (JDK ${jdkVersion}) in any known OpenJDK repository location. This test was not retested."
                        echo "TARGET resolution failed: ${cleanPath} (JDK ${jdkVersion})"
                    }
                    return
                }

                def record = [:] + entry
                record['TARGET']        = result.target
                record['CUSTOM_TARGET'] = cleanPath
                resolved << record
            }
        }

        echo "TARGET resolution complete: ${resolved.size()} record(s) ready (${targetErrors.size()} test(s) skipped)."
        expanded = resolved

        Map dedupMap = [:]
        expanded.each { entry ->
            def fullTestPath = entry['CUSTOM_TARGET'] ?: entry['TARGET']
            def shortTest    = fullTestPath.toString().tokenize('/').last().replaceAll('\\.java.*$', '')
            def excludeFile  = entry['EXCLUDE_FILE'] ?: ''
            def jobKey       = "jdk${entry['JDK_VERSION']}-${entry['PLATFORM']}-${shortTest}"
            def recordKey    = "jdk${entry['JDK_VERSION']}|${entry['PLATFORM']}|${fullTestPath}|${excludeFile}"
            if (!dedupMap.containsKey(recordKey)) {
                dedupMap[recordKey] = entry + [_jobKey: jobKey, _recordKey: recordKey]
            }
        }

        def deduped = dedupMap.values().toList()
        echo "After deduplication: ${deduped.size()} unique jobs."

        if (deduped.size() > MAX_GRINDERS) {
            echo "WARNING: ${deduped.size()} jobs exceed MAX_GRINDERS (${MAX_GRINDERS}). Truncating to first ${MAX_GRINDERS}."
            deduped = deduped.take(MAX_GRINDERS)
        }

        Map jobKeyCount = [:]
        deduped.each { entry ->
            def jobKey = entry['_jobKey'].toString()
            jobKeyCount[jobKey] = (jobKeyCount[jobKey] ?: 0) + 1
            entry._parallelKey = jobKeyCount[jobKey] > 1 ? "${jobKey}-${jobKeyCount[jobKey]}" : jobKey
        }

        def SKIP_FIELDS = ['ISSUE_TRACKER_STATUS', 'ISSUE_TRACKER', 'ISSUE_TRACKED_FIXED_JDKS', 'EXCLUDE_FILE']

        deduped.each { entry ->
            def parallelKey = entry['_parallelKey'].toString()
            def recordKey   = entry['_recordKey'].toString()

            def childParams = []
            entry.each { k, v ->
                if (k.startsWith('_') || SKIP_FIELDS.contains(k)) return
                childParams << string(name: k.toString(), value: v.toString())
            }
            childParams += [
                string(name: 'SDK_RESOURCE',       value: 'nightly'),
                string(name: 'BUILD_LIST',          value: 'openjdk'),
                string(name: 'ADOPTOPENJDK_REPO',   value: 'https://github.com/adoptium/aqa-tests.git'),
                string(name: 'ADOPTOPENJDK_BRANCH', value: 'master'),
                string(name: 'TKG_OWNER_BRANCH',    value: 'adoptium:master'),
                string(name: 'ADOPTOPENJDK_SYSTEMTEST_OWNER_BRANCH', value: 'adoptium:master'),
                string(name: 'STF_OWNER_BRANCH',    value: 'adoptium:master'),
                string(name: 'OPENJ9_REPO',         value: 'https://github.com/eclipse-openj9/openj9.git'),
                string(name: 'OPENJ9_BRANCH',       value: 'master'),
                string(name: 'OPENJ9_SYSTEMTEST_OWNER_BRANCH', value: 'eclipse:master'),
                string(name: 'JCK_GIT_BRANCH',      value: 'master'),
                string(name: 'CUSTOMIZED_SDK_URL_CREDENTIAL_ID', value: 'eclipse_temurin_bot_email_and_token'),
                string(name: 'SLACK_CHANNEL',        value: 'aqavit-bot'),
                string(name: 'PARALLEL',             value: 'None'),
                string(name: 'ITERATIONS',           value: '1'),
                string(name: 'RERUN_ITERATIONS',     value: '0'),
                string(name: 'TIME_LIMIT',           value: '30'),
                string(name: 'ACTIVE_NODE_TIMEOUT',  value: '10'),
                string(name: 'AUTO_DETECT',          value: 'true'),
                string(name: 'KEEP_REPORTDIR',       value: 'true'),
                string(name: 'GENERATE_JOBS',        value: 'false'),
                string(name: 'PERSONAL_BUILD',       value: 'false'),
                string(name: 'DOCKER_REQUIRED',      value: 'false'),
                string(name: 'DYNAMIC_COMPILE',      value: 'false'),
                string(name: 'ARCHIVE_TEST_RESULTS', value: 'false'),
                string(name: 'RERUN_FAILURE',        value: 'false'),
                string(name: 'USE_JRE',              value: 'false'),
                string(name: 'LIGHT_WEIGHT_CHECKOUT',value: 'false'),
                string(name: 'USE_TESTENV_PROPERTIES', value: 'false'),
                string(name: 'SETUP_JCK_RUN',        value: 'false'),
                string(name: 'KEEP_WORKSPACE',       value: 'false'),
            ]

            parallelJobs[parallelKey] = {
                try {
                    def result = build(job: GRINDER_JOB, parameters: childParams, propagate: false, wait: true)
                    def status = result.getResult()
                    jobResults[recordKey] = (status == 'SUCCESS') ? 'PASS' : 'FAIL'
                    echo "[${parallelKey}] Grinder result: ${status}"
                } catch (Exception e) {
                    jobResults[recordKey] = 'ERROR'
                    echo "[${parallelKey}] ERROR launching Grinder: ${e.message}"
                }
            }
        }

        parallel parallelJobs
    }
}

// ============================================================================
// STAGE 3 — Collect results and produce human-readable summary
// ============================================================================
stage('Summarise results') {
    Map passByJdk  = [:]
    Map totalByJdk = [:]
    List passingKeys = []

    jobResults.each { key, result ->
        def jdkVer = key.split('\\|')[0]
        totalByJdk[jdkVer] = (totalByJdk[jdkVer] ?: 0) + 1
        if (result == 'PASS') {
            passByJdk[jdkVer] = (passByJdk[jdkVer] ?: 0) + 1
            passingKeys << key
        }
    }

    def summaryLines = totalByJdk.keySet().sort().collect { jdk ->
        "${jdk}: ${passByJdk[jdk] ?: 0} test(s) now passing (of ${totalByJdk[jdk]} retested)"
    }
    def summaryText = summaryLines ? summaryLines.join('\n') : 'No tests passed on any platform in this run.'

    echo "=== UNEXCLUSION CANDIDATE SUMMARY ===\n${summaryText}\n====================================="

    env.PASSING_KEYS = passingKeys.join('\n')
    env.SUMMARY_TEXT = summaryText
    currentBuild.description = summaryText.replaceAll('\n', '<br>')
}

// ============================================================================
// STAGE 4 — Open a GitHub PR removing passing tests; assign adamfarley
// ============================================================================
stage('Open unexclusion PR') {
    if (!env.PASSING_KEYS) {
        echo 'No passing tests — skipping PR creation.'
        return
    }

    node('worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)') {
        checkout scm

        def repo        = 'adoptium/aqa-tests'
        def branchName  = "auto-unexclude-${env.BUILD_NUMBER}"
        def summaryText = env.SUMMARY_TEXT
        def passingKeys = env.PASSING_KEYS.split('\n').toList()

        writeFile(file: "${env.WORKSPACE}/aqa-tests/passing_keys.txt", text: passingKeys.join('\n'))

        def commitMsg = "chore: remove exclusions for tests that now pass\n\nAuto-generated by retestClosedIssues (build #${env.BUILD_NUMBER}).\n${summaryText}"
        writeFile(file: "${env.WORKSPACE}/aqa-tests/commit_message.txt", text: commitMsg)

        def skipSection = ''
        if (targetErrors) {
            skipSection = '\n\n### Tests skipped - not found in upstream OpenJDK repos\n\n' +
                targetErrors.collect { '- ' + it }.join('\n') +
                '\n\nThese tests could not be confirmed in any known repository location. Manual follow-up is recommended.'
        }
        def prBody = "This PR was auto-generated by the retestClosedIssues Jenkins job (build #${env.BUILD_NUMBER}).\n\n${summaryText}${skipSection}\n\nPlease review the diff before merging."

        env.S4_BRANCH      = branchName
        env.S4_GITHUB_USER = GITHUB_USER
        env.S4_REPO        = repo

        def ghCredId4 = params.GITHUB_TOKEN ?: 'github-token'
        withCredentials([string(credentialsId: ghCredId4, variable: 'GH_TOKEN')]) {
            env.S4_GH_TOKEN = env.GH_TOKEN

            sh("cd \"\$WORKSPACE/aqa-tests\" && git config user.email 'jenkins@adoptium.net' && git config user.name \"\$S4_GITHUB_USER\" && git checkout -b \"\$S4_BRANCH\" 2>/dev/null || git checkout \"\$S4_BRANCH\"")
            sh("cd \"\$WORKSPACE/aqa-tests\" && python scripts/disabled_tests/remove_exclusions.py --keys-file passing_keys.txt --workspace .")
            sh("cd \"\$WORKSPACE/aqa-tests\" && git add openjdk/excludes && (git diff --cached --quiet || git commit -F commit_message.txt) && rm -f commit_message.txt")

            try {
                sh("git -C \"\$WORKSPACE/aqa-tests\" push \"https://\$S4_GH_TOKEN@github.com/\$S4_REPO.git\" HEAD:refs/heads/\"\$S4_BRANCH\"")

                def prTitle   = "chore: auto-unexclude tests with closed issues (build #${env.BUILD_NUMBER})"
                def prPayload = groovy.json.JsonOutput.toJson([title: prTitle, body: prBody, head: branchName, base: 'master', draft: false])
                writeFile(file: "${env.WORKSPACE}/aqa-tests/pr_payload.json", text: prPayload)

                def prResponse = sh(script: "curl -sf -X POST -H \"Authorization: token \$S4_GH_TOKEN\" -H \"Accept: application/vnd.github+json\" --data @\"\$WORKSPACE/aqa-tests/pr_payload.json\" \"https://api.github.com/repos/\$S4_REPO/pulls\" && rm -f \"\$WORKSPACE/aqa-tests/pr_payload.json\"", returnStdout: true).trim()

                def prJson   = readJSON(text: prResponse)
                def prNumber = prJson['number']
                def prUrl    = prJson['html_url']
                echo "PR created: ${prUrl}"

                env.S4_PR_NUMBER = prNumber.toString()
                sh("curl -sf -X POST -H \"Authorization: token \$S4_GH_TOKEN\" -H \"Accept: application/vnd.github+json\" -d '{\"reviewers\": [\"adamfarley\"]}' \"https://api.github.com/repos/\$S4_REPO/pulls/\$S4_PR_NUMBER/requested_reviewers\"")
                echo "Reviewer adamfarley assigned to PR #${prNumber}."
                currentBuild.description += "<br><a href='${prUrl}'>PR #${prNumber}</a>"

            } catch (Exception pushErr) {
                echo "WARNING: Push/PR creation failed: ${pushErr.message}"

                def patchFile = "unexclude-build-${env.BUILD_NUMBER}.patch"
                env.S4_PATCH_FILE = patchFile
                sh("cd \"\$WORKSPACE/aqa-tests\" && git format-patch HEAD~1 --stdout > \"\$WORKSPACE/\$S4_PATCH_FILE\"")
                archiveArtifacts artifacts: patchFile, fingerprint: true
                currentBuild.result = 'UNSTABLE'
                currentBuild.description += '<br>Push failed - patch artifact available'
                echo "Manual steps: download ${patchFile}, then: git clone https://github.com/${repo}.git && cd aqa-tests && git checkout -b ${branchName} && git am < ${patchFile} && git push origin ${branchName}"
            }
        }
    }
}
