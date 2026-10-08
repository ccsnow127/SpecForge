"""
SpecForge package.
Agent-oriented code documentation optimization via multi-frontier tree search.
"""

from .search import SpecForge
from .node import Node
from .pareto_archive import ParetoArchive
from .bandit import DiscountedUCB
from .eligibility import get_eligible_entities
from .expansion import Expansion

__all__ = [
    "SpecForge",
    "Node",
    "ParetoArchive",
    "DiscountedUCB",
    "get_eligible_entities",
    "Expansion",
]
