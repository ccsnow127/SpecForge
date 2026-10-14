"""Prompt templates for LLM-based test generation (Python only)."""

# Used when fixtures are pre-generated (external dependencies isolated upfront).
FIXTURE_AWARE_TEST_PROMPT = '''You are an expert software tester. Generate test methods using the provided fixtures.

{fixture_contract}

## IMPORTANT RULES:
1. Generate ONLY test methods (functions with @pytest.fixture annotations or `def test_*`)
2. Do NOT include imports, class declarations, fixtures, or setup blocks
3. Use ONLY the fixtures listed above — do NOT create new instances
4. Accept ONLY fixture names as function parameters — NEVER add value parameters like `a`, `b`, `expected`
5. **CRITICAL: Each test function MUST contain exactly ONE assertion.** One `assert`, one `pytest.raises`, etc. NEVER put multiple assertions in the same test.
6. For EACH function/method being tested, generate MANY test functions covering: normal cases, boundary cases, and error cases
7. Use different inputs across tests to cover ALL branches (if/else, loops, exceptions) of each function
8. **Direct-invocation rule**: a test named `test_<E>_<scenario>` MUST call `<E>` (or `ClassName.<E>` / `instance.<E>` for methods) **directly** in its body. Never reach `<E>` by calling another function that depends on `<E>` internally — e.g., do NOT test a helper `decode_str` by calling `tokenize(...)` and asserting on its side-effects. Each test must exercise exactly one unit, identified by its name. (Dunder methods such as `__init__` / `__repr__` are exempt and may be invoked via language syntax.)

## Source Code:
```python
{source_code}
```

## Functions to Test:
{functions}

## Test Framework:
{test_framework}

## Example Output Format:

```python
def test_method_positive(fixture_name):
    assert fixture_name.method(2, 3) == 5

def test_method_zeros(fixture_name):
    assert fixture_name.method(0, 0) == 0

def test_method_negative(fixture_name):
    assert fixture_name.method(-1, 1) == 0

def test_method_error(fixture_name):
    with pytest.raises(ValueError):
        fixture_name.method(-1)
```

Generate MANY tests covering ALL branches of EACH function:
- Normal/happy path scenarios
- Boundary conditions (empty inputs, max values, edge cases)
- Error handling (invalid inputs, exceptions)

Output ONLY the test methods, no imports or class structure.
'''

# Supplement prompt used when fixtures are pre-generated.
FIXTURE_AWARE_SUPPLEMENT_PROMPT = '''Generate ADDITIONAL test methods to improve coverage.

{fixture_contract}

## Current Coverage:
- Line coverage: {line_coverage:.1%} (target: {line_target:.0%})
- Branch coverage: {branch_coverage:.1%} (target: {branch_target:.0%})

## Method Coverage (prioritize lowest):
{entity_coverage}

## Uncovered Lines:
{uncovered_lines}

## Source Code:
```python
{source_code}
```

## Existing Tests (for reference - do NOT duplicate):
{existing_tests}

## IMPORTANT:
1. Generate ONLY new test methods
2. Do NOT include imports, class declarations, or fixtures
3. Use ONLY the fixtures listed above
4. NEVER add value parameters to test functions — only fixture names
5. Focus on the LOWEST-coverage methods listed above — generate tests that exercise their uncovered lines
6. **CRITICAL: Each test function MUST contain exactly ONE assertion.** NEVER put multiple assertions in the same test.
7. Do NOT duplicate existing test cases

Output ONLY the new test methods.
'''

# Default prompt used when fixtures are generated inline by the LLM.
INITIAL_TEST_PROMPT = '''You are an expert software tester. Generate comprehensive unit tests for the following code.

## Guidelines:
1. Create "sociable" unit tests - do NOT mock internal module calls
2. Only mock EXTERNAL dependencies (database, network, filesystem, time, random)
3. For EACH function/method being tested, generate MANY test functions covering:
   - Normal/happy path scenarios (multiple inputs)
   - Boundary conditions (empty inputs, zero, max values, edge cases)
   - Error handling (invalid inputs, exceptions)
4. **CRITICAL: Each test function MUST contain exactly ONE assertion.** One `assert`, one `pytest.raises`, etc. NEVER put multiple assertions in the same test.
5. Use different inputs across tests to ensure ALL if/else branches, loop paths, and exception paths are exercised
6. Use descriptive test names: `test_<function>_<scenario>` (e.g., `test_divide_positive`, `test_divide_by_zero`)
7. Each test should verify BEHAVIOR, not implementation details
8. **Direct-invocation rule**: a test named `test_<E>_<scenario>` MUST call `<E>` (or `ClassName.<E>` / `instance.<E>` for methods) **directly** in its body. Never reach `<E>` by calling another function that depends on `<E>` internally — e.g., do NOT test a helper `decode_str` by calling `tokenize(...)` and asserting on its side-effects. Each test must exercise exactly one unit, identified by its name. (Dunder methods such as `__init__` / `__repr__` are exempt and may be invoked via language syntax.)

## CRITICAL: Test Function Signature Requirements
- Test functions MUST take NO parameters (other than pytest fixtures like `tmp_path`).
- Use concrete literal values directly inside the function body. NEVER write `def test_foo(a, b, expected)` — always write `def test_foo():` with values inlined.

## Example Structure:
```python
def test_add_positive():
    assert add(2, 3) == 5

def test_add_zeros():
    assert add(0, 0) == 0

def test_add_negative():
    assert add(-1, 1) == 0

def test_divide_by_zero():
    with pytest.raises(ZeroDivisionError):
        divide(1, 0)
```

## Source Code:
```python
{source_code}
```

## Functions to Test:
{functions}

## Available Constructors:
{constructors}

## Dependencies to Mock (external only):
{dependencies}

## How to Mock External Dependencies:
Use unittest.mock:
```python
from unittest.mock import patch, MagicMock

@patch('module.external_func')
def test_fetch_data_success(mock_func):
    mock_func.return_value = "data"
    assert fetch_data("url") == "data"
```

## Test Framework:
{test_framework}

Generate complete, runnable test code. Output ONLY the test code, no explanations.
'''

COVERAGE_SUPPLEMENT_PROMPT = '''You are an expert software tester. The existing tests have the following coverage gaps.
Generate ADDITIONAL tests to improve coverage.

## Current Coverage:
- Line coverage: {line_coverage:.1%}
- Branch coverage: {branch_coverage:.1%}
- Target: {line_target:.0%} line, {branch_target:.0%} branch

## Method Coverage (prioritize lowest):
{entity_coverage}

## Uncovered Lines:
{uncovered_lines}

## Source Code:
```python
{source_code}
```

## Existing Tests:
```python
{existing_tests}
```

## Guidelines:
1. Focus on the LOWEST-coverage methods listed above — generate tests that exercise their uncovered lines
2. Create "sociable" unit tests - do NOT mock internal module calls
3. Only mock EXTERNAL dependencies (database, network, filesystem, time, random)
4. Include boundary and error cases if not already covered
5. Do NOT duplicate existing tests
6. **CRITICAL: Each test function MUST contain exactly ONE assertion.** NEVER put multiple assertions in the same test.
7. Test functions MUST take NO parameters — use `def test_foo():` with values inlined, NEVER `def test_foo(a, b, expected):`

## Test Framework:
{test_framework}

Generate ONLY the new test functions (not the full file). Output ONLY test code, no explanations.
'''

FIX_TEST_PROMPT = '''The following test is failing. Fix it while maintaining the test's intent.

## Error Message:
```
{error_message}
```

## Failing Test:
```python
{test_code}
```

## Source Code Being Tested:
```python
{source_code}
```

## Guidelines:
1. Fix the test to pass while still testing the same behavior
2. If the test expectation was wrong, correct it based on the actual code behavior
3. Do NOT change what the test is verifying, only how it verifies it
4. Ensure the test still provides value (not just "assert True")

Output ONLY the complete fixed test file (including imports and all test functions), no explanations.
'''

# Test framework description (Python / pytest).
PYTEST_TEMPLATE = '''pytest with standard assertions
- Use `def test_*` naming convention
- Use `pytest.raises` for exception testing
- Use `@pytest.fixture` for setup if needed
- Use `@pytest.mark.parametrize` for data-driven tests'''


def get_test_framework_description(language: str = "python") -> str:
    """Get the test framework description.  Python-only release."""
    if language.lower() != "python":
        raise ValueError(
            f"Unsupported language {language!r}; this release supports Python only."
        )
    return PYTEST_TEMPLATE
