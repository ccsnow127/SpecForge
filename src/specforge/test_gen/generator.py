"""Main test generator orchestrator (Python only)."""

from pathlib import Path
from typing import Optional

from .analyzer.python_adapter import PythonAdapter
from .config import TestGeneratorConfig
from .coverage.feedback_loop import CoverageFeedbackLoop
from .fixture.base import FixtureContext
from .fixture.python_fixtures import PythonFixtureGenerator
from .llm.client import LLMClient
from .llm.parser import LLMOutputParser
from .models import Language, TestSuite
from .utils.deduplication import deduplicate_tests
from .validator.validator import TestValidator


class TestGenerator:
    """Main orchestrator for test generation.

    Pipeline:
        1. Analyze Python source to extract functions
        2. Generate initial tests using LLM
        3. Validate, deduplicate, filter (direct-invocation)
        4. Run coverage feedback loop until coverage thresholds met or no-progress early-stop
        5. Produce final test suite
    """

    def __init__(self, config: Optional[TestGeneratorConfig] = None):
        self.config = config or TestGeneratorConfig()
        self._adapter: Optional[PythonAdapter] = None
        self._fixture_generator: Optional[PythonFixtureGenerator] = None

    def generate(
        self,
        source_file: str | Path,
        output_file: Optional[str | Path] = None,
    ) -> TestSuite:
        """Generate Python tests for a source file."""
        source_path = Path(source_file)
        if not source_path.exists():
            raise FileNotFoundError(f"Source file not found: {source_file}")
        if source_path.suffix.lower() != ".py":
            raise ValueError(
                f"Only Python source files are supported in this release; "
                f"got {source_path.suffix!r}."
            )

        adapter = self._get_adapter()
        source_code = source_path.read_text()

        if self.config.verbose:
            print("Extracting functions...")

        functions = adapter.extract_functions(source_code)
        if not functions:
            if self.config.verbose:
                print("No testable functions found")
            return TestSuite(
                module_name=source_path.stem,
                language=Language.PYTHON,
                test_file_content="",
                source_file=str(source_path),
            )

        if self.config.verbose:
            print(f"Found {len(functions)} functions to test")

        imports = adapter.get_imports(source_code)
        constructors = adapter.get_constructors(source_code)
        dependencies = adapter.identify_external_dependencies(source_code)

        self.config.validate()
        llm_client = LLMClient(self.config)

        # Pytest fixtures isolate external dependencies (filesystem/network/time/random)
        fixture_generator = self._get_fixture_generator()
        fixture_context = fixture_generator.generate_fixtures(
            source_code=source_code,
            source_file=source_path,
            functions=functions,
        )
        use_fixtures = bool(fixture_context.fixtures)

        parser = LLMOutputParser(Language.PYTHON, include_class_setup=False)

        if self.config.verbose and fixture_context.fixtures:
            print(f"Generated {len(fixture_context.fixtures)} fixtures: "
                  f"{', '.join(f.name for f in fixture_context.fixtures)}")
            print("Generating initial tests...")

        if use_fixtures:
            fixture_contract = fixture_context.get_prompt_contract()
            llm_output = llm_client.generate_tests_with_fixtures(
                source_code=source_code,
                functions=functions,
                fixture_contract=fixture_contract,
            )
        else:
            llm_output = llm_client.generate_tests(
                source_code=source_code,
                functions=functions,
                constructors=constructors,
                dependencies=dependencies,
            )

        if self.config.verbose:
            print(f"\n{'='*60}\nLLM INITIAL OUTPUT:\n{'='*60}")
            print(llm_output)
            print(f"{'='*60}\n")

        test_cases = parser.parse(llm_output)
        if self.config.verbose:
            print(f"Generated {len(test_cases)} test cases")

        test_imports = parser.extract_imports(llm_output)
        all_imports = list(set(imports + test_imports + fixture_context.imports))

        validator = TestValidator(adapter, llm_client, self.config)

        if self.config.verbose:
            print("Validating tests...")

        if use_fixtures:
            fg = self._get_fixture_generator()
            build_fn = lambda cases: fg.combine_template_and_tests(
                fg.generate_test_template(fixture_context, source_path),
                "\n\n".join(tc.code for tc in cases),
            )
        else:
            build_fn = None

        test_cases = validator.validate_test_cases(
            test_cases, source_path, all_imports, build_test_fn=build_fn
        )
        test_cases = deduplicate_tests(test_cases)

        if self.config.verbose:
            print(f"{len(test_cases)} valid test cases after validation")
            print("\nStarting coverage feedback loop...")

        feedback_loop = CoverageFeedbackLoop(
            adapter=adapter,
            llm_client=llm_client,
            parser=parser,
            validator=validator,
            config=self.config,
        )

        final_tests, coverage, metadata = feedback_loop.run(
            initial_tests=test_cases,
            source_file=source_path,
            source_code=source_code,
            imports=all_imports,
            fixture_context=fixture_context,
        )

        if use_fixtures:
            fg = self._get_fixture_generator()
            template = fg.generate_test_template(fixture_context, source_path)
            test_methods_content = "\n\n".join(tc.code for tc in final_tests)
            test_content = fg.combine_template_and_tests(template, test_methods_content)
        else:
            test_content = adapter.generate_test_file(final_tests, source_path, all_imports)

        if output_file:
            output_path = Path(output_file)
        else:
            output_path = source_path.parent / adapter.get_test_file_name(source_path)

        if self.config.verbose:
            print("\nFinal validation...")

        success, result, test_content = validator.validate_test_file(
            test_content, source_path
        )

        if self.config.verbose:
            if success:
                print(f"All {result.passed} tests pass!")
            else:
                print(f"Warning: {result.failed} failures, {result.errors} errors remain")

        test_suite = TestSuite(
            module_name=source_path.stem,
            language=Language.PYTHON,
            test_file_content=test_content,
            test_cases=final_tests,
            coverage=coverage,
            generation_metadata=metadata,
            source_file=str(source_path),
            test_file_path=str(output_path),
        )

        if output_file:
            if self.config.verbose:
                print(f"\nWriting tests to {output_path}")
            output_path.write_text(test_content)

        if self.config.verbose:
            self._print_summary(test_suite)

        return test_suite

    def _get_adapter(self) -> PythonAdapter:
        if self._adapter is None:
            self._adapter = PythonAdapter()
        return self._adapter

    def _get_fixture_generator(self) -> PythonFixtureGenerator:
        if self._fixture_generator is None:
            self._fixture_generator = PythonFixtureGenerator()
        return self._fixture_generator

    def _print_summary(self, test_suite: TestSuite) -> None:
        """Print a summary of test generation."""
        print("\n" + "=" * 50)
        print("TEST GENERATION SUMMARY")
        print("=" * 50)
        print(f"Module: {test_suite.module_name}")
        print(f"Test cases: {len(test_suite.test_cases)}")

        if test_suite.coverage:
            cov = test_suite.coverage
            print(f"\nCoverage:")
            print(f"  Line: {cov.line:.1%} ({cov.covered_lines}/{cov.total_lines})")
            print(f"  Branch: {cov.branch:.1%} ({cov.covered_branches}/{cov.total_branches})")

            line_met = cov.line >= self.config.line_coverage_threshold
            branch_met = cov.branch >= self.config.branch_coverage_threshold
            print(f"\nTargets:")
            print(f"  Line ({self.config.line_coverage_threshold:.0%}): {'OK' if line_met else 'FAIL'}")
            print(f"  Branch ({self.config.branch_coverage_threshold:.0%}): {'OK' if branch_met else 'FAIL'}")

        if test_suite.generation_metadata:
            meta = test_suite.generation_metadata
            print(f"\nGeneration stats:")
            print(f"  Iterations: {meta.iterations}")
            print(f"  Tests generated: {meta.tests_generated}")
            print(f"  Tests discarded: {meta.tests_discarded}")
            print(f"  Fix attempts: {meta.fix_attempts}")
            print(f"  LLM calls: {meta.llm_calls}")

        print(f"\nOutput: {test_suite.test_file_path}")
        print("=" * 50)
