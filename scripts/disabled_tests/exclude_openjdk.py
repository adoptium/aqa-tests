#!/usr/bin/env python3
"""
Script to add OpenJDK test exclusions to ProblemList files.

Usage:
    python exclude_openjdk.py -m "/exclude <testcases> <issue_url> <platform> [jdk=<versions>] [impl=<impl>] [variant=<variant>]" -c <comment_url> -d <workspace_dir>

Examples:
    # Simple exclusion (all JDK versions)
    python exclude_openjdk.py -m "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 macosx-all" -c "https://..." -d "."
    
    # Specific JDK versions
    python exclude_openjdk.py -m "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 macosx-all jdk=11,17,21" -c "https://..." -d "."
    
    # With implementation
    python exclude_openjdk.py -m "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 linux-x64 jdk=11,21 impl=openj9" -c "https://..." -d "."
    
    # With variant (alpine or vendor)
    python exclude_openjdk.py -m "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 linux-x64 jdk=11 variant=alpine" -c "https://..." -d "."
"""

import argparse
import os
import re
import sys
from typing import List, Dict, Tuple, Any, Optional


class ExcludeCommandError(Exception):
    """Exception raised for errors in the exclude command."""
    pass


def parse_optional_params(params_str: str) -> Dict[str, str]:
    """
    Parse optional parameters in format key=value.
    
    Args:
        params_str: String containing optional parameters
        
    Returns:
        Dictionary of parameter name to value
    """
    params = {}
    # Match patterns like jdk=11,17 impl=openj9 variant=alpine
    pattern = r'(\w+)=([^\s]+)'
    matches = re.findall(pattern, params_str)
    
    for key, value in matches:
        params[key] = value
    
    return params


def parse_exclude_command(comment_body: str) -> Tuple[List[str], str, str, Dict[str, str]]:
    """
    Parse /exclude command with optional parameters.
    
    Format: /exclude <testcases> <issue_url> <platform> [jdk=<versions>] [impl=<impl>] [variant=<variant>]
    
    Where:
    - testcases: comma-separated (e.g., test1.java,test2.java)
    - issue_url: single URL
    - platform: platform spec (can contain commas)
    - jdk: optional, comma-separated versions (e.g., 11,17,21) - defaults to all
    - impl: optional, implementation (e.g., openj9, sap)
    - variant: optional, variant (e.g., alpine, eclipse, microsoft)
    
    Args:
        comment_body: The full comment body containing the /exclude command
        
    Returns:
        Tuple of (testcase_list, issue_url, platform, optional_params)
        
    Raises:
        ExcludeCommandError: If command format is invalid
    """
    # Extract the /exclude command line
    lines = comment_body.strip().split('\n')
    exclude_line = None
    for line in lines:
        if line.strip().startswith('/exclude '):
            exclude_line = line.strip()
            break
    
    if not exclude_line:
        raise ExcludeCommandError("No /exclude command found in comment")
    
    # Split the command: /exclude <testcases> <issue_url> <platform> [optional params...]
    # First split on spaces, but we need at least 4 parts (command, testcases, issue_url, platform)
    parts = exclude_line.split(None, 3)
    
    if len(parts) < 4:
        raise ExcludeCommandError(
            f"Invalid format. Expected: /exclude <testcases> <issue_url> <platform> [jdk=<versions>] [impl=<impl>] [variant=<variant>]\n"
            f"Got {len(parts)} parts: {parts}"
        )
    
    _, testcases_str, issue_url, remaining = parts
    
    # Parse remaining part for platform and optional parameters
    # Platform is everything before the first key=value pattern
    optional_match = re.search(r'\s+(\w+=)', remaining)
    if optional_match:
        # Found optional parameters
        platform = remaining[:optional_match.start()].strip()
        optional_str = remaining[optional_match.start():].strip()
        optional_params = parse_optional_params(optional_str)
    else:
        # No optional parameters, everything is platform
        platform = remaining.strip()
        optional_params = {}
    
    # Parse comma-separated test cases
    testcases = [tc.strip() for tc in testcases_str.split(',') if tc.strip()]
    
    if not testcases:
        raise ExcludeCommandError("At least one test case must be specified")
    
    if not platform:
        raise ExcludeCommandError("Platform must be specified")
    
    return testcases, issue_url, platform, optional_params


def resolve_targets_from_params(optional_params: Dict[str, str], base_dir: str) -> List[Tuple[str, str]]:
    """
    Resolve target files from optional parameters.
    
    Args:
        optional_params: Dictionary of optional parameters (jdk, impl, variant)
        base_dir: Base directory of the workspace
        
    Returns:
        List of tuples (target_description, file_path)
        
    Raises:
        ExcludeCommandError: If parameters are invalid or files don't exist
    """
    excludes_dir = os.path.join(base_dir, "openjdk", "excludes")
    
    # Parse JDK versions (default to all if not specified)
    jdk_versions = []
    if 'jdk' in optional_params:
        jdk_versions = [v.strip() for v in optional_params['jdk'].split(',') if v.strip()]
    else:
        # Default: find all available JDK versions
        jdk_versions = get_all_jdk_versions(excludes_dir)
    
    impl = optional_params.get('impl', '')  # e.g., 'openj9', 'sap'
    variant = optional_params.get('variant', '')  # e.g., 'alpine', 'eclipse', 'microsoft'
    
    targets = []
    
    for version in jdk_versions:
        # Construct file path based on parameters
        if variant:
            # Variant takes precedence
            if variant == 'alpine':
                file_path = os.path.join(excludes_dir, "alpine", f"ProblemList_openjdk{version}.txt")
                target_desc = f"jdk{version}-{variant}"
            else:
                # Assume it's a vendor
                file_path = os.path.join(excludes_dir, "vendors", variant, f"ProblemList_openjdk{version}.txt")
                target_desc = f"jdk{version}-{variant}"
        elif impl:
            # Implementation-specific
            file_path = os.path.join(excludes_dir, f"ProblemList_openjdk{version}-{impl}.txt")
            target_desc = f"jdk{version}-{impl}"
        else:
            # Root level (default)
            file_path = os.path.join(excludes_dir, f"ProblemList_openjdk{version}.txt")
            target_desc = f"jdk{version}"
        
        # Validate file exists
        if not os.path.isfile(file_path):
            raise ExcludeCommandError(
                f"ProblemList file not found for {target_desc}: {file_path}"
            )
        
        targets.append((target_desc, file_path))
    
    return targets


def get_all_jdk_versions(excludes_dir: str) -> List[str]:
    """
    Get default JDK versions for exclusions (LTS versions + latest).
    
    Returns LTS versions (8, 11, 17, 21, 25) and the latest version (currently 26).
    This list should be updated quarterly when new versions are released.
    
    Args:
        excludes_dir: Path to the excludes directory
        
    Returns:
        List of JDK version strings (e.g., ['8', '11', '17', '21', '25', '26'])
    """
    # LTS versions: 8, 11, 17, 21, 25 (next LTS will be 29 in 2028)
    # Latest: 26 (update quarterly: 27 in Q1 2026, 28 in Q3 2026, etc.)
    default_versions = ['8', '11', '17', '21', '25', '26']
    
    # Verify each version has a ProblemList file
    available_versions = []
    for version in default_versions:
        file_path = os.path.join(excludes_dir, f"ProblemList_openjdk{version}.txt")
        if os.path.isfile(file_path):
            available_versions.append(version)
    
    return available_versions


def check_exclusion_exists(file_path: str, testcase: str, platform: str) -> bool:
    """
    Check if an exclusion already exists in the ProblemList file.
    
    Args:
        file_path: Path to the ProblemList file
        testcase: Test case name
        platform: Platform specification
        
    Returns:
        True if exclusion already exists, False otherwise
    """
    with open(file_path, 'r') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            
            parts = line.split()
            if len(parts) >= 3:
                existing_testcase = parts[0]
                existing_platform = parts[2]
                
                if existing_testcase == testcase and existing_platform == platform:
                    return True
    
    return False


def find_insertion_index(lines: List[str], testcase: str) -> int:
    """
    Find the best line index to insert the new testcase based on directory structure.
    
    Args:
        lines: List of lines in the ProblemList file
        testcase: Test case name
        
    Returns:
        Index where the new testcase should be inserted
    """
    parts = testcase.split('/')
    
    # Try matching progressively shorter directory prefixes
    for depth in range(len(parts) - 1, 0, -1):
        prefix = '/'.join(parts[:depth]) + '/'
        
        last_match_idx = -1
        for i, line in enumerate(lines):
            line_str = line.strip()
            if not line_str:
                continue
                
            tc = None
            if not line_str.startswith('#'):
                tc = line_str.split()[0]
            elif line_str.startswith('# ') and len(line_str.split()) >= 2:
                tc = line_str.split()[1]
                
            if tc and tc.startswith(prefix):
                last_match_idx = i
                
        if last_match_idx != -1:
            return last_match_idx + 1
            
    # Fallback to appending to the end
    return len(lines)


def add_exclusion_to_file(file_path: str, testcase: str, issue_url: str, platform: str) -> bool:
    """
    Add an exclusion entry to a ProblemList file.
    
    Args:
        file_path: Path to the ProblemList file
        testcase: Test case name
        issue_url: Issue tracker URL
        platform: Platform specification
        
    Returns:
        True if exclusion was added, False if it already existed
    """
    # Check if exclusion already exists
    if check_exclusion_exists(file_path, testcase, platform):
        print(f"  ⚠️  Exclusion already exists in {os.path.basename(file_path)}")
        return False
    
    # Read the file
    with open(file_path, 'r') as f:
        lines = f.readlines()
    
    exclusion_line = f"{testcase} {issue_url} {platform}\n"
    
    insert_idx = find_insertion_index(lines, testcase)
    
    if insert_idx == len(lines):
        # Ensure file ends with newline before adding
        if lines and not lines[-1].endswith('\n'):
            lines[-1] += '\n'
        lines.append(exclusion_line)
    else:
        # Ensure the previous line has a newline
        if insert_idx > 0 and not lines[insert_idx - 1].endswith('\n'):
            lines[insert_idx - 1] += '\n'
        lines.insert(insert_idx, exclusion_line)
    
    # Write back to file
    with open(file_path, 'w') as f:
        f.writelines(lines)
    
    print(f"  ✅ Added exclusion to {os.path.basename(file_path)}")
    return True


def process_exclude_command(comment_body: str, workspace_dir: str) -> Dict[str, Any]:
    """
    Process /exclude command and modify ProblemList files.
    
    Args:
        comment_body: The full comment body containing the /exclude command
        workspace_dir: Base directory of the workspace
        
    Returns:
        Dictionary with processing results
        
    Raises:
        ExcludeCommandError: If command processing fails
    """
    # Parse the command
    testcases, issue_url, platform, optional_params = parse_exclude_command(comment_body)
    
    print(f"Processing /exclude command:")
    print(f"  Test cases: {', '.join(testcases)}")
    print(f"  Issue URL: {issue_url}")
    print(f"  Platform: {platform}")
    if optional_params:
        print(f"  Optional parameters: {optional_params}")
    print()
    
    # Resolve targets from parameters
    targets = resolve_targets_from_params(optional_params, workspace_dir)
    
    print(f"Resolved targets:")
    for target_desc, file_path in targets:
        print(f"  {target_desc} → {os.path.relpath(file_path, workspace_dir)}")
    print()
    
    # Process each target
    modified_files = []
    skipped_files = []
    total_exclusions_added = 0
    total_exclusions_skipped = 0
    
    for target_desc, file_path in targets:
        print(f"Processing {target_desc}...")
        
        # Add each testcase as a separate line
        target_added = 0
        target_skipped = 0
        
        for testcase in testcases:
            was_added = add_exclusion_to_file(file_path, testcase, issue_url, platform)
            
            if was_added:
                target_added += 1
                total_exclusions_added += 1
            else:
                target_skipped += 1
                total_exclusions_skipped += 1
        
        if target_added > 0:
            modified_files.append({
                'target': target_desc,
                'file_path': os.path.relpath(file_path, workspace_dir),
                'exclusions_added': target_added,
                'exclusions_skipped': target_skipped
            })
        else:
            skipped_files.append({
                'target': target_desc,
                'file_path': os.path.relpath(file_path, workspace_dir),
                'exclusions_skipped': target_skipped
            })
        
        print()
    
    print(f"Summary:")
    print(f"  Modified: {len(modified_files)} file(s)")
    print(f"  Total exclusions added: {total_exclusions_added}")
    print(f"  Total exclusions skipped: {total_exclusions_skipped} (already excluded)")
    
    if not modified_files and not skipped_files:
        raise ExcludeCommandError("No files were processed")
    
    return {
        'testcases': testcases,
        'issue_url': issue_url,
        'platform': platform,
        'optional_params': optional_params,
        'modified_files': modified_files,
        'skipped_files': skipped_files,
        'total_exclusions_added': total_exclusions_added,
        'total_exclusions_skipped': total_exclusions_skipped
    }


def main():
    parser = argparse.ArgumentParser(
        description="Add OpenJDK test exclusions to ProblemList files"
    )
    parser.add_argument(
        '-m', '--message',
        required=True,
        help='Comment body containing /exclude command'
    )
    parser.add_argument(
        '-c', '--comment-url',
        required=True,
        help='URL of the comment that triggered this action'
    )
    parser.add_argument(
        '-d', '--directory',
        required=True,
        help='Base directory of the aqa-tests workspace'
    )
    
    args = parser.parse_args()
    
    try:
        # Process the exclude command
        result = process_exclude_command(args.message, args.directory)
        
        # Exit successfully
        sys.exit(0)
    
    except ExcludeCommandError as e:
        print(f"\n❌ Error: {e}", file=sys.stderr)
        sys.exit(1)
    
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()

# Made with Bob
