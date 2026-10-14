"""
W-width expansion for the search tree.
Generates up to W candidate refinements for a (node, entity) pair and evaluates each.
"""

from __future__ import annotations

import json
import logging
import random
from itertools import combinations
from math import comb
from pathlib import Path
from typing import Callable, Optional, TYPE_CHECKING

from ..utils.text_utils import (
    extract_entity_code,
    extract_entity_doc,
    replace_entity_doc,
    filter_errors_for_entity,
    get_all_errors_info,
)

if TYPE_CHECKING:
    from .node import Node
    from ..eval.codegen import EntityDocGenerator

logger = logging.getLogger(__name__)


def _sample_error_batches(errors: list, w: int) -> list[list]:
    """Sample W diversified error batches."""
    if not errors:
        return [[] for _ in range(w)]

    n = len(errors)
    target = (n + 1) // 2  # ceil(n / 2)

    if comb(n, target) < w:
        all_subsets = [
            s
            for size in range(1, n + 1)
            for s in combinations(range(n), size)
        ]
        random.shuffle(all_subsets)
        chosen = all_subsets[:w]
        while len(chosen) < w and all_subsets:
            chosen.append(random.choice(all_subsets))
        return [[errors[i] for i in idxs] for idxs in chosen]

    seen: set[tuple[int, ...]] = set()
    batches: list[list] = []
    while len(batches) < w:
        idxs = tuple(sorted(random.sample(range(n), target)))
        if idxs in seen:
            continue
        seen.add(idxs)
        batches.append([errors[i] for i in idxs])
    return batches


class Expansion:
    """Generate W diversified children for a (node, entity) pair."""

    def __init__(
        self,
        entity_doc_generator: "EntityDocGenerator",
        evaluate_fn: Callable[["Node"], None],
        source_code: str,
        hint_temperature: float,
        save_node_fn: Callable[["Node"], None] | None = None,
        bandit_get_scores_fn: Callable[["Node"], dict] | None = None,
    ):
        self.entity_doc_generator = entity_doc_generator
        self.evaluate_fn = evaluate_fn
        self.source_code = source_code
        self.hint_temperature = hint_temperature
        self.save_node_fn = save_node_fn
        self.bandit_get_scores_fn = bandit_get_scores_fn

    def expand(
        self,
        node: "Node",
        entity: str,
        width: int,
        budget_remaining: int,
        all_nodes: list["Node"],
        output_dir: Optional[Path] = None,
    ) -> list["Node"]:
        from .node import Node  # local import to avoid cycle at import time

        attempts = node.get_attempts_for_entity(entity)
        remaining_width = width - attempts
        if remaining_width <= 0:
            return []
        num_children = min(remaining_width, budget_remaining)
        if num_children <= 0:
            return []

        entity_errors = filter_errors_for_entity(node.errors, entity)
        entity_source = extract_entity_code(self.source_code, entity)
        entity_generated = extract_entity_code(node.generated_code or "", entity)
        original_entity_doc = extract_entity_doc(node.doc, entity)
        child_base_idx = len(node.children)
        parent_phi_e = node.entity_phi.get(entity, 0.0)

        error_batches = _sample_error_batches(entity_errors, num_children)

        children: list["Node"] = []
        all_hypotheses: list[str] = []

        for i in range(num_children):
            batch = error_batches[i]
            hypothesis = (
                "General documentation improvement — ensure all behaviors of the "
                "reference implementation are accurately documented."
            )
            hypothesis_prompt = ""
            hypothesis_response = ""
            if batch:
                hyps, hypothesis_prompt, hypothesis_response = (
                    self.entity_doc_generator.generate_hypotheses(
                        entity=entity,
                        entity_source_code=entity_source,
                        entity_generated_code=entity_generated,
                        original_entity_doc=original_entity_doc,
                        errors=batch,
                        num_hypotheses=1,
                    )
                )
                if hyps:
                    hypothesis = hyps[0]
                else:
                    logger.warning(f"Hypothesis parsing empty for {node.id}/{entity} iter {i}.")
            all_hypotheses.append(hypothesis)

            doc_result = self.entity_doc_generator.generate(
                entity=entity,
                entity_source_code=entity_source,
                original_entity_doc=original_entity_doc,
                errors=batch,
                hypothesis=hypothesis,
                temperature=self.hint_temperature,
            )

            child_doc = replace_entity_doc(node.doc, entity, doc_result.documentation)

            child = Node(
                id=f"{node.id}_{child_base_idx + i}",
                parent=node,
                entity=entity,
                hint=doc_result.documentation,
                hint_prompt=doc_result.prompt,
                focused_error=get_all_errors_info(batch),
                doc=child_doc,
                hypothesis=hypothesis,
                hypothesis_prompt=hypothesis_prompt,
                hypothesis_response=hypothesis_response,
            )

            self.evaluate_fn(child)
            node.children.append(child)
            all_nodes.append(child)
            children.append(child)

            child_phi_e = child.entity_phi.get(entity, 0.0)
            improved = child_phi_e > parent_phi_e

            mark = "OK" if improved else "x"
            print(
                f"    iter {i}: {child.id} phi={child.phi:.4f} "
                f"|S|={len(child.solved_set)} phi_e {parent_phi_e:.2f}->{child_phi_e:.2f} {mark}"
            )

            if child_phi_e >= 1.0:
                print(f"    entity {entity} solved, skipping remaining iterations")
                break

        if output_dir:
            parent_dir = output_dir / "nodes" / node.id
            parent_dir.mkdir(parents=True, exist_ok=True)
            artifact = {"entity": entity, "hypotheses": all_hypotheses}
            if self.bandit_get_scores_fn:
                bandit_scores = self.bandit_get_scores_fn(node)
                if bandit_scores:
                    for sc in bandit_scores.values():
                        if sc.get("score") == float('inf'):
                            sc["score"] = "inf"
                        if sc.get("exploration") == float('inf'):
                            sc["exploration"] = "inf"
                    artifact["bandit_scores"] = bandit_scores
                    artifact["selected_entity"] = entity
            with open(parent_dir / "hypotheses.json", "w") as f:
                json.dump(artifact, f, indent=2)

        if self.save_node_fn:
            for child in children:
                self.save_node_fn(child)

        return children
