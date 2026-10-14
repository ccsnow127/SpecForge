"""
LLM client wrappers for OpenAI, Anthropic, DeepSeek, and Gemini.
Exposes a uniform interface (`generate`, `generate_with_tools`) across providers.
"""

import os
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Optional
import json

from openai import OpenAI

try:
    import anthropic
    ANTHROPIC_AVAILABLE = True
except ImportError:
    ANTHROPIC_AVAILABLE = False


@dataclass
class ToolCall:
    id: str
    name: str
    arguments: dict


@dataclass
class LLMResponse:
    content: str
    tool_calls: list[ToolCall] = field(default_factory=list)
    raw_message: Optional[dict] = None


class LLMClient(ABC):
    def __init__(self, model: str):
        self.model = model
        self.total_input_tokens: int = 0
        self.total_output_tokens: int = 0
        self.total_api_calls: int = 0
        self.default_temperature: float = 0.7

    def get_token_stats(self) -> dict:
        return {
            "total_input_tokens": self.total_input_tokens,
            "total_output_tokens": self.total_output_tokens,
            "total_api_calls": self.total_api_calls,
        }

    @abstractmethod
    def generate(self, prompt: str, temperature: float = 0, system: str = None) -> str:
        ...

    @abstractmethod
    def generate_with_tools(
        self,
        messages: list[dict],
        tools: list[dict],
        temperature: float = 0,
    ) -> LLMResponse:
        ...


class OpenAIClient(LLMClient):
    def __init__(
        self,
        model: str = "gpt-4o-mini",
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
    ):
        super().__init__(model)
        self.client = OpenAI(
            api_key=api_key or os.environ.get("OPENAI_API_KEY"),
            base_url=base_url or os.environ.get("OPENAI_BASE_URL"),
        )
        self._is_qwen = model.startswith("qwen")
        self._fixed_temperature = model.startswith("gpt-5")

    def _extra_kwargs(self) -> dict:
        if self._is_qwen:
            return {"extra_body": {"enable_thinking": False}}
        return {}

    def generate(self, prompt: str, temperature: float = 0, system: str = None) -> str:
        messages = []
        if system:
            messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})

        kwargs = {"model": self.model, "messages": messages, **self._extra_kwargs()}
        if not self._fixed_temperature:
            kwargs["temperature"] = temperature
        response = self.client.chat.completions.create(**kwargs)
        if response.usage:
            self.total_input_tokens += response.usage.prompt_tokens
            self.total_output_tokens += response.usage.completion_tokens
            self.total_api_calls += 1
        return response.choices[0].message.content or ""

    def generate_with_tools(self, messages, tools, temperature=0) -> LLMResponse:
        openai_tools = [{"type": "function", "function": tool} for tool in tools]

        kwargs = {
            "model": self.model,
            "messages": messages,
            "tools": openai_tools if openai_tools else None,
            **self._extra_kwargs(),
        }
        if not self._fixed_temperature:
            kwargs["temperature"] = temperature
        response = self.client.chat.completions.create(**kwargs)
        if response.usage:
            self.total_input_tokens += response.usage.prompt_tokens
            self.total_output_tokens += response.usage.completion_tokens
            self.total_api_calls += 1

        message = response.choices[0].message
        content = message.content or ""

        tool_calls = []
        if message.tool_calls:
            for tc in message.tool_calls:
                try:
                    args = json.loads(tc.function.arguments)
                except json.JSONDecodeError:
                    args = {"raw": tc.function.arguments}
                tool_calls.append(ToolCall(id=tc.id, name=tc.function.name, arguments=args))

        return LLMResponse(
            content=content,
            tool_calls=tool_calls,
            raw_message=message.model_dump() if hasattr(message, 'model_dump') else None,
        )


class AnthropicClient(LLMClient):
    def __init__(self, model: str = "claude-3-5-sonnet-20241022", api_key: Optional[str] = None):
        super().__init__(model)
        if not ANTHROPIC_AVAILABLE:
            raise ImportError(
                "anthropic package is required for Claude models. "
                "Install it with: pip install anthropic"
            )
        self.client = anthropic.Anthropic(api_key=api_key or os.environ.get("ANTHROPIC_API_KEY"))

    def generate(self, prompt: str, temperature: float = 0, system: str = None) -> str:
        messages = [{"role": "user", "content": prompt}]
        kwargs = {
            "model": self.model,
            "max_tokens": 8192,
            "messages": messages,
            "temperature": temperature,
        }
        if system:
            kwargs["system"] = system
        response = self.client.messages.create(**kwargs)
        if response.usage:
            self.total_input_tokens += response.usage.input_tokens
            self.total_output_tokens += response.usage.output_tokens
            self.total_api_calls += 1
        return response.content[0].text if response.content else ""

    def generate_with_tools(self, messages, tools, temperature=0) -> LLMResponse:
        anthropic_tools = [
            {
                "name": tool.get("name"),
                "description": tool.get("description", ""),
                "input_schema": tool.get("parameters", {}),
            }
            for tool in tools
        ]

        system = None
        filtered_messages = []
        for msg in messages:
            if msg.get("role") == "system":
                system = msg.get("content")
            else:
                filtered_messages.append(msg)

        kwargs = {
            "model": self.model,
            "max_tokens": 8192,
            "messages": filtered_messages,
            "temperature": temperature,
        }
        if system:
            kwargs["system"] = system
        if anthropic_tools:
            kwargs["tools"] = anthropic_tools

        response = self.client.messages.create(**kwargs)
        if response.usage:
            self.total_input_tokens += response.usage.input_tokens
            self.total_output_tokens += response.usage.output_tokens
            self.total_api_calls += 1

        content = ""
        tool_calls = []
        for block in response.content:
            if block.type == "text":
                content += block.text
            elif block.type == "tool_use":
                tool_calls.append(ToolCall(
                    id=block.id,
                    name=block.name,
                    arguments=block.input if isinstance(block.input, dict) else {},
                ))

        return LLMResponse(
            content=content,
            tool_calls=tool_calls,
            raw_message={"content": [b.model_dump() for b in response.content]},
        )


class DeepSeekClient(LLMClient):
    DEEPSEEK_BASE_URL = "https://api.deepseek.com"

    def __init__(self, model: str = "deepseek-chat", api_key: Optional[str] = None):
        super().__init__(model)
        self.client = OpenAI(
            api_key=api_key or os.environ.get("DEEPSEEK_API_KEY"),
            base_url=self.DEEPSEEK_BASE_URL,
        )

    def generate(self, prompt: str, temperature: float = 0, system: str = None) -> str:
        messages = []
        if system:
            messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})

        response = self.client.chat.completions.create(
            model=self.model, messages=messages, temperature=temperature,
        )
        if response.usage:
            self.total_input_tokens += response.usage.prompt_tokens
            self.total_output_tokens += response.usage.completion_tokens
            self.total_api_calls += 1
        return response.choices[0].message.content or ""

    def generate_with_tools(self, messages, tools, temperature=0) -> LLMResponse:
        openai_tools = [{"type": "function", "function": tool} for tool in tools]

        response = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=openai_tools if openai_tools else None,
            temperature=temperature,
        )
        if response.usage:
            self.total_input_tokens += response.usage.prompt_tokens
            self.total_output_tokens += response.usage.completion_tokens
            self.total_api_calls += 1

        message = response.choices[0].message
        content = message.content or ""

        tool_calls = []
        if message.tool_calls:
            for tc in message.tool_calls:
                try:
                    args = json.loads(tc.function.arguments)
                except json.JSONDecodeError:
                    args = {"raw": tc.function.arguments}
                tool_calls.append(ToolCall(id=tc.id, name=tc.function.name, arguments=args))

        return LLMResponse(
            content=content,
            tool_calls=tool_calls,
            raw_message=message.model_dump() if hasattr(message, 'model_dump') else None,
        )


class GeminiClient(LLMClient):
    GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"

    def __init__(self, model: str = "gemini-2.0-flash", api_key: Optional[str] = None):
        super().__init__(model)
        api_key = api_key or os.environ.get("GOOGLE_API_KEY") or os.environ.get("GEMINI_API_KEY")
        self.client = OpenAI(api_key=api_key, base_url=self.GEMINI_BASE_URL)

    def generate(self, prompt: str, temperature: float = 0, system: str = None) -> str:
        messages = []
        if system:
            messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})

        response = self.client.chat.completions.create(
            model=self.model, messages=messages, temperature=temperature,
        )
        if response.usage:
            self.total_input_tokens += response.usage.prompt_tokens
            self.total_output_tokens += response.usage.completion_tokens
            self.total_api_calls += 1
        return response.choices[0].message.content or ""

    def generate_with_tools(self, messages, tools, temperature=0) -> LLMResponse:
        openai_tools = [{"type": "function", "function": tool} for tool in tools]

        response = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=openai_tools if openai_tools else None,
            temperature=temperature,
        )
        if response.usage:
            self.total_input_tokens += response.usage.prompt_tokens
            self.total_output_tokens += response.usage.completion_tokens
            self.total_api_calls += 1

        message = response.choices[0].message
        content = message.content or ""

        tool_calls = []
        if message.tool_calls:
            for tc in message.tool_calls:
                try:
                    args = json.loads(tc.function.arguments)
                except json.JSONDecodeError:
                    args = {"raw": tc.function.arguments}
                tool_calls.append(ToolCall(id=tc.id, name=tc.function.name, arguments=args))

        return LLMResponse(
            content=content,
            tool_calls=tool_calls,
            raw_message=message.model_dump() if hasattr(message, 'model_dump') else None,
        )


MODEL_PROVIDERS = {
    "gpt-4o": OpenAIClient, "gpt-4o-mini": OpenAIClient, "gpt-4-turbo": OpenAIClient,
    "gpt-4": OpenAIClient, "gpt-3.5-turbo": OpenAIClient,
    "claude-sonnet-4-20250514": AnthropicClient, "claude-4-sonnet": AnthropicClient,
    "claude-3-5-sonnet-20241022": AnthropicClient, "claude-3-5-sonnet": AnthropicClient,
    "claude-3-opus-20240229": AnthropicClient, "claude-3-sonnet-20240229": AnthropicClient,
    "claude-3-haiku-20240307": AnthropicClient,
    "deepseek-chat": DeepSeekClient, "deepseek-coder": DeepSeekClient, "deepseek-v2": DeepSeekClient,
    "gemini-2.0-flash": GeminiClient, "gemini-2.0-flash-exp": GeminiClient,
    "gemini-1.5-pro": GeminiClient, "gemini-1.5-flash": GeminiClient,
}


def create_llm_client(model: str, api_key: Optional[str] = None) -> LLMClient:
    model_lower = model.lower()
    if model_lower in ("claude-3-5-sonnet", "claude-3.5-sonnet"):
        model = "claude-3-5-sonnet-20241022"
    elif model_lower in ("deepseek-v2", "deepseek"):
        model = "deepseek-chat"

    if model in MODEL_PROVIDERS:
        return MODEL_PROVIDERS[model](model=model, api_key=api_key)

    if model.startswith("gpt-") or model.startswith("o1"):
        return OpenAIClient(model=model, api_key=api_key)
    if model.startswith("claude-"):
        return AnthropicClient(model=model, api_key=api_key)
    if model.startswith("deepseek"):
        return DeepSeekClient(model=model, api_key=api_key)
    if model.startswith("gemini-"):
        return GeminiClient(model=model, api_key=api_key)
    if model.startswith("qwen"):
        return OpenAIClient(
            model=model,
            api_key=api_key or os.environ.get("DASHSCOPE_API_KEY"),
            base_url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
        )

    raise ValueError(
        f"Unknown model: {model}. Supported models: {list(MODEL_PROVIDERS.keys())}. "
        "For custom models, use prefix 'gpt-', 'claude-', 'deepseek-', 'gemini-', or 'qwen'."
    )
