"""Pareto archive for node selection.
"""

from __future__ import annotations

import random
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from .node import Node


class ParetoArchive:
    def __init__(self, entities: list[str]):
        self.entities = entities
        self.archive: list["Node"] = []

    def _dominates(self, a: "Node", b: "Node") -> bool:
        sa, pa = len(a.solved_set), a.phi
        sb, pb = len(b.solved_set), b.phi
        return sa >= sb and pa >= pb and (sa > sb or pa > pb)

    def add(self, node: "Node") -> bool:
        if node.phi is None:
            return False

        for arch_node in self.archive:
            if self._dominates(arch_node, node):
                return False

        self.archive = [n for n in self.archive if not self._dominates(node, n)]
        if node not in self.archive:
            self.archive.append(node)
        return True

    def remove_exhausted(self, node: "Node"):
        self.archive = [n for n in self.archive if n.id != node.id]

    def pareto_sample(self) -> "Node":
        node, _ = self.pareto_sample_with_scores()
        return node

    def pareto_sample_with_scores(self) -> tuple["Node", list[dict]]:
        if not self.archive:
            raise ValueError("Archive is empty, cannot sample")

        if len(self.archive) == 1:
            scores = [{
                "node_id": self.archive[0].id,
                "phi": round(self.archive[0].phi, 4) if self.archive[0].phi is not None else 0.0,
                "solved": len(self.archive[0].solved_set),
                "weight": float(len(self.entities)),
                "prob": 1.0,
            }]
            return self.archive[0], scores

        per_entity_best: dict[str, list["Node"]] = {}
        for entity in self.entities:
            best_phi_e = -1.0
            best_nodes: list["Node"] = []
            for node in self.archive:
                phi_e = node.entity_phi.get(entity, 0.0)
                if phi_e > best_phi_e:
                    best_phi_e = phi_e
                    best_nodes = [node]
                elif phi_e == best_phi_e:
                    best_nodes.append(node)
            per_entity_best[entity] = best_nodes

        union_ids: set[str] = set()
        union_nodes: list["Node"] = []
        for nodes in per_entity_best.values():
            for node in nodes:
                if node.id not in union_ids:
                    union_ids.add(node.id)
                    union_nodes.append(node)

        front: list["Node"] = []
        for candidate in union_nodes:
            dominated = False
            for other in union_nodes:
                if other.id == candidate.id:
                    continue
                if self._dominates(other, candidate):
                    dominated = True
                    break
            if not dominated:
                front.append(candidate)

        if not front:
            front = list(self.archive)

        front_ids = {node.id for node in front}
        weight: dict[str, float] = {node.id: 0.0 for node in front}
        for nodes in per_entity_best.values():
            share = 1.0 / len(nodes)
            for node in nodes:
                if node.id in front_ids:
                    weight[node.id] += share

        weights = [weight[node.id] for node in front]
        total = sum(weights)
        probabilities = [1.0 / len(front)] * len(front) if total == 0 else [w / total for w in weights]

        scores = []
        for i, node in enumerate(front):
            scores.append({
                "node_id": node.id,
                "phi": round(node.phi, 4) if node.phi is not None else 0.0,
                "solved": len(node.solved_set),
                "weight": round(weight[node.id], 4),
                "prob": round(probabilities[i], 3),
            })

        r = random.random()
        cumulative = 0.0
        for i, p in enumerate(probabilities):
            cumulative += p
            if r <= cumulative:
                return front[i], scores

        return front[-1], scores

    @property
    def best_node(self) -> "Node":
        if not self.archive:
            raise ValueError("Archive is empty")
        return max(self.archive, key=lambda n: (len(n.solved_set), n.phi))
