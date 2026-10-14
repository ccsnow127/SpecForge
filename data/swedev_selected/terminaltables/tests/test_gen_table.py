import unittest
from main import BaseTable
from main import flatten
from main import max_dimensions
'Test method in BaseTable class.'

class GeneratedTestCase(unittest.TestCase):

    def test_inner_row_borders_True_True_True(self):
        inner_heading_row_border = True
        inner_footing_row_border = True
        inner_row_border = True
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_inner_row_borders_True_True_False(self):
        inner_heading_row_border = True
        inner_footing_row_border = True
        inner_row_border = False
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_inner_row_borders_True_False_True(self):
        inner_heading_row_border = True
        inner_footing_row_border = False
        inner_row_border = True
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_inner_row_borders_True_False_False(self):
        inner_heading_row_border = True
        inner_footing_row_border = False
        inner_row_border = False
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_inner_row_borders_False_True_True(self):
        inner_heading_row_border = False
        inner_footing_row_border = True
        inner_row_border = True
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_inner_row_borders_False_True_False(self):
        inner_heading_row_border = False
        inner_footing_row_border = True
        inner_row_border = False
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_inner_row_borders_False_False_True(self):
        inner_heading_row_border = False
        inner_footing_row_border = False
        inner_row_border = True
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_inner_row_borders_False_False_False(self):
        inner_heading_row_border = False
        inner_footing_row_border = False
        inner_row_border = False
        'Test heading/footing/row borders.\n\n    :param bool inner_heading_row_border: Passed to table.\n    :param bool inner_footing_row_border: Passed to table.\n    :param bool inner_row_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data)
        table.inner_heading_row_border = inner_heading_row_border
        table.inner_footing_row_border = inner_footing_row_border
        table.inner_row_border = inner_row_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if inner_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n+---------+-------+-----------+\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border and inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_heading_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        elif inner_footing_row_border:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n+---------+-------+-----------+\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = '+---------+-------+-----------+\n| Name    | Color | Type      |\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        self.assertEqual(actual, expected)

    def test_outer_borders_True(self):
        outer_border = True
        'Test left/right/top/bottom table borders.\n\n    :param bool outer_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data, 'Example Table')
        table.outer_border = outer_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if outer_border:
            expected = '+Example Table----+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = ' Name    | Color | Type      \n---------+-------+-----------\n Avocado | green | nut       \n Tomato  | red   | fruit     \n Lettuce | green | vegetable '
        self.assertEqual(actual, expected)

    def test_outer_borders_False(self):
        outer_border = False
        'Test left/right/top/bottom table borders.\n\n    :param bool outer_border: Passed to table.\n    '
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable']]
        table = BaseTable(table_data, 'Example Table')
        table.outer_border = outer_border
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if outer_border:
            expected = '+Example Table----+-----------+\n| Name    | Color | Type      |\n+---------+-------+-----------+\n| Avocado | green | nut       |\n| Tomato  | red   | fruit     |\n| Lettuce | green | vegetable |\n+---------+-------+-----------+'
        else:
            expected = ' Name    | Color | Type      \n---------+-------+-----------\n Avocado | green | nut       \n Tomato  | red   | fruit     \n Lettuce | green | vegetable '
        self.assertEqual(actual, expected)

    def test_one_no_rows_row_False(self):
        mode = 'row'
        bare = False
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_row_True(self):
        mode = 'row'
        bare = True
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_one_False(self):
        mode = 'one'
        bare = False
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_one_True(self):
        mode = 'one'
        bare = True
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_blank_False(self):
        mode = 'blank'
        bare = False
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_blank_True(self):
        mode = 'blank'
        bare = True
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_empty_False(self):
        mode = 'empty'
        bare = False
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_empty_True(self):
        mode = 'empty'
        bare = True
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_none_False(self):
        mode = 'none'
        bare = False
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)

    def test_one_no_rows_none_True(self):
        mode = 'none'
        bare = True
        'Test with one or no rows.\n\n    :param str mode: Type of table contents to test.\n    :param bool bare: Disable padding/borders.\n    '
        if mode == 'row':
            table_data = [['Avocado', 'green', 'nut']]
        elif mode == 'one':
            table_data = [['Avocado']]
        elif mode == 'blank':
            table_data = [['']]
        elif mode == 'empty':
            table_data = [[]]
        else:
            table_data = []
        table = BaseTable(table_data)
        if bare:
            table.inner_column_border = False
            table.inner_footing_row_border = False
            table.inner_heading_row_border = False
            table.inner_row_border = False
            table.outer_border = False
            table.padding_left = 0
            table.padding_right = 0
        (inner_widths, inner_heights, outer_widths) = max_dimensions(table_data, table.padding_left, table.padding_right)[:3]
        actual = flatten(table.gen_table(inner_widths, inner_heights, outer_widths))
        if mode == 'row':
            if bare:
                expected = 'Avocadogreennut'
            else:
                expected = '+---------+-------+-----+\n| Avocado | green | nut |\n+---------+-------+-----+'
        elif mode == 'one':
            if bare:
                expected = 'Avocado'
            else:
                expected = '+---------+\n| Avocado |\n+---------+'
        elif mode == 'blank':
            if bare:
                expected = ''
            else:
                expected = '+--+\n|  |\n+--+'
        elif mode == 'empty':
            if bare:
                expected = ''
            else:
                expected = '++\n||\n++'
        elif bare:
            expected = ''
        else:
            expected = '++\n++'
        self.assertEqual(actual, expected)