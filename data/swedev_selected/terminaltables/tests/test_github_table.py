import unittest
from main import GithubFlavoredMarkdownTable
'GithubFlavoredMarkdownTable end to end testing.'

class GeneratedTestCase(unittest.TestCase):

    def test_single_line(self):
        """Test single-lined cells."""
        table_data = [['Name', 'Color', 'Type'], ['Avocado', 'green', 'nut'], ['Tomato', 'red', 'fruit'], ['Lettuce', 'green', 'vegetable'], ['Watermelon', 'green'], []]
        table = GithubFlavoredMarkdownTable(table_data)
        table.inner_footing_row_border = True
        table.justify_columns[0] = 'left'
        table.justify_columns[1] = 'center'
        table.justify_columns[2] = 'right'
        actual = table.table
        expected = '| Name       | Color |      Type |\n|:-----------|:-----:|----------:|\n| Avocado    | green |       nut |\n| Tomato     |  red  |     fruit |\n| Lettuce    | green | vegetable |\n| Watermelon | green |           |\n|            |       |           |'
        self.assertEqual(actual, expected)

    def test_multi_line(self):
        """Test multi-lined cells."""
        table_data = [['Show', 'Characters'], ['Rugrats', 'Tommy Pickles, Chuckie Finster, Phillip DeVille, Lillian DeVille, Angelica Pickles,\nDil Pickles'], ['South Park', 'Stan Marsh, Kyle Broflovski, Eric Cartman, Kenny McCormick']]
        table = GithubFlavoredMarkdownTable(table_data)
        actual = table.table
        expected = '| Show       | Characters                                                                          |\n|------------|-------------------------------------------------------------------------------------|\n| Rugrats    | Tommy Pickles, Chuckie Finster, Phillip DeVille, Lillian DeVille, Angelica Pickles, |\n|            | Dil Pickles                                                                         |\n| South Park | Stan Marsh, Kyle Broflovski, Eric Cartman, Kenny McCormick                          |'
        self.assertEqual(actual, expected)
        table.inner_row_border = True
        actual = table.table
        expected = '| Show       | Characters                                                                          |\n|------------|-------------------------------------------------------------------------------------|\n| Rugrats    | Tommy Pickles, Chuckie Finster, Phillip DeVille, Lillian DeVille, Angelica Pickles, |\n|            | Dil Pickles                                                                         |\n| South Park | Stan Marsh, Kyle Broflovski, Eric Cartman, Kenny McCormick                          |'
        self.assertEqual(actual, expected)
        table.justify_columns = {1: 'right'}
        actual = table.table
        expected = '| Show       |                                                                          Characters |\n|------------|------------------------------------------------------------------------------------:|\n| Rugrats    | Tommy Pickles, Chuckie Finster, Phillip DeVille, Lillian DeVille, Angelica Pickles, |\n|            |                                                                         Dil Pickles |\n| South Park |                          Stan Marsh, Kyle Broflovski, Eric Cartman, Kenny McCormick |'
        self.assertEqual(actual, expected)