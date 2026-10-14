# PROJECT NAME: vulture-test_config

# FOLDER STRUCTURE:
```
..
└── vulture/
    └── config.py
        ├── _check_input_config
        ├── _parse_args
        ├── _parse_toml
        └── make_config
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
This module is responsible for validating and processing configuration inputs for a software tool, supporting both command-line arguments and TOML configuration files. It ensures that user-defined settings, such as paths to analyze, elements to exclude, and other parameters like verbosity and confidence thresholds, are correctly parsed and merged, with command-line arguments taking precedence over file-based configurations. The module provides robust error handling to abort execution when invalid configurations, unknown keys, or type mismatches are detected. By standardizing and enforcing configuration consistency, it eliminates ambiguities, ensuring seamless integration for users and developers relying on flexible and predictable configuration management.

## FILE 1: vulture/config.py

- FUNCTION NAME: _parse_toml
  - SIGNATURE: def _parse_toml(infile):
  - DOCSTRING: 
```python
"""
Parse a TOML file for configuration values specific to the Vulture tool.

This function reads a TOML file and retrieves configuration settings defined 
under the `[tool.vulture]` section. It expects the keys to match the command-line 
arguments available for configuring the Vulture tool. The configuration settings 
are validated against predefined defaults in the `DEFAULTS` dictionary using the 
`_check_input_config` function. The function raises an `InputError` if any keys 
are unknown or if their types do not match expected values.

Parameters:
- infile: A file-like object containing the TOML data to be parsed.

Returns:
A dictionary containing the configuration settings retrieved from the TOML file.

Uses:
- `tomllib`: For loading TOML data.
- `DEFAULTS`: Defined at the module level to specify valid configuration keys 
  and their default values. These constants help in ensuring the integrity of 
  the configuration values.
"""
```
  - DEPENDENCIES:
    - vulture/config.py:_check_input_config

- FUNCTION NAME: _parse_args
  - SIGNATURE: def _parse_args(args=None):
  - DOCSTRING: 
```python
"""
Parse command-line arguments for the Vulture application.

This function utilizes Python's `argparse` module to define and process command-line options and their associated values. The parsed arguments include file paths, exclusion patterns, decorators to ignore, and configuration options. It validates the input against predefined configuration defaults found in the `DEFAULTS` dictionary.

Parameters:
- args (list of str, optional): A list of strings representing the command-line arguments. By default, the function operates on `sys.argv`.

Returns:
- dict: A dictionary of parsed command-line arguments with values that are not missing. The keys in this dictionary correspond to the options defined in the arguments.

Raises:
- InputError: If any of the parsed arguments do not conform to the expected types as defined in the `DEFAULTS` dictionary.

Constants:
- `missing`: A unique sentinel object used to differentiate between arguments explicitly set to `False` and those that were not provided.
- `usage`: A string defining the usage message for the command-line interface.
- `version`: A string containing the current version of the Vulture application, imported from the module-level `__version__`.
- `glob_help`: A string indicating that the patterns may contain glob wildcards, which is included in the help messages for relevant arguments.
"""
```
  - DEPENDENCIES:
    - vulture/config.py:csv
    - vulture/config.py:_check_input_config

- FUNCTION NAME: make_config
  - SIGNATURE: def make_config(argv=None, tomlfile=None):
  - DOCSTRING: 
```python
"""
Returns a configuration object for the Vulture static code analysis tool by merging settings from a TOML configuration file (typically `pyproject.toml`) and command-line arguments. The command-line arguments take precedence over the TOML file values.

:param argv: A list of command-line arguments to be parsed. If not specified, defaults to `sys.argv`.
:param tomlfile: An IO instance containing TOML data for testing purposes. If not provided, the function attempts to locate and read `pyproject.toml`.

:returns: A dictionary containing configuration settings.

This function utilizes the constant `DEFAULTS`, which defines the default values for various configuration options and is used to fill any missing settings in the final configuration. It also leverages helper functions such as `_parse_args` for command-line parsing and `_parse_toml` for reading TOML files. After merging configurations, it verifies the output configuration with the `_check_output_config` function to ensure that essential settings are provided.
"""
```
  - DEPENDENCIES:
    - vulture/config.py:_check_output_config
    - vulture/config.py:_parse_args
    - vulture/config.py:_parse_toml

- FUNCTION NAME: _check_input_config
  - SIGNATURE: def _check_input_config(data):
  - DOCSTRING: 
```python
"""
Checks the types of values in the provided configuration *data* against expected types defined in the DEFAULTS dictionary. Raises an InputError if an unknown configuration key is encountered or if a value has the wrong type. 

Parameters:
- data (dict): A dictionary containing configuration key-value pairs to be validated, where keys should match those in DEFAULTS.

Raises:
- InputError: If any key in *data* is not recognized or if the type of a value does not match the expected type.

Dependencies:
- The function relies on the DEFAULTS constant, which defines valid configuration keys along with their expected types. The InputError class is used for raising exceptions related to invalid inputs.
"""
```
  - DEPENDENCIES:
    - vulture/config.py:InputError:__init__

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
