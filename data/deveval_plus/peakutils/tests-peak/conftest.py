import os
import sys

_here = os.path.dirname(os.path.abspath(__file__))
_pkg_dir = os.path.dirname(_here)
_dataset_root = os.path.dirname(_pkg_dir)
if _dataset_root not in sys.path:
    sys.path.insert(0, _dataset_root)

# Tests load fixtures from baseline/, exp/, noise/ subdirs by relative path.
os.chdir(_here)

import peakutils.peak  # noqa: E402
sys.modules['peak'] = peakutils.peak
