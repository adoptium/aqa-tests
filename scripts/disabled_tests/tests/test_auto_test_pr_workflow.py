import os
from pathlib import Path
import re
import subprocess
import tempfile
import textwrap
from unittest import TestCase


WORKFLOW = Path(__file__).resolve().parents[3] / ".github/workflows/autoTestPR.yml"


class TestAutoTestPRTitles(TestCase):
    def run_step(self, name, comment):
        workflow = WORKFLOW.read_text()
        step = workflow.split(f"      - name: {name}\n", 1)[1]
        script = re.search(r"        run: \|\n((?:          .*\n|\n)+)", step).group(1)
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "output"
            env = dict(os.environ, COMMENT_BODY=comment, COMMENTER="tester",
                       GITHUB_OUTPUT=str(output))
            subprocess.run(["bash", "-e", "-c", textwrap.dedent(script)],
                           env=env, check=True, capture_output=True, text=True)
            return dict(line.split("=", 1) for line in output.read_text().splitlines())

    def test_legacy_title_boundaries(self):
        for length in (65, 66, 67, 200):
            with self.subTest(length=length):
                comment = "a" * length
                title = self.run_step("Prepare title", comment)["title"]
                expected = comment if length <= 66 else comment[:63] + "..."
                self.assertEqual(title, expected)
                self.assertLessEqual(len("AUTO: " + title), 72)

    def test_exclude_title_boundaries_and_full_testcases(self):
        prefix = "Exclude OpenJDK test: "
        for length in (49, 50, 51, 200):
            with self.subTest(length=length):
                testcases = "a" * length
                result = self.run_step("Parse exclude command",
                                       f"/exclude {testcases} https://example.com linux-x64")
                full_title = prefix + testcases
                expected = full_title if len(full_title) <= 72 else full_title[:69] + "..."
                self.assertEqual(result["title"], expected)
                self.assertEqual(result["testcases"], testcases)

    def test_multiple_tests_show_first_and_preserve_full_list(self):
        prefix = "Exclude OpenJDK test: "
        for first in ("test1.java", "a" * 47, "a" * 48, "a" * 200):
            with self.subTest(first=first):
                testcases = f"{first},test2.java,test3.java"
                result = self.run_step("Parse exclude command",
                                       f"/exclude {testcases} https://example.com linux-x64")
                title = prefix + first + "..."
                expected = title if len(title) <= 72 else title[:69] + "..."
                self.assertEqual(result["title"], expected)
                self.assertEqual(result["testcases"], testcases)
                self.assertLessEqual(len(result["title"]), 72)

    def test_multiline_and_unicode(self):
        legacy = self.run_step("Prepare title", "auto exclude test é\r\ninjected=value")
        self.assertEqual(legacy["title"], "auto exclude test é ")
        self.assertNotIn("injected", legacy)
        result = self.run_step("Parse exclude command",
                               "/exclude é.java https://example.com linux-x64\ninjected=value")
        self.assertEqual(result["title"], "Exclude OpenJDK test: é.java")
        self.assertEqual(result["testcases"], "é.java")
        self.assertNotIn("injected", result)

    def test_create_pr_uses_bounded_titles_and_full_testcases(self):
        workflow = WORKFLOW.read_text()
        self.assertEqual(workflow.count("${{ steps.title.outputs.title }}"), 2)
        self.assertEqual(workflow.count("${{ steps.parse.outputs.title }}"), 2)
        self.assertIn("- **Test(s)**: `${{ steps.parse.outputs.testcases }}`", workflow)
