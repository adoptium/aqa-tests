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

// ============================================================================
// STAGE 1 — Parse exclude files (inline, aqa-tests source only)
// ============================================================================
List<Map<String, Object>> candidates = []

node('worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)') {
    stage('Parse disabled tests') {
        // Check out this pipeline's own repo so its scripts are available on the agent.
        // Note: remove_exclusions.py lives in scripts/disabled_tests/ inside this repo.
        checkout scm
        def ws = env.WORKSPACE

        // Clone aqa-tests into a predictable subdirectory.
        // We do a shallow clone to keep Stage 1 fast.
        sh """
            set -e
            if [ ! -d "${ws}/aqa-tests/.git" ]; then
                git clone --depth 1 https://github.com/adoptium/aqa-tests.git "${ws}/aqa-tests"
            else
                # On subsequent runs the workspace already exists; the shallow clone
                # may have local modifications from Stage 4's git-add/commit on the
                # *previous* build.  Reset hard to origin/HEAD so Stage 1 sees a
                # clean tree, then re-fetch to pick up any upstream changes.
                git -C "${ws}/aqa-tests" fetch --depth 1 origin
                git -C "${ws}/aqa-tests" reset --hard origin/HEAD
            fi
        """

        withCredentials([
            string(credentialsId: params.AQA_GITHUB_TOKEN ?: 'aqa-github-token',
                   variable: 'AQA_GH_TOKEN')
        ]) {
            def maxFixedArg = MAX_FIXED > 0 ? "--max-fixed ${MAX_FIXED}" : ''
            def aqaUser     = params.AQA_GITHUB_USER ?: env.AQA_GITHUB_USER ?: ''

            sh """
                set -e
                cd ${ws}/aqa-tests

                pip install --quiet -r scripts/disabled_tests/requirements.txt

                # Discover exclude files.
                find openjdk/excludes -maxdepth 1 -name 'ProblemList_openjdk[0-9]*.txt' > exclude_files.txt
                find openjdk/excludes/alpine -name 'ProblemList_openjdk[0-9]*.txt' >> exclude_files.txt

                if [ -n "${JDK_IMPL_ARG}" ] && [ -d "openjdk/excludes/vendors/${JDK_IMPL_ARG}" ]; then
                    find "openjdk/excludes/vendors/${JDK_IMPL_ARG}" -name 'ProblemList_openjdk[0-9]*.txt' >> exclude_files.txt
                fi

                if [ "${JDK_IMPL_ARG}" = "openj9" ]; then
                    find openjdk/excludes -maxdepth 1 \\( \\
                        -name 'ProblemList_openjdk[0-9]*-openj9.txt' \\
                        -o -name 'ProblemList_openjdkvalhalla-openj9.txt' \\
                    \\) >> exclude_files.txt
                fi

                cat exclude_files.txt | python scripts/disabled_tests/exclude_parser.py > exclude.json
                jq -s 'flatten(1)' exclude.json > all.json
                cat all.json | python scripts/disabled_tests/issue_status.py \\
                    --github-user "${aqaUser}" \\
                    --github-token "\${AQA_GH_TOKEN}" \\
                    ${maxFixedArg} > output.json
            """
        }

        def all = readJSON(file: "${ws}/aqa-tests/output.json") as List<Map<String, Object>>

        def jdkFilter = params.JDK_VERSIONS ? params.JDK_VERSIONS.split(',').collect { it.trim() } : null

        all.each { entry ->
            if (entry['ISSUE_TRACKER_STATUS'] != 'closed') return
            // Only hotspot — never modify exclusions for other JDK implementations.
            if (entry['JDK_IMPL'].toString() != 'hotspot') return
            if (jdkFilter && !jdkFilter.contains(entry['JDK_VERSION'].toString())) return

            // Only run a Grinder if this specific JDK version is listed in ISSUE_TRACKED_FIXED_JDKS.
            // ISSUE_TRACKED_FIXED_JDKS is populated by issue_status.py from the bug-tracker
            // resolution (e.g. "8,11,17+"). An empty string means fix applies to all versions.
            // A non-numeric JDK_VERSION (e.g. "all") passes through — it has no version to check.
            def fixedJdks = entry['ISSUE_TRACKED_FIXED_JDKS']?.toString()?.trim() ?: ''
            if (fixedJdks) {
                def jdkVer = entry['JDK_VERSION'].toString()
                if (jdkVer.isInteger()) {
                    def ver = jdkVer.toInteger()
                    // Parse tokens like "8", "11", "17+" where "17+" means 17 and above.
                    boolean covered = fixedJdks.split(',').any { token ->
                        token = token.trim()
                        if (token.endsWith('+')) {
                            ver >= token[0..-2].toInteger()
                        } else if (token.isInteger()) {
                            ver == token.toInteger()
                        } else {
                            false
                        }
                    }
                    if (!covered) return   // this JDK version not fixed — skip
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

/**
 * Strip everything after the last filename extension, so that
 * "path/to/Test.java#id0" becomes "path/to/Test.java".
 * The extension is the last occurrence of a dot followed by one or more
 * lower-case letters (e.g. .java, .sh, .py).
 * If the path contains no such extension the input is returned unchanged.
 */
def stripTestSuffix(String path) {
    // Match up to and including the last .<lower-case-letters> block
    def m = path =~ /^(.*\.[a-z]+)/
    return m ? m[0][1] : path
}

/**
 * Return true if the given GitHub raw URL exists (HTTP 200).
 * Uses curl --head so no body is downloaded; -f makes curl fail silently on 4xx/5xx.
 * Requires curl to be available on the agent — standard on all CI Linux agents.
 *
 * ghToken — GitHub personal access token string (not a credential ID).
 *           Passed explicitly so the function works outside a withCredentials block.
 *           Pass an empty string to make unauthenticated requests (rate-limited).
 */
def githubFileExists(String url, String ghToken = '') {
    // Convert blob URL to raw URL for a lightweight HEAD check
    def rawUrl = url
        .replace('https://github.com/', 'https://raw.githubusercontent.com/')
        .replace('/blob/', '/')
    def authHeader = ghToken ? "-H 'Authorization: token ${ghToken}'" : ''
    def status = sh(
        script: "curl -sf --head --max-time 10 ${authHeader} '${rawUrl}' -o /dev/null && echo YES || echo NO",
        returnStdout: true
    ).trim()
    return status == 'YES'
}

/**
 * Resolve the Grinder TARGET value for one (testPath, jdkVersion) combination
 * by checking candidate GitHub locations in order.
 *
 * Returns a Map with keys:
 *   target  — the Grinder TARGET string (e.g. "jdk_custom"), or null if not found
 *   url     — the matching GitHub blob URL, or null
 *
 * testPath    — cleaned path with no #suffix (e.g. "com/sun/jdi/JdwpAttachTest.java")
 * jdkVersion  — string JDK major version from output.json (e.g. "25", "8", "all")
 *
 * Non-numeric JDK versions (e.g. "all", "8+") cannot be looked up and always
 * return null so the caller can emit an appropriate error.
 */
def resolveTarget(String testPath, String jdkVersion, String ghToken = '') {
    def ver = jdkVersion.isInteger() ? jdkVersion.toInteger() : -1
    if (ver == -1) return [target: null, url: null]

    List<Map<String, String>> candidates = []

    if (ver == 8) {
        // JDK 8: fixed repo jdk8u, three sub-trees
        def base = "https://github.com/openjdk/jdk8u/blob/master"
        candidates = [
            [url: "${base}/jdk/test/${testPath}",      target: 'jdk_custom'],
            [url: "${base}/hotspot/test/${testPath}",  target: 'hotspot_custom'],
            [url: "${base}/langtools/test/${testPath}", target: 'langtools_custom'],
        ]
    } else {
        // JDK 11+: try jdk{N}u repo first, then jdk{N} repo; branch always master
        def repos = ["jdk${ver}u", "jdk${ver}"]
        repos.each { repo ->
            def base = "https://github.com/openjdk/${repo}/blob/master"
            candidates << [url: "${base}/test/jdk/${testPath}",      target: 'jdk_custom']
            candidates << [url: "${base}/test/hotspot/${testPath}",   target: 'hotspot_custom']
            candidates << [url: "${base}/test/langtools/${testPath}", target: 'langtools_custom']
        }
    }

    // .find() is CPS-safe in Jenkins pipelines; for..in is not.
    // ghToken is threaded through from the Stage 2 withCredentials block so that
    // existence checks are authenticated and not subject to GitHub's anonymous
    // rate limit (~60 requests/hour).
    def hit = candidates.find { githubFileExists(it.url, ghToken) }
    return hit ? [target: hit.target, url: hit.url] : [target: null, url: null]
}

// ============================================================================
// STAGE 2 — Deduplicate and fan out to Grinder (cap at MAX_GRINDERS)
// ============================================================================
stage('Launch Grinder jobs') {
  // githubFileExists() calls sh(), which requires a node context.
  node('worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)') {

    // --- Platform alias expansion -------------------------------------------
    // Maps the PLATFORM field from output.json (format 2) to the concrete
    // Grinder platform strings (format 3).  Formats 2 and 3 are identical for
    // single-platform tokens, so concrete entries are identity mappings.
    // Compound aliases are expanded to their constituent concrete platforms.
    // Solaris and z/OS platforms are passed through verbatim — Grinder will
    // reject them if unsupported, which is the desired behaviour.
    // These must stay in sync with GRINDER_TO_PROBLEMLIST in remove_exclusions.py.
    // All values are Lists so callers never need an instanceof check.
    def PLATFORM_MAP = [
        // Single-platform: output.json token → Grinder token (identity)
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
        // Compound aliases — expanded to multiple concrete Grinder platform strings.
        // These must stay in sync with the _coverage() map in remove_exclusions.py.
        'all_linux'   : ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux'],
        'all_windows' : ['x86-64_windows', 'aarch64_windows'],
        'all_mac'     : ['x86-64_mac', 'aarch64_mac'],
        'all_aix'     : ['ppc64_aix'],
        'all'         : ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux',
                         'x86-64_windows', 'aarch64_windows',
                         'x86-64_mac', 'aarch64_mac',
                         'ppc64_aix'],
    ]

    // Expand each candidate to one record per concrete platform.
    // The PLATFORM field from output.json may itself be a comma-separated list
    // of tokens (e.g. "x86-64_mac,all_linux"), so we split on commas first,
    // then look up each token individually in PLATFORM_MAP.
    List<Map<String, Object>> expanded = []
    candidates.each { entry ->
        List<String> concretePlatforms = []
        entry['PLATFORM'].toString().split(',').each { rawToken ->
            def token = rawToken.trim().toLowerCase().replace(' ', '_')
            def resolved = PLATFORM_MAP[token]
            if (resolved) {
                concretePlatforms.addAll(resolved)
            } else {
                // Unknown token — pass through verbatim so Grinder can reject it
                concretePlatforms << token
            }
        }
        concretePlatforms.each { concretePlatform ->
            def record = [:] + entry
            record['PLATFORM'] = concretePlatform
            expanded << record
        }
    }

    // --- TARGET resolution --------------------------------------------------
    // For each expanded record we determine the correct Grinder TARGET value
    // by probing the canonical OpenJDK GitHub repos for the test file.
    //
    // We deduplicate the probes: many expanded records share the same
    // (CUSTOM_TARGET, JDK_VERSION) pair (one per platform).  We resolve once
    // per unique (cleanPath, jdkVersion) pair and cache the result, then stamp
    // every matching record.  Records whose test cannot be located are dropped
    // and an error message is appended to targetErrors for the Stage 4 PR body.
    //
    // The resolution requires network access to raw.githubusercontent.com.
    // Each failing probe is a fast 404; a hit exits the loop immediately.
    //
    // GH_TOKEN is obtained here (authenticated HEAD checks) to prevent hitting
    // GitHub's anonymous rate limit (~60 requests/hour per IP).
    Map<String, Map<String, String>> targetCache = [:]   // key: "cleanPath|jdkVersion"
    List<Map<String, Object>> resolved = []

    withCredentials([string(credentialsId: params.GITHUB_TOKEN ?: 'github-token',
                            variable: 'GH_TOKEN_STAGE2')]) {
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
            // Emit one error per unique (test, jdk) pair — not one per platform.
            // Use a sentinel in the cache to suppress duplicate messages.
            if (result.target == null) {
                // null → not yet errored; set to '' to mark as reported
                targetCache[cacheKey] = [target: '', url: '']
                def msg = "⚠ Could not locate test file for **${cleanPath}** " +
                          "(JDK ${jdkVersion}) in any known OpenJDK repository location. " +
                          "This test was not retested."
                targetErrors << msg
                echo "TARGET resolution failed: ${cleanPath} (JDK ${jdkVersion})"
            }
            // Skip this record (and every other platform expansion of the same test)
            return
        }

        def record = [:] + entry
        record['TARGET']       = result.target
        record['CUSTOM_TARGET'] = cleanPath   // normalise to suffix-stripped path
        resolved << record
      } // end expanded.each
    } // end withCredentials

    echo "TARGET resolution complete: ${resolved.size()} record(s) ready " +
         "(${targetErrors.size()} test(s) skipped — not found in upstream repos)."

    // Replace expanded with the resolved list for the deduplication step below.
    expanded = resolved

    // --- Deduplication ------------------------------------------------------
    // Keyed by _recordKey (fully unique: JDK + platform + full test path +
    // exclude file) so that two different tests that share only a filename
    // (e.g. com/sun/jdi/Basic.java and java/lang/reflect/Basic.java) are
    // never collapsed into one Grinder job.
    //
    // Multiple rows sharing the same _recordKey differ only in ISSUE_TRACKER
    // and are intentionally collapsed — one Grinder run is sufficient.
    //
    // _jobKey     — human-readable display label: "jdk{VER}-{PLATFORM}-{SHORT_TEST}"
    //               Used in log output and (with a collision suffix) as the
    //               Jenkins parallel map key.
    // _recordKey  — pipe-delimited structured key written to passing_keys.txt
    //               and consumed by remove_exclusions.py:
    //               "jdk{VER}|{PLATFORM}|{FULL_TEST_PATH}|{EXCLUDE_FILE}"
    //               Pipes are safe: none of the field values contain them.
    Map<String, Map<String, Object>> dedupMap = [:]
    expanded.each { entry ->
        def fullTestPath = entry['CUSTOM_TARGET'] ?: entry['TARGET']
        def shortTest    = fullTestPath.toString().tokenize('/').last()
                                       .replaceAll('\\.java.*$', '')
        def excludeFile  = entry['EXCLUDE_FILE'] ?: ''
        def jobKey       = "jdk${entry['JDK_VERSION']}-${entry['PLATFORM']}-${shortTest}"
        def recordKey    = "jdk${entry['JDK_VERSION']}|${entry['PLATFORM']}|${fullTestPath}|${excludeFile}"
        // Deduplicate on recordKey — two identical full paths on the same
        // platform/JDK need only one Grinder run regardless of issue count.
        if (!dedupMap.containsKey(recordKey)) {
            dedupMap[recordKey] = entry + [_jobKey: jobKey, _recordKey: recordKey]
        }
    }

    def deduped = dedupMap.values().toList()
    echo "After deduplication: ${deduped.size()} unique jobs."

    // --- Hard cap -----------------------------------------------------------
    if (deduped.size() > MAX_GRINDERS) {
        echo "WARNING: ${deduped.size()} jobs exceed MAX_GRINDERS (${MAX_GRINDERS}). " +
             "Truncating to first ${MAX_GRINDERS} entries. Adjust JDK_VERSIONS/JDK_IMPL " +
             "filters or raise MAX_GRINDERS to process the remainder."
        deduped = deduped.take(MAX_GRINDERS)
    }

    // --- Build parallel closures --------------------------------------------
    // Jenkins requires every key in the parallel map to be unique. _jobKey is
    // human-readable but not guaranteed unique (two tests can share a filename).
    // We track seen display labels and append a numeric suffix on collision:
    // "jdk25-x86-64_linux-Basic" -> "jdk25-x86-64_linux-Basic-2" etc.
    Map<String, Integer> jobKeyCount = [:]
    deduped.each { entry ->
        def jobKey = entry['_jobKey'].toString()
        jobKeyCount[jobKey] = (jobKeyCount[jobKey] ?: 0) + 1
        entry._parallelKey = jobKeyCount[jobKey] > 1
            ? "${jobKey}-${jobKeyCount[jobKey]}"
            : jobKey
    }
    deduped.each { entry ->
        def parallelKey = entry['_parallelKey'].toString()
        def recordKey   = entry['_recordKey'].toString()

        def childParams = []
        // Fields that are internal to this pipeline or not meaningful to Grinder
        // are excluded from the child job parameters.
        // ISSUE_TRACKED_FIXED_JDKS is pipeline-internal metadata from issue_status.py;
        // Grinder does not declare it as a parameter and will reject the job if it is present.
        def SKIP_FIELDS = ['ISSUE_TRACKER_STATUS', 'ISSUE_TRACKER', 'ISSUE_TRACKED_FIXED_JDKS']
        entry.each { k, v ->
            if (k.startsWith('_') || SKIP_FIELDS.contains(k)) return
            childParams << string(name: k.toString(), value: v.toString())
        }

        parallelJobs[parallelKey] = {
            try {
                def result = build(
                    job: GRINDER_JOB,
                    parameters: childParams,
                    propagate: false,
                    wait: true
                )
                def status = result.getResult()
                // Store under recordKey so Stage 4 can write passing_keys.txt
                // with fully-structured entries; log under jobKey for readability.
                jobResults[recordKey] = (status == 'SUCCESS') ? 'PASS' : 'FAIL'
                echo "[${parallelKey}] Grinder result: ${status}"
            } catch (Exception e) {
                jobResults[recordKey] = 'ERROR'
                echo "[${parallelKey}] ERROR launching Grinder: ${e.message}"
            }
        }
    }

    parallel parallelJobs
  } // end node
}

// ============================================================================
// STAGE 3 — Collect results and produce human-readable summary
// ============================================================================
stage('Summarise results') {

    // Count passing tests per JDK version
    Map<String, Integer> passByJdk   = [:]
    Map<String, Integer> totalByJdk  = [:]
    List<String> passingKeys = []

    jobResults.each { key, result ->
        // key format: jdk{VER}|{PLATFORM}|{FULL_TEST_PATH}|{EXCLUDE_FILE}
        def jdkVer = key.split('\\|')[0]  // e.g. "jdk25"
        totalByJdk[jdkVer] = (totalByJdk[jdkVer] ?: 0) + 1
        if (result == 'PASS') {
            passByJdk[jdkVer] = (passByJdk[jdkVer] ?: 0) + 1
            passingKeys << key
        }
    }

    // Build the human-readable summary string.
    // Include every JDK version that was retested — even those with zero passes —
    // so the operator can see that tests ran but did not pass.
    def summaryLines = totalByJdk
        .keySet()
        .sort()
        .collect { jdk ->
            def total  = totalByJdk[jdk]
            def passes = passByJdk[jdk] ?: 0
            "${jdk}: ${passes} test(s) now passing (of ${total} retested)"
        }

    def summaryText = summaryLines
        ? summaryLines.join('\n')
        : 'No tests passed on any platform in this run.'

    echo """
=== UNEXCLUSION CANDIDATE SUMMARY ===
${summaryText}
====================================="""

    // Stash for Stage 4.
    // Newline delimiter avoids any ambiguity with commas in future path formats.
    env.PASSING_KEYS  = passingKeys.join('\n')
    env.SUMMARY_TEXT  = summaryText

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

        withCredentials([string(credentialsId: params.GITHUB_TOKEN ?: 'github-token',
                                variable: 'GH_TOKEN')]) {

            def ws          = env.WORKSPACE
            def repo        = 'adoptium/aqa-tests'
            def branchName  = "auto-unexclude-${env.BUILD_NUMBER}"
            def passingKeys = env.PASSING_KEYS.split('\n').toList()
            def summaryText = env.SUMMARY_TEXT

            // --- Perform removals in Python (reuses existing repo tooling) ----
            // Write the list of job keys to a file so the helper script can
            // look up the matching lines in each ProblemList and remove them.
            writeFile(
                file: "${ws}/aqa-tests/passing_keys.txt",
                text: passingKeys.join('\n')
            )

            // Write the commit message to a file to avoid shell injection from
            // summaryText (which contains arbitrary test names and JDK versions).
            def commitMsg = "chore: remove exclusions for tests that now pass\n\n" +
                            "Auto-generated by retestClosedIssues (build #${env.BUILD_NUMBER}).\n" +
                            summaryText
            writeFile(file: "${ws}/aqa-tests/commit_message.txt", text: commitMsg)

            // --- Commit the removals locally (always) -------------------------
            // The commit is always made locally so that git format-patch can
            // produce an artifact even if the push or PR creation fails later.
            sh """
                set -e
                cd ${ws}/aqa-tests

                git config user.email "jenkins@adoptium.net"
                git config user.name  "${GITHUB_USER}"
                # Create the branch; if a previous run already created it (same
                # BUILD_NUMBER, e.g. on a manual re-run), just switch to it.
                git checkout -b ${branchName} 2>/dev/null || git checkout ${branchName}

                python scripts/disabled_tests/remove_exclusions.py \
                    --keys-file passing_keys.txt \
                    --workspace .

                git add openjdk/excludes
                # Only commit if there is something to commit
                git diff --cached --quiet || git commit -F commit_message.txt
                rm -f commit_message.txt
            """

            // --- Build skip-notice section (used in both PR body and patch) ---
            def skipSection = ''
            if (targetErrors) {
                skipSection = '\n\n### Tests skipped — not found in upstream OpenJDK repos\n\n' +
                    targetErrors.collect { "- ${it}" }.join('\n') +
                    '\n\nThese tests were excluded from this run because their source file ' +
                    'could not be confirmed in any known repository location. Manual follow-up is recommended.'
            }

            def prBody = """This PR was auto-generated by the retestClosedIssues Jenkins job (build #${env.BUILD_NUMBER}).
The tests listed below passed on their target platforms when retested against a nightly JDK build, and have been removed from the relevant ProblemList exclude files.

${summaryText}${skipSection}

Please review the diff to confirm the removals are appropriate before merging."""

            // --- Attempt push + PR; fall back to patch artifact on any failure -
            try {
                sh """
                    set -e
                    git -C ${ws}/aqa-tests push \
                        "https://${GH_TOKEN}@github.com/${repo}.git" \
                        HEAD:refs/heads/${branchName}
                """

                // Build the PR payload as a JSON string and write it to a file
                // so that curl reads it directly — avoids quoting/injection risk.
                def prTitle   = "chore: auto-unexclude tests with closed issues (build #${env.BUILD_NUMBER})"
                def prPayload = groovy.json.JsonOutput.toJson([
                    title: prTitle,
                    body:  prBody,
                    head:  branchName,
                    base:  'master',
                    draft: false,
                ])
                writeFile(file: "${ws}/aqa-tests/pr_payload.json", text: prPayload)

                def prResponse = sh(
                    script: """
                        curl -sf -X POST \
                            -H "Authorization: token ${GH_TOKEN}" \
                            -H "Accept: application/vnd.github+json" \
                            --data @${ws}/aqa-tests/pr_payload.json \
                            https://api.github.com/repos/${repo}/pulls
                        rm -f ${ws}/aqa-tests/pr_payload.json
                    """,
                    returnStdout: true
                ).trim()

                def prJson   = readJSON(text: prResponse)
                def prNumber = prJson['number']
                def prUrl    = prJson['html_url']

                echo "PR created: ${prUrl}"

                // Assign reviewer
                sh """
                    curl -sf -X POST \
                        -H "Authorization: token ${GH_TOKEN}" \
                        -H "Accept: application/vnd.github+json" \
                        -d '{"reviewers": ["adamfarley"]}' \
                        https://api.github.com/repos/${repo}/pulls/${prNumber}/requested_reviewers
                """

                echo "Reviewer adamfarley assigned to PR #${prNumber}."
                currentBuild.description += "<br><a href='${prUrl}'>PR #${prNumber}</a>"

            } catch (Exception pushErr) {
                // Push or PR creation failed.  Generate a git patch file so
                // the operator can apply the changes and open a PR manually.
                echo "WARNING: Push/PR creation failed: ${pushErr.message}"
                echo "Generating patch artifact for manual application..."

                def patchFile = "unexclude-build-${env.BUILD_NUMBER}.patch"
                sh """
                    set -e
                    cd ${ws}/aqa-tests
                    # format-patch produces one .patch file per commit; stdout
                    # mode (--stdout) gives a single combined mbox-format file.
                    git format-patch HEAD~1 --stdout > "${ws}/${patchFile}"
                """

                // Archive in the Jenkins build workspace so it appears on the
                // build's Artifacts page and can be downloaded via the UI.
                archiveArtifacts artifacts: patchFile, fingerprint: true

                currentBuild.result = 'UNSTABLE'
                currentBuild.description += '<br>⚠ Push failed — patch artifact available'

                echo """
╔══════════════════════════════════════════════════════════════════╗
║  MANUAL PR INSTRUCTIONS                                          ║
╠══════════════════════════════════════════════════════════════════╣
║  A patch file has been saved as a build artifact:               ║
║    ${patchFile}                                                  ║
║                                                                  ║
║  To apply it and open the PR yourself:                          ║
║                                                                  ║
║  1. Clone (or update) your aqa-tests fork:                      ║
║       git clone https://github.com/${repo}.git                  ║
║       cd aqa-tests                                               ║
║                                                                  ║
║  2. Create a new branch:                                        ║
║       git checkout -b ${branchName}                             ║
║                                                                  ║
║  3. Download the patch from this build's Artifacts page, then:  ║
║       git am < ${patchFile}                                      ║
║                                                                  ║
║  4. Push and open a PR:                                         ║
║       git push origin ${branchName}                             ║
║     Then open a PR on GitHub targeting master/main.             ║
╚══════════════════════════════════════════════════════════════════╝
"""
            }
        }
    }
}
