"""Parser for LLM-generated test output (Python only)."""

import re

from ..models import TestCase, TestType, Language


class LLMOutputParser:
    """Parser for extracting Python test cases from LLM output."""

    def __init__(self, language: Language = Language.PYTHON, include_class_setup: bool = True):
        if language != Language.PYTHON:
            raise ValueError(
                f"Unsupported language {language!r}; this release supports Python only."
            )
        self.language = language
        # Kept for API compatibility with the feedback loop, no longer used.
        self.include_class_setup = include_class_setup

    def parse(self, llm_output: str) -> list[TestCase]:
        """Parse LLM output into test cases."""
        cleaned = self._remove_markdown(llm_output)
        return self._parse_python_tests(cleaned)

    def _remove_markdown(self, text: str) -> str:
        """Remove markdown code block markers."""
        text = re.sub(r"^```\w*\n?", "", text, flags=re.MULTILINE)
        text = re.sub(r"\n?```$", "", text, flags=re.MULTILINE)
        text = re.sub(r"^```$", "", text, flags=re.MULTILINE)
        return text.strip()

    def _parse_python_tests(self, code: str) -> list[TestCase]:
        """Parse Python test functions from code."""
        test_cases = []

        pattern = r"((?:@\w+(?:\([^)]*\))?\s*\n)*def\s+(test_\w+)\s*\([^)]*\)\s*(?:->\s*\w+)?\s*:.*?)(?=(?:\n(?:@|\s*def\s+test_)|\Z))"
        matches = re.findall(pattern, code, re.DOTALL)

        for match in matches:
            full_code = match[0].strip()
            test_name = match[1]

            test_type = self._infer_test_type(test_name)
            function_name = self._extract_tested_function(test_name)
            description = self._extract_python_docstring(full_code)

            test_cases.append(
                TestCase(
                    name=test_name,
                    function_name=function_name,
                    code=full_code,
                    test_type=test_type,
                    description=description,
                )
            )

        # Fallback: if no matches found but code mentions a test_ definition,
        # treat the whole blob as a single test.
        if not test_cases and "def test_" in code:
            test_cases.append(
                TestCase(
                    name="test_generated",
                    function_name="unknown",
                    code=code,
                    test_type=TestType.NORMAL,
                )
            )

        return test_cases

    def _infer_test_type(self, test_name: str) -> TestType:
        """Infer test type from test name."""
        name_lower = test_name.lower()

        error_patterns = [
            "error", "exception", "invalid", "fail", "throw", "raises",
            "null", "negative", "illegal", "malformed", "corrupt",
        ]
        if any(p in name_lower for p in error_patterns):
            return TestType.ERROR

        boundary_patterns = [
            "boundary", "edge", "empty", "zero", "max", "min", "limit",
            "overflow", "underflow", "large", "small", "single", "none",
        ]
        if any(p in name_lower for p in boundary_patterns):
            return TestType.BOUNDARY

        return TestType.NORMAL

    def _extract_tested_function(self, test_name: str) -> str:
        """Extract the name of the function being tested from test name."""
        name = test_name
        for prefix in ["test_", "test", "should_", "should"]:
            if name.lower().startswith(prefix):
                name = name[len(prefix):]
                break

        parts = re.split(r"[_A-Z]", name)
        if parts:
            return parts[0].lower() or name
        return name

    def _extract_python_docstring(self, code: str) -> str:
        """Extract docstring from Python test function."""
        match = re.search(r'def\s+\w+[^:]*:\s*["\']([^"\']+)["\']', code)
        if match:
            return match.group(1)

        match = re.search(r'def\s+\w+[^:]*:\s*"""([^"]+)"""', code)
        if match:
            return match.group(1).strip()

        return ""

    def extract_imports(self, code: str) -> list[str]:
        """Extract Python import statements from test code."""
        imports = []
        for line in code.splitlines():
            line = line.strip()
            if line.startswith("import ") or line.startswith("from "):
                imports.append(line)
        return imports
