# PROJECT NAME: terminaltables-test_horizontal_border

# FOLDER STRUCTURE:
```
..
└── terminaltables3/
    ├── base_table.py
    │   ├── BaseTable.__init__
    │   └── BaseTable.horizontal_border
    ├── build.py
    │   ├── build_border
    │   └── combine
    └── width_and_alignment.py
        ├── max_dimensions
        └── visible_width
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
The module is designed to validate the rendering and formatting functionality of the `BaseTable` class used for creating visually structured tables with configurable borders and alignment. It ensures that horizontal borders, including top, bottom, heading, footing, and row dividers, are rendered correctly based on configurable parameters such as inner column borders, outer borders, and style variations. By simulating different table configurations and asserting against expected outputs, the module provides robust verification of table layout behavior, addressing the need for consistent and precise table formatting in text-based interfaces. This reliability simplifies table integration for developers, reducing errors and ensuring predictable display formatting across use cases.

## FILE 1: terminaltables3/build.py

- FUNCTION NAME: build_border
  - SIGNATURE: def build_border(outer_widths: Sequence[int], horizontal: str, left: str, intersect: str, right: str, title: Optional[str]=None):
  - DOCSTRING: 
```python
"""
Builds a border for a table, optionally embedding a title within it, while ensuring the title fits between the border characters. 

Parameters:
- outer_widths (Sequence[int]): List of widths for each column, including padding.
- horizontal (str): Character used to stretch across each column.
- left (str): Character for the left border of the table.
- intersect (str): Character used as a column separator.
- right (str): Character for the right border of the table.
- title (Optional[str]): The title to overlay on the border; hidden if it doesn't fit.

Returns:
Generator[str]: Yields strings representing the constructed border, including the title if applicable.

Dependencies:
This function uses the `visible_width` function from the `terminaltables3.width_and_alignment` module to determine the visual width of the title. The `combine` function is used to construct the sections of the border based on the provided parameters and width settings.
"""
```
  - DEPENDENCIES:
    - terminaltables3/width_and_alignment.py:visible_width
    - terminaltables3/base_table.py:BaseTable:horizontal_border

- FUNCTION NAME: combine
  - SIGNATURE: def combine(line: Union[Generator[Union[int, str], None, None], Iterator[Optional[Union[int, str]]]], left: str, intersect: Optional[str], right: str) -> Generator[int, None, None]:
  - DOCSTRING: 
```python
"""
Combine multiple elements from an iterable (line) into a formatted string with specified borders and separators. This function is designed to create table-like structures in a readable format by yielding a sequence of combined items from the line along with specified left and right borders and an optional column separator.

Parameters:
- line: A generator or iterator yielding items to be combined (can be integers or strings).
- left: A string representing the left border of the combined row.
- intersect: An optional string representing the separator between items in the line.
- right: A string representing the right border of the combined row.

Returns:
- A generator yielding combined string elements that include the left border, items from the line, the intersect (if provided), and the right border.

This function interacts with other parts of the code, such as 'build_border' and 'build_row', to facilitate the creation of table structures. The 'combine' function is critical for ensuring that the output maintains a consistent visual format.
"""
```

## FILE 2: terminaltables3/width_and_alignment.py

- FUNCTION NAME: max_dimensions
  - SIGNATURE: def max_dimensions(table_data, padding_left=0, padding_right=0, padding_top=0, padding_bottom=0):
  - DOCSTRING: 
```python
"""
Get the maximum widths of each column and the maximum height of each row in a provided table data structure, considering optional padding on all sides.

:param iter table_data: A list of lists containing unmodified strings that represent table data.
:param int padding_left: The number of space characters for padding on the left side of each cell.
:param int padding_right: The number of space characters for padding on the right side of each cell.
:param int padding_top: The number of empty lines for padding on the top side of each cell.
:param int padding_bottom: The number of empty lines for padding on the bottom side of each cell.

:return: A tuple containing four lists: inner column widths, inner row heights, outer column widths (including padding), and outer row heights (including padding).
:rtype: tuple

This function calculates dimensions based on the visible width of each string, handled by the `visible_width` function, ensuring correct display in terminal applications where column width and row height can vary. It accommodates both multiline cells and the presence of padding.
"""
```
  - DEPENDENCIES:
    - terminaltables3/width_and_alignment.py:visible_width

- FUNCTION NAME: visible_width
  - SIGNATURE: def visible_width(string: str) -> int:
  - DOCSTRING: 
```python
"""
Get the visible width of a Unicode string, accounting for East Asian characters that take up more space than standard ASCII characters. This function removes ANSI color codes using the RE_COLOR_ANSI regular expression to ensure accurate width calculations. 

:param str string: The UTF-8 encoded string whose visible width is to be measured. 
:return: The calculated width of the string, represented as an integer. 
:rtype: int

The function utilizes the `unicodedata.east_asian_width` method to determine the width contribution of each character in the string. It also handles potential decoding errors by attempting to process the string as UTF-8 with error handling for unsupported formats. The presence of ANSI escape sequences is managed by removing them before width evaluation to prevent miscalculating the string width due to color formatting.
"""
```
  - DEPENDENCIES:
    - terminaltables3/width_and_alignment.py:max_dimensions

## FILE 3: terminaltables3/base_table.py

- CLASS METHOD: BaseTable.__init__
  - CLASS SIGNATURE: class BaseTable:
  - SIGNATURE: def __init__(self, table_data: Sequence[Sequence[str]], title: Optional[str]=None):
  - DOCSTRING: 
```python
"""
Initialize a BaseTable instance with specified table data and optional title.

:param table_data: A sequence (list of lists) containing string representations of the table's rows and columns. It can be empty or any combination of strings.
:param title: An optional string to display as a title at the top of the table.

This constructor sets up several attributes for customizing the table's appearance:
- `inner_column_border`: (bool) Indicates whether to show borders between columns (default: True).
- `inner_footing_row_border`: (bool) Indicates whether to show a border before the last row (default: False).
- `inner_heading_row_border`: (bool) Indicates whether to show a border after the first row (default: True).
- `inner_row_border`: (bool) Indicates whether to show borders between rows (default: False).
- `outer_border`: (bool) Indicates whether to display outer borders around the table (default: True).
- `justify_columns`: (dict) Allows for setting horizontal justification for columns (default: empty).
- `padding_left`: (int) Number of spaces to pad on the left side of each cell (default: 1).
- `padding_right`: (int) Number of spaces to pad on the right side of each cell (default: 1).

The constants defined at the class level, such as `CHAR_F_INNER_HORIZONTAL`, are utilized by other class methods to construct and render borders, affecting the visual representation of the table.
"""
```

- CLASS METHOD: BaseTable.horizontal_border
  - CLASS SIGNATURE: class BaseTable:
  - SIGNATURE: def horizontal_border(self, style: str, outer_widths: Sequence[int]) -> Tuple[str, ...]:
  - DOCSTRING: 
```python
"""
Builds a horizontal border for the table based on the specified style. The style parameter determines the type of border to create (e.g., top, bottom, heading, footing, or row). The outer_widths parameter specifies the widths of each column, including padding.

Parameters:
- style (str): The type of border to return.
- outer_widths (Sequence[int]): A list of widths for each column.

Returns:
- Tuple[str, ...]: Prepared border as a tuple of strings.

This method utilizes several constants defined within the BaseTable class to determine the characters used for the border, including CHAR_OUTER_TOP_HORIZONTAL, CHAR_OUTER_BOTTOM_HORIZONTAL, CHAR_H_INNER_HORIZONTAL, and others. The method also checks the instance attributes inner_column_border and outer_border to determine the presence of intersection and vertical border characters.
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
