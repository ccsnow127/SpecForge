"""Evaluation primitives: metrics, code generation, sandboxed test execution."""

from .codegen import EntityDocGenerator, BaselineDocGenerator, CodeGenerator
from .test_runner import TestExecutor, TestResult
from .metrics import (
    solve_set_size,
    module_pass_rate,
    compute_entity_phi,
    compute_solved_set,
)

__all__ = [
    "EntityDocGenerator",
    "BaselineDocGenerator",
    "CodeGenerator",
    "TestExecutor",
    "TestResult",
    "solve_set_size",
    "module_pass_rate",
    "compute_entity_phi",
    "compute_solved_set",
]
