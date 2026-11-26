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


def parse_platform(platform_str: str) -> Tuple[str, str]:
    """
    Parse a platform string 'os-arch' into (os, arch).
    
    Args:
        platform_str: Platform specification (e.g. 'linux-x64', 'macosx-all')
        
    Returns:
        Tuple of (os, arch)
    """
    if '-' in platform_str:
        os_val, arch_val = platform_str.split('-', 1)
        return os_val.strip(), arch_val.strip()
    return platform_str.strip(), ""


def check_platform_conflicts(platforms: List[str], testcase: str) -> None:
    """
    Check if any platforms share the same OS but different arch where one is 'all'.
    
    Args:
        platforms: List of platform strings (e.g. ['linux-x64', 'linux-all'])
        testcase: Test case name for error reporting
        
    Raises:
        ExcludeCommandError: If conflicting platforms are detected
    """
    for i in range(len(platforms)):
        os1, arch1 = parse_platform(platforms[i])
        for j in range(i + 1, len(platforms)):
            os2, arch2 = parse_platform(platforms[j])
            if os1 == os2 and arch1 != arch2 and (arch1 == "all" or arch2 == "all"):
                raise ExcludeCommandError(
                    f"Conflicting platforms for test '{testcase}': '{platforms[i]}' and '{platforms[j]}' "
                    f"share the same OS '{os1}' with conflicting architectures (one is 'all')."
                )


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


def dedupe_preserving_order(items: List[str]) -> List[str]:
    """Remove duplicate entries from a list while preserving first-seen order."""
    seen = set()
    result = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def add_exclusion_to_file(file_path: str, testcase: str, issue_url: str, platform: str) -> bool:
    """
    Add or update an exclusion entry in a ProblemList file.
    
    Args:
        file_path: Path to the ProblemList file
        testcase: Test case name
        issue_url: Issue tracker URL
        platform: Platform specification
        
    Returns:
        True if file was modified, False otherwise
        
    Raises:
        ExcludeCommandError: If conflicting platforms are detected
    """
    # newline='' prevents Python translating line endings, so existing lines are not normalised
    with open(file_path, 'r', newline='') as f:
        lines = f.readlines()
    
    # Detect the line ending used by the file, for any lines we add
    eol = "\n"
    for line in lines:
        if line.endswith("\r\n"):
            eol = "\r\n"
            break
        elif line.endswith("\n"):
            eol = "\n"
            break
    
    # Identify if there is an existing uncommented line for this testcase
    existing_line_idx = None
    existing_parts = None
    
    for idx, line in enumerate(lines):
        if line.strip().startswith('#'):
            continue
        
        # Check if line begins with full test name followed by space or tab
        if line.startswith(f"{testcase} ") or line.startswith(f"{testcase}\t"):
            parts = line.strip().split()
            if len(parts) >= 3:
                existing_line_idx = idx
                existing_parts = parts
                break
    
    new_platforms = [p.strip() for p in platform.split(',') if p.strip()]
    
    if existing_line_idx is not None:
        existing_issue = existing_parts[1]
        existing_platform_str = existing_parts[2]
        existing_platforms = [p.strip() for p in existing_platform_str.split(',') if p.strip()]
        
        # Exception: existing line has a matching issue link
        if existing_issue == issue_url:
            existing_os_set = {parse_platform(p)[0] for p in existing_platforms}
            new_os_set = {parse_platform(p)[0] for p in new_platforms}
            
            # Exception to the exception: an existing platform OS matches a new platform OS
            if existing_os_set & new_os_set:
                # 1. Array of all platforms (existing and new), 2. remove duplicates
                deduped_platforms = dedupe_preserving_order(existing_platforms + new_platforms)
                
                # 3. Same OS with a different arch, where one arch is 'all', is an error
                check_platform_conflicts(deduped_platforms, testcase)
                
                updated_platform_str = ",".join(deduped_platforms)
            else:
                # No matching OS: simply append the new platform(s)
                updated_platform_str = f"{existing_platform_str},{platform}"
            
            if updated_platform_str == existing_platform_str:
                print(f"  ⚠️  Exclusion already exists in {os.path.basename(file_path)}")
                return False
            
            # Update the existing line in place, keeping that line's own ending
            old_line = lines[existing_line_idx]
            if old_line.endswith("\r\n"):
                line_eol = "\r\n"
            elif old_line.endswith("\n"):
                line_eol = "\n"
            else:
                line_eol = ""
            lines[existing_line_idx] = f"{testcase} {issue_url} {updated_platform_str}{line_eol}"
            print(f"  ✅ Updated existing exclusion in {os.path.basename(file_path)}")
        
        else:
            # Different issue link: new line carries new platforms plus the existing ones
            combined_platforms = dedupe_preserving_order(new_platforms + existing_platforms)
            check_platform_conflicts(combined_platforms, testcase)
            
            # Comment out the existing line by adding a hash to its start
            lines[existing_line_idx] = f"#{lines[existing_line_idx]}"
            
            if lines and not lines[-1].endswith('\n'):
                lines[-1] += eol
            lines.append(f"{testcase} {issue_url} {','.join(combined_platforms)}{eol}")
            print(f"  ✅ Commented existing exclusion and added new exclusion to {os.path.basename(file_path)}")
    
    else:
        # No existing line found: add new exclusion at the end
        if lines and not lines[-1].endswith('\n'):
            lines[-1] += eol
        lines.append(f"{testcase} {issue_url} {platform}{eol}")
        print(f"  ✅ Added exclusion to {os.path.basename(file_path)}")
    
    with open(file_path, 'w', newline='') as f:
        f.writelines(lines)
    
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
