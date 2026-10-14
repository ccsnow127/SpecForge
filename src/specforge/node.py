"""
Node dataclass for the SpecForge search tree.
Holds documentation, generated code, test results, and tree structure for each search state.
"""

from __future__ import annotations
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class Node:
    id: str
    parent: Optional["Node"]

    entity: Optional[str] = None
    hint: Optional[str] = None
    hint_prompt: Optional[str] = None
    focused_error: Optional[dict] = None
    hypothesis: Optional[str] = None
    hypothesis_prompt: Optional[str] = None
    hypothesis_response: Optional[str] = None

    children: list["Node"] = field(default_factory=list)

    doc: Optional[str] = None
    doc_prompt: Optional[str] = None
    generated_code: Optional[str] = None
    generation_prompt: Optional[dict] = None

    phi: Optional[float] = None
    errors: list[str] = field(default_factory=list)
    entity_phi: dict[str, float] = field(default_factory=dict)
    solved_set: set[str] = field(default_factory=set)
    intractable_set: set[str] = field(default_factory=set)

    def accumulated_hints(self) -> dict[str, str]:
        hints: dict[str, str] = {}
        node: Optional[Node] = self
        while node is not None:
            if node.entity and node.hint and node.entity not in hints:
                hints[node.entity] = node.hint
            node = node.parent
        return hints

    def get_attempts_for_entity(self, entity: str) -> int:
        return sum(1 for c in self.children if c.entity == entity)

    def is_leaf(self) -> bool:
        return len(self.children) == 0

    def get_phi_vector(self, entities: list[str]) -> list[float]:
        return [self.entity_phi.get(e, 0.0) for e in entities]

    def path_from_root(self) -> list["Node"]:
        path = []
        node: Optional[Node] = self
        while node is not None:
            path.append(node)
            node = node.parent
        return list(reversed(path))

    def depth(self) -> int:
        d = 0
        node = self.parent
        while node is not None:
            d += 1
            node = node.parent
        return d

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "entity": self.entity,
            "hint": self.hint,
            "phi": self.phi,
            "errors": self.errors[:5] if self.errors else [],
            "num_children": len(self.children),
            "depth": self.depth(),
            "hypothesis": self.hypothesis,
            "solved_set": list(self.solved_set),
            "intractable_set": list(self.intractable_set),
            "entity_phi": self.entity_phi,
        }

    def __repr__(self) -> str:
        hint_preview = self.hint[:30] + "..." if self.hint and len(self.hint) > 30 else self.hint
        return f"Node(id={self.id}, entity={self.entity}, hint={hint_preview}, phi={self.phi}, |S|={len(self.solved_set)})"
