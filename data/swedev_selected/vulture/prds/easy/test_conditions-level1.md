# PROJECT NAME: vulture-test_conditions

# FOLDER STRUCTURE:
```
..
├── tests/
│   └── __init__.py
│       └── check_unreachable
└── vulture/
    ├── core.py
    │   ├── Item.size
    │   ├── Vulture.__init__
    │   └── Vulture.scan
    └── utils.py
        ├── condition_is_always_false
        └── condition_is_always_true
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
The module is designed to analyze and validate the logical structure of Python code by identifying unreachable or redundant code patterns, such as dead branches in conditionals or loops that will never be executed. It provides functionality to evaluate the truthfulness of conditions, determine whether certain conditional expressions are always true or false, and flag code that cannot be reached due to logical impossibilities. This helps developers identify and eliminate unnecessary or redundant code, improving code quality, maintainability, and runtime efficiency. By automating the detection of unreachable paths and logical inconsistencies, the module solves the problem of manually inspecting code for these issues, saving time and reducing the likelihood of human error.

## FILE 1: vulture/core.py

- CLASS METHOD: Vulture.scan
  - CLASS SIGNATURE: class Vulture(ast.NodeVisitor):
  - SIGNATURE: def scan(self, code, filename=''):
  - DOCSTRING: 
```python
"""
Scan the provided Python code for unused code elements and potential errors.

Parameters:
- code (str): The source code to be scanned, provided as a string.
- filename (str): The name of the file containing the code, defaulting to an empty string. This is used for logging and error reporting.

This method parses the provided source code into an Abstract Syntax Tree (AST) using the `ast.parse` function. It handles potential syntax errors gracefully by logging them with the relevant line number and error message. The method also processes `noqa` directives to determine lines of code that should be ignored during the scan. Additionally, if the scanning process encounters any `SyntaxError` while visiting nodes, it logs the error accordingly.

Dependencies:
- Utilizes the `noqa` module for parsing ignore comments.
- Makes use of utility functions from the `utils` module for formatting log messages and for handling errors.
- Interacts with other methods of the Vulture class to analyze the scanned nodes to identify dead code.

Constants:
- `self.exit_code`: Updated in case of any errors during the parsing process, indicating the status of the scan operation defined in the ExitCode enumeration.
"""
```

- CLASS METHOD: Item.size
  - CLASS SIGNATURE: class Item:
  - SIGNATURE: def size(self):
  - DOCSTRING: 
```python
"""
Calculate the size of the code item in terms of the number of lines.

This property computes the size as the difference between the last and first line numbers, adding one to ensure the correct count of lines. It asserts that `last_lineno` is greater than or equal to `first_lineno` to prevent any logical errors in size computation. This size is relevant for reporting unused code and is utilized in the `get_report` method of the `Item` class.

Attributes:
- `first_lineno`: The line number where the code item starts, defined during initialization.
- `last_lineno`: The line number where the code item ends, also defined during initialization.
"""
```

- CLASS METHOD: Vulture.__init__
  - CLASS SIGNATURE: class Vulture(ast.NodeVisitor):
  - SIGNATURE: def __init__(self, verbose=False, ignore_names=None, ignore_decorators=None):
  - DOCSTRING: 
```python
"""
Initialize a Vulture instance to analyze Python code for unused elements.

Parameters:
- verbose (bool, optional): A flag to enable verbose logging. Defaults to False.
- ignore_names (list of str, optional): A list of names to ignore during the analysis. Defaults to None.
- ignore_decorators (list of str, optional): A list of decorators to ignore during the analysis. Defaults to None.

Attributes:
- defined_attrs (LoggingList): Tracks defined attributes in the analyzed code.
- defined_classes (LoggingList): Tracks defined classes in the analyzed code.
- defined_funcs (LoggingList): Tracks defined functions in the analyzed code.
- defined_imports (LoggingList): Tracks defined imports in the analyzed code.
- defined_methods (LoggingList): Tracks defined methods in the analyzed code.
- defined_props (LoggingList): Tracks defined properties in the analyzed code.
- defined_vars (LoggingList): Tracks defined variables in the analyzed code.
- unreachable_code (LoggingList): Records unreachable code snippets.
- used_names (LoggingSet): Keeps track of names that are used in the analyzed code.
- filename (Path): Stores the current file path being analyzed.
- code (list of str): Contains the lines of code from the current file.
- exit_code (ExitCode): Holds the exit code for the analysis process, initialized to NoDeadCode.
- noqa_lines (dict): Stores lines that are marked to be excluded from analysis.

Constants used:
- ExitCode: Provides different exit code statuses for the analysis outcome.
- LoggingList and LoggingSet: Utility classes from the utils module that facilitate tracking and logging during analysis. Their verbosity is linked to the `verbose` parameter to control the amount of logging output.
"""
```

## FILE 2: vulture/utils.py

- FUNCTION NAME: condition_is_always_true
  - SIGNATURE: def condition_is_always_true(condition):
  - DOCSTRING: 
```python
"""
Determine if a given condition always evaluates to True.

Parameters:
- condition (ast.AST): An abstract syntax tree (AST) node representing a Boolean expression.

Returns:
- bool: True if the condition evaluates to True for all possible inputs, otherwise False.

This function utilizes the _safe_eval function to evaluate the condition while defaulting to False for any undefined variables or functions. The significance of this is to assess the reliability of the condition in various execution contexts without executing arbitrary code.
"""
```
  - DEPENDENCIES:
    - vulture/utils.py:_safe_eval

- FUNCTION NAME: condition_is_always_false
  - SIGNATURE: def condition_is_always_false(condition):
  - DOCSTRING: 
```python
"""
Determine if a given condition represented as an Abstract Syntax Tree (AST) node is always false.

Parameters:
- condition (ast.AST): An AST node representing a boolean expression to be evaluated.

Returns:
- bool: True if the condition is always false, otherwise False.

This function relies on the `_safe_eval` utility to evaluate the condition safely, substituting for undefined variables or functions with a `True` value. The decision on whether the input condition is always false is based on the negation of the result from `_safe_eval`.
"""
```
  - DEPENDENCIES:
    - vulture/utils.py:_safe_eval

## FILE 3: tests/__init__.py

- FUNCTION NAME: check_unreachable
  - SIGNATURE: def check_unreachable(v, lineno, size, name):
  - DOCSTRING: 
```python
"""
Checks that a single unreachable code item in a Vulture instance matches expected attributes.

Parameters:
- v (core.Vulture): An instance of the Vulture class, which analyzes code for unused functions and variables.
- lineno (int): The expected line number where the unreachable code starts.
- size (int): The expected size of the unreachable code item.
- name (str): The expected name of the unreachable code item.

This function asserts that the Vulture instance has exactly one unreachable code item in its `unreachable_code` attribute, and checks that this item's first line number, size, and name match the provided expected values.
"""
```
  - DEPENDENCIES:
    - vulture/core.py:Item:size

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
