"""Configuration, environment loading, LLM initialization, and global state for FRIDAY Ultra."""
from __future__ import annotations

import inspect
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

PACKAGE_DIR = Path(__file__).resolve().parent
REPO_ROOT = PACKAGE_DIR.parent
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))


def _load_env_files() -> None:
    """Load .env using python-dotenv when available, falling back to an internal parser."""
    candidates = [Path.cwd() / ".env", REPO_ROOT / ".env", PACKAGE_DIR / ".env"]
    try:
        from dotenv import load_dotenv  # type: ignore

        for env_file in candidates:
            if env_file.is_file():
                load_dotenv(dotenv_path=env_file, override=False)
        return
    except ImportError:
        pass

    for env_file in candidates:
        if not env_file.is_file():
            continue
        for raw_line in env_file.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, val = line.partition("=")
            key = key.strip()
            val = val.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = val


_load_env_files()


def _parse_bool(value: str | bool | None, default: bool = False) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on", "y"}


def _parse_int(value: str | int | None, default: int) -> int:
    if isinstance(value, int):
        return value
    if value is None or not str(value).strip():
        return default
    try:
        return int(str(value).strip())
    except ValueError:
        return default


# ====================== EXACT .env CONFIGURATION KEYS ======================
OPENAI_API_KEY: str = os.environ.get("OPENAI_API_KEY", "").strip()
MODEL: str = os.environ.get("MODEL", "gpt-4o").strip() or "gpt-4o"
USER_NAME: str = os.environ.get("USER_NAME", "sir").strip() or "sir"
VOICE_ENABLED: bool = _parse_bool(os.environ.get("VOICE_ENABLED"), default=True)
TTS_VOICE: str = os.environ.get("TTS_VOICE", "en-GB-LibbyNeural").strip() or "en-GB-LibbyNeural"
WHISPER_MODEL: str = os.environ.get("WHISPER_MODEL", "base").strip() or "base"
OPENWEATHER_API_KEY: str = os.environ.get("OPENWEATHER_API_KEY", "").strip()
GMAIL_ADDRESS: str = os.environ.get("GMAIL_ADDRESS", "").strip()
GMAIL_APP_PASSWORD: str = os.environ.get("GMAIL_APP_PASSWORD", "").strip()
TELEGRAM_BOT_TOKEN: str = os.environ.get("TELEGRAM_BOT_TOKEN", "").strip()
TELEGRAM_CHAT_ID: str = os.environ.get("TELEGRAM_CHAT_ID", "").strip()
OLLAMA_MODEL: str = os.environ.get("OLLAMA_MODEL", "llama3.1").strip() or "llama3.1"
USE_LOCAL_LLM: bool = _parse_bool(os.environ.get("USE_LOCAL_LLM"), default=False)
GRADIO_PORT: int = _parse_int(os.environ.get("GRADIO_PORT"), default=7860)


def get_data_dir() -> Path:
    """Resolve and initialize FRIDAY's persistent runtime storage directory."""
    override = os.environ.get("FRIDAY_DATA_DIR", "").strip()
    base = Path(override).expanduser().resolve() if override else (PACKAGE_DIR / "data")
    for subdir in ("chroma_db", "screenshots", "logs", "calendar"):
        (base / subdir).mkdir(parents=True, exist_ok=True)
    return base


DATA_DIR: Path = get_data_dir()
PLUGINS_DIR: Path = PACKAGE_DIR / "plugins"
PLUGINS_DIR.mkdir(parents=True, exist_ok=True)


@dataclass
class RuntimeState:
    """Mutable runtime state shared safely across CLI, Web Dashboard, Voice, and Daemons."""

    user_name: str = USER_NAME
    voice_enabled: bool = VOICE_ENABLED
    use_local_llm: bool = USE_LOCAL_LLM
    active_thread_id: str = "main"
    gradio_url: str = ""
    telegram_running: bool = False


STATE = RuntimeState()


# ====================== UNIVERSAL @tool COMPATIBILITY ======================
try:
    from langchain_core.tools import BaseTool as LCBaseTool  # type: ignore
    from langchain_core.tools import tool as lc_tool  # type: ignore

    LANGCHAIN_TOOLS_AVAILABLE = True
except ImportError:
    LCBaseTool = None  # type: ignore
    lc_tool = None  # type: ignore
    LANGCHAIN_TOOLS_AVAILABLE = False


class FallbackStructuredTool:
    """Lightweight LangChain-compatible tool wrapper when langchain-core is not installed."""

    def __init__(self, func: Callable[..., Any]) -> None:
        self.func = func
        self.name: str = func.__name__
        self.description: str = (inspect.getdoc(func) or self.name).strip()
        self._signature = inspect.signature(func)

    @property
    def args(self) -> dict[str, dict[str, Any]]:
        schema: dict[str, dict[str, Any]] = {}
        for pname, param in self._signature.parameters.items():
            ptype = "string"
            if param.annotation in (int, "int"):
                ptype = "integer"
            elif param.annotation in (float, "float"):
                ptype = "number"
            elif param.annotation in (bool, "bool"):
                ptype = "boolean"
            entry: dict[str, Any] = {"type": ptype}
            if param.default is not inspect.Parameter.empty:
                entry["default"] = param.default
            schema[pname] = entry
        return schema

    def invoke(self, tool_input: dict[str, Any] | str | None = None, **kwargs: Any) -> Any:
        if isinstance(tool_input, dict):
            merged = {**tool_input, **kwargs}
            return self.func(**merged)
        if isinstance(tool_input, str):
            params = list(self._signature.parameters.keys())
            if params:
                return self.func(**{params[0]: tool_input, **kwargs})
        return self.func(**kwargs)

    def run(self, tool_input: dict[str, Any] | str | None = None, **kwargs: Any) -> Any:
        return self.invoke(tool_input, **kwargs)

    def __call__(self, *args: Any, **kwargs: Any) -> Any:
        return self.func(*args, **kwargs)


def friday_tool(func: Callable[..., Any]) -> Any:
    """Decorate a skill function as a native LangChain @tool (or compatible fallback)."""
    if LANGCHAIN_TOOLS_AVAILABLE and lc_tool is not None:
        return lc_tool(func)
    return FallbackStructuredTool(func)


def invoke_tool(tool_obj: Any, **kwargs: Any) -> Any:
    """Uniformly invoke a tool object from Python code regardless of wrapper implementation."""
    if hasattr(tool_obj, "invoke"):
        return tool_obj.invoke(kwargs)
    if hasattr(tool_obj, "func"):
        return tool_obj.func(**kwargs)
    return tool_obj(**kwargs)


def is_tool_object(obj: Any) -> bool:
    """Return True if obj is a LangChain BaseTool or FallbackStructuredTool."""
    if isinstance(obj, FallbackStructuredTool):
        return True
    if LCBaseTool is not None and isinstance(obj, LCBaseTool):
        return True
    return bool(hasattr(obj, "name") and hasattr(obj, "description") and hasattr(obj, "invoke"))


# ====================== LLM INITIALIZATION (OPENAI + OLLAMA) ======================
class UnconfiguredLLM:
    """Diagnostic fallback LLM used when neither OPENAI_API_KEY nor local Ollama is active."""

    def __init__(self, reason: str) -> None:
        self.reason = reason
        self._bound_tools: list[Any] = []

    def bind_tools(self, tools: list[Any]) -> "UnconfiguredLLM":
        clone = UnconfiguredLLM(self.reason)
        clone._bound_tools = list(tools)
        return clone

    def invoke(self, messages: Any, **_: Any) -> Any:
        text = (
            f"My primary neural uplink isn't configured yet, {STATE.user_name}. "
            f"({self.reason}) Populate OPENAI_API_KEY in .env or set USE_LOCAL_LLM=true "
            f"with Ollama running '{OLLAMA_MODEL}', and I'll be at full tactical capacity."
        )
        try:
            from langchain_core.messages import AIMessage  # type: ignore

            return AIMessage(content=text)
        except ImportError:
            return {"role": "assistant", "content": text}


def build_llm(temperature: float = 0.2) -> Any:
    """Instantiate FRIDAY's primary chat model (OpenAI GPT-4o or Local Ollama)."""
    use_local = _parse_bool(os.environ.get("USE_LOCAL_LLM"), default=USE_LOCAL_LLM)
    if use_local:
        model_name = os.environ.get("OLLAMA_MODEL", OLLAMA_MODEL).strip() or OLLAMA_MODEL
        try:
            from langchain_ollama import ChatOllama  # type: ignore

            return ChatOllama(model=model_name, temperature=temperature)
        except ImportError:
            pass
        try:
            from langchain_community.chat_models import ChatOllama as CommunityChatOllama  # type: ignore

            return CommunityChatOllama(model=model_name, temperature=temperature)
        except ImportError:
            return UnconfiguredLLM(f"USE_LOCAL_LLM=true but langchain-community/ollama packages are missing for {model_name}")

    api_key = os.environ.get("OPENAI_API_KEY", OPENAI_API_KEY).strip()
    model_name = os.environ.get("MODEL", MODEL).strip() or MODEL
    if not api_key:
        return UnconfiguredLLM("OPENAI_API_KEY is empty")

    try:
        from langchain_openai import ChatOpenAI  # type: ignore

        return ChatOpenAI(model=model_name, api_key=api_key, temperature=temperature)
    except ImportError:
        return UnconfiguredLLM("langchain-openai is not installed (run: pip install -r requirements.txt)")


llm: Any = build_llm()
