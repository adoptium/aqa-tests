#!/usr/bin/env python3
"""
Script to remove OpenJDK test exclusions from ProblemList files.

Usage:
    python reenable_openjdk.py -m "/re-enable <testcases> <platform> [jdk=<versions>] [impl=<impl>] [variant=<variant>]" -c <comment_url> -d <workspace_dir>

Examples:
    # Simple re-enable (all JDK versions)
    python reenable_openjdk.py -m "/re-enable test.java macosx-all" -c "https://..." -d "."
    
    # Specific JDK versions
    python reenable_openjdk.py -m "/re-enable test.java macosx-all jdk=11,17,21" -c "https://..." -d "."
    
    # With implementation
    python reenable_openjdk.py -m "/re-enable test.java linux-x64 jdk=11,21 impl=openj9" -c "https://..." -d "."
    
    # With variant (alpine or vendor)
    python reenable_openjdk.py -m "/re-enable test.java linux-x64 jdk=11 variant=alpine" -c "https://..." -d "."
"""

import argparse
import os
import re
import sys
from typing import List, Dict, Tuple, Any, Optional


class ReenableCommandError(Exception):
    """Exception raised for errors in the re-enable command."""
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


def parse_reenable_command(comment_body: str) -> Tuple[List[str], str, Dict[str, str]]:
    """
    Parse /re-enable command with optional parameters.
    
    Format: /re-enable <testcases> <platform> [jdk=<versions>] [impl=<impl>] [variant=<variant>]
    
    Where:
    - testcases: comma-separated (e.g., test1.java,test2.java)
    - platform: platform spec (can contain commas)
    - jdk: optional, comma-separated versions (e.g., 11,17,21) - defaults to all
    - impl: optional, implementation (e.g., openj9, sap)
    - variant: optional, variant (e.g., alpine, eclipse, microsoft)
    
    Args:
        comment_body: The full comment body containing the /re-enable command
        
    Returns:
        Tuple of (testcase_list, platform, optional_params)
        
    Raises:
        ReenableCommandError: If command format is invalid
    """
    # Extract the /re-enable command line
    lines = comment_body.strip().split('\n')
    reenable_line = None
    for line in lines:
        if line.strip().startswith('/re-enable '):
            reenable_line = line.strip()
            break
    
    if not reenable_line:
        raise ReenableCommandError("No /re-enable command found in comment")
    
    # Split the command: /re-enable <testcases> <platform> [optional params...]
    # First split on spaces, but we need at least 3 parts (command, testcases, platform)
    parts = reenable_line.split(None, 2)
    
    if len(parts) < 3:
        raise ReenableCommandError(
            f"Invalid format. Expected: /re-enable <testcases> <platform> [jdk=<versions>] [impl=<impl>] [variant=<variant>]\n"
            f"Got {len(parts)} parts: {parts}"
        )
    
    _, testcases_str, remaining = parts
    
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
        raise ReenableCommandError("At least one test case must be specified")
    
    if not platform:
        raise ReenableCommandError("Platform must be specified")
    
    return testcases, platform, optional_params


def resolve_targets_from_params(optional_params: Dict[str, str], base_dir: str) -> List[Tuple[str, str]]:
    """
    Resolve target files from optional parameters.
    
    Args:
        optional_params: Dictionary of optional parameters (jdk, impl, variant)
        base_dir: Base directory of the workspace
        
    Returns:
        List of tuples (target_description, file_path)
        
    Raises:
        ReenableCommandError: If parameters are invalid or files don't exist
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
            raise ReenableCommandError(
                f"ProblemList file not found for {target_desc}: {file_path}"
            )
        
        targets.append((target_desc, file_path))
    
    return targets


def get_all_jdk_versions(excludes_dir: str) -> List[str]:
    """
    Get default JDK versions for re-enable (LTS versions + latest).
    
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


def remove_exclusion_from_file(file_path: str, testcase: str, platform: str) -> bool:
    """
    Remove an exclusion entry from a ProblemList file.
    
    Matches lines by testcase name and platform, regardless of issue URL.
    
    Args:
        file_path: Path to the ProblemList file
        testcase: Test case name
        platform: Platform specification
        
    Returns:
        True if exclusion was removed, False if it was not found
    """
    with open(file_path, 'r') as f:
        lines = f.readlines()
    
    new_lines = []
    removed = False
    
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith('#'):
            parts = stripped.split()
            if len(parts) >= 3:
                existing_testcase = parts[0]
                existing_platform = parts[2]
                if existing_testcase == testcase and existing_platform == platform:
                    print(f"  ✅ Removed exclusion from {os.path.basename(file_path)}: {stripped}")
                    removed = True
                    continue  # Skip this line (i.e., remove it)
        new_lines.append(line)
    
    if not removed:
        print(f"  ⚠️  Exclusion not found in {os.path.basename(file_path)} for testcase={testcase} platform={platform}")
        return False
    
    with open(file_path, 'w') as f:
        f.writelines(new_lines)
    
    return True


def process_reenable_command(comment_body: str, workspace_dir: str) -> Dict[str, Any]:
    """
    Process /re-enable command and modify ProblemList files.
    
    Args:
        comment_body: The full comment body containing the /re-enable command
        workspace_dir: Base directory of the workspace
        
    Returns:
        Dictionary with processing results
        
    Raises:
        ReenableCommandError: If command processing fails
    """
    # Parse the command
    testcases, platform, optional_params = parse_reenable_command(comment_body)
    
    print(f"Processing /re-enable command:")
    print(f"  Test cases: {', '.join(testcases)}")
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
    total_exclusions_removed = 0
    total_exclusions_skipped = 0
    
    for target_desc, file_path in targets:
        print(f"Processing {target_desc}...")
        
        target_removed = 0
        target_skipped = 0
        
        for testcase in testcases:
            was_removed = remove_exclusion_from_file(file_path, testcase, platform)
            
            if was_removed:
                target_removed += 1
                total_exclusions_removed += 1
            else:
                target_skipped += 1
                total_exclusions_skipped += 1
        
        if target_removed > 0:
            modified_files.append({
                'target': target_desc,
                'file_path': os.path.relpath(file_path, workspace_dir),
                'exclusions_removed': target_removed,
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
    print(f"  Total exclusions removed: {total_exclusions_removed}")
    print(f"  Total exclusions skipped: {total_exclusions_skipped} (not found)")
    
    if not modified_files and not skipped_files:
        raise ReenableCommandError("No files were processed")
    
    return {
        'testcases': testcases,
        'platform': platform,
        'optional_params': optional_params,
        'modified_files': modified_files,
        'skipped_files': skipped_files,
        'total_exclusions_removed': total_exclusions_removed,
        'total_exclusions_skipped': total_exclusions_skipped
    }


def main():
    parser = argparse.ArgumentParser(
        description="Remove OpenJDK test exclusions from ProblemList files"
    )
    parser.add_argument(
        '-m', '--message',
        required=True,
        help='Comment body containing /re-enable command'
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
        # Process the re-enable command
        result = process_reenable_command(args.message, args.directory)
        
        # Exit successfully
        sys.exit(0)
    
    except ReenableCommandError as e:
        print(f"\n❌ Error: {e}", file=sys.stderr)
        sys.exit(1)
    
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
