"""Static check that a generated test invokes its target entity directly.

A well-isolated unit test for entity `E` should contain an explicit call to
`E` (or a method call whose attribute is `E`) in its body. If a test claims
to target `E` but only reaches `E` transitively through a higher-level
caller, its pass/fail is coupled to the caller's correctness and breaks the
callee-before-caller refinement order.

This module parses the test's Python source via `ast` and verifies that at
least one `ast.Call` in the test body targets `E`.
"""

from __future__ import annotations

import ast


def _is_dunder(name: str) -> bool:
    return name.startswith("__") and name.endswith("__") and len(name) >= 4


def _collect_call_targets(node: ast.AST) -> set[str]:
    """Return the set of simple names referenced as call targets in `node`."""
    targets: set[str] = set()
    for child in ast.walk(node):
        if not isinstance(child, ast.Call):
            continue
        func = child.func
        if isinstance(func, ast.Name):
            targets.add(func.id)
        elif isinstance(func, ast.Attribute):
            targets.add(func.attr)
            # Also capture ClassName.method chains for static / classmethod calls.
            if isinstance(func.value, ast.Name):
                targets.add(f"{func.value.id}.{func.attr}")
    return targets


def test_invokes_target(test_code: str, target_entity: str) -> bool:
    """Check whether `test_code` contains a direct call to `target_entity`.

    `target_entity` may be either a bare function name (`"decode_str"`) or a
    qualified method name (`"Lexer._convert_rules"`). Dunder methods are
    exempt because they are always invoked indirectly via language syntax
    (`__init__` via constructor, `__repr__` via `repr()`, etc.).

    Returns True if the test passes the direct-invocation rule; False if it
    appears to reach the target only through a caller (or not at all).
    """
    leaf = target_entity.rsplit(".", 1)[-1]
    if _is_dunder(leaf):
        return True  # dunders are always invoked via language syntax

    try:
        tree = ast.parse(test_code)
    except SyntaxError:
        # Leave syntax rejection to the dedicated syntax validator.
        return True

    targets = _collect_call_targets(tree)
    return leaf in targets or target_entity in targets
