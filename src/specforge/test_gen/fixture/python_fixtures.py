"""Python pytest fixture generator."""

import ast
import re
from pathlib import Path
from typing import Optional

from .base import FixtureGenerator, FixtureInfo, FixtureContext
from ..models import FunctionInfo, Language


class PythonFixtureGenerator(FixtureGenerator):
    """Generate pytest fixtures for Python source code."""

    @property
    def language(self) -> Language:
        return Language.PYTHON

    def generate_fixtures(
        self,
        source_code: str,
        source_file: Path,
        functions: list[FunctionInfo],
    ) -> FixtureContext:
        """Generate pytest fixtures from source code analysis."""
        context = FixtureContext()

        try:
            tree = ast.parse(source_code)
        except SyntaxError:
            return context

        module_name = source_file.stem

        # Extract class information
        classes = self._extract_classes(tree)

        # Generate imports
        context.imports = self._generate_imports(module_name, classes)

        # Generate fixtures for each class
        fixtures = []
        for cls_info in classes:
            fixture = self._create_class_fixture(cls_info, classes)
            if fixture:
                fixtures.append(fixture)

        # Add sample data fixtures if needed
        sample_fixtures = self._create_sample_data_fixtures(classes)
        fixtures = sample_fixtures + fixtures

        context.fixtures = fixtures
        context.fixture_names = [f.name for f in fixtures]

        # Generate setup code
        context.setup_code = self._generate_setup_code(fixtures, module_name, classes)

        return context

    def _extract_classes(self, tree: ast.AST) -> list[dict]:
        """Extract class information from AST."""
        classes = []
        class_nodes = {}  # name -> node for inheritance lookup

        # First pass: collect all class nodes
        for node in ast.walk(tree):
            if isinstance(node, ast.ClassDef):
                class_nodes[node.name] = node

        # Second pass: extract class info with inheritance
        for node in ast.walk(tree):
            if isinstance(node, ast.ClassDef):
                cls_info = {
                    "name": node.name,
                    "init_params": [],
                    "methods": [],
                    "needs_dataframe": False,
                    "needs_list": False,
                    "bases": [ast.unparse(base) for base in node.bases],
                }

                # Check for __init__ in this class
                has_own_init = False
                for item in node.body:
                    # Extract __init__ parameters
                    if isinstance(item, ast.FunctionDef) and item.name == "__init__":
                        has_own_init = True
                        for arg in item.args.args[1:]:  # Skip self
                            param_name = arg.arg
                            param_type = None
                            if arg.annotation:
                                param_type = ast.unparse(arg.annotation)
                            cls_info["init_params"].append({
                                "name": param_name,
                                "type": param_type,
                            })
                            # Check if needs DataFrame or list
                            if param_type:
                                if "DataFrame" in param_type:
                                    cls_info["needs_dataframe"] = True
                                elif "list" in param_type.lower() or "List" in param_type:
                                    cls_info["needs_list"] = True

                    # Extract public methods
                    elif isinstance(item, ast.FunctionDef) and not item.name.startswith("_"):
                        cls_info["methods"].append(item.name + "()")

                # If no own __init__, inherit from base class
                if not has_own_init and cls_info["bases"]:
                    for base_name in cls_info["bases"]:
                        if base_name in class_nodes:
                            base_node = class_nodes[base_name]
                            for item in base_node.body:
                                if isinstance(item, ast.FunctionDef) and item.name == "__init__":
                                    for arg in item.args.args[1:]:
                                        param_name = arg.arg
                                        param_type = None
                                        if arg.annotation:
                                            param_type = ast.unparse(arg.annotation)
                                        cls_info["init_params"].append({
                                            "name": param_name,
                                            "type": param_type,
                                        })
                                        if param_type and "DataFrame" in param_type:
                                            cls_info["needs_dataframe"] = True
                                    break

                classes.append(cls_info)

        return classes

    def _generate_imports(self, module_name: str, classes: list[dict]) -> list[str]:
        """Generate import statements."""
        imports = [
            "import pytest",
        ]

        # Check if any class needs DataFrame
        needs_pandas = any(cls["needs_dataframe"] for cls in classes)
        if needs_pandas:
            imports.append("import pandas as pd")
            imports.append("from datetime import datetime")

        # Import from source module
        class_names = [cls["name"] for cls in classes]
        if class_names:
            imports.append(f"from {module_name} import {', '.join(class_names)}")

        return imports

    def _create_sample_data_fixtures(self, classes: list[dict]) -> list[FixtureInfo]:
        """Create sample data fixtures (DataFrame, etc.)."""
        fixtures = []

        # Check if any class needs DataFrame
        needs_dataframe = any(cls["needs_dataframe"] for cls in classes)
        if needs_dataframe:
            fixtures.append(FixtureInfo(
                name="sample_df",
                type_name="pd.DataFrame",
                description="Sample OHLC DataFrame with 10 rows",
                initialization="pd.DataFrame({...})",
                available_methods=["head()", "tail()", "iloc[]", "loc[]", "columns"],
                example_usage="assert len(sample_df) == 10",
                depends_on=[],
            ))

        return fixtures

    def _create_class_fixture(
        self,
        cls_info: dict,
        all_classes: list[dict],
    ) -> Optional[FixtureInfo]:
        """Create a fixture for a class."""
        cls_name = cls_info["name"]
        fixture_name = self._to_snake_case(cls_name)

        # Determine dependencies
        depends_on = []
        init_args = []

        for param in cls_info["init_params"]:
            param_type = param.get("type", "")
            if param_type and "DataFrame" in param_type:
                depends_on.append("sample_df")
                init_args.append("sample_df")
            elif param_type and any(c["name"] in param_type for c in all_classes):
                # Depends on another class
                dep_name = self._to_snake_case(param_type.split("[")[0])
                depends_on.append(dep_name)
                init_args.append(dep_name)
            else:
                # Use default value
                init_args.append(self._get_default_value(param))

        initialization = f"{cls_name}({', '.join(init_args)})"

        return FixtureInfo(
            name=fixture_name,
            type_name=cls_name,
            description=f"{cls_name} instance for testing",
            initialization=initialization,
            available_methods=cls_info["methods"][:10],  # Limit methods
            example_usage=f"result = {fixture_name}.{cls_info['methods'][0] if cls_info['methods'] else 'method()'}",
            depends_on=depends_on,
        )

    def _get_default_value(self, param: dict) -> str:
        """Get a default value for a parameter."""
        param_type = param.get("type", "")
        param_name = param.get("name", "")

        if not param_type:
            # Guess from name
            if "size" in param_name or "count" in param_name or "num" in param_name:
                return "10"
            elif "name" in param_name:
                return "'test'"
            elif "flag" in param_name or "enable" in param_name:
                return "True"
            return "None"

        if "int" in param_type.lower():
            return "10"
        elif "float" in param_type.lower():
            return "1.0"
        elif "str" in param_type.lower():
            return "'test'"
        elif "bool" in param_type.lower():
            return "True"
        elif "list" in param_type.lower():
            return "[]"
        elif "dict" in param_type.lower():
            return "{}"
        return "None"

    def _to_snake_case(self, name: str) -> str:
        """Convert CamelCase to snake_case."""
        s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
        return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

    def _generate_setup_code(
        self,
        fixtures: list[FixtureInfo],
        module_name: str,
        classes: list[dict],
    ) -> str:
        """Generate the complete fixture setup code."""
        lines = []

        # Add imports
        imports = self._generate_imports(module_name, classes)
        lines.extend(imports)
        lines.append("")
        lines.append("")

        # Check if we need sample DataFrame
        needs_dataframe = any(f.name == "sample_df" for f in fixtures)
        if needs_dataframe:
            # Generate multi-row OHLC data with trend reversals to exercise all algorithm branches
            lines.extend([
                "@pytest.fixture",
                "def sample_df():",
                '    """Sample OHLC DataFrame with trend reversals for testing algorithms."""',
                "    # Data pattern: uptrend -> big drop (reversal) -> downtrend -> big rise (reversal)",
                "    return pd.DataFrame({",
                "        'date': [datetime(2023, 1, i+1) for i in range(6)],",
                "        'open':  [100.0, 101.0, 104.0, 107.0, 94.0, 88.0],",
                "        'high':  [102.0, 105.0, 108.0, 108.0, 95.0, 115.0],",
                "        'low':   [99.0, 100.0, 103.0, 92.0, 85.0, 87.0],",
                "        'close': [101.0, 104.0, 107.0, 94.0, 88.0, 112.0],",
                "    })",
                "",
                "",
            ])

        # Generate fixtures for classes
        for fixture in fixtures:
            if fixture.name == "sample_df":
                continue  # Already handled

            # Build fixture function
            deps = fixture.depends_on
            params = ", ".join(deps) if deps else ""

            lines.append("@pytest.fixture")
            lines.append(f"def {fixture.name}({params}):")
            lines.append(f'    """{fixture.description}"""')
            lines.append(f"    return {fixture.initialization}")
            lines.append("")
            lines.append("")

        return "\n".join(lines)

    def generate_test_template(
        self,
        context: FixtureContext,
        source_file: Path,
    ) -> str:
        """Generate complete test file template."""
        lines = [
            f'"""Tests for {source_file.name}"""',
            "",
        ]

        lines.append(context.setup_code)

        # Add marker for where tests go
        lines.append("# === TEST METHODS BELOW ===")
        lines.append("")

        return "\n".join(lines)

    def combine_template_and_tests(
        self,
        template: str,
        test_methods: str,
    ) -> str:
        """Combine template with LLM-generated tests."""
        # Find the marker and replace
        marker = "# === TEST METHODS BELOW ==="
        if marker in template:
            return template.replace(marker, test_methods.strip())

        # If no marker, just append
        return template + "\n" + test_methods

    def validate_fixture_usage(
        self,
        test_code: str,
        context: FixtureContext,
    ) -> list[str]:
        """Validate that test code correctly uses fixtures."""
        errors = []

        # Check for illegal instantiation patterns
        for fixture in context.fixtures:
            if fixture.name == "sample_df":
                # Check for direct DataFrame creation
                if re.search(r"pd\.DataFrame\s*\(", test_code):
                    errors.append(
                        f"Do not create DataFrame directly. Use fixture `sample_df` instead."
                    )
            else:
                # Check for direct class instantiation
                pattern = rf"=\s*{fixture.type_name}\s*\("
                if re.search(pattern, test_code):
                    errors.append(
                        f"Do not instantiate {fixture.type_name} directly. "
                        f"Use fixture `{fixture.name}` instead."
                    )

        # Check for fixture redefinition
        if "@pytest.fixture" in test_code:
            errors.append("Do not define new fixtures. Use the provided fixtures.")

        # Check for import statements
        if re.search(r"^import |^from ", test_code, re.MULTILINE):
            errors.append("Do not include import statements. Imports are provided.")

        return errors
