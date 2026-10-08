import os
import sys

# Add deveval_plus/ so `from croniter import ...` resolves.
_here = os.path.dirname(os.path.abspath(__file__))
_dataset_root = os.path.dirname(os.path.dirname(_here))

if _dataset_root not in sys.path:
    sys.path.insert(0, _dataset_root)

# The bundled test file imports `from croniter.tests import base`. Alias the
# tests-croniter/ dir (where base.py lives) as `croniter.tests`.
import croniter as _cron_pkg
import types

_tests_pkg = types.ModuleType("croniter.tests")
_tests_pkg.__path__ = [_here]
sys.modules["croniter.tests"] = _tests_pkg
_cron_pkg.tests = _tests_pkg

import croniter.croniter  # noqa: E402 — make croniter.croniter.VALID_LEN_EXPRESSION importable
