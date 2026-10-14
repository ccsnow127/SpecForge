"""
Text utilities: source / documentation / error parsing.
Pure regex helpers — no LLM calls, no file I/O.
"""

from __future__ import annotations

import re

CRASH_KEYWORDS = [
    "AttributeError", "TypeError", "NameError", "KeyError",
    "IndexError", "SyntaxError", "ValueError", "RuntimeError",
]


def extract_imports(source_code: str) -> str:
    lines: list[str] = []
    in_docstring = False
    for line in source_code.split('\n'):
        stripped = line.strip()
        if '"""' in stripped or "'''" in stripped:
            count = stripped.count('"""') + stripped.count("'''")
            if count % 2 == 1:
                in_docstring = not in_docstring
            continue
        if in_docstring:
            continue
        if not stripped or stripped.startswith('#'):
            continue
        if stripped.startswith('import ') or stripped.startswith('from '):
            lines.append(line)
        elif lines:
            break
    return '\n'.join(lines)


def extract_entity_code(code: str, entity: str) -> str:
    if not code:
        return "(no code generated)"
    parts = entity.split(".")
    if len(parts) == 1:
        cls = _extract_class(code, parts[0])
        if "not found" not in cls:
            return cls
        fn = _extract_function(code, parts[0])
        if "not found" not in fn:
            return fn
        return _extract_global_variable(code, parts[0])
    return _extract_method(code, parts[0], parts[1])


def _extract_class(code: str, class_name: str) -> str:
    pattern = rf'^class\s+{re.escape(class_name)}\b.*?(?=^class\s|^def\s|\Z)'
    m = re.search(pattern, code, re.MULTILINE | re.DOTALL)
    return m.group(0).strip() if m else f"(class {class_name} not found)"


def _extract_function(code: str, func_name: str) -> str:
    pattern = rf'^def\s+{re.escape(func_name)}\s*\([^)]*\).*?(?=^def\s|^class\s|\Z)'
    m = re.search(pattern, code, re.MULTILINE | re.DOTALL)
    return m.group(0).strip() if m else f"(function {func_name} not found)"


def _extract_global_variable(code: str, var_name: str) -> str:
    pattern = rf'^{re.escape(var_name)}\s*=\s*.*?(?=^def\s|^class\s|^[A-Za-z_]\w*\s*=|\Z)'
    m = re.search(pattern, code, re.MULTILINE | re.DOTALL)
    return m.group(0).strip() if m else f"(variable {var_name} not found)"


def _extract_method(code: str, class_name: str, method_name: str) -> str:
    class_code = _extract_class(code, class_name)
    if "not found" in class_code:
        return class_code
    pattern = rf'^\s+def\s+{re.escape(method_name)}\s*\([^)]*\).*?(?=^\s+def\s|\Z)'
    m = re.search(pattern, class_code, re.MULTILINE | re.DOTALL)
    if not m:
        return f"(method {method_name} not found in {class_name})"
    method_text = m.group(0).rstrip()
    preceding = class_code[:m.start()].split('\n')
    decorators = []
    for line in reversed(preceding):
        s = line.strip()
        if s.startswith('@'):
            decorators.append(line)
        elif s == '':
            continue
        else:
            break
    if decorators:
        decorators.reverse()
        method_text = '\n'.join(decorators) + '\n' + method_text
    return method_text


def extract_entity_doc(doc: str, entity: str) -> str:
    if '.' in entity:
        patterns = [rf'(### Method: {re.escape(entity)}(?:\s*\(.*?\))?.*?)(\n### |\n## |\n---|\Z)']
    else:
        patterns = [
            rf'(## Class: {re.escape(entity)}(?![\w.]).*?)(\n## |\n---|\Z)',
            rf'(## Function: {re.escape(entity)}(?![\w.]).*?)(\n## |\n---|\Z)',
            rf'(## Variable: {re.escape(entity)}(?![\w.]).*?)(\n## |\n---|\Z)',
        ]
    for p in patterns:
        m = re.search(p, doc, re.DOTALL)
        if m:
            return m.group(1).strip()
    return f"(no documentation found for {entity})"


def replace_entity_doc(doc: str, entity: str, new_documentation: str) -> str:
    if '.' in entity:
        patterns = [rf'(### Method: {re.escape(entity)}(?:\s*\(.*?\))?.*?)(\n### |\n## |\n---|\Z)']
    else:
        patterns = [
            rf'(## Class: {re.escape(entity)}(?![\w.]).*?)(\n## |\n---|\Z)',
            rf'(## Function: {re.escape(entity)}(?![\w.]).*?)(\n## |\n---|\Z)',
            rf'(## Variable: {re.escape(entity)}(?![\w.]).*?)(\n## |\n---|\Z)',
        ]
    for p in patterns:
        m = re.search(p, doc, re.DOTALL)
        if not m:
            continue
        old_section = m.group(1)
        next_part = m.group(2)
        hdr = re.match(r'(##+ \w+: [^\n]+)', old_section)
        header = hdr.group(1) if hdr else (
            f"### Method: {entity}" if '.' in entity else f"## {entity}"
        )
        stripped_doc = _strip_duplicate_header(new_documentation, entity)
        return doc[:m.start()] + f"{header}\n\n{stripped_doc}\n" + next_part + doc[m.end():]

    stripped_doc = _strip_duplicate_header(new_documentation, entity)
    suffix = f"\n\n### Method: {entity}\n\n{stripped_doc}\n" if '.' in entity else f"\n\n## {entity}\n\n{stripped_doc}\n"
    return doc + suffix


def _strip_duplicate_header(new_documentation: str, entity: str) -> str:
    out = new_documentation
    for hdr_pattern in [
        rf'###\s+Method:\s+{re.escape(entity)}(?:\s*\(.*?\))?[^\n]*\n*',
        rf'##\s+Class:\s+{re.escape(entity)}[^\n]*\n*',
        rf'##\s+Function:\s+{re.escape(entity)}[^\n]*\n*',
    ]:
        out = re.sub(rf'^\s*{hdr_pattern}', '', out, count=1).strip()
    return out


def filter_errors_for_entity(errors: list[str], entity: str) -> list[str]:
    out = []
    for error in errors:
        m = re.search(r'Target:\s*([\w.]+)\s*\|', error)
        if m and m.group(1) == entity:
            out.append(error)
    return out


def get_entities_with_errors(errors: list[str], entities: list[str]) -> list[str]:
    out: set[str] = set()
    for entity in entities:
        if filter_errors_for_entity(errors, entity):
            out.add(entity)
    return list(out)


def count_errors_by_severity(errors: list[str]) -> tuple[int, int]:
    crash_count = sum(1 for e in errors if any(kw in e for kw in CRASH_KEYWORDS))
    return crash_count, len(errors) - crash_count


def get_all_errors_info(errors: list[str]) -> dict:
    if not errors:
        return {"errors": [], "total": 0, "crash_count": 0, "logic_count": 0}

    keywords = CRASH_KEYWORDS + ["AssertionError"]

    def severity(e: str) -> int:
        return 2 if any(kw in e for kw in CRASH_KEYWORDS) else 1

    def error_type(e: str) -> str:
        for kw in keywords:
            if kw in e:
                m = re.search(rf'{kw}[:\s]+(.{{0,50}})', e)
                if m:
                    detail = m.group(1).strip()
                    attr_match = re.search(r"'(\w+)'", detail)
                    if attr_match:
                        return f"{kw}:{attr_match.group(1)}"
                    return f"{kw}:{detail[:20]}"
                return kw
        return "unknown"

    crash_count = logic_count = 0
    out_list = []
    for e in errors:
        sev = severity(e)
        if sev == 2:
            crash_count += 1
        else:
            logic_count += 1
        out_list.append({"type": error_type(e), "severity": sev, "message": e})

    out_list.sort(key=lambda x: x["severity"], reverse=True)
    return {"errors": out_list, "total": len(errors),
            "crash_count": crash_count, "logic_count": logic_count}


def compute_tests_per_entity(entity_map: dict[str, str], entities: list[str]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for entity in entity_map.values():
        if entity in entities:
            counts[entity] = counts.get(entity, 0) + 1
    return counts
