import sys
import os

# Add dataset/python/ to sys.path so "import arxiv_digest" resolves correctly
dataset_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if dataset_root not in sys.path:
    sys.path.insert(0, dataset_root)

import arxiv_digest.query_arxiv
sys.modules['query_arxiv'] = arxiv_digest.query_arxiv
