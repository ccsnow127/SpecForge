# PROJECT NAME: getschema-test_types

# FOLDER STRUCTURE:
```
..
└── getschema/
    └── impl.py
        ├── _compare_props
        ├── _convert_key
        ├── _do_infer_schema
        ├── _infer_from_two
        ├── _replace_null_type
        └── infer_schema
```

# IMPLEMENTATION REQUIREMENTS:
## MODULE DESCRIPTION:
This module is designed to infer and validate schemas dynamically from provided data records, ensuring consistency and format compliance. It analyzes input datasets with fields of varying types, including nullable fields, nested objects, and arrays, and produces a schema definition that accurately represents the structure of the data. The module offers capabilities such as detecting data types (e.g., string, number, null), identifying nested fields, and verifying format-specific attributes like date-time. By automating schema inference, it simplifies data validation processes and assists developers in ensuring the integrity of data pipelines or APIs consuming complex and variable input structures.

## FILE 1: getschema/impl.py

- FUNCTION NAME: _compare_props
  - SIGNATURE: def _compare_props(prop1, prop2):
  - DOCSTRING: 
```python
"""
Compare two property schemas and return a conservative merged schema.

Parameters:
- prop1 (dict): The first property schema to compare, containing its type and optionally a format.
- prop2 (dict): The second property schema to compare, which will be considered the more recent definition.

Returns:
- dict: A merged property schema that combines the types and formats of both input schemas, favoring the more conservative types where discrepancies exist.

Raises:
- ValueError: If the types do not match when both properties are of type "object" or "array" and have structural differences.
  
Notes:
- The function handles properties defined as objects or arrays recursively. If both properties have types that are numeric (either "integer" or "number"), the resulting type will be "number" if no other types are compatible.
- The function utilizes constants like `numbers` to identify types relevant to numeric properties and checks against this list to determine the output type when discrepancies arise.
"""
```
  - DEPENDENCIES:
    - getschema/impl.py:_compare_props

- FUNCTION NAME: _convert_key
  - SIGNATURE: def _convert_key(old_key, lower=False, replace_special=False, snake_case=False):
  - DOCSTRING: 
```python
"""
Converts a given string key into a new format based on specified options.

Parameters:
- old_key (str): The original string key to be converted.
- lower (bool): If True, converts the key to all lowercase letters.
- replace_special (bool): If True, replaces any character that is not alphanumeric (including spaces) with an underscore ('_').
- snake_case (bool): If True, replaces spaces in the key with underscores ('_').

Returns:
- str: The transformed key based on the specified options.

The function interacts with the `re` module for pattern matching and replacement. It serves as a utility for normalizing keys in JSON objects or other data structures before inferring schemas or manipulating data. The resulting keys may be utilized elsewhere in the code, particularly in the `infer_schema` and `_do_infer_schema` functions, to ensure consistent formatting.
"""
```

- FUNCTION NAME: infer_schema
  - SIGNATURE: def infer_schema(obj, record_level=None, lower=False, replace_special=False, snake_case=False):
  - DOCSTRING: 
```python
"""
Infer a JSON schema from a given object or a list of objects.

The function analyzes the structure of the provided object(s) and generates a corresponding JSON schema that captures the types and properties of the input data. It supports optional transformations for the keys, such as converting to lowercase, replacing special characters, and transforming spaces into snake_case.

Parameters:
- obj: A single object or a list of objects (must be a dict). The data for which the schema will be inferred.
- record_level: Optional; a JSONPath expression specifying which part of the object to focus on.
- lower: Optional; if True, converts all keys to lowercase in the inferred schema.
- replace_special: Optional; if True, replaces special characters in keys with underscores.
- snake_case: Optional; if True, replaces spaces in keys with underscores.

Returns:
- A dictionary representing the inferred JSON schema, including types and properties.

Side Effects:
- Updates the `LOGGER` with info about the number of records processed.

The function relies on several helper functions, including `_do_infer_schema` for type inference, `_infer_from_two` for combining schemas, and `_replace_null_type` for ensuring types are robust, especially with null values.
"""
```
  - DEPENDENCIES:
    - getschema/impl.py:_replace_null_type
    - getschema/impl.py:_infer_from_two
    - getschema/impl.py:_do_infer_schema

- FUNCTION NAME: _replace_null_type
  - SIGNATURE: def _replace_null_type(schema, path=''):
  - DOCSTRING: 
```python
"""
Replace 'null' types in the provided JSON schema with a default type definition. This function recursively traverses the schema, updating types for objects, arrays, and null types based on specific conditions. 

Parameters:
- schema (dict): The JSON schema to be modified. It may include properties like 'type' and 'properties'.
- path (str): The path for the schema property currently being processed, primarily for logging purposes. Defaults to an empty string.

Returns:
- dict: A new schema dictionary with updated types where necessary. 

Side Effects:
- Logs warnings when encountering arrays or null types without specified types, and modifies them to use the default type defined by the constant DEFAULT_TYPE, which is set as ["null", "string"] at the module level.
"""
```
  - DEPENDENCIES:
    - getschema/impl.py:_replace_null_type
    - getschema/impl.py:infer_schema

- FUNCTION NAME: _infer_from_two
  - SIGNATURE: def _infer_from_two(schema1, schema2):
  - DOCSTRING: 
```python
"""
Compare two JSON schema objects and return the more conservative schema by evaluating their properties. This function takes two schemas, `schema1` and `schema2`, and merges their properties by determining which types are more restrictive. If conflicting property types are encountered, an exception is raised, indicating which key caused the conflict.

Parameters:
- schema1 (dict): The first JSON schema object to compare.
- schema2 (dict): The second JSON schema object to compare.

Returns:
- dict: The merged schema object that represents the most conservative properties from both input schemas.

Notes:
- The function relies on the `_compare_props` function to compare individual properties of the schemas. It utilizes the `properties` key in the schema dictionaries to access the definitions of the schema.
- If one of the schemas is None, it simply returns the other schema, effectively allowing for incremental schema construction from multiple records.
"""
```
  - DEPENDENCIES:
    - getschema/impl.py:_compare_props
    - getschema/impl.py:infer_schema

- FUNCTION NAME: _do_infer_schema
  - SIGNATURE: def _do_infer_schema(obj, record_level=None, lower=False, replace_special=False, snake_case=False):
  - DOCSTRING: 
```python
"""
Infer the JSON schema from a given object or list of objects, recursively determining the types and structure based on the content. This function supports customization of key formatting through options for lowercasing, replacing special characters, and converting to snake_case.

Parameters:
- obj (Any): The input object (dict, list, or primitive type) from which to infer the schema.
- record_level (Optional[str]): A JSONPath string to specify sub-records for schema inference.
- lower (Optional[bool]): If True, convert keys to lowercase.
- replace_special (Optional[bool]): If True, replace special characters in keys with underscores.
- snake_case (Optional[bool]): If True, convert spaces in keys to underscores.

Returns:
- dict: A dictionary representing the inferred JSON schema, with properties and types specified according to JSON Schema standards.

Dependencies:
- _get_jsonpath: Used to fetch specific records from the input based on JSONPath.
- _is_datetime: Checks if a given object is a valid datetime string.
- _convert_key: Transforms keys based on the specified formatting options.

Constants:
- DEFAULT_TYPE: A list ["null", "string"], used as a fallback type when no specific type can be inferred for a property.
"""
```
  - DEPENDENCIES:
    - getschema/impl.py:_convert_key
    - getschema/impl.py:infer_schema
    - getschema/impl.py:_do_infer_schema

# TASK DESCRIPTION:
In this project, you need to implement the functions and methods listed above. The functions have been removed from the code but their docstrings remain.
Your task is to:
1. Read and understand the docstrings of each function/method
2. Understand the dependencies and how they interact with the target functions
3. Implement the functions/methods according to their docstrings and signatures
4. Ensure your implementations work correctly with the rest of the codebase
