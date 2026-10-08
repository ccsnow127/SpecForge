"""
Eligibility check for entity selection.
Filters entities at a node to those that are unsolved, not exhausted, have failing tests, and whose callees are already resolved.
"""

from __future__ import annotations

from typing import Callable, TYPE_CHECKING

if TYPE_CHECKING:
    from .node import Node
    from ..utils.call_graph import CallGraph


def get_eligible_entities(
    node: "Node",
    entities: list[str],
    width: int,
    call_graph: "CallGraph",
    entity_error_filter: Callable[[list[str], str], list[str]],
) -> list[str]:
    exhausted_at_node = {
        e for e in entities if node.get_attempts_for_entity(e) >= width
    }
    resolved_for_topo = node.solved_set | exhausted_at_node
    entities_with_errors = {
        e for e in entities if entity_error_filter(node.errors, e)
    }

    eligible: list[str] = []
    for entity in entities:
        if entity in node.solved_set:
            continue
        if entity in exhausted_at_node:
            continue
        if entity not in entities_with_errors:
            continue
        if call_graph.descendants(entity) <= resolved_for_topo:
            eligible.append(entity)
    return eligible
