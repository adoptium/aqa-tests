import abc
import argparse
import enum
import json
import logging
import os
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

# Partial URL segments used to filter exceptional issues
EXCEPTIONS = [
    '/aqa-tests/issues/1297',
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
        return self.name_to_status(status_name)


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
        return self.name_to_status(status_name)


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
    for exception in EXCEPTIONS:
        if exception in url:
            return True, exception
    return False, ''


def _handle_completed_future(future, log_prefix, url, url_to_issues) -> List[models.SchemeWithStatus]:
    try:
        issue_status: Status = future.result()
    except HandlerException as he:
        LOG.error(f"{log_prefix} Error when handling {url!r}: {he}")
    except NoHandlerFoundException:
        LOG.error(f"{log_prefix} No handler found for {url!r}")
    except Exception as e:
        LOG.error(f"{log_prefix} Uncaught exception for {url!r}: {e}")
    else:
        LOG.info(f"{log_prefix} Ended processing for {url!r}: {issue_status.name}")
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


if __name__ == '__main__':
    main()
