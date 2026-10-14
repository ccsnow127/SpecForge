#!/usr/bin/env python3
"""SWE-Dev new-feature implementation harness.
"""

from __future__ import annotations

import argparse
import ast
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))

from src.utils.llm_client import create_llm_client  # noqa: E402

SWEDEV_DATA = REPO_ROOT / "data" / "swedev_selected"

SYSTEM_MESSAGE = """\
You are an expert Python engineer.  Implement the feature described in the
PRD as a single-file `main.py` that satisfies all importable names referenced
by the test suite.  Return only Python code in one ```python block."""


def extract_python(response: str) -> str:
    matches = re.findall(r"```(?:python)?\s*\n?(.*?)\n?```", response, re.DOTALL)
    if matches:
        return "\n\n".join(matches).strip()
    return response.strip()


def find_test_file(pkg_dir: Path, test_name: str) -> Path | None:
    matches = list(pkg_dir.glob(f"tests/**/test_{test_name}.py"))
    if matches:
        return matches[0]
    matches = list(pkg_dir.glob(f"tests/**/{test_name}.py"))
    return matches[0] if matches else None


def parse_prd_path(prd_path: str) -> dict:
    """Parse a `<pkg>/prds/<difficulty>/<test_name>-<level>.md` PRD path into
    a manifest-style instance dict."""
    p = Path(prd_path).resolve()
    if not p.exists():
        raise FileNotFoundError(prd_path)
    if p.suffix != ".md":
        raise ValueError(f"PRD must be a .md file, got {p.name}")

    difficulty = p.parent.name
    if difficulty not in ("easy", "hard"):
        raise ValueError(f"PRD parent dir must be 'easy' or 'hard', got {difficulty!r}")
    if p.parent.parent.name != "prds":
        raise ValueError(f"PRD grandparent dir must be 'prds', got {p.parent.parent.name!r}")
    pkg = p.parent.parent.parent.name

    if "-level" not in p.stem:
        raise ValueError(f"PRD filename must end with '-levelN.md', got {p.name}")
    test_name, level = p.stem.rsplit("-", 1)

    try:
        prd_rel = str(p.relative_to(SWEDEV_DATA))
    except ValueError:
        prd_rel = str(p)

    return {
        "package": pkg,
        "difficulty": difficulty,
        "level": level,
        "test_name": test_name,
        "prd_file": prd_rel,
    }


_PRD_SECTION_RE = re.compile(r"^# ([A-Z][A-Z\s]+?):\s*(.*)$")


def parse_prd_sections(prd_text: str) -> dict[str, str]:
    """Split a PRD into sections keyed by `# HEADER:` line."""
    sections: dict[str, str] = {}
    current: str | None = None
    buf: list[str] = []
    for line in prd_text.splitlines():
        m = _PRD_SECTION_RE.match(line)
        if m:
            if current is not None:
                sections[current] = "\n".join(buf).strip("\n")
            current = m.group(1).strip()
            inline = m.group(2)
            buf = [inline] if inline else []
        elif current is not None:
            buf.append(line)
    if current is not None:
        sections[current] = "\n".join(buf).strip("\n")
    return sections


def extract_targets_from_prd(prd_sections: dict[str, str]) -> list[tuple[str, str | None, str]]:
    """Parse FOLDER STRUCTURE into [(file_path, class_or_None, func_name)]."""
    folder = prd_sections.get("FOLDER STRUCTURE", "")
    if "```" in folder:
        folder = re.sub(r"```[a-z]*\n?", "", folder).strip("`\n ")

    targets: list[tuple[str, str | None, str]] = []
    current_file: str | None = None
    pending_dir: list[str] = []

    for raw in folder.splitlines():
        token = re.sub(r"^[\s│├└─]+", "", raw).rstrip()
        if not token or token in ("..", "."):
            continue
        if token.endswith(".py"):
            current_file = "/".join(pending_dir + [token]) if pending_dir else token
            pending_dir = []
        elif token.endswith("/"):
            pending_dir.append(token.rstrip("/"))
        elif current_file:
            if "." in token:
                cls, fn = token.rsplit(".", 1)
                targets.append((current_file, cls, fn))
            else:
                targets.append((current_file, None, token))
    return targets


def _remove_target_functions(source_code: str, targets: set[tuple[str | None, str]]) -> str:
    """Delete the `def` block of each target function (signature + docstring
    included). Empty classes receive `pass`."""
    try:
        tree = ast.parse(source_code)
    except SyntaxError:
        return source_code

    class Remover(ast.NodeTransformer):
        def __init__(self) -> None:
            self.class_stack: list[str] = []

        def visit_ClassDef(self, node: ast.ClassDef) -> ast.ClassDef:
            self.class_stack.append(node.name)
            node.body = [s for s in (self.visit(c) for c in node.body) if s is not None]
            self.class_stack.pop()
            if not node.body:
                node.body = [ast.Pass()]
            return node

        def visit_FunctionDef(self, node):
            return self._maybe_remove(node)

        def visit_AsyncFunctionDef(self, node):
            return self._maybe_remove(node)

        def _maybe_remove(self, node):
            cls = self.class_stack[-1] if self.class_stack else None
            if (cls, node.name) in targets or (None, node.name) in targets:
                return None
            return node

        def visit_Module(self, node: ast.Module) -> ast.Module:
            node.body = [s for s in (self.visit(c) for c in node.body) if s is not None]
            return node

    removed = Remover().visit(tree)
    ast.fix_missing_locations(removed)
    return ast.unparse(removed)


def build_stub_source(pkg_dir: Path, prd_sections: dict[str, str]) -> str:
    """Concatenate package source with target functions removed."""
    by_file: dict[str, set[tuple[str | None, str]]] = {}
    for file_path, cls, fn in extract_targets_from_prd(prd_sections):
        by_file.setdefault(file_path, set()).add((cls, fn))

    py_files: list[Path] = []
    for p in sorted(pkg_dir.rglob("*.py")):
        rel_parts = p.relative_to(pkg_dir).parts
        if rel_parts and rel_parts[0] in ("tests", "test", "prds", "build", "dist"):
            continue
        if p.name in ("setup.py", "conftest.py"):
            continue
        py_files.append(p)

    chunks: list[str] = []
    for p in py_files:
        try:
            text = p.read_text()
        except UnicodeDecodeError:
            continue
        rel = str(p.relative_to(pkg_dir))
        targets_for_file: set[tuple[str | None, str]] = set()
        for prd_path, fns in by_file.items():
            if rel == prd_path or rel.endswith("/" + prd_path):
                targets_for_file |= fns
        if targets_for_file:
            text = _remove_target_functions(text, targets_for_file)
        chunks.append(f"# === {rel} ===\n{text}")
    return "\n\n".join(chunks)


def rebuild_prd_with_module_doc(prd_sections: dict[str, str], module_doc: str) -> str:
    """Replace IMPLEMENTATION REQUIREMENTS with module_doc; preserve other sections."""
    section_order = [
        "PROJECT NAME",
        "FOLDER STRUCTURE",
        "IMPLEMENTATION REQUIREMENTS",
        "TASK DESCRIPTION",
    ]
    seen: set[str] = set()
    parts: list[str] = []
    for name in section_order:
        if name not in prd_sections:
            continue
        seen.add(name)
        if name == "IMPLEMENTATION REQUIREMENTS":
            parts.append(f"# IMPLEMENTATION REQUIREMENTS:\n{module_doc.strip()}")
        else:
            parts.append(f"# {name}:\n{prd_sections[name]}".rstrip())
    for name, body in prd_sections.items():
        if name not in seen:
            parts.append(f"# {name}:\n{body}".rstrip())
    return "\n\n".join(parts)


def find_module_doc(docs_dir: Path, pkg: str) -> str | None:
    """Look up `<docs_dir>/<pkg>.md` or concatenate `<docs_dir>/<pkg>/*.md`."""
    flat = docs_dir / f"{pkg}.md"
    if flat.exists():
        return flat.read_text()
    sub = docs_dir / pkg
    if sub.is_dir():
        parts = [p.read_text() for p in sorted(sub.glob("*.md"))]
        if parts:
            return "\n\n".join(parts)
    return None


def build_prompt(
    setup: str,
    prd_text: str,
    prd_sections: dict[str, str],
    stub_source: str | None,
    module_doc: str | None,
    test_source: str,
) -> str:
    """Setup 1 = PRD verbatim; Setup 2 = PRD with IMPLEMENTATION REQUIREMENTS
    swapped for module_doc. Both use stubbed source."""
    if stub_source is None:
        raise ValueError("stub source is required")

    head = (
        "Implement a single `main.py` so that the following test passes.\n\n"
        f"## Test (read-only)\n```python\n{test_source}\n```\n"
    )

    if setup == "1":
        spec = prd_text
    elif setup == "2":
        if module_doc is None:
            raise ValueError("setup 2 requires --docs-dir")
        spec = rebuild_prd_with_module_doc(prd_sections, module_doc)
    else:
        raise ValueError(f"unknown setup {setup!r}")

    return head + (
        f"\n{spec}\n"
        f"\n## Stubbed Package Source\n```python\n{stub_source}\n```\n"
        "\n## Task\nGenerate `main.py` containing implementations of the "
        "target functions listed in the FOLDER STRUCTURE."
    )


def parse_pytest(out: str) -> tuple[int, int]:
    m_pass = re.search(r"(\d+) passed", out)
    m_fail = re.search(r"(\d+) failed", out)
    m_err = re.search(r"(\d+) error", out)
    n_pass = int(m_pass.group(1)) if m_pass else 0
    n_fail = (int(m_fail.group(1)) if m_fail else 0) + (int(m_err.group(1)) if m_err else 0)
    return n_pass, n_fail


def run_pytest(rundir: Path, pkg_dir: Path, test_rel: str, timeout: int) -> tuple[str, int]:
    env = os.environ.copy()
    env["PYTHONPATH"] = f"{rundir}{os.pathsep}{pkg_dir}{os.pathsep}{env.get('PYTHONPATH', '')}"
    proc = subprocess.run(
        ["python", "-m", "pytest", test_rel, "-q", "--tb=short"],
        cwd=rundir, env=env, capture_output=True, text=True, timeout=timeout,
    )
    return proc.stdout + proc.stderr, proc.returncode


def run_one(inst: dict, setup: str, llm, args, docs_dir: Path | None) -> dict:
    pkg = inst["package"]
    test_name = inst["test_name"]
    diff = inst["difficulty"]
    level = inst["level"]
    pkg_dir = SWEDEV_DATA / pkg

    out_dir = Path(args.output_dir) / setup / f"{pkg}__{diff}__{level}__{test_name}"
    result_path = out_dir / "result.json"
    if args.resume and result_path.exists():
        return {**inst, "setup": setup, "status": "RESUMED",
                **json.loads(result_path.read_text())}

    test_path = find_test_file(pkg_dir, test_name)
    if test_path is None:
        return {**inst, "setup": setup, "status": "NO_TEST", "n_pass": 0, "n_fail": 0}

    prd_path = Path(inst["prd_file"])
    if not prd_path.is_absolute():
        prd_path = SWEDEV_DATA / prd_path
    if not prd_path.exists():
        return {**inst, "setup": setup, "status": "NO_PRD", "n_pass": 0, "n_fail": 0}
    prd = prd_path.read_text()

    prd_sections = parse_prd_sections(prd)
    stub_source = build_stub_source(pkg_dir, prd_sections)
    module_doc = find_module_doc(docs_dir, pkg) if (setup == "2" and docs_dir) else None
    if setup == "2" and module_doc is None:
        return {**inst, "setup": setup, "status": "NO_MODULE_DOC", "n_pass": 0, "n_fail": 0}

    test_source = test_path.read_text()
    prompt = build_prompt(setup, prd, prd_sections, stub_source, module_doc, test_source)

    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "prompt.txt").write_text(f"SYSTEM:\n{SYSTEM_MESSAGE}\n\nUSER:\n{prompt}")

    if args.dry_run:
        return {**inst, "setup": setup, "status": "DRY_RUN", "n_pass": 0, "n_fail": 0}

    try:
        response = llm.generate(prompt, temperature=args.temperature, system=SYSTEM_MESSAGE)
    except Exception as e:
        result = {"status": "LLM_ERROR", "msg": f"{type(e).__name__}: {e}",
                  "n_pass": 0, "n_fail": 0, "pass_rate": 0.0, "fully_solved": False}
        result_path.write_text(json.dumps(result, indent=2))
        return {**inst, "setup": setup, **result}

    code = extract_python(response)
    (out_dir / "main.py").write_text(code)

    rundir = out_dir / "_run"
    if rundir.exists():
        shutil.rmtree(rundir)
    rundir.mkdir()
    shutil.copy2(out_dir / "main.py", rundir / "main.py")
    test_rel_dir = rundir / "tests"
    test_rel_dir.mkdir()
    (test_rel_dir / "__init__.py").write_text("")
    shutil.copy2(test_path, test_rel_dir / test_path.name)
    for extra in test_path.parent.iterdir():
        if extra.suffix in (".txt", ".html", ".json", ".csv") and extra.is_file():
            shutil.copy2(extra, test_rel_dir / extra.name)

    try:
        out, rc = run_pytest(rundir, pkg_dir, f"tests/{test_path.name}", args.timeout)
    except subprocess.TimeoutExpired:
        out = "TIMEOUT"
        rc = -1
    (out_dir / "pytest.txt").write_text(out)
    n_pass, n_fail = parse_pytest(out)
    total = n_pass + n_fail
    pass_rate = (n_pass / total) if total else 0.0

    result = {
        "status": "OK" if rc == 0 else "TEST_FAIL",
        "n_pass": n_pass, "n_fail": n_fail,
        "pass_rate": pass_rate,
        "fully_solved": (n_pass > 0 and n_fail == 0),
    }
    result_path.write_text(json.dumps(result, indent=2))
    shutil.rmtree(rundir, ignore_errors=True)
    return {**inst, "setup": setup, **result}


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--prd",
                    help="Path to a single PRD .md file (e.g. "
                         "data/swedev_selected/vulture/prds/easy/test_conditions-level1.md)")
    ap.add_argument("--all", action="store_true",
                    help="Run every PRD found under data/swedev_selected/*/prds/{easy,hard}/")
    ap.add_argument("--model", default="gpt-4o")
    ap.add_argument("--temperature", type=float, default=0.2)
    ap.add_argument("--num-workers", type=int, default=4,
                    help="Parallel pytest workers (only meaningful with --all)")
    ap.add_argument("--timeout", type=int, default=120)
    ap.add_argument("--docs-dir", default=None,
                    help="Directory of per-package module-level docs (one <pkg>.md "
                         "or one <pkg>/*.md per package); the docs may come from any "
                         "source (SpecForge, RepoAgent, hand-written). "
                         "Omit to run Setup 1 (PRD + source — SWE-Dev's default); "
                         "pass to run Setup 2 (module-level doc + source).")
    ap.add_argument("--output-dir", default=str(REPO_ROOT / "artifacts" / "swedev_eval"))
    ap.add_argument("--resume", action="store_true",
                    help="Skip instances whose result.json already exists (--all only)")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    if not args.prd and not args.all:
        ap.error("specify --prd <path> or --all")
    if args.prd and args.all:
        ap.error("--prd and --all are mutually exclusive")

    setup = "2" if args.docs_dir else "1"
    docs_dir = Path(args.docs_dir) if args.docs_dir else None

    if args.prd:
        try:
            selected = [parse_prd_path(args.prd)]
        except (ValueError, FileNotFoundError) as e:
            ap.error(str(e))
    else:
        selected = [parse_prd_path(str(p))
                    for p in sorted(SWEDEV_DATA.glob("*/prds/*/*.md"))]
    print(f"Running setup {setup} on {len(selected)} instance(s)", flush=True)

    llm = None if args.dry_run else create_llm_client(args.model)
    Path(args.output_dir).mkdir(parents=True, exist_ok=True)

    rows: list[dict] = []
    with ThreadPoolExecutor(max_workers=args.num_workers) as pool:
        futures = {pool.submit(run_one, inst, setup, llm, args, docs_dir): inst
                   for inst in selected}
        for fut in as_completed(futures):
            inst = futures[fut]
            try:
                row = fut.result()
            except Exception as e:
                row = {**inst, "setup": setup, "status": f"ERROR: {e}",
                       "n_pass": 0, "n_fail": 0}
            rows.append(row)
            print(f"  [{row['status']:>10s}] {setup}/{inst['difficulty']:4s} "
                  f"{inst['package']:18s} {inst['test_name']:36s} "
                  f"pass={row.get('n_pass', 0)} fail={row.get('n_fail', 0)}", flush=True)

    summary_path = Path(args.output_dir) / "summary.json"
    summary_path.write_text(json.dumps(rows, indent=2))
    print(f"\nSummary -> {summary_path}")


if __name__ == "__main__":
    main()
