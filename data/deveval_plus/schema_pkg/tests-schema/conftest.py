import os
import sys

_here = os.path.dirname(os.path.abspath(__file__))
_schema_dir = os.path.dirname(_here)
_dataset_root = os.path.dirname(_schema_dir)

if _dataset_root not in sys.path:
    sys.path.insert(0, _dataset_root)

# test_validate_file / test_complex do `Schema(Use(open)).validate("LICENSE-MIT")`
# with a relative path, so the CWD must contain LICENSE-MIT.
os.chdir(_schema_dir)

import schema_pkg.schema  # noqa: E402
# The bundled test file does `from schema import Schema, ...` — alias so it resolves.
sys.modules['schema'] = schema_pkg.schema
