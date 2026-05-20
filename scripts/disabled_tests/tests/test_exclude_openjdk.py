import os
import tempfile
from unittest import TestCase

from exclude_openjdk import (
    add_exclusion_to_file,
    parse_exclude_command,
    process_exclude_command,
    resolve_targets_from_params,
)


class Test(TestCase):
    def _create_file(self, path, content=""):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            f.write(content)

    def test_parse_exclude_command_with_optional_params(self):
        comment_body = """Some text above
/exclude java/lang/String/StringRepeat.java,java/util/Map/Basic.java https://github.com/adoptium/aqa-tests/issues/1272 linux-x64 jdk=11,17 impl=openj9
Some text below"""

        testcases, issue_url, platform, optional_params = parse_exclude_command(comment_body)

        self.assertEqual(
            ["java/lang/String/StringRepeat.java", "java/util/Map/Basic.java"],
            testcases,
        )
        self.assertEqual("https://github.com/adoptium/aqa-tests/issues/1272", issue_url)
        self.assertEqual("linux-x64", platform)
        self.assertEqual({"jdk": "11,17", "impl": "openj9"}, optional_params)

    def test_resolve_targets_for_root_impl_alpine_and_vendor(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            excludes_dir = os.path.join(temp_dir, "openjdk", "excludes")

            root_file = os.path.join(excludes_dir, "ProblemList_openjdk11.txt")
            impl_file = os.path.join(excludes_dir, "ProblemList_openjdk11-openj9.txt")
            alpine_file = os.path.join(excludes_dir, "alpine", "ProblemList_openjdk11.txt")
            vendor_file = os.path.join(excludes_dir, "vendors", "microsoft", "ProblemList_openjdk11.txt")

            self._create_file(root_file)
            self._create_file(impl_file)
            self._create_file(alpine_file)
            self._create_file(vendor_file)

            self.assertEqual(
                [("jdk11", root_file)],
                resolve_targets_from_params({"jdk": "11"}, temp_dir),
            )
            self.assertEqual(
                [("jdk11-openj9", impl_file)],
                resolve_targets_from_params({"jdk": "11", "impl": "openj9"}, temp_dir),
            )
            self.assertEqual(
                [("jdk11-alpine", alpine_file)],
                resolve_targets_from_params({"jdk": "11", "variant": "alpine"}, temp_dir),
            )
            self.assertEqual(
                [("jdk11-microsoft", vendor_file)],
                resolve_targets_from_params({"jdk": "11", "variant": "microsoft"}, temp_dir),
            )

    def test_process_exclude_command_skips_duplicate_and_adds_new(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            excludes_file = os.path.join(temp_dir, "openjdk", "excludes", "ProblemList_openjdk11.txt")
            existing = (
                "java/lang/String/StringRepeat.java https://github.com/adoptium/aqa-tests/issues/1272 linux-x64\n"
            )
            self._create_file(excludes_file, existing)

            comment_body = (
                "/exclude java/lang/String/StringRepeat.java,java/util/Map/Basic.java "
                "https://github.com/adoptium/aqa-tests/issues/1272 linux-x64 jdk=11"
            )

            result = process_exclude_command(comment_body, temp_dir)

            self.assertEqual(1, result["total_exclusions_added"])
            self.assertEqual(1, result["total_exclusions_skipped"])

            with open(excludes_file, 'r') as f:
                lines = [line.strip() for line in f if line.strip()]

            self.assertEqual(2, len(lines))
            self.assertIn(
                "java/lang/String/StringRepeat.java https://github.com/adoptium/aqa-tests/issues/1272 linux-x64",
                lines,
            )
            self.assertIn(
                "java/util/Map/Basic.java https://github.com/adoptium/aqa-tests/issues/1272 linux-x64",
                lines,
            )

    def test_add_exclusion_to_file_returns_false_for_duplicate(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            file_path = os.path.join(temp_dir, "ProblemList_openjdk11.txt")
            line = "java/lang/String/StringRepeat.java https://github.com/adoptium/aqa-tests/issues/1272 linux-x64\n"
            self._create_file(file_path, line)

            was_added = add_exclusion_to_file(
                file_path,
                "java/lang/String/StringRepeat.java",
                "https://github.com/adoptium/aqa-tests/issues/1272",
                "linux-x64",
            )

            self.assertFalse(was_added)
            with open(file_path, 'r') as f:
                self.assertEqual([line], f.readlines())
