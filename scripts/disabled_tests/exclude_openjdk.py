#!/usr/bin/env python3
"""
Script to add OpenJDK test exclusions to ProblemList files.

Usage:
    python exclude_openjdk.py -m "/exclude <targets> <testcase> <issue_url> <platform>" -c <comment_url> -d <workspace_dir>

Example:
    python exclude_openjdk.py -m "/exclude jdk11,jdk21-openj9 java/beans/PropertyEditor/TestColorClassValue.java https://bugs.openjdk.java.net/browse/JDK-8173082 macosx-all" -c "https://github.com/..." -d "/path/to/aqa-tests"
"""

import argparse
import os
import re
import sys
from typing import List, Dict, Tuple, Any


class ExcludeCommandError(Exception):
    """Exception raised for errors in the exclude command."""
    pass


def parse_exclude_command(comment_body: str) -> Tuple[List[str], List[str], str, str]:
    """
    Parse /exclude command with target specifiers.
    
    Format: /exclude <targets> <testcases> <issue_urls> <platforms>
    
    Where:
    - targets: comma-separated (e.g., jdk11,jdk17,jdk21) - will be parsed
    - testcases: comma-separated (e.g., test1.java,test2.java) - will be parsed
    - issue_urls: kept as-is (can contain commas like https://...,https://...)
    - platforms: kept as-is (can contain commas like linux-all,windows-x64)
    
    Each testcase will create one line in ProblemList with the same issue_urls and platforms.
    
    Args:
        comment_body: The full comment body containing the /exclude command
        
    Returns:
        Tuple of (target_list, testcase_list, issue_urls, platforms)
        
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
    
    # Split the command into parts (space-separated)
    parts = exclude_line.split(None, 4)
    
    if len(parts) != 5:
        raise ExcludeCommandError(
            f"Invalid format. Expected: /exclude <targets> <testcases> <issue_urls> <platforms>\n"
            f"Got {len(parts)} parts: {parts}"
        )
    
    _, targets_str, testcases_str, issue_urls, platforms = parts
    
    # Parse comma-separated targets
    targets = [t.strip() for t in targets_str.split(',') if t.strip()]
    
    if not targets:
        raise ExcludeCommandError("At least one target must be specified")
    
    # Parse comma-separated test cases
    testcases = [tc.strip() for tc in testcases_str.split(',') if tc.strip()]
    
    if not testcases:
        raise ExcludeCommandError("At least one test case must be specified")
    
    return targets, testcases, issue_urls, platforms


def resolve_target_to_file(target: str, base_dir: str) -> str:
    """
    Resolves a target specifier to ProblemList file path.
    
    Format: jdk{version}[-{variant}]
    
    Variants:
    - None (default): openjdk/excludes/ProblemList_openjdk{version}.txt
    - openj9/sap: openjdk/excludes/ProblemList_openjdk{version}-{impl}.txt
    - alpine: openjdk/excludes/alpine/ProblemList_openjdk{version}.txt
    - vendor names: openjdk/excludes/vendors/{vendor}/ProblemList_openjdk{version}.txt
    
    Args:
        target: Target specifier (e.g., jdk11, jdk21-openj9, jdk11-alpine)
        base_dir: Base directory of the workspace
        
    Returns:
        Full path to the ProblemList file
        
    Raises:
        ExcludeCommandError: If target format is invalid or file doesn't exist
    """
    # Parse target: jdk{version}[-{variant}]
    match = re.match(r'^jdk(\d+|valhalla)(?:-(.+))?$', target.lower())
    if not match:
        raise ExcludeCommandError(
            f"Invalid target format: '{target}'. Expected: jdk{{version}}[-{{variant}}]"
        )
    
    version = match.group(1)
    variant = match.group(2)
    
    excludes_dir = os.path.join(base_dir, "openjdk", "excludes")
    
    if variant is None:
        # Root level: ProblemList_openjdk{version}.txt
        file_path = os.path.join(excludes_dir, f"ProblemList_openjdk{version}.txt")
    
    elif variant in ['openj9', 'sap']:
        # Implementation-specific: ProblemList_openjdk{version}-{impl}.txt
        file_path = os.path.join(excludes_dir, f"ProblemList_openjdk{version}-{variant}.txt")
    
    elif variant == 'alpine':
        # Alpine: alpine/ProblemList_openjdk{version}.txt
        file_path = os.path.join(excludes_dir, "alpine", f"ProblemList_openjdk{version}.txt")
    
    else:
        # Vendor-specific: vendors/{vendor}/ProblemList_openjdk{version}.txt
        file_path = os.path.join(excludes_dir, "vendors", variant, f"ProblemList_openjdk{version}.txt")
    
    # Validate file exists
    if not os.path.isfile(file_path):
        raise ExcludeCommandError(
            f"ProblemList file not found for target '{target}': {file_path}"
        )
    
    return file_path


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
    
    # Read the file to find the right place to insert
    with open(file_path, 'r') as f:
        lines = f.readlines()
    
    # Add the exclusion at the end of the file
    exclusion_line = f"{testcase} {issue_url} {platform}\n"
    
    # Ensure file ends with newline before adding
    if lines and not lines[-1].endswith('\n'):
        lines[-1] += '\n'
    
    lines.append(exclusion_line)
    
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
    targets, testcases, issue_urls, platforms = parse_exclude_command(comment_body)
    
    print(f"Processing /exclude command:")
    print(f"  Targets: {', '.join(targets)}")
    print(f"  Test cases: {', '.join(testcases)}")
    print(f"  Issue URLs: {issue_urls}")
    print(f"  Platforms: {platforms}")
    print()
    
    # Process each target
    modified_files = []
    skipped_files = []
    total_exclusions_added = 0
    total_exclusions_skipped = 0
    
    for target in targets:
        try:
            file_path = resolve_target_to_file(target, workspace_dir)
            print(f"Target '{target}' → {os.path.relpath(file_path, workspace_dir)}")
            
            # Add each testcase as a separate line
            target_added = 0
            target_skipped = 0
            
            for testcase in testcases:
                was_added = add_exclusion_to_file(file_path, testcase, issue_urls, platforms)
                
                if was_added:
                    target_added += 1
                    total_exclusions_added += 1
                else:
                    target_skipped += 1
                    total_exclusions_skipped += 1
            
            if target_added > 0:
                modified_files.append({
                    'target': target,
                    'file_path': os.path.relpath(file_path, workspace_dir),
                    'exclusions_added': target_added,
                    'exclusions_skipped': target_skipped
                })
            else:
                skipped_files.append({
                    'target': target,
                    'file_path': os.path.relpath(file_path, workspace_dir),
                    'exclusions_skipped': target_skipped
                })
        
        except ExcludeCommandError as e:
            print(f"  ❌ Error: {e}")
            raise
    
    print()
    print(f"Summary:")
    print(f"  Modified: {len(modified_files)} file(s)")
    print(f"  Total exclusions added: {total_exclusions_added}")
    print(f"  Total exclusions skipped: {total_exclusions_skipped} (already excluded)")
    
    if not modified_files and not skipped_files:
        raise ExcludeCommandError("No files were processed")
    
    return {
        'targets': targets,
        'testcases': testcases,
        'issue_urls': issue_urls,
        'platforms': platforms,
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
