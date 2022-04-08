import json
import os
from unittest import TestCase

from issue_filter import JdkVersionFilter, JdkImplementationFilter, PlatformFilter, filter_all_issues


class Test(TestCase):

    def test_filter(self):
        test_data_dir = os.path.join(os.path.dirname(__file__), 'data')
        infile = os.path.join(test_data_dir, 'exclude_out.json')
        expected_out_file = os.path.join(test_data_dir, 'exclude_out_filtered.json')

        with open(infile) as f:
            issues = json.load(f)
        with open(expected_out_file) as f:
            expected = json.load(f)

        filters = [
            JdkVersionFilter.from_string('13'),
            JdkImplementationFilter.from_string('hotspot'),
            PlatformFilter.from_string('re:.*linux.*'),
        ]

        actual = filter_all_issues(issues, filters)

        self.assertEqual(actual, expected)
