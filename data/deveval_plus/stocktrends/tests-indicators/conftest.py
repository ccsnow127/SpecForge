import sys
import os

# Add deveval_plus/ to sys.path so `import stocktrends` resolves correctly.
dataset_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if dataset_root not in sys.path:
    sys.path.insert(0, dataset_root)

import stocktrends.indicators
sys.modules['indicators'] = stocktrends.indicators
