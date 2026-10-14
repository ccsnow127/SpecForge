import unittest
from main import BaseTable
from main import max_dimensions
'Test method in BaseTable class.'
SINGLE_LINE = (('Name', 'Color', 'Type'), ('Avocado', 'green', 'nut'), ('Tomato', 'red', 'fruit'), ('Lettuce', 'green', 'vegetable'))

class GeneratedTestCase(unittest.TestCase):

    def test_top_bottom_True_top(self):
        inner_column_border = True
        style = 'top'
        'Test top and bottom borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE, 'Example')
        table.inner_column_border = inner_column_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'top' and inner_column_border:
            expected = '+Example--+-------+-----------+'
        elif style == 'top':
            expected = '+Example--------------------+'
        elif style == 'bottom' and inner_column_border:
            expected = '+---------+-------+-----------+'
        else:
            expected = '+---------------------------+'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_top_bottom_True_bottom(self):
        inner_column_border = True
        style = 'bottom'
        'Test top and bottom borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE, 'Example')
        table.inner_column_border = inner_column_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'top' and inner_column_border:
            expected = '+Example--+-------+-----------+'
        elif style == 'top':
            expected = '+Example--------------------+'
        elif style == 'bottom' and inner_column_border:
            expected = '+---------+-------+-----------+'
        else:
            expected = '+---------------------------+'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_top_bottom_False_top(self):
        inner_column_border = False
        style = 'top'
        'Test top and bottom borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE, 'Example')
        table.inner_column_border = inner_column_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'top' and inner_column_border:
            expected = '+Example--+-------+-----------+'
        elif style == 'top':
            expected = '+Example--------------------+'
        elif style == 'bottom' and inner_column_border:
            expected = '+---------+-------+-----------+'
        else:
            expected = '+---------------------------+'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_top_bottom_False_bottom(self):
        inner_column_border = False
        style = 'bottom'
        'Test top and bottom borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE, 'Example')
        table.inner_column_border = inner_column_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'top' and inner_column_border:
            expected = '+Example--+-------+-----------+'
        elif style == 'top':
            expected = '+Example--------------------+'
        elif style == 'bottom' and inner_column_border:
            expected = '+---------+-------+-----------+'
        else:
            expected = '+---------------------------+'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_True_True_heading(self):
        inner_column_border = True
        outer_border = True
        style = 'heading'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_True_True_footing(self):
        inner_column_border = True
        outer_border = True
        style = 'footing'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_True_False_heading(self):
        inner_column_border = True
        outer_border = False
        style = 'heading'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_True_False_footing(self):
        inner_column_border = True
        outer_border = False
        style = 'footing'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_False_True_heading(self):
        inner_column_border = False
        outer_border = True
        style = 'heading'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_False_True_footing(self):
        inner_column_border = False
        outer_border = True
        style = 'footing'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_False_False_heading(self):
        inner_column_border = False
        outer_border = False
        style = 'heading'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_heading_footing_False_False_footing(self):
        inner_column_border = False
        outer_border = False
        style = 'footing'
        'Test heading and footing borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    :param str style: Passed to method.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if style == 'heading' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        elif style == 'heading':
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        elif style == 'footing' and outer_border:
            expected = '+---------+-------+-----------+' if inner_column_border else '+---------------------------+'
        else:
            expected = '---------+-------+-----------' if inner_column_border else '---------------------------'
        actual = ''.join(table.horizontal_border(style, outer_widths))
        self.assertEqual(actual, expected)

    def test_row_True_True(self):
        inner_column_border = True
        outer_border = True
        'Test inner borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if inner_column_border and outer_border:
            expected = '+---------+-------+-----------+'
        elif inner_column_border:
            expected = '---------+-------+-----------'
        elif outer_border:
            expected = '+---------------------------+'
        else:
            expected = '---------------------------'
        actual = ''.join(table.horizontal_border('row', outer_widths))
        self.assertEqual(actual, expected)

    def test_row_True_False(self):
        inner_column_border = True
        outer_border = False
        'Test inner borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if inner_column_border and outer_border:
            expected = '+---------+-------+-----------+'
        elif inner_column_border:
            expected = '---------+-------+-----------'
        elif outer_border:
            expected = '+---------------------------+'
        else:
            expected = '---------------------------'
        actual = ''.join(table.horizontal_border('row', outer_widths))
        self.assertEqual(actual, expected)

    def test_row_False_True(self):
        inner_column_border = False
        outer_border = True
        'Test inner borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if inner_column_border and outer_border:
            expected = '+---------+-------+-----------+'
        elif inner_column_border:
            expected = '---------+-------+-----------'
        elif outer_border:
            expected = '+---------------------------+'
        else:
            expected = '---------------------------'
        actual = ''.join(table.horizontal_border('row', outer_widths))
        self.assertEqual(actual, expected)

    def test_row_False_False(self):
        inner_column_border = False
        outer_border = False
        'Test inner borders.\n\n    :param bool inner_column_border: Passed to table class.\n    :param bool outer_border: Passed to table class.\n    '
        table = BaseTable(SINGLE_LINE)
        table.inner_column_border = inner_column_border
        table.outer_border = outer_border
        outer_widths = max_dimensions(table.table_data, table.padding_left, table.padding_right)[2]
        if inner_column_border and outer_border:
            expected = '+---------+-------+-----------+'
        elif inner_column_border:
            expected = '---------+-------+-----------'
        elif outer_border:
            expected = '+---------------------------+'
        else:
            expected = '---------------------------'
        actual = ''.join(table.horizontal_border('row', outer_widths))
        self.assertEqual(actual, expected)