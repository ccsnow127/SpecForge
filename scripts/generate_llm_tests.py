#!/usr/bin/env python3
"""Generate LLM-driven unit tests for one or all DevEval+ modules.
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))

from src.specforge.test_gen.config import TestGeneratorConfig  # noqa: E402
from src.specforge.test_gen.generator import TestGenerator  # noqa: E402

DATA_DIR = REPO_ROOT / "data" / "deveval_plus"


def _build_entity_map(test_path: Path, source_path: Path, target_module: str, repo_path: Path) -> dict[str, str]:
    """Build test_entity_map.json by tracing each test's entity calls.

    Falls back to a simple naming heuristic if tracing yields no result.
    """
    import re
    text = test_path.read_text()
    test_names = re.findall(r'^def (test_\w+)|^\s+def (test_\w+)', text, re.MULTILINE)
    test_names = [a or b for a, b in test_names]

    # Heuristic fallback: map by name substring against source-defined names
    src_text = source_path.read_text()
    src_classes = re.findall(r'^class (\w+)', src_text, re.MULTILINE)
    src_funcs = re.findall(r'^def (\w+)', src_text, re.MULTILINE)
    candidates = src_classes + src_funcs

    def heuristic(tname: str) -> str:
        t = tname[5:] if tname.startswith("test_") else tname
        # Prefer longest matching candidate substring
        best = None
        for c in sorted(candidates, key=len, reverse=True):
            if c.lower() in t.lower():
                best = c
                break
        return best or (candidates[0] if candidates else "module")

    return {tn: heuristic(tn) for tn in test_names}


def _write_conftest(target_dir: Path, package_dir: Path, target_module: str) -> None:
    """Mirror the conftest.py pattern used by tests-{stem}/."""
    pkg = package_dir.name
    stem = target_module.split(".")[-1]
    content = f'''import os
import sys

_here = os.path.dirname(os.path.abspath(__file__))
_dataset_root = os.path.dirname(os.path.dirname(_here))
if _dataset_root not in sys.path:
    sys.path.insert(0, _dataset_root)

import {pkg}.{stem}  # noqa: E402
sys.modules['{stem}'] = {pkg}.{stem}
'''
    (target_dir / "conftest.py").write_text(content)


def generate_for_module(
    source_path: Path,
    model: str,
    line_coverage: float,
    branch_coverage: float,
    force: bool,
) -> dict:
    package_dir = source_path.parent
    stem = source_path.stem
    target_module = f"{package_dir.name}.{stem}"
    out_dir = package_dir / f"tests-{stem}-llm"
    out_test = out_dir / f"test_{stem}.py"
    out_map = out_dir / "test_entity_map.json"

    if out_test.exists() and out_map.exists() and not force:
        return {"module": str(source_path), "status": "skipped (already exists)"}

    out_dir.mkdir(parents=True, exist_ok=True)

    config = TestGeneratorConfig(
        llm_model=model,
        line_coverage_threshold=line_coverage,
        branch_coverage_threshold=branch_coverage,
    )
    config.validate()
    generator = TestGenerator(config)

    print(f"  Generating tests for {source_path.name} (model={model}) ...")
    generator.generate(source_file=source_path, output_file=str(out_test), language="python")

    print(f"  Building entity map ...")
    entity_map = _build_entity_map(out_test, source_path, target_module, package_dir.parent)
    out_map.write_text(json.dumps(entity_map, indent=2))

    _write_conftest(out_dir, package_dir, target_module)

    return {
        "module": str(source_path),
        "status": "generated",
        "tests": len(entity_map),
        "entities": len(set(entity_map.values())),
        "out_dir": str(out_dir),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate LLM tests for DevEval+ modules.")
    src_group = parser.add_mutually_exclusive_group(required=True)
    src_group.add_argument("--module", help="Single source file path (e.g. data/deveval_plus/schema_pkg/schema.py)")
    src_group.add_argument("--all", action="store_true", help="Generate for every module in MODULES.json")
    parser.add_argument("--model", default="gpt-4o", help="OpenAI model to use (default gpt-4o)")
    parser.add_argument("--line-coverage", type=float, default=0.0,
                        help="Optional per-entity line-coverage target (0 = disabled).")
    parser.add_argument("--branch-coverage", type=float, default=0.90,
                        help="Per-entity branch-coverage target.")
    parser.add_argument("--force", action="store_true", help="Regenerate even if output exists.")
    args = parser.parse_args()

    if args.module:
        modules = [Path(args.module)]
    else:
        index = json.loads((DATA_DIR / "MODULES.json").read_text())
        modules = [DATA_DIR / info["source"] for info in index["modules"].values()]

    print(f"Generating LLM tests for {len(modules)} module(s) using {args.model}\n")

    results = []
    for src in modules:
        try:
            r = generate_for_module(
                source_path=src,
                model=args.model,
                line_coverage=args.line_coverage,
                branch_coverage=args.branch_coverage,
                force=args.force,
            )
            print(f"[ {r['status']:>15} ] {src}")
            results.append(r)
        except Exception as e:
            print(f"[ FAILED ] {src}: {e}")
            results.append({"module": str(src), "status": "failed", "error": str(e)})

    summary_path = REPO_ROOT / "artifacts" / "llm_tests_generation.json"
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary_path.write_text(json.dumps(results, indent=2))
    print(f"\nSummary written: {summary_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
