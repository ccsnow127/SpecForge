import sys
import os

# Add dataset/python/ to sys.path so "import abrvalg" resolves correctly
dataset_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if dataset_root not in sys.path:
    sys.path.insert(0, dataset_root)

# Alias so "from interpreter import ..." works (same as docsearch test runner)
import abrvalg.interpreter
sys.modules['interpreter'] = abrvalg.interpreter
