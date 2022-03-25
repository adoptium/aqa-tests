import dataclasses as datacls
import os
import json
import argparse
import re
import logging
import sys
from typing import List, ClassVar, Optional, Iterable

from common.models import Scheme, JdkInfo
from common.utils import to_shallow_dict, DEFAULT_TARGET

logging.basicConfig(
    format="%(levelname)s - %(message)s"
)
LOG = logging.getLogger()

OS_EXCEPTIONS = {
    "macosx": "mac",
    "z/os": "zos",
    "sunos": "solaris",
}

ARCH_EXCEPTIONS = {
    "x64": "x86-64",
    "x86": "x86-32",
}


class ExclusionFileProcessingException(Exception):
    pass


class TestExclusionProcessingException(Exception):
    def __init__(self, message: str, test_excl=None, *args):
        self.test_excl = test_excl
        super().__init__(message, *args)


@datacls.dataclass
class ExcludeFileInfo:
    """Information extracted from an 'exclude' file"""
    jdk_info: JdkInfo
    path: os.PathLike
    lines: List['TestExclusionRawLine']

    FILE_PATTERN: ClassVar = re.compile(r'ProblemList_openjdk(?P<jdk_version>\d+)-?(?P<jdk_impl>.*).txt')

    @classmethod
    def get_jdk_info(cls, exclude_path):
        filename = os.path.basename(exclude_path)
        # As of now, the ProblemList*.txt files are named in the following format:
        # ProblemList_openjdk<JDK_VERSION>-<JDK_IMPL>.txt  # for JDK_IMPL = openj9 and sap
        # or
        # ProblemList_openjdk<JDK_VERSION>.txt              # for JDK_IMPL = hotspot
        match = cls.FILE_PATTERN.search(filename)
        if match is None:
            raise ExclusionFileProcessingException(
                f'filename of {exclude_path!r} does not match regex pattern {cls.FILE_PATTERN.pattern!r}')

        return JdkInfo(
            version=match.group('jdk_version'),
            implementation=match.group('jdk_impl'),
        )

    @classmethod
    def from_path(cls, exclude_path):
        if not os.path.isfile(exclude_path):
            raise ExclusionFileProcessingException(f'{exclude_path!r} is not a file')

        jdk_info = cls.get_jdk_info(exclude_path)

        # 'exclude tests' are lines that are not empty AND do not start with #
        with open(exclude_path, mode='r') as f:
            raw_lines = [
                TestExclusionRawLine(
                    line_number=(i + 1),
                    raw_line=line.rstrip(),
                    origin_file=None,  # populated right below
                )
                for i, line in enumerate(f.readlines())
                if line.strip() not in '' and not line.strip().startswith("#")
            ]

        file_info = cls(
            jdk_info=jdk_info,
            path=exclude_path,
            lines=raw_lines,
        )

        # populate back reference to file for each line
        for line in file_info.lines:
            line.origin_file = file_info

        return file_info


@datacls.dataclass
class TestExclusionRawLine:
    """Raw test exclusion line from a 'exclude' file"""
    line_number: int
    raw_line: str
    origin_file: Optional[ExcludeFileInfo]


@datacls.dataclass
class TestExclusionSplitLine(TestExclusionRawLine):
    """Test exclusion line split to a triplet"""
    custom_target: str
    issue_url: str
    raw_platform: str

    @classmethod
    def from_raw_line(cls, test_excl: TestExclusionRawLine):
        split_line = test_excl.raw_line.split(maxsplit=2)
        if len(split_line) != 3:
            raise TestExclusionProcessingException(
                f'Not exactly 3 elements when splitting {test_excl.raw_line}', test_excl)
        custom_target, issue_url, raw_platform = split_line
        return cls(
            **to_shallow_dict(test_excl),
            custom_target=custom_target,
            issue_url=issue_url,
            raw_platform=raw_platform,
        )


@datacls.dataclass
class TestExclusion(TestExclusionSplitLine):
    """Test exclusion line with raw platform expanded to a set of concrete platform names"""
    platforms: List[str]

    @classmethod
    def from_split_line(cls, test_excl: TestExclusionSplitLine):
        try:
            platforms = resolve_platforms(test_excl)
        except ValueError as e:
            raise TestExclusionProcessingException(str(e), test_excl) from e
        return cls(
            **to_shallow_dict(test_excl),
            platforms=platforms,
        )

    def to_scheme(self) -> Scheme:
        return {
            "JDK_VERSION": self.origin_file.jdk_info.version,
            "JDK_IMPL": self.origin_file.jdk_info.implementation,
            "TARGET": DEFAULT_TARGET,
            "CUSTOM_TARGET": self.custom_target,
            "PLATFORM": ','.join(self.platforms),
            "ISSUE_TRACKER": self.issue_url,
        }


def transform_platform(os_arch_platform: str) -> str:
    """
    Transforms an "os-arch"-formatted platform into
    an "arch_os"-formatted platform, suitable for consumption by Jenkins.

    :param os_arch_platform: the "os-arch"-formatted platform
    :return: a Jenkins-suitable platform name
    """

    # only deal in lower case
    os_arch_platform = os_arch_platform.lower()

    # split over a dash
    # EXCEPT if it is preceded by "x86" (because of "x86-32" and "x86-64")
    # or if it is preceded by "alpine" (because of linux distros like "alpine-linux")
    split_pattern = re.compile(r"(?<!x86)(?<!alpine)-")
    split_list = split_pattern.split(os_arch_platform)

    if len(split_list) != 2:
        raise ValueError(f"Cannot split {os_arch_platform!r} over regex pattern {split_pattern.pattern!r}")

    os_name, arch_name = split_list

    # rename OS if needed
    if os_name in OS_EXCEPTIONS:
        os_name = OS_EXCEPTIONS[os_name]

    # rename Arch if needed
    if arch_name in ARCH_EXCEPTIONS:
        arch_name = ARCH_EXCEPTIONS[arch_name]

    return f"{arch_name}_{os_name}"


def resolve_platforms(split: TestExclusionSplitLine) -> List[str]:
    revolved_platforms = []
    list_of_unresolved_platform_names = [s.strip() for s in split.raw_platform.split(",") if s.strip()]
    for plat in list_of_unresolved_platform_names:
        if plat == "generic-all":
            return ["all"]

        if "_" in plat:
            LOG.warning(f'{split.origin_file.path}:{split.line_number} : '
                        f'assuming {plat!r} already formatted to ARCH_OS; continuing without transformation')
            revolved_platforms.append(plat)
        else:
            revolved_platforms.append(transform_platform(plat))

    return revolved_platforms


def parse_all_files(exclude_files: Iterable[str]) -> List[TestExclusion]:
    all_exclusions: List[TestExclusion] = []
    for exclude_path in exclude_files:
        LOG.debug(f"Processing {exclude_path}...")
        try:
            exclusions = parse_file(exclude_path)
            all_exclusions.extend(exclusions)
        except ExclusionFileProcessingException as e:
            LOG.error(f'{exclude_path} : {e}')
    return all_exclusions


def parse_file(exclude_path) -> List[TestExclusion]:
    exclude_file = ExcludeFileInfo.from_path(exclude_path)

    exclusions = []
    for raw_line in exclude_file.lines:
        try:
            test_excl = parse_line(raw_line)
            exclusions.append(test_excl)
        except TestExclusionProcessingException as e1:
            LOG.error(f'{raw_line.origin_file.path}:{raw_line.line_number} : {e1}')
    return exclusions


def parse_line(line: TestExclusionRawLine) -> TestExclusion:
    test_excl_split = TestExclusionSplitLine.from_raw_line(line)
    test_excl = TestExclusion.from_split_line(test_excl_split)
    return test_excl


def main():
    parser = argparse.ArgumentParser(description="Generate disabled test list JSON file from "
                                                 "exclude/ProblemList*.txt files")
    parser.add_argument('--exclude_dir', type=str,
                        help='Source directory containing ProblemList*.txt files. '
                             'Defaults to lines passed from stdin')
    parser.add_argument('--json_out', type=str,
                        help='Destination path of the generated JSON file. '
                             'Defaults to printing to stdout')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help="Enable logging debug mode")
    args = parser.parse_args()

    if args.verbose:
        LOG.setLevel(logging.DEBUG)

    # if the dir containing the exclude ProblemList*.txt is not passed, the attempt to use openjdk/excludes/ dir instead
    if args.exclude_dir:
        LOG.debug("Taking file list from directory")
        exclude_files = [os.path.join(args.exclude_dir, file_name)
                         for file_name in os.listdir(args.exclude_dir)]
    else:
        LOG.debug("Taking file list from stdin")
        exclude_files = [line.rstrip() for line in sys.stdin.readlines()]  # remove the \n from each lines

    if args.json_out:
        output_dir = os.path.dirname(args.json_out)
        if not os.path.exists(output_dir):
            LOG.error(f"Directory of {args.json_out!r} does not exist")
            exit(1)

    all_exclusions = parse_all_files(exclude_files)

    # convert to schemed format
    json_exclusions = [excl.to_scheme() for excl in all_exclusions]

    if args.json_out:
        LOG.debug(f"Outputting to {args.json!r}")
        stream = open(args.json_out, 'w')
    else:
        LOG.debug(f"Outputting to stdout")
        stream = sys.stdout

    with stream as fp:
        json.dump(
            obj=json_exclusions,
            fp=fp,
            indent=2,
        )


if __name__ == '__main__':
    main()
