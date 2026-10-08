#!/usr/bin/env python3
"""Aggregate per-instance result.json files written by `run_swedev.py`.
"""

from __future__ import annotations

import argparse
import json
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]


def parse_dir_name(name: str) -> tuple[str, str, str, str] | None:
    """`pkg__difficulty__level__test_name` -> tuple, or None if malformed."""
    parts = name.split("__")
    if len(parts) < 4:
        return None
    pkg, diff, level = parts[0], parts[1], parts[2]
    test_name = "__".join(parts[3:])
    return pkg, diff, level, test_name


def collect(runs_dir: Path) -> list[dict]:
    rows: list[dict] = []
    for setup_dir in sorted(runs_dir.iterdir()):
        if not setup_dir.is_dir():
            continue
        setup = setup_dir.name
        for inst_dir in sorted(setup_dir.iterdir()):
            if not inst_dir.is_dir():
                continue
            parsed = parse_dir_name(inst_dir.name)
            if parsed is None:
                continue
            pkg, diff, level, test_name = parsed
            res_path = inst_dir / "result.json"
            if not res_path.exists():
                continue
            res = json.loads(res_path.read_text())
            rows.append({
                "setup": setup, "package": pkg, "difficulty": diff,
                "level": level, "test_name": test_name, **res,
            })
    return rows


def aggregate(rows: list[dict]) -> dict:
    grouped: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for r in rows:
        grouped[(r["setup"], r["difficulty"])].append(r)

    summary: dict[str, dict[str, dict[str, float | int]]] = {}
    for (setup, diff), items in sorted(grouped.items()):
        n = len(items)
        if n == 0:
            continue
        avg_pass = sum(r.get("pass_rate", 0.0) for r in items) / n
        n_solved = sum(1 for r in items if r.get("fully_solved"))
        summary.setdefault(setup, {})[diff] = {
            "n_instances": n,
            "pass_rate_mean": 100.0 * avg_pass,
            "fully_solved": n_solved,
            "fully_solved_pct": 100.0 * n_solved / n,
        }
    return summary


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--runs-dir", required=True)
    ap.add_argument("--out", default=str(REPO_ROOT / "artifacts" / "swedev_summary.json"))
    args = ap.parse_args()

    runs_dir = Path(args.runs_dir)
    rows = collect(runs_dir)
    summary = aggregate(rows)

    print(f"{'Setup':>6} {'Diff':>6} {'N':>4} {'PassRate':>10} {'Solved':>14}")
    print("-" * 48)
    for setup in sorted(summary):
        for diff in sorted(summary[setup]):
            d = summary[setup][diff]
            print(f"{setup:>6} {diff:>6} {d['n_instances']:>4} "
                  f"{d['pass_rate_mean']:>9.2f}% "
                  f"{d['fully_solved']:>3}/{d['n_instances']:<3} ({d['fully_solved_pct']:>5.1f}%)")

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps({"per_setup": summary, "rows": rows}, indent=2))
    print(f"\nSummary -> {out_path}")


if __name__ == "__main__":
    main()
