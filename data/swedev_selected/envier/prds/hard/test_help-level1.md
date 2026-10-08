# PROJECT NAME: envier-test_help

# FOLDER STRUCTURE:
```
..
└── envier/
    └── env.py
        ├── Env.help_info
        ├── Env.var
        └── EnvMeta.__new__
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
The module provides a centralized configuration management system designed for applications that rely on environment variables for runtime configuration. It enables developers to define and manage configuration variables with support for default values, type enforcement, and comprehensive metadata such as descriptions and data types. The module also offers an introspection feature that generates structured help documentation, allowing developers or system administrators to quickly understand and validate all required and optional environment variables, including their defaults and purposes. By streamlining the management of configuration values and ensuring their clarity and correctness, the module simplifies the process of deploying and maintaining applications across various environments.

## FILE 1: envier/env.py

- CLASS METHOD: Env.var
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def var(cls, type: t.Type[T], name: str, parser: t.Optional[t.Callable[[str], T]]=None, validator: t.Optional[t.Callable[[T], None]]=None, map: t.Optional[MapType]=None, default: t.Union[T, NoDefaultType]=NoDefault, deprecations: t.Optional[t.List[DeprecationInfo]]=None, private: bool=False, help: t.Optional[str]=None, help_type: t.Optional[str]=None, help_default: t.Optional[str]=None) -> EnvVariable[T]:
  - DOCSTRING: 
```python
"""
Declare a new environment variable for the `Env` subclass.

This method creates an instance of `EnvVariable`, which represents an environment variable that can be parsed, validated, and retrieved within the `Env` context. The variable can be configured with various options such as parsing logic, default values, and deprecation information.

Parameters:
- type (t.Type[T]): The expected type of the environment variable value.
- name (str): The name of the environment variable.
- parser (Optional[t.Callable[[str], T]]): An optional function to parse the raw string value from the environment into the specified type.
- validator (Optional[t.Callable[[T], None]]): An optional function to validate the parsed value.
- map (Optional[MapType]): An optional mapping function for modifying the input values, if dealing with collections.
- default (Union[T, NoDefaultType]): The default value to use if the environment variable is not set. Defaults to `NoDefault`.
- deprecations (Optional[t.List[DeprecationInfo]]): A list of tuples containing deprecation information.
- private (bool): If `True`, the variable is treated as private (prefixing its name with an underscore).
- help (Optional[str]): An optional help text for the variable.
- help_type (Optional[str]): An optional description of the variable's type for help documentation.
- help_default (Optional[str]): An optional description of the default value for help documentation.

Returns:
- EnvVariable[T]: An instance of `EnvVariable` representing the configured environment variable.

The method helps to manage environment variables efficiently and allows for custom behaviors related to their handling and documentation within the `Env` framework.
"""
```

- CLASS METHOD: EnvMeta.__new__
  - CLASS SIGNATURE: class EnvMeta(type):
  - SIGNATURE: def __new__(cls, name: str, bases: t.Tuple[t.Type], ns: t.Dict[str, t.Any]) -> t.Any:
  - DOCSTRING: 
```python
"""
This method is responsible for creating a new instance of the `Env` class, using its metaclass `EnvMeta`. It overrides the default behavior to modify the fully normalized environment variable names by incorporating a prefix, if specified. The method retrieves the class-level namespace dictionary (`ns`) to check for an `__prefix__`. If found, it iterates through all `EnvVariable` instances in the environment and updates their `_full_name` attribute to include the normalized prefix, transforming the names into uppercase format.

Parameters:
- `name` (str): The name of the new class being created.
- `bases` (Tuple[t.Type]): A tuple containing the base classes for the class being created.
- `ns` (Dict[str, t.Any]): A dictionary representing the namespace of the new class, containing attributes defined in the class.

Returns:
- t.Any: The newly created instance of the `Env` class.

Side Effects:
- Modifies `_full_name` of `EnvVariable` instances by prefixing their names with a normalized version of the `__prefix__`, if one exists.

Constants:
- `_normalized`: A utility function defined outside the class, used to standardize the prefix by converting it to uppercase and replacing dots with underscores, ensuring consistent naming for environment variables.
"""
```

- CLASS METHOD: Env.help_info
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def help_info(cls, recursive: bool=False, include_private: bool=False) -> t.List[HelpInfo]:
  - DOCSTRING: 
```python
"""
Extract and return help information for environment variables declared in the Env class. This method provides a structured list of environment variable metadata, including each variable's name, type, default value, and help text. The help information can include variables from nested Env classes if the `recursive` parameter is set to `True`. Additionally, setting `include_private` to `True` will include variables designated as private (starting with an underscore).

Parameters:
- `cls`: The class itself (in practice, the Env subclass).
- `recursive` (bool): If `True`, includes environment variables from nested Env classes. Default is `False`.
- `include_private` (bool): If `True`, includes private environment variables. Default is `False`.

Returns:
- A list of `HelpInfo` namedtuples, each containing the variable's formatted name, type, default value (or help default if specified), and help message.

Dependencies:
- This method relies on the `EnvVariable` class to define and hold the configuration variables and their help information. 
- The `_normalized` function is used to standardize variable names for formatting purposes. The method traverses the class's attributes to find instances of `EnvVariable` and retrieves their attributes to construct the help information.
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
