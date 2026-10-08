# PROJECT NAME: pathvalidate-test_ltsv

# FOLDER STRUCTURE:
```
..
└── pathvalidate/
    ├── _ltsv.py
    │   ├── sanitize_ltsv_label
    │   └── validate_ltsv_label
    └── error.py
        └── ValidationError.reason
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
This module is designed to validate and sanitize labels used in Labeled Tab-Separated Values (LTSV) files, ensuring compliance with accepted character standards. It provides functionality to check labels for invalid characters and raises specific errors when violations are detected, as well as methods to automatically sanitize labels by replacing or removing disallowed characters. By enforcing strict validation and sanitation of labels, the module prevents formatting issues and ensures compatibility with systems handling LTSV data. This simplifies the process for developers by automating compliance checks and corrections, reducing potential errors and enhancing data integrity.

## FILE 1: pathvalidate/_ltsv.py

- FUNCTION NAME: sanitize_ltsv_label
  - SIGNATURE: def sanitize_ltsv_label(label: str, replacement_text: str='') -> str:
  - DOCSTRING: 
```python
"""
Sanitizes a given Labeled Tab-separated Values (LTSV) label by replacing all invalid characters with a specified replacement string.

:param label: The input label string to sanitize. It must not contain whitespace characters.
:param replacement_text: The string to replace invalid characters with. Defaults to an empty string.
:return: A sanitized label with invalid characters replaced.
:rtype: str

This function validates the label using `validate_pathtype`, ensuring the label does not include any whitespace. The constant `__RE_INVALID_LTSV_LABEL`, defined as a regular expression, identifies characters that are not valid in an LTSV label (including anything that is not a letter, digit, underscore, period, or hyphen). The invalid characters are substituted with `replacement_text` in the returned sanitized label.
"""
```
  - DEPENDENCIES:
    - pathvalidate/_common.py:to_str
    - pathvalidate/_common.py:validate_pathtype

- FUNCTION NAME: validate_ltsv_label
  - SIGNATURE: def validate_ltsv_label(label: str) -> None:
  - DOCSTRING: 
```python
"""
Verifies whether the provided `label` is a valid Labeled Tab-separated Values (LTSV) label. The function checks for invalid characters using a precompiled regular expression, `__RE_INVALID_LTSV_LABEL`, which matches any character that is not a letter, digit, underscore, hyphen, or dot. 

:param label: The label string to validate.
:raises pathvalidate.ValidationError: If the label contains invalid character(s) as defined by LTSV specifications.
:raises InvalidCharError: If invalid characters are found, indicating which characters are not allowed.

This function also calls `validate_pathtype` to ensure the label meets path type validation criteria without allowing whitespaces.
"""
```
  - DEPENDENCIES:
    - pathvalidate/_common.py:to_str
    - pathvalidate/error.py:InvalidCharError:__init__
    - pathvalidate/_common.py:validate_pathtype

## FILE 2: pathvalidate/error.py

- CLASS METHOD: ValidationError.reason
  - CLASS SIGNATURE: class ValidationError(ValueError):
  - SIGNATURE: def reason(self) -> ErrorReason:
  - DOCSTRING: 
```python
"""
@property
def reason(self) -> ErrorReason:
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
