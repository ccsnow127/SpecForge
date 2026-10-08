"""
Test runner: executes pytest against generated code in a sandboxed subprocess.
Injects generated code into the target module, runs pytest, and attributes pass/fail per entity via `test_entity_map.json`.
"""

import json
import os
import re
import subprocess
import sys
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Optional


@dataclass
class TestResult:
    passed: int
    failed: int
    total: int
    errors: list[str]
    output: str = ""
    failed_per_entity: dict = None
    tested_entities: set = None

    def __post_init__(self):
        if self.failed_per_entity is None:
            self.failed_per_entity = {}
        if self.tested_entities is None:
            self.tested_entities = set()

    @property
    def pass_rate(self) -> float:
        return self.passed / self.total if self.total > 0 else 0.0

    @property
    def success(self) -> bool:
        return self.failed == 0 and self.total > 0


class TestExecutor:
    def __init__(
        self,
        test_suite_path: str,
        target_module: str,
        repo_path: str = ".",
        timeout: int = 120,
        per_test_timeout: int = 10,
    ):
        self.test_suite_path = test_suite_path
        self.target_module = target_module
        self.repo_path = Path(repo_path).resolve()
        self.timeout = timeout
        self.per_test_timeout = per_test_timeout
        self._entity_map = self._load_entity_map()

    def _load_entity_map(self) -> dict[str, str]:
        map_path = Path(self.test_suite_path).parent / "test_entity_map.json"
        if map_path.exists():
            try:
                return json.loads(map_path.read_text())
            except (json.JSONDecodeError, OSError):
                return {}
        return {}

    def run(self, generated_code: str, iteration: int = 0) -> TestResult:
        return self._run_tests(generated_code)

    def _run_tests(self, code: str) -> TestResult:
        try:
            compile(code, '<generated>', 'exec')
        except SyntaxError as e:
            return TestResult(
                passed=0, failed=1, total=1,
                errors=[f"SyntaxError: {e}"], output=str(e),
            )

        runner_script = self._create_runner_script(code)
        test_path = Path(self.test_suite_path).resolve()
        test_dir = test_path.parent
        runner_path = test_dir / f"_test_runner_{uuid.uuid4().hex[:8]}.py"

        try:
            with open(runner_path, 'w') as f:
                f.write(runner_script)

            env = os.environ.copy()
            env['PYTHONPATH'] = str(self.repo_path) + ':' + env.get('PYTHONPATH', '')

            result = subprocess.run(
                [sys.executable, str(runner_path)],
                capture_output=True, text=True,
                timeout=self.timeout,
                cwd=str(self.repo_path),
                env=env,
            )

            return self._parse_pytest_output(result.stdout + result.stderr)

        except subprocess.TimeoutExpired:
            return TestResult(
                passed=0, failed=1, total=1,
                errors=["Test execution timed out"], output="Timeout",
            )
        except Exception as e:
            import traceback
            return TestResult(
                passed=0, failed=1, total=1,
                errors=[f"Test execution error: {e}", traceback.format_exc()],
                output=str(e),
            )
        finally:
            try:
                if runner_path.exists():
                    os.unlink(runner_path)
            except Exception:
                pass

    def _create_runner_script(self, code: str) -> str:
        escaped_code = code.replace('\\', '\\\\').replace("'''", "\\'\\'\\'")
        abs_test_path = Path(self.test_suite_path).resolve()
        source_dir = str(Path(self.test_suite_path).resolve().parent.parent)

        return f'''#!/usr/bin/env python
import sys
sys.path.insert(0, "{self.repo_path}")
sys.path.append("{source_dir}")

import importlib
import pkgutil
import pandas as pd
import numpy as np
from datetime import datetime
from uuid import UUID
from typing import Optional, List, Dict, Any, Union, Tuple, Set, Callable, Literal
from collections import OrderedDict, defaultdict, namedtuple

module = importlib.import_module("{self.target_module}")
_module_stem = "{self.target_module}".rsplit(".", 1)[-1]
if _module_stem != "{self.target_module}":
    sys.modules[_module_stem] = module

generated_code = \'\'\'
{escaped_code}
\'\'\'

namespace = {{
    "pd": pd, "np": np, "datetime": datetime, "UUID": UUID,
    "Optional": Optional, "List": List, "Dict": Dict, "Any": Any,
    "Union": Union, "Tuple": Tuple, "Set": Set, "Callable": Callable,
    "Literal": Literal, "OrderedDict": OrderedDict,
    "defaultdict": defaultdict, "namedtuple": namedtuple,
}}

try:
    exec(generated_code, namespace)
except Exception as e:
    import traceback
    tb_lines = traceback.format_exc().strip().split('\\n')
    for tb_line in tb_lines[-4:]:
        print(f"E   {{tb_line}}")
    print("1 failed")
    sys.exit(1)

generated_items = {{}}
for name, obj in namespace.items():
    if isinstance(obj, type) or callable(obj):
        if not name.startswith('_') and name not in ['pd', 'np', 'datetime', 'UUID', 'Optional', 'List', 'Dict', 'Any', 'Union', 'Tuple', 'Set', 'Callable', 'Literal', 'OrderedDict', 'defaultdict', 'namedtuple']:
            generated_items[name] = obj

if not generated_items:
    print("No classes or functions found in generated code")
    sys.exit(1)

items_replaced = []
for name, obj in generated_items.items():
    if hasattr(module, name):
        setattr(module, name, obj)
        items_replaced.append(f"{{name}} (main)")

try:
    if hasattr(module, '__path__'):
        for importer, modname, ispkg in pkgutil.iter_modules(module.__path__, module.__name__ + '.'):
            try:
                submodule = importlib.import_module(modname)
                for name, obj in generated_items.items():
                    if hasattr(submodule, name):
                        setattr(submodule, name, obj)
                        items_replaced.append(f"{{name}} ({{modname}})")
            except ImportError as e:
                print(f"Warning: Could not import submodule {{modname}}: {{e}}")
except Exception as e:
    print(f"Warning: Error while patching submodules: {{e}}")

print(f"Replaced items: {{items_replaced}}")

import pytest
sys.exit(pytest.main([
    "{abs_test_path}",
    "-v", "--tb=short",
    "-p", "no:cacheprovider",
    "--timeout={self.per_test_timeout}",
    "--timeout_method=thread",
]))
'''

    def _parse_pytest_output(self, output: str) -> TestResult:
        passed = failed = 0

        if m := re.search(r'(\d+) passed', output):
            passed = int(m.group(1))
        if m := re.search(r'(\d+) failed', output):
            failed = int(m.group(1))
        if m := re.search(r'(\d+) error', output):
            failed += int(m.group(1))

        total = passed + failed
        lines = output.split('\n')
        seen_errors: set = set()
        exception_errors: list[str] = []
        failed_tests: list[str] = []
        current_test: Optional[str] = None

        for line in lines:
            stripped = line.strip()

            if 'FAILED' in line and '::' in line:
                failed_tests.append(stripped)
                if m := re.search(r'::(test_\w+)', line):
                    current_test = m.group(1)
            elif '_____' in line and 'test_' in line:
                if m := re.search(r'(test_\w+)', line):
                    current_test = m.group(1)
            elif line.startswith('E   ') or line.startswith('E\t'):
                error_msg = line[1:].strip()
                if error_msg and error_msg not in seen_errors:
                    if 'Target:' not in error_msg and current_test:
                        entity = self._test_name_to_entity(current_test)
                        if entity:
                            error_msg = f"Target: {entity} | {error_msg}"
                    exception_errors.append(error_msg)
                    seen_errors.add(error_msg)
            elif any(t in stripped for t in [
                'AttributeError:', 'TypeError:', 'ValueError:', 'KeyError:',
                'IndexError:', 'NameError:', 'AssertionError:', 'RuntimeError:',
                'ImportError:', 'SyntaxError:', 'ModuleNotFoundError:',
                'Failed: Timeout >',
            ]):
                if stripped not in seen_errors:
                    if 'Target:' not in stripped and current_test:
                        entity = self._test_name_to_entity(current_test)
                        if entity:
                            stripped = f"Target: {entity} | {stripped}"
                    exception_errors.append(stripped)
                    seen_errors.add(stripped)

        errors = exception_errors

        if not errors and failed_tests:
            errors.append(f"{len(failed_tests)} tests failed (check test output for details)")

        if total == 0 and output.strip():
            if 'SyntaxError' in output or 'ImportError' in output:
                errors.append("Code has syntax or import errors")
                failed = total = 1
            elif 'collected 0 items' in output:
                errors.append("No tests collected")
                failed = total = 1

        failed_per_entity: dict[str, int] = {}
        tested_entities: set[str] = set()
        for line in lines:
            if m := re.search(r'::(test_\w+)\s+(PASSED|FAILED|ERROR)', line):
                test_name, status = m.group(1), m.group(2)
                entity = self._test_name_to_entity(test_name)
                if entity:
                    tested_entities.add(entity)
                    if status in ('FAILED', 'ERROR'):
                        failed_per_entity[entity] = failed_per_entity.get(entity, 0) + 1

        return TestResult(
            passed=passed, failed=failed, total=total,
            errors=errors, output=output,
            failed_per_entity=failed_per_entity,
            tested_entities=tested_entities,
        )

    def _test_name_to_entity(self, test_name: str) -> Optional[str]:
        if self._entity_map and test_name in self._entity_map:
            return self._entity_map[test_name]

        if not test_name.startswith("test_"):
            return None
        parts = test_name[5:].split('_')
        if not parts:
            return None

        class_name = None
        method_start_idx = None
        for i, part in enumerate(parts):
            if part and part[0].isupper():
                class_name = part
                method_start_idx = i + 1
                break

        if class_name and method_start_idx is not None:
            method_parts = parts[method_start_idx:]
            if method_parts and method_parts[-1].isdigit():
                method_parts = method_parts[:-1]
            if not method_parts:
                return class_name
            return f"{class_name}.{'_'.join(method_parts)}"

        if parts:
            if parts[-1].isdigit():
                parts = parts[:-1]
            if parts:
                return '_'.join(parts)
        return None
