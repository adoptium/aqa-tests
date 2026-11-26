import json
import os
import tempfile
from unittest import TestCase

from playlist_parser import parse_all_files, DisableNodeProcessingException


class Test(TestCase):

    def test_parse_file(self):
        test_data_dir = os.path.join(os.path.dirname(__file__), 'data')
        input_file = os.path.join(test_data_dir, 'playlist_input.xml')
        expected_out_file = os.path.join(test_data_dir, 'playlist_out.json')
        with open(expected_out_file) as f:
            expected = json.load(f)
        actual = [excl.to_scheme() for excl in parse_all_files([input_file])]
        self.assertEqual(expected, actual)

    def _parse_xml_text(self, xml_text: str):
        with tempfile.NamedTemporaryFile('w', delete=False, suffix='.xml') as f:
            f.write(xml_text)
            path = f.name
        try:
            return parse_all_files([path])
        finally:
            os.unlink(path)

    def test_misspelled_variation_raises_exception(self):
        xml_text = """<?xml version='1.0' encoding='UTF-8'?>
<playlist>
    <test>
        <testCaseName>Test_Invalid_Variation</testCaseName>
        <disables>
            <disable>
                <comment>https://github.com/adoptium/aqa-tests/issues/1</comment>
                <variation>misspelled-variation</variation>
            </disable>
        </disables>
        <variations>
            <variation>Mode101</variation>
        </variations>
    </test>
</playlist>
"""
        with self.assertRaises(DisableNodeProcessingException):
            self._parse_xml_text(xml_text)

    def test_newline_in_comment_raises_exception(self):
        xml_text = """<?xml version='1.0' encoding='UTF-8'?>
<playlist>
    <test>
        <testCaseName>Test_Newline_In_Comment</testCaseName>
        <disables>
            <disable>
                <comment>https://github.com/adoptium/aqa-tests/issues/1
https://github.com/adoptium/aqa-tests/issues/2</comment>
            </disable>
        </disables>
    </test>
</playlist>
"""
        with self.assertRaises(DisableNodeProcessingException):
            self._parse_xml_text(xml_text)

