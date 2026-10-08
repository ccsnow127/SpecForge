import os
import sys

_here = os.path.dirname(os.path.abspath(__file__))
_dataset_root = os.path.dirname(os.path.dirname(_here))
if _dataset_root not in sys.path:
    sys.path.insert(0, _dataset_root)

import schedule_pkg.schedule  # noqa: E402
sys.modules['schedule'] = schedule_pkg.schedule
