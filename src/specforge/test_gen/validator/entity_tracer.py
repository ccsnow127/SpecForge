"""
Entity tracer for test-to-entity attribution.
Instruments source entities to record which ones each test calls at runtime.
"""

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Optional

from ..models import FunctionInfo


class EntityTracer:
    """Traces which source entities a test calls at runtime.

    Works by generating a conftest.py that wraps every known entity
    (class method or standalone function) with call-tracking, then
    running pytest and reading back the recorded call set.
    """

    def __init__(self, entities: list[FunctionInfo], language: str = "python"):
        self.entities = entities
        self.language = language

    def generate_conftest(self, source_module: str) -> str:
        """Generate conftest.py content with monkey-patch tracing.

        Args:
            source_module: The module name to import (stem of source file).

        Returns:
            conftest.py content as a string.
        """
        patches = []
        for entity in self.entities:
            if entity.class_name:
                entity_name = f"{entity.class_name}.{entity.name}"
                patch = (
                    f"try:\n"
                    f"    {source_module}.{entity.class_name}.{entity.name} = "
                    f'_wrap("{entity_name}", '
                    f"{source_module}.{entity.class_name}.{entity.name})\n"
                    f"except (AttributeError, TypeError):\n"
                    f"    pass"
                )
            else:
                entity_name = entity.name
                patch = (
                    f"try:\n"
                    f"    {source_module}.{entity.name} = "
                    f'_wrap("{entity_name}", {source_module}.{entity.name})\n'
                    f"except (AttributeError, TypeError):\n"
                    f"    pass"
                )
            patches.append(patch)

        patches_code = "\n".join(patches)

        return (
            "import functools\n"
            "import json\n"
            "import os\n"
            "\n"
            "_entity_calls = set()\n"
            '_RESULT_FILE = os.environ.get("_ENTITY_TRACE_RESULT", "")\n'
            "\n"
            "\n"
            "def _wrap(entity_name, original):\n"
            "    @functools.wraps(original)\n"
            "    def wrapper(*args, **kwargs):\n"
            "        _entity_calls.add(entity_name)\n"
            "        return original(*args, **kwargs)\n"
            "    return wrapper\n"
            "\n"
            "\n"
            f"import {source_module}\n"
            "\n"
            f"{patches_code}\n"
            "\n"
            "\n"
            "def pytest_sessionfinish(session, exitstatus):\n"
            "    if _RESULT_FILE:\n"
            '        with open(_RESULT_FILE, "w") as f:\n'
            "            json.dump(sorted(_entity_calls), f)\n"
        )

    def trace_test(
        self,
        test_content: str,
        source_file: Path,
        timeout: int = 30,
    ) -> list[str]:
        """Run a single test with tracing and return called entity names.

        Args:
            test_content: Complete test file content.
            source_file: Path to the source file being tested.
            timeout: Timeout in seconds.

        Returns:
            List of entity names called during the test
            (e.g. ["Renko.get_ohlc_data"]).
        """
        if self.language != "python":
            return []

        source_module = source_file.stem
        conftest_content = self.generate_conftest(source_module)

        with tempfile.TemporaryDirectory() as tmpdir_str:
            tmpdir = Path(tmpdir_str)

            # Write test file
            test_file = tmpdir / "test_trace.py"
            test_file.write_text(test_content)

            # Write conftest.py (loaded by pytest before test collection)
            conftest_file = tmpdir / "conftest.py"
            conftest_file.write_text(conftest_content)

            # Result file path
            result_file = tmpdir / "_entity_trace_result.json"

            env = {
                **os.environ,
                "PYTHONPATH": str(source_file.parent),
                "_ENTITY_TRACE_RESULT": str(result_file),
            }

            try:
                subprocess.run(
                    [
                        sys.executable, "-m", "pytest",
                        str(test_file),
                        "-x", "--tb=no", "-q",
                        "-p", "no:cacheprovider",
                    ],
                    capture_output=True,
                    text=True,
                    timeout=timeout,
                    cwd=str(source_file.parent),
                    env=env,
                )
            except (subprocess.TimeoutExpired, Exception):
                return []

            # Read results
            if result_file.exists():
                try:
                    return json.loads(result_file.read_text())
                except (json.JSONDecodeError, OSError):
                    return []

            return []

    @staticmethod
    def pick_primary_entity(called_entities: list[str]) -> Optional[str]:
        """Pick the primary entity from a list of called entities.

        Filters out __init__ and returns the first remaining entity.
        Falls back to the first entity if all are __init__.

        Args:
            called_entities: List of entity names.

        Returns:
            The primary entity name, or None if the list is empty.
        """
        if not called_entities:
            return None
        candidates = [e for e in called_entities if not e.endswith(".__init__")]
        if candidates:
            return candidates[0]
        return called_entities[0]
