"""
Evaluation metrics for search nodes.
Exposes solve-set size and module-level pass rate, plus per-entity phi computation.
"""

from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ..specforge.node import Node


def solve_set_size(node: "Node") -> int:
    return len(node.solved_set)


def module_pass_rate(node: "Node") -> float:
    return node.phi if node.phi is not None else 0.0


def compute_entity_phi(
    node: "Node",
    entities: list[str],
    tests_per_entity: dict[str, int],
    entity_error_filter,
) -> dict[str, float]:
    """Per-entity pass rate phi_e = (tests_passing_for_e / tests_total_for_e)."""
    if getattr(node, 'compile_error', False):
        return {e: 0.0 for e in entities}
    if node.phi is not None and node.phi == 0.0:
        return {e: 0.0 for e in entities}

    failed_per_entity = getattr(node, 'failed_per_entity', {}) or {}
    entity_phi: dict[str, float] = {}

    for entity in entities:
        total = tests_per_entity.get(entity, 0)
        if total == 0:
            entity_phi[entity] = 0.0
            continue
        if failed_per_entity:
            failed = min(failed_per_entity.get(entity, 0), total)
        else:
            failed = min(len(entity_error_filter(node.errors or [], entity)), total)
        entity_phi[entity] = (total - failed) / total

    if node.phi is not None and node.phi < 0.5:
        total_attributed = (
            sum(failed_per_entity.get(e, 0) for e in entities)
            if failed_per_entity
            else sum(len(entity_error_filter(node.errors or [], e)) for e in entities)
        )
        if total_attributed == 0:
            entity_phi = {e: 0.0 for e in entities}

    return entity_phi


def compute_solved_set(entity_phi: dict[str, float]) -> set[str]:
    return {e for e, phi in entity_phi.items() if phi == 1.0}
