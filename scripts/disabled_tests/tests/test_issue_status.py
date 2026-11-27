import json
import os
import threading
import time
import urllib.error
from unittest import TestCase
from unittest.mock import patch

import issue_status
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

    # ------------------------------------------------------------------
    # Retry / rate-limit handling
    # ------------------------------------------------------------------

    def test_retry_delay_exponential_without_headers(self):
        self.assertEqual(issue_status._retry_delay(1), 7)
        self.assertEqual(issue_status._retry_delay(3), 28)

    def test_retry_delay_honours_retry_after(self):
        self.assertEqual(issue_status._retry_delay(5, {'Retry-After': '12'}), 12)

    def test_retry_delay_honours_rate_limit_reset(self):
        headers = {'X-RateLimit-Remaining': '0', 'X-RateLimit-Reset': str(time.time() + 50)}
        self.assertAlmostEqual(issue_status._retry_delay(1, headers), 51, delta=2)

    def test_retry_delay_ignores_reset_when_quota_remains(self):
        headers = {'X-RateLimit-Remaining': '10', 'X-RateLimit-Reset': str(time.time() + 500)}
        self.assertEqual(issue_status._retry_delay(1, headers), 7)

    def _http_error(self, code, headers=None):
        return urllib.error.HTTPError('https://x', code, 'err', headers or {}, None)

    def test_fetch_gives_up_when_total_wait_exceeds_cap(self):
        issue_status._STOP_EVENT.clear()
        waits = []
        with patch.object(issue_status.urllib.request, 'urlopen',
                          side_effect=self._http_error(429)), \
             patch.object(issue_status._STOP_EVENT, 'wait',
                          side_effect=lambda d: waits.append(d) or False):
            with self.assertRaises(issue_status.RetryExhausted):
                issue_status._fetch_with_retry('https://x')
        # 7 + 14 + 28 + 56 = 105 <= 120; the next wait (112) would exceed the cap
        self.assertEqual(waits, [7, 14, 28, 56])
        self.assertLessEqual(sum(waits), issue_status._RETRY_MAX_TOTAL_WAIT)

    def test_fetch_abandons_immediately_on_long_rate_limit_reset(self):
        issue_status._STOP_EVENT.clear()
        headers = {'X-RateLimit-Remaining': '0', 'X-RateLimit-Reset': str(time.time() + 3000)}
        with patch.object(issue_status.urllib.request, 'urlopen',
                          side_effect=self._http_error(403, headers)) as urlopen, \
             patch.object(issue_status._STOP_EVENT, 'wait') as wait:
            with self.assertRaises(issue_status.RetryExhausted):
                issue_status._fetch_with_retry('https://x')
        self.assertEqual(urlopen.call_count, 1)
        wait.assert_not_called()

    def test_fetch_does_not_retry_404(self):
        issue_status._STOP_EVENT.clear()
        with patch.object(issue_status.urllib.request, 'urlopen',
                          side_effect=self._http_error(404)) as urlopen:
            with self.assertRaises(urllib.error.HTTPError):
                issue_status._fetch_with_retry('https://x')
        self.assertEqual(urlopen.call_count, 1)

    def test_fetch_raises_aborted_when_stop_event_set(self):
        issue_status._STOP_EVENT.set()
        try:
            with patch.object(issue_status.urllib.request, 'urlopen') as urlopen:
                with self.assertRaises(issue_status.FetchAborted):
                    issue_status._fetch_with_retry('https://x')
            urlopen.assert_not_called()
        finally:
            issue_status._STOP_EVENT.clear()

    # ------------------------------------------------------------------
    # --max-fixed early stop
    # ------------------------------------------------------------------

    def test_max_fixed_returns_promptly_while_worker_is_blocked(self):
        """A worker stuck in a long backoff must not delay return once the limit is hit."""
        release = threading.Event()
        blocked_started = threading.Event()

        class FakeDispatcher:
            def dispatch(self, url):
                if url.endswith('/1'):
                    # Ensure the other worker is running (not merely queued) first.
                    blocked_started.wait(5)
                    return issue_status.Status.CLOSED, 'CLOSED: Fixed. Action: Unexclude'
                # Simulates a worker asleep in retry backoff; woken by _STOP_EVENT.
                blocked_started.set()
                issue_status._STOP_EVENT.wait(30)
                release.set()
                raise issue_status.FetchAborted(url)

        issues = [
            {'ISSUE_TRACKER': f'https://github.com/adoptium/aqa-tests/issues/{n}'}
            for n in (1, 2)
        ]
        start = time.monotonic()
        result = issue_status.fetch_all_statuses(
            issues, FakeDispatcher(), max_workers=2, max_fixed=1)
        elapsed = time.monotonic() - start

        self.assertLess(elapsed, 5)
        self.assertEqual(len(result), 1)
        # The blocked worker is woken by the stop event rather than left sleeping.
        self.assertTrue(release.wait(5))

    # ------------------------------------------------------------------
    # Skipped URLs (retries exhausted) are non-fatal
    # ------------------------------------------------------------------

    def test_get_resp_body_converts_retry_exhausted_to_skipped(self):
        handler = GitHubHandler()
        with patch.object(issue_status, '_fetch_with_retry',
                          side_effect=issue_status.RetryExhausted('https://x', None)):
            with self.assertRaises(issue_status.SkippedUrlException):
                handler.get_resp_body('https://api.github.com/repos/a/b/issues/1')

    def test_skipped_url_is_non_fatal_and_reported(self):
        class FakeDispatcher:
            def dispatch(self, url):
                if url.endswith('/1'):
                    raise issue_status.SkippedUrlException('rate limited')
                return issue_status.Status.CLOSED, 'CLOSED: Fixed. Action: Unexclude'

        issues = [
            {'ISSUE_TRACKER': f'https://github.com/adoptium/aqa-tests/issues/{n}'}
            for n in (1, 2)
        ]
        issue_status.return_code = 0
        with self.assertLogs(level='WARNING') as logs:
            result = issue_status.fetch_all_statuses(issues, FakeDispatcher(), max_workers=1)

        self.assertEqual(issue_status.return_code, 0)
        self.assertEqual(len(result), 1)
        self.assertEqual(issue_status.skipped_urls, ['https://github.com/adoptium/aqa-tests/issues/1'])
        self.assertTrue(any('1 issue tracker URL(s) were skipped' in m for m in logs.output))

    def test_real_handler_error_is_still_fatal(self):
        class FakeDispatcher:
            def dispatch(self, url):
                raise issue_status.HandlerException('HTTP 500 fetching')

        issues = [{'ISSUE_TRACKER': 'https://github.com/adoptium/aqa-tests/issues/1'}]
        issue_status.return_code = 0
        with self.assertLogs(level='ERROR'):
            issue_status.fetch_all_statuses(issues, FakeDispatcher(), max_workers=1)
        self.assertEqual(issue_status.return_code, 1)
        issue_status.return_code = 0

    # ------------------------------------------------------------------
    # Random selection with --max-fixed
    # ------------------------------------------------------------------

    def _submission_order(self, max_fixed):
        order = []

        class RecordingDispatcher:
            def dispatch(self, url):
                order.append(url)
                return issue_status.Status.OPEN, 'OPEN'

        issues = [
            {'ISSUE_TRACKER': f'https://github.com/adoptium/aqa-tests/issues/{n}'}
            for n in range(1, 31)
        ]
        issue_status.fetch_all_statuses(issues, RecordingDispatcher(), max_workers=1,
                                        max_fixed=max_fixed)
        return order

    def test_max_fixed_processes_urls_in_random_order(self):
        natural = [f'https://github.com/adoptium/aqa-tests/issues/{n}' for n in range(1, 31)]
        orders = [self._submission_order(max_fixed=5) for _ in range(3)]
        for order in orders:
            self.assertEqual(sorted(order), sorted(natural))
        # 30! orderings: the chance of three runs all matching file order is nil.
        self.assertTrue(any(order != natural for order in orders))
        self.assertTrue(any(a != b for a, b in zip(orders, orders[1:])))

    def test_no_max_fixed_keeps_file_order(self):
        natural = [f'https://github.com/adoptium/aqa-tests/issues/{n}' for n in range(1, 31)]
        self.assertEqual(self._submission_order(max_fixed=0), natural)
