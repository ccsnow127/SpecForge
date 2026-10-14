"""
Test generator module.
Generates unit tests for source code using an LLM with coverage feedback loops.
"""

from .config import TestGeneratorConfig
from .models import (
    FunctionInfo,
    DependencyInfo,
    TestContext,
    TestCase,
    Coverage,
    TestSuite,
    TestResult,
)
from .generator import TestGenerator

__all__ = [
    "TestGeneratorConfig",
    "TestGenerator",
    "FunctionInfo",
    "DependencyInfo",
    "TestContext",
    "TestCase",
    "Coverage",
    "TestSuite",
    "TestResult",
]

__version__ = "1.0.0"
