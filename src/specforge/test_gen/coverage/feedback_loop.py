"""Coverage-driven test generation feedback loop."""

import tempfile
from pathlib import Path
from typing import Optional, TYPE_CHECKING

from ..analyzer.base import LanguageAdapter
from ..config import TestGeneratorConfig
from ..llm.client import LLMClient
from ..llm.parser import LLMOutputParser
from ..models import Coverage, TestCase, GenerationMetadata, TestResult
from ..utils.deduplication import deduplicate_tests
from ..validator.validator import TestValidator

if TYPE_CHECKING:
    from ..fixture.base import FixtureContext


class CoverageFeedbackLoop:

    def __init__(
        self,
        adapter: LanguageAdapter,
        llm_client: LLMClient,
        parser: LLMOutputParser,
        validator: TestValidator,
        config: TestGeneratorConfig,
    ):
        self.adapter = adapter
        self.llm_client = llm_client
        self.parser = parser
        self.validator = validator
        self.config = config

    def run(
        self,
        initial_tests: list[TestCase],
        source_file: Path,
        source_code: str,
        imports: list[str],
        fixture_context: Optional["FixtureContext"] = None,
    ) -> tuple[list[TestCase], Coverage, GenerationMetadata]:

        test_cases = list(initial_tests)
        self._fixture_context = fixture_context

        metadata = GenerationMetadata(
            tests_generated=len(initial_tests),
        )

        # Measure initial coverage (no fixing — just measure)
        initial_coverage, initial_test_result = self._measure_coverage(
            test_cases, source_file, imports
        )
        metadata.initial_coverage = initial_coverage

        if self.config.verbose:
            print(f"Initial coverage: {initial_coverage.line:.1%} line, "
                  f"{initial_coverage.branch:.1%} branch")
            if initial_test_result:
                print(f"Initial tests: {initial_test_result.passed} passed, "
                      f"{initial_test_result.failed} failed, "
                      f"{initial_test_result.errors} errors")
            if initial_coverage.method_coverage:
                print("Method coverage (lowest first):")
                for mc in initial_coverage.method_coverage[:10]:
                    if mc["pct"] < 1.0:
                        print(f"  {mc['class']}.{mc['method']}: "
                              f"{mc['pct']:.0%} ({mc['covered']}/{mc['total']})")
            if initial_coverage.uncovered_lines:
                print(f"Uncovered lines ({len(initial_coverage.uncovered_lines)} total): "
                      f"{initial_coverage.uncovered_lines[:15]}")

        # Check if done
        if self._should_stop(initial_coverage, len(test_cases)):
            if self.config.verbose:
                print("Coverage targets already met!")
            metadata.final_coverage = initial_coverage
            return test_cases, initial_coverage, metadata

        previous_coverage = initial_coverage
        stagnant_rounds = 0
        iteration = 0

        while True:
            iteration += 1
            metadata.iterations = iteration

            if self.config.verbose:
                print(f"\n--- Iteration {iteration} ---")

            # Generate supplementary tests
            new_tests = self._generate_supplementary_tests(
                test_cases, source_code, previous_coverage, imports
            )

            if not new_tests:
                if self.config.verbose:
                    print("No new tests generated")
                stagnant_rounds += 1
                if stagnant_rounds >= self.config.no_progress_patience:
                    if self.config.verbose:
                        print(f"Stopping: {stagnant_rounds} consecutive stagnant rounds")
                    break
                continue

            metadata.tests_generated += len(new_tests)

            combined = deduplicate_tests(test_cases + new_tests)
            new_only = combined[len(test_cases):]

            if not new_only:
                if self.config.verbose:
                    print("All new tests were duplicates")
                stagnant_rounds += 1
                if stagnant_rounds >= self.config.no_progress_patience:
                    break
                continue

            build_fn = lambda cases: self._build_test_content(cases, source_file, imports)
            valid_new = self.validator.validate_test_cases(
                new_only, source_file, imports, build_test_fn=build_fn
            )
            metadata.tests_discarded += len(new_only) - len(valid_new)

            if not valid_new:
                if self.config.verbose:
                    print("No valid new tests after validation")
                stagnant_rounds += 1
                if stagnant_rounds >= self.config.no_progress_patience:
                    break
                continue


            prev_count = len(test_cases)
            candidate_cases = test_cases + valid_new

            current_coverage, current_test_result = self._measure_coverage(
                candidate_cases, source_file, imports
            )


            eps = self.config.no_progress_epsilon
            line_improved = (current_coverage.line - previous_coverage.line) >= eps
            branch_improved = (current_coverage.branch - previous_coverage.branch) >= eps

            if line_improved or branch_improved:
                test_cases = candidate_cases
                stagnant_rounds = 0
                if self.config.verbose:
                    print(f"Added {len(valid_new)} tests "
                          f"({prev_count} → {len(test_cases)})")
            else:
                metadata.tests_discarded += len(valid_new)
                stagnant_rounds += 1
                if self.config.verbose:
                    print(f"Discarded {len(valid_new)} tests "
                          f"(coverage gain < ε={eps:.0%})")

            if self.config.verbose:
                print(f"Coverage: {current_coverage.line:.1%} line, "
                      f"{current_coverage.branch:.1%} branch")
                improvement = current_coverage.line - previous_coverage.line
                print(f"Line coverage improvement: {improvement:+.1%}")
                if current_coverage.method_coverage:
                    print("Method coverage (lowest first):")
                    for mc in current_coverage.method_coverage[:10]:
                        if mc["pct"] < 1.0:
                            print(f"  {mc['class']}.{mc['method']}: "
                                  f"{mc['pct']:.0%} ({mc['covered']}/{mc['total']})")
                if current_coverage.uncovered_lines:
                    print(f"Uncovered lines ({len(current_coverage.uncovered_lines)} total): "
                          f"{current_coverage.uncovered_lines[:15]}")
                if current_test_result:
                    print(f"Tests: {current_test_result.passed} passed, "
                          f"{current_test_result.failed} failed, "
                          f"{current_test_result.errors} errors")

            if not (line_improved or branch_improved):
                previous_coverage = current_coverage
                if stagnant_rounds >= self.config.no_progress_patience:
                    if self.config.verbose:
                        print(f"Stopping: {stagnant_rounds} consecutive no-progress rounds")
                    break
                continue

            # Check if done
            if self._should_stop(current_coverage, len(test_cases)):
                if self.config.verbose:
                    print("Targets met!")
                metadata.final_coverage = current_coverage
                return test_cases, current_coverage, metadata

            previous_coverage = current_coverage

        # Final coverage measurement
        final_coverage, final_test_result = self._measure_coverage(
            test_cases, source_file, imports
        )
        metadata.final_coverage = final_coverage
        metadata.fix_attempts = self.validator.fix_attempts
        metadata.llm_calls = self.llm_client.call_count

        if self.config.verbose and final_test_result:
            print(f"Final tests: {final_test_result.passed} passed, "
                  f"{final_test_result.failed} failed, "
                  f"{final_test_result.errors} errors")

        return test_cases, final_coverage, metadata

    def _should_stop(self, coverage: Coverage, test_count: int) -> bool:
        """Check if both coverage and test count targets are met."""
        coverage_met = coverage.meets_threshold(
            self.config.line_coverage_threshold,
            self.config.branch_coverage_threshold,
        )
        tests_sufficient = (
            self.config.min_tests <= 0 or test_count >= self.config.min_tests
        )
        return coverage_met and tests_sufficient

    def _build_test_content(
        self,
        test_cases: list[TestCase],
        source_file: Path,
        imports: list[str],
    ) -> str:
        """Build test file content from test cases."""
        if self._fixture_context and self._fixture_context.fixtures:
            from ..fixture.python_fixtures import PythonFixtureGenerator
            fixture_gen = PythonFixtureGenerator()
            template = fixture_gen.generate_test_template(
                self._fixture_context, source_file
            )
            test_methods = "\n\n".join(tc.code for tc in test_cases)
            return fixture_gen.combine_template_and_tests(template, test_methods)
        return self.adapter.generate_test_file(test_cases, source_file, imports)

    def _measure_coverage(
        self,
        test_cases: list[TestCase],
        source_file: Path,
        imports: list[str],
    ) -> tuple[Coverage, Optional[TestResult]]:
        """Measure coverage for the given test cases.
        """
        if not test_cases:
            return Coverage(), None

        test_content = self._build_test_content(test_cases, source_file, imports)

        with tempfile.NamedTemporaryFile(
            mode="w",
            suffix=self.adapter.get_test_file_extension(),
            delete=False,
        ) as f:
            f.write(test_content)
            test_file = Path(f.name)

        try:
            coverage, test_result = self.adapter.measure_coverage(
                test_file, source_file, self.config.timeout_seconds
            )

            return coverage, test_result
        finally:
            try:
                test_file.unlink()
            except OSError:
                pass

    def _generate_supplementary_tests(
        self,
        existing_tests: list[TestCase],
        source_code: str,
        coverage: Coverage,
        imports: list[str],
    ) -> list[TestCase]:
        """Generate supplementary tests to improve coverage."""
        existing_code = "\n\n".join(tc.code for tc in existing_tests)

        try:
            if self._fixture_context and self._fixture_context.fixtures:
                fixture_contract = self._fixture_context.get_prompt_contract()
                llm_output = self.llm_client.generate_supplementary_tests_with_fixtures(
                    source_code=source_code,
                    existing_tests=existing_code,
                    coverage=coverage,
                    fixture_contract=fixture_contract,
                    line_target=self.config.line_coverage_threshold,
                    branch_target=self.config.branch_coverage_threshold,
                )
            else:
                llm_output = self.llm_client.generate_supplementary_tests(
                    source_code=source_code,
                    existing_tests=existing_code,
                    coverage=coverage,
                    line_target=self.config.line_coverage_threshold,
                    branch_target=self.config.branch_coverage_threshold,
                )

            if self.config.verbose:
                print(f"\n{'='*60}\nLLM SUPPLEMENTARY OUTPUT:\n{'='*60}")
                print(llm_output)
                print(f"{'='*60}\n")

            return self.parser.parse(llm_output)

        except Exception as e:
            if self.config.verbose:
                print(f"Error generating supplementary tests: {e}")
            return []
