"""OpenAI client for the test generator (Python only).

Wraps chat-completion calls used during test synthesis and repair.
"""

import time

from ..config import TestGeneratorConfig
from ..models import FunctionInfo, DependencyInfo, Coverage
from .prompts import (
    INITIAL_TEST_PROMPT,
    COVERAGE_SUPPLEMENT_PROMPT,
    FIX_TEST_PROMPT,
    FIXTURE_AWARE_TEST_PROMPT,
    FIXTURE_AWARE_SUPPLEMENT_PROMPT,
    get_test_framework_description,
)


class LLMClient:
    """LLM client for generating and fixing Python tests."""

    def __init__(self, config: TestGeneratorConfig):
        self.config = config
        config.validate()

        try:
            import openai
            self._openai = openai
        except ImportError:
            raise ImportError(
                "openai package is required. Install it with: pip install openai"
            )

        self.client = openai.OpenAI(api_key=config.openai_api_key)
        self.call_count = 0

    def generate_tests(
        self,
        source_code: str,
        functions: list[FunctionInfo],
        constructors: list[str],
        dependencies: list[DependencyInfo],
    ) -> str:
        """Generate initial tests for the given source code."""
        functions_str = "\n".join(
            f"- {f.signature}" + (f"\n  Docstring: {f.docstring}" if f.docstring else "")
            for f in functions
        )
        constructors_str = "\n".join(f"- {c}" for c in constructors) or "None"
        deps_str = "\n".join(
            f"- {d.name} ({d.type}): {d.module_path or 'N/A'}"
            for d in dependencies if d.needs_mock
        ) or "None"

        prompt = INITIAL_TEST_PROMPT.format(
            source_code=source_code,
            functions=functions_str,
            constructors=constructors_str,
            dependencies=deps_str,
            test_framework=get_test_framework_description(),
        )
        return self._call_llm(prompt)

    def generate_tests_with_fixtures(
        self,
        source_code: str,
        functions: list[FunctionInfo],
        fixture_contract: str,
    ) -> str:
        """Generate tests using pre-defined pytest fixtures."""
        functions_str = "\n".join(
            f"- {f.signature}" + (f"\n  Docstring: {f.docstring}" if f.docstring else "")
            for f in functions
        )

        prompt = FIXTURE_AWARE_TEST_PROMPT.format(
            fixture_contract=fixture_contract,
            source_code=source_code,
            functions=functions_str,
            test_framework=get_test_framework_description(),
        )
        response = self._call_llm(prompt)
        return self._remove_markdown(response)

    def generate_supplementary_tests_with_fixtures(
        self,
        source_code: str,
        existing_tests: str,
        coverage: Coverage,
        fixture_contract: str,
        line_target: float,
        branch_target: float,
    ) -> str:
        """Generate additional tests using pre-defined pytest fixtures."""
        entity_str = self._format_entity_coverage(coverage)
        uncovered_str = self._format_uncovered(source_code, coverage)

        prompt = FIXTURE_AWARE_SUPPLEMENT_PROMPT.format(
            fixture_contract=fixture_contract,
            line_coverage=coverage.line,
            branch_coverage=coverage.branch,
            line_target=line_target,
            branch_target=branch_target,
            entity_coverage=entity_str,
            uncovered_lines=uncovered_str,
            source_code=source_code,
            existing_tests=existing_tests,
        )
        response = self._call_llm(prompt)
        return self._remove_markdown(response)

    def generate_supplementary_tests(
        self,
        source_code: str,
        existing_tests: str,
        coverage: Coverage,
        line_target: float,
        branch_target: float,
    ) -> str:
        """Generate additional tests to improve coverage."""
        entity_str = self._format_entity_coverage(coverage)
        uncovered_str = self._format_uncovered(source_code, coverage)

        prompt = COVERAGE_SUPPLEMENT_PROMPT.format(
            line_coverage=coverage.line,
            branch_coverage=coverage.branch,
            line_target=line_target,
            branch_target=branch_target,
            entity_coverage=entity_str,
            uncovered_lines=uncovered_str,
            source_code=source_code,
            existing_tests=existing_tests,
            test_framework=get_test_framework_description(),
        )
        return self._call_llm(prompt)

    def fix_test(
        self,
        test_code: str,
        error_message: str,
        source_code: str,
    ) -> str:
        """Attempt to fix a failing test."""
        prompt = FIX_TEST_PROMPT.format(
            error_message=error_message,
            test_code=test_code,
            source_code=source_code,
        )
        response = self._call_llm(prompt)
        return self._remove_markdown(response)

    @staticmethod
    def _format_entity_coverage(coverage: Coverage) -> str:
        entity_lines = [
            f"- {mc['class']}.{mc['method']}: "
            f"{mc['pct']:.0%} ({mc['covered']}/{mc['total']} lines)"
            for mc in getattr(coverage, 'method_coverage', [])
            if mc["pct"] < 1.0
        ]
        return "\n".join(entity_lines[:10]) or "All methods covered"

    @staticmethod
    def _format_uncovered(source_code: str, coverage: Coverage) -> str:
        source_lines = source_code.splitlines()
        ctx = []
        for line_num in coverage.uncovered_lines[:20]:
            if 0 < line_num <= len(source_lines):
                ctx.append(f"Line {line_num}: {source_lines[line_num - 1]}")
        return "\n".join(ctx) or "All lines covered"

    def _remove_markdown(self, text: str) -> str:
        """Remove markdown code block markers from LLM output."""
        import re
        text = re.sub(r"^```\w*\n?", "", text, flags=re.MULTILINE)
        text = re.sub(r"\n?```$", "", text, flags=re.MULTILINE)
        text = re.sub(r"^```$", "", text, flags=re.MULTILINE)
        return text.strip()

    def _call_llm(self, prompt: str, max_retries: int = 3) -> str:
        """Call the LLM with retry + exponential backoff on rate limit / API errors."""
        last_exception = None
        for attempt in range(max_retries):
            try:
                response = self.client.chat.completions.create(
                    model=self.config.llm_model,
                    messages=[
                        {
                            "role": "system",
                            "content": "You are an expert software tester. Generate high-quality, runnable Python test code.",
                        },
                        {"role": "user", "content": prompt},
                    ],
                    temperature=self.config.llm_temperature,
                    max_tokens=self.config.llm_max_tokens,
                )
                self.call_count += 1
                return response.choices[0].message.content or ""

            except self._openai.RateLimitError as e:
                last_exception = e
                wait_time = 2 ** attempt
                if self.config.verbose:
                    print(f"Rate limited, waiting {wait_time}s...")
                time.sleep(wait_time)

            except self._openai.APIError as e:
                last_exception = e
                if attempt < max_retries - 1:
                    time.sleep(2 ** attempt)
                else:
                    raise

        raise last_exception or Exception("LLM call failed after retries")
