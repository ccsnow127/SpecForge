# PROJECT NAME: envier-test_help

# FOLDER STRUCTURE:
```
..
└── envier/
    └── env.py
        ├── Env.add_entries
        ├── Env.help_info
        ├── Env.values
        ├── Env.var
        ├── EnvMeta.__new__
        ├── EnvVariable.__init__
        └── _normalized
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
The module facilitates configuration management for an application by providing a structured mechanism to define and access environment variables. It allows developers to declare configurations with type safety, default values, and detailed help descriptions, making application settings explicit and easier to manage. The module supports hierarchical configuration through nested structures, enabling complex multi-component environments to be handled efficiently. By dynamically pulling required settings from environment variables, it simplifies the process of managing environment-specific configurations while ensuring mandatory variables are explicitly defined, reducing runtime errors and improving maintainability.

## FILE 1: envier/env.py

- CLASS METHOD: Env.var
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def var(cls, type: t.Type[T], name: str, parser: t.Optional[t.Callable[[str], T]]=None, validator: t.Optional[t.Callable[[T], None]]=None, map: t.Optional[MapType]=None, default: t.Union[T, NoDefaultType]=NoDefault, deprecations: t.Optional[t.List[DeprecationInfo]]=None, private: bool=False, help: t.Optional[str]=None, help_type: t.Optional[str]=None, help_default: t.Optional[str]=None) -> EnvVariable[T]:
  - DOCSTRING: 
```python
"""
Register an environment variable within the Env class.

This class method creates an instance of EnvVariable, which represents an environment variable that can be configured, validated, and parsed. The variable is associated with a name and type, and it allows for additional processing through optional parameters such as parser and validator.

Parameters:
- type (t.Type[T]): The expected type of the environment variable.
- name (str): The name of the environment variable.
- parser (t.Optional[t.Callable[[str], T]]): An optional function to parse the raw string value into the expected type.
- validator (t.Optional[t.Callable[[T], None]]): An optional function to validate the parsed value.
- map (t.Optional[MapType]): An optional mapping function for modifying the value(s).
- default (t.Union[T, NoDefaultType]): The default value if the environment variable is not set. Defaults to NoDefault for required variables.
- deprecations (t.Optional[t.List[DeprecationInfo]]): Optional list of tuples containing deprecation information for the variable.
- private (bool): If True, the variable name will be private (prefixed with an underscore).
- help (t.Optional[str]): Optional help text describing the variable.
- help_type (t.Optional[str]): Optional description of the variable's type for help documentation.
- help_default (t.Optional[str]): Optional description of the default value for help documentation.

Returns:
- EnvVariable[T]: An instance of the EnvVariable class configured with the given parameters.

This method plays a crucial role in creating and managing environment variables in subclasses of Env and relies on other components like EnvVariable and the _normalized function for proper name formatting.
"""
```

- CLASS METHOD: EnvMeta.__new__
  - CLASS SIGNATURE: class EnvMeta(type):
  - SIGNATURE: def __new__(cls, name: str, bases: t.Tuple[t.Type], ns: t.Dict[str, t.Any]) -> t.Any:
  - DOCSTRING: 
```python
"""
Creates a new instance of the Env class, initializing it with environment variable mappings and applying a prefix to the variable names if specified.

Parameters:
- cls: The class that is being created.
- name: The name of the class being created.
- bases: A tuple containing the base classes from which the class is derived.
- ns: A dictionary containing the namespace (attributes and methods) defined in the class.

Returns:
- An instance of the Env class, properly configured with environment variable mappings.

Side Effects:
- If a `__prefix__` attribute is defined in the class namespace, it normalizes the prefix and updates the `_full_name` attribute of each `EnvVariable` instance using `_normalized()` function to ensure consistent naming format. The `_normalized()` function creates a formatted string by converting dots to underscores and changing the string to uppercase.

Dependencies:
- Relies on the `EnvVariable` class to handle the environment variable definitions, which are retrieved using the `values()` method of the Env class.
- Uses the `os` module to access environment variables indirectly through the `Env` class.
"""
```

- CLASS METHOD: Env.values
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def values(cls, recursive: bool=False, include_derived: bool=False) -> t.Iterator[t.Union[EnvVariable, DerivedVariable, t.Type['Env']]]:
  - DOCSTRING: 
```python
"""
Return an iterator over the values of all configuration items defined in the class.

Parameters:
    recursive (bool): If True, include values from nested Env classes as well. Defaults to False.
    include_derived (bool): If True, also include derived variables in the output. Defaults to False.

Returns:
    t.Iterator[t.Union[EnvVariable, DerivedVariable, t.Type["Env"]]]: An iterator yielding the values of configuration items, which can include instances of EnvVariable, DerivedVariable, or subclasses of Env itself.

This method relies on the `cls.items` method to retrieve the configuration items, iterating over and yielding their corresponding values. The inclusion of derived variables and recursion into nested Env classes is determined by the method parameters, allowing for flexible extraction of configuration data.
"""
```

- CLASS METHOD: Env.help_info
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def help_info(cls, recursive: bool=False, include_private: bool=False) -> t.List[HelpInfo]:
  - DOCSTRING: 
```python
"""
Extracts help information for environment variables declared in the Env class.

Parameters:
- recursive (bool): If True, includes environment variables from nested Env subclasses. Defaults to False.
- include_private (bool): If True, includes variables marked as private (starting with an underscore). Defaults to False.

Returns:
- List[HelpInfo]: A list of HelpInfo tuples, each containing the variable name (formatted for display), its type, the default value, and a help message.

The method relies on the EnvVariable class, which represents environment variables within the Env class, and uses the _normalized helper function to format names. The help messages consider whether variables are private based on their naming convention and aggregate data for both declared and derived variables.
"""
```

- FUNCTION NAME: _normalized
  - SIGNATURE: def _normalized(name: str) -> str:
  - DOCSTRING: 
```python
"""
Normalize the given variable name by converting it to uppercase, replacing dots with underscores, and removing any trailing underscores.

Parameters:
- name (str): The input string representing a variable name that needs normalization.

Returns:
- str: The normalized version of the input variable name.

This function is primarily used in the context of processing environment variable names within the `EnvVariable` and `Env` classes, where consistent naming conventions are necessary for variable retrieval and management.
"""
```
  - DEPENDENCIES:
    - envier/env.py:EnvMeta:__new__
    - envier/env.py:Env:help_info

- CLASS METHOD: EnvVariable.__init__
  - CLASS SIGNATURE: class EnvVariable(t.Generic[T]):
  - SIGNATURE: def __init__(self, type: t.Union[object, t.Type[T]], name: str, parser: t.Optional[t.Callable[[str], T]]=None, validator: t.Optional[t.Callable[[T], None]]=None, map: t.Optional[MapType]=None, default: t.Union[T, NoDefaultType]=NoDefault, deprecations: t.Optional[t.List[DeprecationInfo]]=None, private: bool=False, help: t.Optional[str]=None, help_type: t.Optional[str]=None, help_default: t.Optional[str]=None) -> None:
  - DOCSTRING: 
```python
"""
Initialize an instance of the `EnvVariable` class, which represents an environment variable with type safety and optional metadata.

Parameters:
- `type` (Union[object, Type[T]]): The expected type of the environment variable, which may include a union of types.
- `name` (str): The name of the environment variable.
- `parser` (Optional[Callable[[str], T]]): A function to convert the raw string value to the specified type.
- `validator` (Optional[Callable[[T], None]]): A function to validate the parsed value.
- `map` (Optional[MapType]): A mapping function for transforming values, applicable for collection types.
- `default` (Union[T, NoDefaultType]): The default value to use if the environment variable is not set; defaults to `NoDefault`.
- `deprecations` (Optional[List[DeprecationInfo]]): A list of tuples containing deprecated variable information.
- `private` (bool): A flag indicating if the variable should be treated as private; defaults to `False`.
- `help` (Optional[str]): Help text describing the variable's purpose.
- `help_type` (Optional[str]): A description of the variable type for documentation purposes.
- `help_default` (Optional[str]): A description of the default value for documentation purposes.

Raises:
- `TypeError`: If the `default` value does not match the expected type defined by `type`.

Attributes initialized:
- `self.type`, `self.name`, `self.parser`, `self.validator`, `self.map`, `self.default`, `self.deprecations`, `self.private`, `self.help`, `self.help_type`, `self.help_default`: Store the corresponding parameters passed to the initializer.
- `self._full_name`: A normalized version of the variable name, intended for consistent internal representation, generated using the `_normalized` function.

Constants:
- `NoDefault`: An instance of `NoDefaultType` indicating the absence of a default value, used to distinguish between set and unset default values during initialization.
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
