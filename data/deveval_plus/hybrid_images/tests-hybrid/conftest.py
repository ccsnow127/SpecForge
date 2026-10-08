import sys
import os

# Add dataset/python/ to sys.path so "import hybrid_images" resolves correctly
dataset_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if dataset_root not in sys.path:
    sys.path.insert(0, dataset_root)

import hybrid_images.hybrid
sys.modules['hybrid'] = hybrid_images.hybrid
