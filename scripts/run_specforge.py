#!/usr/bin/env python3
"""Run SpecForge on a single module.
"""

from __future__ import annotations

import argparse
import json
import random
import sys
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))

import yaml  # noqa: E402

from src.specforge.search import SpecForge  # noqa: E402
from src.eval.test_runner import TestExecutor  # noqa: E402
from src.utils.llm_client import create_llm_client  # noqa: E402


def load_default_config() -> dict:
    cfg_path = REPO_ROOT / "configs" / "default.yaml"
    return yaml.safe_load(cfg_path.read_text())


def _load_test_dir(test_dir: Path, stem: str) -> tuple[str, list[str]] | None:
    """Return (test_path, entities) if the directory has a usable test+entity_map, else None."""
    test_path = test_dir / f"test_{stem}.py"
    map_path = test_dir / "test_entity_map.json"
    if not test_path.exists() or not map_path.exists():
        return None
    entity_map = json.loads(map_path.read_text())
    entities = sorted(set(entity_map.values()))
    return str(test_path), entities


def load_module_data(module_path: str, prefer_llm_tests: bool) -> dict:
    """Resolve search-time tests and ground-truth tests for a module.

    Returns dict with:
      - source_code, target_module, module_path
      - search_test_path, search_entities      (used by SpecForge during search)
      - eval_test_path                         (used for final ground-truth eval)
      - using_llm_for_search (bool)
    """
    source_path = Path(module_path)
    if not source_path.exists() or not source_path.is_file():
        raise FileNotFoundError(f"Source file not found: {module_path}")

    source_code = source_path.read_text()
    package_dir = source_path.parent
    pkg_name = package_dir.name
    module_stem = source_path.stem
    target_module = f"{pkg_name}.{module_stem}"

    gt_dir = package_dir / f"tests-{module_stem}"
    llm_dir = package_dir / f"tests-{module_stem}-llm"

    gt = _load_test_dir(gt_dir, module_stem)
    if gt is None:
        raise FileNotFoundError(
            f"Ground-truth tests not found at {gt_dir}/test_{module_stem}.py "
            f"+ test_entity_map.json"
        )
    gt_test_path, gt_entities = gt

    llm = _load_test_dir(llm_dir, module_stem) if prefer_llm_tests else None
    if prefer_llm_tests and llm is None:
        print(
            f"[WARN] LLM-generated tests not found at {llm_dir}. "
            f"Falling back to ground-truth tests for search "
            f"(this leaks ground-truth into the search loop). "
            f"Generate LLM tests with: "
            f"python scripts/generate_llm_tests.py --module {module_path}"
        )
        search_test_path, search_entities = gt_test_path, gt_entities
        using_llm = False
    elif llm is not None:
        search_test_path, search_entities = llm
        using_llm = True
    else:
        search_test_path, search_entities = gt_test_path, gt_entities
        using_llm = False

    return {
        "source_code": source_code,
        "target_module": target_module,
        "module_path": str(package_dir.parent),
        "search_test_path": search_test_path,
        "search_entities": search_entities,
        "eval_test_path": gt_test_path,
        "eval_entities": gt_entities,
        "using_llm_for_search": using_llm,
    }


def main() -> int:
    cfg = load_default_config()
    parser = argparse.ArgumentParser(description="Run SpecForge on a single module.")
    parser.add_argument("--module", required=True, help="Path to source file (e.g., data/deveval_plus/schema_pkg/schema.py)")
    parser.add_argument("--budget", type=int, default=cfg["search"]["budget"])
    parser.add_argument("--width", type=int, default=cfg["search"]["width"])
    parser.add_argument("--model", default=None,
                        help="LLM backbone used for both refinement and code generation.")
    parser.add_argument("--hint-model", default=cfg["llm"]["hint_model"])
    parser.add_argument("--code-model", default=cfg["llm"]["code_model"])
    parser.add_argument("--temperature", type=float, default=cfg["llm"]["hint_temperature"])
    parser.add_argument("--seed", type=int, default=cfg["random"]["seed"])
    parser.add_argument("--no-pareto-archive", action="store_true")
    parser.add_argument("--no-bandit", action="store_true")
    parser.add_argument("--ablation", choices=["iterative", "iterative+topo"], default=None)
    parser.add_argument("--output-dir", default=None)
    parser.add_argument("-o", "--output", default=None, help="Save final document to this path")
    parser.add_argument("--no-llm-tests", action="store_true",
                        help="Use ground-truth tests for both search and final eval (deviates from paper).")
    args = parser.parse_args()

    if args.model is not None:
        args.hint_model = args.model
        args.code_model = args.model

    random.seed(args.seed)
    try:
        import numpy as np
        np.random.seed(args.seed)
    except ImportError:
        pass

    data = load_module_data(args.module, prefer_llm_tests=not args.no_llm_tests)

    module_path = Path(args.module)
    display_name = f"{module_path.parent.name}_{module_path.stem}"
    if args.output_dir:
        output_dir = Path(args.output_dir)
    else:
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_dir = REPO_ROOT / "artifacts" / f"{display_name}_{args.hint_model}_{args.budget}_{ts}"
    output_dir.mkdir(parents=True, exist_ok=True)
    print(f"Artifacts: {output_dir}")
    print(f"Search-time tests: {data['search_test_path']} "
          f"({'LLM-generated' if data['using_llm_for_search'] else 'ground-truth (FALLBACK)'})")
    print(f"Ground-truth tests: {data['eval_test_path']}")
    print(f"Search entities ({len(data['search_entities'])}): {data['search_entities']}")
    if data['eval_entities'] != data['search_entities']:
        print(f"Eval entities ({len(data['eval_entities'])}): {data['eval_entities']}")

    run_config = {
        "module": args.module,
        "budget": args.budget,
        "width": args.width,
        "hint_model": args.hint_model,
        "code_model": args.code_model,
        "ablation_mode": args.ablation,
        "use_pareto_archive": not args.no_pareto_archive,
        "use_bandit": not args.no_bandit,
        "seed": args.seed,
        "temperature": args.temperature,
        "search_test_path": data["search_test_path"],
        "eval_test_path": data["eval_test_path"],
        "using_llm_for_search": data["using_llm_for_search"],
        "timestamp": datetime.now().isoformat(),
    }
    (output_dir / "config.json").write_text(json.dumps(run_config, indent=2))

    hint_llm = create_llm_client(model=args.hint_model)
    code_llm = create_llm_client(model=args.code_model)
    hint_llm.default_temperature = args.temperature

    common_te_kwargs = dict(
        target_module=data["target_module"],
        repo_path=data["module_path"],
        timeout=cfg["execution"]["test_timeout_s"],
        per_test_timeout=cfg["execution"]["per_test_timeout_s"],
    )
    search_test_executor = TestExecutor(test_suite_path=data["search_test_path"], **common_te_kwargs)
    eval_test_executor = TestExecutor(test_suite_path=data["eval_test_path"], **common_te_kwargs)

    searcher = SpecForge(
        source_code=data["source_code"],
        entities=data["search_entities"],
        test_executor=search_test_executor,
        hint_llm=hint_llm,
        code_llm=code_llm,
        budget=args.budget,
        width=args.width,
        use_pareto_archive=not args.no_pareto_archive,
        use_bandit=not args.no_bandit,
        ablation_mode=args.ablation,
        output_dir=output_dir,
        repo_name=args.module,
    )

    print(f"\nStarting SpecForge: budget={args.budget}, width={args.width}")
    print("=" * 60)
    trajectory, search_phi = searcher.search()
    print("=" * 60)
    print(f"Best phi (search / LLM tests): {search_phi:.4f}")
    print(f"Trajectory length: {len(trajectory)}")

    # === Phase 2: re-evaluate best node's code on ground-truth tests ===
    best_node = trajectory[-1] if trajectory else searcher.root
    final_phi = 0.0
    final_passed = final_failed = final_total = 0
    if best_node is not None and best_node.generated_code:
        print("\nRe-evaluating best generated code on ground-truth tests ...")
        eval_result = eval_test_executor.run(best_node.generated_code)
        final_phi = eval_result.pass_rate
        final_passed, final_failed, final_total = eval_result.passed, eval_result.failed, eval_result.total
        print(f"final_phi (ground-truth): {final_phi:.4f}  ({final_passed}/{final_total} pass)")
    else:
        print("\nNo generated code available for ground-truth eval.")

    hint_stats = hint_llm.get_token_stats()
    code_stats = code_llm.get_token_stats()
    result = {
        "search_phi": search_phi,
        "final_phi": final_phi,
        "final_passed": final_passed,
        "final_failed": final_failed,
        "final_total": final_total,
        "trajectory_length": len(trajectory),
        "total_nodes": len(searcher.all_nodes),
        "using_llm_for_search": data["using_llm_for_search"],
        "hint_llm_tokens": hint_stats,
        "code_llm_tokens": code_stats,
        "total_input_tokens": hint_stats["total_input_tokens"] + code_stats["total_input_tokens"],
        "total_output_tokens": hint_stats["total_output_tokens"] + code_stats["total_output_tokens"],
        "total_api_calls": hint_stats["total_api_calls"] + code_stats["total_api_calls"],
    }
    (output_dir / "result.json").write_text(json.dumps(result, indent=2))
    print(f"Tokens: {result['total_input_tokens']} in / {result['total_output_tokens']} out "
          f"({result['total_api_calls']} calls)")

    if best_node and best_node.doc and args.output:
        Path(args.output).parent.mkdir(parents=True, exist_ok=True)
        Path(args.output).write_text(best_node.doc)
        print(f"Final doc saved to: {args.output}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
