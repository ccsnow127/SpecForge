"""Configuration for the Test Generator module."""

import os
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class TestGeneratorConfig:
    """Configuration class for test generation.

    Termination follows paper §App.testcases: the feedback loop stops when
    (1) both coverage thresholds are met (primary), or
    (2) `no_progress_patience` consecutive iterations contribute less than
        `no_progress_epsilon` additional coverage (no-progress early-stop).

    Termination is guaranteed: branch coverage is bounded above by 1.0,
    so at most floor(1 / no_progress_epsilon) iterations can each contribute
    a gain of at least no_progress_epsilon; the loop cannot run indefinitely
    without entering a sustained no-progress regime that triggers the early-stop.

    Attributes:
        llm_model: OpenAI model to use for test generation.
        llm_temperature: Temperature for LLM generation (lower = more deterministic).
        llm_max_tokens: Maximum tokens for LLM response.
        line_coverage_threshold: Target line coverage (0.0 to 1.0).
        branch_coverage_threshold: Target branch coverage (0.0 to 1.0).
        no_progress_epsilon: Minimum per-iteration coverage gain (in either
            line or branch) below which the iteration counts as "no progress".
        no_progress_patience: Number of consecutive no-progress iterations
            before the loop terminates.
        max_fix_attempts: Maximum attempts to fix a failing test.
        openai_api_key: OpenAI API key (defaults to OPENAI_API_KEY env var).
        verbose: Enable verbose output.
        timeout_seconds: Timeout for test execution in seconds.
    """

    llm_model: str = "gpt-4o"
    llm_temperature: float = 0.2
    llm_max_tokens: int = 4096
    line_coverage_threshold: float = 0.70
    branch_coverage_threshold: float = 0.90
    no_progress_epsilon: float = 0.01
    no_progress_patience: int = 2
    max_fix_attempts: int = 2
    openai_api_key: Optional[str] = field(default=None)
    verbose: bool = False
    timeout_seconds: int = 60
    min_tests: int = 0

    def __post_init__(self):
        """Load API key from environment if not provided."""
        if self.openai_api_key is None:
            self.openai_api_key = os.environ.get("OPENAI_API_KEY")

        # Validate thresholds
        if not 0.0 <= self.line_coverage_threshold <= 1.0:
            raise ValueError("line_coverage_threshold must be between 0.0 and 1.0")
        if not 0.0 <= self.branch_coverage_threshold <= 1.0:
            raise ValueError("branch_coverage_threshold must be between 0.0 and 1.0")
        if not 0.0 <= self.no_progress_epsilon <= 1.0:
            raise ValueError("no_progress_epsilon must be between 0.0 and 1.0")
        if self.no_progress_patience < 1:
            raise ValueError("no_progress_patience must be at least 1")
        if self.max_fix_attempts < 0:
            raise ValueError("max_fix_attempts cannot be negative")

    def validate(self) -> None:
        """Validate that all required configuration is present.

        Raises:
            ValueError: If required configuration is missing.
        """
        if not self.openai_api_key:
            raise ValueError(
                "OpenAI API key is required. Set OPENAI_API_KEY environment variable "
                "or pass openai_api_key to TestGeneratorConfig."
            )
