import json
import os
from unittest import TestCase

from playlist_parser import parse_all_files


class Test(TestCase):

    def test_parse_file(self):
        test_data_dir = os.path.join(os.path.dirname(__file__), 'data')
        input_file = os.path.join(test_data_dir, 'playlist_input.xml')
        expected_out_file = os.path.join(test_data_dir, 'playlist_out.json')
        with open(expected_out_file) as f:
            expected = json.load(f)
        actual = [excl.to_scheme() for excl in parse_all_files([input_file])]
        self.assertEqual(expected, actual)

