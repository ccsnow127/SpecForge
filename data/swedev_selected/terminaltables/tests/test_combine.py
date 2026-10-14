import unittest
from main import combine
'Test function in module.'

class GeneratedTestCase(unittest.TestCase):

    def test_borders_False(self):
        generator = False
        'Test with borders.\n\n    :param bool generator: Test with generator instead of list.\n    '
        line = ['One', 'Two', 'Three']
        actual = list(combine(iter(line) if generator else line, '>', '|', '<'))
        self.assertEqual(actual, ['>', 'One', '|', 'Two', '|', 'Three', '<'])

    def test_borders_True(self):
        generator = True
        'Test with borders.\n\n    :param bool generator: Test with generator instead of list.\n    '
        line = ['One', 'Two', 'Three']
        actual = list(combine(iter(line) if generator else line, '>', '|', '<'))
        self.assertEqual(actual, ['>', 'One', '|', 'Two', '|', 'Three', '<'])

    def test_no_border_False(self):
        generator = False
        'Test without borders.\n\n    :param bool generator: Test with generator instead of list.\n    '
        line = ['One', 'Two', 'Three']
        actual = list(combine(iter(line) if generator else line, '', '', ''))
        self.assertEqual(actual, ['One', 'Two', 'Three'])

    def test_no_border_True(self):
        generator = True
        'Test without borders.\n\n    :param bool generator: Test with generator instead of list.\n    '
        line = ['One', 'Two', 'Three']
        actual = list(combine(iter(line) if generator else line, '', '', ''))
        self.assertEqual(actual, ['One', 'Two', 'Three'])

    def test_no_items_False(self):
        generator = False
        'Test with empty list.\n\n    :param bool generator: Test with generator instead of list.\n    '
        actual = list(combine(iter([]) if generator else [], '>', '|', '<'))
        self.assertEqual(actual, ['>', '<'])

    def test_no_items_True(self):
        generator = True
        'Test with empty list.\n\n    :param bool generator: Test with generator instead of list.\n    '
        actual = list(combine(iter([]) if generator else [], '>', '|', '<'))
        self.assertEqual(actual, ['>', '<'])