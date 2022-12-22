import json
import os
from unittest import TestCase

from exclude_parser import transform_platform, parse_all_files

platform_map = {
    "linux-aarch64": "aarch64_linux",
    "linux-ppc64le": "ppc64le_linux",
    "linux-arm": "arm_linux",
    "linux-s390x": "s390x_linux",
    "linux-x64": "x86-64_linux",
    "macosx-x64": "x86-64_mac",
    "windows-x64": "x86-64_windows",
    "windows-x86": "x86-32_windows",
    "z/OS-s390x": "s390x_zos",
    "linux-ppc32": "ppc32_linux",
    "linux-ppc64": "ppc64_linux",
    "linux-riscv64": "riscv64_linux",
    "linux-loongarch64": "loongarch64_linux",
    "linux-s390": "s390_linux",
    "solaris-sparcv9": "sparcv9_solaris",
    "solaris-x86-64": "x86-64_solaris",
    "alpine-linux-x86-64": "x86-64_alpine-linux",
    "alpine-linux_aarch64": "aarch64_alpine-linux",
    "linux-x86-32": "x86-32_linux",

    "linux-all": "all_linux",
    "macosx-all": "all_mac",
}


class Test(TestCase):
    def test_transform_platform_with_map(self):
        for k, v in platform_map.items():
            self.assertEqual(transform_platform(k), v)

    def test_parse_file(self):
        test_data_dir = os.path.join(os.path.dirname(__file__), 'data')
        input_file1 = os.path.join(test_data_dir, 'ProblemList_openjdk13.txt')
        input_file2 = os.path.join(test_data_dir, 'ProblemList_openjdk11-openj9.txt')
        expected_out_file = os.path.join(test_data_dir, 'exclude_out.json')
        with open(expected_out_file) as f:
            expected = json.load(f)
        actual = [excl.to_scheme() for excl in parse_all_files([input_file1, input_file2])]
        self.assertEqual(expected, actual)

