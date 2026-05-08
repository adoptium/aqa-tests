import abc
import argparse
import enum
import json
import logging
import os
import re
import sys
import urllib.parse
from collections import defaultdict
from concurrent import futures
from typing import List, Dict, Tuple

import requests
import requests.auth

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


class Status(enum.Enum):
    OPEN = ('open',)
    CLOSED = ('closed',)

    def __init__(self, scheme_name):
        self.scheme_name = scheme_name


def _extract_path(url):
    """
    Extracts the URL path, including the root slash

    e.g. extracts '/3/library/urllib.parse.html'
    from 'https://docs.python.org:80/3/library/urllib.parse.html'
    """
    path = urllib.parse.urlparse(url).path
    return path


class HandlerException(Exception):
    pass


class NoHandlerFoundException(Exception):
    pass


class BaseHandler(abc.ABC):
    """
    Fetches the status of an issue given its tracking URL
    """
    STATUS_NAME_TO_ENUM = {
        'new': Status.OPEN,
        'open': Status.OPEN,
        'closed': Status.CLOSED,
        'resolved': Status.CLOSED,
    }

    @classmethod
    def name_to_status(cls, name):
        maybe_status = cls.STATUS_NAME_TO_ENUM.get(name)
        if maybe_status is None:
            raise HandlerException(f"Unrecognized issue status name: {name!r}")
        return maybe_status

    @abc.abstractmethod
    def can_handle(self, url) -> bool:
        pass

    @abc.abstractmethod
    def handle(self, url) -> Status:
        pass


class GitHubHandler(BaseHandler):
    """
    URL handler for GitHub
    """
    GITHUB_API_BASE_URL = f'https://api.github.com/repos'
    PARAMS = {'accept': 'application/vnd.github.v3+json', 'state': 'all'}

    def __init__(self, user=None, token=None):
        self.user = user
        self.token = token

    def can_handle(self, url):
        return 'github.com' in url

    def handle(self, url):
        path = _extract_path(url)
        url = self.GITHUB_API_BASE_URL + path

        # Use anonymous auth if user/token not provided
        if all([self.user, self.token]):
            auth = requests.auth.HTTPBasicAuth(self.user, self.token)
        else:
            auth = None
        resp = requests.get(url, params=self.PARAMS, auth=auth)
        resp.raise_for_status()
        resp_json = resp.json()
        status_name = resp_json["state"]
        status_enum = self.name_to_status(status_name)
        labels_list = resp_json.get('labels', {})
        if status_enum == Status.OPEN:
            return (status_enum, "OPEN",)
        else:
            return (status_enum, self.resolution_parser(labels_list),)

    def resolution_parser(self, labels_list):
        for single_label in labels_list:
            if single_label['name'] == 'wontfix' or single_label['name'] == 'exclusion:permanent':
                return "Won't Fix"
            elif single_label['name'] == 'fixed':
                return "Fixed. Action: Unexclude"
        return "Unknown resolution. Action: Add label: fixed wontfix exclusion:permanent"


class BugsOpenJdkHandler(BaseHandler):
    """
    URL handler for bugs.openjdk (Jira-based board)
    """
    BUGS_OPENJDK_API_BASE_URL = f'https://bugs.openjdk.java.net/rest/api/latest/issue'

    def can_handle(self, url):
        return 'bugs.openjdk' in url

    def handle(self, url):
        path = _extract_path(url)
        *_, issue_key = path.split('/')  # get the element after the last slash
        url = self.BUGS_OPENJDK_API_BASE_URL + '/' + issue_key

        resp = requests.get(url)
        resp.raise_for_status()
        resp_json = resp.json()
        status_name = resp_json.get('fields', {}).get('status', {}).get('name', '').lower()
        status_enum = self.name_to_status(status_name)
        if status_enum == Status.OPEN:
            return (status_enum, "OPEN",)
        else:
            resolution = resp_json.get('fields', {}).get('resolution', {}).get('name', '')
            return (status_enum, "CLOSED: " + self.resolution_parser(resolution, resp_json),)

    def resolution_parser(self, resolution, resp_json):
        if resolution == 'null' or resolution == '':
            return "Unknown resolution. Action: Investigate"
        elif resolution == "Won't Fix":
            return "Won't Fix"
        elif resolution == "Fixed":
            # Identify fix commit links while ignoring -dev links
            fix_commits_list = []
            comments_dict = resp_json.get('fields', {}).get('comment', {}).get('comments', [])
            fix_commits_list = self.comments_parser(comments_dict, fix_commits_list)
            # For each issue link identified as a backport, do the same.
            issues_list = resp_json.get('fields', {}).get('issuelinks', {})
            for issue_dict in issues_list:
                if issue_dict.get('type', {}).get('name', '') == "Backport":
                    backport_url = issue_dict.get('outwardIssue', {}).get('key', '')
                    if len(backport_url) == 0:
                        continue
                    backport_url = self.BUGS_OPENJDK_API_BASE_URL + "/" + backport_url
                    backport_resp = requests.get(backport_url)
                    backport_resp.raise_for_status()
                    backport_resp_json = backport_resp.json()
                    backport_comments = backport_resp_json.get('fields', {}).get('comment', {}).get('comments', [])
                    if len(backport_comments) == 0:
                        continue
                    fix_commits_list = self.comments_parser(backport_comments, fix_commits_list)
            # For every link, reduce it to the jdk version int and append to the fixed string.
            versions_csv = ","
            if len(fix_commits_list) == 0:
                return "Fixed but unpropagated. No action."
            for commit_url in fix_commits_list:
                single_version = ""
                if "/jdk/commit" in commit_url:
                    *_, commit_key = commit_url.split('/')  # get the element after the last slash
                    # Fix went into the openjdk/jdk repository.
                    # Will now attempt to identify the earliest tagged version.
                    single_version = "unknown_jdk_head_tag"
                    commit_resp = requests.get("https://github.com/openjdk/jdk/branch_commits/" + commit_key)
                    commit_resp.raise_for_status()
                    commit_resp_text = commit_resp.text
                    commit_tag_list = re.findall(">jdk-[0-9]+[^<]+<", commit_resp_text)
                    if len(commit_tag_list) == 0:
                        continue
                    commit_tag_end = commit_tag_list[len(commit_tag_list) - 1]
                    if len(commit_tag_end) <= 2:
                        continue
                    commit_tag_end = commit_tag_end[1:-1]
                    version_matcher = re.search("jdk-[0-9]+", commit_tag_end)
                    if version_matcher is None:
                        continue
                    version_matcher = re.search("[0-9]+", version_matcher.group())
                    if version_matcher is None:
                        continue
                    single_version = version_matcher.group() + "+"
                else:
                    version_matcher = re.search("jdk[0-9]+u?/commit", commit_url)
                    if version_matcher is None:
                        continue
                    version_matcher = re.search("[0-9]+", version_matcher.group())
                    if version_matcher is None:
                        continue
                    single_version = version_matcher.group()
                if ("," + single_version + ",") not in versions_csv:
                    versions_csv += single_version + ","
            return "Fixed. Action: Unexclude for JDK" + versions_csv[1:-1] + " only"
        else:
            return "Unknown resolution \"" + resolution + "\". Action: Investigate."

    def comments_parser(self, comments, URLs_list):
        for comment in comments:
            author = comment.get('author', {}).get('name', '')
            if author == "dukebot":
                comment_text = comment.get('body', '')
                comment_url = re.search(r"URL: +https://git\.openjdk\.org/jdk[0-9]*u?/commit/[0-9a-z]+", comment_text)
                if comment_url is None:
                    continue
                comment_url = re.search("https.*", comment_url.group())
                if comment_url is None:
                    continue
                URLs_list.append(comment_url.group())
        return URLs_list

class Dispatcher:
    """
    Dispatches each issue tracker to its respective handler
    """

    def __init__(self, handlers: List[BaseHandler]):
        self.handlers = handlers

    def dispatch(self, issue_url: str):
        for hdl in self.handlers:
            if hdl.can_handle(issue_url):
                return hdl.handle(issue_url)

        raise NoHandlerFoundException()


def augment_with_status(issues, issue_status):
    """
    Augment all issue items with the provided status
    """
    issues_with_status = [
        models.SchemeWithStatus(
            **issue,
            ISSUE_TRACKER_STATUS=issue_status.scheme_name,
        )
        for issue in issues
    ]
    return issues_with_status


def group_issues_by_url(issues: List[models.Scheme]) -> Dict[str, List[models.Scheme]]:
    url_to_issues = defaultdict(list)
    for issue in issues:
        url_to_issues[issue["ISSUE_TRACKER"]].append(issue)
    return url_to_issues


def should_exclude(url) -> Tuple[bool, str]:
    if url.startswith("#"):
        return True, "Ignoring non-url comment that starts with a hash."
    for exception in EXCEPTIONS:
        if exception in url:
            return True, exception
    return False, ''


def _handle_completed_future(future, log_prefix, url, url_to_issues) -> List[models.SchemeWithStatus]:
    global return_code
    try:
        result_tuple = future.result()
        issue_status: Status = result_tuple[0]
        complex_status: str = result_tuple[1]
    except HandlerException as he:
        LOG.error(f"{log_prefix} Error when handling {url!r}: {he}")
        return_code = 1
    except NoHandlerFoundException:
        LOG.error(f"{log_prefix} No handler found for {url!r}")
        return_code = 1
    except Exception as e:
        # Ignore "Unauthorized for url" errors as this is currently permitted.
        if "Unauthorized for url" in str(e):
            LOG.debug(f"{log_prefix} Ignoring access denial when handling {url!r}: {e}")
        else:
            LOG.error(f"{log_prefix} Uncaught exception for {url!r}: {e}")
            return_code = 1
    else:
        LOG.info(f"{log_prefix} Ended processing for {url!r}: {complex_status}")
        issues_with_status = augment_with_status(url_to_issues[url], issue_status)
        return issues_with_status
    # return an empty list if an error was caught
    return []


def fetch_all_statuses(issues: List[models.Scheme], dispatcher: Dispatcher, max_workers):
    """
    Fetch the status of all issue trackers using the provided dispatcher
    and augment each issue with their respective status
    """
    raw_url_to_issues = group_issues_by_url(issues)

    # Remove all issues whose tracker contains a URL segment listed in `EXCEPTIONS`
    url_to_issues = {}
    for url, issues in raw_url_to_issues.items():
        exclude, reason = should_exclude(url)
        if exclude:
            LOG.warning(f"Excluding {url!r} due to exception segment {reason!r}")
        else:
            url_to_issues[url] = issues

    len_trackers = len(url_to_issues)
    LOG.debug(f"Unique issue trackers found: {len_trackers}")

    # When `max_workers` is None, `ThreadPoolExecutor` uses a sensible default value based on the number of cores
    all_issues = []
    with futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        future_to_url = {executor.submit(dispatcher.dispatch, url): url for url in url_to_issues}
        for i, future in enumerate(futures.as_completed(future_to_url)):
            url = future_to_url[future]
            log_prefix = f"{i + 1}/{len_trackers} -"
            issues_with_status = _handle_completed_future(future, log_prefix, url, url_to_issues)
            all_issues.extend(issues_with_status)
    return all_issues


def main():
    parser = argparse.ArgumentParser(description="Fetch issue status for each disabled test", allow_abbrev=False)
    parser.add_argument('--github-user', metavar='USER',
                        help=f"GitHub User used for API [env: {GITHUB_USER_ENV}]")
    parser.add_argument('--github-token', metavar='TOKEN',
                        help=f"GitHub Token used for API [env: {GITHUB_TOKEN_ENV}]")
    parser.add_argument('--max-workers', type=int, default=None,
                        help=f"Maximum number of threads to spawn, default determined by ThreadPoolExecutor")
    parser.add_argument('--infile', '-i', type=argparse.FileType('r'), default=sys.stdin,
                        help='Input file, defaults to stdin')
    parser.add_argument('--outfile', '-o', type=argparse.FileType('w'), default=sys.stdout,
                        help='Output file, defaults to stdout')
    parser.add_argument('--verbose', '-v', action='count', default=0,
                        help="Enable info logging level, debug level if -vv")
    args = parser.parse_args()

    if not args.github_user:
        args.github_user = os.getenv(GITHUB_USER_ENV)
        if not args.github_user:
            parser.print_usage()
            parser.exit(1, message="GitHub User must be provided as environment variable or command-line argument")

    if not args.github_token:
        args.github_token = os.getenv(GITHUB_TOKEN_ENV)
        if not args.github_token:
            parser.print_usage()
            parser.exit(1, message="GitHub Token must be provided as environment variable or command-line argument")

    if args.verbose == 1:
        LOG.setLevel(logging.INFO)
    elif args.verbose == 2:
        LOG.setLevel(logging.DEBUG)

    LOG.debug(f"Loading JSON from {getattr(args.infile, 'name', '<unknown>')}")
    issues: List[models.Scheme] = json.load(args.infile)

    dispatcher = Dispatcher(
        handlers=[
            GitHubHandler(args.github_user, args.github_token),
            BugsOpenJdkHandler(),
        ]
    )

    issues_with_status = fetch_all_statuses(issues, dispatcher, args.max_workers)

    LOG.debug(f"Outputting JSON to {getattr(args.outfile, 'name', '<unknown>')}")
    json.dump(
        obj=issues_with_status,
        fp=args.outfile,
        indent=2,
    )

    global return_code
    return return_code


if __name__ == '__main__':
    sys.exit(main())
