import json
import os
from unittest import TestCase
from unittest.mock import patch

from issue_status import Status, fetch_all_statuses, Dispatcher, GitHubHandler, BugsOpenJdkHandler


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
            base_item = {k: v[k] for k in expected[0].keys()}
            self.assertIn(base_item, expected)

    def test_duplicate_issue_resolution_fixed(self):
        handler = BugsOpenJdkHandler()

        issue_8050470 = {
            'key': 'JDK-8050470',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': 'Duplicate'},
                'issuelinks': [
                    {
                        'type': {'name': 'Duplicate'},
                        'inwardIssue': {'key': 'JDK-8251217'}
                    }
                ]
            }
        }

        issue_8251217 = {
            'key': 'JDK-8251217',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': 'Fixed'},
                'comment': {
                    'comments': [
                        {
                            'author': {'name': 'roboduke'},
                            'body': 'Changeset: 1234567\nAuthor: test\nDate: 2021-01-01\nURL: https://git.openjdk.org/jdk/commit/abcdef123456'
                        }
                    ]
                },
                'issuelinks': []
            }
        }

        def mock_get_resp_json(url):
            if 'JDK-8050470' in url:
                return issue_8050470
            elif 'JDK-8251217' in url:
                return issue_8251217
            raise ValueError(f"Unexpected url {url}")

        with patch.object(handler, 'get_resp_json', side_effect=mock_get_resp_json), \
             patch.object(handler, 'get_resp_text', return_value='>jdk-17+35<'):
            status_enum, complex_status = handler.handle('https://bugs.openjdk.org/browse/JDK-8050470')
            self.assertEqual(status_enum, Status.CLOSED)
            self.assertEqual(complex_status, 'CLOSED: Fixed. Action: Unexclude for JDK: 17+')

    def test_duplicate_issue_chain_resolution(self):
        handler = BugsOpenJdkHandler()

        issue_a = {
            'key': 'JDK-1111111',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': 'Duplicate'},
                'issuelinks': [
                    {
                        'type': {'name': 'Duplicate'},
                        'inwardIssue': {'key': 'JDK-2222222'}
                    }
                ]
            }
        }

        issue_b = {
            'key': 'JDK-2222222',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': 'Duplicate'},
                'issuelinks': [
                    {
                        'type': {'name': 'Duplicate'},
                        'inwardIssue': {'key': 'JDK-3333333'}
                    }
                ]
            }
        }

        issue_c = {
            'key': 'JDK-3333333',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': "Won't Fix"},
                'issuelinks': []
            }
        }

        def mock_get_resp_json(url):
            if 'JDK-1111111' in url:
                return issue_a
            elif 'JDK-2222222' in url:
                return issue_b
            elif 'JDK-3333333' in url:
                return issue_c
            raise ValueError(f"Unexpected url {url}")

        with patch.object(handler, 'get_resp_json', side_effect=mock_get_resp_json):
            status_enum, complex_status = handler.handle('https://bugs.openjdk.org/browse/JDK-1111111')
            self.assertEqual(status_enum, Status.CLOSED)
            self.assertEqual(complex_status, "CLOSED: Won't Fix")

    def test_duplicate_issue_core_open(self):
        handler = BugsOpenJdkHandler()

        issue_a = {
            'key': 'JDK-1111111',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': 'Duplicate'},
                'issuelinks': [
                    {
                        'type': {'name': 'Duplicate'},
                        'inwardIssue': {'key': 'JDK-2222222'}
                    }
                ]
            }
        }

        issue_b = {
            'key': 'JDK-2222222',
            'fields': {
                'status': {'name': 'Open'},
                'resolution': None,
                'issuelinks': []
            }
        }

        def mock_get_resp_json(url):
            if 'JDK-1111111' in url:
                return issue_a
            elif 'JDK-2222222' in url:
                return issue_b
            raise ValueError(f"Unexpected url {url}")

        with patch.object(handler, 'get_resp_json', side_effect=mock_get_resp_json):
            status_enum, complex_status = handler.handle('https://bugs.openjdk.org/browse/JDK-1111111')
            self.assertEqual(status_enum, Status.OPEN)
            self.assertEqual(complex_status, "OPEN")

    def test_duplicate_issue_circular(self):
        handler = BugsOpenJdkHandler()

        issue_a = {
            'key': 'JDK-1111111',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': 'Duplicate'},
                'issuelinks': [
                    {
                        'type': {'name': 'Duplicate'},
                        'inwardIssue': {'key': 'JDK-2222222'}
                    }
                ]
            }
        }

        issue_b = {
            'key': 'JDK-2222222',
            'fields': {
                'status': {'name': 'Closed'},
                'resolution': {'name': 'Duplicate'},
                'issuelinks': [
                    {
                        'type': {'name': 'Duplicate'},
                        'inwardIssue': {'key': 'JDK-1111111'}
                    }
                ]
            }
        }

        def mock_get_resp_json(url):
            if 'JDK-1111111' in url:
                return issue_a
            elif 'JDK-2222222' in url:
                return issue_b
            raise ValueError(f"Unexpected url {url}")

        with patch.object(handler, 'get_resp_json', side_effect=mock_get_resp_json):
            status_enum, complex_status = handler.handle('https://bugs.openjdk.org/browse/JDK-1111111')
            self.assertEqual(status_enum, Status.CLOSED)
            self.assertIn("Action: investigate", complex_status)
