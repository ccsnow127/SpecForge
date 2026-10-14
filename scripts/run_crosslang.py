#!/usr/bin/env python3
"""Cross-language translation harness — Python module -> Java code.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))

from src.utils.llm_client import create_llm_client  # noqa: E402

JAVA_DATA = REPO_ROOT / "data" / "deveval_plus_java"
PY_DATA = REPO_ROOT / "data" / "deveval_plus"
ARTIFACTS = REPO_ROOT / "artifacts"

CONFIGS = ["source_only", "doc_only", "doc_source"]
DEFAULT_TEMPERATURES = [0.0, 0.3, 0.5]

SYSTEM_MESSAGE = """\
You are an expert software engineer translating Python to Java.
Generate a complete, compilable Java 17 source file.
- Use the class/method signatures from the provided API stub verbatim
- Convert snake_case to camelCase, __init__ to constructors
- Implement ALL classes and methods — do not skip or leave stubs
- Generate ONLY Java code in ```java blocks, no explanations"""


_CLASS_RE = re.compile(r"(?:public\s+)?class\s+([A-Z][A-Za-z0-9_]*)")


def java_class_from_stub(stub: str) -> str:
    """Parse the first top-level `class X` declaration from an api_stub.java."""
    m = _CLASS_RE.search(stub)
    if not m:
        raise ValueError("could not find `class` declaration in api_stub.java")
    return m.group(1)


def discover_modules() -> list[str]:
    return sorted(p.name for p in JAVA_DATA.iterdir()
                  if p.is_dir() and not p.name.startswith("."))


def find_python_source(module: str) -> Path | None:
    """Locate the Python source file for a Java module name.

    `data/deveval_plus_java/<module>/meta.json` may carry a `source` pointer
    relative to `data/deveval_plus/`. Falls back to common naming patterns.
    """
    meta_path = JAVA_DATA / module / "meta.json"
    if meta_path.exists():
        meta = json.loads(meta_path.read_text())
        rel = meta.get("python_source") or meta.get("source")
        if rel:
            cand = PY_DATA / rel
            if cand.exists():
                return cand
    for pattern in (
        f"{module}/{module}.py",
        f"{module.split('_', 1)[0]}/{module.split('_', 1)[-1]}.py",
        f"{module}/*.py",
    ):
        matches = list(PY_DATA.glob(pattern))
        if matches:
            return matches[0]
    return None


def read_doc(doc_dir: Path | None) -> str | None:
    if doc_dir is None:
        return None
    p = Path(doc_dir)
    if p.is_file():
        text = p.read_text()
    elif p.is_dir():
        cand = p / "doc.md"
        if not cand.exists():
            return None
        text = cand.read_text()
    else:
        return None
    if text.startswith("```markdown"):
        text = text[len("```markdown"):].strip()
        if text.endswith("```"):
            text = text[:-3].strip()
    return text


def extract_java_code(response: str) -> str:
    matches = re.findall(r"```(?:java)?\s*\n?(.*?)\n?```", response, re.DOTALL)
    if matches:
        return "\n\n".join(matches).strip()
    return response.strip()


def stub_block(stub: str) -> str:
    return f"\n## Java API Stub (signatures you MUST match)\n```java\n{stub}\n```\n"


def build_prompt(config: str, source: str, doc: str | None, stub: str, java_class: str) -> str:
    head = stub_block(stub)
    if config == "source_only":
        body = f"## Python Source Code\n{source}"
    elif config == "doc_only":
        body = f"## API Documentation\n{doc}"
    elif config == "doc_source":
        body = f"## API Documentation\n{doc}\n\n## Reference Python Source\n{source}"
    else:
        raise ValueError(f"unknown config {config!r}")
    return (
        f"Implement this API as Java code.{head}\n{body}\n\n"
        f"## Java Class Name: {java_class}"
    )


def generate_with_retry(llm, prompt: str, temperatures: list[float]) -> tuple[str, bool]:
    for i, temp in enumerate(temperatures):
        print(f"    attempt {i + 1}/{len(temperatures)} (temp={temp})", flush=True)
        try:
            response = llm.generate(prompt, temperature=temp, system=SYSTEM_MESSAGE)
        except Exception as e:
            print(f"    LLM error: {e}", flush=True)
            continue
        code = extract_java_code(response)
        if code:
            return code, True
    return "", False


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--module", help="Module name under data/deveval_plus_java/")
    ap.add_argument("--all", action="store_true", help="Run all 16 modules")
    ap.add_argument("--configs", nargs="+", default=CONFIGS, choices=CONFIGS)
    ap.add_argument("--doc-dir", help="Path to SpecForge doc.md (file or run dir); required for doc_only/doc_source")
    ap.add_argument("--model", default="gpt-4o")
    ap.add_argument("--temperatures", nargs="+", type=float, default=DEFAULT_TEMPERATURES,
                    help="Temperatures swept across retries (default: 0.0 0.3 0.5)")
    ap.add_argument("--output-dir", default=None,
                    help="Default: artifacts/crosslang_<model>_<timestamp>")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    if not args.module and not args.all:
        ap.error("specify --module or --all")
    modules = discover_modules() if args.all else [args.module]

    needs_doc = any(c in args.configs for c in ("doc_only", "doc_source"))
    doc = read_doc(args.doc_dir) if needs_doc and args.doc_dir else None
    if needs_doc and doc is None:
        print("WARNING: --doc-dir not provided or empty; doc_only/doc_source will be skipped", flush=True)

    if args.output_dir:
        out_dir = Path(args.output_dir)
    else:
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        out_dir = ARTIFACTS / f"crosslang_{args.model}_{ts}"
    out_dir.mkdir(parents=True, exist_ok=True)

    llm = None if args.dry_run else create_llm_client(args.model)
    summary: dict[str, dict[str, str]] = {}

    for module in modules:
        stub_path = JAVA_DATA / module / "api_stub.java"
        if not stub_path.exists():
            print(f"[SKIP] {module}: no api_stub.java", flush=True)
            summary[module] = {"_status": "no_stub"}
            continue
        stub = stub_path.read_text()
        try:
            java_class = java_class_from_stub(stub)
        except ValueError as e:
            print(f"[SKIP] {module}: {e}", flush=True)
            summary[module] = {"_status": "no_public_class"}
            continue
        py_path = find_python_source(module)
        if py_path is None:
            print(f"[SKIP] {module}: no Python source found under data/deveval_plus/", flush=True)
            summary[module] = {"_status": "no_python_source"}
            continue

        source = py_path.read_text()
        mod_dir = out_dir / module
        mod_dir.mkdir(parents=True, exist_ok=True)

        print(f"\n=== {module}  ->  {java_class}.java ===", flush=True)
        per_config: dict[str, str] = {"java_class": java_class, "python_source": str(py_path.relative_to(REPO_ROOT))}

        for config in args.configs:
            print(f"  [{config}]", flush=True)
            if config in ("doc_only", "doc_source") and doc is None:
                per_config[config] = "skipped_no_doc"
                continue
            prompt = build_prompt(config, source, doc, stub, java_class)
            (mod_dir / f"prompt_{config}.txt").write_text(
                f"SYSTEM:\n{SYSTEM_MESSAGE}\n\nUSER:\n{prompt}"
            )
            if args.dry_run:
                per_config[config] = "dry_run"
                continue
            code, ok = generate_with_retry(llm, prompt, args.temperatures)
            if ok:
                (mod_dir / f"{config}.java").write_text(code)
                per_config[config] = "success"
                print(f"    -> {config}.java ({len(code)} chars)", flush=True)
            else:
                per_config[config] = "fail"
                print("    FAILED after all retries", flush=True)

        summary[module] = per_config

    (out_dir / "summary.json").write_text(json.dumps(summary, indent=2))
    print(f"\nSummary -> {out_dir / 'summary.json'}", flush=True)


if __name__ == "__main__":
    main()
