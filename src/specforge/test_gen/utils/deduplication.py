"""Test deduplication utilities."""

import re
from typing import Optional

from ..models import TestCase


def deduplicate_tests(test_cases: list[TestCase]) -> list[TestCase]:
    """Remove duplicate test cases based on input signature.

    Args:
        test_cases: List of test cases to deduplicate.

    Returns:
        Deduplicated list of test cases with unique names.
    """
    seen_signatures: dict[str, TestCase] = {}
    seen_names: set[str] = set()
    result: list[TestCase] = []

    for test in test_cases:
        # Generate signature for comparison
        signature = _get_test_signature(test)

        if signature in seen_signatures:
            # Duplicate found, skip
            continue

        # Handle name collisions
        unique_name = _make_unique_name(test.name, seen_names)
        if unique_name != test.name:
            # Create new test case with updated name
            test = TestCase(
                name=unique_name,
                function_name=test.function_name,
                code=_rename_test_in_code(test.code, test.name, unique_name),
                inputs=test.inputs,
                test_type=test.test_type,
                description=test.description,
            )

        seen_signatures[signature] = test
        seen_names.add(unique_name)
        result.append(test)

    return result


def are_tests_equivalent(test1: TestCase, test2: TestCase) -> bool:
    """Check if two tests are functionally equivalent.

    Args:
        test1: First test case.
        test2: Second test case.

    Returns:
        True if tests are equivalent.
    """
    sig1 = _get_test_signature(test1)
    sig2 = _get_test_signature(test2)
    return sig1 == sig2


def _get_test_signature(test: TestCase) -> str:
    """Generate a signature for a test case for deduplication.

    The signature is based on:
    - The function being tested
    - The inputs/assertions (normalized)

    Args:
        test: Test case to generate signature for.

    Returns:
        Signature string.
    """
    # Normalize the code for comparison
    code = test.code

    # Remove comments
    code = re.sub(r"#.*$", "", code, flags=re.MULTILINE)  # Python
    code = re.sub(r"//.*$", "", code, flags=re.MULTILINE)  # Java
    code = re.sub(r"/\*.*?\*/", "", code, flags=re.DOTALL)  # Multi-line

    # Remove whitespace
    code = re.sub(r"\s+", " ", code).strip()

    # Remove test function/method name (we care about the body)
    code = re.sub(r"def test_\w+\([^)]*\)\s*:", "", code)  # Python
    code = re.sub(r"void \w+\([^)]*\)\s*\{", "", code)  # Java
    code = re.sub(r"func Test\w+\(\w+\s+\*testing\.T\)\s*\{", "", code)  # Go

    # Extract key elements for signature
    elements = []

    # Function being tested
    elements.append(test.function_name)

    # Extract function calls and their arguments
    # Matches: function(args) or object.method(args)
    calls = re.findall(r"(\w+(?:\.\w+)*)\s*\(([^)]*)\)", code)
    for func, args in calls:
        # Skip common assertion functions for the function name part
        if func not in {"assert", "assertEqual", "assertEquals", "assertTrue",
                        "assertFalse", "assertRaises", "assertThrows", "pytest"}:
            elements.append(f"{func}({_normalize_args(args)})")

    # Extract assertions
    assertions = re.findall(r"assert\w*\s*\(([^)]+)\)", code)
    for assertion in assertions:
        elements.append(f"assert({_normalize_args(assertion)})")

    return "|".join(sorted(elements))


def _normalize_args(args: str) -> str:
    """Normalize argument string for comparison.

    Args:
        args: Argument string.

    Returns:
        Normalized argument string.
    """
    # Remove whitespace
    args = re.sub(r"\s+", "", args)
    # Sort if it looks like keyword args
    if "=" in args:
        parts = args.split(",")
        parts.sort()
        return ",".join(parts)
    return args


def _make_unique_name(name: str, existing: set[str]) -> str:
    """Generate a unique test name.

    Args:
        name: Proposed test name.
        existing: Set of existing names.

    Returns:
        Unique name (possibly with numeric suffix).
    """
    if name not in existing:
        return name

    # Add numeric suffix
    base = name
    counter = 2

    # Check if name already has a numeric suffix
    match = re.match(r"(.+)_(\d+)$", name)
    if match:
        base = match.group(1)
        counter = int(match.group(2)) + 1

    while True:
        new_name = f"{base}_{counter}"
        if new_name not in existing:
            return new_name
        counter += 1


def _rename_test_in_code(code: str, old_name: str, new_name: str) -> str:
    """Rename a test function in code.

    Args:
        code: Test code.
        old_name: Old function name.
        new_name: New function name.

    Returns:
        Code with renamed function.
    """
    # Python: def test_name(
    code = re.sub(
        rf"\bdef {re.escape(old_name)}\s*\(",
        f"def {new_name}(",
        code,
    )

    # Java: void testName(
    code = re.sub(
        rf"\bvoid {re.escape(old_name)}\s*\(",
        f"void {new_name}(",
        code,
    )

    # Go: func TestName(
    code = re.sub(
        rf"\bfunc {re.escape(old_name)}\s*\(",
        f"func {new_name}(",
        code,
    )

    return code
