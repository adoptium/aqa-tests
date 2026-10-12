# retestClosedIssues — Integration Guide

This document explains how to deploy the retestClosedIssues pipeline into a running
Jenkins+aqa-tests environment.  Follow every section in order.

---

## 1. File locations inside aqa-tests

The files are already at their correct locations within this repository:

| File | Path in aqa-tests |
|---|---|
| Jenkins pipeline | `buildenv/jenkins/retestClosedIssues.groovy` |
| Issue status script (stdlib rewrite, no pip) | `buildenv/jenkins/issue_status.py` |
| Exclusion removal script | `scripts/disabled_tests/remove_exclusions.py` |
| Unit tests | `scripts/disabled_tests/tests/test_remove_exclusions.py` |
| CI workflow | `.github/workflows/test-grinder-maker.yml` |

`retestClosedIssues.groovy` lives in `buildenv/jenkins/` alongside all other
aqa-tests pipeline files (`aqaTestPipeline.groovy`, `perfPipeline.groovy`, etc.).
`remove_exclusions.py` is called by Stage 4 of the pipeline at the hard-coded path:

```
python scripts/disabled_tests/remove_exclusions.py \
    --keys-file passing_keys.txt \
    --workspace .
```

If the script is ever moved, update that path in `retestClosedIssues.groovy`.

---

## 2. Verify the default branch

The PR created by Stage 4 targets `base: "master"`.  Confirm the default
branch of `adoptium/aqa-tests` before running:

```bash
gh repo view adoptium/aqa-tests --json defaultBranchRef -q .defaultBranchRef.name
```

If the result is `main` (or anything other than `master`), update the `base`
field in `retestClosedIssues.groovy`:

```groovy
// Change this:
base:  'master',
// To:
base:  'main',
```

---

## 3. Jenkins credentials

Create two Jenkins credentials before registering the job.

### 3a. GitHub token for issue_status.py (`aqa-github-token`)

This token is passed to `issue_status.py --github-token` so it can query the
GitHub API for issue statuses without hitting anonymous rate limits.

- **Kind:** Secret text
- **ID:** `aqa-github-token`  *(or override via the `AQA_GITHUB_TOKEN` parameter)*
- **Scope:** Minimum required: `public_repo` (read-only access to public repos)

### 3b. GitHub token for PR creation and TARGET resolution (`github-token`)

This token is used for:
- HEAD requests to `raw.githubusercontent.com` (TARGET resolution in Stage 2)
- `git push` to the fork in Stage 4
- Creating the GitHub PR via the REST API
- Assigning the reviewer

- **Kind:** Secret text
- **ID:** `github-token`  *(or override via the `GITHUB_TOKEN` parameter)*
- **Scope:** `repo` (read+write on the target repository)

---

## 4. Jenkins job registration

1. In Jenkins, create a new **Pipeline** job (not Multibranch).
2. Under **Pipeline → Definition**, choose **Pipeline script from SCM**.
3. Set **SCM** to Git, pointing at your aqa-tests fork.
4. Set **Script Path** to:
   ```
   buildenv/jenkins/retestClosedIssues.groovy
   ```
5. Save.  Do **not** run yet — complete section 5 first.

---

## 5. Pipeline parameters

Define the following parameters on the Jenkins job
(Manage Job → Pipeline Syntax → Parameters, or add them directly to
the job's `parameters {}` block if you prefer a parameterised pipeline):

| Name | Type | Default | Description |
|---|---|---|---|
| `JDK_VERSIONS` | String | *(empty)* | Comma-separated list of JDK major versions to process, e.g. `11,17,21,25`.  Leave empty to process all versions found. |
| `JDK_IMPL` | String | *(empty)* | Passed to `exclude_parser.py` to include vendor-specific exclude files (e.g. `openj9`).  Leave empty for hotspot-only (the default and only mode that modifies ProblemList files). |
| `MAX_FIXED` | String | `0` | Passed to `issue_status.py --max-fixed`.  Set to a small integer (e.g. `3`) for a quick smoke-run without processing all issues.  `0` means no limit. |
| `MAX_GRINDERS` | String | `100` | Hard cap on parallel Grinder jobs in Stage 2. |
| `GRINDER_JOB` | String | `Grinder` | Jenkins job name for the upstream Grinder job. |
| `GITHUB_USER` | String | `jenkins-bot` | Git author name used for the automated commit in Stage 4. |
| `GITHUB_TOKEN` | String | `github-token` | Jenkins credential ID for the `github-token` secret (PR creation, git push, TARGET resolution). |
| `AQA_GITHUB_TOKEN` | String | `aqa-github-token` | Jenkins credential ID for the token passed to `issue_status.py`. |
| `AQA_GITHUB_USER` | String | *(empty)* | GitHub username for `issue_status.py --github-user`.  Can also be set as a Jenkins environment variable. |

---

## 6. Agent label

The pipeline requests:

```groovy
node('worker || (ci.role.test&&hw.arch.x86&&sw.os.linux)')
```

The agent must have:
- **Python 3.9+** (3.11 recommended — matches the CI workflow)
- **git**
- **curl**
- **jq** (used in Stage 1 to flatten the JSON arrays from `exclude_parser.py`)

pip is **not** required. `issue_status.py` in `buildenv/jenkins/` is a stdlib
rewrite that Stage 1 copies into `scripts/disabled_tests/` at runtime,
replacing the upstream version which depends on `requests`.

If your agents use a different label expression, update both `node()` calls
(there is one at the top of Stage 1 and one inside Stage 4).

---

## 7. First run checklist

Before running with `MAX_FIXED=0` against the full issue list, do a quick
smoke-run:

1. Set `MAX_FIXED=3` and `JDK_VERSIONS=25` (or whichever version you want to test).
2. Trigger a manual build.
3. Verify Stage 1 logs show a small number of candidates (≤3).
4. Verify Stage 2 resolves TARGET values — check for `TARGET resolution complete` in the log.
5. Verify Stage 3 prints the summary block.
6. If any tests passed, verify Stage 4 creates a PR on the correct repository.
7. Close the test PR without merging before running for real.

---

## 8. What the pipeline does NOT do

- It does **not** modify the `main`/`master` branch directly — all changes go
  through a PR.
- It does **not** remove exclusions for non-hotspot JDK implementations
  (openj9, ibm, etc.), regardless of `JDK_IMPL`.
- It does **not** remove exclusions for tests whose issue is still open.
- It does **not** remove exclusions for solaris or z/OS platform tokens.
- It does **not** re-run any test more than once per build (deduplication by
  `_recordKey`).
- It does **not** automatically merge the PR — a human reviewer must approve
  and merge.
