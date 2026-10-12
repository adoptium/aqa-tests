#!/usr/bin/env python3
"""
remove_exclusions.py

Removes entries from ProblemList exclude files for tests that have been
confirmed passing by a Grinder run.

Called by retestClosedIssues.groovy Stage 4.

Input: a plain-text file of record keys, one per line, in the pipe-delimited
format written by retestClosedIssues.groovy:

    jdk{VER}|{GRINDER_PLATFORM}|{FULL_TEST_PATH}|{EXCLUDE_FILE}

Fields:
    jdk{VER}          — e.g. "jdk25"
    GRINDER_PLATFORM  — concrete Grinder platform, e.g. "s390x_linux"
    FULL_TEST_PATH    — full path as it appears in the exclude file,
                        e.g. "com/sun/jdi/JdwpAttachTest.java"
                        or   "java/lang/ProcessBuilder/Basic.java#id0"
    EXCLUDE_FILE      — repo-relative path to the ProblemList file the entry
                        was originally found in, e.g.
                        "openjdk/excludes/ProblemList_openjdk25.txt"
                        Empty string if the entry came from a playlist.xml.

Processing overview
-------------------
All keys are first grouped by (jdk_label, full_test_path) to build a
per-test passing set.  Then, for each unique (jdk_label, full_test_path,
exclude_file) combination, the relevant ProblemList file is opened and
modified in one pass.

Active line handling
--------------------
An active (non-comment) line is matched when:
  - test-name column (col 0) matches FULL_TEST_PATH (exact, or base-path
    match after stripping #id suffixes), AND
  - platform column (col 2) contains the translated platform token as one
    of its comma-separated values.

On a match:
  - The matched token is removed from the platform column.
  - If other tokens remain, the line is rewritten with the survivors.
  - If no tokens remain, the line is deleted and comment-promotion rules
    are applied (see below).

solaris.* tokens are never removed and never count as passing coverage.

Comment-promotion rules (applied when an active line is deleted entirely)
-------------------------------------------------------------------------
We look at all commented-out lines for the same test in the same file.
For each comment line we check whether its platform tokens are all
"covered" by the passing set (coverage is JDK-version-aware — see
COVERAGE_MAP below).  A comment line is deleted if and only if ALL of
its non-solaris tokens are covered.  solaris tokens are ignored.

After deleting fully-covered comment lines:
  - If no comment lines remain: nothing to promote (deletion is complete).
  - If one or more comment lines remain: the LAST surviving comment line
    is promoted to an active line.  Its platform list is the union of all
    surviving comment lines' platform lists, reduced by rules c/e/f.
    All other surviving comment lines are kept as comments (they are a
    log of platform-specific issues that have not yet been resolved).

Platform union/reduction rules (applied to the promoted line's platforms)
-------------------------------------------------------------------------
  Rule c: No spaces and no duplicate tokens (case-insensitive dedup,
          output lower-case).
  Rule e: If generic-all is present, reduce to just [generic-all].
  Rule f: If any {prefix}-all token is present, remove all
          {prefix}-<specific> tokens.

Coverage rules (which concrete passing platforms satisfy a broad token)
-----------------------------------------------------------------------
Varies by JDK major version — see _coverage().
generic-all requires all concrete platforms for the relevant JDK version.
solaris.* tokens are ignored entirely and never block removal.

Usage:
    python remove_exclusions.py --keys-file passing_keys.txt --workspace .
"""

import argparse
import re
import sys
from pathlib import Path
from typing import Dict, FrozenSet, List, NamedTuple, Optional, Set, Tuple


# ---------------------------------------------------------------------------
# Platform translation — Grinder/output.json format (3)  →  ProblemList format (1)
# ---------------------------------------------------------------------------
# Keys:   format 3 (Grinder job parameter, identical to output.json format 2)
# Values: format 1 (ProblemList token)
# Solaris and z/OS have no ProblemList token — they map to None and are ignored.

GRINDER_TO_PROBLEMLIST: Dict[str, Optional[str]] = {
    's390x_linux'    : 'linux-s390x',
    'ppc64le_linux'  : 'linux-ppc64le',
    'x86-64_linux'   : 'linux-x64',
    'aarch64_linux'  : 'linux-aarch64',
    'arm_linux'      : 'linux-arm',
    'riscv64_linux'  : 'linux-riscv64',
    'x86-64_windows' : 'windows-x64',
    'x86-32_windows' : 'windows-x86',
    'aarch64_windows': 'windows-aarch64',
    'x86-64_mac'     : 'macosx-x64',
    'aarch64_mac'    : 'macosx-aarch64',
    'ppc64_aix'      : 'aix-ppc64',
    # Solaris and z/OS: no ProblemList token — map to None so callers can skip them
    'sparcv9_solaris': None,
    'x86-64_solaris' : None,
    's390x_zos'      : None,
}


def grinder_to_pl(grinder_platform: str) -> Optional[str]:
    """
    Translate a Grinder platform string (format 3) to ProblemList format (format 1).
    Returns None for platforms that have no ProblemList token (solaris, z/OS).
    Falls back to the input string unchanged for any unknown platform.
    """
    key = grinder_platform.lower()
    if key in GRINDER_TO_PROBLEMLIST:
        return GRINDER_TO_PROBLEMLIST[key]
    return grinder_platform


# ---------------------------------------------------------------------------
# Coverage map — which concrete ProblemList platforms must ALL pass to
# remove a broad token, keyed by JDK major version range.
# ---------------------------------------------------------------------------

def _coverage(jdk_version: int) -> Dict[str, FrozenSet[str]]:
    """
    Return the broad-token coverage map for the given JDK major version.
    Each key is a broad ProblemList platform token; each value is the
    frozenset of concrete ProblemList platforms that must ALL appear in
    the passing set before that token can be removed.
    """
    # All tokens are ProblemList format 1 (e.g. linux-x64, macosx-aarch64).
    # linux-arm and linux-riscv64 are included in linux-all across all versions.
    if jdk_version <= 8:
        base = {
            'linux-all':   frozenset({'linux-s390x', 'linux-ppc64le', 'linux-x64',
                                      'linux-arm', 'linux-riscv64'}),
            'macosx-all':  frozenset({'macosx-x64'}),
            'aix-all':     frozenset({'aix-ppc64'}),
            'windows-all': frozenset({'windows-x64'}),
            'windows-x86': frozenset({'windows-x64'}),
        }
    elif jdk_version <= 17:
        base = {
            'linux-all':   frozenset({'linux-s390x', 'linux-ppc64le', 'linux-x64',
                                      'linux-aarch64', 'linux-arm', 'linux-riscv64'}),
            'macosx-all':  frozenset({'macosx-x64', 'macosx-aarch64'}),
            'aix-all':     frozenset({'aix-ppc64'}),
            'windows-all': frozenset({'windows-x64'}),
            'windows-x86': frozenset({'windows-x64'}),
        }
    elif jdk_version <= 25:
        base = {
            'linux-all':   frozenset({'linux-s390x', 'linux-ppc64le', 'linux-x64',
                                      'linux-aarch64', 'linux-arm', 'linux-riscv64'}),
            'macosx-all':  frozenset({'macosx-x64', 'macosx-aarch64'}),
            'aix-all':     frozenset({'aix-ppc64'}),
            'windows-all': frozenset({'windows-x64', 'windows-aarch64'}),
            'windows-x86': frozenset({'windows-x64'}),
        }
    else:  # JDK 26+
        base = {
            'linux-all':   frozenset({'linux-s390x', 'linux-ppc64le', 'linux-x64',
                                      'linux-aarch64', 'linux-arm', 'linux-riscv64'}),
            'macosx-all':  frozenset({'macosx-aarch64'}),
            'aix-all':     frozenset({'aix-ppc64'}),
            'windows-all': frozenset({'windows-x64', 'windows-aarch64'}),
            'windows-x86': frozenset({'windows-x64'}),
        }
    # generic-all requires the union of all concrete platforms above
    base['generic-all'] = frozenset().union(*base.values())
    return base


def is_no_problemlist_platform(token: str) -> bool:
    """
    True for platform tokens that have no ProblemList representation and
    should be silently ignored: solaris variants and z/OS.
    These are distinct from aix-all/aix-ppc64 which ARE valid ProblemList tokens.
    """
    t = token.lower()
    return t.startswith('solaris') or t == 's390x_zos'


def token_represented_in_comment_set(token: str, comment_platforms: Set[str]) -> bool:
    """
    Return True if `token` (a ProblemList platform token on the active line)
    is already represented by at least one token in `comment_platforms` (the
    union of all surviving comment lines' platform tokens).

    This uses ProblemList containment rules (comment → active), not the
    Grinder coverage map (concrete passing → broad token):

    - Exact match: token == comment_token.
    - A broader comment token subsumes a specific active token:
        e.g. comment has 'windows-all' → covers active 'windows-x64',
             comment has 'generic-all' → covers everything.
    - A specific comment token does NOT cover a broader active token:
        e.g. comment has 'windows-x64' → does NOT cover active 'windows-all'.
    - Platforms with no ProblemList representation are always considered
      represented (ignored).
    """
    t = token.lower()
    if is_no_problemlist_platform(t):
        return True
    if t in comment_platforms:
        return True
    # Check if any broader token in the comment set subsumes this token.
    # 'generic-all' subsumes everything.
    if 'generic-all' in comment_platforms:
        return True
    # '{prefix}-all' subsumes '{prefix}-<specific>'
    t_prefix = t.split('-')[0] if '-' in t else None
    if t_prefix and f'{t_prefix}-all' in comment_platforms:
        return True
    return False


def token_covered_by_passing(token: str, passing: Set[str], coverage: Dict[str, FrozenSet[str]]) -> bool:
    """
    Return True if `token` (ProblemList format) is satisfied by the passing set.

    - solaris tokens are always ignored (return True so they never block removal).
    - Exact-match specific tokens: True iff token is in passing.
    - Broad tokens (in coverage map): True iff every required concrete platform
      is in passing.
    - Unknown tokens: exact-match only.
    """
    t = token.lower()
    if is_no_problemlist_platform(t):
        return True
    if t in coverage:
        return coverage[t].issubset(passing)
    # Specific token — exact match
    return t in passing


def comment_fully_covered(platform_col: str, passing: Set[str], coverage: Dict[str, FrozenSet[str]]) -> bool:
    """
    Return True if every platform token in platform_col that has a ProblemList
    representation is covered by the passing set.  Platforms with no ProblemList
    representation (solaris, z/OS) are ignored.
    """
    tokens = [t.strip().lower() for t in platform_col.split(',') if t.strip()]
    meaningful = [t for t in tokens if not is_no_problemlist_platform(t)]
    if not meaningful:
        return True  # only ignored platforms — treat as covered
    return all(token_covered_by_passing(t, passing, coverage) for t in meaningful)


# ---------------------------------------------------------------------------
# Platform token set reduction (rules c, e, f)
# ---------------------------------------------------------------------------

def reduce_platforms(tokens: List[str]) -> List[str]:
    """
    Apply rules c, e, f to a list of ProblemList platform tokens.

    Rule c: deduplicate, lower-case, no spaces.
    Rule e (takes precedence over f): if generic-all is present, return
          ['generic-all'].
    Rule f: if any {prefix}-all token is present, remove all
          {prefix}-<specific> tokens.

    Returns a sorted, deduplicated list of lower-case tokens.
    """
    # dict.fromkeys preserves insertion order while deduplicating.
    unique = list(dict.fromkeys(t.lower() for t in tokens))

    if 'generic-all' in unique:
        return ['generic-all']

    all_tokens = [t for t in unique if t.endswith('-all')]
    result = list(unique)
    for all_tok in all_tokens:
        prefix = all_tok[:-len('-all')]
        result = [
            t for t in result
            if t == all_tok or not (t.startswith(prefix + '-') and t != all_tok)
        ]

    return sorted(result)


# ---------------------------------------------------------------------------
# Data types
# ---------------------------------------------------------------------------

class RecordKey(NamedTuple):
    jdk_label:      str   # e.g. "jdk25"
    platform:       str   # Grinder format, e.g. "x86-64_linux"
    full_test_path: str   # e.g. "com/sun/jdi/JdwpAttachTest.java#id0"
    exclude_file:   str   # e.g. "openjdk/excludes/ProblemList_openjdk25.txt"


# ---------------------------------------------------------------------------
# Key parsing
# ---------------------------------------------------------------------------

def parse_key(raw: str) -> RecordKey:
    """Parse one pipe-delimited record key. Raises ValueError if malformed."""
    parts = raw.split('|')
    if len(parts) != 4:
        raise ValueError(
            f"Expected 4 pipe-separated fields, got {len(parts)}: {raw!r}"
        )
    jdk_label, platform, full_test_path, exclude_file = (p.strip() for p in parts)
    if not jdk_label.startswith('jdk'):
        raise ValueError(f"First field must start with 'jdk': {raw!r}")
    if not platform:
        raise ValueError(f"Platform field is empty: {raw!r}")
    if not full_test_path:
        raise ValueError(f"Test path field is empty: {raw!r}")
    return RecordKey(jdk_label, platform, full_test_path, exclude_file)


def jdk_version_int(jdk_label: str) -> int:
    """Extract the integer JDK major version from a label like 'jdk25'."""
    try:
        return int(jdk_label.removeprefix('jdk'))
    except ValueError:
        return 0


# ---------------------------------------------------------------------------
# ProblemList file resolution
# ---------------------------------------------------------------------------

def resolve_files(jdk_label: str, exclude_file: str, workspace: Path) -> List[Path]:
    """
    Return the ProblemList file(s) to search for this jdk/exclude combination.
    If exclude_file is set, use it directly.  Otherwise glob by JDK version.
    """
    if exclude_file:
        target = workspace / exclude_file
        if target.is_file():
            return [target]
        print(
            f"  WARNING: EXCLUDE_FILE not found, falling back to glob: {target}",
            file=sys.stderr,
        )

    jdk_version = jdk_label.removeprefix('jdk')
    excludes_dir = workspace / 'openjdk' / 'excludes'
    pattern = re.compile(
        rf'ProblemList_openjdk{re.escape(jdk_version)}([-.].*)?\.txt$'
    )
    return sorted(
        p for p in excludes_dir.rglob('ProblemList_*.txt')
        if pattern.match(p.name)
    )


# ---------------------------------------------------------------------------
# Line-level helpers
# ---------------------------------------------------------------------------

def test_path_matches(test_col: str, full_test_path: str) -> bool:
    """
    True if test_col refers to the same test as full_test_path.
    Exact match first; falls back to base-path match (strips #id suffix).
    """
    if test_col == full_test_path:
        return True
    return test_col.split('#')[0] == full_test_path.split('#')[0]


def parse_comment_line(line: str) -> Optional[Tuple[str, str, str]]:
    """
    If line is a commented-out exclusion entry, return (test_path, url, platform_col).
    Returns None for section headings and blank comment lines (second field
    must start with 'http' to qualify as an exclusion record).
    """
    stripped = line.rstrip('\r\n')
    if not stripped.lstrip().startswith('#'):
        return None
    content = stripped.lstrip()[1:].lstrip()
    parts = content.split()
    if len(parts) < 3 or not parts[1].startswith('http'):
        return None
    return parts[0], parts[1], parts[2]


# ---------------------------------------------------------------------------
# Output line validation
# ---------------------------------------------------------------------------

def assert_valid_problemlist_line(line: str, context: str) -> None:
    """
    Assert that `line` is a valid ProblemList exclusion line: exactly 3
    space-separated fields (test path, issue URL, platform list) with no
    leading/trailing whitespace other than a single trailing newline.

    Raises ValueError if the check fails, including `context` in the message
    so the caller's location is identifiable in logs.
    """
    content = line.rstrip('\n').rstrip('\r')
    fields = content.split(' ')
    if len(fields) != 3:
        raise ValueError(
            f"ProblemList line has {len(fields)} space-separated field(s), "
            f"expected exactly 3 ({context!r}):\n  {line!r}"
        )


# ---------------------------------------------------------------------------
# Core file modification
# ---------------------------------------------------------------------------

def process_file(
    file_path: Path,
    test_path: str,
    pl_platforms: Set[str],       # ProblemList-format platforms that passed for this test
    jdk_ver: int,
) -> int:
    """
    Process one ProblemList file for one test.

    `pl_platforms` is the full set of concrete ProblemList platforms that
    passed for this (test, JDK) combination — may span multiple Grinder runs.

    For each active line matching the test:
      - Remove any platform token that is in pl_platforms.
      - If tokens remain, rewrite the line.
      - If no tokens remain, delete the line and apply comment-promotion.

    Returns the number of active lines modified.
    """
    with open(file_path, 'r', encoding='utf-8') as fh:
        lines = fh.readlines()

    coverage = _coverage(jdk_ver)
    kept = list(lines)   # we'll mutate this in place
    modified = 0

    # We may need multiple passes if there are multiple active lines for the
    # same test (rare but possible with #id variants sharing a base path).
    # Iterate by index so we can safely splice.
    i = 0
    while i < len(kept):
        line = kept[i]
        stripped = line.rstrip('\r\n')

        if not stripped or stripped.lstrip().startswith('#'):
            i += 1
            continue

        parts = stripped.split()
        if len(parts) < 3:
            i += 1
            continue

        test_col     = parts[0]
        platform_col = parts[2]

        if not test_path_matches(test_col, test_path):
            i += 1
            continue

        # Strip any platform tokens that are covered by the passing set.
        # Uses coverage-aware checking so that broad tokens like windows-all
        # are removed when all required concrete platforms have passed.
        # Ignored platforms (solaris, z/OS) are always kept — they have no
        # ProblemList meaning but should not be silently deleted.
        all_tokens = [t.strip() for t in platform_col.split(',')]
        remaining = [
            t for t in all_tokens
            if is_no_problemlist_platform(t.lower())
            or not token_covered_by_passing(t.lower(), pl_platforms, coverage)
        ]

        if remaining == all_tokens:
            # No token on this line is covered by the passing set — leave it alone
            i += 1
            continue

        modified += 1

        active_line_deleted = not remaining

        if remaining:
            # Partial rewrite — update the active line in place
            new_line = f'{parts[0]} {parts[1]} {",".join(remaining)}\n'
            assert_valid_problemlist_line(new_line, f'rewrite at index {i} in {file_path}')
            kept[i] = new_line
        else:
            # All tokens removed — delete the active line entirely
            del kept[i]
            # Do not increment i; the next line has shifted into position i.

        # --- Comment cleanup (runs on both partial rewrite and full deletion) ---
        # Find all comment lines for this test in the current file state.
        comment_positions = []
        for j, kline in enumerate(kept):
            parsed = parse_comment_line(kline)
            if parsed and test_path_matches(parsed[0], test_path):
                comment_positions.append(j)

        if not comment_positions:
            if not active_line_deleted:
                i += 1
            continue

        # Classify: covered (delete) vs surviving (keep / promote)
        covered_positions = []
        surviving_positions = []
        for pos in comment_positions:
            parsed = parse_comment_line(kept[pos])
            if parsed and comment_fully_covered(parsed[2], pl_platforms, coverage):
                covered_positions.append(pos)
            else:
                surviving_positions.append(pos)

        # Delete fully-covered comment lines (reverse order preserves indices)
        for pos in reversed(covered_positions):
            del kept[pos]
            surviving_positions = [
                p if p < pos else p - 1 for p in surviving_positions
            ]
            if pos < i:
                i -= 1

        # Build the union of all surviving comment platforms — used both for
        # the redundancy check below and for the promoted line's platform list.
        all_surviving_platforms: List[str] = []
        for pos in surviving_positions:
            parsed = parse_comment_line(kept[pos])
            if parsed:
                all_surviving_platforms.extend(
                    t.strip() for t in parsed[2].split(',') if t.strip()
                )

        if not active_line_deleted:
            # Active line was partially rewritten.  Delete it and promote only
            # if every remaining token is already covered by the union of
            # surviving comment platforms — i.e. the active line is now
            # fully redundant.  If any token is unique to the active line,
            # keep the active line as-is (covered comments were already removed).
            if surviving_positions:
                surviving_pl_set = {
                    t.lower() for t in all_surviving_platforms if t.strip()
                }
                active_redundant = all(
                    token_represented_in_comment_set(t.lower(), surviving_pl_set)
                    for t in remaining
                )
                if active_redundant:
                    # Remove the rewritten active line — it is fully represented
                    # by the surviving comments which will be promoted below.
                    del kept[i]
                    active_line_deleted = True
                else:
                    # Active line has unique platforms — leave it, no promotion.
                    i += 1
                    continue
            else:
                # No surviving comments — active line stays, nothing to promote.
                i += 1
                continue

        if not surviving_positions:
            # All comments were covered and the active line is gone — done.
            continue

        # Active line was deleted (originally or due to redundancy) and
        # survivors remain: promote the LAST surviving comment to an active line.
        # Its platform list = union of all surviving comment lines' platforms,
        # reduced by rules c/e/f.
        last_pos = surviving_positions[-1]
        last_parsed = parse_comment_line(kept[last_pos])

        merged = reduce_platforms(all_surviving_platforms)

        # Rewrite last comment as active line with merged platforms
        promoted_line = f'{last_parsed[0]} {last_parsed[1]} {",".join(merged)}\n'
        assert_valid_problemlist_line(promoted_line, f'promotion at index {last_pos} in {file_path}')
        kept[last_pos] = promoted_line

        # All other surviving comment lines remain as comments (historical log).

    if modified:
        with open(file_path, 'w', encoding='utf-8') as fh:
            fh.writelines(kept)

    return modified


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description='Remove ProblemList exclusions for tests confirmed passing by Grinder.'
    )
    parser.add_argument(
        '--keys-file', required=True,
        help=(
            'File containing one pipe-delimited record key per line: '
            'jdk{VER}|{GRINDER_PLATFORM}|{FULL_TEST_PATH}|{EXCLUDE_FILE}'
        ),
    )
    parser.add_argument(
        '--workspace', required=True,
        help='Root of the aqa-tests checkout (contains openjdk/excludes/)',
    )
    args = parser.parse_args()

    keys_file = Path(args.keys_file)
    workspace = Path(args.workspace)

    if not keys_file.is_file():
        print(f'ERROR: keys file not found: {keys_file}', file=sys.stderr)
        sys.exit(1)

    if not (workspace / 'openjdk' / 'excludes').is_dir():
        print(
            f'ERROR: openjdk/excludes not found under workspace: {workspace}',
            file=sys.stderr,
        )
        sys.exit(1)

    raw_keys = [
        line.strip()
        for line in keys_file.read_text(encoding='utf-8').splitlines()
        if line.strip()
    ]

    # --- Parse all keys ------------------------------------------------------
    parsed_keys: List[RecordKey] = []
    for raw in raw_keys:
        try:
            parsed_keys.append(parse_key(raw))
        except ValueError as exc:
            print(f'WARNING: skipping malformed key {raw!r}: {exc}', file=sys.stderr)

    # --- Group by (jdk_label, full_test_path) to build per-test passing sets --
    # Key: (jdk_label, full_test_path)
    # Value: set of ProblemList-format platforms that passed
    PassingKey = Tuple[str, str]
    passing_sets: Dict[PassingKey, Set[str]] = {}
    # Also track which exclude_file each (jdk, test) combination came from
    exclude_files: Dict[PassingKey, str] = {}

    for key in parsed_keys:
        pk: PassingKey = (key.jdk_label, key.full_test_path)
        pl_platform = grinder_to_pl(key.platform)
        if pl_platform is None:
            # Platform has no ProblemList representation (solaris, z/OS) — skip
            continue
        passing_sets.setdefault(pk, set()).add(pl_platform)
        # Last seen exclude_file wins (they should all be the same for a given test)
        exclude_files[pk] = key.exclude_file

    # --- Process each unique (jdk, test) combination -------------------------
    total_removed        = 0
    total_files_modified = 0

    for (jdk_label, test_path), pl_platforms in sorted(passing_sets.items()):
        jdk_ver     = jdk_version_int(jdk_label)
        excl_file   = exclude_files[(jdk_label, test_path)]

        print(f'\nProcessing: {jdk_label} | {test_path}')
        print(f'  Passing platforms: {", ".join(sorted(pl_platforms))}')
        if excl_file:
            print(f'  Source file: {excl_file}')

        target_files = resolve_files(jdk_label, excl_file, workspace)
        if not target_files:
            print(f'  WARNING: no ProblemList files found for {jdk_label} — skipping.')
            continue

        for pl_file in target_files:
            removed = process_file(pl_file, test_path, pl_platforms, jdk_ver)
            if removed:
                rel = pl_file.relative_to(workspace)
                print(f'  Modified {removed} line(s) in {rel}')
                total_removed        += removed
                total_files_modified += 1

    print(
        f'\nDone. {total_removed} line(s) modified across '
        f'{total_files_modified} file(s).'
    )


if __name__ == '__main__':
    main()
