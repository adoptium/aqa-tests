import argparse
import json
import logging
import os
import re
import sys
import textwrap
from typing import List, Sequence

from common import models

logging.basicConfig(
    format="%(levelname)s - %(message)s",
)
LOG = logging.getLogger()

# common prefix for environment variables
ENV_ARG_PREFIX = 'AQA_ISSUE_FILTER_'


class Filter:
    # name of command-line argument for this filter. Overridden by subclasses
    CLI_ARG_NAME: str
    # name of the environment variable for this filter. Overridden by subclasses
    ENV_ARG_NAME: str
    # name of the property under which the value of the argument will be stored. Overridden by subclasses
    CLI_METAVAR: str

    # flag to indicate raw regex expressions
    RE_PREFIX: str = 're:'

    def __init__(self, pattern_source: str, pattern: re.Pattern):
        self.pattern_source = pattern_source
        self.pattern = pattern

    def accept(self, issue: models.Scheme) -> bool:
        """
        Returns `True` if this filter "matches" the given issue; `False` otherwise
        """
        field_value = self.extract_field(issue)
        return self.pattern.match(field_value) is not None

    def extract_field(self, s: models.Scheme) -> str:
        """
        Extract the relevant field value for a filter from an issue scheme
        """
        raise NotImplemented

    # extra information regarding filter formats; appended to the command-line help text
    CLI_EXTRA_HELP_TEXT: str = textwrap.dedent(f"""
            Filter expressions can be provided through (in ascending order of priority):
            1. command-line switches
            2. environment variables

            Filter expressions can be of any 2 formats:
            1. a comma-separated list of exact matches
               info: whitespace around commas matters; case-insensitive
               e.g.
               - 11,17,18+
               - openJ9
               - aarch64_linux,Aarch64_macos
            2. a regular expression, prefixed with {RE_PREFIX!r}
               info: be cautious of escaping rules on your shell when using a backslash '\\'
               e.g.
               - {RE_PREFIX}1[4-8]
               - {RE_PREFIX}x86\\-64.*
            Note: an empty expression is equivalent to not specifying the filter at all
               e.g.
               --jdk-version=\"\" is equivalent to not specifying a filter on jdk-version""")

    @classmethod
    def from_string(cls, string: str):
        """
        Construct a `Filter` from a user-provided string
        """
        if string.startswith(cls.RE_PREFIX):
            re_pattern = string[len(cls.RE_PREFIX):]  # remove prefix
            LOG.debug(f'Using user-provided pattern {re_pattern!r} for {cls.CLI_ARG_NAME}')
        else:
            exact_matches = string.split(',')
            # escape characters that have a significance in regular expressions (like '+', '.', etc.)
            escaped_matches = [re.escape(m) for m in exact_matches]
            re_pattern = '(?i)^' + '|'.join(f'({m})' for m in escaped_matches) + '$'
            LOG.debug(f'Using generated pattern {re_pattern!r} for {cls.CLI_ARG_NAME}')

        compiled_pattern = re.compile(re_pattern)
        return cls(
            pattern_source=string,
            pattern=compiled_pattern
        )


class JdkVersionFilter(Filter):
    CLI_ARG_NAME = 'jdk-version'
    ENV_ARG_NAME = ENV_ARG_PREFIX + 'JDK_VERSION'
    CLI_METAVAR = 'jdk_version'

    def extract_field(self, s: models.Scheme) -> str:
        return s['JDK_VERSION']


class JdkImplementationFilter(Filter):
    CLI_ARG_NAME = 'jdk-implementation'
    ENV_ARG_NAME = ENV_ARG_PREFIX + 'JDK_IMPLEMENTATION'
    CLI_METAVAR = 'jdk_implementation'

    def extract_field(self, s: models.Scheme) -> str:
        return s['JDK_IMPL']


class PlatformFilter(Filter):
    CLI_ARG_NAME = 'platform'
    ENV_ARG_NAME = ENV_ARG_PREFIX + 'PLATFORM'
    CLI_METAVAR = 'platform'

    def extract_field(self, s: models.Scheme) -> str:
        return s['PLATFORM']


def build_filters_from_args_and_env(args):
    """
    Construct `Filter` instances corresponding to each filter specified on the command-line
    """
    filters = []

    # for all subclasses of `Filter`
    for filter_klass in Filter.__subclasses__():
        # get argument value corresponding to that subclass, either from the cli or the env
        filter_arg = getattr(args, filter_klass.CLI_METAVAR, None) or os.getenv(filter_klass.ENV_ARG_NAME, None)

        if filter_arg:
            # user provided a value
            filter_inst = filter_klass.from_string(filter_arg)
            filters.append(filter_inst)
        else:
            LOG.debug(f'No filter applied for {filter_klass.CLI_ARG_NAME}')

    return filters


def filter_all_issues(issues: Sequence[models.Scheme], filters: Sequence[Filter]):
    """
    Filter issues using filter instances
    """
    filtered_issues = []
    len_issues = len(issues)
    for i, issue in enumerate(issues):
        log_prefix = f'{i + 1}/{len_issues}: '
        if all((objector := f).accept(issue) for f in filters):
            filtered_issues.append(issue)
            LOG.debug(log_prefix + 'accepted')
        else:
            LOG.info(log_prefix + f'rejected: {objector.CLI_ARG_NAME}={objector.extract_field(issue)!r} '
                                  f'does not match {objector.pattern_source!r} (re: {objector.pattern.pattern})')

    return filtered_issues


def main():
    parser = argparse.ArgumentParser(description="Filter issues stored in JSON files\n\n" + Filter.CLI_EXTRA_HELP_TEXT,
                                     allow_abbrev=False, formatter_class=argparse.RawDescriptionHelpFormatter)
    for filter_klass in Filter.__subclasses__():
        parser.add_argument(f'--{filter_klass.CLI_ARG_NAME}', type=str, default=None, metavar=filter_klass.CLI_METAVAR,
                            help=f"Filter for {filter_klass.CLI_ARG_NAME} [env: {filter_klass.ENV_ARG_NAME}]")
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

    LOG.debug("Building filters")
    filters = build_filters_from_args_and_env(args)

    LOG.debug(f"Loading JSON from {getattr(args.infile, 'name', '<unknown>')}")
    issues: List[models.Scheme] = json.load(args.infile)

    filtered_issues: List[models.Scheme] = filter_all_issues(issues, filters)

    LOG.debug(f"Outputting JSON to {getattr(args.outfile, 'name', '<unknown>')}")
    json.dump(
        obj=filtered_issues,
        fp=args.outfile,
        indent=2,
    )


if __name__ == '__main__':
    main()
