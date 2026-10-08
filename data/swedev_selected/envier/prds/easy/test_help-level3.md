# PROJECT NAME: envier-test_help

# FOLDER STRUCTURE:
```
..
└── envier/
    └── env.py
        ├── Env.add_entries
        ├── Env.help_info
        ├── Env.items
        ├── Env.values
        ├── Env.var
        ├── EnvMeta.__new__
        ├── EnvVariable.EnvVariable
        ├── EnvVariable.__init__
        ├── NoDefaultType.__str__
        └── _normalized
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
The module provides a centralized configuration management system for an application, enabling both global and service-specific configuration variables to be defined, accessed, and documented. It supports defining environment variables with type enforcement, default values, and detailed descriptions to ensure clarity and ease of use. The module allows recursive documentation of all available configuration variables, making it easier for developers to understand and manage application settings. By standardizing configuration handling and providing built-in help documentation, the module simplifies environment setup, reduces misconfiguration errors, and improves developer productivity.

## FILE 1: envier/env.py

- CLASS METHOD: Env.var
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def var(cls, type: t.Type[T], name: str, parser: t.Optional[t.Callable[[str], T]]=None, validator: t.Optional[t.Callable[[T], None]]=None, map: t.Optional[MapType]=None, default: t.Union[T, NoDefaultType]=NoDefault, deprecations: t.Optional[t.List[DeprecationInfo]]=None, private: bool=False, help: t.Optional[str]=None, help_type: t.Optional[str]=None, help_default: t.Optional[str]=None) -> EnvVariable[T]:
  - DOCSTRING: 
```python
"""
Register an environment variable within the Env subclass.

This method is used to declare a mapping between an attribute of the subclass and an environment variable. It creates an instance of the `EnvVariable` class, which handles the retrieval and validation of the environment variable.

Parameters:
- `type` (t.Type[T]): The expected type of the environment variable.
- `name` (str): The name of the environment variable.
- `parser` (Optional[t.Callable[[str], T]]): An optional function to parse the raw string value.
- `validator` (Optional[t.Callable[[T], None]]): An optional function to validate the parsed value. It should raise a ValueError for invalid values.
- `map` (Optional[MapType]): A function to map raw values before returning them.
- `default` (Union[T, NoDefaultType]): A default value if the environment variable is not set. Must be of the specified type unless using `NoDefault`.
- `deprecations` (Optional[t.List[DeprecationInfo]]): A list of tuples providing deprecation information for the variable.
- `private` (bool): If True, the variable will be treated as private, prefixing the full name with an underscore.
- `help` (Optional[str]): A help message for the variable.
- `help_type` (Optional[str]): A description of the expected type for the variable.
- `help_default` (Optional[str]): A message describing the default value if it is used.

Returns:
- EnvVariable[T]: An instance of `EnvVariable` which can be used to fetch the environment variable value.

This method utilizes the `NoDefaultType` constant to differentiate between provided default values and the absence of a default. It interacts closely with the `EnvVariable` class, which defines the behavior for retrieving and validating the environment variable's value.
"""
```

- CLASS METHOD: EnvMeta.__new__
  - CLASS SIGNATURE: class EnvMeta(type):
  - SIGNATURE: def __new__(cls, name: str, bases: t.Tuple[t.Type], ns: t.Dict[str, t.Any]) -> t.Any:
  - DOCSTRING: 
```python
"""
This method is a custom metaclass for the Env class, facilitating the creation of environment variable bindings. It initializes a new Env instance, allowing for the configuration of environment variables within a given namespace.

Parameters:
- cls: The metaclass itself, usually passed implicitly.
- name: The name of the class being created.
- bases: A tuple containing the base classes of the class being created.
- ns: A dictionary containing the namespace (attributes and methods) of the class being created.

Returns:
- An instance of the Env class, populated with its environment variables and prefixed appropriately.

Side Effects:
- If the class has a `__prefix__` attribute, this method updates the `_full_name` for each EnvVariable in the environment to include the prefix, normalized to uppercase and formatted with underscores. This affects how environment variables are looked up and ensures consistency in naming conventions.

Dependencies:
- The `_normalized` function is used to transform the prefix and variable names into a suitable format for environment variable handling.
- It leverages the `values` method to gather all EnvVariables defined in the Env class, setting their full names according to the defined prefix.
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
- recursive (bool): If set to True, this parameter allows the method to traverse and include values from nested Env subclasses. Defaults to False.
- include_derived (bool): If set to True, this parameter enables the inclusion of derived variables in the results. Defaults to False.

Returns:
- Iterator[Union[EnvVariable, DerivedVariable, Type[Env]]]: An iterator yielding values of type EnvVariable, DerivedVariable, or the type of the Env subclass itself.

This method interacts with the `items` method to retrieve the configuration items, and it is designed to work within the class context of Env, which manages environment variable definitions. The method facilitates access to environment configurations, enhancing data retrieval from various Env subclasses.
"""
```

- CLASS METHOD: Env.help_info
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def help_info(cls, recursive: bool=False, include_private: bool=False) -> t.List[HelpInfo]:
  - DOCSTRING: 
```python
"""
Extracts help information from the environment configuration class.

Parameters:
- cls (Type[Env]): The class from which to extract environment variable information.
- recursive (bool): If True, includes environment variables from nested Env classes.
- include_private (bool): If True, includes variables marked as private (name starts with an underscore).

Returns:
- List[HelpInfo]: A list of tuples containing the variable name (formatted for display), type, default value, and help text for each environment variable declared in the class.

This method relies on the `EnvVariable` class for its entries and uses the `_normalized` function to format variable names consistently. It also appends a period to help messages if needed and gathers help information in a depth-first manner from nested Env classes when `recursive` is set to True.
"""
```

- CLASS METHOD: Env.items
  - CLASS SIGNATURE: class Env(metaclass=EnvMeta):
  - SIGNATURE: def items(cls, recursive: bool=False, include_derived: bool=False) -> t.Iterator[t.Tuple[str, t.Union[EnvVariable, DerivedVariable]]]:
  - DOCSTRING: 
```python
"""
Returns an iterator over the environment variable items declared in the Env class.

The method retrieves both `EnvVariable` and `DerivedVariable` items from the Env class, optionally including derived variables based on the `include_derived` flag. If `recursive` is set to True, it will also traverse any nested subclass instances of Env and include their variables. Each yield produces a tuple containing the full path to the variable (as a dot-separated string) and the variable itself.

Parameters:
- cls: The Env subclass from which to retrieve the variable items.
- recursive (bool): Indicates whether to include variables from nested Env subclasses.
- include_derived (bool): When True, includes derived variables in the output.

Returns:
An iterator of tuples, where each tuple contains the full name of the variable and the corresponding variable instance (either EnvVariable or DerivedVariable).

Internal Usage:
- `q`: A deque used for breadth-first traversal of subclasses to gather variables.
- `classes`: A tuple determining which variable types to include in the results based on the `include_derived` flag.
"""
```

- FUNCTION NAME: _normalized
  - SIGNATURE: def _normalized(name: str) -> str:
  - DOCSTRING: 
```python
"""
Normalize the input string by converting it to uppercase, replacing periods with underscores, and removing trailing underscores.

Parameters:
- name (str): The input string to be normalized.

Returns:
- str: The normalized version of the input string with the specified transformations applied.

This function is used in the `EnvVariable` class to standardize environment variable names for consistent handling within the environment configuration system.
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
Initialize an environment variable descriptor for the Env class.

This initializer sets up the properties of an EnvVariable, defining its type, name, and various optional parameters for parsing, validation, mapping, and documentation. It checks the compatibility of the default value with the specified type and prepares the full name for environment variable retrieval, adhering to the naming conventions defined in the _normalized function. This function interacts with the Env class, allowing users to define environment variables that can be managed and retrieved safely.

Parameters:
- type (t.Union[object, t.Type[T]]): The expected data type of the environment variable.
- name (str): The name of the environment variable.
- parser (Optional[t.Callable[[str], T]]): A function for parsing the raw string value into the expected type.
- validator (Optional[t.Callable[[T], None]]): A function for validating the parsed value.
- map (Optional[MapType]): A mapping function for transforming the value.
- default (t.Union[T, NoDefaultType]): The default value if the environment variable is not set. Defaults to NoDefault.
- deprecations (Optional[t.List[DeprecationInfo]]): A list of deprecation warnings associated with this variable.
- private (bool): A flag indicating if the variable is private (prefixed with an underscore).
- help (Optional[str]): Help text associated with the variable.
- help_type (Optional[str]): Help text describing the type of the variable.
- help_default (Optional[str]): Help message for the default value.

Raises:
- TypeError: If the default value is not compatible with the specified type.

Attributes:
- self.type: Stores the expected type of the environment variable.
- self.name: The environment variable's name.
- self.parser: The optional parser for type conversion.
- self.validator: The optional validator for value checking.
- self.default: The default value for the environment variable.
- self.private: Indicates if the variable is private.

The _normalized function is used to transform the variable name into a standardized format, which is crucial for consistency when accessing environment variables.
"""
```

- CLASS METHOD: NoDefaultType.__str__
  - CLASS SIGNATURE: class NoDefaultType(object):
  - SIGNATURE: def __str__(self):
  - DOCSTRING: 
```python
"""
This method returns an empty string representation of the NoDefaultType instance.

It overrides the __str__ method from the default object behavior to provide a specific output for instances of this class, which is used to signify the absence of a default value in the context of environment variable management within the EnvVariable class. The NoDefault class serves as a marker for cases where a default value is not set, allowing the EnvVariable class to handle mandatory environment variables appropriately.
"""
```

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
