import sys
import os

# Add dataset/python/ to sys.path so "import hone_pkg" resolves correctly
dataset_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if dataset_root not in sys.path:
    sys.path.insert(0, dataset_root)

import hone_pkg.hone
import hone_pkg.utils
import hone_pkg.utils.csv_utils
sys.modules['hone'] = hone_pkg.hone
sys.modules['hone.utils'] = hone_pkg.utils
sys.modules['hone.utils.csv_utils'] = hone_pkg.utils.csv_utils
