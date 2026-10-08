"""Base classes for fixture generation."""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

from ..models import FunctionInfo, Language


@dataclass
class FixtureInfo:
    """Information about a generated fixture.

    Attributes:
        name: Variable/fixture name (e.g., "renko", "sample_df")
        type_name: Type of the fixture (e.g., "Renko", "DataFrame")
        description: Human-readable description
        initialization: How the fixture is initialized (e.g., "Renko(df)")
        available_methods: List of methods available on this fixture
        example_usage: Example of how to use this fixture in a test
        depends_on: Names of other fixtures this one depends on
    """
    name: str
    type_name: str
    description: str = ""
    initialization: str = ""
    available_methods: list[str] = field(default_factory=list)
    example_usage: str = ""
    depends_on: list[str] = field(default_factory=list)

    def to_prompt_str(self) -> str:
        """Convert fixture info to a string for LLM prompt."""
        methods_str = ", ".join(self.available_methods[:5])  # Limit to 5 methods
        if len(self.available_methods) > 5:
            methods_str += ", ..."

        return (
            f"- `{self.name}` ({self.type_name}): {self.description}\n"
            f"  Methods: {methods_str or 'N/A'}\n"
            f"  Example: {self.example_usage or self.initialization}"
        )


@dataclass
class FixtureContext:
    """Complete fixture context for test generation.

    Attributes:
        fixtures: List of available fixtures
        imports: Import statements needed
        setup_code: Complete fixture/setup code (Python fixtures or Java @BeforeEach)
        fixture_names: List of fixture names for validation
    """
    fixtures: list[FixtureInfo] = field(default_factory=list)
    imports: list[str] = field(default_factory=list)
    setup_code: str = ""
    fixture_names: list[str] = field(default_factory=list)

    def get_prompt_contract(self) -> str:
        """Generate the fixture contract for the LLM prompt (Python pytest fixtures)."""
        if not self.fixtures:
            return "No fixtures available. Create instances directly in tests."

        lines = [
            "## Available Test Fixtures (DO NOT redefine these):",
            "",
        ]
        for fixture in self.fixtures:
            lines.append(fixture.to_prompt_str())
            lines.append("")

        lines.extend([
            "## Rules:",
            "1. Fixtures are initialized with DEFAULT values (collections may be empty)",
            "2. Each test should set up its own test data by calling methods on the fixtures",
            "3. Do NOT redefine imports or fixtures",
            "4. Each test function should accept the fixtures it needs as parameters",
            "",
        ])
        return "\n".join(lines)

    def get_fixture_names_str(self) -> str:
        """Get comma-separated list of fixture names."""
        return ", ".join(f"`{f.name}`" for f in self.fixtures)


class FixtureGenerator(ABC):
    """Abstract base class for fixture generators."""

    @property
    @abstractmethod
    def language(self) -> Language:
        """Return the language this generator handles."""
        pass

    @abstractmethod
    def generate_fixtures(
        self,
        source_code: str,
        source_file: Path,
        functions: list[FunctionInfo],
    ) -> FixtureContext:
        """Generate fixtures for the given source code.

        Args:
            source_code: The source code to analyze.
            source_file: Path to the source file.
            functions: Extracted function information.

        Returns:
            FixtureContext with all fixture information.
        """
        pass

    @abstractmethod
    def generate_test_template(
        self,
        context: FixtureContext,
        source_file: Path,
    ) -> str:
        """Generate the test file template (imports + fixtures).

        Args:
            context: The fixture context.
            source_file: Path to the source file being tested.

        Returns:
            Template code string (everything except test methods).
        """
        pass

    @abstractmethod
    def combine_template_and_tests(
        self,
        template: str,
        test_methods: str,
    ) -> str:
        """Combine the template with LLM-generated test methods.

        Args:
            template: The fixture template code.
            test_methods: LLM-generated test method code.

        Returns:
            Complete test file content.
        """
        pass

    def validate_fixture_usage(
        self,
        test_code: str,
        context: FixtureContext,
    ) -> list[str]:
        """Validate that test code correctly uses fixtures.

        Args:
            test_code: The test code to validate.
            context: The fixture context.

        Returns:
            List of validation error messages (empty if valid).
        """
        # Default implementation - subclasses can override
        return []
