import os
import sys

# Add deveval_plus/ so `from geotext.geotext import GeoText` resolves correctly.
_here = os.path.dirname(os.path.abspath(__file__))
_dataset_root = os.path.dirname(os.path.dirname(_here))

if _dataset_root not in sys.path:
    sys.path.insert(0, _dataset_root)

import geotext.geotext  # noqa: E402
sys.modules['geotext_mod'] = geotext.geotext
