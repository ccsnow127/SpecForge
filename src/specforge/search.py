"""SpecForge search orchestrator.
"""

from __future__ import annotations

import json
import logging
import random
from pathlib import Path
from typing import Optional

from .node import Node
from .pareto_archive import ParetoArchive
from .bandit import DiscountedUCB
from .eligibility import get_eligible_entities
from .expansion import Expansion

from ..eval.codegen import (
    BaselineDocGenerator,
    CodeGenerator,
    EntityDocGenerator,
)
from ..eval.metrics import compute_entity_phi, compute_solved_set
from ..utils.call_graph import CallGraph
from ..utils.text_utils import (
    count_errors_by_severity,
    extract_entity_code,
    extract_imports,
    filter_errors_for_entity,
    get_entities_with_errors,
    compute_tests_per_entity,
)

logger = logging.getLogger(__name__)


class SpecForge:
    """Multi-frontier tree search for agent-oriented code documentation optimization."""

    def __init__(
        self,
        source_code: str,
        entities: list[str],
        test_executor,
        hint_llm,
        code_llm,
        budget: int = 50,
        width: int = 3,
        output_dir: Optional[Path] = None,
        repo_name: str = "",
        use_pareto_archive: bool = True,
        use_bandit: bool = True,
        ablation_mode: Optional[str] = None,
    ):
        self.source_code = source_code
        self.imports = extract_imports(source_code)
        self.entities = entities
        self.test_executor = test_executor
        self.budget = budget
        self.width = width
        self.output_dir = output_dir
        self.repo_name = repo_name
        self.use_pareto_archive = use_pareto_archive
        self.use_bandit = use_bandit
        self.ablation_mode = ablation_mode

        if self.ablation_mode == "iterative":
            self.width = 1

        self.hint_llm = hint_llm
        self.entity_doc_generator = EntityDocGenerator(hint_llm)
        self.code_generator = CodeGenerator(code_llm)
        self.baseline_generator = BaselineDocGenerator(hint_llm)

        self.tests_per_entity = compute_tests_per_entity(
            test_executor._entity_map, entities,
        )

        self.root: Optional[Node] = None
        self.all_nodes: list[Node] = []
        self.total_tests: int = 0

        self.call_graph = CallGraph(source_code, entities, extract_entity_code)
        self.entity_bandit = DiscountedUCB(entities=entities, gamma=0.95)

        self.expansion = Expansion(
            entity_doc_generator=self.entity_doc_generator,
            evaluate_fn=self._evaluate_node,
            source_code=self.source_code,
            hint_temperature=hint_llm.default_temperature,
            save_node_fn=self._save_node if output_dir else None,
            bandit_get_scores_fn=(
                lambda n: self.entity_bandit.get_scores(
                    self._eligible(n), n.entity_phi,
                ) if self.use_bandit else None
            ),
        )

        if self.output_dir:
            self.nodes_dir = self.output_dir / "nodes"
            self.nodes_dir.mkdir(parents=True, exist_ok=True)

    def _eligible(self, node: Node) -> list[str]:
        return get_eligible_entities(
            node, self.entities, self.width, self.call_graph, filter_errors_for_entity,
        )

    def _select_entity_for_node(self, node: Node) -> Optional[str]:
        eligible = self._eligible(node)
        if not eligible:
            return None
        if not self.use_bandit:
            return random.choice(eligible)
        return self.entity_bandit.select(eligible, node.entity_phi)

    def _greedy_select_node(self, archive_nodes: list[Node]) -> Node:
        candidates = [n for n in archive_nodes if n.phi is not None]
        candidates.sort(key=lambda n: (len(n.solved_set), n.phi), reverse=True)
        for n in candidates:
            if self._eligible(n):
                return n
        return candidates[0] if candidates else self.root

    def search(self) -> tuple[list[Node], float]:
        if self.ablation_mode in ("iterative", "iterative+topo"):
            return self._search_iterative()

        self._save_call_graph()

        archive = ParetoArchive(entities=self.entities)
        self._initialize_root(archive)

        while self._get_node_count() < self.budget:
            if not archive.archive:
                print("Archive empty — stopping.")
                break

            best = archive.best_node
            if len(best.solved_set) >= len(self.entities):
                print(f"All entities solved: |S|={len(best.solved_set)}")
                break

            if self.use_pareto_archive:
                v, _ = archive.pareto_sample_with_scores()
            else:
                v = self._greedy_select_node(archive.archive)

            e = self._select_entity_for_node(v)
            if e is None:
                archive.remove_exhausted(v)
                continue

            children = self.expansion.expand(
                v, e, self.width,
                self.budget - self._get_node_count(),
                self.all_nodes, self.output_dir,
            )
            if not children:
                continue

            for c in children:
                archive.add(c)

            if self.use_bandit:
                parent_phi_e = v.entity_phi.get(e, 0.0)
                max_child_phi = max(c.entity_phi.get(e, 0.0) for c in children)
                raw_reward = max(0.0, max_child_phi - parent_phi_e)
                gap = 1.0 - parent_phi_e
                self.entity_bandit.update(e, raw_reward, gap)

            self._save_summary(self._get_node_count())
            self._save_tree_visualization()

        self._save_summary(len(self.all_nodes) - 1)

        if archive.archive:
            best = archive.best_node
        else:
            evaluated = [n for n in self.all_nodes if n.phi is not None]
            best = max(evaluated, key=lambda n: (len(n.solved_set), n.phi))
        return best.path_from_root(), best.phi

    def _search_iterative(self) -> tuple[list[Node], float]:
        """Iterative ablation: flat loop, no tree / archive / bandit."""
        self._save_call_graph()

        doc, doc_prompt = self.baseline_generator.generate(self.source_code, temperature=0.0)
        current = Node(id="root_0", parent=None, doc=doc, doc_prompt=doc_prompt)
        self._evaluate_node(current)
        self.all_nodes.append(current)
        self.root = current
        self._save_node(current)
        print(f"  baseline: phi={current.phi:.4f}, |S|={len(current.solved_set)}")

        best = current
        while self._get_node_count() < self.budget:
            if current.phi >= 1.0:
                break

            entity = self._select_entity_iterative(current)
            if entity is None:
                break

            children = self.expansion.expand(
                current, entity, self.width,
                self.budget - self._get_node_count(),
                self.all_nodes, self.output_dir,
            )
            if not children:
                continue

            child = max(children, key=lambda c: c.phi)
            for c in children:
                self._save_node(c)

            if child.phi >= current.phi:
                current = child
            if child.phi > best.phi:
                best = child

            self._save_summary(self._get_node_count())

        return best.path_from_root(), best.phi

    def _select_entity_iterative(self, node: Node) -> Optional[str]:
        unsolved = [e for e in self.entities if e not in node.solved_set]
        if not unsolved:
            return None

        entities_with_errors = set(get_entities_with_errors(node.errors, self.entities))
        candidates = [e for e in unsolved if e in entities_with_errors]
        if not candidates:
            if getattr(node, 'compile_error', False) or node.phi == 0.0:
                candidates = unsolved
            else:
                return None

        if self.ablation_mode == "iterative+topo":
            topo = [e for e in candidates if self.call_graph.descendants(e) <= node.solved_set]
            if topo:
                candidates = topo

        return random.choice(candidates)

    def _initialize_root(self, archive: ParetoArchive):
        """Algorithm 1 line 8: A ← {v_0}."""
        doc, doc_prompt = self.baseline_generator.generate(self.source_code, temperature=0.0)
        root = Node(id="root_0", parent=None, doc=doc, doc_prompt=doc_prompt)
        self._evaluate_node(root)

        self.all_nodes.append(root)
        self.root = root
        self._save_node(root)
        archive.add(root)
        print(f"Baseline: phi={root.phi:.4f}, |S|={len(root.solved_set)}")
        self._save_tree_visualization()

    def _evaluate_node(self, node: Node):
        accumulated = node.accumulated_hints()
        hints_list = [{"entity": entity, "hint": hint} for entity, hint in accumulated.items()]

        is_valid, format_error = self.code_generator.validate_doc_format(node.doc)
        if not is_valid:
            self._set_failure_state(node, [format_error], compile_error=False)
            return

        try:
            code, prompt = self.code_generator.generate(
                node.doc, hints=hints_list, imports=self.imports,
            )
        except RuntimeError as e:
            self._set_failure_state(node, [f"Code generation failed: {e}"], compile_error=True)
            return

        node.generated_code = code
        node.generation_prompt = prompt

        result = self.test_executor.run(code, len(self.all_nodes))

        node.phi = result.pass_rate
        node.errors = result.errors
        node.compile_error = getattr(result, 'compile_error', None) is not None
        node.failed_per_entity = getattr(result, 'failed_per_entity', {})

        if self.total_tests == 0:
            self.total_tests = result.total

        node.entity_phi = compute_entity_phi(
            node, self.entities, self.tests_per_entity, filter_errors_for_entity,
        )
        node.solved_set = compute_solved_set(node.entity_phi)

        if node.parent is None:
            node.intractable_set = set()

    def _set_failure_state(self, node: Node, errors: list[str], compile_error: bool):
        node.generated_code = ""
        node.generation_prompt = {}
        node.phi = 0.0
        node.errors = errors
        node.compile_error = compile_error
        node.failed_per_entity = {}
        node.entity_phi = {e: 0.0 for e in self.entities}
        node.solved_set = set()
        if node.parent is None:
            node.intractable_set = set()

    def _best_phi(self) -> float:
        return max(n.phi for n in self.all_nodes if n.phi is not None)

    def _get_node_count(self) -> int:
        return max(0, len(self.all_nodes) - 1)

    def _get_best_node(self) -> Node:
        return max(
            (n for n in self.all_nodes if n.phi is not None),
            key=lambda n: n.phi, default=self.root,
        )

    def _save_call_graph(self):
        if not self.output_dir:
            return
        graph = self.call_graph.graph()
        with open(self.output_dir / "call_graph.json", "w") as f:
            json.dump(graph, f, indent=2)

    def _save_node(self, node: Node):
        if not self.output_dir:
            return
        node_dir = self.nodes_dir / node.id
        node_dir.mkdir(parents=True, exist_ok=True)

        if node.doc:
            (node_dir / "doc.md").write_text(node.doc)
        if node.doc_prompt:
            (node_dir / "doc_prompt.txt").write_text(node.doc_prompt)
        if node.generated_code:
            (node_dir / "code.py").write_text(node.generated_code)
        if node.generation_prompt:
            (node_dir / "code_prompt.json").write_text(
                json.dumps(node.generation_prompt, indent=2),
            )
        if node.hint:
            (node_dir / "hint.txt").write_text(node.hint)
        if node.errors:
            (node_dir / "errors.txt").write_text("\n".join(node.errors))

        crash_count, logic_count = (
            count_errors_by_severity(node.errors) if node.errors else (0, 0)
        )
        info = {
            "id": node.id,
            "entity": node.entity,
            "refinement": node.hint,
            "phi": node.phi,
            "parent_phi": node.parent.phi if node.parent else None,
            "crash_count": crash_count,
            "logic_count": logic_count,
            "depth": node.depth(),
            "parent_id": node.parent.id if node.parent else None,
            "children_ids": [c.id for c in node.children],
            "num_errors": len(node.errors) if node.errors else 0,
            "entity_phi": node.entity_phi,
            "solved_set": list(node.solved_set),
            "intractable_set": list(node.intractable_set),
            "solved_count": len(node.solved_set),
            "intractable_count": len(node.intractable_set),
            "hypothesis": node.hypothesis,
        }
        with open(node_dir / "info.json", "w") as f:
            json.dump(info, f, indent=2)

    def _save_tree_visualization(self):
        if not self.output_dir:
            return

        lines = ["Tree visualization", "=" * 60]
        children_map: dict[str, list[Node]] = {n.id: [] for n in self.all_nodes}
        for node in self.all_nodes:
            if node.parent and node.parent.id in children_map:
                children_map[node.parent.id].append(node)
        for nid in children_map:
            children_map[nid].sort(key=lambda n: (len(n.id), n.id))

        def print_node(node: Node, prefix: str = "", is_last: bool = True):
            phi = node.phi or 0
            solved = len(node.solved_set)
            connector = "+-- " if is_last else "|-- "
            if node.parent is None:
                lines.append(f"{node.id} [phi={phi:.4f}, |S|={solved}]")
            else:
                lines.append(
                    f"{prefix}{connector}{node.id} [phi={phi:.4f}, |S|={solved}, e={node.entity}]"
                )
            child_prefix = prefix + ("    " if is_last else "|   ")
            child_list = children_map.get(node.id, [])
            for i, child in enumerate(child_list):
                print_node(child, child_prefix, i == len(child_list) - 1)

        if self.root is not None:
            print_node(self.root)

        lines.append("")
        best = self._get_best_node()
        lines.append(f"Total nodes: {len(self.all_nodes)}")
        lines.append(f"Best |S|: {len(best.solved_set)}/{len(self.entities)}")
        lines.append(f"Best phi: {best.phi:.4f}")
        (self.output_dir / "tree.txt").write_text("\n".join(lines))

    def _save_summary(self, rollout_num: int):
        if not self.output_dir:
            return
        best = self._get_best_node()
        trajectory = best.path_from_root()
        summary = {
            "rollout": rollout_num,
            "total_nodes": len(self.all_nodes),
            "total_entities": len(self.entities),
            "baseline_phi": self.root.phi if self.root else 0.0,
            "baseline_solved_count": len(self.root.solved_set) if self.root else 0,
            "best_phi": best.phi,
            "best_solved_count": len(best.solved_set),
            "solved_entities": list(best.solved_set),
            "trajectory_length": len(trajectory),
            "trajectory_ids": [n.id for n in trajectory],
            "refinements": [
                {
                    "entity": n.entity,
                    "refinement": n.hint,
                    "phi": n.phi,
                    "solved_count": len(n.solved_set),
                }
                for n in trajectory if n.hint
            ],
        }
        with open(self.output_dir / "summary.json", "w") as f:
            json.dump(summary, f, indent=2)
        with open(self.output_dir / "trajectory.json", "w") as f:
            json.dump([n.to_dict() for n in trajectory], f, indent=2)
