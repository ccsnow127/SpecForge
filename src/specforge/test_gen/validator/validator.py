"""Test validation and error fixing."""

import tempfile
from pathlib import Path
from typing import Callable, Optional

from ..analyzer.base import LanguageAdapter
from ..config import TestGeneratorConfig
from ..llm.client import LLMClient
from ..models import TestCase, TestResult
from .assertion_fixer import AssertionFixer
from .direct_invocation import test_invokes_target


class TestValidator:
    """Validates generated tests and attempts to fix failures."""

    def __init__(
        self,
        adapter: LanguageAdapter,
        llm_client: LLMClient,
        config: TestGeneratorConfig,
    ):
        """Initialize the validator.

        Args:
            adapter: Language adapter for running tests.
            llm_client: LLM client for fixing tests.
            config: Test generator configuration.
        """
        self.adapter = adapter
        self.llm_client = llm_client
        self.config = config
        self.fix_attempts = 0

    def validate_syntax(self, code: str) -> tuple[bool, Optional[str]]:
        """Validate that test code has correct syntax.

        Args:
            code: Test code to validate.

        Returns:
            Tuple of (is_valid, error_message).
        """
        return self.adapter.check_syntax(code)

    def validate_test_file(
        self,
        test_content: str,
        source_file: Path,
    ) -> tuple[bool, TestResult, str]:
        """Validate a complete test file by running it.

        Args:
            test_content: Complete test file content.
            source_file: Path to the source file being tested.

        Returns:
            Tuple of (success, test_result, final_test_content).
        """
        # First check syntax
        is_valid, error = self.validate_syntax(test_content)
        if not is_valid:
            if self.config.verbose:
                print(f"Syntax error in generated tests: {error}")
            return False, TestResult(errors=1, output=error or "Syntax error"), test_content

        # Write to temp file and run tests
        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=self.adapter.get_test_file_extension(),
            delete=False,
        ) as f:
            f.write(test_content)
            test_file = Path(f.name)

        try:
            result = self.adapter.run_tests(
                test_file, source_file, self.config.timeout_seconds
            )

            if result.success:
                return True, result, test_content

            # Attempt to fix failing tests
            if self.config.verbose:
                print(f"Tests failed: {result.failed} failures, {result.errors} errors")
                print("Attempting to fix...")

            fixed_content = self._attempt_fixes(
                test_content, result, source_file
            )

            if fixed_content != test_content:
                # Re-validate fixed content
                with open(test_file, "w") as f:
                    f.write(fixed_content)

                result = self.adapter.run_tests(
                    test_file, source_file, self.config.timeout_seconds
                )

                return result.success, result, fixed_content

            return False, result, test_content

        finally:
            # Clean up temp file
            try:
                test_file.unlink()
            except OSError:
                pass

    def _attempt_fixes(
        self,
        test_content: str,
        result: TestResult,
        source_file: Path,
    ) -> str:
        """Attempt to fix failing tests, trying assertion replacement first, then LLM.

        Args:
            test_content: Current test content.
            result: Test result with failure information.
            source_file: Source file being tested.

        Returns:
            Fixed test content (or original if fix failed).
        """
        source_code = source_file.read_text()
        current_content = test_content

        # Step 1: Try assertion value replacement (fast, no LLM call)
        fixer = AssertionFixer()
        fixed_content, num_fixes = fixer.fix_assertions(current_content, result)

        if num_fixes > 0:
            if self.config.verbose:
                print(f"Assertion fixer: replaced {num_fixes} expected value(s)")

            # Re-run tests with patched values
            with tempfile.NamedTemporaryFile(
                mode="w",
                suffix=self.adapter.get_test_file_extension(),
                delete=False,
            ) as f:
                f.write(fixed_content)
                temp_file = Path(f.name)

            try:
                result = self.adapter.run_tests(
                    temp_file, source_file, self.config.timeout_seconds
                )
                if result.success:
                    if self.config.verbose:
                        print("Assertion fixer: all tests pass after replacement")
                    return fixed_content
                current_content = fixed_content
                if self.config.verbose:
                    print("Assertion fixer: some tests still failing, trying LLM fix")
            finally:
                try:
                    temp_file.unlink()
                except OSError:
                    pass

        # Step 2: LLM fix loop (existing logic)
        # Check both failure_details and errors count to decide whether to attempt fixes
        needs_fix = bool(result.failure_details) or result.errors > 0 or result.failed > 0
        attempts = 0

        while attempts < self.config.max_fix_attempts and needs_fix:
            attempts += 1
            self.fix_attempts += 1

            if self.config.verbose:
                print(f"Fix attempt {attempts}/{self.config.max_fix_attempts}")

            try:
                fixed_content = self.llm_client.fix_test(
                    test_code=current_content,
                    error_message=result.output[:2000],  # Limit error message size
                    source_code=source_code,
                )

                # Validate syntax — if AST rejects it, still try running it
                # (the real interpreter is authoritative).
                is_valid, error = self.validate_syntax(fixed_content)
                if not is_valid and self.config.verbose:
                    print(f"Syntax checker warning (will try compilation anyway): {error}")

                # Test the fix by actually running it
                with tempfile.NamedTemporaryFile(
                    mode="w",
                    suffix=self.adapter.get_test_file_extension(),
                    delete=False,
                ) as f:
                    f.write(fixed_content)
                    temp_file = Path(f.name)

                try:
                    result = self.adapter.run_tests(
                        temp_file, source_file, self.config.timeout_seconds
                    )

                    if result.success:
                        if self.config.verbose:
                            print("Fix successful!")
                        return fixed_content

                    # Even if not fully successful, keep the fix if it improved things
                    current_content = fixed_content
                    needs_fix = bool(result.failure_details) or result.errors > 0 or result.failed > 0

                finally:
                    try:
                        temp_file.unlink()
                    except OSError:
                        pass

            except Exception as e:
                if self.config.verbose:
                    print(f"Fix attempt failed: {e}")
                continue

        return current_content

    def validate_test_cases(
        self,
        test_cases: list[TestCase],
        source_file: Path,
        imports: list[str],
        build_test_fn: Optional[Callable[[list[TestCase]], str]] = None,
    ) -> list[TestCase]:
        """Validate individual test cases by actually running them.

        Each test is compiled and executed against the source file.
        Only tests that pass are kept.

        Args:
            test_cases: List of test cases to validate.
            source_file: Source file being tested.
            imports: Import statements needed.
            build_test_fn: Optional function to build test file content.
                If provided, uses the same assembly logic as the final output.
                If not, falls back to adapter.generate_test_file.

        Returns:
            List of test cases that pass when run.
        """
        valid_cases = []
        fixer = AssertionFixer()

        for test_case in test_cases:
            # Direct-invocation check: the test body must call its declared
            # target entity directly, not reach it via a caller.
            if test_case.function_name and not test_invokes_target(
                test_case.code, test_case.function_name
            ):
                if self.config.verbose:
                    print(
                        f"Discarding {test_case.name}: does not invoke target "
                        f"{test_case.function_name!r} directly."
                    )
                continue

            if build_test_fn:
                test_content = build_test_fn([test_case])
            else:
                test_content = self.adapter.generate_test_file(
                    [test_case], source_file, imports
                )

            is_valid, error = self.validate_syntax(test_content)
            if not is_valid:
                if self.config.verbose:
                    print(f"Discarding {test_case.name}: syntax error: {error}")
                continue

            with tempfile.NamedTemporaryFile(
                mode="w",
                suffix=self.adapter.get_test_file_extension(),
                delete=False,
            ) as f:
                f.write(test_content)
                test_file = Path(f.name)

            try:
                result = self.adapter.run_tests(
                    test_file, source_file, self.config.timeout_seconds
                )

                if result.success:
                    valid_cases.append(test_case)
                    continue

                # Test failed — try assertion auto-fix before discarding
                fixed_content, num_fixes = fixer.fix_assertions(
                    test_content, result, self.adapter.language
                )

                if self.config.verbose:
                    if num_fixes > 0:
                        print(f"  Assertion fixer: {num_fixes} patch(es) for {test_case.name}")
                    else:
                        # Show why fixer found nothing
                        has_details = len(result.failure_details)
                        has_output = bool(result.output)
                        print(f"  Assertion fixer: 0 patches "
                              f"(failure_details={has_details}, has_output={has_output})")

                if num_fixes > 0:
                    # Re-run with patched assertion values
                    with open(test_file, "w") as ff:
                        ff.write(fixed_content)

                    fixed_result = self.adapter.run_tests(
                        test_file, source_file, self.config.timeout_seconds
                    )

                    if fixed_result.success:
                        # Also patch the test_case.code so downstream uses the fixed values
                        fixed_code, _ = fixer.fix_assertions(
                            test_case.code, result, self.adapter.language
                        )
                        test_case.code = fixed_code
                        valid_cases.append(test_case)
                        if self.config.verbose:
                            print(f"Fixed {test_case.name}: "
                                  f"{num_fixes} assertion value(s) auto-corrected")
                        continue

                # Could not fix — discard
                if self.config.verbose:
                    print(f"Discarding {test_case.name}: "
                          f"{result.failed} failures, {result.errors} errors")
                    if not valid_cases and result.output:
                        # Print error details for the first failure to aid debugging
                        lines = result.output.split('\n')
                        for i, l in enumerate(lines):
                            if 'COMPILATION ERROR' in l or 'error:' in l.lower():
                                # Print this line and next 5 lines for context
                                snippet = '\n'.join(lines[i:i+6])
                                print(f"  Error details:\n{snippet}")
                                break
            finally:
                try:
                    test_file.unlink()
                except OSError:
                    pass

        return valid_cases
