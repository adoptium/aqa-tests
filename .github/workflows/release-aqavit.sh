#!/usr/bin/env bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Release engineering for the AQAvit repositories. Invoked by release-aqavit.yml,
# which supplies all configuration through environment variables:
#   GH_TOKEN, TAG, RELEASE_TYPE, BASE_BRANCH_INPUT, CENTRAL_INPUT, REPOSITORIES,
#   TAG_REPOS_INPUT, UPDATE_TESTENV_INPUT, DRY_RUN
# See the header of release-aqavit.yml for the behavior of each release type.

set -uo pipefail

DEFAULT_ORG="adoptium"
# Repositories of the AQAvit project, from
# https://projects.eclipse.org/projects/adoptium.aqavit/developer
# Only these (in the adoptium org) can be tagged or released by this workflow.
AQAVIT_REPOS=" TKG aqa-systemtest aqa-test-tools STF bumblebench run-aqa aqa-tests openj9-systemtest aqa-cbt aqa-triage-data "
TESTENV_PATH="testenv/testenv.properties"
RELEASE_NAME="${TAG}-release"
RELEASE_BRANCH="${TAG}-release"
TESTENV_BRANCH="${TAG}-testenv-update"

# full_name <repo|adoptium/repo> : prints adoptium/<repo>, or fails if it is not an AQAvit repository
full_name() {
  local r="${1#"${DEFAULT_ORG}"/}"
  if [[ "$AQAVIT_REPOS" == *" $r "* ]]; then
    echo "${DEFAULT_ORG}/${r}"
  else
    echo "::error::'$1' is not an AQAvit repository in the ${DEFAULT_ORG} org; allowed:${AQAVIT_REPOS}" >&2
    return 1
  fi
}
run() {
  if [[ "$DRY_RUN" == "true" ]]; then echo "[dry-run] $*" >&2; return 0; fi
  "$@"
}

CENTRAL=$(full_name "${CENTRAL_INPUT:-aqa-tests}") || exit 1

# ---- validate names and release type up front -------------------------
# Tags look like v1.0.16 or v1.0.14.1. The strict pattern also keeps characters that are
# special to sed (&, |, \) out of the testenv.properties edit, and guarantees that
# <tag> and <tag>-release are valid git ref names.
if [[ ! "$TAG" =~ ^v[0-9]+(\.[0-9]+)+$ ]]; then
  echo "::error::'${TAG}' is not a valid tag; expected a version such as v1.0.16 or v1.0.14.1"; exit 1
fi

case "$RELEASE_TYPE" in
  feature) label="Feature release"; expected="03 09";             defaults=true  ;;
  cpu)     label="CPU";             expected="01 04 07 10";       defaults=true  ;;
  cspu)    label="CSPU";            expected="02 05 06 08 11 12"; defaults=false ;;
  ifix)    label="iFix";            expected="";                  defaults=false ;;
  *) echo "::error::Unknown release type: $RELEASE_TYPE"; exit 1 ;;
esac

month=$(date -u +%m)
if [[ -n "$expected" && " $expected " != *" $month "* ]]; then
  echo "::warning::$label releases are expected in months [$expected]; the current UTC month is $month"
fi

resolve_flag() { # input default -> true|false
  case "$1" in yes) echo true ;; no) echo false ;; *) echo "$2" ;; esac
}
TAG_OTHERS=$(resolve_flag "${TAG_REPOS_INPUT:-auto}" "$defaults")
UPDATE_ENV=$(resolve_flag "${UPDATE_TESTENV_INPUT:-auto}" "$defaults")

# ---- resolve the base branch of the central repository ---------------
BASE_BRANCH="$BASE_BRANCH_INPUT"
if [[ -z "$BASE_BRANCH" ]]; then
  case "$RELEASE_TYPE" in
    feature|cpu) BASE_BRANCH="master" ;;
    ifix)
      BASE_BRANCH=$(gh api --paginate "repos/${CENTRAL}/branches" --jq '.[].name' \
        | grep -E '^v[0-9][0-9.]*-release$' | grep -vx "$RELEASE_BRANCH" | sort -V | tail -1)
      if [[ -z "$BASE_BRANCH" ]]; then
        echo "::error::No release branch found in ${CENTRAL}; provide baseBranch"; exit 1
      fi
      echo "iFix: using most recent release branch ${BASE_BRANCH}" ;;
    cspu)
      echo "::error::A CSPU is branched off the previous CPU release branch; provide baseBranch (e.g. v1.0.16-release)"
      exit 1 ;;
  esac
fi
if ! git check-ref-format "refs/heads/${BASE_BRANCH}"; then
  echo "::error::'${BASE_BRANCH}' is not a valid branch name"; exit 1
fi

# ---- repositories to tag (other than the central one) ----------------
others=()
for r in ${REPOSITORIES//,/ }; do
  r=$(full_name "$r") || exit 1
  [[ "$r" == "$CENTRAL" ]] && continue
  others+=("$r")
done

failed=()
declare -A TAGGED=()   # short repo name -> 1 when TAG exists (or would exist) in it
rows=""

ref_sha() { gh api "repos/$1/git/ref/$2" --jq '.object.sha' 2>/dev/null || true; }

# ensure_tag <repo> <sha> ; sets TAG_STATUS
ensure_tag() {
  local repo="$1" sha="$2" existing
  existing=$(ref_sha "$repo" "tags/${TAG}")
  if [[ -n "$existing" ]]; then
    TAG_STATUS="exists"
    [[ "$existing" == "$sha" ]] || echo "::warning::${repo}: tag ${TAG} already exists at ${existing}, not ${sha}; leaving it unchanged"
  elif run gh api -X POST "repos/${repo}/git/refs" -f "ref=refs/tags/${TAG}" -f "sha=${sha}" >/dev/null; then
    TAG_STATUS="created"
  else
    TAG_STATUS="failed"
  fi
}

# ---- 1. tag the other repositories ------------------------------------
if [[ "$TAG_OTHERS" == "true" ]]; then
  for repo in "${others[@]}"; do
    echo "::group::Tag ${repo}"
    branch=$(gh api "repos/${repo}" --jq '.default_branch' 2>/dev/null || true)
    sha=""
    [[ -n "$branch" ]] && sha=$(ref_sha "$repo" "heads/${branch}")
    if [[ -z "$sha" ]]; then
      echo "::error::${repo}: cannot resolve the head of its default branch"
      failed+=("$repo"); rows+="| ${repo} | - | failed |\n"
    else
      echo "Tagging ${branch} at ${sha}"
      ensure_tag "$repo" "$sha"
      [[ "$TAG_STATUS" == "failed" ]] && failed+=("$repo") || TAGGED["${repo##*/}"]=1
      rows+="| ${repo} | ${branch} | ${TAG_STATUS} |\n"
    fi
    echo "::endgroup::"
  done
else
  echo "Not tagging other repositories (tagRepositories resolved to false)"
fi

# ---- 2. central repository: tag, branch, release ---------------------
echo "::group::Central repository ${CENTRAL}"
base_sha=$(ref_sha "$CENTRAL" "heads/${BASE_BRANCH}")
if [[ -z "$base_sha" ]]; then
  echo "::error::${CENTRAL}: base branch '${BASE_BRANCH}' not found or not accessible"
  echo "::endgroup::"
  failed+=("$CENTRAL")
  central_rows="| tag | failed |\n| branch | failed |\n| release | failed |\n"
else
  echo "Base branch ${BASE_BRANCH} is at ${base_sha}"

  ensure_tag "$CENTRAL" "$base_sha"
  c_tag="$TAG_STATUS"
  [[ "$c_tag" == "failed" ]] || TAGGED["${CENTRAL##*/}"]=1

  if [[ -n "$(ref_sha "$CENTRAL" "heads/${RELEASE_BRANCH}")" ]]; then
    c_branch="exists"
  elif run gh api -X POST "repos/${CENTRAL}/git/refs" -f "ref=refs/heads/${RELEASE_BRANCH}" -f "sha=${base_sha}" >/dev/null; then
    c_branch="created"
  else
    c_branch="failed"
  fi

  if [[ "$c_tag" == "failed" ]]; then
    c_release="skipped"
  elif gh release view "$TAG" --repo "$CENTRAL" >/dev/null 2>&1; then
    c_release="exists"
  elif run gh release create "$TAG" --repo "$CENTRAL" --verify-tag \
        --title "$RELEASE_NAME" \
        --notes "${label} ${TAG}. Tagged from \`${BASE_BRANCH}\` at ${base_sha}. Release branch: \`${RELEASE_BRANCH}\`."; then
    c_release="created"
  else
    c_release="failed"
  fi
  echo "::endgroup::"
  for s in "$c_tag" "$c_branch" "$c_release"; do
    [[ "$s" == "failed" ]] && { failed+=("$CENTRAL"); break; }
  done
  central_rows="| tag | ${c_tag} |\n| branch ${RELEASE_BRANCH} | ${c_branch} |\n| GitHub release ${RELEASE_NAME} | ${c_release} |\n"
fi

# ---- 3. PR updating testenv.properties on the release branch ----------
pr_status="not requested"
if [[ "$UPDATE_ENV" == "true" ]]; then
  echo "::group::Update ${TESTENV_PATH} in ${CENTRAL}@${RELEASE_BRANCH}"
  pr_status="failed"
  # Only repositories that have a *_BRANCH entry in testenv.properties.
  declare -A KEY_FOR=(
    [TKG]=TKG_BRANCH
    [STF]=STF_BRANCH
    [openj9-systemtest]=OPENJ9_SYSTEMTEST_BRANCH
    [aqa-systemtest]=ADOPTOPENJDK_SYSTEMTEST_BRANCH
  )
  if [[ "${c_branch:-failed}" == "failed" || -z "${c_branch:-}" ]]; then
    echo "::error::Release branch is not available; cannot update ${TESTENV_PATH}"
  else
    read_ref="$RELEASE_BRANCH"
    [[ "$c_branch" == "created" && "$DRY_RUN" == "true" ]] && read_ref="$BASE_BRANCH"

    orig=$(mktemp); new=$(mktemp)
    resp=$(gh api "repos/${CENTRAL}/contents/${TESTENV_PATH}?ref=${read_ref}")
    file_sha=$(jq -r '.sha' <<<"$resp")
    jq -r '.content' <<<"$resp" | base64 -d > "$orig"
    cp "$orig" "$new"

    for name in "${!KEY_FOR[@]}"; do
      key="${KEY_FOR[$name]}"
      if [[ -z "${TAGGED[$name]:-}" ]]; then
        echo "Skipping ${key}: ${name} was not tagged with ${TAG} in this run"
      elif grep -q "^${key}=" "$new"; then
        sed -i "s|^${key}=.*|${key}=${TAG}|" "$new"
      else
        echo "::warning::${key} not found in ${TESTENV_PATH}"
      fi
    done

    if diff -u "$orig" "$new"; then
      echo "No changes needed in ${TESTENV_PATH}"
      pr_status="no changes needed"
    else
      existing_pr=$(gh pr list --repo "$CENTRAL" --head "$TESTENV_BRANCH" --base "$RELEASE_BRANCH" --state open --json url --jq '.[0].url' 2>/dev/null || true)
      if [[ -n "$existing_pr" ]]; then
        pr_status="exists: ${existing_pr}"
      else
        rel_sha=$(ref_sha "$CENTRAL" "heads/${RELEASE_BRANCH}")
        [[ -z "$rel_sha" ]] && rel_sha="$base_sha"
        b64=$(base64 < "$new" | tr -d '\n')
        pr_body=$(printf 'Updates `%s` on `%s` to reference the repositories tagged `%s` for the %s.\n\nCreated by the Release AQAvit workflow.' \
          "$TESTENV_PATH" "$RELEASE_BRANCH" "$TAG" "$label")
        if [[ -z "$(ref_sha "$CENTRAL" "heads/${TESTENV_BRANCH}")" ]]; then
          run gh api -X POST "repos/${CENTRAL}/git/refs" -f "ref=refs/heads/${TESTENV_BRANCH}" -f "sha=${rel_sha}" >/dev/null
        fi
        if run gh api -X PUT "repos/${CENTRAL}/contents/${TESTENV_PATH}" \
              -f "message=Update testenv.properties for ${RELEASE_NAME}" \
              -f "content=${b64}" -f "sha=${file_sha}" -f "branch=${TESTENV_BRANCH}" >/dev/null; then
          pr_url=$(run gh pr create --repo "$CENTRAL" --base "$RELEASE_BRANCH" --head "$TESTENV_BRANCH" \
            --title "Update testenv.properties for ${RELEASE_NAME}" --body "$pr_body") && pr_status="created: ${pr_url:-dry-run}"
        fi
      fi
    fi
  fi
  echo "::endgroup::"
  [[ "$pr_status" == "failed" ]] && failed+=("$CENTRAL (testenv PR)")
fi

# ---- summary ---------------------------------------------------------
{
  printf '### %s %s\n\n' "$label" "$TAG"
  printf 'Central repository: `%s`, base branch: `%s`, dry run: `%s`\n\n' "$CENTRAL" "$BASE_BRANCH" "$DRY_RUN"
  if [[ -n "$rows" ]]; then
    printf '| Repository | Tagged from | Tag |\n| --- | --- | --- |\n%b\n' "$rows"
  fi
  printf '| %s | Status |\n| --- | --- |\n%b\n' "$CENTRAL" "${central_rows:-}"
  printf 'testenv.properties PR: %s\n' "$pr_status"
} >> "$GITHUB_STEP_SUMMARY"

if [[ ${#failed[@]} -gt 0 ]]; then
  echo "::error::Failed for: ${failed[*]}"
  exit 1
fi
