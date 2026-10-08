# PROJECT NAME: envier-test_validators

# FOLDER STRUCTURE:
```
..
└── envier/
    ├── env.py
    │   ├── DerivedVariable.DerivedVariable
    │   ├── Env.__init__
    │   ├── Env.v
    │   ├── EnvVariable.EnvVariable
    │   ├── EnvVariable.__call__
    │   ├── EnvVariable.__init__
    │   └── _normalized
    └── validators.py
        ├── choice
        └── range
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
The module provides a robust configuration management system that validates environment variables based on predefined rules and constraints, ensuring reliability and correctness in application settings. It enables developers to define required and optional environment variables with specific value constraints, such as allowable choices or numerical ranges. By enforcing these validations, the module prevents invalid configurations at runtime, reducing potential errors and improving application stability. This solution streamlines the handling of environment-specific configurations, allowing developers to enforce stricter controls and enhance the overall quality of their services.

## FILE 1: envier/env.py

- FUNCTION NAME: _normalized
  - SIGNATURE: def _normalized(name: str) -> str:
  - DOCSTRING: 
```python
"""
Normalize a given string by transforming it to uppercase, replacing any dots (`.`) with underscores (`_`), and trimming any trailing underscores from the end.

Parameters:
- name (str): The string to be normalized.

Returns:
- str: The normalized string in uppercase with dots replaced by underscores and trailing underscores removed.

This function is used within the context of environment variable handling, specifically in the `EnvVariable` class, to ensure consistent naming conventions when accessing environment variables. It helps in standardizing variable names before they are used to form keys for environment sources, thereby facilitating easier management and retrieval of configuration settings.
"""
```
  - DEPENDENCIES:
    - envier/env.py:Env:__init__

- CLASS METHOD: EnvVariable.__call__
  - CLASS SIGNATURE: class EnvVariable(t.Generic[T]):
  - SIGNATURE: def __call__(self, env: 'Env', prefix: str) -> T:
  - DOCSTRING: 
```python
"""
Calls the `EnvVariable` instance to retrieve its value from the specified environment (`env`) using the given prefix for name resolution. It first invokes the `_retrieve` method to get the raw value associated with the environment variable. If a validator function is provided, it checks the validity of the retrieved value, raising a `ValueError` if the value is deemed invalid. This method ultimately returns the parsed and possibly validated value of the environment variable.

Parameters:
- env (Env): An instance of the `Env` class from which to fetch the variable value.
- prefix (str): A namespace prefix used to construct the full name of the environment variable.

Returns:
- T: The value of the environment variable, potentially transformed by a parser or validator.

The method interacts with the `_retrieve` method of the `EnvVariable` class to handle the process of fetching and validating the environment variable's value. The `full_name` property is used to construct the formatted name for retrieving the variable from the environment. Should validator raise a `ValueError`, it is wrapped with additional context about the environment variable in the raised exception.
"""
```

- CLASS METHOD: Env.v
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def v(cls, type: t.Union[object, t.Type[T]], name: str, parser: t.Optional[t.Callable[[str], T]]=None, validator: t.Optional[t.Callable[[T], None]]=None, map: t.Optional[MapType]=None, default: t.Union[T, NoDefaultType]=NoDefault, deprecations: t.Optional[t.List[DeprecationInfo]]=None, private: bool=False, help: t.Optional[str]=None, help_type: t.Optional[str]=None, help_default: t.Optional[str]=None) -> EnvVariable[T]:
  - DOCSTRING: 
```python
"""
Add an environment variable mapping to the Env class.

This method allows the declaration of an environment variable for the Env class, associating it with a specified type, name, and optional processing functions (parser, validator, and map). It also supports default values, deprecation notes, and help documentation, which can facilitate user understanding of the variable's purpose.

Parameters:
- type (Union[object, Type[T]]): The expected data type of the environment variable.
- name (str): The name of the environment variable.
- parser (Optional[Callable[[str], T]]): An optional function to parse the raw string value into the desired type.
- validator (Optional[Callable[[T], None]]): An optional function to validate the parsed value.
- map (Optional[MapType]): An optional mapping function to convert environment values.
- default (Union[T, NoDefaultType]): A default value, which must match the specified type if provided.
- deprecations (Optional[List[DeprecationInfo]]): An optional list of tuples containing deprecation info for this variable.
- private (bool): A flag indicating if the environment variable should be considered private.
- help (Optional[str]): A help message describing the variable.
- help_type (Optional[str]): A string specifying the expected type for help purposes.
- help_default (Optional[str]): A help string indicating the default value.

Returns:
- EnvVariable[T]: An instance of EnvVariable, encapsulating the defined environment variable.

This method interacts with the EnvVariable class, which is responsible for handling the specified configurations and retrieving values from the environment based on defined types and properties.
"""
```

- CLASS METHOD: EnvVariable.__init__
  - CLASS SIGNATURE: class EnvVariable(t.Generic[T]):
  - SIGNATURE: def __init__(self, type: t.Union[object, t.Type[T]], name: str, parser: t.Optional[t.Callable[[str], T]]=None, validator: t.Optional[t.Callable[[T], None]]=None, map: t.Optional[MapType]=None, default: t.Union[T, NoDefaultType]=NoDefault, deprecations: t.Optional[t.List[DeprecationInfo]]=None, private: bool=False, help: t.Optional[str]=None, help_type: t.Optional[str]=None, help_default: t.Optional[str]=None) -> None:
  - DOCSTRING: 
```python
"""
Initialize an instance of the EnvVariable class, which represents an environment variable mapping in the Env configuration system.

Parameters:
- type (Union[object, Type[T]]): The expected type of the environment variable's value.
- name (str): The name of the environment variable.
- parser (Optional[Callable[[str], T]]): A function to parse the raw string value from the environment.
- validator (Optional[Callable[[T], None]]): A function to validate the parsed value.
- map (Optional[MapType]): A function or callable used to transform the parsed values.
- default (Union[T, NoDefaultType]): The default value if the environment variable is not set. Defaults to NoDefault.
- deprecations (Optional[List[DeprecationInfo]]): A list of tuples containing deprecation information.
- private (bool): A flag indicating if the variable should be treated as private. Default is False.
- help (Optional[str]): A help string describing the environment variable.
- help_type (Optional[str]): A description of the variable's type for documentation purposes.
- help_default (Optional[str]): A description of the default value for documentation purposes.

Raises:
- TypeError: If the default value does not match the expected type.
- KeyError: If a mandatory environment variable is not set and no default is provided.

The method uses the _normalized function to convert the variable name into a consistent format. It also checks for the expected type against a possible Union type and initializes several instance attributes to manage parsing, validation, and help documentation for the environment variable, enhancing the interaction with the broader Env class for managing configuration.
"""
```

- CLASS METHOD: Env.__init__
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def __init__(self, source: t.Optional[t.Dict[str, str]]=None, parent: t.Optional['Env']=None, dynamic: t.Optional[t.Dict[str, str]]=None) -> None:
  - DOCSTRING: 
```python
"""
Initialize an environment variable configuration instance.

This constructor sets up the environment object by configuring the source of environment variables, hierarchical relationships with a parent environment, and dynamically adjustable key-value pairs. It normalizes variable names based on the class prefix and initializes all declared `EnvVariable` and `DerivedVariable` instances. 

Parameters:
- `source` (Optional[Dict[str, str]]): A dictionary providing the source of environment variables. If not provided, defaults to `os.environ`.
- `parent` (Optional[Env]): An optional parent `Env` instance which can provide context and prefixed variable names.
- `dynamic` (Optional[Dict[str, str]]): An optional dictionary of dynamic values, where keys are strings and values are strings, used for any variable name substitution.

Side Effects:
- Sets up the full prefixed naming convention for environment variables.
- Instantiates any declared `EnvVariable` and `DerivedVariable` in the environment, ensuring they are configured correctly against the source and parent.

Constants:
- The `__prefix__` class attribute is used to create a normalized full prefix for variable names by replacing dots with underscores. The normalization process is handled by the `_normalized` function.
- The `derived` list temporarily holds any derived variable definitions for later instantiation, ensuring that all variables are correctly initialized upon creation of the `Env` instance.
"""
```

## FILE 2: envier/validators.py

- FUNCTION NAME: choice
  - SIGNATURE: def choice(choices: t.Iterable) -> t.Callable[[T], None]:
  - DOCSTRING: 
```python
"""
A validator function that checks if a given value is one of the specified choices. 

Parameters:
- choices (t.Iterable): An iterable collection of valid options that the value can be compared against.

Returns:
- A callable (validate function) that takes a single argument (value) and raises a ValueError if the value is not one of the choices or is None.

This function relies on the generic type variable T from the typing module, which allows the validation to be applicable to any type of value provided by the user.
"""
```

- FUNCTION NAME: range
  - SIGNATURE: def range(min_value: int, max_value: int) -> t.Callable[[T], None]:
  - DOCSTRING: 
```python
"""
Validates that a given value lies within a specified range.

Parameters:
- min_value (int): The minimum acceptable value (inclusive).
- max_value (int): The maximum acceptable value (inclusive).

Returns:
- Callable[[T], None]: A validation function that checks if the input value is within the given range. If the value is outside the range or not an integer, a ValueError is raised with a descriptive message.

Notes:
- This function is intended to be used as a decorator or a validator for other functions that require input validation.
- It handles `None` values gracefully; if the input value is `None`, no validation is performed.
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
