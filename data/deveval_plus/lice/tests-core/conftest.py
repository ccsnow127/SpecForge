import os
import sys

# lice/core.py does `os.listdir('templates')` at module-import time with a
# relative path, so the process CWD must be the `lice/` dir before the test
# module imports it.
_here = os.path.dirname(os.path.abspath(__file__))
_lice_dir = os.path.dirname(_here)
_dataset_root = os.path.dirname(_lice_dir)

if _dataset_root not in sys.path:
    sys.path.insert(0, _dataset_root)

os.chdir(_lice_dir)

import lice.core  # noqa: E402
sys.modules['core'] = lice.core
