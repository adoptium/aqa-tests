import os
import tempfile
import shutil
from unittest import TestCase

from exclude_openjdk import (
    parse_exclude_command,
    parse_optional_params,
    resolve_targets_from_params,
    get_all_jdk_versions,
    check_exclusion_exists,
    add_exclusion_to_file,
    ExcludeCommandError,
)


class TestParseOptionalParams(TestCase):
    """Test parsing of optional parameters from command."""
    
    def test_parse_single_param(self):
        result = parse_optional_params("jdk=11")
        self.assertEqual(result, {'jdk': '11'})
    
    def test_parse_multiple_params(self):
        result = parse_optional_params("jdk=11,17,21 impl=openj9")
        self.assertEqual(result, {'jdk': '11,17,21', 'impl': 'openj9'})
    
    def test_parse_all_params(self):
        result = parse_optional_params("jdk=11 impl=openj9 variant=alpine")
        self.assertEqual(result, {
            'jdk': '11',
            'impl': 'openj9',
            'variant': 'alpine'
        })
    
    def test_parse_empty_string(self):
        result = parse_optional_params("")
        self.assertEqual(result, {})
    
    def test_parse_with_extra_spaces(self):
        result = parse_optional_params("  jdk=11   impl=openj9  ")
        self.assertEqual(result, {'jdk': '11', 'impl': 'openj9'})


class TestParseExcludeCommand(TestCase):
    """Test parsing of /exclude command."""
    
    def test_simple_command(self):
        comment = "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 macosx-all"
        testcases, issue_url, platform, params = parse_exclude_command(comment)
        
        self.assertEqual(testcases, ['test.java'])
        self.assertEqual(issue_url, 'https://bugs.openjdk.java.net/browse/JDK-8173082')
        self.assertEqual(platform, 'macosx-all')
        self.assertEqual(params, {})
    
    def test_command_with_jdk_param(self):
        comment = "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 macosx-all jdk=11,17,21"
        testcases, issue_url, platform, params = parse_exclude_command(comment)
        
        self.assertEqual(testcases, ['test.java'])
        self.assertEqual(platform, 'macosx-all')
        self.assertEqual(params, {'jdk': '11,17,21'})
    
    def test_command_with_impl_param(self):
        comment = "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 linux-x64 jdk=11,21 impl=openj9"
        testcases, issue_url, platform, params = parse_exclude_command(comment)
        
        self.assertEqual(platform, 'linux-x64')
        self.assertEqual(params, {'jdk': '11,21', 'impl': 'openj9'})
    
    def test_command_with_variant_param(self):
        comment = "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 linux-x64 jdk=11 variant=alpine"
        testcases, issue_url, platform, params = parse_exclude_command(comment)
        
        self.assertEqual(params, {'jdk': '11', 'variant': 'alpine'})
    
    def test_command_with_multiple_testcases(self):
        comment = "/exclude test1.java,test2.java,test3.java https://bugs.openjdk.java.net/browse/JDK-8173082 macosx-all"
        testcases, issue_url, platform, params = parse_exclude_command(comment)
        
        self.assertEqual(testcases, ['test1.java', 'test2.java', 'test3.java'])
    
    def test_command_with_platform_containing_comma(self):
        comment = "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 linux-x64,windows-x64"
        testcases, issue_url, platform, params = parse_exclude_command(comment)
        
        self.assertEqual(platform, 'linux-x64,windows-x64')
    
    def test_command_in_multiline_comment(self):
        comment = """
Some text before
/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082 macosx-all jdk=11
Some text after
"""
        testcases, issue_url, platform, params = parse_exclude_command(comment)
        
        self.assertEqual(testcases, ['test.java'])
        self.assertEqual(params, {'jdk': '11'})
    
    def test_invalid_command_missing_parts(self):
        comment = "/exclude test.java https://bugs.openjdk.java.net/browse/JDK-8173082"
        
        with self.assertRaises(ExcludeCommandError) as cm:
            parse_exclude_command(comment)
        
        self.assertIn("Invalid format", str(cm.exception))
    
    def test_invalid_command_no_exclude(self):
        comment = "some other comment"
        
        with self.assertRaises(ExcludeCommandError) as cm:
            parse_exclude_command(comment)
        
        self.assertIn("No /exclude command found", str(cm.exception))


class TestGetAllJdkVersions(TestCase):
    """Test getting default JDK versions."""
    
    def setUp(self):
        # Create temporary directory structure
        self.temp_dir = tempfile.mkdtemp()
        self.excludes_dir = os.path.join(self.temp_dir, "openjdk", "excludes")
        os.makedirs(self.excludes_dir)
        
        # Create some ProblemList files
        for version in ['8', '11', '17', '21', '25', '26']:
            file_path = os.path.join(self.excludes_dir, f"ProblemList_openjdk{version}.txt")
            with open(file_path, 'w') as f:
                f.write("# Test file\n")
    
    def tearDown(self):
        shutil.rmtree(self.temp_dir)
    
    def test_get_all_jdk_versions(self):
        versions = get_all_jdk_versions(self.excludes_dir)
        
        # Should return LTS + latest that exist
        self.assertEqual(versions, ['8', '11', '17', '21', '25', '26'])
    
    def test_get_all_jdk_versions_missing_some(self):
        # Remove version 25
        os.remove(os.path.join(self.excludes_dir, "ProblemList_openjdk25.txt"))
        
        versions = get_all_jdk_versions(self.excludes_dir)
        
        # Should only return versions that exist
        self.assertEqual(versions, ['8', '11', '17', '21', '26'])
    
    def test_get_all_jdk_versions_nonexistent_dir(self):
        versions = get_all_jdk_versions("/nonexistent/path")
        
        self.assertEqual(versions, [])


class TestResolveTargetsFromParams(TestCase):
    """Test resolving target files from parameters."""
    
    def setUp(self):
        # Create temporary directory structure
        self.temp_dir = tempfile.mkdtemp()
        self.excludes_dir = os.path.join(self.temp_dir, "openjdk", "excludes")
        os.makedirs(self.excludes_dir)
        
        # Create base ProblemList files
        for version in ['8', '11', '17', '21']:
            file_path = os.path.join(self.excludes_dir, f"ProblemList_openjdk{version}.txt")
            with open(file_path, 'w') as f:
                f.write("# Test file\n")
        
        # Create openj9 files
        for version in ['11', '21']:
            file_path = os.path.join(self.excludes_dir, f"ProblemList_openjdk{version}-openj9.txt")
            with open(file_path, 'w') as f:
                f.write("# Test file\n")
        
        # Create alpine directory and files
        alpine_dir = os.path.join(self.excludes_dir, "alpine")
        os.makedirs(alpine_dir)
        file_path = os.path.join(alpine_dir, "ProblemList_openjdk11.txt")
        with open(file_path, 'w') as f:
            f.write("# Test file\n")
        
        # Create vendor directory and files
        vendor_dir = os.path.join(self.excludes_dir, "vendors", "eclipse")
        os.makedirs(vendor_dir)
        file_path = os.path.join(vendor_dir, "ProblemList_openjdk11.txt")
        with open(file_path, 'w') as f:
            f.write("# Test file\n")
    
    def tearDown(self):
        shutil.rmtree(self.temp_dir)
    
    def test_resolve_specific_jdk_versions(self):
        params = {'jdk': '11,17,21'}
        targets = resolve_targets_from_params(params, self.temp_dir)
        
        self.assertEqual(len(targets), 3)
        self.assertEqual([desc for desc, _ in targets], ['jdk11', 'jdk17', 'jdk21'])
    
    def test_resolve_with_impl(self):
        params = {'jdk': '11,21', 'impl': 'openj9'}
        targets = resolve_targets_from_params(params, self.temp_dir)
        
        self.assertEqual(len(targets), 2)
        self.assertEqual([desc for desc, _ in targets], ['jdk11-openj9', 'jdk21-openj9'])
    
    def test_resolve_with_alpine_variant(self):
        params = {'jdk': '11', 'variant': 'alpine'}
        targets = resolve_targets_from_params(params, self.temp_dir)
        
        self.assertEqual(len(targets), 1)
        self.assertEqual(targets[0][0], 'jdk11-alpine')
        self.assertIn('alpine', targets[0][1])
    
    def test_resolve_with_vendor_variant(self):
        params = {'jdk': '11', 'variant': 'eclipse'}
        targets = resolve_targets_from_params(params, self.temp_dir)
        
        self.assertEqual(len(targets), 1)
        self.assertEqual(targets[0][0], 'jdk11-eclipse')
        self.assertIn('vendors/eclipse', targets[0][1])
    
    def test_resolve_nonexistent_file(self):
        params = {'jdk': '99'}
        
        with self.assertRaises(ExcludeCommandError) as cm:
            resolve_targets_from_params(params, self.temp_dir)
        
        self.assertIn("not found", str(cm.exception))


class TestCheckExclusionExists(TestCase):
    """Test checking if exclusion already exists."""
    
    def setUp(self):
        self.temp_file = tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt')
        self.temp_file.write("""# Test ProblemList file
test1.java https://bugs.openjdk.java.net/browse/JDK-1234 macosx-all
test2.java https://bugs.openjdk.java.net/browse/JDK-5678 linux-x64
# Comment line
test3.java https://bugs.openjdk.java.net/browse/JDK-9012 windows-x64
""")
        self.temp_file.close()
    
    def tearDown(self):
        os.unlink(self.temp_file.name)
    
    def test_exclusion_exists(self):
        result = check_exclusion_exists(self.temp_file.name, 'test1.java', 'macosx-all')
        self.assertTrue(result)
    
    def test_exclusion_not_exists(self):
        result = check_exclusion_exists(self.temp_file.name, 'test4.java', 'macosx-all')
        self.assertFalse(result)
    
    def test_exclusion_different_platform(self):
        result = check_exclusion_exists(self.temp_file.name, 'test1.java', 'linux-x64')
        self.assertFalse(result)


class TestAddExclusionToFile(TestCase):
    """Test adding exclusion to file."""
    
    def setUp(self):
        self.temp_file = tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt')
        self.temp_file.write("""# Test ProblemList file
test1.java https://bugs.openjdk.java.net/browse/JDK-1234 macosx-all
""")
        self.temp_file.close()
    
    def tearDown(self):
        os.unlink(self.temp_file.name)
    
    def test_add_new_exclusion(self):
        result = add_exclusion_to_file(
            self.temp_file.name,
            'test2.java',
            'https://bugs.openjdk.java.net/browse/JDK-5678',
            'linux-x64'
        )
        
        self.assertTrue(result)
        
        # Verify it was added
        with open(self.temp_file.name, 'r') as f:
            content = f.read()
        
        self.assertIn('test2.java https://bugs.openjdk.java.net/browse/JDK-5678 linux-x64', content)
    
    def test_add_duplicate_exclusion(self):
        # Try to add existing exclusion
        result = add_exclusion_to_file(
            self.temp_file.name,
            'test1.java',
            'https://bugs.openjdk.java.net/browse/JDK-1234',
            'macosx-all'
        )
        
        self.assertFalse(result)
        
        # Verify file wasn't modified
        with open(self.temp_file.name, 'r') as f:
            lines = f.readlines()
        
        # Should still have only 2 lines (comment + original exclusion)
        non_empty_lines = [l for l in lines if l.strip() and not l.strip().startswith('#')]
        self.assertEqual(len(non_empty_lines), 1)

class TestMergeExclusionIntoExistingLine(TestCase):
    """Test merging platforms/issues into an existing ProblemList line."""
    
    ISSUE = 'https://bugs.openjdk.org/browse/JDK-8368206'
    TEST = 'compiler/vectorapi/VectorMaskCompareNotTest.java'
    
    def setUp(self):
        self.temp_file = tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt')
        self.temp_file.write(
            "# Test ProblemList file\n"
            f"{self.TEST} {self.ISSUE} linux-riscv64,aix-ppc64\n"
            "other.java https://bugs.openjdk.org/browse/JDK-1 macosx-all\n"
        )
        self.temp_file.close()
    
    def tearDown(self):
        os.unlink(self.temp_file.name)
    
    def _read(self):
        with open(self.temp_file.name, 'r') as f:
            return f.read()
    
    def test_merge_platform_into_existing_line(self):
        result = add_exclusion_to_file(self.temp_file.name, self.TEST, self.ISSUE, 'x86-64_linux')
        
        self.assertTrue(result)
        self.assertEqual(
            self._read(),
            "# Test ProblemList file\n"
            f"{self.TEST} {self.ISSUE} linux-riscv64,aix-ppc64,x86-64_linux\n"
            "other.java https://bugs.openjdk.org/browse/JDK-1 macosx-all\n"
        )
    
    def test_merge_is_idempotent(self):
        add_exclusion_to_file(self.temp_file.name, self.TEST, self.ISSUE, 'x86-64_linux')
        before = self._read()
        
        result = add_exclusion_to_file(self.temp_file.name, self.TEST, self.ISSUE, 'x86-64_linux')
        
        self.assertFalse(result)
        self.assertEqual(self._read(), before)
    
    def test_merge_multiple_platforms_skips_present_ones(self):
        result = add_exclusion_to_file(
            self.temp_file.name, self.TEST, self.ISSUE, 'aix-ppc64,x86-64_linux,windows-all')
        
        self.assertTrue(result)
        self.assertIn(f"{self.TEST} {self.ISSUE} linux-riscv64,aix-ppc64,x86-64_linux,windows-all\n", self._read())
    
    def test_merge_different_issue_url_appends_comma_separated(self):
        new_issue = 'https://bugs.openjdk.org/browse/JDK-9999'
        result = add_exclusion_to_file(self.temp_file.name, self.TEST, new_issue, 'x86-64_linux')
        
        self.assertTrue(result)
        self.assertIn(
            f"{self.TEST} {self.ISSUE},{new_issue} linux-riscv64,aix-ppc64,x86-64_linux\n", self._read())
    
    def test_new_testcase_still_appended(self):
        result = add_exclusion_to_file(self.temp_file.name, 'new.java', self.ISSUE, 'x86-64_linux')
        
        self.assertTrue(result)
        self.assertTrue(self._read().endswith(f"new.java {self.ISSUE} x86-64_linux\n"))
    
    def test_commented_line_is_not_merged(self):
        with open(self.temp_file.name, 'a') as f:
            f.write("#commented.java https://bugs.openjdk.org/browse/JDK-2 linux-x64\n")
        
        add_exclusion_to_file(self.temp_file.name, 'commented.java', self.ISSUE, 'aix-ppc64')
        
        content = self._read()
        self.assertIn("#commented.java https://bugs.openjdk.org/browse/JDK-2 linux-x64\n", content)
        self.assertTrue(content.endswith(f"commented.java {self.ISSUE} aix-ppc64\n"))

# Made with Bob
