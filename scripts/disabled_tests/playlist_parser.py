import argparse
import dataclasses as datacls
import json
import logging
import os
import sys
from typing import List, Iterable, ClassVar, Optional

import lxml.etree

from common import models, utils

logging.basicConfig(
    format="%(levelname)s - %(message)s"
)
LOG = logging.getLogger()

Element = lxml.etree.ElementBase


class TestNodeProcessingException(Exception):
    pass


class DisableNodeProcessingException(Exception):
    pass


@datacls.dataclass
class PlaylistFile:
    """Parser-specific information extracted from a playlist.xml file"""
    path: os.PathLike
    raw_tests: Iterable['RawTest']

    TEST_TAG: ClassVar = 'test'

    @classmethod
    def from_path(cls, playlist_path) -> 'PlaylistFile':
        if not os.path.isfile(playlist_path):
            raise ValueError(f'{playlist_path!r} is not a file')

        tree = lxml.etree.parse(playlist_path)
        tests = tree.findall(f'.//{cls.TEST_TAG}')

        raw_tests = [RawTest(node=test, playlist_file=None) for test in tests]

        playlist_file = PlaylistFile(
            path=playlist_path,
            raw_tests=raw_tests,
        )

        # populate back reference to the file
        for raw_disable in playlist_file.raw_tests:
            raw_disable.playlist_file = playlist_file

        return playlist_file


@datacls.dataclass
class RawTest:
    """Connects a test node to its originating file"""
    node: Element
    playlist_file: Optional[PlaylistFile]


@datacls.dataclass
class Test(RawTest):
    """Parsed information from a test node"""
    name: str
    disables: Iterable['RawDisable']
    variations: List[str]

    TEST_NAME_TAG: ClassVar = 'testCaseName'
    DISABLE_TAG: ClassVar = 'disable'
    VARIATIONS_TAG: ClassVar = 'variations'

    @classmethod
    def from_raw_test(cls, raw: RawTest) -> 'Test':
        test_name_node = raw.node.find(f'.//{cls.TEST_NAME_TAG}')
        if test_name_node is None:
            raise TestNodeProcessingException(f"test node has no {cls.TEST_NAME_TAG!r} child; skipping node")

        # this is expected to be an empty list for some, if not most, test nodes
        # we still create a `Test` instance instead of raising an error to make this more reusable
        disable_nodes = raw.node.findall(f'.//{cls.DISABLE_TAG}')
        disable_nodes = [RawDisable(node=node, parent_test=None) for node in disable_nodes]

        maybe_variations = raw.node.find(f'.//{cls.VARIATIONS_TAG}')
        if maybe_variations is not None:
            variations = [v.text for v in maybe_variations]
        else:
            variations = []

        test = Test(
            **utils.to_shallow_dict(raw),
            name=test_name_node.text,
            disables=disable_nodes,
            variations=variations,
        )

        # populate back reference to the parent test
        for rd in test.disables:
            rd.parent_test = test

        return test


@datacls.dataclass
class RawDisable:
    """Connects a disable node to its parent test node"""
    node: Element
    parent_test: Optional[Test]


@datacls.dataclass
class Disable(RawDisable):
    """Parsed information from a disable node"""
    issue_url: str
    custom_target: str
    jdk_info: models.JdkInfo

    ISSUE_TAG: ClassVar = 'comment'
    VARIATION_TAG: ClassVar = 'variation'
    VERSION_TAG: ClassVar = 'version'
    IMPLEMENTATION_TAG: ClassVar = 'impl'

    @classmethod
    def from_raw_disable(cls, raw_disable: RawDisable) -> 'Disable':
        issue_url_node = raw_disable.node.find(f'.//{cls.ISSUE_TAG}')
        if issue_url_node is None:
            raise DisableNodeProcessingException(f'disable node has no {cls.ISSUE_TAG!r} child; skipping node')
        issue_url = issue_url_node.text

        test_name = raw_disable.parent_test.name
        custom_target = test_name + cls.get_suffix(raw_disable)

        impl_node = raw_disable.node.find(f'.//{cls.IMPLEMENTATION_TAG}')
        if impl_node is not None:
            impl = impl_node.text
        else:
            impl = None

        version_node = raw_disable.node.find(f'.//{cls.VERSION_TAG}')
        if version_node is not None:
            version = version_node.text
        else:
            version = None

        jdk_info = models.JdkInfo(
            version=version,
            implementation=impl,
        )

        disable = Disable(
            **utils.to_shallow_dict(raw_disable),
            issue_url=issue_url,
            custom_target=custom_target,
            jdk_info=jdk_info,
        )
        return disable

    @classmethod
    def get_suffix(cls, raw_disable: RawDisable) -> str:
        """Get the suffix applied to the target in order to specify the variation. May be empty."""
        variations = raw_disable.parent_test.variations

        maybe_variation_node = raw_disable.node.find(f'.//{cls.VARIATION_TAG}')
        if maybe_variation_node is None:
            return ''

        variation = maybe_variation_node.text
        if variation not in variations:
            raise DisableNodeProcessingException(f'could not find {variation!r} in defined variations; skipping node')

        idx = variations.index(variation)
        suffix = f'_{idx}'
        return suffix

    def to_scheme(self) -> models.Scheme:
        return models.Scheme(
            JDK_VERSION=self.jdk_info.version,
            PLATFORM='all',
            ISSUE_TRACKER=self.issue_url,
            TARGET=utils.DEFAULT_TARGET,
            CUSTOM_TARGET=self.custom_target,
            JDK_IMPL=self.jdk_info.implementation,
        )


def parse_test(raw_test: RawTest) -> List[Disable]:
    test = Test.from_raw_test(raw_test)

    disables = []
    for raw_disable in test.disables:
        try:
            disable = Disable.from_raw_disable(raw_disable)
            disables.append(disable)
        except DisableNodeProcessingException as e:
            LOG.error(f'{raw_test.playlist_file.path}:{raw_disable.node.sourceline} : {e}')
    return disables


def parse_file(playlist_path: str) -> List[Disable]:
    playlist = PlaylistFile.from_path(playlist_path)

    disables_from_file = []
    for test in playlist.raw_tests:
        try:
            disables = parse_test(test)
            disables_from_file.extend(disables)
        except TestNodeProcessingException as e:
            LOG.error(f'{playlist_path}:{test.node.sourceline} : {e}')
    return disables_from_file


def parse_all_files(playlist_files: Iterable[str]) -> List[Disable]:
    all_disables: List[Disable] = []
    for playlist_path in playlist_files:
        LOG.debug(f"Processing {playlist_path!r}...")
        try:
            disables = parse_file(playlist_path)
        except Exception as e:
            LOG.error(f'Uncaught exception while processing {playlist_path!r} : {e}')
        else:
            all_disables.extend(disables)
            LOG.info(f"Processed {playlist_path!r} : n_disables={len(disables)}")
    return all_disables


def main():
    parser = argparse.ArgumentParser(description="Generate disabled test list JSON file from "
                                                 "playlist.xml files", allow_abbrev=False)
    parser.add_argument('--outfile', '-o', type=argparse.FileType('w'), default=sys.stdout,
                        help='Output file, defaults to stdout')
    parser.add_argument('--verbose', '-v', action='count', default=0,
                        help="Enable info logging level, debug level if -vv")
    args = parser.parse_args()

    if args.verbose == 1:
        LOG.setLevel(logging.INFO)
    elif args.verbose == 2:
        LOG.setLevel(logging.DEBUG)

    LOG.debug("Taking file list from <stdin>")
    playlist_files = [line.rstrip() for line in sys.stdin.readlines()]  # remove the \n from each lines

    all_exclusions = parse_all_files(playlist_files)

    # convert to schemed format
    json_exclusions = [excl.to_scheme() for excl in all_exclusions]

    LOG.debug(f"Outputting JSON to {getattr(args.outfile, 'name', '<unknown>')}")
    json.dump(
        obj=json_exclusions,
        fp=args.outfile,
        indent=2,
    )


if __name__ == '__main__':
    main()
