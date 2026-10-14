import sys
import os

# Add dataset/python/ to sys.path so "import readtime_result" resolves correctly
dataset_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if dataset_root not in sys.path:
    sys.path.insert(0, dataset_root)

import readtime_result.result
sys.modules['result'] = readtime_result.result
