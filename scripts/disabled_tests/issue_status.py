"""
issue_status.py  —  stdlib rewrite (no pip / no third-party deps)

Drop-in replacement for scripts/disabled_tests/issue_status.py from
adoptium/aqa-tests.  All third-party imports (requests, urllib3, lxml)
have been replaced with stdlib equivalents:

  requests.get / Session / HTTPAdapter(Retry)  →  urllib.request + manual retry
  requests.auth.HTTPBasicAuth                  →  base64-encoded Authorization header
  resp.json()                                  →  json.loads(resp.read().decode())
  resp.text                                    →  resp.read().decode()
  resp.raise_for_status()                      →  explicit HTTP-error check

The retry strategy exactly mirrors the original urllib3.Retry parameters:
  total=10, backoff_factor=7, status_forcelist=[403, 429, 500, 502, 503, 504]
  Backoff delay for attempt n (1-based): 7 * 2**(n-1) seconds.
"""

import abc
import argparse
import base64
import enum
import json
import logging
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import defaultdict
from concurrent import futures
from typing import Dict, List, Optional, Tuple

from common import models

logging.basicConfig(
    format="%(levelname)s - %(message)s",
)
LOG = logging.getLogger()

GITHUB_USER_ENV = "AQA_ISSUE_TRACKER_GITHUB_USER"
GITHUB_TOKEN_ENV = "AQA_ISSUE_TRACKER_GITHUB_TOKEN"

return_code = 0

# Partial URL segments used to filter exceptional issues
EXCEPTIONS = [
    '/aqa-tests/issues/1297',
    'https://github.ibm.com/',
]

# Mirrors urllib3.Retry(total=10, backoff_factor=7, status_forcelist=[403,429,500,502,503,504])
_RETRY_TOTAL        = 10
_RETRY_BACKOFF      = 7        # seconds; delay = backoff * 2**(attempt-1)
_RETRY_STATUS_LIST  = frozenset({403, 429, 500, 502, 503, 504})
_REQUEST_TIMEOUT    = 30       # seconds per attempt


class Status(enum.Enum):
    OPEN   = ('open',)
    CLOSED = ('closed',)

    def __init__(self, scheme_name):
        self.scheme_name = scheme_name


def _extract_path(url: str) -> str:
    """Return the path component of *url*, including the leading slash."""
    return urllib.parse.urlparse(url).path


def _basic_auth_header(user: str, token: str) -> str:
    """Return a Base64-encoded HTTP Basic Authorization header value."""
    credentials = f"{user}:{token}"
    encoded = base64.b64encode(credentials.encode()).decode()
    return f"Basic {encoded}"


def _http_get(url: str, headers: dict = None, timeout: int = _REQUEST_TIMEOUT) -> urllib.request.Request:
    """Build a GET Request with the given headers."""
    req = urllib.request.Request(url, headers=headers or {}, method='GET')
    return req


def _fetch(url: str,
           headers: dict = None,
           timeout: int = _REQUEST_TIMEOUT) -> Tuple[int, bytes]:
    """
    Perform a single GET and return (status_code, body_bytes).
    Raises urllib.error.HTTPError on non-2xx/3xx responses that urllib
    did not auto-redirect away from.
    """
    req = urllib.request.Request(url, headers=headers or {}, method='GET')
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.status, resp.read()


def _fetch_with_retry(url: str,
                      headers: dict = None,
                      timeout: int = _REQUEST_TIMEOUT) -> Tuple[int, bytes]:
    """
    GET *url* with automatic retries on transient failures.

    Retries up to _RETRY_TOTAL times when:
      - The response status is in _RETRY_STATUS_LIST, OR
      - An OSError / urllib.error.URLError is raised (network glitch).

    Backoff: _RETRY_BACKOFF * 2**(attempt-1) seconds between attempts.
    Raises the last exception / HTTPError on exhaustion.
    """
    last_exc: Exception = None
    for attempt in range(_RETRY_TOTAL + 1):
        if attempt:
            delay = _RETRY_BACKOFF * (2 ** (attempt - 1))
            LOG.debug(f"Retry {attempt}/{_RETRY_TOTAL} for {url!r} in {delay}s")
            time.sleep(delay)
        try:
            req = urllib.request.Request(url, headers=headers or {}, method='GET')
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                status = resp.status
                body   = resp.read()
            if status in _RETRY_STATUS_LIST:
                last_exc = urllib.error.HTTPError(url, status, f"HTTP {status}", {}, None)
                continue
            return status, body
        except urllib.error.HTTPError as e:
            last_exc = e
            if e.code in _RETRY_STATUS_LIST:
                continue
            raise
        except OSError as e:          # includes urllib.error.URLError
            last_exc = e
            continue
    raise last_exc


class HandlerException(Exception):
    pass


class NoHandlerFoundException(Exception):
    pass


class BaseHandler(abc.ABC):

    # GitHub API query params sent as URL query string
    GH_API_PARAMS = 'accept=application%2Fvnd.github.v3%2Bjson&state=all'

    STATUS_NAME_TO_ENUM = {
        'new':      Status.OPEN,
        'open':     Status.OPEN,
        'closed':   Status.CLOSED,
        'resolved': Status.CLOSED,
    }

    def __init__(self, user=None, token=None):
        self.user  = user
        self.token = token

    def _auth_headers(self) -> dict:
        """Return Authorization header dict if credentials are set, else empty."""
        if self.user and self.token:
            return {'Authorization': _basic_auth_header(str(self.user), str(self.token))}
        return {}

    def get_resp_body(self, url: str) -> bytes:
        """
        Fetch *url* and return the raw response body bytes.
        Applies authentication and retry logic appropriate to the URL.
        Raises HandlerException on HTTP error.
        """
        is_github_api = url.startswith("https://api.github.com/")
        is_github_web = url.startswith("https://github.com/")

        headers = {}
        if is_github_api or is_github_web:
            headers.update(self._auth_headers())
        if is_github_api:
            sep = '&' if '?' in url else '?'
            url = url + sep + self.GH_API_PARAMS

        try:
            _status, body = _fetch_with_retry(url, headers=headers)
        except urllib.error.HTTPError as e:
            if "Unauthorized" in str(e) or e.code == 401:
                raise HandlerException(f"Unauthorized for url: {url!r}") from e
            raise HandlerException(f"HTTP {e.code} fetching {url!r}") from e
        except OSError as e:
            raise HandlerException(f"Network error fetching {url!r}: {e}") from e
        return body

    def get_resp_json(self, url: str) -> dict:
        """Fetch *url* and return the decoded JSON body."""
        body = self.get_resp_body(url)
        return json.loads(body.decode('utf-8'))

    def get_resp_text(self, url: str) -> str:
        """Fetch *url* and return the decoded text body."""
        return self.get_resp_body(url).decode('utf-8', errors='replace')

    @classmethod
    def name_to_status(cls, name: str) -> Status:
        maybe_status = cls.STATUS_NAME_TO_ENUM.get(name)
        if maybe_status is None:
            raise HandlerException(f"Unrecognized issue status name: {name!r}")
        return maybe_status

    @abc.abstractmethod
    def can_handle(self, url: str) -> bool:
        pass

    @abc.abstractmethod
    def handle(self, url: str) -> tuple:
        pass


class GitHubHandler(BaseHandler):
    """URL handler for GitHub issues."""

    GITHUB_API_BASE_URL = 'https://api.github.com/repos'

    def can_handle(self, url: str) -> bool:
        return 'github.com' in url

    def handle(self, url: str) -> tuple:
        path = _extract_path(url)
        api_url = self.GITHUB_API_BASE_URL + path
        resp_json = self.get_resp_json(api_url)
        status_name = resp_json["state"]
        status_enum = self.name_to_status(status_name)
        labels_list = resp_json.get('labels', [])
        if status_enum == Status.OPEN:
            return (status_enum, "OPEN")
        else:
            return (status_enum, "CLOSED: " + self._resolution_parser(labels_list))

    def _resolution_parser(self, labels_list: list) -> str:
        for label in labels_list:
            name = label['name']
            if name in ('wontfix', 'exclusion:permanent'):
                return "Won't Fix"
            if name == 'fixed':
                return "Fixed. Action: Unexclude"
        return "Fixed. Action: Unexclude or add issue label: wontfix / exclusion:permanent"


class BugsOpenJdkHandler(BaseHandler):
    """URL handler for bugs.openjdk (Jira-based board)."""

    BUGS_OPENJDK_API_BASE_URL = 'https://bugs.openjdk.java.net/rest/api/latest/issue'

    def can_handle(self, url: str) -> bool:
        return 'bugs.openjdk' in url

    def _find_duplicate_target_key(self, resp_json: dict) -> Optional[str]:
        """
        In Jira issuelinks, an inwardIssue with type 'Duplicate' indicates that
        the current issue is a duplicate of that inward issue.
        """
        for link in (resp_json.get('fields', {}).get('issuelinks') or []):
            type_name = link.get('type', {}).get('name', '')
            if type_name.lower() == 'duplicate':
                inward = link.get('inwardIssue')
                if inward and inward.get('key'):
                    return inward.get('key')
        return None

    def _follow_duplicates(self, issue_key: str, resp_json: dict) -> Tuple[str, dict]:
        """
        Follows the chain of duplicate issues until finding the core issue
        that is not closed as a duplicate (or has no further duplicate links).
        """
        visited = {issue_key}
        current_key = issue_key
        current_json = resp_json

        while True:
            resolution = ((current_json.get('fields', {}).get('resolution') or {}).get('name') or '')
            if resolution.lower() != 'duplicate':
                break

            target_key = self._find_duplicate_target_key(current_json)
            if not target_key or target_key in visited:
                break

            visited.add(target_key)
            target_url = self.BUGS_OPENJDK_API_BASE_URL + '/' + target_key
            try:
                target_json = self.get_resp_json(target_url)
                current_key = target_key
                current_json = target_json
            except Exception as e:
                LOG.warning(f"Could not fetch duplicate target issue {target_key}: {e}")
                break

        return current_key, current_json

    def handle(self, url: str) -> tuple:
        path = _extract_path(url)
        *_, issue_key = path.split('/')
        api_url = self.BUGS_OPENJDK_API_BASE_URL + '/' + issue_key
        resp_json = self.get_resp_json(api_url)

        resolution = ((resp_json.get('fields', {}).get('resolution') or {}).get('name') or '')
        if resolution.lower() == 'duplicate':
            core_key, core_json = self._follow_duplicates(issue_key, resp_json)
            if core_key != issue_key:
                resp_json = core_json

        status_name = resp_json.get('fields', {}).get('status', {}).get('name', '').lower()
        status_enum = self.name_to_status(status_name)
        if status_enum == Status.OPEN:
            return (status_enum, "OPEN")
        resolution = ((resp_json.get('fields', {}).get('resolution') or {}).get('name') or '')
        return (status_enum, "CLOSED: " + self._resolution_parser(resolution, resp_json))

    def _resolution_parser(self, resolution: str, resp_json: dict) -> str:
        if resolution in ('null', ''):
            return "Unknown resolution. Action: Investigate"
        if resolution == "Won't Fix":
            return "Won't Fix"
        if resolution == "Fixed":
            fix_commits_list = []
            comments = resp_json.get('fields', {}).get('comment', {}).get('comments', [])
            fix_commits_list = self._comments_parser(comments, fix_commits_list)

            for issue_dict in (resp_json.get('fields', {}).get('issuelinks') or []):
                if issue_dict.get('type', {}).get('name', '') != "Backport":
                    continue
                backport_key = issue_dict.get('outwardIssue', {}).get('key', '')
                if not backport_key:
                    continue
                backport_url = self.BUGS_OPENJDK_API_BASE_URL + '/' + backport_key
                backport_json = self.get_resp_json(backport_url)
                backport_comments = backport_json.get('fields', {}).get('comment', {}).get('comments', [])
                if not backport_comments:
                    continue
                if "Fixed" not in ((resp_json.get('fields', {}).get('resolution') or {}).get('name', '')):
                    continue
                fix_commits_list = self._comments_parser(backport_comments, fix_commits_list)

            if not fix_commits_list:
                return "Fixed but unpropagated. No action."

            versions_list = []
            version_plus  = 0

            for commit_url in fix_commits_list:
                if "/jdk/commit" in commit_url:
                    *_, commit_key = commit_url.split('/')
                    text = self.get_resp_text(
                        "https://github.com/openjdk/jdk/branch_commits/" + commit_key
                    )
                    tag_list = re.findall(r">jdk-[0-9]+[^<]+<", text)
                    if not tag_list:
                        continue
                    tag_end = tag_list[-1][1:-1]
                    m = re.search(r"jdk-[0-9]+", tag_end)
                    if not m:
                        continue
                    m2 = re.search(r"[0-9]+", m.group())
                    if not m2:
                        continue
                    ver = int(m2.group())
                    if version_plus == 0 or version_plus > ver:
                        version_plus = ver
                    versions_list.append(version_plus)

                elif "hg.openjdk.java.net" in commit_url:
                    issue_int = resp_json.get('key', '')[4:]
                    for version_int in [8, 11, 17]:
                        search_url = (
                            "https://api.github.com/search/commits"
                            f"?q=repo%3Aopenjdk%2Fjdk{version_int}u+{issue_int}%3A"
                        )
                        search_json = self.get_resp_json(search_url)
                        if int(search_json.get('total_count', 0)) > 0:
                            versions_list.append(version_int)
                    if 17 in versions_list:
                        if version_plus == 0 or version_plus > 17:
                            version_plus = 17

                else:
                    m = re.search(r"jdk[0-9]+u?/commit", commit_url)
                    if not m:
                        continue
                    m2 = re.search(r"[0-9]+", m.group())
                    if not m2:
                        continue
                    versions_list.append(int(m2.group()))

            versions_list = list(set(versions_list))
            versions_string = ""
            for v in versions_list:
                if v < version_plus:
                    versions_string += str(v) + ","
            if version_plus:
                versions_string += str(version_plus) + "+,"
            if versions_string:
                return "Fixed. Action: Unexclude for JDK: " + versions_string[:-1]
            return "Fixed. No commits found."

        return f'Unknown resolution: "{resolution}". Action: investigate.'

    def _comments_parser(self, comments: list, urls_list: List[str]) -> List[str]:
        authors = {"dukebot", "roboduke", "hgupdate"}
        patterns = [
            re.compile(r"URL: +https://git\.openjdk\.org/jdk[0-9]*u?/commit/[0-9a-z]+"),
            re.compile(r"URL: +https://git\.openjdk\.java\.net/jdk[0-9]*u?/commit/[0-9a-z]+"),
            re.compile(r"URL: +https?://hg\.openjdk\.java\.net/.*"),
        ]
        for comment in comments:
            if comment.get('author', {}).get('name', '') not in authors:
                continue
            text = comment.get('body', '')
            for pat in patterns:
                m = pat.search(text)
                if m:
                    m2 = re.search(r"https?://.*", m.group())
                    if m2:
                        urls_list.append(m2.group())
                        break
        return urls_list


class Dispatcher:
    """Dispatches each issue tracker URL to its registered handler."""

    def __init__(self, handlers: List[BaseHandler]):
        self.handlers = handlers

    def dispatch(self, issue_url: str) -> tuple:
        for hdl in self.handlers:
            if hdl.can_handle(issue_url):
                return hdl.handle(issue_url)
        raise NoHandlerFoundException()


# ---------------------------------------------------------------------------
# Helpers shared with the original issue_status.py API surface
# ---------------------------------------------------------------------------

def _extract_fixed_jdks(complex_status: str) -> str:
    """
    Extract 'JDK: 11,17,21+' from a resolution string.
    Returns the version token (e.g. '11,17,21+'), or '' when not present.
    """
    m = re.search(r'Action: Unexclude for JDK:\s*([0-9+,]+)', complex_status)
    return m.group(1).rstrip(',') if m else ''


def augment_with_status(issues, issue_status: Status, complex_status: str = ''):
    fixed_jdks = _extract_fixed_jdks(complex_status)
    return [
        models.SchemeWithStatus(
            **issue,
            ISSUE_TRACKER_STATUS=issue_status.scheme_name,
            ISSUE_TRACKED_FIXED_JDKS=fixed_jdks,
            _complex_status=complex_status,
        )
        for issue in issues
    ]


def group_issues_by_url(issues: List[models.Scheme]) -> Dict[str, List[models.Scheme]]:
    url_to_issues: Dict[str, list] = defaultdict(list)
    for issue in issues:
        for possible_url in issue["ISSUE_TRACKER"].replace(",", " ").split():
            if possible_url.startswith(("http://", "https://")):
                url = possible_url.rstrip('.')
                url_to_issues[url.strip()].append(issue)
    return url_to_issues


def should_exclude(url: str) -> Tuple[bool, str]:
    if not url.startswith("http"):
        return True, "Ignoring non-url."
    for exc in EXCEPTIONS:
        if exc in url:
            return True, exc
    return False, ''


def _handle_completed_future(future, log_prefix, url, url_to_issues):
    global return_code
    try:
        result_tuple   = future.result()
        issue_status   = result_tuple[0]
        complex_status = result_tuple[1]
    except HandlerException as he:
        if "Unauthorized for url" in str(he):
            LOG.debug(f"{log_prefix} Ignoring access denial when handling {url!r}: {he}")
        else:
            LOG.error(f"{log_prefix} Error when handling {url!r}: {he}")
            return_code = 1
    except NoHandlerFoundException:
        LOG.error(f"{log_prefix} No handler found for {url!r}")
        return_code = 1
    except Exception as e:
        if "Unauthorized for url" in str(e):
            LOG.debug(f"{log_prefix} Ignoring access denial when handling {url!r}: {e}")
        else:
            LOG.error(f"{log_prefix} Uncaught exception for {url!r}: {e}")
            return_code = 1
    else:
        LOG.info(f"{log_prefix} Ended processing for {url!r}: {complex_status}")
        return augment_with_status(url_to_issues[url], issue_status, complex_status)
    return []


def fetch_all_statuses(issues: List[models.Scheme], dispatcher: Dispatcher,
                       max_workers, max_fixed: int = 0):
    raw_url_to_issues = group_issues_by_url(issues)

    url_to_issues = {}
    for url, url_issues in raw_url_to_issues.items():
        exclude, reason = should_exclude(url)
        if exclude:
            LOG.warning(f"Excluding {url!r} due to exception segment {reason!r}")
        else:
            url_to_issues[url] = url_issues

    len_trackers = len(url_to_issues)
    LOG.debug(f"Unique issue trackers found: {len_trackers}")

    fixed_count = 0
    all_issues  = []

    with futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        future_to_url = {executor.submit(dispatcher.dispatch, url): url
                         for url in url_to_issues}
        for i, future in enumerate(futures.as_completed(future_to_url)):
            url        = future_to_url[future]
            log_prefix = f"{i + 1}/{len_trackers} -"
            items = _handle_completed_future(future, log_prefix, url, url_to_issues)
            all_issues.extend(items)

            if any("Action: Unexclude" in item.get("_complex_status", "") for item in items):
                fixed_count += 1

            if max_fixed > 0 and fixed_count >= max_fixed:
                LOG.info(
                    f"Reached max_fixed limit ({max_fixed}). Stopping early after "
                    f"{i + 1}/{len_trackers} trackers."
                )
                for pending in future_to_url:
                    pending.cancel()
                break

    return all_issues


def is_known_url_format(url: str) -> bool:
    patterns = [
        re.compile(r"^https://github\.com/adoptium/[a-z0-9-]+/issues/[0-9]+(#issuecomment-[0-9]+)?$"),
        re.compile(r"^https://github\.com/eclipse-openj9/[a-z0-9-]+/issues/[0-9]+(#issuecomment-[0-9]+)?$"),
        re.compile(r"^https://github\.ibm\.com/runtimes/[a-z0-9-]+/issues/[0-9]+(#issuecomment-[0-9]+)?$"),
        re.compile(r"^https://bugs\.openjdk\.org/browse/JDK-[0-9]+$"),
        re.compile(r"^https://bugs\.openjdk\.java\.net/browse/JDK-[0-9]+$"),
    ]
    return any(p.match(url) for p in patterns)


def minimal_issues_check(issues: List[models.Scheme], auth: Tuple[str, str], output_json):
    """
    High-speed check of each URL's validity.
    *auth* is a (user, token) tuple or None.
    """
    global return_code
    output_json_contents = []
    raw_url_to_issues = group_issues_by_url(issues)

    for url in raw_url_to_issues:
        if not url.startswith("http"):
            continue
        if is_known_url_format(url):
            message = f"{url!r} uses a known url format."
            LOG.info(message)
            output_json_contents.append(message)
            continue

        try:
            headers = {}
            if auth and url.startswith("https://github.com/"):
                headers['Authorization'] = _basic_auth_header(auth[0], auth[1])
            req = urllib.request.Request(url, headers=headers, method='HEAD')
            with urllib.request.urlopen(req, timeout=_REQUEST_TIMEOUT) as resp:
                status = resp.status
            acceptable = {405, 429}
            if status < 404 or status in acceptable:
                message = f"{url!r} exists. Status code {status}"
                LOG.info(message)
                output_json_contents.append(message)
            else:
                message = f"{url!r} cannot be found. Status code {status}"
                LOG.error(message)
                output_json_contents.append("ERROR: " + message)
                return_code = 1
        except urllib.error.HTTPError as e:
            acceptable = {405, 429}
            if e.code < 404 or e.code in acceptable:
                message = f"{url!r} exists. Status code {e.code}"
                LOG.info(message)
                output_json_contents.append(message)
            else:
                message = f"{url!r} cannot be found. Status code {e.code}"
                LOG.error(message)
                output_json_contents.append("ERROR: " + message)
                return_code = 1
        except OSError as e:
            LOG.error(f"Uncaught exception while processing {url!r}: {e}")
            return_code = 1

    name = getattr(output_json, 'name', '<unknown>')
    if name not in ('<unknown>', '<stdout>'):
        LOG.info(f"Outputting JSON to {name}")
        json.dump(obj=output_json_contents, fp=output_json, indent=2)


def main():
    global return_code

    parser = argparse.ArgumentParser(
        description="Fetch issue status for each disabled test",
        allow_abbrev=False,
    )
    parser.add_argument('--github-user',  metavar='USER',
                        help=f"GitHub User used for API [env: {GITHUB_USER_ENV}]")
    parser.add_argument('--github-token', metavar='TOKEN',
                        help=f"GitHub Token used for API [env: {GITHUB_TOKEN_ENV}]")
    parser.add_argument('--max-workers', type=int, default=None,
                        help="Maximum number of threads (default: ThreadPoolExecutor default)")
    parser.add_argument('--max-fixed', type=int, default=0, metavar='N',
                        help="Stop after N fixed issues. 0 = no limit.")
    parser.add_argument('--infile',  '-i', type=argparse.FileType('r'), default=sys.stdin,
                        help='Input file (default: stdin)')
    parser.add_argument('--outfile', '-o', type=argparse.FileType('w'), default=sys.stdout,
                        help='Output file (default: stdout)')
    parser.add_argument('--verbose', '-v', action='count', default=0,
                        help="Increase verbosity (-v info, -vv debug)")
    parser.add_argument('--minimal', '-m', action='store_true',
                        help="Minimal high-speed URL validity check only.")
    args = parser.parse_args()

    if not args.github_user:
        args.github_user = os.getenv(GITHUB_USER_ENV)
        if not args.github_user:
            parser.print_usage()
            parser.exit(1, message="GitHub User must be provided as environment variable or "
                                    "command-line argument\n")

    if not args.github_token:
        args.github_token = os.getenv(GITHUB_TOKEN_ENV)
        if not args.github_token:
            parser.print_usage()
            parser.exit(1, message="GitHub Token must be provided as environment variable or "
                                    "command-line argument\n")

    if args.verbose == 1:
        LOG.setLevel(logging.INFO)
    elif args.verbose >= 2:
        LOG.setLevel(logging.DEBUG)

    LOG.debug(f"Loading JSON from {getattr(args.infile, 'name', '<unknown>')}")
    issues: List[models.Scheme] = json.load(args.infile)

    if args.minimal:
        auth = (args.github_user, args.github_token) if args.github_user and args.github_token else None
        minimal_issues_check(issues, auth, args.outfile)
        LOG.info("Script complete.")
        return return_code

    dispatcher = Dispatcher(handlers=[
        GitHubHandler(args.github_user, args.github_token),
        BugsOpenJdkHandler(args.github_user, args.github_token),
    ])

    issues_with_status = fetch_all_statuses(issues, dispatcher, args.max_workers, args.max_fixed)

    LOG.debug(f"Outputting JSON to {getattr(args.outfile, 'name', '<unknown>')}")
    json.dump(obj=issues_with_status, fp=args.outfile, indent=2)

    return return_code


if __name__ == '__main__':
    sys.exit(main())
