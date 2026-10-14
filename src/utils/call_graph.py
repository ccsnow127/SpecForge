"""
Static call-graph extraction for module-level entities.
Builds an entity-to-callees graph by walking the Python AST of the source, with descendant/ancestor queries.
"""

from __future__ import annotations

import ast
from typing import Iterator, Optional


class CallGraph:
    """Entity-level call graph used to order entity refinement by dependencies."""

    def __init__(
        self,
        source_code: str,
        entities: list[str],
        entity_code_extractor=None,
    ):
        self.source_code = source_code
        self.entities = entities
        self._entity_set = set(entities)
        self._graph: dict[str, list[str]] | None = None
        self._descendants_cache: dict[str, set[str]] = {}
        self._ancestors_cache: dict[str, set[str]] = {}

    def graph(self) -> dict[str, list[str]]:
        if self._graph is None:
            self._graph = self._build()
        return self._graph

    def descendants(self, entity: str) -> set[str]:
        if entity in self._descendants_cache:
            return self._descendants_cache[entity]
        graph = self.graph()
        out: set[str] = set()
        visited: set[str] = set()

        def dfs(e: str):
            if e in visited:
                return
            visited.add(e)
            for callee in graph.get(e, []):
                out.add(callee)
                dfs(callee)

        dfs(entity)
        self._descendants_cache[entity] = out
        return out

    def ancestors(self, entity: str) -> set[str]:
        if entity in self._ancestors_cache:
            return self._ancestors_cache[entity]
        graph = self.graph()
        reverse: dict[str, list[str]] = {e: [] for e in self.entities}
        for caller, callees in graph.items():
            for callee in callees:
                if callee in reverse:
                    reverse[callee].append(caller)

        out: set[str] = set()
        visited: set[str] = set()

        def dfs(e: str):
            if e in visited:
                return
            visited.add(e)
            for caller in reverse.get(e, []):
                out.add(caller)
                dfs(caller)

        dfs(entity)
        self._ancestors_cache[entity] = out
        return out

    def leaf_entities(self, entities_with_errors: list[str]) -> list[str]:
        graph = self.graph()
        leaves = []
        for e in entities_with_errors:
            callees = graph.get(e, [])
            if not any(c in entities_with_errors for c in callees):
                leaves.append(e)
        return leaves

    def _build(self) -> dict[str, list[str]]:
        graph: dict[str, list[str]] = {e: [] for e in self.entities}
        try:
            tree = ast.parse(self.source_code)
        except SyntaxError:
            return graph

        for entity_name, subtree, class_context in self._walk_entities(tree):
            callees = self._collect_calls(subtree, class_context)
            callees.discard(entity_name)  # no self-loops
            # Preserve stable order + dedup
            seen = set()
            out: list[str] = []
            for c in sorted(callees):
                if c in graph and c not in seen:
                    out.append(c)
                    seen.add(c)
            graph[entity_name] = out
        return graph

    def _walk_entities(
        self, tree: ast.AST
    ) -> Iterator[tuple[str, ast.AST, Optional[str]]]:
        entity_set = self._entity_set

        def walk(node: ast.AST, class_stack: list[str]):
            if isinstance(node, ast.Module):
                for child in node.body:
                    yield from walk(child, class_stack)
                return

            if isinstance(node, ast.ClassDef):
                enclosing = class_stack[-1] if class_stack else None
                if node.name in entity_set:
                    yield (node.name, node, enclosing)
                class_stack.append(node.name)
                for child in node.body:
                    yield from walk(child, class_stack)
                class_stack.pop()
                return

            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                current = class_stack[-1] if class_stack else None
                if current is not None:
                    qualified = f"{current}.{node.name}"
                    if qualified in entity_set:
                        yield (qualified, node, current)
                    if node.name in entity_set:
                        yield (node.name, node, current)
                else:
                    if node.name in entity_set:
                        yield (node.name, node, None)
                # Do not recurse into function bodies for entity discovery;
                # nested defs inside functions are not treated as entities here.
                return

            if isinstance(node, ast.Assign):
                enclosing = class_stack[-1] if class_stack else None
                for target in node.targets:
                    if isinstance(target, ast.Name) and target.id in entity_set:
                        yield (target.id, node, enclosing)
                return

            if isinstance(node, ast.AnnAssign):
                enclosing = class_stack[-1] if class_stack else None
                if (
                    isinstance(node.target, ast.Name)
                    and node.target.id in entity_set
                ):
                    yield (node.target.id, node, enclosing)
                return

        yield from walk(tree, [])

    def _collect_calls(
        self, subtree: ast.AST, initial_class: Optional[str]
    ) -> set[str]:
        entity_set = self._entity_set
        class_stack: list[str] = [initial_class] if initial_class else []
        found: set[str] = set()

        def resolve(func: ast.AST) -> Optional[str]:
            current = class_stack[-1] if class_stack else None
            if isinstance(func, ast.Name):
                return func.id if func.id in entity_set else None
            if isinstance(func, ast.Attribute):
                attr = func.attr
                # self.method(...)
                if (
                    isinstance(func.value, ast.Name)
                    and func.value.id == "self"
                    and current is not None
                ):
                    qualified = f"{current}.{attr}"
                    if qualified in entity_set:
                        return qualified
                    if attr in entity_set:
                        return attr
                    return None
                # ClassName.method(...) or module_obj.method(...)
                if isinstance(func.value, ast.Name):
                    qualified = f"{func.value.id}.{attr}"
                    if qualified in entity_set:
                        return qualified
                    return None
                # Deeper attribute chains: fall back to bare attr as a last resort
                if attr in entity_set:
                    return attr
            return None

        def walk(node: ast.AST):
            if isinstance(node, ast.ClassDef):
                class_stack.append(node.name)
                for child in ast.iter_child_nodes(node):
                    walk(child)
                class_stack.pop()
                return
            if isinstance(node, ast.Call):
                target = resolve(node.func)
                if target is not None:
                    found.add(target)
            for child in ast.iter_child_nodes(node):
                walk(child)

        walk(subtree)
        return found
