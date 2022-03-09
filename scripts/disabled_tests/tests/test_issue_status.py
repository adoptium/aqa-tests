import json
import os
from unittest import TestCase

from issue_status import fetch_all_statuses, Dispatcher, GitHubHandler, BugsOpenJdkHandler


class Test(TestCase):

    def test_status_fetch(self):
        test_data_dir = os.path.join(os.path.dirname(__file__), 'data')
        infile = os.path.join(test_data_dir, 'exclude_out.json')
        expected_out_file = os.path.join(test_data_dir, 'exclude_with_status_out.json')

        # GitHub handler will query anonymously; keep rate limits in mind if running this test often
        dispatcher = Dispatcher(handlers=[GitHubHandler(), BugsOpenJdkHandler()])
        with open(infile) as f:
            issues = json.load(f)
        with open(expected_out_file) as f:
            expected = json.load(f)

        # running with only 1 worker ensures we get consistent results
        actual = fetch_all_statuses(issues, dispatcher, max_workers=1)

        for v in actual:
            self.assertIn(v, expected)
