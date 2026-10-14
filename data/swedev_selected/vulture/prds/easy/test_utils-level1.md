# PROJECT NAME: vulture-test_utils

# FOLDER STRUCTURE:
```
..
└── vulture/
    └── utils.py
        ├── format_path
        └── get_decorator_name
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
This module provides functionality to evaluate and process Python source code for extracting and standardizing information about decorators and file paths, primarily for use in code analysis or linting tools. It offers the ability to transform file paths into relative or absolute formats depending on their context and examines Python code to identify and extract fully qualified decorator names applied to functions, methods, or classes. By enabling accurate path formatting and consistent extraction of decorator metadata, the module facilitates better introspection, debugging, or analysis of complex Python projects, solving challenges related to navigating file structures and understanding decorator usage across codebases.

## FILE 1: vulture/utils.py

- FUNCTION NAME: get_decorator_name
  - SIGNATURE: def get_decorator_name(decorator):
  - DOCSTRING: 
```python
"""
Retrieve the fully qualified name of a decorator from its AST node representation.

Parameters:
- decorator (ast.AST): The AST node representing the decorator, which may be an instance of ast.Call or ast.Attribute.

Returns:
- str: The fully qualified name of the decorator, prefixed with '@'. If the decorator is composed of multiple attributes, they are joined with dots in reverse order to form the complete name.

This function relies on the `ast` module to inspect the structure of the decorator node. If the node represents a function call, it extracts the function's name from the call. The function may handle nested attributes (e.g., `@module.Class.method`) by traversing through `ast.Attribute` instances, collecting their attributes until reaching the base identifier.
"""
```

- FUNCTION NAME: format_path
  - SIGNATURE: def format_path(path):
  - DOCSTRING: 
```python
"""
Format a given file path relative to the current working directory.

Parameters:
- path (pathlib.Path): The path to format, which can be either a file or directory path.

Returns:
- pathlib.Path: The relative path to the current working directory if the path is below it; otherwise, returns the original path.

This function utilizes `pathlib.Path.cwd()` to retrieve the current working directory. If the provided path is outside of the current directory, it raises a ValueError, and the original path is returned unmodified.
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
