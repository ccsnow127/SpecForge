"""Assertion failure auto-fixer (Python only).

Replaces wrong expected values in test code using actual values parsed
from pytest assertion-failure messages.
"""

import re
from dataclasses import dataclass

from ..models import TestResult


@dataclass
class AssertionPatch:
    """A single assertion value replacement.

    Attributes:
        expected_value: The wrong value the LLM guessed.
        actual_value: The real value from running the code.
        test_name: Which test function (for scoped replacement).
        line_hint: Approximate line number from error output.
    """

    expected_value: str
    actual_value: str
    test_name: str = ""
    line_hint: int = 0


class AssertionFixer:
    """Fixes assertion failures by replacing expected values with actual values."""

    def fix_assertions(
        self, test_content: str, test_result: TestResult
    ) -> tuple[str, int]:
        """Fix assertion failures in test code using error output.

        Returns:
            Tuple of (fixed_code, number_of_fixes_applied).
        """
        all_patches: list[AssertionPatch] = []

        for failure in test_result.failure_details:
            if self._classify_failure(failure) != "assertion":
                continue
            message = failure.get("message", "") or failure.get("output", "") or ""
            test_name = failure.get("test_name", "") or failure.get("name", "") or ""
            for patch in self._parse_assertion_failure(message):
                if not patch.test_name and test_name:
                    patch.test_name = test_name
                all_patches.append(patch)

        if not all_patches and test_result.output:
            all_patches = self._parse_assertion_failure(test_result.output)

        if not all_patches:
            return test_content, 0

        fixed_content = self._apply_patches(test_content, all_patches)
        num_fixes = 0 if fixed_content == test_content else len(all_patches)
        return fixed_content, num_fixes

    def _classify_failure(self, failure: dict) -> str:
        """Classify a failure as assertion / compilation / other."""
        message = failure.get("message", "") or failure.get("output", "") or ""
        message_lower = message.lower()

        compilation_indicators = ("syntaxerror", "syntax error", "indentationerror")
        if any(ind in message_lower for ind in compilation_indicators):
            return "compilation"

        assertion_indicators = ("assertionerror", "assert ")
        if any(ind in message_lower for ind in assertion_indicators):
            return "assertion"

        return "other"

    def _parse_assertion_failure(self, message: str) -> list[AssertionPatch]:
        """Parse pytest assertion failure messages.

        Handles:
            assert ACTUAL == EXPECTED
            E       assert ACTUAL == EXPECTED
            AssertionError: ACTUAL != EXPECTED
        """
        patches: list[AssertionPatch] = []
        seen: set[tuple[str, str]] = set()

        eq_pattern = re.compile(
            r"(?:^|\n)\s*(?:E\s+)?assert\s+(.+?)\s*==\s*(.+?)(?:\s*$|\s*\n)",
            re.MULTILINE,
        )
        for match in eq_pattern.finditer(message):
            actual_val = match.group(1).strip()
            expected_val = match.group(2).strip()
            key = (expected_val, actual_val)
            if (key not in seen
                    and self._is_simple_literal(expected_val)
                    and self._is_simple_literal(actual_val)):
                seen.add(key)
                patches.append(
                    AssertionPatch(expected_value=expected_val, actual_value=actual_val)
                )

        ne_pattern = re.compile(
            r"Asserti?onError:\s*(.+?)\s*!=\s*(.+?)(?:\s*$|\s*\n)",
            re.MULTILINE,
        )
        for match in ne_pattern.finditer(message):
            actual_val = match.group(1).strip()
            expected_val = match.group(2).strip()
            key = (expected_val, actual_val)
            if (key not in seen
                    and self._is_simple_literal(expected_val)
                    and self._is_simple_literal(actual_val)):
                seen.add(key)
                patches.append(
                    AssertionPatch(expected_value=expected_val, actual_value=actual_val)
                )

        return patches

    def _apply_patches(
        self, test_content: str, patches: list[AssertionPatch]
    ) -> str:
        """Apply assertion-value patches to Python test code."""
        result = test_content
        for patch in patches:
            result = self._apply_python_patch(result, patch)
        return result

    def _apply_python_patch(self, content: str, patch: AssertionPatch) -> str:
        """Apply a single patch to Python test code.

        Handles `assert actual == expected`. Replaces only when exactly one
        match is found (or when scoped to a unique test method) to avoid
        ambiguity.
        """
        expected_src = self._format_python_literal(patch.expected_value)
        actual_src = self._format_python_literal(patch.actual_value)

        lines = content.split("\n")
        match_indices = [
            i for i, line in enumerate(lines)
            if self._line_has_assert(line.strip(), expected_src)
        ]

        if len(match_indices) == 1:
            idx = match_indices[0]
            lines[idx] = self._replace_assert_expected(
                lines[idx], expected_src, actual_src
            )
            return "\n".join(lines)

        if patch.test_name and len(match_indices) > 1:
            scoped = self._find_in_test_method(
                lines, match_indices, patch.test_name, expected_src, actual_src
            )
            if scoped is not None:
                return "\n".join(scoped)

        return content

    def _line_has_assert(self, line: str, expected_src: str) -> bool:
        if "assert " not in line:
            return False
        return f"== {expected_src}" in line or f"=={expected_src}" in line

    def _replace_assert_expected(
        self, line: str, old_val: str, new_val: str
    ) -> str:
        if f"== {old_val}" in line:
            return line.replace(f"== {old_val}", f"== {new_val}", 1)
        if f"=={old_val}" in line:
            return line.replace(f"=={old_val}", f"=={new_val}", 1)
        return line.replace(old_val, new_val, 1)

    def _find_in_test_method(
        self,
        lines: list[str],
        match_indices: list[int],
        test_name: str,
        expected_src: str,
        actual_src: str,
    ) -> list[str] | None:
        """Try to find and replace within a specific test function's scope."""
        method_start = None
        for i, line in enumerate(lines):
            if test_name in line:
                method_start = i
                break
        if method_start is None:
            return None

        # Python: next def at same or lower indentation closes the scope.
        method_end = len(lines) - 1
        base_indent = len(lines[method_start]) - len(lines[method_start].lstrip())
        for i in range(method_start + 1, len(lines)):
            stripped = lines[i].strip()
            if stripped and not stripped.startswith("#"):
                indent = len(lines[i]) - len(lines[i].lstrip())
                if indent <= base_indent and stripped.startswith("def "):
                    method_end = i - 1
                    break

        scoped = [idx for idx in match_indices if method_start <= idx <= method_end]
        if len(scoped) == 1:
            idx = scoped[0]
            lines[idx] = self._replace_assert_expected(
                lines[idx], expected_src, actual_src
            )
            return lines
        return None

    def _is_simple_literal(self, value: str) -> bool:
        """Allow only safely-replaceable literals: numbers, strings, booleans, None."""
        v = value.strip()
        if not v:
            return False
        if v in ("None", "True", "False", "null", "true", "false"):
            return True
        if re.match(r"^-?\d+(\.\d+)?([eE][+-]?\d+)?$", v):
            return True
        if (v.startswith('"') and v.endswith('"')) or (
            v.startswith("'") and v.endswith("'")
        ):
            return True
        if v.startswith("-") and self._is_simple_literal(v[1:]):
            return True
        # Reject expressions with operators or accessors
        if any(c in v for c in ("(", ")", ".", "[", "]", "+", "*", "/")):
            return False
        if re.match(r"^[A-Za-z_]\w*$", v) and len(v) < 30:
            return True
        return True

    def _format_python_literal(self, value: str) -> str:
        """Format a parsed value as a Python source-code literal."""
        v = value.strip()
        if v in ("True", "False"):
            return v
        if v == "true":
            return "True"
        if v == "false":
            return "False"
        if v in ("None", "null"):
            return "None"
        if re.match(r"^-?\d+(\.\d+)?([eE][+-]?\d+)?$", v):
            return v
        if (v.startswith('"') and v.endswith('"')) or (
            v.startswith("'") and v.endswith("'")
        ):
            return v
        if not re.match(r"^-?\d", v) and v not in ("True", "False", "None"):
            return f'"{v}"'
        return v
