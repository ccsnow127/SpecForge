"""
Documentation and code generation primitives.
Three generators: BaselineDocGenerator (seed docs from source), EntityDocGenerator (per-entity hypothesis + refinement), CodeGenerator (doc to code). Prompts load from `prompts/*.txt`.
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass
from pathlib import Path

logger = logging.getLogger(__name__)

_PROMPTS_DIR = Path(__file__).resolve().parents[2] / "prompts"


def _load_prompt(name: str) -> str:
    return (_PROMPTS_DIR / f"{name}.txt").read_text()


HYPOTHESIS_GENERATION_PROMPT = _load_prompt("diagnosis")
DOC_REFINEMENT_PROMPT = _load_prompt("prescription")
CODE_GENERATION_PROMPT = _load_prompt("codegen")
BASELINE_DOC_PROMPT = _load_prompt("baseline_doc")
BASELINE_DOC_SYSTEM_MESSAGE = _load_prompt("baseline_doc_system")
PYTHON_BASELINE_EXAMPLE = _load_prompt("baseline_example_python")


@dataclass
class EntityDocResult:
    entity: str
    documentation: str
    prompt: str = ""


class EntityDocGenerator:
    def __init__(self, llm_client):
        self.llm = llm_client

    def generate(
        self,
        entity: str,
        entity_source_code: str,
        original_entity_doc: str,
        errors: list[str],
        hypothesis: str,
        temperature: float = 0.7,
    ) -> EntityDocResult:
        error_str = "\n".join(errors[:10]) if errors else "(no errors)"
        prompt = DOC_REFINEMENT_PROMPT.format(
            entity=entity,
            source_code=entity_source_code,
            current_documentation=original_entity_doc or "(no documentation)",
            hypothesis=hypothesis,
            error_messages=error_str,
        )
        response = self.llm.generate(prompt, temperature=temperature)
        return EntityDocResult(
            entity=entity,
            documentation=self._parse_response(response),
            prompt=prompt,
        )

    def generate_hypotheses(
        self,
        entity: str,
        entity_source_code: str,
        entity_generated_code: str,
        original_entity_doc: str,
        errors: list[str],
        num_hypotheses: int,
    ) -> tuple[list[str], str, str]:
        error_str = "\n".join(errors[:10]) if errors else "(no errors)"
        prompt = HYPOTHESIS_GENERATION_PROMPT.format(
            entity=entity,
            source_code=entity_source_code,
            generated_code=entity_generated_code,
            current_documentation=original_entity_doc or "(no documentation)",
            error_messages=error_str,
            num_hypotheses=num_hypotheses,
        )
        response = self.llm.generate(prompt, temperature=self.llm.default_temperature)
        return self._parse_hypotheses(response), prompt, response

    def _parse_hypotheses(self, response: str) -> list[str]:
        matches = re.findall(r'<hypothesis[^>]*>(.*?)</hypothesis>', response, re.DOTALL)
        return [m.strip() for m in matches if m.strip()]

    def _parse_response(self, response: str) -> str:
        m = re.search(r'<documentation>(.*?)</documentation>', response, re.DOTALL)
        if m:
            doc = m.group(1).strip()
        else:
            m = re.search(r'<documentation>(.*)', response, re.DOTALL)
            doc = m.group(1).strip() if m else response.strip()
        doc = re.sub(r'^```\w*\n?', '', doc)
        doc = re.sub(r'\n?```\s*$', '', doc)
        return doc.strip()


class BaselineDocGenerator:
    def __init__(self, llm_client):
        self.llm_client = llm_client

    def generate(self, source_code: str, temperature: float = 0) -> tuple[str, str]:
        prompt = BASELINE_DOC_PROMPT.format(
            source_code=source_code,
            language="python",
            example_format=PYTHON_BASELINE_EXAMPLE,
        )
        doc = self.llm_client.generate(
            prompt,
            temperature=temperature,
            system=BASELINE_DOC_SYSTEM_MESSAGE,
        )
        return doc, prompt


class CodeGenerator:
    def __init__(self, llm_client):
        self.llm = llm_client

    def generate(
        self,
        documentation: str,
        hints: list[dict] | None = None,
        imports: str = "",
        max_retries: int = 3,
    ) -> tuple[str, dict]:
        prompt_dict = None
        for attempt in range(max_retries):
            temperature = 0.0 if attempt == 0 else 0.2 + (attempt * 0.1)
            code, prompt_dict = self._generate_once(
                documentation, hints=hints, imports=imports, temperature=temperature,
            )
            try:
                compile(code, '<generated>', 'exec')
            except SyntaxError as e:
                logger.warning(f"Syntax error on attempt {attempt + 1}/{max_retries}: {e}")
                if attempt == max_retries - 1:
                    raise RuntimeError(
                        f"Failed to generate syntactically valid code after {max_retries} attempts. "
                        f"Last error: {e}"
                    )
                continue
            return code, prompt_dict
        return code, prompt_dict

    def validate_doc_format(self, doc: str) -> tuple[bool, str]:
        heading_count = sum(1 for l in doc.split('\n') if re.match(r'^#{2,4}\s+', l))
        if heading_count < 2:
            return False, (
                "Documentation format violation: found only "
                f"{heading_count} markdown heading(s). Required: "
                "'## Class: Name', '### Method: Class.method', '## Function: name'."
            )
        return True, ""

    def _generate_once(
        self,
        documentation: str,
        hints: list[dict] | None = None,
        imports: str = "",
        temperature: float = 0.0,
    ) -> tuple[str, dict]:
        user_message = CODE_GENERATION_PROMPT.format(
            documentation=documentation, imports=imports,
        )
        response = self.llm.generate(user_message)
        return self._extract_code(response), {"user": user_message}

    def _extract_code(self, response: str) -> str:
        pattern = r'```(?:python)?\s*\n?(.*?)\n?```'
        matches = re.findall(pattern, response, re.DOTALL)
        if matches:
            return "\n\n".join(matches).strip()
        return response.strip()
