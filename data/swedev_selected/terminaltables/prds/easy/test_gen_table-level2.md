# PROJECT NAME: terminaltables-test_gen_table

# FOLDER STRUCTURE:
```
..
└── terminaltables3/
    ├── base_table.py
    │   ├── BaseTable.__init__
    │   └── BaseTable.gen_table
    ├── build.py
    │   ├── combine
    │   └── flatten
    └── width_and_alignment.py
        ├── max_dimensions
        └── visible_width
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
The module facilitates the creation, configuration, and rendering of text-based tables for terminal output. It provides functionality to define table data, control the presence and styling of inner and outer borders, and adjust padding and alignment to produce visually distinct and customizable tabular displays. With configurable options such as row borders, heading/footing styles, and flexible data formats, the module addresses the need for clear and precise text representations of organized data in terminal environments. It simplifies the process for developers to render well-structured tables, enhancing readability and user experience in command-line applications.

## FILE 1: terminaltables3/build.py

- FUNCTION NAME: combine
  - SIGNATURE: def combine(line: Union[Generator[Union[int, str], None, None], Iterator[Optional[Union[int, str]]]], left: str, intersect: Optional[str], right: str) -> Generator[int, None, None]:
  - DOCSTRING: 
```python
"""
Combine items in a sequence into a single string with specified borders and separators, primarily for formatting tables.

:param Union[Generator[Union[int, str], None, None], Iterator[Optional[Union[int, str]]] line: A sequence of items to be combined, which can be a generator or an iterator. 
:param str left: The string to be placed at the beginning of the combined output (left border).
:param Optional[str] intersect: A separator string to be placed between items in the sequence if multiple items are yielded.
:param str right: The string to be placed at the end of the combined output (right border).

:return: A generator yielding the combined items in the specified format.

The function is designed to format table rows by combining cell contents with specified borders and column separators, making it useful in conjunction with other functions that manage table layouts, such as `build_border` and `build_row`. It handles different types of input sequences and gracefully manages cases where the input is empty.
"""
```
  - DEPENDENCIES:
    - terminaltables3/build.py:flatten

- FUNCTION NAME: flatten
  - SIGNATURE: def flatten(table):
  - DOCSTRING: 
```python
"""
Flatten a table data structure into a single string, with each row separated by a newline character.

:param iter table: A padded and bordered table data, represented as an iterable of rows, where each row is a list of strings.

:return: A single string that concatenates all the rows and cells of the table, separated by newlines.
:rtype: str

This function utilizes nested joins to construct the final string representation of the table, relying on the layout provided by previous functions in the code, such as `build_row` and `combine`, which handle the construction of the individual rows and cell combinations.
"""
```
  - DEPENDENCIES:
    - terminaltables3/base_table.py:BaseTable:gen_table
    - terminaltables3/build.py:combine

## FILE 2: terminaltables3/width_and_alignment.py

- FUNCTION NAME: max_dimensions
  - SIGNATURE: def max_dimensions(table_data, padding_left=0, padding_right=0, padding_top=0, padding_bottom=0):
  - DOCSTRING: 
```python
"""
Get the maximum widths of each column and heights of each row in a table, considering specified padding.

:param iter table_data: A list of lists containing strings representing the unmodified table data.
:param int padding_left: The number of space characters to add to the left side of each cell.
:param int padding_right: The number of space characters to add to the right side of each cell.
:param int padding_top: The number of empty lines to add above each cell.
:param int padding_bottom: The number of empty lines to add below each cell.

:return: A tuple containing four lists: the maximum inner widths of each column, the maximum heights of each row, and their respective outer widths and heights after applying padding.
:rtype: tuple

The function uses the `visible_width` helper function to account for varying widths of Unicode characters. It determines inner dimensions based on the longest string within each column and the highest string in each row, and then computes outer dimensions by adding the provided padding values.
"""
```
  - DEPENDENCIES:
    - terminaltables3/width_and_alignment.py:visible_width

- FUNCTION NAME: visible_width
  - SIGNATURE: def visible_width(string: str) -> int:
  - DOCSTRING: 
```python
"""
Calculate the visible width of a Unicode string, accounting for varying widths of characters, including CJK characters that may occupy more space than ASCII characters. 

:param str string: The input string whose visible width is to be measured. This string may contain ANSI color codes, which will be stripped before calculation.
:return: The number of visible characters in the string, accounting for multi-byte characters.
:rtype: int

The function utilizes the `RE_COLOR_ANSI` regular expression, defined globally, to identify and remove ANSI color codes from the input string. It leverages `unicodedata.east_asian_width` to determine the width of individual characters, categorizing them as wide or narrow. The resulting width is used to accommodate proper text alignment in terminal-based applications.
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
Initialize a BaseTable instance with optional title and table data.

This constructor sets up the table's structural properties including borders, padding, 
and column justification. The `table_data` parameter is a sequence of sequences, where each 
inner sequence represents a row of the table containing strings. The `title` parameter, 
if provided, will be displayed at the top of the table.

Attributes:
    table_data (Sequence[Sequence[str]]): List representing the table's content, can be empty.
    title (Optional[str]): Title displayed within the top border.
    inner_column_border (bool): Indicates if there are vertical borders between columns (default: True).
    inner_footing_row_border (bool): Indicates if a border appears above the last row (default: False).
    inner_heading_row_border (bool): Indicates if a border appears below the first row (default: True).
    inner_row_border (bool): Indicates if borders appear between every row (default: False).
    outer_border (bool): Indicates if the outer border is displayed (default: True).
    justify_columns (dict): Maps column indices to justification types (e.g., 'left', 'right', 'center').
    padding_left (int): Number of spaces to pad on the left side of every cell (default: 1).
    padding_right (int): Number of spaces to pad on the right side of every cell (default: 1).

No return value.
"""
```

- CLASS METHOD: BaseTable.gen_table
  - CLASS SIGNATURE: class BaseTable:
  - SIGNATURE: def gen_table(self, inner_widths: Sequence[int], inner_heights: Sequence[int], outer_widths: Sequence[int]) -> Generator[Tuple[str, ...], None, None]:
  - DOCSTRING: 
```python
"""
Combine all elements to generate the entire table as a sequence of strings, including borders and cell data.

This method iterates through the table's data to yield formatted lines for each row. Based on the configuration
of the class, it adds borders around the table and between rows. The parameters define the widths and heights of the
columns and rows, ensuring proper alignment and padding. It relies on helper methods like horizontal_border and 
gen_row_lines for structure and formatting.

:param inner_widths: A sequence of integers representing the widths (without padding) for each column.
:param inner_heights: A sequence of integers indicating the heights (without padding) for each row.
:param outer_widths: A sequence of integers for the widths (with padding) for each column, used in building borders.
:yield: Yields tuples of strings, each representing a line of the table, ready to be printed.
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
