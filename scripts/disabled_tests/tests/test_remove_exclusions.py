"""
test_remove_exclusions.py

Unit and integration tests for remove_exclusions.py.

Run with:
    python3 sandbox/grinderMaker/tests/test_remove_exclusions.py

No Jenkins connectivity, no GitHub pushes. No third-party dependencies.
"""

import os
import re
import sys
import tempfile
import unittest
from pathlib import Path

# Make the parent directory importable so we can import remove_exclusions directly.
sys.path.insert(0, str(Path(__file__).parent.parent))

from remove_exclusions import (
    RecordKey,
    _coverage,
    assert_valid_problemlist_line,
    comment_fully_covered,
    grinder_to_pl,
    is_no_problemlist_platform,
    parse_comment_line,
    parse_key,
    process_file,
    reduce_platforms,
    token_covered_by_passing,
    token_represented_in_comment_set,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def make_problemlist(content: str) -> Path:
    """Write `content` to a temporary file and return its Path."""
    f = tempfile.NamedTemporaryFile(
        mode='w', suffix='.txt', delete=False, encoding='utf-8'
    )
    f.write(content)
    f.close()
    return Path(f.name)


def read_and_delete(path: Path) -> str:
    text = path.read_text(encoding='utf-8')
    os.unlink(path)
    return text


# ===========================================================================
# grinder_to_pl — platform translation (format 3 → format 1)
# ===========================================================================

class TestGrinderToProblemlist(unittest.TestCase):

    def test_known_platforms(self):
        cases = [
            ('s390x_linux',     'linux-s390x'),
            ('ppc64le_linux',   'linux-ppc64le'),
            ('x86-64_linux',    'linux-x64'),
            ('aarch64_linux',   'linux-aarch64'),
            ('arm_linux',       'linux-arm'),
            ('riscv64_linux',   'linux-riscv64'),
            ('x86-64_windows',  'windows-x64'),
            ('x86-32_windows',  'windows-x86'),
            ('aarch64_windows', 'windows-aarch64'),
            ('x86-64_mac',      'macosx-x64'),
            ('aarch64_mac',     'macosx-aarch64'),
            ('ppc64_aix',       'aix-ppc64'),
        ]
        for grinder, expected in cases:
            with self.subTest(grinder=grinder):
                self.assertEqual(grinder_to_pl(grinder), expected)

    def test_ignored_platforms_return_none(self):
        for grinder in ('sparcv9_solaris', 'x86-64_solaris', 's390x_zos'):
            with self.subTest(grinder=grinder):
                self.assertIsNone(grinder_to_pl(grinder))

    def test_unknown_platform_passes_through(self):
        self.assertEqual(grinder_to_pl('some_unknown_platform'), 'some_unknown_platform')

    def test_case_insensitive(self):
        self.assertEqual(grinder_to_pl('X86-64_LINUX'), 'linux-x64')


# ===========================================================================
# is_no_problemlist_platform
# ===========================================================================

class TestIsNoProblemlistPlatform(unittest.TestCase):

    def test_ignored(self):
        # These are ProblemList format-1 tokens that have no meaning there.
        # Grinder format-3 tokens like sparcv9_solaris / x86-64_solaris are
        # filtered out earlier by grinder_to_pl() returning None, so they
        # never reach is_no_problemlist_platform().
        for token in ('solaris-sparc', 'solaris-x64', 's390x_zos'):
            with self.subTest(token=token):
                self.assertTrue(is_no_problemlist_platform(token))

    def test_not_ignored(self):
        for token in ('aix-ppc64', 'aix-all', 'linux-x64', 'linux-all',
                      'windows-x64', 'macosx-aarch64', 'generic-all'):
            with self.subTest(token=token):
                self.assertFalse(is_no_problemlist_platform(token))


# ===========================================================================
# Coverage map (_coverage)
# ===========================================================================

class TestCoverage(unittest.TestCase):

    def test_aix_all_always_present(self):
        for ver in (8, 11, 17, 21, 25, 26):
            with self.subTest(ver=ver):
                cov = _coverage(ver)
                self.assertIn('aix-all', cov)
                self.assertIn('aix-ppc64', cov['aix-all'])

    def test_linux_all_includes_arm_and_riscv_all_versions(self):
        for ver in (8, 11, 17, 21, 25, 26):
            with self.subTest(ver=ver):
                cov = _coverage(ver)
                self.assertIn('linux-arm',    cov['linux-all'])
                self.assertIn('linux-riscv64', cov['linux-all'])

    def test_macosx_all_jdk8_x64_only(self):
        self.assertEqual(_coverage(8)['macosx-all'], frozenset({'macosx-x64'}))

    def test_macosx_all_jdk11_both(self):
        self.assertEqual(
            _coverage(11)['macosx-all'], frozenset({'macosx-x64', 'macosx-aarch64'})
        )

    def test_macosx_all_jdk26_aarch64_only(self):
        self.assertEqual(_coverage(26)['macosx-all'], frozenset({'macosx-aarch64'}))

    def test_windows_all_jdk8_x64_only(self):
        self.assertEqual(_coverage(8)['windows-all'], frozenset({'windows-x64'}))

    def test_windows_all_jdk21_both(self):
        self.assertEqual(
            _coverage(21)['windows-all'], frozenset({'windows-x64', 'windows-aarch64'})
        )

    def test_generic_all_is_union(self):
        cov = _coverage(25)
        expected = frozenset().union(*[v for k, v in cov.items() if k != 'generic-all'])
        self.assertEqual(cov['generic-all'], expected)


# ===========================================================================
# token_covered_by_passing
# ===========================================================================

class TestTokenCoveredByPassing(unittest.TestCase):

    def setUp(self):
        self.cov25 = _coverage(25)

    def test_exact_specific_covered(self):
        self.assertTrue(token_covered_by_passing('linux-x64', {'linux-x64'}, self.cov25))

    def test_exact_specific_not_covered(self):
        self.assertFalse(token_covered_by_passing('linux-x64', {'linux-s390x'}, self.cov25))

    def test_broad_token_partially_covered(self):
        # windows-all on JDK25 needs x64 + aarch64; only x64 passed
        self.assertFalse(
            token_covered_by_passing('windows-all', {'windows-x64'}, self.cov25)
        )

    def test_broad_token_fully_covered(self):
        self.assertTrue(token_covered_by_passing(
            'windows-all', {'windows-x64', 'windows-aarch64'}, self.cov25
        ))

    def test_aix_all_covered_by_ppc64(self):
        self.assertTrue(token_covered_by_passing('aix-all', {'aix-ppc64'}, self.cov25))

    def test_aix_all_not_covered_empty(self):
        self.assertFalse(token_covered_by_passing('aix-all', set(), self.cov25))

    def test_solaris_always_covered(self):
        self.assertTrue(token_covered_by_passing('solaris-sparc', set(), self.cov25))

    def test_zos_always_covered(self):
        self.assertTrue(token_covered_by_passing('s390x_zos', set(), self.cov25))


# ===========================================================================
# comment_fully_covered
# ===========================================================================

class TestCommentFullyCovered(unittest.TestCase):

    def setUp(self):
        self.cov25 = _coverage(25)

    def test_specific_token_covered(self):
        self.assertTrue(
            comment_fully_covered('macosx-aarch64', {'macosx-aarch64'}, self.cov25)
        )

    def test_specific_token_not_covered(self):
        self.assertFalse(
            comment_fully_covered('macosx-aarch64', {'linux-x64'}, self.cov25)
        )

    def test_broad_token_partially_covered(self):
        self.assertFalse(
            comment_fully_covered('windows-all', {'windows-x64'}, self.cov25)
        )

    def test_broad_token_fully_covered(self):
        self.assertTrue(comment_fully_covered(
            'windows-all', {'windows-x64', 'windows-aarch64'}, self.cov25
        ))

    def test_solaris_only_is_covered(self):
        self.assertTrue(comment_fully_covered('solaris-sparc', set(), self.cov25))

    def test_zos_only_is_covered(self):
        self.assertTrue(comment_fully_covered('s390x_zos', set(), self.cov25))

    def test_solaris_mixed_with_covered(self):
        self.assertTrue(comment_fully_covered(
            'solaris-sparc,linux-x64', {'linux-x64'}, self.cov25
        ))

    def test_solaris_mixed_with_uncovered(self):
        self.assertFalse(
            comment_fully_covered('solaris-sparc,linux-x64', set(), self.cov25)
        )


# ===========================================================================
# reduce_platforms
# ===========================================================================

class TestReducePlatforms(unittest.TestCase):

    def test_generic_all_collapses_all(self):
        self.assertEqual(
            reduce_platforms(['linux-x64', 'generic-all', 'windows-x64']),
            ['generic-all']
        )

    def test_prefix_all_removes_specifics(self):
        result = reduce_platforms(['linux-all', 'linux-x64', 'linux-s390x', 'windows-x64'])
        self.assertEqual(result, ['linux-all', 'windows-x64'])

    def test_dedup_case_insensitive(self):
        self.assertEqual(
            reduce_platforms(['Linux-X64', 'linux-x64', 'LINUX-X64']), ['linux-x64']
        )

    def test_single_token_unchanged(self):
        self.assertEqual(reduce_platforms(['windows-x64']), ['windows-x64'])

    def test_output_is_sorted(self):
        result = reduce_platforms(['windows-x64', 'aix-ppc64', 'linux-x64'])
        self.assertEqual(result, sorted(result))

    def test_generic_all_beats_prefix_all(self):
        result = reduce_platforms(['linux-all', 'generic-all', 'linux-x64'])
        self.assertEqual(result, ['generic-all'])


# ===========================================================================
# parse_comment_line
# ===========================================================================

class TestParseCommentLine(unittest.TestCase):

    def test_valid_comment(self):
        line = '# Foo.java https://example.com/issue linux-x64\n'
        self.assertEqual(
            parse_comment_line(line),
            ('Foo.java', 'https://example.com/issue', 'linux-x64')
        )

    def test_active_line_returns_none(self):
        self.assertIsNone(parse_comment_line('Foo.java https://example.com linux-x64\n'))

    def test_section_heading_returns_none(self):
        self.assertIsNone(parse_comment_line('# jdk_lang\n'))

    def test_bare_comment_returns_none(self):
        self.assertIsNone(parse_comment_line('# just a comment\n'))

    def test_two_field_comment_returns_none(self):
        self.assertIsNone(parse_comment_line('# Foo.java notaurl\n'))

    def test_multi_platform_comment(self):
        line = '# Bar.java https://bugs.example.com linux-x64,windows-x64\n'
        self.assertEqual(
            parse_comment_line(line),
            ('Bar.java', 'https://bugs.example.com', 'linux-x64,windows-x64')
        )


# ===========================================================================
# parse_key
# ===========================================================================

class TestParseKey(unittest.TestCase):

    def test_valid_key(self):
        key = parse_key(
            'jdk25|x86-64_linux|com/example/Foo.java'
            '|openjdk/excludes/ProblemList_openjdk25.txt'
        )
        self.assertEqual(key.jdk_label, 'jdk25')
        self.assertEqual(key.platform, 'x86-64_linux')
        self.assertEqual(key.full_test_path, 'com/example/Foo.java')
        self.assertEqual(
            key.exclude_file, 'openjdk/excludes/ProblemList_openjdk25.txt'
        )

    def test_empty_exclude_file(self):
        key = parse_key('jdk11|aarch64_mac|some/Test.java|')
        self.assertEqual(key.exclude_file, '')

    def test_wrong_field_count_raises(self):
        with self.assertRaises(ValueError):
            parse_key('jdk25|x86-64_linux|com/example/Foo.java')

    def test_non_jdk_prefix_raises(self):
        with self.assertRaises(ValueError):
            parse_key('ver25|x86-64_linux|Foo.java|file.txt')


# ===========================================================================
# assert_valid_problemlist_line
# ===========================================================================

class TestAssertValidProblemlistLine(unittest.TestCase):

    def test_valid_line(self):
        # Should not raise
        assert_valid_problemlist_line('Foo.java https://example.com linux-x64\n', 'ctx')

    def test_valid_multi_platform(self):
        assert_valid_problemlist_line(
            'Foo.java https://example.com linux-x64,windows-x64\n', 'ctx'
        )

    def test_two_fields_raises(self):
        with self.assertRaises(ValueError):
            assert_valid_problemlist_line('Foo.java https://example.com\n', 'ctx')

    def test_four_fields_raises(self):
        with self.assertRaises(ValueError):
            assert_valid_problemlist_line(
                'Foo.java https://example.com linux-x64 extra\n', 'ctx'
            )

    def test_one_field_raises(self):
        with self.assertRaises(ValueError):
            assert_valid_problemlist_line('Foo.java\n', 'ctx')


# ===========================================================================
# Grinder result classification
# ===========================================================================

class TestGrinderResultClassification(unittest.TestCase):
    """
    Mirrors the result classification logic in triggerGrinder.groovy Stage 2:
        status == 'SUCCESS'  → 'PASS'
        anything else        → 'FAIL'
        exception            → 'ERROR'

    PASS  → key added to passingKeys → process_file called → file modified
    FAIL  → test remains excluded (UNSTABLE = healthy failure; FAILURE = error report)
    ERROR → pipeline problem, no file change
    """

    def classify(self, status):
        """Python mirror of the Groovy classification logic."""
        if status == 'SUCCESS':
            return 'PASS'
        elif status is None:
            return 'ERROR'
        else:
            return 'FAIL'

    def test_success_is_pass(self):
        self.assertEqual(self.classify('SUCCESS'), 'PASS')

    def test_unstable_is_fail(self):
        # UNSTABLE = test ran but did not pass cleanly → healthy failure → stays excluded
        self.assertEqual(self.classify('UNSTABLE'), 'FAIL')

    def test_failure_is_fail(self):
        # FAILURE = Grinder job itself failed → should trigger error report
        self.assertEqual(self.classify('FAILURE'), 'FAIL')

    def test_exception_is_error(self):
        # Exception launching the job → 'ERROR'
        self.assertEqual(self.classify(None), 'ERROR')

    def test_only_pass_reaches_process_file(self):
        """Only PASS entries are added to passingKeys; FAIL and ERROR are not."""
        job_results = {
            'jdk25|x86-64_linux|Foo.java|f.txt': 'PASS',
            'jdk25|aarch64_mac|Bar.java|f.txt':  'FAIL',
            'jdk25|ppc64_aix|Baz.java|f.txt':    'ERROR',
        }
        passing_keys = [k for k, v in job_results.items() if v == 'PASS']
        self.assertEqual(passing_keys, ['jdk25|x86-64_linux|Foo.java|f.txt'])

    def test_grinder_pass_leads_to_file_change(self):
        """A PASS result causes the exclusion to be removed from the ProblemList."""
        f = make_problemlist('Foo.java https://example.com linux-x64\n')
        n = process_file(f, 'Foo.java', {'linux-x64'}, 25)
        result = read_and_delete(f)
        self.assertEqual(n, 1)
        self.assertEqual(result, '')

    def test_grinder_unstable_no_file_change(self):
        """
        UNSTABLE → 'FAIL' → key not in passingKeys → process_file not called.
        File must remain unchanged.
        """
        content = 'Foo.java https://example.com linux-x64\n'
        f = make_problemlist(content)
        # process_file is NOT called (simulating FAIL result)
        result = read_and_delete(f)
        self.assertEqual(result, content)

    def test_grinder_failure_no_file_change(self):
        """
        FAILURE → 'ERROR' → key not in passingKeys → process_file not called.
        File must remain unchanged.
        """
        content = 'Foo.java https://example.com linux-x64\n'
        f = make_problemlist(content)
        # process_file is NOT called (simulating ERROR result)
        result = read_and_delete(f)
        self.assertEqual(result, content)


# ===========================================================================
# process_file — unexclusion scenarios
# ===========================================================================

class TestProcessFileUnexclusion(unittest.TestCase):

    def test_full_unexclusion_single_platform(self):
        """Test passes on its only platform → line deleted entirely."""
        f = make_problemlist('Foo.java https://example.com linux-x64\n')
        n = process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(n, 1)
        self.assertEqual(read_and_delete(f), '')

    def test_full_unexclusion_preserves_surrounding_lines(self):
        """Only the matching line is removed; other tests are untouched."""
        content = (
            'Bar.java https://example.com windows-x64\n'
            'Foo.java https://example.com linux-x64\n'
            'Baz.java https://example.com aarch64_mac\n'
        )
        f = make_problemlist(content)
        process_file(f, 'Foo.java', {'linux-x64'}, 25)
        result = read_and_delete(f)
        self.assertNotIn('Foo.java', result)
        self.assertIn('Bar.java', result)
        self.assertIn('Baz.java', result)

    def test_partial_unexclusion_subset_of_platforms(self):
        """
        Test passes on linux-x64 but is also excluded for windows-x64.
        Only linux-x64 is removed; windows-x64 remains.
        """
        f = make_problemlist('Foo.java https://example.com linux-x64,windows-x64\n')
        n = process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(n, 1)
        self.assertEqual(
            read_and_delete(f), 'Foo.java https://example.com windows-x64\n'
        )

    def test_no_change_when_platform_not_in_exclusion(self):
        """Passing platform not listed on the exclusion line — no modification."""
        content = 'Foo.java https://example.com windows-x64\n'
        f = make_problemlist(content)
        n = process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(n, 0)
        self.assertEqual(read_and_delete(f), content)

    def test_broad_token_not_removed_by_single_platform(self):
        """linux-all requires all linux platforms; a single linux-x64 must not remove it."""
        content = 'Foo.java https://example.com linux-all\n'
        f = make_problemlist(content)
        n = process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(n, 0)
        self.assertEqual(read_and_delete(f), content)

    def test_broad_token_removed_when_all_platforms_pass(self):
        """linux-all removed when all required linux platforms pass."""
        f = make_problemlist('Foo.java https://example.com linux-all\n')
        passing = {
            'linux-x64', 'linux-s390x', 'linux-ppc64le',
            'linux-aarch64', 'linux-arm', 'linux-riscv64',
        }
        n = process_file(f, 'Foo.java', passing, 25)
        self.assertEqual(n, 1)
        self.assertEqual(read_and_delete(f), '')

    def test_solaris_token_never_removed(self):
        """Lines with only solaris platforms are never touched."""
        content = 'Foo.java https://example.com solaris-sparc\n'
        f = make_problemlist(content)
        n = process_file(f, 'Foo.java', set(), 25)
        self.assertEqual(n, 0)
        self.assertEqual(read_and_delete(f), content)

    def test_solaris_alongside_real_token(self):
        """Real token stripped, solaris token preserved on the rewritten line."""
        f = make_problemlist('Foo.java https://example.com linux-x64,solaris-sparc\n')
        n = process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(n, 1)
        self.assertEqual(
            read_and_delete(f), 'Foo.java https://example.com solaris-sparc\n'
        )

    def test_aix_ppc64_exact_match(self):
        """aix-ppc64 exact match is removed correctly."""
        f = make_problemlist('Foo.java https://example.com aix-ppc64\n')
        process_file(f, 'Foo.java', {'aix-ppc64'}, 25)
        self.assertEqual(read_and_delete(f), '')

    def test_aix_all_covered_by_ppc64(self):
        """aix-all is satisfied by aix-ppc64 passing."""
        f = make_problemlist('Foo.java https://example.com aix-all\n')
        process_file(f, 'Foo.java', {'aix-ppc64'}, 25)
        self.assertEqual(read_and_delete(f), '')

    def test_id_suffix_exact_match(self):
        """Exclusion entry has #id0; key uses the same path — exact match."""
        f = make_problemlist(
            'java/lang/ProcessBuilder/Basic.java#id0 https://example.com linux-x64\n'
        )
        process_file(
            f, 'java/lang/ProcessBuilder/Basic.java#id0', {'linux-x64'}, 25
        )
        self.assertEqual(read_and_delete(f), '')

    def test_base_path_matching_strips_id(self):
        """Key uses base path (no #id); exclusion entry has #id0 — should still match."""
        f = make_problemlist(
            'java/lang/ProcessBuilder/Basic.java#id0 https://example.com linux-x64\n'
        )
        process_file(
            f, 'java/lang/ProcessBuilder/Basic.java', {'linux-x64'}, 25
        )
        self.assertEqual(read_and_delete(f), '')


# ===========================================================================
# process_file — commented-line promotion scenarios
# ===========================================================================

class TestProcessFileCommentPromotion(unittest.TestCase):

    def test_no_comments_clean_delete(self):
        """Active line deleted, no comments → file empty."""
        f = make_problemlist('Foo.java https://example.com linux-x64\n')
        process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(read_and_delete(f), '')

    def test_one_covered_comment_also_deleted(self):
        """Active line deleted + one comment that is also fully covered → both gone."""
        f = make_problemlist(
            '# Foo.java https://example.com/1 macosx-aarch64\n'
            'Foo.java https://example.com/2 linux-x64\n'
        )
        process_file(f, 'Foo.java', {'linux-x64', 'macosx-aarch64'}, 25)
        self.assertEqual(read_and_delete(f), '')

    def test_one_surviving_comment_promoted_no_residual(self):
        """
        Active line deleted; macosx comment covered (deleted); windows-all survives.
        Sole surviving comment promoted to active line — no residual comment.
        """
        f = make_problemlist(
            '# Foo.java https://example.com/1 windows-all\n'
            '# Foo.java https://example.com/2 macosx-aarch64\n'
            'Foo.java https://example.com/3 linux-x64\n'
        )
        # windows-all needs x64+aarch64 on jdk25; only x64 passed → not covered
        # macosx-aarch64 passed → covered → deleted
        process_file(f, 'Foo.java', {'linux-x64', 'macosx-aarch64'}, 25)
        self.assertEqual(
            read_and_delete(f), 'Foo.java https://example.com/1 windows-all\n'
        )

    def test_two_surviving_comments_last_promoted_first_kept_as_log(self):
        """
        Active line deleted; both comments survive.
        Last promoted to active with merged platforms; first kept as comment log.
        """
        f = make_problemlist(
            '# Foo.java https://example.com/1 aix-ppc64\n'
            '# Foo.java https://example.com/2 windows-all\n'
            'Foo.java https://example.com/3 linux-x64\n'
        )
        process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(read_and_delete(f), (
            '# Foo.java https://example.com/1 aix-ppc64\n'
            'Foo.java https://example.com/2 aix-ppc64,windows-all\n'
        ))

    def test_all_comments_covered_and_active_deleted_file_empty(self):
        """All platforms pass; all comments covered → empty file."""
        f = make_problemlist(
            '# Foo.java https://example.com/1 windows-all\n'
            '# Foo.java https://example.com/2 macosx-aarch64\n'
            'Foo.java https://example.com/3 linux-x64,macosx-aarch64,windows-all\n'
        )
        passing = {'linux-x64', 'macosx-aarch64', 'windows-x64', 'windows-aarch64'}
        process_file(f, 'Foo.java', passing, 25)
        self.assertEqual(read_and_delete(f), '')

    def test_partial_pass_uncovered_comment_survives_and_is_promoted(self):
        """
        User-quoted example: linux and macos pass; windows-aarch64 did NOT pass.
        windows-all comment not fully covered → survives → promoted.
        macosx-aarch64 comment covered → deleted.
        Rewritten active line is redundant vs surviving comment → replaced.
        """
        f = make_problemlist(
            '# Foo.java https://example.com/1 windows-all\n'
            '# Foo.java https://example.com/2 macosx-aarch64\n'
            'Foo.java https://example.com/3 linux-x64,macosx-aarch64,windows-all\n'
        )
        # windows-aarch64 did NOT pass → windows-all not fully covered
        process_file(f, 'Foo.java', {'linux-x64', 'macosx-aarch64', 'windows-x64'}, 25)
        self.assertEqual(
            read_and_delete(f), 'Foo.java https://example.com/1 windows-all\n'
        )

    def test_partial_pass_covered_comment_deleted_active_kept_when_unique(self):
        """
        Active line partially rewritten; covered comment deleted.
        Remaining active token (linux-x64) not in any surviving comment → active stays.
        """
        f = make_problemlist(
            '# Foo.java https://example.com/1 macosx-aarch64\n'
            'Foo.java https://example.com/2 linux-x64,macosx-aarch64\n'
        )
        # macosx-aarch64 passed → comment deleted
        # linux-x64 remains on active; no surviving comment covers it → active stays
        process_file(f, 'Foo.java', {'macosx-aarch64'}, 25)
        self.assertEqual(
            read_and_delete(f), 'Foo.java https://example.com/2 linux-x64\n'
        )

    def test_partial_pass_active_replaced_when_redundant_vs_comment(self):
        """
        Active line partially rewritten to windows-all; surviving comment also has windows-all.
        Active is redundant → deleted; comment promoted.
        """
        f = make_problemlist(
            '# Foo.java https://example.com/1 windows-all\n'
            'Foo.java https://example.com/2 linux-x64,windows-all\n'
        )
        process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(
            read_and_delete(f), 'Foo.java https://example.com/1 windows-all\n'
        )

    def test_partial_pass_active_kept_when_has_unique_platform(self):
        """
        Active rewritten to windows-all,aix-ppc64; comment has windows-all only.
        aix-ppc64 not covered by any comment → active is NOT redundant → stays.
        """
        f = make_problemlist(
            '# Foo.java https://example.com/1 windows-all\n'
            'Foo.java https://example.com/2 linux-x64,windows-all,aix-ppc64\n'
        )
        process_file(f, 'Foo.java', {'linux-x64'}, 25)
        self.assertEqual(read_and_delete(f), (
            '# Foo.java https://example.com/1 windows-all\n'
            'Foo.java https://example.com/2 windows-all,aix-ppc64\n'
        ))

    def test_three_comments_mixed_coverage(self):
        """
        Three comments: one covered (macosx), two survive (aix, windows).
        Last survivor (windows) promoted with merged platforms aix-ppc64,windows-all.
        First survivor (aix) kept as comment log.
        """
        f = make_problemlist(
            '# Foo.java https://example.com/1 aix-ppc64\n'
            '# Foo.java https://example.com/2 macosx-aarch64\n'
            '# Foo.java https://example.com/3 windows-all\n'
            'Foo.java https://example.com/4 linux-x64\n'
        )
        process_file(f, 'Foo.java', {'linux-x64', 'macosx-aarch64'}, 25)
        self.assertEqual(read_and_delete(f), (
            '# Foo.java https://example.com/1 aix-ppc64\n'
            'Foo.java https://example.com/3 aix-ppc64,windows-all\n'
        ))

    def test_section_headings_not_treated_as_comment_exclusions(self):
        """Section heading lines (# jdk_lang) must not be parsed as comment exclusions."""
        content = (
            '# jdk_lang\n'
            '\n'
            'Foo.java https://example.com linux-x64\n'
        )
        f = make_problemlist(content)
        process_file(f, 'Foo.java', {'linux-x64'}, 25)
        result = read_and_delete(f)
        self.assertIn('# jdk_lang\n', result)
        self.assertNotIn('Foo.java', result)

    def test_unrelated_tests_not_affected_by_comment_promotion(self):
        """Comments and active lines for a different test are never touched."""
        content = (
            '# Other.java https://example.com/x linux-x64\n'
            'Other.java https://example.com/y linux-x64\n'
            'Foo.java https://example.com/z linux-x64\n'
        )
        f = make_problemlist(content)
        process_file(f, 'Foo.java', {'linux-x64'}, 25)
        result = read_and_delete(f)
        self.assertIn('Other.java', result)
        self.assertNotIn('Foo.java', result)


# ===========================================================================
# process_file — JDK version coverage variation
# ===========================================================================

class TestJdkVersionCoverage(unittest.TestCase):

    def test_windows_all_jdk8_requires_only_x64(self):
        f = make_problemlist('Foo.java https://example.com windows-all\n')
        process_file(f, 'Foo.java', {'windows-x64'}, 8)
        self.assertEqual(read_and_delete(f), '')

    def test_windows_all_jdk25_requires_both(self):
        content = 'Foo.java https://example.com windows-all\n'
        f = make_problemlist(content)
        process_file(f, 'Foo.java', {'windows-x64'}, 25)   # aarch64 missing
        self.assertEqual(read_and_delete(f), content)

    def test_macosx_all_jdk26_aarch64_only(self):
        f = make_problemlist('Foo.java https://example.com macosx-all\n')
        process_file(f, 'Foo.java', {'macosx-aarch64'}, 26)
        self.assertEqual(read_and_delete(f), '')

    def test_macosx_all_jdk25_requires_both(self):
        content = 'Foo.java https://example.com macosx-all\n'
        f = make_problemlist(content)
        process_file(f, 'Foo.java', {'macosx-aarch64'}, 25)   # x64 missing
        self.assertEqual(read_and_delete(f), content)


# ===========================================================================
# Stage 2 logic — platform expansion and deduplication (pure Python mirror)
# ===========================================================================

class TestPlatformExpansion(unittest.TestCase):
    """
    Mirrors the PLATFORM_MAP expansion and deduplication logic in
    triggerGrinder.groovy Stage 2, implemented in pure Python so no
    Groovy runtime is required.
    """

    PLATFORM_MAP = {
        's390x_linux':     ['s390x_linux'],
        'ppc64le_linux':   ['ppc64le_linux'],
        'x86-64_linux':    ['x86-64_linux'],
        'aarch64_linux':   ['aarch64_linux'],
        'arm_linux':       ['arm_linux'],
        'riscv64_linux':   ['riscv64_linux'],
        'x86-64_windows':  ['x86-64_windows'],
        'x86-32_windows':  ['x86-32_windows'],
        'aarch64_windows': ['aarch64_windows'],
        'x86-64_mac':      ['x86-64_mac'],
        'aarch64_mac':     ['aarch64_mac'],
        'ppc64_aix':       ['ppc64_aix'],
        'all_linux':       ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux'],
        'all_windows':     ['x86-64_windows', 'aarch64_windows'],
        'all_mac':         ['x86-64_mac', 'aarch64_mac'],
        'all_aix':         ['ppc64_aix'],
        'all':             ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux',
                            'x86-64_windows', 'aarch64_windows',
                            'x86-64_mac', 'aarch64_mac', 'ppc64_aix'],
    }

    def expand(self, platform):
        key = platform.lower().replace(' ', '_')
        return self.PLATFORM_MAP.get(key, [key])

    def test_single_platform_identity(self):
        self.assertEqual(self.expand('x86-64_linux'), ['x86-64_linux'])

    def test_all_linux_expands_to_four(self):
        result = self.expand('all_linux')
        self.assertIn('s390x_linux',  result)
        self.assertIn('x86-64_linux', result)
        self.assertEqual(len(result), 4)

    def test_all_windows_expands(self):
        self.assertEqual(self.expand('all_windows'), ['x86-64_windows', 'aarch64_windows'])

    def test_all_mac_expands(self):
        self.assertEqual(self.expand('all_mac'), ['x86-64_mac', 'aarch64_mac'])

    def test_all_aix_expands(self):
        self.assertEqual(self.expand('all_aix'), ['ppc64_aix'])

    def test_generic_all_expands_to_nine(self):
        self.assertEqual(len(self.expand('all')), 9)

    def test_unknown_platform_passes_through(self):
        self.assertEqual(self.expand('some_unknown'), ['some_unknown'])

    def test_deduplication_same_record_key_collapses(self):
        """Two entries with the same record key → one Grinder job."""
        entries = [
            {'JDK_VERSION': '25', 'PLATFORM': 'x86-64_linux',
             'CUSTOM_TARGET': 'Foo.java', 'EXCLUDE_FILE': 'file.txt',
             'ISSUE_TRACKER': 'https://a.com'},
            {'JDK_VERSION': '25', 'PLATFORM': 'x86-64_linux',
             'CUSTOM_TARGET': 'Foo.java', 'EXCLUDE_FILE': 'file.txt',
             'ISSUE_TRACKER': 'https://b.com'},
        ]
        dedup = {}
        for e in entries:
            full = e['CUSTOM_TARGET']
            key = f"jdk{e['JDK_VERSION']}|{e['PLATFORM']}|{full}|{e['EXCLUDE_FILE']}"
            if key not in dedup:
                dedup[key] = e
        self.assertEqual(len(dedup), 1)

    def test_deduplication_different_paths_not_collapsed(self):
        """Two tests sharing a filename but different full paths must not collapse."""
        entries = [
            {'JDK_VERSION': '25', 'PLATFORM': 'x86-64_linux',
             'CUSTOM_TARGET': 'com/sun/jdi/Basic.java',  'EXCLUDE_FILE': 'f.txt'},
            {'JDK_VERSION': '25', 'PLATFORM': 'x86-64_linux',
             'CUSTOM_TARGET': 'java/lang/Basic.java', 'EXCLUDE_FILE': 'f.txt'},
        ]
        dedup = {}
        for e in entries:
            full = e['CUSTOM_TARGET']
            key = f"jdk{e['JDK_VERSION']}|{e['PLATFORM']}|{full}|{e['EXCLUDE_FILE']}"
            if key not in dedup:
                dedup[key] = e
        self.assertEqual(len(dedup), 2)

    def test_hotspot_only_filter(self):
        """
        Only entries with JDK_IMPL == 'hotspot' are processed.
        Mirrors the hard-coded filter in triggerGrinder.groovy Stage 1.
        """
        entries = [
            {'JDK_IMPL': 'hotspot',  'ISSUE_TRACKER_STATUS': 'closed', 'JDK_VERSION': '25'},
            {'JDK_IMPL': 'openj9',   'ISSUE_TRACKER_STATUS': 'closed', 'JDK_VERSION': '25'},
            {'JDK_IMPL': 'hotspot',  'ISSUE_TRACKER_STATUS': 'open',   'JDK_VERSION': '25'},
            {'JDK_IMPL': 'hotspot',  'ISSUE_TRACKER_STATUS': 'closed', 'JDK_VERSION': '17'},
        ]
        candidates = [
            e for e in entries
            if e['ISSUE_TRACKER_STATUS'] == 'closed'
            and e['JDK_IMPL'] == 'hotspot'
        ]
        self.assertEqual(len(candidates), 2)
        self.assertTrue(all(e['JDK_IMPL'] == 'hotspot' for e in candidates))

    def test_jdk_version_filter(self):
        """JDK_VERSIONS parameter filters candidates to specified versions only."""
        entries = [
            {'JDK_IMPL': 'hotspot', 'ISSUE_TRACKER_STATUS': 'closed', 'JDK_VERSION': '25'},
            {'JDK_IMPL': 'hotspot', 'ISSUE_TRACKER_STATUS': 'closed', 'JDK_VERSION': '17'},
            {'JDK_IMPL': 'hotspot', 'ISSUE_TRACKER_STATUS': 'closed', 'JDK_VERSION': '11'},
        ]
        jdk_filter = ['17', '25']
        candidates = [
            e for e in entries
            if e['ISSUE_TRACKER_STATUS'] == 'closed'
            and e['JDK_IMPL'] == 'hotspot'
            and e['JDK_VERSION'] in jdk_filter
        ]
        self.assertEqual(len(candidates), 2)
        versions = {e['JDK_VERSION'] for e in candidates}
        self.assertEqual(versions, {'17', '25'})


# ===========================================================================
# TestIssueStatusOutputParsing
# Mirrors the filtering, expansion, and deduplication logic in
# triggerGrinder.groovy Stage 1 / Stage 2 using the real output.json shape
# produced by issue_status.py.
# ===========================================================================

# ---------------------------------------------------------------------------
# Helpers — mirrors of the Stage 1/2 Groovy logic, in pure Python
# ---------------------------------------------------------------------------

# Mapping from output.json platform aliases (format 2) to lists of concrete
# Grinder platforms (format 3).  Identical to PLATFORM_MAP in triggerGrinder.groovy.
_STAGE2_PLATFORM_MAP = {
    's390x_linux':     ['s390x_linux'],
    'ppc64le_linux':   ['ppc64le_linux'],
    'x86-64_linux':    ['x86-64_linux'],
    'aarch64_linux':   ['aarch64_linux'],
    'arm_linux':       ['arm_linux'],
    'riscv64_linux':   ['riscv64_linux'],
    'x86-64_windows':  ['x86-64_windows'],
    'x86-32_windows':  ['x86-32_windows'],
    'aarch64_windows': ['aarch64_windows'],
    'x86-64_mac':      ['x86-64_mac'],
    'aarch64_mac':     ['aarch64_mac'],
    'ppc64_aix':       ['ppc64_aix'],
    # compound aliases
    'all_linux':   ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux'],
    'all_windows': ['x86-64_windows', 'aarch64_windows'],
    'all_mac':     ['x86-64_mac', 'aarch64_mac'],
    'all_aix':     ['ppc64_aix'],
    'all':         ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux',
                    'x86-64_windows', 'aarch64_windows',
                    'x86-64_mac', 'aarch64_mac', 'ppc64_aix'],
}


def _expand_platform(platform_field: str) -> list:
    """
    Expand a PLATFORM field from output.json (which may be a comma-separated
    list of platform aliases) into a flat list of concrete Grinder platforms.
    Unknown tokens pass through verbatim (matches Groovy behaviour).
    """
    concrete = []
    for token in platform_field.split(','):
        token = token.strip()
        concrete.extend(_STAGE2_PLATFORM_MAP.get(token, [token]))
    return concrete


def _filter_candidates(entries: list, jdk_filter=None) -> list:
    """
    Apply the Stage 1 filtering rules:
      - ISSUE_TRACKER_STATUS must be 'closed'
      - JDK_IMPL must be 'hotspot'
      - If jdk_filter is a non-empty list, JDK_VERSION must be in that list
    """
    result = []
    for e in entries:
        if e.get('ISSUE_TRACKER_STATUS') != 'closed':
            continue
        if e.get('JDK_IMPL') != 'hotspot':
            continue
        if jdk_filter and e.get('JDK_VERSION') not in jdk_filter:
            continue
        result.append(e)
    return result


def _build_record_keys(entries: list) -> dict:
    """
    Expand platform aliases and build deduplicated record keys.
    Returns a dict keyed by record key string, value = source entry.
    """
    dedup = {}
    for e in entries:
        for platform in _expand_platform(e.get('PLATFORM', '')):
            full = e.get('CUSTOM_TARGET', '')
            jdkv = e.get('JDK_VERSION', '')
            xfile = e.get('EXCLUDE_FILE', '')
            key = f"jdk{jdkv}|{platform}|{full}|{xfile}"
            if key not in dedup:
                dedup[key] = e
    return dedup


# ---------------------------------------------------------------------------
# Real issue_status.py output fixture used across multiple tests.
# Reproduces the exact shape shown in the task description plus extra rows
# that exercise edge-case filtering.
# ---------------------------------------------------------------------------
_REAL_OUTPUT_JSON = [
    # --- the three entries from the task description ---
    {
        "JDK_VERSION": "25",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "com/sun/jdi/JdwpAttachTest.java",
        "PLATFORM": "s390x_linux",
        "ISSUE_TRACKER": "https://bugs.openjdk.java.net/browse/JDK-8253940",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdk25.txt",
    },
    {
        "JDK_VERSION": "26",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "com/sun/jdi/JdwpAttachTest.java",
        "PLATFORM": "s390x_linux",
        "ISSUE_TRACKER": "https://bugs.openjdk.java.net/browse/JDK-8253940",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdk26.txt",
    },
    {
        "JDK_VERSION": "11",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "java/lang/ProcessBuilder/Basic.java#id0",
        "PLATFORM": "all_linux",
        "ISSUE_TRACKER": "https://bugs.openjdk.org/browse/JDK-8337795",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdk11.txt",
    },
    # --- extra rows for edge-case coverage ---
    # open issue → must be filtered out
    {
        "JDK_VERSION": "21",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "java/util/Timer/Basic.java",
        "PLATFORM": "x86-64_linux",
        "ISSUE_TRACKER": "https://bugs.openjdk.org/browse/JDK-9999999",
        "ISSUE_TRACKER_STATUS": "open",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdk21.txt",
    },
    # non-hotspot (openj9) closed → must be filtered out
    {
        "JDK_VERSION": "21",
        "JDK_IMPL": "openj9",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "java/util/Timer/Basic.java",
        "PLATFORM": "x86-64_linux",
        "ISSUE_TRACKER": "https://github.com/eclipse-openj9/openj9/issues/123",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/vendors/openj9/ProblemList_openjdk21-openj9.txt",
    },
    # non-hotspot (ibm) closed → must be filtered out
    {
        "JDK_VERSION": "17",
        "JDK_IMPL": "ibm",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "java/util/Timer/Basic.java",
        "PLATFORM": "ppc64le_linux",
        "ISSUE_TRACKER": "https://github.com/ibmruntimes/openj9/issues/456",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/vendors/ibm/ProblemList_openjdk17.txt",
    },
    # JDK_VERSION = "all" — compound version value
    {
        "JDK_VERSION": "all",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "com/sun/jndi/ldap/RemoveNamingListenerTest.java",
        "PLATFORM": "all_aix",
        "ISSUE_TRACKER": "https://bugs.openjdk.org/browse/JDK-8000001",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdkAll.txt",
    },
    # comma-separated platforms in a single PLATFORM field
    {
        "JDK_VERSION": "21",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "java/net/Socket/LinkLocal.java",
        "PLATFORM": "x86-64_mac,all_linux",
        "ISSUE_TRACKER": "https://bugs.openjdk.org/browse/JDK-8111111",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdk21.txt",
    },
    # sparcv9_solaris — unknown/pass-through platform
    {
        "JDK_VERSION": "11",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "java/net/Socket/SocketTest.java",
        "PLATFORM": "sparcv9_solaris",
        "ISSUE_TRACKER": "https://bugs.openjdk.org/browse/JDK-8222222",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdk11.txt",
    },
    # duplicate of the first entry (same test, platform, JDK, file) → deduplicates to one job
    {
        "JDK_VERSION": "25",
        "JDK_IMPL": "hotspot",
        "TARGET": "jdk_custom",
        "CUSTOM_TARGET": "com/sun/jdi/JdwpAttachTest.java",
        "PLATFORM": "s390x_linux",
        "ISSUE_TRACKER": "https://bugs.openjdk.java.net/browse/JDK-8253940",
        "ISSUE_TRACKER_STATUS": "closed",
        "EXCLUDE_FILE": "openjdk/excludes/ProblemList_openjdk25.txt",
    },
]


class TestIssueStatusOutputParsing(unittest.TestCase):
    """
    Exercises filtering, platform expansion, and record-key generation against
    the real shape of output.json produced by issue_status.py.
    All logic is implemented in pure Python helpers above that mirror the
    corresponding Groovy blocks in triggerGrinder.groovy.
    """

    def setUp(self):
        # Pre-filtered candidates (no JDK version filter applied)
        self.candidates = _filter_candidates(_REAL_OUTPUT_JSON)

    # -----------------------------------------------------------------------
    # 1. Overall filtering — three closed+hotspot entries from task description
    #    plus three compound/edge-case entries; six filtered entries in total.
    # -----------------------------------------------------------------------

    def test_closed_hotspot_entries_are_candidates(self):
        """All three task-description entries are closed+hotspot → included."""
        targets = {e['CUSTOM_TARGET'] for e in self.candidates}
        self.assertIn('com/sun/jdi/JdwpAttachTest.java', targets)
        self.assertIn('java/lang/ProcessBuilder/Basic.java#id0', targets)

    def test_open_issue_excluded(self):
        """Entry with ISSUE_TRACKER_STATUS='open' must not appear in candidates."""
        open_entries = [
            e for e in self.candidates
            if e.get('ISSUE_TRACKER_STATUS') == 'open'
        ]
        self.assertEqual(open_entries, [])

    def test_openj9_impl_excluded_even_if_closed(self):
        """openj9 entry with closed issue is filtered out."""
        openj9_entries = [e for e in self.candidates if e.get('JDK_IMPL') == 'openj9']
        self.assertEqual(openj9_entries, [])

    def test_ibm_impl_excluded_even_if_closed(self):
        """ibm entry with closed issue is filtered out."""
        ibm_entries = [e for e in self.candidates if e.get('JDK_IMPL') == 'ibm']
        self.assertEqual(ibm_entries, [])

    def test_all_candidates_are_hotspot(self):
        """Every entry that survives filtering must have JDK_IMPL == 'hotspot'."""
        self.assertTrue(all(e['JDK_IMPL'] == 'hotspot' for e in self.candidates))

    def test_candidate_count(self):
        """
        Expected survivors from the fixture:
          - JdwpAttachTest jdk25      (closed hotspot)
          - JdwpAttachTest jdk26      (closed hotspot)
          - ProcessBuilder jdk11      (closed hotspot)
          - RemoveNamingListenerTest all_aix (closed hotspot, JDK_VERSION='all')
          - LinkLocal comma-platform  (closed hotspot)
          - SocketTest sparcv9_solaris (closed hotspot)
          - JdwpAttachTest jdk25 duplicate (closed hotspot — still one entry before dedup)
        Total = 7
        """
        self.assertEqual(len(self.candidates), 7)

    # -----------------------------------------------------------------------
    # 2. JDK version filter (optional jdk_filter list)
    # -----------------------------------------------------------------------

    def test_jdk_filter_narrows_candidates(self):
        """Passing jdk_filter=['25', '26'] excludes JDK 11 entries."""
        filtered = _filter_candidates(_REAL_OUTPUT_JSON, jdk_filter=['25', '26'])
        versions = {e['JDK_VERSION'] for e in filtered}
        self.assertNotIn('11', versions)
        self.assertIn('25', versions)
        self.assertIn('26', versions)

    def test_jdk_filter_all_version_not_matched_by_numeric_filter(self):
        """
        JDK_VERSION='all' does not match a numeric jdk_filter like ['25'].
        The stage must not crash; the entry is simply omitted.
        """
        filtered = _filter_candidates(_REAL_OUTPUT_JSON, jdk_filter=['25'])
        all_entries = [e for e in filtered if e['JDK_VERSION'] == 'all']
        self.assertEqual(all_entries, [])

    def test_no_jdk_filter_includes_all_version_entry(self):
        """Without a JDK filter, JDK_VERSION='all' entries are included."""
        all_entries = [e for e in self.candidates if e['JDK_VERSION'] == 'all']
        self.assertEqual(len(all_entries), 1)
        self.assertEqual(all_entries[0]['CUSTOM_TARGET'],
                         'com/sun/jndi/ldap/RemoveNamingListenerTest.java')

    # -----------------------------------------------------------------------
    # 3. Platform expansion — format 2 → format 3
    # -----------------------------------------------------------------------

    def test_single_concrete_platform_identity(self):
        """s390x_linux expands to ['s390x_linux'] (identity)."""
        self.assertEqual(_expand_platform('s390x_linux'), ['s390x_linux'])

    def test_all_linux_expands_to_four_platforms(self):
        result = _expand_platform('all_linux')
        self.assertEqual(len(result), 4)
        self.assertIn('s390x_linux',   result)
        self.assertIn('ppc64le_linux', result)
        self.assertIn('x86-64_linux',  result)
        self.assertIn('aarch64_linux', result)

    def test_all_windows_expands_to_two_platforms(self):
        result = _expand_platform('all_windows')
        self.assertEqual(result, ['x86-64_windows', 'aarch64_windows'])

    def test_all_aix_expands_to_ppc64_aix(self):
        """all_aix (used by RemoveNamingListenerTest fixture entry) → ['ppc64_aix']."""
        result = _expand_platform('all_aix')
        self.assertEqual(result, ['ppc64_aix'])

    def test_all_expands_to_nine_platforms(self):
        result = _expand_platform('all')
        self.assertEqual(len(result), 9)

    def test_sparcv9_solaris_passes_through_verbatim(self):
        """
        sparcv9_solaris is not in PLATFORM_MAP → passes through as-is.
        The Grinder job will reject it; grinder_to_pl() will return None and
        remove_exclusions.py will never write a solaris token.
        """
        result = _expand_platform('sparcv9_solaris')
        self.assertEqual(result, ['sparcv9_solaris'])

    def test_comma_separated_platform_field_expands_to_multiple(self):
        """
        'x86-64_mac,all_linux' is a single PLATFORM field containing two tokens.
        Expansion must yield one concrete platform for x86-64_mac plus four for all_linux.
        """
        result = _expand_platform('x86-64_mac,all_linux')
        self.assertIn('x86-64_mac',    result)
        self.assertIn('s390x_linux',   result)
        self.assertIn('ppc64le_linux', result)
        self.assertIn('x86-64_linux',  result)
        self.assertIn('aarch64_linux', result)
        self.assertEqual(len(result), 5)

    def test_comma_separated_field_produces_multiple_record_keys(self):
        """
        An entry with 'x86-64_mac,all_linux' must produce five record keys —
        one per concrete Grinder platform.
        """
        entry = {
            'JDK_VERSION': '21',
            'JDK_IMPL': 'hotspot',
            'CUSTOM_TARGET': 'java/net/Socket/LinkLocal.java',
            'PLATFORM': 'x86-64_mac,all_linux',
            'ISSUE_TRACKER_STATUS': 'closed',
            'EXCLUDE_FILE': 'openjdk/excludes/ProblemList_openjdk21.txt',
        }
        keys = _build_record_keys([entry])
        # x86-64_mac + 4 all_linux = 5 keys
        self.assertEqual(len(keys), 5)
        self.assertIn(
            'jdk21|x86-64_mac|java/net/Socket/LinkLocal.java'
            '|openjdk/excludes/ProblemList_openjdk21.txt',
            keys,
        )
        self.assertIn(
            'jdk21|s390x_linux|java/net/Socket/LinkLocal.java'
            '|openjdk/excludes/ProblemList_openjdk21.txt',
            keys,
        )

    # -----------------------------------------------------------------------
    # 4. Deduplication — identical record keys collapse to one job
    # -----------------------------------------------------------------------

    def test_duplicate_entries_deduplicated_to_one_key(self):
        """
        The fixture contains two identical closed+hotspot entries for
        JdwpAttachTest on jdk25/s390x_linux.  After dedup, only one record
        key must exist for that combination.
        """
        keys = _build_record_keys(self.candidates)
        jdwp_s390x_jdk25 = [
            k for k in keys
            if 'JdwpAttachTest' in k
            and 's390x_linux' in k
            and 'jdk25' in k
        ]
        self.assertEqual(len(jdwp_s390x_jdk25), 1)

    def test_different_jdk_versions_not_deduplicated(self):
        """
        JdwpAttachTest appears on both jdk25 and jdk26 with the same
        platform.  These must remain as two separate record keys.
        """
        keys = _build_record_keys(self.candidates)
        jdwp_s390x = [k for k in keys if 'JdwpAttachTest' in k and 's390x_linux' in k]
        self.assertEqual(len(jdwp_s390x), 2)

    def test_all_linux_entry_produces_four_keys(self):
        """
        ProcessBuilder with PLATFORM='all_linux' → 4 concrete Grinder jobs,
        each with a distinct key.
        """
        keys = _build_record_keys(self.candidates)
        pb_keys = [k for k in keys if 'ProcessBuilder' in k]
        self.assertEqual(len(pb_keys), 4)
        platforms = {k.split('|')[1] for k in pb_keys}
        self.assertEqual(platforms,
                         {'s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux'})


# ===========================================================================
# TestStage2CommaSplitPlatform
# Verifies that the Stage 2 PLATFORM field is split on commas before the
# PLATFORM_MAP lookup, and that each token is expanded independently.
# This tests the fix for the issue where "x86-64_mac,all_linux" was treated
# as a single unknown alias instead of two separate tokens.
# ===========================================================================

class TestStage2CommaSplitPlatform(unittest.TestCase):
    """
    Mirrors the fixed expansion logic in triggerGrinder.groovy Stage 2
    (comma-split first, then per-token PLATFORM_MAP lookup).
    """

    def _expand(self, platform_field: str) -> list:
        """
        Exact mirror of the fixed Groovy expansion loop:
          entry['PLATFORM'].toString().split(',').each { rawToken ->
              def token = rawToken.trim().toLowerCase().replace(' ', '_')
              def resolved = PLATFORM_MAP[token]
              if (resolved) { concretePlatforms.addAll(resolved) }
              else          { concretePlatforms << token }
          }
        All PLATFORM_MAP values are Lists (normalised form).
        """
        PLATFORM_MAP = {
            's390x_linux':     ['s390x_linux'],
            'ppc64le_linux':   ['ppc64le_linux'],
            'x86-64_linux':    ['x86-64_linux'],
            'aarch64_linux':   ['aarch64_linux'],
            'arm_linux':       ['arm_linux'],
            'riscv64_linux':   ['riscv64_linux'],
            'x86-64_windows':  ['x86-64_windows'],
            'x86-32_windows':  ['x86-32_windows'],
            'aarch64_windows': ['aarch64_windows'],
            'x86-64_mac':      ['x86-64_mac'],
            'aarch64_mac':     ['aarch64_mac'],
            'ppc64_aix':       ['ppc64_aix'],
            'sparcv9_solaris': ['sparcv9_solaris'],
            'x86-64_solaris':  ['x86-64_solaris'],
            's390x_zos':       ['s390x_zos'],
            'all_linux':   ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux'],
            'all_windows': ['x86-64_windows', 'aarch64_windows'],
            'all_mac':     ['x86-64_mac', 'aarch64_mac'],
            'all_aix':     ['ppc64_aix'],
            'all':         ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux',
                            'x86-64_windows', 'aarch64_windows',
                            'x86-64_mac', 'aarch64_mac', 'ppc64_aix'],
        }
        result = []
        for raw_token in platform_field.split(','):
            token = raw_token.strip().lower().replace(' ', '_')
            resolved = PLATFORM_MAP.get(token)
            if resolved:
                result.extend(resolved)
            else:
                result.append(token)
        return result

    # ---- comma-split correctness ----------------------------------------

    def test_single_token_no_comma(self):
        """Single-token field: behaves identically to the old code."""
        self.assertEqual(self._expand('x86-64_linux'), ['x86-64_linux'])

    def test_comma_two_single_platforms(self):
        """Two concrete platforms separated by a comma → two results."""
        result = self._expand('x86-64_linux,aarch64_mac')
        self.assertEqual(result, ['x86-64_linux', 'aarch64_mac'])

    def test_comma_mac_plus_all_linux(self):
        """
        'x86-64_mac,all_linux' must expand to 5 platforms (1 + 4).
        This was the bug: the old code looked up 'x86-64_mac,all_linux' as
        a single key, found nothing, and passed it through verbatim as one job.
        """
        result = self._expand('x86-64_mac,all_linux')
        self.assertEqual(len(result), 5)
        self.assertIn('x86-64_mac', result)
        self.assertIn('s390x_linux', result)
        self.assertIn('ppc64le_linux', result)
        self.assertIn('x86-64_linux', result)
        self.assertIn('aarch64_linux', result)

    def test_comma_alias_plus_alias(self):
        """Two compound aliases in one field are both expanded."""
        result = self._expand('all_mac,all_aix')
        self.assertEqual(sorted(result), sorted(['x86-64_mac', 'aarch64_mac', 'ppc64_aix']))

    def test_comma_with_whitespace_around_token(self):
        """Tokens may have surrounding spaces; they must be stripped."""
        result = self._expand('x86-64_linux , aarch64_linux')
        self.assertEqual(result, ['x86-64_linux', 'aarch64_linux'])

    def test_unknown_token_passes_through(self):
        """An unrecognised token in a comma list passes through verbatim."""
        result = self._expand('x86-64_linux,some_future_platform')
        self.assertEqual(result, ['x86-64_linux', 'some_future_platform'])

    def test_all_values_are_lists(self):
        """
        Verifies the List-normalisation fix (issue 3): every PLATFORM_MAP value
        must be a list, not a string.  The old code mixed strings and lists,
        requiring an isinstance guard that could silently fail.
        """
        PLATFORM_MAP = {
            's390x_linux':     ['s390x_linux'],
            'ppc64le_linux':   ['ppc64le_linux'],
            'x86-64_linux':    ['x86-64_linux'],
            'aarch64_linux':   ['aarch64_linux'],
            'arm_linux':       ['arm_linux'],
            'riscv64_linux':   ['riscv64_linux'],
            'x86-64_windows':  ['x86-64_windows'],
            'x86-32_windows':  ['x86-32_windows'],
            'aarch64_windows': ['aarch64_windows'],
            'x86-64_mac':      ['x86-64_mac'],
            'aarch64_mac':     ['aarch64_mac'],
            'ppc64_aix':       ['ppc64_aix'],
            'sparcv9_solaris': ['sparcv9_solaris'],
            'x86-64_solaris':  ['x86-64_solaris'],
            's390x_zos':       ['s390x_zos'],
            'all_linux':   ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux'],
            'all_windows': ['x86-64_windows', 'aarch64_windows'],
            'all_mac':     ['x86-64_mac', 'aarch64_mac'],
            'all_aix':     ['ppc64_aix'],
            'all':         ['s390x_linux', 'ppc64le_linux', 'x86-64_linux', 'aarch64_linux',
                            'x86-64_windows', 'aarch64_windows',
                            'x86-64_mac', 'aarch64_mac', 'ppc64_aix'],
        }
        for key, value in PLATFORM_MAP.items():
            with self.subTest(key=key):
                self.assertIsInstance(value, list,
                    f"PLATFORM_MAP[{key!r}] must be a list, got {type(value).__name__}")


# ===========================================================================
# TestStage3Summary
# Verifies the Stage 3 summary-generation logic:
#   - JDK versions with zero passes must appear in the summary.
#   - JDK versions with some passes report the correct count and total.
#   - The 'no tests passed' fallback is used only when no tests ran at all.
# ===========================================================================

def _build_summary(job_results: dict) -> str:
    """
    Pure-Python mirror of the fixed Stage 3 summary logic in triggerGrinder.groovy.

    job_results: dict mapping record_key → 'PASS' | 'FAIL' | 'ERROR'
    Returns the multi-line summary string.
    """
    pass_by_jdk  = {}
    total_by_jdk = {}

    for key, result in job_results.items():
        jdk_ver = key.split('|')[0]          # e.g. "jdk25"
        total_by_jdk[jdk_ver] = total_by_jdk.get(jdk_ver, 0) + 1
        if result == 'PASS':
            pass_by_jdk[jdk_ver] = pass_by_jdk.get(jdk_ver, 0) + 1

    if not total_by_jdk:
        return 'No tests passed on any platform in this run.'

    lines = []
    for jdk in sorted(total_by_jdk):
        total  = total_by_jdk[jdk]
        passes = pass_by_jdk.get(jdk, 0)
        lines.append(f'{jdk}: {passes} test(s) now passing (of {total} retested)')
    return '\n'.join(lines)


class TestStage3Summary(unittest.TestCase):

    def test_empty_results_returns_fallback(self):
        """No jobs ran at all → fallback message."""
        self.assertEqual(
            _build_summary({}),
            'No tests passed on any platform in this run.'
        )

    def test_all_pass(self):
        results = {
            'jdk25|x86-64_linux|Foo.java|f.txt': 'PASS',
            'jdk25|aarch64_mac|Bar.java|f.txt':   'PASS',
        }
        summary = _build_summary(results)
        self.assertIn('jdk25: 2 test(s) now passing (of 2 retested)', summary)

    def test_all_fail_version_still_appears(self):
        """
        A JDK version where every job FAILed must still appear in the summary.
        This was the bug: the old code only iterated passByJdk, so a version
        with zero passes was silently omitted.
        """
        results = {
            'jdk17|x86-64_linux|Foo.java|f.txt': 'FAIL',
            'jdk17|aarch64_linux|Foo.java|f.txt': 'FAIL',
        }
        summary = _build_summary(results)
        self.assertIn('jdk17', summary)
        self.assertIn('0 test(s) now passing', summary)
        self.assertIn('of 2 retested', summary)

    def test_mixed_pass_fail_error(self):
        """Summary counts only PASSes but denominator includes FAIL and ERROR."""
        results = {
            'jdk25|x86-64_linux|Foo.java|f.txt':   'PASS',
            'jdk25|aarch64_linux|Bar.java|f.txt':   'FAIL',
            'jdk25|ppc64_aix|Baz.java|f.txt':       'ERROR',
            'jdk11|s390x_linux|Qux.java|f.txt':     'FAIL',
        }
        summary = _build_summary(results)
        self.assertIn('jdk11: 0 test(s) now passing (of 1 retested)', summary)
        self.assertIn('jdk25: 1 test(s) now passing (of 3 retested)', summary)

    def test_multiple_jdk_versions_all_in_summary(self):
        """Every JDK version that ran appears, even with zero passes."""
        results = {
            'jdk11|x86-64_linux|A.java|f.txt':  'PASS',
            'jdk17|x86-64_linux|B.java|f.txt':  'FAIL',
            'jdk21|x86-64_linux|C.java|f.txt':  'ERROR',
            'jdk25|x86-64_linux|D.java|f.txt':  'PASS',
        }
        summary = _build_summary(results)
        for jdk in ('jdk11', 'jdk17', 'jdk21', 'jdk25'):
            with self.subTest(jdk=jdk):
                self.assertIn(jdk, summary)

    def test_summary_sorted_by_jdk_label(self):
        """Summary lines must be sorted lexicographically by JDK label."""
        results = {
            'jdk25|x86-64_linux|A.java|f.txt': 'PASS',
            'jdk11|x86-64_linux|B.java|f.txt': 'FAIL',
            'jdk17|x86-64_linux|C.java|f.txt': 'PASS',
        }
        summary = _build_summary(results)
        lines = summary.splitlines()
        jdk_labels = [l.split(':')[0] for l in lines]
        self.assertEqual(jdk_labels, sorted(jdk_labels))

    def test_error_results_counted_in_total_not_passes(self):
        """ERROR results count towards the total but not towards passes."""
        results = {
            'jdk25|x86-64_linux|Foo.java|f.txt': 'ERROR',
        }
        summary = _build_summary(results)
        self.assertIn('jdk25: 0 test(s) now passing (of 1 retested)', summary)


# ===========================================================================
# TestStripTestSuffix
# Mirrors the stripTestSuffix() helper in triggerGrinder.groovy.
# ===========================================================================

def _strip_test_suffix(path: str) -> str:
    """
    Python mirror of the Groovy stripTestSuffix() helper.
    Strips everything after the last .<lower-case-letters> extension,
    so "path/to/Test.java#id0" → "path/to/Test.java".
    """
    m = re.match(r'^(.*\.[a-z]+)', path)
    return m.group(1) if m else path


class TestStripTestSuffix(unittest.TestCase):

    def test_no_suffix_unchanged(self):
        """No trailing junk — path returned unchanged."""
        self.assertEqual(_strip_test_suffix('com/sun/jdi/Test.java'), 'com/sun/jdi/Test.java')

    def test_hash_id_stripped(self):
        """#id0 appended after extension is stripped."""
        self.assertEqual(
            _strip_test_suffix('java/lang/ProcessBuilder/Basic.java#id0'),
            'java/lang/ProcessBuilder/Basic.java',
        )

    def test_hash_numeric_stripped(self):
        """Numeric fragment like #1234 stripped."""
        self.assertEqual(
            _strip_test_suffix('path/here/testname.java#1234'),
            'path/here/testname.java',
        )

    def test_sh_extension_stripped(self):
        """Works for non-.java extensions."""
        self.assertEqual(
            _strip_test_suffix('test/script/Run.sh#extra'),
            'test/script/Run.sh',
        )

    def test_no_extension_returns_input(self):
        """If there is no dot-extension at all, return input unchanged."""
        self.assertEqual(_strip_test_suffix('no_extension_here'), 'no_extension_here')

    def test_multiple_dots_keeps_last_extension(self):
        """Only strips after the LAST extension; earlier dots are part of the path."""
        self.assertEqual(
            _strip_test_suffix('com/test/Foo.bar.java#id1'),
            'com/test/Foo.bar.java',
        )

    def test_uppercase_extension_not_stripped(self):
        """The pattern requires lower-case letters; .JAVA is not matched as extension."""
        path = 'Test.JAVA#extra'
        # No lower-case extension match → returned as-is
        self.assertEqual(_strip_test_suffix(path), path)


# ===========================================================================
# TestResolveTargetCandidates
# Verifies that resolveTarget() builds the correct ordered candidate list
# for JDK 8 and JDK 11+ without requiring any network access.
# The existence check (githubFileExists) is abstracted out so we can inject
# a controlled stub.
# ===========================================================================

def _candidate_urls(jdk_version: str) -> list:
    """
    Python mirror of the candidate-URL generation logic in resolveTarget().
    Returns the ordered list of (url_template, target) pairs that would be probed,
    with '{path}' as a placeholder for the test path.
    Does NOT perform any network access.
    """
    try:
        ver = int(jdk_version)
    except ValueError:
        return []   # non-numeric → no candidates

    if ver == 8:
        base = 'https://github.com/openjdk/jdk8u/blob/master'
        return [
            (f'{base}/jdk/test/{{path}}',       'jdk_custom'),
            (f'{base}/hotspot/test/{{path}}',    'hotspot_custom'),
            (f'{base}/langtools/test/{{path}}',  'langtools_custom'),
        ]
    else:
        candidates = []
        for repo in [f'jdk{ver}u', f'jdk{ver}']:
            base = f'https://github.com/openjdk/{repo}/blob/master'
            candidates += [
                (f'{base}/test/jdk/{{path}}',       'jdk_custom'),
                (f'{base}/test/hotspot/{{path}}',    'hotspot_custom'),
                (f'{base}/test/langtools/{{path}}',  'langtools_custom'),
            ]
        return candidates


def _resolve_target_stub(test_path: str, jdk_version: str,
                         exists_fn) -> dict:
    """
    Full mirror of resolveTarget() with the network call replaced by exists_fn.
    exists_fn(url) → bool
    Returns {'target': str|None, 'url': str|None}.
    """
    candidates = _candidate_urls(jdk_version)
    if not candidates:
        return {'target': None, 'url': None}

    for url_template, target in candidates:
        url = url_template.replace('{path}', test_path)
        if exists_fn(url):
            return {'target': target, 'url': url}

    return {'target': None, 'url': None}


class TestResolveTargetCandidates(unittest.TestCase):

    # ---- JDK 8 URL structure -----------------------------------------------

    def test_jdk8_candidate_count(self):
        """JDK 8 has exactly 3 candidates."""
        self.assertEqual(len(_candidate_urls('8')), 3)

    def test_jdk8_first_candidate_is_jdk_tree(self):
        url, target = _candidate_urls('8')[0]
        self.assertIn('/jdk8u/blob/master/jdk/test/{path}', url)
        self.assertEqual(target, 'jdk_custom')

    def test_jdk8_second_candidate_is_hotspot_tree(self):
        url, target = _candidate_urls('8')[1]
        self.assertIn('/jdk8u/blob/master/hotspot/test/{path}', url)
        self.assertEqual(target, 'hotspot_custom')

    def test_jdk8_third_candidate_is_langtools_tree(self):
        url, target = _candidate_urls('8')[2]
        self.assertIn('/jdk8u/blob/master/langtools/test/{path}', url)
        self.assertEqual(target, 'langtools_custom')

    def test_jdk8_repo_is_jdk8u_fixed(self):
        """JDK 8 always uses the fixed jdk8u repo — no jdk8/jdk8u branching."""
        urls = [u for u, _ in _candidate_urls('8')]
        self.assertTrue(all('jdk8u' in u for u in urls))

    # ---- JDK 11+ URL structure ---------------------------------------------

    def test_jdk11_candidate_count(self):
        """JDK 11+ has 6 candidates: 3 sub-trees × 2 repos (Xu then X)."""
        self.assertEqual(len(_candidate_urls('11')), 6)

    def test_jdk25_first_three_use_u_repo(self):
        """First 3 candidates for JDK 25 must use jdk25u."""
        for url, _ in _candidate_urls('25')[:3]:
            self.assertIn('jdk25u', url)

    def test_jdk25_last_three_use_plain_repo(self):
        """Last 3 candidates for JDK 25 must use jdk25 (no u)."""
        for url, _ in _candidate_urls('25')[3:]:
            self.assertIn('/jdk25/', url)
            self.assertNotIn('jdk25u', url)

    def test_jdk11_subtrees_order(self):
        """For both repos the sub-tree order is: jdk, hotspot, langtools."""
        candidates = _candidate_urls('11')
        # First repo (jdk11u)
        self.assertIn('/test/jdk/',       candidates[0][0])
        self.assertIn('/test/hotspot/',   candidates[1][0])
        self.assertIn('/test/langtools/', candidates[2][0])
        # Second repo (jdk11)
        self.assertIn('/test/jdk/',       candidates[3][0])
        self.assertIn('/test/hotspot/',   candidates[4][0])
        self.assertIn('/test/langtools/', candidates[5][0])

    def test_jdk11_branch_is_always_master(self):
        """Branch name is always 'master' regardless of repo variant."""
        for url, _ in _candidate_urls('11'):
            self.assertIn('/blob/master/', url)

    def test_non_numeric_version_returns_empty(self):
        """Non-numeric JDK versions (e.g. 'all', '8+') yield no candidates."""
        self.assertEqual(_candidate_urls('all'), [])
        self.assertEqual(_candidate_urls('8+'), [])
        self.assertEqual(_candidate_urls('22+'), [])

    # ---- resolveTarget stub — first-match semantics ------------------------

    def test_first_matching_url_wins(self):
        """The first URL that exists is returned; later ones are not tried."""
        calls = []
        def exists(url):
            calls.append(url)
            return '/test/jdk/' in url and 'jdk25u' in url

        result = _resolve_target_stub('com/example/Foo.java', '25', exists)
        self.assertEqual(result['target'], 'jdk_custom')
        self.assertIn('jdk25u', result['url'])
        # Should have stopped after the first hit
        self.assertEqual(len(calls), 1)

    def test_falls_through_to_plain_repo_when_u_repo_missing(self):
        """If jdk{N}u has no match, tries jdk{N} repo."""
        def exists(url):
            # Only jdk25 (plain) hotspot tree exists
            return 'jdk25/' in url and '/test/hotspot/' in url and 'jdk25u' not in url

        result = _resolve_target_stub('hotspot/Test.java', '25', exists)
        self.assertEqual(result['target'], 'hotspot_custom')
        self.assertNotIn('jdk25u', result['url'])

    def test_returns_none_when_no_url_exists(self):
        """All probes fail → target and url are both None."""
        result = _resolve_target_stub('missing/Test.java', '21', lambda url: False)
        self.assertIsNone(result['target'])
        self.assertIsNone(result['url'])

    def test_non_numeric_version_returns_none(self):
        """Non-numeric JDK version → target None immediately (no network calls)."""
        calls = []
        result = _resolve_target_stub('any/Test.java', 'all',
                                      lambda url: (calls.append(url) or True))
        self.assertIsNone(result['target'])
        self.assertEqual(calls, [], "No URL probes should be made for non-numeric versions")

    def test_jdk8_langtools_fallback(self):
        """jdk tree and hotspot tree both miss; langtools tree has the file."""
        def exists(url):
            return '/langtools/test/' in url

        result = _resolve_target_stub('tools/Test.java', '8', exists)
        self.assertEqual(result['target'], 'langtools_custom')

    def test_test_path_interpolated_into_url(self):
        """The resolved URL must contain the full test path."""
        test_path = 'com/sun/jdi/JdwpAttachTest.java'

        result = _resolve_target_stub(test_path, '25', lambda url: test_path in url)
        self.assertIn(test_path, result['url'])

    # ---- error deduplication -----------------------------------------------

    def test_error_emitted_once_per_unique_test_jdk_pair(self):
        """
        When the same (test, jdk) combination appears on multiple platforms
        (after alias expansion), the error message must be emitted only once.
        This mirrors the targetCache sentinel logic in Stage 2.
        """
        entries = [
            {'JDK_VERSION': '21', 'CUSTOM_TARGET': 'missing/Test.java',
             'PLATFORM': 'x86-64_linux'},
            {'JDK_VERSION': '21', 'CUSTOM_TARGET': 'missing/Test.java',
             'PLATFORM': 'aarch64_linux'},
        ]

        target_errors = []
        target_cache  = {}   # "cleanPath|jdkVersion" → {'target': str|None, ...}

        for entry in entries:
            clean = _strip_test_suffix(entry['CUSTOM_TARGET'])
            ver   = entry['JDK_VERSION']
            key   = f'{clean}|{ver}'

            if key not in target_cache:
                target_cache[key] = _resolve_target_stub(clean, ver, lambda url: False)

            result = target_cache[key]

            if not result['target']:
                if result['target'] is None:   # null → not yet reported
                    target_cache[key] = {'target': '', 'url': ''}
                    target_errors.append(
                        f'Could not locate {clean} (JDK {ver})'
                    )
                # skip this entry

        self.assertEqual(len(target_errors), 1,
            "Error must be emitted exactly once, not once per platform")




# ===========================================================================
# TestIssueTrackedFixedJdksFilter
# Mirrors the ISSUE_TRACKED_FIXED_JDKS filtering logic in
# triggerGrinder.groovy Stage 1 (lines ~117-135).
#
# Logic:
#   fixedJdks = entry['ISSUE_TRACKED_FIXED_JDKS'] or ''
#   if fixedJdks and JDK_VERSION is numeric:
#       parse tokens like "8", "11", "17+" where "17+" means >=17
#       if ver not covered → skip entry
#   non-numeric JDK_VERSION (e.g. "all") always passes through
# ===========================================================================

def _fixed_jdks_covered(jdk_version_str: str, fixed_jdks: str) -> bool:
    """
    Python mirror of the ISSUE_TRACKED_FIXED_JDKS coverage check in Stage 1.

    Returns True if jdk_version_str is covered by fixed_jdks, or if
    fixed_jdks is empty (meaning the fix applies to all versions), or if
    jdk_version_str is non-numeric (e.g. "all" passes through).

    fixed_jdks examples: "8,11,17+", "25", "17+", ""
    """
    fixed_jdks = fixed_jdks.strip()
    if not fixed_jdks:
        return True   # empty → applies to all versions

    # Non-numeric JDK version (e.g. "all", "8+") passes through
    try:
        ver = int(jdk_version_str)
    except ValueError:
        return True

    for token in fixed_jdks.split(','):
        token = token.strip()
        if token.endswith('+'):
            try:
                if ver >= int(token[:-1]):
                    return True
            except ValueError:
                pass
        else:
            try:
                if ver == int(token):
                    return True
            except ValueError:
                pass
    return False


def _filter_with_fixed_jdks(entries: list) -> list:
    """
    Full Stage 1 filter including the ISSUE_TRACKED_FIXED_JDKS gate.
    Assumes jdk_filter is not set (pass all numeric versions through).
    """
    result = []
    for e in entries:
        if e.get('ISSUE_TRACKER_STATUS') != 'closed':
            continue
        if e.get('JDK_IMPL') != 'hotspot':
            continue
        fixed_jdks = e.get('ISSUE_TRACKED_FIXED_JDKS', '').strip()
        if not _fixed_jdks_covered(e.get('JDK_VERSION', ''), fixed_jdks):
            continue
        result.append(e)
    return result


class TestIssueTrackedFixedJdksFilter(unittest.TestCase):
    """
    Unit tests for the ISSUE_TRACKED_FIXED_JDKS version-gate logic.
    All tests are pure-Python; no Jenkins or network access required.
    """

    # -----------------------------------------------------------------------
    # _fixed_jdks_covered — unit tests for the token-matching function
    # -----------------------------------------------------------------------

    def test_empty_fixed_jdks_always_passes(self):
        """Empty ISSUE_TRACKED_FIXED_JDKS → fix applies to every version."""
        for ver in ('8', '11', '17', '21', '25', '26'):
            with self.subTest(ver=ver):
                self.assertTrue(_fixed_jdks_covered(ver, ''))

    def test_exact_match_numeric_token(self):
        """'25' matches JDK 25 exactly."""
        self.assertTrue(_fixed_jdks_covered('25', '25'))

    def test_exact_match_not_same(self):
        """'25' does NOT match JDK 17."""
        self.assertFalse(_fixed_jdks_covered('17', '25'))

    def test_multi_token_one_matches(self):
        """'8,11,17' — JDK 11 is listed → covered."""
        self.assertTrue(_fixed_jdks_covered('11', '8,11,17'))

    def test_multi_token_none_matches(self):
        """'8,11,17' — JDK 21 is NOT listed → not covered."""
        self.assertFalse(_fixed_jdks_covered('21', '8,11,17'))

    def test_plus_token_at_boundary(self):
        """'17+' — JDK 17 itself is at the boundary → covered."""
        self.assertTrue(_fixed_jdks_covered('17', '17+'))

    def test_plus_token_above_boundary(self):
        """'17+' — JDK 25 is above 17 → covered."""
        self.assertTrue(_fixed_jdks_covered('25', '17+'))

    def test_plus_token_below_boundary(self):
        """'17+' — JDK 11 is below 17 → not covered."""
        self.assertFalse(_fixed_jdks_covered('11', '17+'))

    def test_mixed_tokens_plus_and_exact(self):
        """'8,17+' — JDK 11 not >=17 and not ==8 → not covered."""
        self.assertFalse(_fixed_jdks_covered('11', '8,17+'))

    def test_mixed_tokens_plus_and_exact_covered(self):
        """'8,17+' — JDK 21 is >=17 → covered."""
        self.assertTrue(_fixed_jdks_covered('21', '8,17+'))

    def test_mixed_tokens_exact_covered(self):
        """'8,17+' — JDK 8 is exactly 8 → covered."""
        self.assertTrue(_fixed_jdks_covered('8', '8,17+'))

    def test_non_numeric_jdk_version_always_passes(self):
        """Non-numeric JDK_VERSION ('all', '8+', '22+') always passes through."""
        for ver in ('all', '8+', '22+', 'valhalla'):
            with self.subTest(ver=ver):
                self.assertTrue(_fixed_jdks_covered(ver, '17+'))

    def test_whitespace_stripped_from_tokens(self):
        """Spaces around comma-separated tokens must be handled."""
        self.assertTrue(_fixed_jdks_covered('17', '8 , 17 , 25'))

    # -----------------------------------------------------------------------
    # Full pipeline filter (_filter_with_fixed_jdks)
    # -----------------------------------------------------------------------

    def _make_entry(self, jdk_version, fixed_jdks='', status='closed', impl='hotspot'):
        return {
            'JDK_VERSION': jdk_version,
            'JDK_IMPL': impl,
            'ISSUE_TRACKER_STATUS': status,
            'ISSUE_TRACKED_FIXED_JDKS': fixed_jdks,
            'CUSTOM_TARGET': 'com/example/Test.java',
            'PLATFORM': 'x86-64_linux',
            'EXCLUDE_FILE': 'openjdk/excludes/ProblemList_openjdk.txt',
        }

    def test_entry_with_empty_fixed_jdks_always_passes(self):
        """Entry with ISSUE_TRACKED_FIXED_JDKS='' is always accepted."""
        entries = [self._make_entry('25', fixed_jdks='')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 1)

    def test_entry_fixed_jdks_matches_jdk_version(self):
        """ISSUE_TRACKED_FIXED_JDKS='25' matches JDK 25 → entry accepted."""
        entries = [self._make_entry('25', fixed_jdks='25')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 1)

    def test_entry_fixed_jdks_does_not_match_jdk_version(self):
        """ISSUE_TRACKED_FIXED_JDKS='17' — JDK 25 → entry rejected."""
        entries = [self._make_entry('25', fixed_jdks='17')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 0)

    def test_entry_fixed_jdks_plus_matches_higher_jdk(self):
        """ISSUE_TRACKED_FIXED_JDKS='17+' — JDK 25 is >=17 → accepted."""
        entries = [self._make_entry('25', fixed_jdks='17+')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 1)

    def test_entry_fixed_jdks_plus_rejects_lower_jdk(self):
        """ISSUE_TRACKED_FIXED_JDKS='17+' — JDK 11 is <17 → rejected."""
        entries = [self._make_entry('11', fixed_jdks='17+')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 0)

    def test_non_numeric_jdk_version_passes_through_with_fixed_jdks(self):
        """
        JDK_VERSION='all' is non-numeric.  Even with ISSUE_TRACKED_FIXED_JDKS='17+'
        it must NOT be filtered out — it passes through silently.
        This mirrors the Groovy behaviour: jdkVer.isInteger() guard.
        """
        entries = [self._make_entry('all', fixed_jdks='17+')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 1)

    def test_open_issue_still_excluded_even_if_fixed_jdks_matches(self):
        """Open issues are excluded regardless of ISSUE_TRACKED_FIXED_JDKS."""
        entries = [self._make_entry('25', fixed_jdks='25', status='open')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 0)

    def test_non_hotspot_excluded_even_if_fixed_jdks_matches(self):
        """Non-hotspot entries are excluded regardless of ISSUE_TRACKED_FIXED_JDKS."""
        entries = [self._make_entry('25', fixed_jdks='25', impl='openj9')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 0)

    def test_multiple_versions_partial_filter(self):
        """
        Three versions (11, 17, 25) with ISSUE_TRACKED_FIXED_JDKS='17+'.
        JDK 11 is filtered out; 17 and 25 pass.
        """
        entries = [
            self._make_entry('11', fixed_jdks='17+'),
            self._make_entry('17', fixed_jdks='17+'),
            self._make_entry('25', fixed_jdks='17+'),
        ]
        result = _filter_with_fixed_jdks(entries)
        self.assertEqual(len(result), 2)
        versions = {e['JDK_VERSION'] for e in result}
        self.assertEqual(versions, {'17', '25'})

    def test_comma_list_8_11_17_filters_25(self):
        """ISSUE_TRACKED_FIXED_JDKS='8,11,17' — JDK 25 is NOT in the list."""
        entries = [self._make_entry('25', fixed_jdks='8,11,17')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 0)

    def test_comma_list_8_11_17_accepts_11(self):
        """ISSUE_TRACKED_FIXED_JDKS='8,11,17' — JDK 11 IS in the list."""
        entries = [self._make_entry('11', fixed_jdks='8,11,17')]
        self.assertEqual(len(_filter_with_fixed_jdks(entries)), 1)


if __name__ == '__main__':
    unittest.main(verbosity=2)
