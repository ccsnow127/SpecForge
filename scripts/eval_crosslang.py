#!/usr/bin/env python3
"""Compile + run the JUnit 5 harness against LLM-generated Java translations.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
JAVA_DATA = REPO_ROOT / "data" / "deveval_plus_java"

_CLASS_RE = re.compile(r"(?:public\s+)?class\s+([A-Z][A-Za-z0-9_]*)")


def java_class_for_module(module: str) -> str:
    stub = (JAVA_DATA / module / "api_stub.java").read_text()
    m = _CLASS_RE.search(stub)
    if not m:
        raise ValueError(f"{module}: no `class` declaration in api_stub.java")
    return m.group(1)


def discover_modules() -> list[str]:
    return sorted(p.name for p in JAVA_DATA.iterdir()
                  if p.is_dir() and not p.name.startswith("."))


def stage_java(module: str, src: Path, java_class: str) -> tuple[Path, Path | None]:
    """Copy `src` into the module's main java dir; return (target_path, backup_path)."""
    main_dir = JAVA_DATA / module / "src" / "main" / "java"
    main_dir.mkdir(parents=True, exist_ok=True)
    target = main_dir / f"{java_class}.java"
    backup = None
    if target.exists():
        backup = target.with_suffix(".java.bak")
        shutil.copy2(target, backup)
    shutil.copy2(src, target)
    return target, backup


def restore(target: Path, backup: Path | None) -> None:
    if backup is None:
        try:
            target.unlink()
        except FileNotFoundError:
            pass
    else:
        shutil.move(str(backup), str(target))


def run_mvn(module: str, mvn_bin: str, timeout: int) -> tuple[str, int]:
    pom = JAVA_DATA / module / "pom.xml"
    proc = subprocess.run(
        [mvn_bin, "-q", "test", "-f", str(pom)],
        capture_output=True, text=True, env=os.environ.copy(), timeout=timeout,
    )
    return proc.stdout + proc.stderr, proc.returncode


def parse_surefire(module: str) -> dict:
    """Aggregate Surefire XML reports.  Returns counts + per-test failure set."""
    reports_dir = JAVA_DATA / module / "target" / "surefire-reports"
    failed: set[str] = set()
    total = 0
    failures = 0
    errors = 0
    skipped = 0
    if not reports_dir.exists():
        return {"compiled": False, "total": 0, "passed": 0, "failed_tests": set()}
    for xml in reports_dir.glob("TEST-*.xml"):
        try:
            root = ET.parse(xml).getroot()
        except ET.ParseError:
            continue
        total += int(root.attrib.get("tests", 0))
        failures += int(root.attrib.get("failures", 0))
        errors += int(root.attrib.get("errors", 0))
        skipped += int(root.attrib.get("skipped", 0))
        for case in root.findall("testcase"):
            name = case.attrib.get("name", "")
            if case.find("failure") is not None or case.find("error") is not None:
                failed.add(name)
    passed = total - failures - errors - skipped
    return {
        "compiled": total > 0,
        "total": total, "passed": passed,
        "failures": failures, "errors": errors, "skipped": skipped,
        "failed_tests": failed,
    }


def compute_phi(failed: set[str], entity_map: dict[str, str]) -> dict:
    """`entity_map` maps test method name -> entity name."""
    by_entity: dict[str, list[str]] = defaultdict(list)
    for test, ent in entity_map.items():
        by_entity[ent].append(test)
    if not by_entity:
        return {"phi_bar": None, "S": None, "S_total": None}
    phis = []
    solved = 0
    for ent, tests in by_entity.items():
        n = len(tests)
        p = sum(1 for t in tests if t not in failed)
        phi = p / n if n else 0.0
        phis.append(phi)
        if phi == 1.0:
            solved += 1
    return {
        "phi_bar": 100.0 * sum(phis) / len(phis),
        "S": solved,
        "S_total": len(by_entity),
    }


def evaluate_one(module: str, config: str, java_file: Path,
                 mvn_bin: str, timeout: int) -> dict:
    java_class = java_class_for_module(module)
    target, backup = stage_java(module, java_file, java_class)
    try:
        output, rc = run_mvn(module, mvn_bin, timeout)
        compiled = "COMPILATION ERROR" not in output
        report = parse_surefire(module)
    finally:
        restore(target, backup)

    if not compiled or not report["compiled"]:
        return {
            "module": module, "config": config,
            "compiled": False, "total": 0, "passed": 0,
            "failed_tests": [], "phi_bar": 0.0, "S": 0, "S_total": None,
        }

    entity_map_path = JAVA_DATA / module / "entity_map.json"
    metrics: dict = {"phi_bar": None, "S": None, "S_total": None}
    if entity_map_path.exists():
        emap = json.loads(entity_map_path.read_text())
        metrics = compute_phi(report["failed_tests"], emap)

    return {
        "module": module, "config": config,
        "compiled": True,
        "total": report["total"], "passed": report["passed"],
        "failures": report["failures"], "errors": report["errors"],
        "failed_tests": sorted(report["failed_tests"]),
        **metrics,
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--module", help="Module under data/deveval_plus_java/")
    ap.add_argument("--all", action="store_true", help="Evaluate every module found in --gen-dir")
    ap.add_argument("--gen-dir", required=True, help="Output dir from run_crosslang.py")
    ap.add_argument("--configs", nargs="+", default=["source_only", "doc_only", "doc_source"])
    ap.add_argument("--output-dir", default=str(REPO_ROOT / "artifacts" / "crosslang_eval"))
    ap.add_argument("--mvn-bin", default="mvn")
    ap.add_argument("--timeout", type=int, default=180)
    args = ap.parse_args()

    if not args.module and not args.all:
        ap.error("specify --module or --all")
    gen_dir = Path(args.gen_dir)
    out_dir = Path(args.output_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    modules = (
        sorted(p.name for p in gen_dir.iterdir() if p.is_dir())
        if args.all else [args.module]
    )

    rows: list[dict] = []
    print(f"{'Module':<24} {'Config':<14} {'Compile':>8} {'Tests':>10} {'φ̄':>8} {'|S|':>10}")
    print("-" * 82)
    for module in modules:
        for cfg in args.configs:
            java_file = gen_dir / module / f"{cfg}.java"
            if not java_file.exists():
                print(f"{module:<24} {cfg:<14} {'N/A':>8}")
                continue
            row = evaluate_one(module, cfg, java_file, args.mvn_bin, args.timeout)
            rows.append(row)
            phi = f"{row['phi_bar']:.1f}%" if row.get("phi_bar") is not None else " — "
            sstr = (f"{row['S']}/{row['S_total']}"
                    if row.get("S_total") else " — ")
            comp = "PASS" if row["compiled"] else "FAIL"
            print(f"{module:<24} {cfg:<14} {comp:>8} {row['passed']:>3}/{row['total']:<5}    {phi:>6} {sstr:>10}")

    (out_dir / "results.json").write_text(json.dumps(rows, indent=2))
    print(f"\nResults -> {out_dir / 'results.json'}")


if __name__ == "__main__":
    main()
