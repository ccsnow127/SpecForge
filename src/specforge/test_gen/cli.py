"""Command-line interface for the test generator."""

import argparse
import sys
from pathlib import Path

from .config import TestGeneratorConfig
from .generator import TestGenerator


def main(args: list[str] | None = None) -> int:
    """Main entry point for the CLI.

    Args:
        args: Command-line arguments (defaults to sys.argv[1:]).

    Returns:
        Exit code (0 for success, non-zero for failure).
    """
    parser = argparse.ArgumentParser(
        prog="test_generator",
        description="Generate Python unit tests for source code using LLM (OpenAI GPT-4o)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate tests for a Python file
  python -m test_generator example.py -o test_example.py

  # Specify custom coverage targets
  python -m test_generator example.py --line-coverage 0.90 --branch-coverage 0.80

  # Use a different model
  python -m test_generator example.py --model gpt-4-turbo

Environment:
  OPENAI_API_KEY    Required. Your OpenAI API key.
""",
    )

    parser.add_argument(
        "source_file",
        type=str,
        help="Path to the Python source file to generate tests for",
    )

    parser.add_argument(
        "-o", "--output",
        type=str,
        help="Output test file path (defaults to test_<source>.py)",
    )

    parser.add_argument(
        "--line-coverage",
        type=float,
        default=0.70,
        help="Target line coverage (0.0-1.0, default: 0.70)",
    )

    parser.add_argument(
        "--branch-coverage",
        type=float,
        default=0.90,
        help="Target branch coverage (0.0-1.0, default: 0.90)",
    )

    parser.add_argument(
        "--model",
        type=str,
        default="gpt-4o",
        help="OpenAI model to use (default: gpt-4o)",
    )

    parser.add_argument(
        "--max-fix-attempts",
        type=int,
        default=2,
        help="Maximum attempts to fix failing tests (default: 2)",
    )

    parser.add_argument(
        "--timeout",
        type=int,
        default=60,
        help="Timeout for test execution in seconds (default: 60)",
    )

    parser.add_argument(
        "--min-tests",
        type=int,
        default=0,
        help="Minimum number of test cases; keeps generating if below this (default: 0 = no minimum)",
    )

    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Enable verbose output",
    )

    parser.add_argument(
        "--version",
        action="version",
        version="%(prog)s 1.0.0",
    )

    parsed_args = parser.parse_args(args)

    # Validate source file exists
    source_path = Path(parsed_args.source_file)
    if not source_path.exists():
        print(f"Error: Source file not found: {parsed_args.source_file}", file=sys.stderr)
        return 1

    # Create configuration
    try:
        config = TestGeneratorConfig(
            llm_model=parsed_args.model,
            line_coverage_threshold=parsed_args.line_coverage,
            branch_coverage_threshold=parsed_args.branch_coverage,
            max_fix_attempts=parsed_args.max_fix_attempts,
            timeout_seconds=parsed_args.timeout,
            verbose=parsed_args.verbose,
            min_tests=parsed_args.min_tests,
        )
    except ValueError as e:
        print(f"Configuration error: {e}", file=sys.stderr)
        return 1

    # Validate API key is available
    try:
        config.validate()
    except ValueError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1

    # Create generator and run
    generator = TestGenerator(config)

    try:
        if parsed_args.verbose:
            print(f"Generating tests for: {source_path}")
            print(f"Model: {config.llm_model}")
            print(f"Coverage targets: {config.line_coverage_threshold:.0%} line, "
                  f"{config.branch_coverage_threshold:.0%} branch")
            print()

        test_suite = generator.generate(
            source_file=source_path,
            output_file=parsed_args.output,
        )

        # Report results
        if not parsed_args.verbose:
            # Minimal output for non-verbose mode
            print(f"Generated {len(test_suite.test_cases)} tests", file=sys.stderr)
            if test_suite.coverage:
                print(f"Coverage: {test_suite.coverage.line:.1%} line, "
                      f"{test_suite.coverage.branch:.1%} branch", file=sys.stderr)

        # Print test content to stdout if no output file specified
        if not parsed_args.output:
            print(test_suite.test_file_content)
        elif not parsed_args.verbose:
            print(f"Output: {test_suite.test_file_path}", file=sys.stderr)

        # Check if coverage targets were met
        if test_suite.coverage:
            if test_suite.coverage.meets_threshold(
                config.line_coverage_threshold,
                config.branch_coverage_threshold,
            ):
                return 0
            else:
                if parsed_args.verbose:
                    print("\nWarning: Coverage targets not fully met")
                return 0  # Still success, just with warning

        return 0

    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    except ValueError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    except ImportError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    except Exception as e:
        print(f"Unexpected error: {e}", file=sys.stderr)
        if parsed_args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
