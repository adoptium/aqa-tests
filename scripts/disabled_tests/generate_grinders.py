import argparse
import json
import logging
import os
import sys
from typing import List, Iterable, ClassVar, Optional

from common import models

return_code = 0

logging.basicConfig(
    format="%(levelname)s - %(message)s"
)
LOG = logging.getLogger()


def identify_fixed_tests(issues: List[models.SchemeWithStatus]):
    LOG.info("Script works")
    global return_code
    
    url_to_issues = defaultdict(list)
    for issue in issues:
        if issue["ISSUE_TRACKER"].startswith("#"):
            url_to_issues[issue["ISSUE_TRACKER"].strip()].append(issue)
        else: 
            urls_list = issue["ISSUE_TRACKER"].split(",")
            for url in urls_list:
                url_to_issues[url.strip()].append(issue)
    return url_to_issues

def test_script_works()

def main():
    global return_code

    parser = argparse.ArgumentParser(description="Generate grinder links in markdown format from"
                                                 "issue_staus.py output file.", allow_abbrev=False)
    parser.add_argument('--infile', '-i', type=argparse.FileType('r'), default=sys.stdin,
                        help='Input file, defaults to stdin')
    parser.add_argument('--outfile', '-o', type=argparse.FileType('w'), default=sys.stdout,
                        help='Output file, defaults to stdout')
    parser.add_argument('--verbose', '-v', action='count', default=0,
                        help="Enable info logging level, debug level if -vv")
    args = parser.parse_args()

    if args.verbose == 1:
        LOG.setLevel(logging.INFO)
    elif args.verbose == 2:
        LOG.setLevel(logging.DEBUG)

    LOG.debug(f"Loading JSON from {getattr(args.infile, 'name', '<unknown>')}")
    issues: List[models.SchemeWithStatus] = json.load(args.infile)

    identify_fixed_tests(issues)

    test_script_works()

    return return_code


if __name__ == '__main__':
    sys.exit(main())
