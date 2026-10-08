import unittest
from main import BaseTable
'Test method in BaseTable class.'

class GeneratedTestCase(unittest.TestCase):

    def test_single_line_heading(self):
        style = 'heading'
        'Test with single-line row.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One Column One', 'Two', 'Three']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [18, 3, 5], 1)]
        expected = [('|', ' Row One Column One ', '|', ' Two ', '|', ' Three ', '|')]
        self.assertEqual(actual, expected)

    def test_single_line_footing(self):
        style = 'footing'
        'Test with single-line row.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One Column One', 'Two', 'Three']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [18, 3, 5], 1)]
        expected = [('|', ' Row One Column One ', '|', ' Two ', '|', ' Three ', '|')]
        self.assertEqual(actual, expected)

    def test_single_line_row(self):
        style = 'row'
        'Test with single-line row.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One Column One', 'Two', 'Three']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [18, 3, 5], 1)]
        expected = [('|', ' Row One Column One ', '|', ' Two ', '|', ' Three ', '|')]
        self.assertEqual(actual, expected)

    def test_multi_line_heading(self):
        style = 'heading'
        'Test with multi-line row.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One\nColumn One', 'Two', 'Three']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [10, 3, 5], 2)]
        expected = [('|', ' Row One    ', '|', ' Two ', '|', ' Three ', '|'), ('|', ' Column One ', '|', '     ', '|', '       ', '|')]
        self.assertEqual(actual, expected)

    def test_multi_line_footing(self):
        style = 'footing'
        'Test with multi-line row.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One\nColumn One', 'Two', 'Three']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [10, 3, 5], 2)]
        expected = [('|', ' Row One    ', '|', ' Two ', '|', ' Three ', '|'), ('|', ' Column One ', '|', '     ', '|', '       ', '|')]
        self.assertEqual(actual, expected)

    def test_multi_line_row(self):
        style = 'row'
        'Test with multi-line row.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One\nColumn One', 'Two', 'Three']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [10, 3, 5], 2)]
        expected = [('|', ' Row One    ', '|', ' Two ', '|', ' Three ', '|'), ('|', ' Column One ', '|', '     ', '|', '       ', '|')]
        self.assertEqual(actual, expected)

    def test_no_padding_no_borders_heading(self):
        style = 'heading'
        'Test without padding or borders.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One\nColumn One', 'Two', 'Three']
        table = BaseTable([row])
        table.inner_column_border = False
        table.outer_border = False
        table.padding_left = 0
        table.padding_right = 0
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [10, 3, 5], 2)]
        expected = [('Row One   ', 'Two', 'Three'), ('Column One', '   ', '     ')]
        self.assertEqual(actual, expected)

    def test_no_padding_no_borders_footing(self):
        style = 'footing'
        'Test without padding or borders.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One\nColumn One', 'Two', 'Three']
        table = BaseTable([row])
        table.inner_column_border = False
        table.outer_border = False
        table.padding_left = 0
        table.padding_right = 0
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [10, 3, 5], 2)]
        expected = [('Row One   ', 'Two', 'Three'), ('Column One', '   ', '     ')]
        self.assertEqual(actual, expected)

    def test_no_padding_no_borders_row(self):
        style = 'row'
        'Test without padding or borders.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One\nColumn One', 'Two', 'Three']
        table = BaseTable([row])
        table.inner_column_border = False
        table.outer_border = False
        table.padding_left = 0
        table.padding_right = 0
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [10, 3, 5], 2)]
        expected = [('Row One   ', 'Two', 'Three'), ('Column One', '   ', '     ')]
        self.assertEqual(actual, expected)

    def test_uneven_heading(self):
        style = 'heading'
        'Test with row missing cells.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One Column One']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [18, 3, 5], 1)]
        expected = [('|', ' Row One Column One ', '|', '     ', '|', '       ', '|')]
        self.assertEqual(actual, expected)

    def test_uneven_footing(self):
        style = 'footing'
        'Test with row missing cells.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One Column One']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [18, 3, 5], 1)]
        expected = [('|', ' Row One Column One ', '|', '     ', '|', '       ', '|')]
        self.assertEqual(actual, expected)

    def test_uneven_row(self):
        style = 'row'
        'Test with row missing cells.\n\n    :param str style: Passed to method.\n    '
        row = ['Row One Column One']
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [18, 3, 5], 1)]
        expected = [('|', ' Row One Column One ', '|', '     ', '|', '       ', '|')]
        self.assertEqual(actual, expected)

    def test_empty_table_heading(self):
        style = 'heading'
        'Test empty table.\n\n    :param str style: Passed to method.\n    '
        row = []
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [], 0)]
        expected = [('|', '|')]
        self.assertEqual(actual, expected)

    def test_empty_table_footing(self):
        style = 'footing'
        'Test empty table.\n\n    :param str style: Passed to method.\n    '
        row = []
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [], 0)]
        expected = [('|', '|')]
        self.assertEqual(actual, expected)

    def test_empty_table_row(self):
        style = 'row'
        'Test empty table.\n\n    :param str style: Passed to method.\n    '
        row = []
        table = BaseTable([row])
        actual = [tuple(i) for i in table.gen_row_lines(row, style, [], 0)]
        expected = [('|', '|')]
        self.assertEqual(actual, expected)