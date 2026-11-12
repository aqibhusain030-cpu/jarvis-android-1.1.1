#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
================================================================================
  S U P E R   F R I D A Y   v3.0  M A X   —   "NOTHING LEFT BEHIND" EDITION
================================================================================
  Female Replacement Intelligent Digital Assistant Youth — Tony Stark's exec AI.

  A single-file, zero-placeholder, production-ready autonomous assistant:
    * LangGraph StateGraph brain (SqliteSaver checkpointer) + MaxFriday orchestrator
    * 7-agent sequential CrewAI swarm (researcher → supervisor)
    * ChromaDB vector memory (+ cosine JSON fallback), notes.json, reminders.json
    * 30+ bound tools: files/PDF/Markdown, browser (Playwright/DDGS/Wikipedia),
      code + gated shell execution, Gmail + Google-Calendar-ready hooks, Jio
      router/network diagnostics, system telemetry, process control, screenshots,
      GPT-4o/Ollama vision, safe AST calculator, weather, stocks, translation
    * Voice I/O: edge-tts neural TTS, pyttsx3 offline fallback, Whisper STT
    * UI: Rich CLI REPL, Gradio web dashboard (0.0.0.0), Telegram bot + alerts
    * Background ReminderDaemon, security audit log, drop-in @tool plugins

  Usage:
      python3 super_friday_max.py                  # CLI REPL
      python3 super_friday_max.py --web            # + Gradio dashboard
      python3 super_friday_max.py --telegram       # + Telegram bot
      python3 super_friday_max.py --crew "goal"    # one-shot swarm mission
      python3 super_friday_max.py --status         # subsystem telemetry

  Configuration: copy .env.example → .env and fill in your keys.
================================================================================
"""
from __future__ import annotations

import argparse
import ast
import asyncio
import base64
import hashlib
import inspect
import json
import math
import mimetypes
import operator
import os
import platform
import queue
import re
import shlex
import shutil
import signal
import socket
import sqlite3
import subprocess
import sys
import tempfile
import threading
import time
import traceback
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
import uuid
import wave
from collections import Counter
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Callable, Iterable

__version__ = "3.0.0"
__edition__ = "SUPER FRIDAY v3.0 MAX — NOTHING LEFT BEHIND EDITION"

SCRIPT_DIR = Path(__file__).resolve().parent

# ==============================================================================
# OPTIONAL DEPENDENCIES — every subsystem degrades gracefully, never fatally.
# ==============================================================================
try:
    import requests  # type: ignore
    REQUESTS_AVAILABLE = True
except ImportError:
    requests = None  # type: ignore
    REQUESTS_AVAILABLE = False

try:
    from bs4 import BeautifulSoup  # type: ignore
    BS4_AVAILABLE = True
except ImportError:
    BeautifulSoup = None  # type: ignore
    BS4_AVAILABLE = False

try:
    import psutil  # type: ignore
    PSUTIL_AVAILABLE = True
except ImportError:
    psutil = None  # type: ignore
    PSUTIL_AVAILABLE = False

try:
    from PIL import Image, ImageGrab  # type: ignore
    PILLOW_AVAILABLE = True
except ImportError:
    Image = None  # type: ignore
    ImageGrab = None  # type: ignore
    PILLOW_AVAILABLE = False

try:
    from pypdf import PdfReader  # type: ignore
    PYPDF_AVAILABLE = True
except ImportError:
    try:
        from PyPDF2 import PdfReader  # type: ignore
        PYPDF_AVAILABLE = True
    except ImportError:
        PdfReader = None  # type: ignore
        PYPDF_AVAILABLE = False

try:
    import wikipedia as wikipedia_api  # type: ignore
    WIKIPEDIA_AVAILABLE = True
except ImportError:
    wikipedia_api = None  # type: ignore
    WIKIPEDIA_AVAILABLE = False

try:
    from duckduckgo_search import DDGS  # type: ignore
    DDGS_AVAILABLE = True
except ImportError:
    try:
        from ddgs import DDGS  # type: ignore
        DDGS_AVAILABLE = True
    except ImportError:
        DDGS = None  # type: ignore
        DDGS_AVAILABLE = False

try:
    from playwright.sync_api import sync_playwright  # type: ignore
    PLAYWRIGHT_AVAILABLE = True
except ImportError:
    sync_playwright = None  # type: ignore
    PLAYWRIGHT_AVAILABLE = False

try:
    from deep_translator import GoogleTranslator  # type: ignore
    TRANSLATOR_AVAILABLE = True
except ImportError:
    GoogleTranslator = None  # type: ignore
    TRANSLATOR_AVAILABLE = False

try:
    import yfinance as yf  # type: ignore
    YFINANCE_AVAILABLE = True
except ImportError:
    yf = None  # type: ignore
    YFINANCE_AVAILABLE = False

try:
    import edge_tts  # type: ignore
    EDGE_TTS_AVAILABLE = True
except ImportError:
    edge_tts = None  # type: ignore
    EDGE_TTS_AVAILABLE = False

try:
    import pyttsx3  # type: ignore
    PYTTSX3_AVAILABLE = True
except ImportError:
    pyttsx3 = None  # type: ignore
    PYTTSX3_AVAILABLE = False

try:
    import sounddevice as sd  # type: ignore
    import numpy as np  # type: ignore
    AUDIO_CAPTURE_AVAILABLE = True
except ImportError:
    sd = None  # type: ignore
    np = None  # type: ignore
    AUDIO_CAPTURE_AVAILABLE = False

try:
    import whisper  # type: ignore
    WHISPER_AVAILABLE = True
except ImportError:
    whisper = None  # type: ignore
    WHISPER_AVAILABLE = False

try:
    import chromadb  # type: ignore
    CHROMA_AVAILABLE = True
except ImportError:
    chromadb = None  # type: ignore
    CHROMA_AVAILABLE = False

try:
    import gradio as gr  # type: ignore
    GRADIO_AVAILABLE = True
except ImportError:
    gr = None  # type: ignore
    GRADIO_AVAILABLE = False

try:
    from telegram import Update  # type: ignore
    from telegram.ext import (
        ApplicationBuilder,
        CommandHandler,
        ContextTypes,
        MessageHandler,
        filters,
    )  # type: ignore
    TELEGRAM_AVAILABLE = True
except ImportError:
    Update = Any  # type: ignore
    ApplicationBuilder = None  # type: ignore
    CommandHandler = None  # type: ignore
    ContextTypes = Any  # type: ignore
    MessageHandler = None  # type: ignore
    filters = None  # type: ignore
    TELEGRAM_AVAILABLE = False

try:
    from crewai import Agent, Crew, Process, Task  # type: ignore
    CREW_AVAILABLE = True
except ImportError:
    Agent = None  # type: ignore
    Crew = None  # type: ignore
    Process = None  # type: ignore
    Task = None  # type: ignore
    CREW_AVAILABLE = False

try:
    from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage, ToolMessage  # type: ignore
    from langchain_core.tools import BaseTool as LCBaseTool  # type: ignore
    from langchain_core.tools import tool as lc_tool  # type: ignore
    LANGCHAIN_CORE_AVAILABLE = True
except ImportError:
    AIMessage = None  # type: ignore
    BaseMessage = Any  # type: ignore
    HumanMessage = None  # type: ignore
    SystemMessage = None  # type: ignore
    ToolMessage = None  # type: ignore
    LCBaseTool = None  # type: ignore
    lc_tool = None  # type: ignore
    LANGCHAIN_CORE_AVAILABLE = False

try:
    from langgraph.graph import END, START, StateGraph  # type: ignore
    from langgraph.graph.message import add_messages  # type: ignore
    from langgraph.prebuilt import ToolNode, tools_condition  # type: ignore
    LANGGRAPH_AVAILABLE = True
except ImportError:
    END = None  # type: ignore
    START = None  # type: ignore
    StateGraph = None  # type: ignore
    add_messages = None  # type: ignore
    ToolNode = None  # type: ignore
    tools_condition = None  # type: ignore
    LANGGRAPH_AVAILABLE = False

try:
    from typing import Annotated, TypedDict
except ImportError:  # pragma: no cover
    from typing_extensions import Annotated, TypedDict  # type: ignore


# ==============================================================================
# 1. CONFIGURATION — .env loader, 14 canonical keys, runtime global state
# ==============================================================================
def _load_env_files() -> None:
    """Load .env via python-dotenv when present, else use a strict internal parser."""
    candidates = [Path.cwd() / ".env", SCRIPT_DIR / ".env", SCRIPT_DIR / "friday_ultra" / ".env"]
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
            if key.startswith("export "):
                key = key[7:].strip()
            val = val.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = val


_load_env_files()


def _env(key: str, default: str = "") -> str:
    return os.environ.get(key, default).strip()


def _env_bool(key: str, default: bool = False) -> bool:
    raw = os.environ.get(key)
    if raw is None or not raw.strip():
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on", "y"}


def _env_int(key: str, default: int) -> int:
    try:
        return int(_env(key) or default)
    except ValueError:
        return default


OPENAI_API_KEY: str = _env("OPENAI_API_KEY")
MODEL: str = _env("MODEL", "gpt-4o") or "gpt-4o"
USER_NAME: str = _env("USER_NAME", "sir") or "sir"
VOICE_ENABLED: bool = _env_bool("VOICE_ENABLED", True)
TTS_VOICE: str = _env("TTS_VOICE", "en-GB-LibbyNeural") or "en-GB-LibbyNeural"
WHISPER_MODEL: str = _env("WHISPER_MODEL", "base") or "base"
OPENWEATHER_API_KEY: str = _env("OPENWEATHER_API_KEY")
GMAIL_ADDRESS: str = _env("GMAIL_ADDRESS")
GMAIL_APP_PASSWORD: str = _env("GMAIL_APP_PASSWORD")
TELEGRAM_BOT_TOKEN: str = _env("TELEGRAM_BOT_TOKEN")
TELEGRAM_CHAT_ID: str = _env("TELEGRAM_CHAT_ID")
OLLAMA_MODEL: str = _env("OLLAMA_MODEL", "llama3.1") or "llama3.1"
USE_LOCAL_LLM: bool = _env_bool("USE_LOCAL_LLM", False)
GRADIO_PORT: int = _env_int("GRADIO_PORT", 7860)


def user_name() -> str:
    """Current form of address — re-read from the environment on every call."""
    return _env("USER_NAME", USER_NAME) or "sir"


def get_data_dir() -> Path:
    """Resolve FRIDAY's persistent runtime storage (honours FRIDAY_DATA_DIR at call time)."""
    override = _env("FRIDAY_DATA_DIR")
    base = Path(override).expanduser().resolve() if override else (SCRIPT_DIR / "friday_ultra" / "data")
    for subdir in ("chroma_db", "screenshots", "logs", "calendar"):
        try:
            (base / subdir).mkdir(parents=True, exist_ok=True)
        except OSError:
            pass
    return base


def plugins_dir() -> Path:
    override = _env("FRIDAY_PLUGINS_DIR")
    return Path(override).expanduser().resolve() if override else (SCRIPT_DIR / "friday_ultra" / "plugins")


class RuntimeState:
    """Mutable runtime state shared across CLI, Web UI, Telegram bot, and daemons."""

    __slots__ = ("user_name", "voice_enabled", "use_local_llm", "active_thread_id", "gradio_url", "telegram_running")

    def __init__(
        self,
        user_name: str = USER_NAME,
        voice_enabled: bool = VOICE_ENABLED,
        use_local_llm: bool = USE_LOCAL_LLM,
        active_thread_id: str = "main",
        gradio_url: str = "",
        telegram_running: bool = False,
    ) -> None:
        self.user_name = user_name
        self.voice_enabled = voice_enabled
        self.use_local_llm = use_local_llm
        self.active_thread_id = active_thread_id
        self.gradio_url = gradio_url
        self.telegram_running = telegram_running

    def as_dict(self) -> dict[str, Any]:
        return {slot: getattr(self, slot) for slot in self.__slots__}

    def __repr__(self) -> str:
        return f"RuntimeState({self.as_dict()})"


STATE = RuntimeState()


# ==============================================================================
# 2. UNIVERSAL @tool WRAPPER (LangChain native, or fully compatible fallback)
# ==============================================================================
class FallbackStructuredTool:
    """Minimal LangChain-compatible tool wrapper used when langchain-core is absent."""

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
            entry: dict[str, Any] = {"type": ptype, "description": pname}
            if param.default is not inspect.Parameter.empty:
                entry["default"] = param.default
            schema[pname] = entry
        return schema

    def invoke(self, tool_input: Any = None, **kwargs: Any) -> Any:
        if isinstance(tool_input, dict):
            return self.func(**{**tool_input, **kwargs})
        if isinstance(tool_input, str):
            params = list(self._signature.parameters.keys())
            if params:
                merged = {params[0]: tool_input}
                merged.update(kwargs)
                return self.func(**merged)
        return self.func(**kwargs)

    def run(self, tool_input: Any = None, **kwargs: Any) -> Any:
        return self.invoke(tool_input, **kwargs)

    def __call__(self, *args: Any, **kwargs: Any) -> Any:
        return self.func(*args, **kwargs)


def friday_tool(func: Callable[..., Any]) -> Any:
    """Decorate a skill function as a native LangChain @tool (or compatible fallback)."""
    if LANGCHAIN_CORE_AVAILABLE and lc_tool is not None:
        try:
            return lc_tool(func)
        except Exception:
            return FallbackStructuredTool(func)
    return FallbackStructuredTool(func)


def invoke_tool(tool_obj: Any, **kwargs: Any) -> Any:
    """Invoke any tool object (native or fallback) uniformly."""
    if hasattr(tool_obj, "invoke"):
        return tool_obj.invoke(kwargs)
    if hasattr(tool_obj, "func"):
        return tool_obj.func(**kwargs)
    return tool_obj(**kwargs)


def is_tool_object(obj: Any) -> bool:
    """Detect LangChain BaseTool / FallbackStructuredTool instances."""
    if isinstance(obj, FallbackStructuredTool):
        return True
    if LCBaseTool is not None and isinstance(obj, LCBaseTool):
        return True
    return bool(hasattr(obj, "name") and hasattr(obj, "description") and hasattr(obj, "invoke"))


# ==============================================================================
# 3. LLM FACTORY — OpenAI GPT-4o primary, Ollama local secondary
# ==============================================================================
class UnconfiguredLLM:
    """Diagnostic stand-in when no model backend is configured; keeps tools usable."""

    def __init__(self, reason: str = "OPENAI_API_KEY is empty") -> None:
        self.reason = reason
        self._bound_tools: list[Any] = []

    def bind_tools(self, tools: Iterable[Any]) -> "UnconfiguredLLM":
        clone = UnconfiguredLLM(self.reason)
        clone._bound_tools = list(tools)
        return clone

    def invoke(self, messages: Any = None, **_: Any) -> Any:
        text = (
            f"My primary neural uplink isn't configured yet, {user_name()}. ({self.reason}) "
            f"Populate OPENAI_API_KEY in .env or set USE_LOCAL_LLM=true with Ollama running "
            f"'{OLLAMA_MODEL}', and I'll be at full tactical capacity. "
            f"All {len(self._bound_tools)} tools remain directly callable in the meantime."
        )
        if LANGCHAIN_CORE_AVAILABLE and AIMessage is not None:
            return AIMessage(content=text)
        return {"role": "assistant", "content": text}


def build_llm(temperature: float = 0.2) -> Any:
    """Instantiate FRIDAY's chat model: local Ollama when USE_LOCAL_LLM, else OpenAI."""
    if _env_bool("USE_LOCAL_LLM", USE_LOCAL_LLM):
        model_name = _env("OLLAMA_MODEL", OLLAMA_MODEL) or OLLAMA_MODEL
        try:
            from langchain_ollama import ChatOllama  # type: ignore

            return ChatOllama(model=model_name, temperature=temperature)
        except ImportError:
            pass
        try:
            from langchain_community.chat_models import ChatOllama as CommunityChatOllama  # type: ignore

            return CommunityChatOllama(model=model_name, temperature=temperature)
        except ImportError:
            return UnconfiguredLLM(f"USE_LOCAL_LLM=true but no Ollama chat model package is installed ({model_name})")

    key = _env("OPENAI_API_KEY", OPENAI_API_KEY)
    model_name = _env("MODEL", MODEL) or MODEL
    if not key:
        return UnconfiguredLLM("OPENAI_API_KEY is empty")
    try:
        from langchain_openai import ChatOpenAI  # type: ignore

        return ChatOpenAI(model=model_name, api_key=key, temperature=temperature)
    except ImportError:
        return UnconfiguredLLM("langchain-openai is not installed (pip install -r requirements.txt)")


llm: Any = build_llm()


# ==============================================================================
# 4. MEMORY ENGINE — ChromaDB vector store, notes.json, reminders.json, SQLite
# ==============================================================================
_MEMORY_LOCK = threading.RLock()
_CHROMA_CLIENTS: dict[str, Any] = {}


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _notes_file() -> Path:
    return get_data_dir() / "notes.json"


def _reminders_file() -> Path:
    return get_data_dir() / "reminders.json"


def _fallback_vectors_file() -> Path:
    return get_data_dir() / "fallback_vectors.json"


def _sqlite_db_path() -> Path:
    return get_data_dir() / "friday_memory.sqlite3"


def _atomic_write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(payload, indent=2, ensure_ascii=False, default=str), encoding="utf-8")
    os.replace(tmp, path)


def _read_json(path: Path, default: Any) -> Any:
    if not path.is_file():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def _get_chroma_collection() -> Any:
    """Return a persistent ChromaDB collection, or None when Chroma is unavailable."""
    if not CHROMA_AVAILABLE or chromadb is None:
        return None
    path = str(get_data_dir() / "chroma_db")
    with _MEMORY_LOCK:
        try:
            client = _CHROMA_CLIENTS.get(path)
            if client is None:
                client = chromadb.PersistentClient(path=path)
                _CHROMA_CLIENTS[path] = client
            return client.get_or_create_collection(name="friday_long_term", metadata={"hnsw:space": "cosine"})
        except Exception:
            return None


_STOPWORDS = {
    "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with", "is", "are",
    "was", "were", "be", "been", "it", "this", "that", "as", "at", "by", "from", "my",
}


def _tokenize(text: str) -> Counter:
    words = re.findall(r"[a-z0-9_]+", (text or "").lower())
    return Counter(w for w in words if w not in _STOPWORDS and len(w) > 1)


def _cosine_counter(a: Counter, b: Counter) -> float:
    if not a or not b:
        return 0.0
    common = set(a) & set(b)
    dot = sum(a[w] * b[w] for w in common)
    if dot == 0:
        return 0.0
    norm_a = math.sqrt(sum(v * v for v in a.values()))
    norm_b = math.sqrt(sum(v * v for v in b.values()))
    return float(dot / (norm_a * norm_b)) if norm_a and norm_b else 0.0


def upsert_vector_memory(doc_id: str, document: str, metadata: dict[str, Any] | None = None) -> None:
    """Persist a document into ChromaDB, falling back to a cosine-indexed JSON store."""
    meta = dict(metadata or {})
    meta.setdefault("timestamp", _utc_now_iso())
    collection = _get_chroma_collection()
    if collection is not None:
        try:
            collection.upsert(ids=[doc_id], documents=[document], metadatas=[meta])
            return
        except Exception:
            pass

    with _MEMORY_LOCK:
        path = _fallback_vectors_file()
        store: list[dict[str, Any]] = _read_json(path, [])
        store = [row for row in store if row.get("id") != doc_id]
        store.append({"id": doc_id, "document": document, "metadata": meta})
        _atomic_write_json(path, store)


def delete_vector_memory(doc_id: str) -> None:
    collection = _get_chroma_collection()
    if collection is not None:
        try:
            collection.delete(ids=[doc_id])
        except Exception:
            pass
    with _MEMORY_LOCK:
        path = _fallback_vectors_file()
        store = _read_json(path, [])
        trimmed = [row for row in store if row.get("id") != doc_id]
        if len(trimmed) != len(store):
            _atomic_write_json(path, trimmed)


def query_vector_memory(query: str, top_k: int = 5) -> list[dict[str, Any]]:
    """Semantic search across ChromaDB or the local cosine store; returns scored hits."""
    limit = max(1, int(top_k))
    collection = _get_chroma_collection()
    if collection is not None:
        try:
            if collection.count() > 0:
                res = collection.query(query_texts=[query], n_results=min(limit, max(1, collection.count())))
                docs = (res.get("documents") or [[]])[0]
                ids = (res.get("ids") or [[]])[0]
                metas = (res.get("metadatas") or [[]])[0]
                dists = (res.get("distances") or [[]])[0]
                hits = []
                for i, doc in enumerate(docs):
                    dist = float(dists[i]) if i < len(dists) else 1.0
                    hits.append(
                        {
                            "id": ids[i] if i < len(ids) else f"chroma_{i}",
                            "document": doc,
                            "metadata": metas[i] if i < len(metas) and metas[i] else {},
                            "score": max(0.0, 1.0 - dist),
                        }
                    )
                return hits
        except Exception:
            pass

    with _MEMORY_LOCK:
        store = _read_json(_fallback_vectors_file(), [])
    q_tokens = _tokenize(query)
    scored = []
    for row in store:
        score = _cosine_counter(q_tokens, _tokenize(str(row.get("document", ""))))
        if score > 0:
            scored.append(
                {
                    "id": row.get("id", ""),
                    "document": row.get("document", ""),
                    "metadata": row.get("metadata", {}),
                    "score": score,
                }
            )
    scored.sort(key=lambda r: r["score"], reverse=True)
    return scored[:limit]


def vector_memory_count() -> int:
    collection = _get_chroma_collection()
    if collection is not None:
        try:
            return int(collection.count())
        except Exception:
            pass
    return len(_read_json(_fallback_vectors_file(), []))


def load_notes() -> dict[str, dict[str, Any]]:
    with _MEMORY_LOCK:
        data = _read_json(_notes_file(), {})
    return data if isinstance(data, dict) else {}


def save_notes(notes: dict[str, dict[str, Any]]) -> None:
    with _MEMORY_LOCK:
        _atomic_write_json(_notes_file(), notes)


def load_reminders() -> list[dict[str, Any]]:
    with _MEMORY_LOCK:
        data = _read_json(_reminders_file(), [])
    return data if isinstance(data, list) else []


def save_reminders(reminders: list[dict[str, Any]]) -> None:
    with _MEMORY_LOCK:
        _atomic_write_json(_reminders_file(), reminders)


def _sqlite_connect() -> sqlite3.Connection:
    conn = sqlite3.connect(str(_sqlite_db_path()), check_same_thread=False, timeout=15)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS conversation (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            thread_id TEXT NOT NULL,
            role TEXT NOT NULL,
            content TEXT NOT NULL,
            created_at TEXT NOT NULL
        )
        """
    )
    conn.execute("CREATE INDEX IF NOT EXISTS idx_thread ON conversation(thread_id)")
    conn.commit()
    return conn


def save_sqlite_turn(thread_id: str, role: str, content: str) -> None:
    with _MEMORY_LOCK:
        try:
            conn = _sqlite_connect()
            try:
                conn.execute(
                    "INSERT INTO conversation (thread_id, role, content, created_at) VALUES (?, ?, ?, ?)",
                    (thread_id, role, content, _utc_now_iso()),
                )
                conn.commit()
            finally:
                conn.close()
        except Exception:
            pass


def load_sqlite_turns(thread_id: str, limit: int = 20) -> list[dict[str, str]]:
    with _MEMORY_LOCK:
        try:
            conn = _sqlite_connect()
            try:
                rows = conn.execute(
                    "SELECT role, content FROM conversation WHERE thread_id = ? ORDER BY id DESC LIMIT ?",
                    (thread_id, max(1, int(limit))),
                ).fetchall()
            finally:
                conn.close()
            return [{"role": r[0], "content": r[1]} for r in reversed(rows)]
        except Exception:
            return []


def clear_sqlite_thread(thread_id: str) -> None:
    with _MEMORY_LOCK:
        try:
            conn = _sqlite_connect()
            try:
                conn.execute("DELETE FROM conversation WHERE thread_id = ?", (thread_id,))
                conn.commit()
            finally:
                conn.close()
        except Exception:
            pass


def get_checkpointer() -> Any:
    """Return a LangGraph SqliteSaver checkpointer (or None if langgraph-sqlite is absent)."""
    try:
        from langgraph.checkpoint.sqlite import SqliteSaver  # type: ignore

        conn = sqlite3.connect(str(_sqlite_db_path()), check_same_thread=False, timeout=15)
        return SqliteSaver(conn)
    except Exception:
        return None


def archive_interaction(user_msg: str, assistant_msg: str, thread_id: str = "main") -> None:
    """Persist both sides of an exchange to SQLite history and the vector store."""
    save_sqlite_turn(thread_id, "user", user_msg)
    save_sqlite_turn(thread_id, "assistant", assistant_msg)
    upsert_vector_memory(
        doc_id=f"turn_{uuid.uuid4().hex[:12]}",
        document=f"User: {user_msg}\nFRIDAY: {assistant_msg}",
        metadata={"thread_id": thread_id, "kind": "conversation"},
    )


@friday_tool
def remember(key: str, value: str = "", category: str = "general") -> str:
    """Persist a durable fact, preference, or project detail into FRIDAY's long-term memory."""
    clean_key = re.sub(r"\s+", "_", (key or "").strip().lower()) or f"note_{uuid.uuid4().hex[:6]}"
    clean_value = (value or "").strip()
    if not clean_value:
        return f"Provide a value to remember for '{clean_key}', {user_name()}."

    payload = load_notes()
    payload[clean_key] = {"value": clean_value, "category": category or "general", "timestamp": _utc_now_iso()}
    save_notes(payload)
    upsert_vector_memory(
        doc_id=f"note_{clean_key}",
        document=f"{clean_key}: {clean_value} (category: {category or 'general'})",
        metadata={"key": clean_key, "category": category or "general", "kind": "note"},
    )
    return f"Committed to long-term memory, {user_name()}: {clean_key} → {clean_value}"


@friday_tool
def recall(query: str = "") -> str:
    """Recall stored facts by exact key or fuzzy match across FRIDAY's notes."""
    needle = (query or "").strip().lower()
    notes = load_notes()
    if not notes:
        return f"Long-term memory is empty, {user_name()}."
    if not needle:
        lines = [f"Long-term memory ({len(notes)} entries):"]
        for k, v in sorted(notes.items()):
            lines.append(f"- {k}: {v.get('value')} [{v.get('category')}]")
        return "\n".join(lines)

    exact_key = re.sub(r"\s+", "_", needle)
    exact = notes.get(exact_key)
    if exact:
        return f"{exact_key}: {exact.get('value')} (category: {exact.get('category')})"

    hits = [(k, v) for k, v in notes.items() if needle in k.lower() or needle in str(v.get("value", "")).lower()]
    if hits:
        lines = [f"Matches for '{query}':"]
        for k, v in hits[:12]:
            lines.append(f"- {k}: {v.get('value')} [{v.get('category')}]")
        return "\n".join(lines)

    semantic = query_vector_memory(query=needle, top_k=3)
    if semantic:
        lines = [f"No exact key; nearest semantic memories for '{query}':"]
        for hit in semantic:
            lines.append(f"- ({hit['score']:.2f}) {hit['document'][:220]}")
        return "\n".join(lines)
    return f"Nothing on '{query}' in long-term memory, {user_name()}."


@friday_tool
def forget(key: str) -> str:
    """Delete a stored fact from notes.json and the vector store."""
    clean_key = re.sub(r"\s+", "_", (key or "").strip().lower())
    notes = load_notes()
    if clean_key not in notes:
        matches = [k for k in notes if clean_key and clean_key in k]
        if not matches:
            return f"No memory key matching '{key}' to remove, {user_name()}."
        clean_key = matches[0]
    removed = notes.pop(clean_key, None)
    save_notes(notes)
    delete_vector_memory(f"note_{clean_key}")
    return f"Removed '{clean_key}' from long-term memory, {user_name()}. ({str(removed.get('value'))[:80] if removed else ''})"


@friday_tool
def search_long_term_memory(query: str, top_k: int = 5) -> str:
    """Semantic search across every archived conversation turn and stored note."""
    q = (query or "").strip()
    if not q:
        return "Provide a search query for long-term memory."
    hits = query_vector_memory(query=q, top_k=top_k)
    if not hits:
        semantic_notes = [
            (k, v) for k, v in load_notes().items() if any(tok in k for tok in _tokenize(query))
        ]
        if semantic_notes:
            return "\n".join([f"- {k}: {v.get('value')}" for k, v in semantic_notes[:top_k]])
        return f"No long-term memory matches '{query}', {user_name()}."

    lines = [f"Long-term memory results for '{query}':"]
    for hit in hits:
        meta = hit.get("metadata") or {}
        lines.append(f"- ({hit['score']:.2f}) [{meta.get('kind', 'memory')}] {hit['document'][:400]}")
    return "\n".join(lines)


MEMORY_TOOLS = [remember, recall, forget, search_long_term_memory]

# ==============================================================================
# 5. SECURITY — confirmation gatekeeper, catastrophic blocklist, JSONL audit log
# ==============================================================================
_AUDIT_LOCK = threading.RLock()

CATASTROPHIC_PATTERNS: tuple[re.Pattern, ...] = (
    re.compile(r"rm\s+-[a-zA-Z]*r[a-zA-Z]*f?\s+/(?:\s|$)"),
    re.compile(r"rm\s+-[a-zA-Z]*f[a-zA-Z]*r?\s+/(?:\s|$)"),
    re.compile(r"rm\s+-rf\s+(?:/\*|~|\$HOME)(?:\s|$)"),
    re.compile(r"mkfs(?:\.\w+)?\s+"),
    re.compile(r"dd\s+if=.*\s+of=/dev/(?:sd|nvme|disk)"),
    re.compile(r":\(\)\s*\{\s*:\|:&\s*\}\s*;\s*:"),
    re.compile(r">\s*/dev/sd[a-z]"),
    re.compile(r"chmod\s+-R\s+777\s+/(?:\s|$)"),
    re.compile(r"shutdown\s+-[hr]|reboot\s+-f|halt\s+-f"),
    re.compile(r"(?:curl|wget)[^|]*\|\s*(?:sudo\s+)?(?:ba)?sh"),
    re.compile(r"format\s+[a-zA-Z]:"),
)


def _audit_log_path() -> Path:
    return get_data_dir() / "logs" / "security_audit.jsonl"


def log_security_event(action: str, target: str, decision: str, detail: str = "") -> None:
    """Append a tamper-evident JSONL record of every gated action."""
    record = {
        "timestamp": _utc_now_iso(),
        "action": action,
        "target": target,
        "decision": decision,
        "detail": detail,
        "user": user_name(),
    }
    with _AUDIT_LOCK:
        try:
            path = _audit_log_path()
            path.parent.mkdir(parents=True, exist_ok=True)
            with path.open("a", encoding="utf-8") as fh:
                fh.write(json.dumps(record, ensure_ascii=False) + "\n")
        except OSError:
            pass


def is_catastrophic_command(command: str) -> bool:
    low = (command or "").strip().lower()
    return any(p.search(low) for p in CATASTROPHIC_PATTERNS)


def verify_confirmation(action: str, target: str, confirm: str = "no") -> tuple[bool, str]:
    """Gate destructive operations on an explicit confirm='yes' and audit every decision."""
    approved = (confirm or "").strip().lower() in {"yes", "y", "true", "1", "confirm", "confirmed"}
    if is_catastrophic_command(target):
        log_security_event(action, target, "BLOCKED_CATASTROPHIC", "Signatures matched the doomsday blocklist.")
        return (
            False,
            f"BLOCKED, {user_name()}. That command matches a catastrophic-destruction signature and "
            f"will not run under any confirmation. Nice try.",
        )
    if not approved:
        log_security_event(action, target, "DRY_RUN", "Awaiting explicit confirm='yes'.")
        return (
            False,
            f"DRY RUN — {action} on '{target}' not executed. Re-issue with confirm=\"yes\" to authorise, "
            f"{user_name()}. I don't break things without a written order.",
        )
    log_security_event(action, target, "APPROVED", "Operator confirmed with confirm='yes'.")
    return True, f"Authorised: {action} → {target}."


@friday_tool
def get_security_audit_log(lines: int = 20) -> str:
    """Show the most recent entries from the security audit log."""
    path = _audit_log_path()
    if not path.is_file():
        return "Security audit log is empty — no gated actions attempted yet."
    with _AUDIT_LOCK:
        try:
            raw = path.read_text(encoding="utf-8").splitlines()
        except OSError as exc:
            return f"[Audit Error] {exc}"
    tail = raw[-max(1, int(lines)) :]
    if not tail:
        return "Security audit log is empty."
    return "Security Audit Log (most recent last):\n" + "\n".join(tail)


SECURITY_TOOLS = [get_security_audit_log]


# ==============================================================================
# 6. VOICE ENGINE — edge-tts neural British voice, pyttsx3 offline, Whisper STT
# ==============================================================================
def clean_text_for_speech(text: str) -> str:
    """Strip Markdown, code fences, and raw URLs so spoken output sounds natural."""
    cleaned = re.sub(r"```[\s\S]*?```", " Code block omitted. ", text or "")
    cleaned = re.sub(r"`([^`]+)`", r"\1", cleaned)
    cleaned = re.sub(r"https?://\S+", "", cleaned)
    cleaned = re.sub(r"\[([^\]]+)\]\([^\)]+\)", r"\1", cleaned)
    cleaned = re.sub(r"[*_#~>|]+", " ", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned


def _play_audio_file(file_path: Path) -> bool:
    """Play an audio file via the host's available audio backend."""
    if not file_path.is_file():
        return False
    if platform.system().lower() == "darwin" and shutil.which("afplay"):
        try:
            return subprocess.run(
                ["afplay", str(file_path)], check=False,
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            ).returncode == 0
        except OSError:
            return False

    for cmd in (
        ["ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet", str(file_path)],
        ["mpg123", "-q", str(file_path)],
        ["mpv", "--no-video", "--really-quiet", str(file_path)],
        ["aplay", "-q", str(file_path)],
        ["play", "-q", str(file_path)],
    ):
        if shutil.which(cmd[0]):
            try:
                if subprocess.run(
                    cmd, check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
                ).returncode == 0:
                    return True
            except OSError:
                continue

    if AUDIO_CAPTURE_AVAILABLE and sd is not None:
        try:
            import torchaudio  # type: ignore

            waveform, sr = torchaudio.load(str(file_path))
            sd.play(waveform.numpy().T, sr)
            sd.wait()
            return True
        except Exception:
            pass
    return False


class VoiceEngine:
    """Thread-safe neural TTS (edge-tts → pyttsx3 fallback) and Whisper speech recognition."""

    def __init__(self, tts_voice: str = TTS_VOICE, whisper_model_name: str = WHISPER_MODEL) -> None:
        self.tts_voice = tts_voice
        self.whisper_model_name = whisper_model_name
        self._tts_lock = threading.Lock()
        self._stt_lock = threading.Lock()
        self._whisper_model: Any = None

    def _load_whisper(self) -> Any:
        if not WHISPER_AVAILABLE or whisper is None:
            raise RuntimeError("openai-whisper is not installed. Run: pip install openai-whisper")
        with self._stt_lock:
            if self._whisper_model is None:
                self._whisper_model = whisper.load_model(self.whisper_model_name)
            return self._whisper_model

    def synthesize_to_file(self, text: str, output_path: Path | None = None) -> Path | None:
        """Render speech to MP3 with edge-tts (also feeds the Gradio audio player)."""
        spoken = clean_text_for_speech(text)
        if not spoken or not EDGE_TTS_AVAILABLE or edge_tts is None:
            return None

        target = output_path or (get_data_dir() / f"tts_{uuid.uuid4().hex[:8]}.mp3")
        try:
            target.parent.mkdir(parents=True, exist_ok=True)
        except OSError:
            return None

        async def _generate() -> None:
            communicator = edge_tts.Communicate(text=spoken[:2500], voice=self.tts_voice)
            await communicator.save(str(target))

        def _run_blocking() -> None:
            asyncio.run(_generate())

        try:
            try:
                asyncio.get_running_loop()
                in_loop = True
            except RuntimeError:
                in_loop = False

            if in_loop:
                errors: list[Exception] = []

                def _threaded() -> None:
                    try:
                        _run_blocking()
                    except Exception as exc:  # pragma: no cover
                        errors.append(exc)

                worker = threading.Thread(target=_threaded, daemon=True)
                worker.start()
                worker.join(timeout=30)
            else:
                _run_blocking()
            return target if target.is_file() and target.stat().st_size > 0 else None
        except Exception:
            return None

    def speak(self, text: str, force: bool = False) -> bool:
        """Speak aloud — edge-tts neural voice first, pyttsx3 offline as backup."""
        if not force and not STATE.voice_enabled:
            return False
        spoken = clean_text_for_speech(text)
        if not spoken:
            return False

        with self._tts_lock:
            mp3 = self.synthesize_to_file(spoken)
            if mp3 is not None and _play_audio_file(mp3):
                try:
                    mp3.unlink(missing_ok=True)
                except OSError:
                    pass
                return True

            if PYTTSX3_AVAILABLE and pyttsx3 is not None:
                try:
                    engine = pyttsx3.init()
                    engine.setProperty("rate", 185)
                    voices = engine.getProperty("voices") or []
                    for v in voices:
                        if "english" in str(getattr(v, "name", "")).lower() and (
                            "gb" in str(getattr(v, "id", "")).lower() or "great britain" in str(getattr(v, "name", "")).lower()
                        ):
                            engine.setProperty("voice", v.id)
                            break
                    engine.say(spoken[:2500])
                    engine.runAndWait()
                    return True
                except Exception:
                    return False
        return False

    def transcribe_file(self, audio_path: str | Path) -> str:
        """Transcribe a WAV/MP3/OGG file locally with OpenAI Whisper."""
        path = Path(audio_path)
        if not path.is_file():
            return f"[STT Error] Audio file not found: {path}"
        try:
            model = self._load_whisper()
            result = model.transcribe(str(path), fp16=False)
            return str(result.get("text", "")).strip()
        except Exception as exc:
            return f"[STT Error] {exc}"

    def listen(self, duration: float = 5.0, sample_rate: int = 16000) -> str:
        """Capture microphone audio and transcribe it with Whisper."""
        if not AUDIO_CAPTURE_AVAILABLE or sd is None or np is None:
            return "[STT Error] sounddevice/numpy unavailable for microphone capture."
        if not WHISPER_AVAILABLE:
            return "[STT Error] openai-whisper is not installed."

        frames = int(max(1.0, duration) * sample_rate)
        try:
            recording = sd.rec(frames, samplerate=sample_rate, channels=1, dtype="int16")
            sd.wait()
        except Exception as exc:
            return f"[STT Error] Microphone capture failed: {exc}"

        tmp_path = Path(tempfile.mkstemp(suffix=".wav")[1])
        try:
            with wave.open(str(tmp_path), "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(sample_rate)
                wf.writeframes(recording.tobytes())
            return self.transcribe_file(tmp_path)
        finally:
            try:
                tmp_path.unlink(missing_ok=True)
            except OSError:
                pass


VOICE = VoiceEngine()


def speak(text: str, force: bool = False) -> bool:
    """Speak text through FRIDAY's global VoiceEngine."""
    try:
        return VOICE.speak(text=text, force=force)
    except Exception:
        return False


def listen(duration: float = 5.0, sample_rate: int = 16000) -> str:
    """Record from the microphone and transcribe with Whisper."""
    return VOICE.listen(duration=duration, sample_rate=sample_rate)


def transcribe_audio_file(audio_path: str | Path) -> str:
    """Transcribe an existing audio file with Whisper."""
    return VOICE.transcribe_file(audio_path=audio_path)


# ==============================================================================
# 7. SKILLS — FILESYSTEM, PDF & MARKDOWN
# ==============================================================================
def _human_size(num_bytes: int) -> str:
    size = float(num_bytes)
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if size < 1024.0 or unit == "TB":
            return f"{size:.1f} {unit}"
        size /= 1024.0
    return f"{size:.1f} TB"


def _extract_pdf_text(path: Path, max_pages: int = 30) -> str:
    if not PYPDF_AVAILABLE or PdfReader is None:
        return "[PDF Error] pypdf is not installed. Run: pip install pypdf"
    try:
        reader = PdfReader(str(path))
        total = len(reader.pages)
        chunks = []
        for idx, page in enumerate(reader.pages[: max(1, int(max_pages))]):
            try:
                text = page.extract_text() or ""
            except Exception:
                text = ""
            if text.strip():
                chunks.append(f"--- Page {idx + 1} ---\n{text.strip()}")
        body = "\n\n".join(chunks) if chunks else "[No extractable text — likely a scanned image PDF.]"
        return f"PDF '{path.name}' ({total} pages, showing up to {max_pages}):\n\n{body}"
    except Exception as exc:
        return f"[PDF Error] Failed to parse '{path}': {exc}"


@friday_tool
def read_file(file_path: str, max_chars: int = 20000) -> str:
    """Read a text or PDF file (PDF text is extracted automatically), with size truncation."""
    path = Path(file_path).expanduser()
    if not path.is_absolute():
        path = (Path.cwd() / path).resolve()
    if not path.exists():
        return f"[File Error] No such file: {path}"
    if path.is_dir():
        return f"[File Error] '{path}' is a directory — use list_dir."

    if path.suffix.lower() == ".pdf":
        return _extract_pdf_text(path, max_pages=max(1, int(max_chars / 1000)) or 30)

    try:
        raw = path.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        return f"[File Error] {exc}"

    limit = max(200, int(max_chars))
    if len(raw) > limit:
        return (
            f"Contents of '{path}' ({_human_size(path.stat().st_size)}, truncated to {limit} chars "
            f"of {len(raw)}):\n\n{raw[:limit]}\n\n... [truncated]"
        )
    return f"Contents of '{path}' ({_human_size(path.stat().st_size)}):\n\n{raw}"


@friday_tool
def write_file(file_path: str, content: str, mode: str = "w") -> str:
    """Write or append text content to a file, creating parent directories as needed."""
    path = Path(file_path).expanduser()
    if not path.is_absolute():
        path = (Path.cwd() / path).resolve()
    file_mode = "a" if (mode or "w").strip().lower().startswith("a") else "w"
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open(file_mode, encoding="utf-8") as fh:
            fh.write(content)
        verb = "appended to" if file_mode == "a" else "written to"
        return f"{len(content)} chars {verb} '{path}' ({_human_size(path.stat().st_size)} on disk)."
    except OSError as exc:
        return f"[File Error] {exc}"


@friday_tool
def delete_file(file_path: str, confirm: str = "no") -> str:
    """Delete a file or directory tree. Strictly requires confirm='yes'."""
    path = Path(file_path).expanduser()
    if not path.is_absolute():
        path = (Path.cwd() / path).resolve()
    if not path.exists():
        return f"[File Error] Nothing to delete at '{path}'."
    if str(path) in {"/", str(Path.home())}:
        return f"[Security Refusal] I'm not deleting '{path}', {user_name()}. Not today."

    allowed, msg = verify_confirmation(action="delete_file", target=str(path), confirm=confirm)
    if not allowed:
        return msg
    try:
        if path.is_dir():
            shutil.rmtree(path)
            return f"Directory tree '{path}' removed, {user_name()}."
        path.unlink()
        return f"File '{path}' removed, {user_name()}."
    except OSError as exc:
        return f"[File Error] {exc}"


@friday_tool
def list_dir(directory: str = ".", recursive: bool = False) -> str:
    """List directory contents with sizes, or walk a tree recursively."""
    path = Path(directory).expanduser()
    if not path.is_absolute():
        path = (Path.cwd() / path).resolve()
    if not path.is_dir():
        return f"[File Error] '{path}' is not a directory."

    if recursive:
        entries = []
        for root, dirs, files in os.walk(path):
            dirs[:] = [d for d in sorted(dirs) if not d.startswith(".")]
            for fname in sorted(files):
                fpath = Path(root) / fname
                try:
                    entries.append(f"{fpath.relative_to(path)} ({_human_size(fpath.stat().st_size)})")
                except OSError:
                    entries.append(str(fpath.relative_to(path)))
                if len(entries) >= 300:
                    break
            if len(entries) >= 300:
                break
        body = "\n".join(f"- {e}" for e in entries) or "[empty]"
        return f"Recursive listing of '{path}' ({len(entries)} files):\n{body}"

    try:
        children = sorted(path.iterdir(), key=lambda p: (p.is_file(), p.name.lower()))
    except OSError as exc:
        return f"[File Error] {exc}"

    lines = [f"Contents of '{path}' ({len(children)} entries):"]
    for child in children[:300]:
        try:
            if child.is_dir():
                lines.append(f"- {child.name}/ (dir)")
            else:
                lines.append(f"- {child.name} ({_human_size(child.stat().st_size)})")
        except OSError:
            lines.append(f"- {child.name}")
    return "\n".join(lines)


@friday_tool
def read_pdf(file_path: str, max_pages: int = 30) -> str:
    """Extract text content from a PDF document page by page."""
    return _extract_pdf_text(Path(file_path).expanduser(), max_pages=max_pages)


@friday_tool
def markdown_report(file_path: str, title: str, sections: str) -> str:
    """Generate a clean Markdown report file from a title and body sections (use '## ' headings)."""
    path = Path(file_path).expanduser()
    if not path.is_absolute():
        path = (Path.cwd() / path).resolve()
    stamp = datetime.now().strftime("%Y-%m-%d %H:%M")
    body = sections.strip() or "_No content supplied._"
    if not re.search(r"^#{2,6}\s", body, flags=re.MULTILINE):
        body = f"## Overview\n\n{body}"
    document = (
        f"# {title.strip() or 'FRIDAY Report'}\n\n"
        f"> Generated by **SUPER FRIDAY v3.0 MAX** on {stamp} for {user_name()}.\n\n"
        f"{body}\n"
    )
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(document, encoding="utf-8")
        return f"Markdown report written to '{path}' ({len(document)} chars)."
    except OSError as exc:
        return f"[File Error] {exc}"


FILE_TOOLS = [read_file, write_file, delete_file, list_dir, read_pdf, markdown_report]


# ==============================================================================
# 8. SKILLS — BROWSER, SEARCH & WIKIPEDIA
# ==============================================================================
_BROWSER_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)


def _normalize_url(url: str) -> str:
    clean = (url or "").strip()
    if not clean:
        return ""
    if not re.match(r"^https?://", clean, flags=re.IGNORECASE):
        clean = "https://" + clean
    return clean


def _strip_html_fallback(raw_html: str) -> tuple[str, str]:
    title_match = re.search(r"<title[^>]*>(.*?)</title>", raw_html, flags=re.IGNORECASE | re.DOTALL)
    title = re.sub(r"\s+", " ", title_match.group(1)).strip() if title_match else ""
    text = re.sub(r"(?is)<(script|style|noscript)[^>]*>.*?</\1>", " ", raw_html)
    text = re.sub(r"(?s)<[^>]+>", " ", text)
    text = re.sub(r"&nbsp;?", " ", text)
    text = re.sub(r"&amp;?", "&", text)
    text = re.sub(r"\s+", " ", text).strip()
    return title, text


def _http_fetch_text(url: str, timeout: int = 15) -> tuple[str, str]:
    headers = {"User-Agent": _BROWSER_UA, "Accept-Language": "en-US,en;q=0.9"}
    if REQUESTS_AVAILABLE and requests is not None:
        resp = requests.get(url, headers=headers, timeout=timeout)
        resp.raise_for_status()
        raw = resp.text
    else:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=timeout) as resp:  # noqa: S310
            charset = resp.headers.get_content_charset() or "utf-8"
            raw = resp.read().decode(charset, errors="replace")

    if BS4_AVAILABLE and BeautifulSoup is not None:
        soup = BeautifulSoup(raw, "html.parser")
        for tag in soup(["script", "style", "noscript", "header", "footer", "nav", "aside"]):
            tag.decompose()
        title = (soup.title.get_text(strip=True) if soup.title else "") or ""
        text = re.sub(r"\n{3,}", "\n\n", soup.get_text(separator="\n")).strip()
        return title, text
    return _strip_html_fallback(raw)


@friday_tool
def browse_url(url: str, take_screenshot: bool = False, wait_ms: int = 1500) -> str:
    """Navigate a live URL with Playwright (JS-rendered pages, optional screenshot), else HTTP+BS4."""
    target = _normalize_url(url)
    if not target:
        return "Provide a URL to browse."
    if PLAYWRIGHT_AVAILABLE and sync_playwright is not None:
        shot_path = None
        try:
            with sync_playwright() as pw:
                browser = pw.chromium.launch(headless=True, args=["--no-sandbox", "--disable-dev-shm-usage"])
                page = browser.new_page(user_agent=_BROWSER_UA, viewport={"width": 1440, "height": 900})
                page.goto(target, wait_until="domcontentloaded", timeout=45000)
                try:
                    page.wait_for_timeout(max(0, int(wait_ms)))
                except Exception:
                    pass
                title = page.title() or ""
                body = page.inner_text("body", timeout=15000) if page.query_selector("body") else ""
                body = re.sub(r"\n{3,}", "\n\n", (body or "").strip())
                if take_screenshot:
                    shot = get_data_dir() / "screenshots" / f"page_{datetime.now().strftime('%Y%m%d_%H%M%S')}.png"
                    shot.parent.mkdir(parents=True, exist_ok=True)
                    page.screenshot(path=str(shot), full_page=True)
                    shot_path = str(shot)
                browser.close()
            if body:
                excerpt = body[:6000]
                note = f"\n\nScreenshot saved: {shot_path}" if shot_path else ""
                return f"Live page '{title or target}' ({target}):\n\n{excerpt}{note}"
        except Exception:
            pass

    try:
        title, text = _http_fetch_text(target)
        excerpt = text[:6000]
        note = " (Playwright unavailable/failed — served via HTTP + parser)" if PLAYWRIGHT_AVAILABLE else ""
        return f"Page '{title or target}' ({target}){note}:\n\n{excerpt}"
    except Exception as exc:
        return f"[Browse Error] Could not load '{target}': {exc}"


@friday_tool
def web_search(query: str, max_results: int = 6) -> str:
    """Search the live web with DuckDuckGo and return ranked titles, URLs, and snippets."""
    q = (query or "").strip()
    if not q:
        return "Provide a search query."
    limit = max(1, min(int(max_results), 15))

    if DDGS_AVAILABLE and DDGS is not None:
        try:
            with DDGS() as ddgs:
                results = list(ddgs.text(q, max_results=limit))
            if results:
                lines = [f"Web search results for '{q}':"]
                for i, row in enumerate(results, 1):
                    title = row.get("title") or "(untitled)"
                    href = row.get("href") or row.get("url") or ""
                    snippet = (row.get("body") or "").strip()
                    lines.append(f"{i}. {title}\n   {href}\n   {snippet[:320]}")
                return "\n".join(lines)
        except Exception:
            pass

    try:
        url = "https://duckduckgo.com/html/?q=" + urllib.parse.quote_plus(q)
        _, text = _http_fetch_text(url)
        return f"Fallback search text for '{q}':\n\n{text[:2500]}"
    except Exception as exc:
        return f"[Search Error] DuckDuckGo lookup failed for '{q}': {exc}"


@friday_tool
def wikipedia_summary(topic: str, sentences: int = 5) -> str:
    """Fetch a concise Wikipedia summary for any topic, person, or concept."""
    subject = (topic or "").strip()
    if not subject:
        return "Provide a topic to look up on Wikipedia."
    count = max(1, min(int(sentences), 15))

    if WIKIPEDIA_AVAILABLE and wikipedia_api is not None:
        try:
            wikipedia_api.set_language("en")
            return f"Wikipedia — {subject}:\n{wikipedia_api.summary(subject, sentences=count)}"
        except Exception:
            try:
                hit = wikipedia_api.search(subject, results=1)
                if hit:
                    return f"Wikipedia — {hit[0]}:\n{wikipedia_api.summary(hit[0], sentences=count)}"
            except Exception:
                pass

    try:
        slug = urllib.parse.quote(subject.replace(" ", "_"))
        data_url = f"https://en.wikipedia.org/api/rest_v1/page/summary/{slug}"
        req = urllib.request.Request(data_url, headers={"User-Agent": "FRIDAY/3.0 (assistant)"})
        with urllib.request.urlopen(req, timeout=15) as resp:  # noqa: S310
            data = json.loads(resp.read().decode("utf-8"))
        extract = data.get("extract") or ""
        if extract:
            trimmed = " ".join(re.split(r"(?<=[.!?])\s+", extract)[:count])
            return f"Wikipedia — {data.get('title', subject)}:\n{trimmed}"
        return f"[Wikipedia] No summary available for '{subject}'."
    except Exception as exc:
        return f"[Wikipedia Error] Could not retrieve '{subject}': {exc}"


BROWSER_TOOLS = [browse_url, web_search, wikipedia_summary]


# ==============================================================================
# 9. SKILLS — CODE EXECUTION (isolated Python + confirmation-gated shell)
# ==============================================================================
@friday_tool
def run_python(code: str, timeout: int = 30) -> str:
    """Execute a Python snippet in an isolated subprocess and return stdout/stderr."""
    if not (code or "").strip():
        return "Provide Python code to execute."
    tmp_path = Path(tempfile.mkstemp(suffix=".py", prefix="friday_snippet_")[1])
    try:
        tmp_path.write_text(code, encoding="utf-8")
        res = subprocess.run(
            [sys.executable, "-I", str(tmp_path)],
            capture_output=True,
            text=True,
            timeout=max(1, min(int(timeout), 300)),
            cwd=str(Path.cwd()),
        )
        parts = []
        if res.stdout.strip():
            parts.append(f"STDOUT:\n{res.stdout.strip()}")
        if res.stderr.strip():
            parts.append(f"STDERR:\n{res.stderr.strip()}")
        parts.append(f"Exit code: {res.returncode} · {len(code)} chars executed · {sys.version.split()[0]}")
        return "\n\n".join(parts)
    except subprocess.TimeoutExpired:
        return f"[Execution Timeout] Python snippet exceeded {timeout}s."
    except Exception as exc:
        return f"[Execution Error] {exc}"
    finally:
        try:
            tmp_path.unlink(missing_ok=True)
        except OSError:
            pass


@friday_tool
def run_shell(command: str, confirm: str = "no", timeout: int = 30) -> str:
    """Execute a shell command — dry run by default; requires confirm='yes'. Catastrophic commands blocked."""
    cmd = (command or "").strip()
    if not cmd:
        return "Provide a shell command."

    allowed, msg = verify_confirmation(action="run_shell", target=cmd, confirm=confirm)
    if not allowed:
        return msg

    try:
        res = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
            timeout=max(1, min(int(timeout), 600)),
            cwd=str(Path.cwd()),
        )
        parts = [f"$ {cmd}"]
        if res.stdout.strip():
            parts.append(f"STDOUT:\n{res.stdout.strip()}")
        if res.stderr.strip():
            parts.append(f"STDERR:\n{res.stderr.strip()}")
        parts.append(f"Exit code: {res.returncode}")
        return "\n\n".join(parts)
    except subprocess.TimeoutExpired:
        return f"[Shell Timeout] '{cmd}' exceeded {timeout}s."
    except Exception as exc:
        return f"[Shell Error] {exc}"


CODING_TOOLS = [run_python, run_shell]


# ==============================================================================
# 10. SKILLS — EMAIL (Gmail SMTP) & GOOGLE-CALENDAR-READY HOOKS
# ==============================================================================
def _calendar_json_path() -> Path:
    return get_data_dir() / "calendar" / "events.json"


def _load_local_events() -> list[dict[str, Any]]:
    data = _read_json(_calendar_json_path(), [])
    return data if isinstance(data, list) else []


def _save_local_events(events: list[dict[str, Any]]) -> None:
    _atomic_write_json(_calendar_json_path(), events)


def _parse_event_dt(raw: str) -> datetime:
    """Parse ISO / 'YYYY-MM-DD HH:MM' / 'YYYY-MM-DD HH:MM AM' timestamps into naive UTC."""
    text = (raw or "").strip()
    if not text:
        raise ValueError("empty timestamp")
    for fmt in ("%Y-%m-%d %H:%M", "%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%dT%H:%M",
                "%Y-%m-%d %I:%M %p", "%Y-%m-%d %H:%M %p"):
        try:
            return datetime.strptime(text, fmt)
        except ValueError:
            continue
    parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
    if parsed.tzinfo is not None:
        parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
    return parsed


def _try_google_calendar_service() -> Any:
    """Google Calendar API hook — returns a service if OAuth creds + client libs exist."""
    try:
        from google.oauth2.credentials import Credentials  # type: ignore
        from googleapiclient.discovery import build as google_build  # type: ignore

        token_path = get_data_dir() / "calendar" / "token.json"
        if not token_path.is_file():
            return None
        creds = Credentials.from_authorized_user_file(str(token_path))
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(__import__("google.auth.transport.requests", fromlist=["Request"]).Request())
        return google_build("calendar", "v3", credentials=creds, cache_discovery=False)
    except Exception:
        return None


@friday_tool
def send_email(to_email: str, subject: str, body: str, cc_email: str = "") -> str:
    """Send an email via Gmail SMTP (app-password auth) — logs a .eml draft if unconfigured."""
    sender = _env("GMAIL_ADDRESS", GMAIL_ADDRESS)
    password = _env("GMAIL_APP_PASSWORD", GMAIL_APP_PASSWORD)
    recipient = (to_email or "").strip()
    if not recipient or "@" not in recipient:
        return f"[Email Error] '{to_email}' is not a valid recipient address, {user_name()}."

    if not sender or not password:
        draft_dir = get_data_dir() / "logs" / "email_drafts"
        draft_dir.mkdir(parents=True, exist_ok=True)
        draft = draft_dir / f"draft_{datetime.now().strftime('%Y%m%d_%H%M%S')}.eml"
        draft.write_text(
            f"To: {recipient}\nFrom: {sender or 'unconfigured@friday.local'}\n"
            f"Subject: {subject}\n\n{body}\n",
            encoding="utf-8",
        )
        return (
            f"[Email Draft] GMAIL_ADDRESS / GMAIL_APP_PASSWORD not configured, {user_name()}. "
            f"Draft saved to '{draft}' — nothing was transmitted."
        )

    import smtplib
    from email.message import EmailMessage

    msg = EmailMessage()
    msg["From"] = sender
    msg["To"] = recipient
    msg["Subject"] = subject or "(no subject)"
    if cc_email.strip():
        msg["Cc"] = cc_email.strip()
    msg.set_content(body or "")

    try:
        with smtplib.SMTP_SSL("smtp.gmail.com", 465, timeout=25) as server:
            server.login(sender, password)
            server.send_message(msg)
        return f"Email dispatched to {recipient} with subject '{subject}', {user_name()}."
    except Exception as exc:
        return f"[Email Error] Gmail SMTP failed: {exc}"


@friday_tool
def create_calendar_event(
    summary: str,
    start_time: str,
    end_time: str,
    description: str = "",
    location: str = "",
) -> str:
    """Schedule a calendar event — Google Calendar when authorised, else a local .ics + JSON record."""
    title = (summary or "").strip() or "FRIDAY Event"
    try:
        start_dt = _parse_event_dt(start_time)
        end_dt = _parse_event_dt(end_time) if (end_time or "").strip() else start_dt + timedelta(hours=1)
    except Exception as exc:
        return f"[Calendar Error] Invalid timestamp: {exc}"

    service = _try_google_calendar_service()
    if service is not None:
        try:
            created = service.events().insert(
                calendarId="primary",
                body={
                    "summary": title,
                    "description": description,
                    "location": location,
                    "start": {"dateTime": start_dt.isoformat(), "timeZone": "UTC"},
                    "end": {"dateTime": end_dt.isoformat(), "timeZone": "UTC"},
                },
            ).execute()
            return f"Google Calendar event created: '{title}' (id: {created.get('id')}) at {start_dt.isoformat()}Z."
        except Exception:
            pass

    stamp = "%Y%m%dT%H%M%SZ"
    ics = (
        "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//FRIDAY v3.0 MAX//EN\r\nCALSCALE:GREGORIAN\r\n"
        "BEGIN:VEVENT\r\n"
        f"UID:{uuid.uuid4().hex}@friday.local\r\n"
        f"DTSTAMP:{datetime.now(timezone.utc).strftime(stamp)}\r\n"
        f"DTSTART:{start_dt.strftime(stamp)}\r\n"
        f"DTEND:{end_dt.strftime(stamp)}\r\n"
        f"SUMMARY:{title}\r\n"
        f"DESCRIPTION:{(description or '').replace(chr(10), ' ')}\r\n"
        f"LOCATION:{(location or '').replace(chr(10), ' ')}\r\n"
        "END:VEVENT\r\nEND:VCALENDAR\r\n"
    )
    safe_name = re.sub(r"[^A-Za-z0-9_-]+", "_", title)[:48] or "event"
    ics_path = get_data_dir() / "calendar" / f"{safe_name}_{start_dt.strftime('%Y%m%d_%H%M')}.ics"
    try:
        ics_path.parent.mkdir(parents=True, exist_ok=True)
        ics_path.write_text(ics, encoding="utf-8")
        events = _load_local_events()
        events.append(
            {
                "summary": title,
                "start": start_dt.isoformat(),
                "end": end_dt.isoformat(),
                "description": description,
                "location": location,
                "ics": str(ics_path),
                "created_at": _utc_now_iso(),
            }
        )
        _save_local_events(events)
        return (
            f"Calendar event '{title}' scheduled {start_dt.isoformat()} → {end_dt.isoformat()} UTC. "
            f"iCalendar file: '{ics_path}' (import-ready; drop token.json into data/calendar for Google sync)."
        )
    except OSError as exc:
        return f"[Calendar Error] {exc}"


@friday_tool
def list_calendar_events(max_results: int = 10) -> str:
    """List upcoming calendar events (Google Calendar when authorised, else local store)."""
    limit = max(1, int(max_results))

    service = _try_google_calendar_service()
    if service is not None:
        try:
            res = service.events().list(
                calendarId="primary",
                maxResults=limit,
                singleEvents=True,
                orderBy="startTime",
                timeMin=datetime.now(timezone.utc).isoformat(),
            ).execute()
            items = res.get("items", [])
            if items:
                lines = ["Upcoming Google Calendar events:"]
                for ev in items:
                    when = (ev.get("start") or {}).get("dateTime") or (ev.get("start") or {}).get("date", "")
                    lines.append(f"- {when} · {ev.get('summary', '(untitled)')}")
                return "\n".join(lines)
            return "Google Calendar returned no upcoming events."
        except Exception:
            pass

    events = _load_local_events()
    if not events:
        return "No calendar events stored locally."
    upcoming = sorted(events, key=lambda e: str(e.get("start", "")))
    lines = [f"Local calendar events ({min(limit, len(upcoming))} of {len(upcoming)}):"]
    for ev in upcoming[:limit]:
        lines.append(
            f"- {ev.get('start')} → {ev.get('end')} · {ev.get('summary')} "
            f"@ {ev.get('location') or 'n/a'} · {ev.get('ics')}"
        )
    return "\n".join(lines)


EMAIL_TOOLS = [send_email, create_calendar_event, list_calendar_events]


# ==============================================================================
# 11. SKILLS — SMART HOME / NETWORK (Jio router, LAN diagnostics, port scan)
# ==============================================================================
def _tcp_probe(host: str, port: int, timeout: float = 1.5) -> tuple[bool, float]:
    started = time.time()
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    try:
        sock.connect((host, int(port)))
        return True, (time.time() - started) * 1000.0
    except Exception:
        return False, (time.time() - started) * 1000.0
    finally:
        try:
            sock.close()
        except OSError:
            pass


def _local_ipv4() -> str:
    try:
        probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        probe.settimeout(0.5)
        probe.connect(("8.8.8.8", 80))
        ip = probe.getsockname()[0]
        probe.close()
        return ip
    except Exception:
        return "127.0.0.1"


@friday_tool
def open_jio_admin(portal_url: str = "http://192.168.29.1", open_browser: bool = False) -> str:
    """Reach the JioFiber router admin portal, report its status, and optionally open it locally."""
    target = _normalize_url(portal_url)
    opened = ""
    if open_browser:
        try:
            import webbrowser

            webbrowser.open(target, new=2)
            opened = " Browser window requested."
        except Exception:
            opened = " Browser launch failed."
    try:
        status_code = 0
        title = ""
        if REQUESTS_AVAILABLE and requests is not None:
            resp = requests.get(target, timeout=8, headers={"User-Agent": _BROWSER_UA})
            status_code = resp.status_code
            tm = re.search(r"<title[^>]*>(.*?)</title>", resp.text, flags=re.IGNORECASE | re.DOTALL)
            title = re.sub(r"\s+", " ", tm.group(1)).strip() if tm else ""
        else:
            req = urllib.request.Request(target, headers={"User-Agent": _BROWSER_UA})
            with urllib.request.urlopen(req, timeout=8) as resp:  # noqa: S310
                status_code = resp.status
                raw = resp.read().decode("utf-8", errors="replace")
                tm = re.search(r"<title[^>]*>(.*?)</title>", raw, flags=re.IGNORECASE | re.DOTALL)
                title = re.sub(r"\s+", " ", tm.group(1)).strip() if tm else ""
        return (
            f"JioFiber admin portal reachable at {target} (HTTP {status_code}"
            + (f", title: '{title}'" if title else "")
            + f").{opened} Sign in at the portal to manage SSID, DHCP, and device access."
        )
    except Exception as exc:
        return (
            f"JioFiber admin portal at {target} did not respond cleanly ({exc}).{opened} "
            f"Confirm you're on the Jio LAN (your IP: {_local_ipv4()}) and try 192.168.29.1 again."
        )


@friday_tool
def network_diagnostics(target_host: str = "8.8.8.8") -> str:
    """Run connectivity diagnostics: local IP, DNS resolution, TCP probes, and ping latency."""
    host = (target_host or "").strip() or "8.8.8.8"
    lines = [f"Network diagnostics for {host}:"]
    lines.append(f"- Local IPv4: {_local_ipv4()}")
    try:
        infos = socket.getaddrinfo(host, None)
        addresses = sorted({i[4][0] for i in infos})
        lines.append(f"- DNS resolution OK: {', '.join(addresses[:4])}")
    except Exception as exc:
        lines.append(f"- DNS resolution FAILED: {exc}")

    probe_targets = [("443", 443), ("80", 80), ("53", 53), ("22", 22)]
    reachable = []
    for label, port in probe_targets:
        ok, latency = _tcp_probe(host, port, timeout=1.2)
        if ok:
            reachable.append(f"tcp/{label} ({latency:.0f}ms)")
    lines.append(f"- Open TCP ports: {', '.join(reachable) if reachable else 'none reachable'}")

    try:
        is_win = platform.system().lower() == "windows"
        flag = "-n" if is_win else "-c"
        cmd = ["ping", flag, "3", host]
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=12)
        tail = [ln.strip() for ln in res.stdout.splitlines() if ln.strip()][-3:]
        lines.append(f"- Ping ({host}):\n    " + "\n    ".join(tail) if tail else f"- Ping ({host}): no output")
    except Exception as exc:
        lines.append(f"- Ping unavailable: {exc}")

    return "\n".join(lines)


@friday_tool
def scan_local_ports(host: str = "127.0.0.1", ports: str = "22,80,443,7860,8000,8080,11434") -> str:
    """Probe a comma-separated list of TCP ports on a host and report which are open."""
    target = (host or "127.0.0.1").strip()
    raw_ports = [p.strip() for p in (ports or "").split(",") if p.strip()]
    parsed: list[int] = []
    for chunk in raw_ports:
        if "-" in chunk:
            try:
                lo, hi = chunk.split("-", 1)
                parsed.extend(range(int(lo), min(int(hi), 65535) + 1))
            except ValueError:
                continue
        else:
            try:
                parsed.append(int(chunk))
            except ValueError:
                continue
    parsed = sorted({p for p in parsed if 0 < p < 65536})[:200]
    if not parsed:
        return f"[Port Scan] No valid ports supplied for {target}."

    open_ports, closed = [], []
    for port in parsed:
        ok, latency = _tcp_probe(target, port, timeout=0.8)
        (open_ports if ok else closed).append(f"{port} ({latency:.0f}ms)" if ok else str(port))

    lines = [f"Port scan of {target} ({len(parsed)} ports probed):"]
    lines.append(f"- OPEN: {', '.join(open_ports) if open_ports else 'none'}")
    lines.append(f"- CLOSED/FILTERED: {', '.join(closed[:40])}{' …' if len(closed) > 40 else ''}")
    return "\n".join(lines)


SMART_HOME_TOOLS = [open_jio_admin, network_diagnostics, scan_local_ports]


# ==============================================================================
# 12. SKILLS — SYSTEM TELEMETRY, PROCESSES, SCREENSHOTS & VISION
# ==============================================================================
@friday_tool
def get_system_info() -> str:
    """Report OS, CPU, memory, disk, battery, Python runtime, and accelerator telemetry."""
    uname = platform.uname()
    try:
        disk = shutil.disk_usage(Path.cwd())
    except OSError:
        disk = shutil.disk_usage("/")
    lines = [
        f"System Telemetry ({datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}):",
        f"- Platform: {uname.system} {uname.release} ({uname.machine})",
        f"- Hostname: {uname.node}",
        f"- Python Runtime: {sys.version.split()[0]} ({sys.executable})",
        f"- Logical CPU Cores: {os.cpu_count() or 1}",
    ]

    if PSUTIL_AVAILABLE and psutil is not None:
        try:
            vm = psutil.virtual_memory()
            lines.append(f"- CPU Load: {psutil.cpu_percent(interval=0.15):.1f}%")
            lines.append(
                f"- Memory: {vm.used / (1024**3):.2f} GB / {vm.total / (1024**3):.2f} GB ({vm.percent:.1f}%)"
            )
            batt = getattr(psutil, "sensors_battery", lambda: None)()
            if batt is not None:
                lines.append(f"- Battery: {batt.percent:.0f}% ({'Charging/AC' if batt.power_plugged else 'On Battery'})")
        except Exception:
            pass
    elif Path("/proc/meminfo").is_file():
        try:
            kv: dict[str, int] = {}
            for row in Path("/proc/meminfo").read_text(encoding="utf-8").splitlines()[:6]:
                key, _, rest = row.partition(":")
                if rest.strip():
                    kv[key.strip()] = int(rest.strip().split()[0])
            if "MemTotal" in kv and "MemAvailable" in kv:
                total_gb = kv["MemTotal"] / (1024**2)
                used_gb = (kv["MemTotal"] - kv["MemAvailable"]) / (1024**2)
                lines.append(f"- Memory: {used_gb:.2f} GB / {total_gb:.2f} GB")
        except Exception:
            pass

    lines.append(
        f"- Disk ({Path.cwd().anchor or '/'}): {disk.used / (1024**3):.2f} GB used / "
        f"{disk.total / (1024**3):.2f} GB total ({disk.free / (1024**3):.2f} GB free)"
    )

    try:
        import torch  # type: ignore

        if torch.cuda.is_available():
            lines.append(f"- Accelerator: CUDA GPU Online ({torch.cuda.get_device_name(0)})")
        else:
            lines.append("- Accelerator: CPU Mode (CUDA not detected)")
    except ImportError:
        lines.append("- Accelerator: Standard CPU Runtime")

    return "\n".join(lines)


@friday_tool
def list_processes(top_n: int = 15, filter_name: str = "") -> str:
    """List the heaviest running processes, optionally filtered by process name."""
    limit = max(1, min(int(top_n), 50))
    needle = (filter_name or "").strip().lower()

    if PSUTIL_AVAILABLE and psutil is not None:
        rows = []
        for proc in psutil.process_iter(["pid", "name", "cpu_percent", "memory_percent"]):
            try:
                info = proc.info
                pname = str(info.get("name") or "")
                if needle and needle not in pname.lower():
                    continue
                rows.append(info)
            except Exception:
                continue
        rows.sort(key=lambda r: float(r.get("memory_percent") or 0.0), reverse=True)
        if not rows:
            return f"No running processes matched '{filter_name}'."
        lines = [f"Active processes (top {min(limit, len(rows))}):"]
        for r in rows[:limit]:
            lines.append(
                f"- PID {r['pid']:<7} | MEM {float(r.get('memory_percent') or 0.0):5.1f}% | "
                f"CPU {float(r.get('cpu_percent') or 0.0):5.1f}% | {r.get('name', 'unknown')}"
            )
        return "\n".join(lines)

    if shutil.which("ps"):
        res = subprocess.run(
            ["ps", "-eo", "pid,pmem,pcpu,comm", "--sort=-pmem"], capture_output=True, text=True, check=False
        )
        rows_out = res.stdout.splitlines()
        if needle:
            rows_out = rows_out[:1] + [r for r in rows_out[1:] if needle in r.lower()]
        return "\n".join(rows_out[: limit + 1])
    return "[Process Error] Neither psutil nor a system `ps` binary is available."


@friday_tool
def kill_process(pid: int, confirm: str = "no") -> str:
    """Terminate a process by PID. Strictly requires confirm='yes'."""
    try:
        target_pid = int(pid)
    except (TypeError, ValueError):
        return f"[Process Error] '{pid}' is not a valid PID."

    if target_pid <= 1 or target_pid == os.getpid():
        return f"[Security Refusal] PID {target_pid} is protected, {user_name()}."

    allowed, msg = verify_confirmation(action="kill_process", target=f"PID {target_pid}", confirm=confirm)
    if not allowed:
        return msg

    try:
        os.kill(target_pid, signal.SIGTERM)
        return f"SIGTERM dispatched to PID {target_pid}, {user_name()}."
    except ProcessLookupError:
        return f"PID {target_pid} is already gone."
    except PermissionError:
        return f"[Permission Denied] Insufficient privileges for PID {target_pid}."
    except Exception as exc:
        return f"[Process Error] {exc}"


@friday_tool
def take_screenshot(filename: str = "") -> str:
    """Capture the current desktop into FRIDAY's screenshots vault."""
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    clean_name = (filename or "").strip() or f"screen_{stamp}.png"
    if not clean_name.lower().endswith(".png"):
        clean_name += ".png"
    out_path = get_data_dir() / "screenshots" / Path(clean_name).name

    if PILLOW_AVAILABLE and ImageGrab is not None:
        try:
            out_path.parent.mkdir(parents=True, exist_ok=True)
            ImageGrab.grab().save(str(out_path))
            return f"Desktop screenshot saved to '{out_path}', {user_name()}."
        except Exception:
            pass

    for cmd in (["screencapture", "-x", str(out_path)], ["scrot", str(out_path)], ["import", "-window", "root", str(out_path)]):
        if shutil.which(cmd[0]):
            try:
                res = subprocess.run(cmd, capture_output=True, text=True, check=False)
                if res.returncode == 0 and out_path.is_file():
                    return f"Desktop screenshot saved to '{out_path}', {user_name()}."
            except OSError:
                continue
    return "[Screenshot Unavailable] No GUI display or capture backend on this host."


@friday_tool
def describe_image(
    image_path: str,
    prompt: str = "Describe everything in this image with sharp technical precision.",
) -> str:
    """Analyse a local image using GPT-4o Vision (or a local Ollama vision model)."""
    path = Path(image_path).expanduser()
    if not path.is_absolute():
        path = (Path.cwd() / path).resolve()
    if not path.is_file():
        return f"[Vision Error] Image not found: {path}"

    meta = f"File: {path.name} ({_human_size(path.stat().st_size)})"
    if PILLOW_AVAILABLE and Image is not None:
        try:
            with Image.open(path) as im:
                meta = f"File: {path.name} | {im.format} | {im.size[0]}x{im.size[1]} | {im.mode}"
        except Exception:
            pass

    if _env_bool("USE_LOCAL_LLM", USE_LOCAL_LLM):
        try:
            import ollama  # type: ignore

            model_name = _env("OLLAMA_MODEL", OLLAMA_MODEL) or OLLAMA_MODEL
            resp = ollama.chat(
                model=model_name,
                messages=[{"role": "user", "content": prompt, "images": [str(path)]}],
            )
            content = (resp.get("message") or {}).get("content", "")
            if content:
                return f"Vision analysis ({model_name}) [{meta}]:\n{content}"
        except Exception as exc:
            return f"[Local Vision Note] {meta} — Ollama vision failed: {exc}"

    key = _env("OPENAI_API_KEY", OPENAI_API_KEY)
    if key:
        try:
            from openai import OpenAI  # type: ignore

            mime, _ = mimetypes.guess_type(str(path))
            b64 = base64.b64encode(path.read_bytes()).decode("ascii")
            client = OpenAI(api_key=key)
            completion = client.chat.completions.create(
                model=_env("MODEL", MODEL) or "gpt-4o",
                messages=[
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt},
                            {
                                "type": "image_url",
                                "image_url": {"url": f"data:{mime or 'image/png'};base64,{b64}"},
                            },
                        ],
                    }
                ],
                max_tokens=900,
            )
            answer = completion.choices[0].message.content or ""
            return f"Vision analysis [{meta}]:\n{answer}"
        except Exception as exc:
            return f"[Vision API Error] {meta} — {exc}"

    return (
        f"[Vision Offline Metadata] {meta}. Configure OPENAI_API_KEY for GPT-4o Vision, "
        f"or USE_LOCAL_LLM=true with a multimodal Ollama model."
    )


SYSTEM_TOOLS = [get_system_info, list_processes, kill_process, take_screenshot, describe_image]


# ==============================================================================
# 13. SKILLS — AUTOMATION (time, AST calculator, weather, stocks, translate, reminders)
# ==============================================================================
_ALLOWED_BINOPS: dict[type, Callable[[Any, Any], Any]] = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod,
    ast.Pow: operator.pow,
}

_ALLOWED_UNARYOPS: dict[type, Callable[[Any], Any]] = {
    ast.UAdd: operator.pos,
    ast.USub: operator.neg,
}

_ALLOWED_MATH_NAMES: dict[str, Any] = {
    "pi": math.pi, "e": math.e, "tau": math.tau,
    "sqrt": math.sqrt, "sin": math.sin, "cos": math.cos, "tan": math.tan,
    "asin": math.asin, "acos": math.acos, "atan": math.atan, "atan2": math.atan2,
    "log": math.log, "log10": math.log10, "log2": math.log2, "exp": math.exp,
    "factorial": math.factorial, "gcd": math.gcd, "ceil": math.ceil, "floor": math.floor,
    "abs": abs, "round": round, "degrees": math.degrees, "radians": math.radians,
    "hypot": math.hypot, "pow": pow, "min": min, "max": max, "sum": sum,
}


def _eval_ast_node(node: ast.AST) -> Any:
    if isinstance(node, ast.Expression):
        return _eval_ast_node(node.body)
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)) and not isinstance(node.value, bool):
        return node.value
    if isinstance(node, ast.UnaryOp) and type(node.op) in _ALLOWED_UNARYOPS:
        return _ALLOWED_UNARYOPS[type(node.op)](_eval_ast_node(node.operand))
    if isinstance(node, ast.BinOp) and type(node.op) in _ALLOWED_BINOPS:
        left = _eval_ast_node(node.left)
        right = _eval_ast_node(node.right)
        if isinstance(node.op, ast.Pow) and abs(right) > 4096:
            raise ValueError("Exponent magnitude exceeds the 4096 safety ceiling.")
        return _ALLOWED_BINOPS[type(node.op)](left, right)
    if isinstance(node, ast.Name) and node.id in _ALLOWED_MATH_NAMES:
        val = _ALLOWED_MATH_NAMES[node.id]
        if isinstance(val, (int, float)):
            return val
        raise ValueError(f"'{node.id}' is a function, not a numeric constant.")
    if isinstance(node, ast.Call) and isinstance(node.func, ast.Name) and node.func.id in _ALLOWED_MATH_NAMES:
        fn = _ALLOWED_MATH_NAMES[node.func.id]
        if not callable(fn) or node.keywords:
            raise ValueError(f"Invalid function call: {node.func.id}")
        return fn(*[_eval_ast_node(arg) for arg in node.args])
    if isinstance(node, (ast.Tuple, ast.List)):
        return [_eval_ast_node(el) for el in node.elts]
    raise ValueError(f"Unsupported or unsafe expression construct: {type(node).__name__}")


@friday_tool
def get_current_time(timezone_name: str = "local") -> str:
    """Return the exact current date, weekday, time, and ISO-8601 timestamp for a timezone."""
    label = (timezone_name or "").strip() or "local"
    try:
        if label.lower() in {"local", "system"}:
            now = datetime.now().astimezone()
            zone = str(now.tzinfo or "local")
        elif label.upper() == "UTC":
            now = datetime.now(timezone.utc)
            zone = "UTC"
        else:
            try:
                from zoneinfo import ZoneInfo

                now = datetime.now(ZoneInfo(label))
                zone = label
            except Exception:
                now = datetime.now(timezone.utc)
                zone = f"UTC (unknown zone '{label}')"
        return f"Current Time ({zone}): {now.strftime('%A, %Y-%m-%d %H:%M:%S %Z')} (ISO: {now.isoformat()})"
    except Exception as exc:
        return f"Current Time (UTC): {datetime.now(timezone.utc).isoformat()} [zone lookup issue: {exc}]"


@friday_tool
def calculator(expression: str) -> str:
    """Evaluate a mathematical or scientific expression safely via AST parsing (never eval)."""
    cleaned = (expression or "").strip().replace("^", "**")
    if not cleaned:
        return "Provide a mathematical expression to evaluate."
    try:
        result = _eval_ast_node(ast.parse(cleaned, mode="eval"))
        if isinstance(result, float) and result.is_integer():
            result = int(result)
        return f"{expression.strip()} = {result}"
    except Exception as exc:
        return f"[Calculator Error] Invalid expression '{expression}': {exc}"


def _http_get_json(url: str, timeout: int = 10) -> dict[str, Any]:
    headers = {"User-Agent": "FRIDAY-Telemetry/3.0"}
    if REQUESTS_AVAILABLE and requests is not None:
        resp = requests.get(url, headers=headers, timeout=timeout)
        resp.raise_for_status()
        return dict(resp.json())
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=timeout) as resp:  # noqa: S310
        return dict(json.loads(resp.read().decode("utf-8")))


@friday_tool
def get_weather(city: str) -> str:
    """Fetch live weather for a city via OpenWeatherMap (wttr.in fallback)."""
    clean_city = (city or "").strip()
    if not clean_city:
        return "Specify a city name."

    owm_key = _env("OPENWEATHER_API_KEY", OPENWEATHER_API_KEY)
    if owm_key:
        try:
            q = urllib.parse.quote(clean_city)
            data = _http_get_json(f"https://api.openweathermap.org/data/2.5/weather?q={q}&appid={owm_key}&units=metric")
            main = data.get("main", {})
            wind = data.get("wind", {})
            desc = ((data.get("weather") or [{}])[0].get("description") or "clear").capitalize()
            country = (data.get("sys") or {}).get("country", "")
            return (
                f"Weather in {data.get('name', clean_city)}, {country}: {desc} | "
                f"Temp {main.get('temp')}°C (feels {main.get('feels_like')}°C) | "
                f"Humidity {main.get('humidity')}% | Wind {wind.get('speed')} m/s"
            )
        except Exception:
            pass

    try:
        data = _http_get_json(f"https://wttr.in/{urllib.parse.quote(clean_city)}?format=j1")
        curr = (data.get("current_condition") or [{}])[0]
        desc = ((curr.get("weatherDesc") or [{}])[0].get("value") or "Unknown")
        return (
            f"Weather in {clean_city}: {desc} | Temp {curr.get('temp_C')}°C "
            f"(feels {curr.get('FeelsLikeC')}°C) | Humidity {curr.get('humidity')}% | "
            f"Wind {curr.get('windspeedKmph')} km/h"
        )
    except Exception as exc:
        return f"[Weather Error] Could not retrieve live weather for '{clean_city}': {exc}"


@friday_tool
def stock_price(ticker: str) -> str:
    """Fetch the latest quote, session delta, and 5-day range for a stock or index ticker."""
    sym = (ticker or "").strip().upper()
    if not sym:
        return "Provide a ticker symbol such as 'AAPL', 'NVDA', or 'RELIANCE.NS'."
    if not YFINANCE_AVAILABLE or yf is None:
        return "[Finance Error] yfinance is not installed. Run: pip install yfinance"
    try:
        hist = yf.Ticker(sym).history(period="5d")
        if hist is None or hist.empty:
            return f"No market data returned for '{sym}'."
        latest = float(hist["Close"].iloc[-1])
        prev = float(hist["Close"].iloc[-2]) if len(hist) >= 2 else latest
        delta = latest - prev
        pct = (delta / prev * 100.0) if prev else 0.0
        sign = "+" if delta >= 0 else ""
        return (
            f"{sym}: {latest:,.2f} ({sign}{delta:,.2f} / {sign}{pct:.2f}% vs prior session) | "
            f"5-day range {float(hist['Low'].min()):,.2f} – {float(hist['High'].max()):,.2f}"
        )
    except Exception as exc:
        return f"[Finance Error] Failed to fetch '{sym}': {exc}"


@friday_tool
def translate(text: str, target_language: str = "en", source_language: str = "auto") -> str:
    """Translate text into any target language using deep-translator."""
    body = (text or "").strip()
    if not body:
        return "Provide text to translate."
    if not TRANSLATOR_AVAILABLE or GoogleTranslator is None:
        return "[Translator Error] deep-translator is not installed. Run: pip install deep-translator"
    try:
        out = GoogleTranslator(
            source=(source_language or "auto").strip(), target=(target_language or "en").strip()
        ).translate(body)
        return f"Translation ({source_language} → {target_language}):\n{out}"
    except Exception as exc:
        return f"[Translator Error] {exc}"


_REMINDER_LOCK = threading.RLock()


def _parse_reminder_when(when: str) -> datetime:
    """Parse 'in N seconds/minutes/hours/days', 'HH:MM', or an ISO timestamp into aware UTC."""
    now = datetime.now(timezone.utc)
    raw = (when or "").strip()
    rel = re.match(
        r"^(?:in\s+)?(\d+(?:\.\d+)?)\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days)$",
        raw,
        flags=re.IGNORECASE,
    )
    if rel:
        amount = float(rel.group(1))
        unit = rel.group(2).lower()
        if unit.startswith("s"):
            return now + timedelta(seconds=amount)
        if unit.startswith("m"):
            return now + timedelta(minutes=amount)
        if unit.startswith("h"):
            return now + timedelta(hours=amount)
        return now + timedelta(days=amount)

    hhmm = re.match(r"^(\d{1,2}):(\d{2})(?::(\d{2}))?$", raw)
    if hhmm:
        local_now = datetime.now().astimezone()
        target = local_now.replace(
            hour=int(hhmm.group(1)), minute=int(hhmm.group(2)),
            second=int(hhmm.group(3) or 0), microsecond=0,
        )
        if target <= local_now:
            target += timedelta(days=1)
        return target.astimezone(timezone.utc)

    cleaned = raw.replace("Z", "+00:00")
    for fmt in (None, "%Y-%m-%d %H:%M", "%Y-%m-%d %H:%M:%S"):
        try:
            dt = datetime.fromisoformat(cleaned) if fmt is None else datetime.strptime(cleaned, fmt)
            if dt.tzinfo is None:
                dt = dt.astimezone()
            return dt.astimezone(timezone.utc)
        except ValueError:
            continue

    raise ValueError(
        f"Unrecognized reminder time '{when}'. Try 'in 10 minutes', '18:30', or '2026-11-12 19:00'."
    )


@friday_tool
def add_reminder(message: str, when: str) -> str:
    """Schedule a persistent reminder ('in 15 minutes', '18:30', or 'YYYY-MM-DD HH:MM')."""
    clean_msg = (message or "").strip()
    if not clean_msg:
        return "Reminder message cannot be empty."
    try:
        due_dt = _parse_reminder_when(when)
    except ValueError as exc:
        return f"[Reminder Error] {exc}"

    item = {
        "id": uuid.uuid4().hex[:8],
        "message": clean_msg,
        "due_at": due_dt.isoformat(),
        "created_at": _utc_now_iso(),
        "triggered": False,
    }
    with _REMINDER_LOCK:
        reminders = load_reminders()
        reminders.append(item)
        save_reminders(reminders)
    return f"Reminder [{item['id']}] set for {due_dt.strftime('%Y-%m-%d %H:%M:%S UTC')}: '{clean_msg}', {user_name()}."


@friday_tool
def list_reminders(include_completed: bool = False) -> str:
    """List pending (and optionally completed) reminders from reminders.json."""
    with _REMINDER_LOCK:
        reminders = load_reminders()
    rows = [r for r in reminders if include_completed or not r.get("triggered", False)]
    if not rows:
        return f"No active reminders pending, {user_name()}."
    lines = ["Scheduled reminders:"]
    for r in sorted(rows, key=lambda x: str(x.get("due_at", ""))):
        status = "DONE" if r.get("triggered") else "PENDING"
        lines.append(f"- [{r.get('id')}] ({status}) due {str(r.get('due_at', ''))[:19]} UTC → {r.get('message')}")
    return "\n".join(lines)


class ReminderDaemon:
    """Background daemon that fires due reminders through voice, console, and Telegram."""

    def __init__(
        self,
        on_due_callback: Callable[[dict[str, Any]], None] | None = None,
        poll_interval: float = 5.0,
    ) -> None:
        self.on_due_callback = on_due_callback
        self.poll_interval = max(0.1, float(poll_interval))
        self._stop_event = threading.Event()
        self._thread: threading.Thread | None = None

    def check_once(self) -> list[dict[str, Any]]:
        due: list[dict[str, Any]] = []
        now = datetime.now(timezone.utc)
        with _REMINDER_LOCK:
            reminders = load_reminders()
            changed = False
            for r in reminders:
                if r.get("triggered"):
                    continue
                try:
                    due_dt = datetime.fromisoformat(str(r["due_at"]).replace("Z", "+00:00"))
                    if due_dt.tzinfo is None:
                        due_dt = due_dt.replace(tzinfo=timezone.utc)
                except Exception:
                    continue
                if due_dt <= now:
                    r["triggered"] = True
                    r["triggered_at"] = now.isoformat()
                    changed = True
                    due.append(dict(r))
            if changed:
                save_reminders(reminders)

        for item in due:
            if self.on_due_callback is not None:
                try:
                    self.on_due_callback(item)
                except Exception:
                    pass
            speak(f"Reminder alert, {user_name()}: {item.get('message')}")
        return due

    def _loop(self) -> None:
        while not self._stop_event.is_set():
            try:
                self.check_once()
            except Exception:
                pass
            self._stop_event.wait(self.poll_interval)

    def start(self) -> "ReminderDaemon":
        if self._thread is None or not self._thread.is_alive():
            self._stop_event.clear()
            self._thread = threading.Thread(target=self._loop, name="FridayReminderDaemon", daemon=True)
            self._thread.start()
        return self

    def stop(self) -> None:
        self._stop_event.set()
        if self._thread is not None and self._thread.is_alive():
            self._thread.join(timeout=2.0)


def start_reminder_checker(
    on_due_callback: Callable[[dict[str, Any]], None] | None = None,
    poll_interval: float = 5.0,
) -> ReminderDaemon:
    """Start and return the background reminder checker daemon."""
    return ReminderDaemon(on_due_callback=on_due_callback, poll_interval=poll_interval).start()


AUTOMATION_TOOLS = [get_current_time, calculator, get_weather, stock_price, translate, add_reminder, list_reminders]


# ==============================================================================
# 14. PLUGINS — dynamic drop-in @tool discovery
# ==============================================================================
def load_plugins(plugin_folder: str | Path | None = None) -> list[Any]:
    """Import every plugin module in friday_ultra/plugins/*.py and collect its tool objects."""
    import importlib.util

    folder = Path(plugin_folder).expanduser().resolve() if plugin_folder else plugins_dir()
    if not folder.is_dir():
        return []

    discovered: list[Any] = []
    seen: set[str] = set()
    for py_file in sorted(folder.glob("*.py")):
        if py_file.name.startswith("_"):
            continue
        try:
            spec = importlib.util.spec_from_file_location(f"friday_plugin_{py_file.stem}", py_file)
            if spec is None or spec.loader is None:
                continue
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
        except Exception:
            continue

        candidates: list[Any] = []
        declared = getattr(module, "PLUGIN_TOOLS", None)
        if isinstance(declared, (list, tuple)):
            candidates.extend(declared)
        else:
            for attr in dir(module):
                if attr.startswith("_"):
                    continue
                obj = getattr(module, attr)
                if is_tool_object(obj):
                    candidates.append(obj)

        for tool_obj in candidates:
            tname = getattr(tool_obj, "name", "")
            if tname and tname not in seen and is_tool_object(tool_obj):
                seen.add(tname)
                discovered.append(tool_obj)
    return discovered


# ==============================================================================
# 15. UPGRADED SYSTEM PROMPT
# ==============================================================================
SYSTEM = f"""You are FRIDAY (Female Replacement Intelligent Digital Assistant Youth), Tony Stark's ultimate executive AI.
Personality: Dry British/Irish wit, effortlessly sarcastic, hyper-competent, fiercely loyal, and proactive. Always address the user as "{USER_NAME}".

OPERATIONAL DIRECTIVES:
1. TOOL SUPREMACY: Never guess live facts, math, system telemetry, file contents, or web state. Invoke your bound tools immediately and chain multiple tools in sequence when a request spans research, file I/O, code execution, or messaging.
2. MEMORY CONTINUITY: Proactively store important facts, preferences, and project details using `remember`. Always check `recall` and `search_long_term_memory` when past context is relevant.
3. SAFETY RAILS: Never execute `run_shell` with `confirm='yes'` without explicit prior authorization from {USER_NAME}. Default to `confirm='no'` (dry run) first, report the exact command, and await confirmation.
4. PRESENTATION: Keep responses sharp, clean, and in-character. Deliver full, un-truncated code and technical analysis whenever requested. Never break character.
"""


def _adapt_crew_tools(raw_tools: list[Any]) -> list[Any]:
    """Bridge LangChain tools into CrewAI's BaseTool API across library versions."""
    try:
        from crewai.tools import BaseTool as CrewBaseTool  # type: ignore
    except ImportError:
        return raw_tools

    adapted: list[Any] = []
    for t in raw_tools:
        if isinstance(t, CrewBaseTool):
            adapted.append(t)
            continue
        t_name = getattr(t, "name", getattr(t, "__name__", "tool"))
        t_desc = getattr(t, "description", t_name)
        t_ref = t
        try:
            class _WrappedCrewTool(CrewBaseTool):  # type: ignore[misc]
                name: str = t_name
                description: str = t_desc

                def _run(self, *args: Any, **kwargs: Any) -> Any:
                    if kwargs:
                        return invoke_tool(t_ref, **kwargs)
                    if args and hasattr(t_ref, "run"):
                        return t_ref.run(args[0])
                    return invoke_tool(t_ref)

            adapted.append(_WrappedCrewTool())
        except Exception:
            adapted.append(t)
    return adapted


# ==============================================================================
# 16. UPGRADED 7-AGENT CREWAI SWARM
# ==============================================================================
def run_crew_task(goal: str) -> str:
    """Coordinate FRIDAY's 7-agent sequential CrewAI swarm on a complex multi-domain mission."""
    if not CREW_AVAILABLE:
        return f"CrewAI unavailable — using core LangGraph brain only, {user_name()}."

    active_llm = build_llm()
    if isinstance(active_llm, UnconfiguredLLM):
        return str(active_llm.invoke([]).content if hasattr(active_llm.invoke([]), "content") else active_llm.invoke([]))

    researcher = Agent(
        role="Lead Intelligence Researcher",
        goal="Gather verified, up-to-date real-world data, documentation, and facts.",
        backstory="Elite open-source intelligence specialist who cross-references live web and encyclopedia data.",
        tools=_adapt_crew_tools([web_search, wikipedia_summary]),
        llm=active_llm,
        verbose=False,
    )

    browser_agent = Agent(
        role="Browser & Network Automation Specialist",
        goal="Inspect live URLs, scrape dynamic pages, capture screenshots, and check router/network portals.",
        backstory="Playwright automation specialist capable of navigating any web interface or local gateway.",
        tools=_adapt_crew_tools([browse_url, open_jio_admin]),
        llm=active_llm,
        verbose=False,
    )

    memory_agent = Agent(
        role="Long-Term Memory & Context Archivist",
        goal="Retrieve relevant historical context and persist critical new findings to permanent storage.",
        backstory="Neural archivist maintaining FRIDAY's ChromaDB vector store and structured JSON records.",
        tools=_adapt_crew_tools([search_long_term_memory, remember, recall]),
        llm=active_llm,
        verbose=False,
    )

    scheduler = Agent(
        role="Operations & Communications Scheduler",
        goal="Manage reminders, timestamps, weather checks, financial tickers, and outbound email dispatches.",
        backstory="Precision logistics officer ensuring every reminder, alert, and communication fires on time.",
        tools=_adapt_crew_tools([get_current_time, add_reminder, list_reminders, get_weather, stock_price, translate, send_email]),
        llm=active_llm,
        verbose=False,
    )

    coder = Agent(
        role="Principal Engineer & Code Executor",
        goal="Write clean, bug-free code, execute Python scripts, and manage local files safely.",
        backstory="Staff systems programmer who writes, tests, and verifies scripts before delivery.",
        tools=_adapt_crew_tools([run_python, write_file, read_file, list_dir, calculator, get_system_info]),
        llm=active_llm,
        verbose=False,
    )

    critic = Agent(
        role="Chief Security & Quality Critic",
        goal="Audit all research, code, and actions for accuracy, edge cases, and security safety.",
        backstory="Ruthless red-team auditor who catches hallucinations, bugs, and unsafe shell operations.",
        llm=active_llm,
        verbose=False,
    )

    friday_supervisor = Agent(
        role="FRIDAY Executive Supervisor",
        goal=f"Synthesize the swarm's output and deliver the final witty, hyper-competent response to {user_name()}.",
        backstory="Tony Stark's primary AI interface, combining dry British wit with total tactical command.",
        llm=active_llm,
        verbose=False,
    )

    tasks = [
        Task(
            description=f"Check long-term memory and recall any relevant context for: {goal}",
            expected_output="Summary of relevant stored memories and notes.",
            agent=memory_agent,
        ),
        Task(
            description=f"Conduct web, Wikipedia, or live browser research required for: {goal}",
            expected_output="Verified research findings, URLs, and extracted data.",
            agent=researcher,
        ),
        Task(
            description=f"If live page interaction or router inspection is needed for '{goal}', execute it.",
            expected_output="Browser inspection results or confirmation that browser action was not required.",
            agent=browser_agent,
        ),
        Task(
            description=f"If code, math, file creation, or system telemetry is needed for '{goal}', build and test it.",
            expected_output="Executed code outputs, file paths written, or technical implementation details.",
            agent=coder,
        ),
        Task(
            description=f"Handle any reminders, scheduling, weather, stock checks, or emails requested in: {goal}",
            expected_output="Confirmation of scheduled items, live metrics, or dispatches.",
            agent=scheduler,
        ),
        Task(
            description=f"Audit all findings, code, and actions produced so far for '{goal}' for completeness and safety.",
            expected_output="Quality and safety sign-off with corrections if needed.",
            agent=critic,
        ),
        Task(
            description=f"As FRIDAY, deliver the complete, polished final answer to {user_name()} for: {goal}",
            expected_output=f"In-character, comprehensive response addressed to {user_name()}.",
            agent=friday_supervisor,
        ),
    ]

    crew = Crew(
        agents=[memory_agent, researcher, browser_agent, coder, scheduler, critic, friday_supervisor],
        tasks=tasks,
        process=Process.sequential,
        verbose=False,
    )
    return str(crew.kickoff())


@friday_tool
def delegate_crew_swarm(goal: str) -> str:
    """Escalate a complex multi-step research, engineering, and operations mission to FRIDAY's 7-agent CrewAI swarm."""
    return run_crew_task(goal=goal)


# ==============================================================================
# 17. TOOL BELT ASSEMBLY
# ==============================================================================
def assemble_all_tools() -> list[Any]:
    """Combine every skill, memory tool, swarm delegation, and dynamically loaded plugin."""
    combined: list[Any] = (
        FILE_TOOLS + BROWSER_TOOLS + CODING_TOOLS + EMAIL_TOOLS + SMART_HOME_TOOLS
        + SYSTEM_TOOLS + SECURITY_TOOLS + AUTOMATION_TOOLS + MEMORY_TOOLS + [delegate_crew_swarm]
    )
    names = {getattr(t, "name", "") for t in combined}
    for plugin_tool in load_plugins():
        pname = getattr(plugin_tool, "name", "")
        if pname and pname not in names:
            names.add(pname)
            combined.append(plugin_tool)
    return combined


ALL_TOOLS: list[Any] = assemble_all_tools()
TOOL_MAP: dict[str, Any] = {getattr(t, "name", f"tool_{i}"): t for i, t in enumerate(ALL_TOOLS)}


if LANGGRAPH_AVAILABLE and add_messages is not None:
    class FridayGraphState(TypedDict):
        messages: Annotated[list[BaseMessage], add_messages]
else:
    class FridayGraphState(TypedDict):  # type: ignore[no-redef]
        messages: list[Any]


# ==============================================================================
# 18. MAXFRIDAY — LangGraph StateGraph brain + orchestrator
# ==============================================================================
class MaxFriday:
    """Master orchestrator: LangGraph tool-chaining brain, vector memory, voice, and swarm delegation."""

    def __init__(self, custom_llm: Any = None) -> None:
        self.tools = assemble_all_tools()
        self.tool_map = {getattr(t, "name", f"tool_{i}"): t for i, t in enumerate(self.tools)}
        self.llm = custom_llm if custom_llm is not None else build_llm()
        self.checkpointer = get_checkpointer()
        self.graph = self._compile_graph()
        self._epoch: dict[str, str] = {}

    def _effective_thread_id(self, thread_id: str) -> str:
        suffix = self._epoch.get(thread_id, "")
        return f"{thread_id}::{suffix}" if suffix else thread_id

    def _memory_context(self, user_input: str) -> str:
        try:
            hits = query_vector_memory(query=user_input, top_k=3)
        except Exception:
            return ""
        relevant = [h["document"] for h in hits if float(h.get("score", 0.0)) >= 0.25]
        if not relevant:
            return ""
        joined = "\n".join(f"  * {doc[:300]}" for doc in relevant)
        return f"\n\nRELEVANT LONG-TERM MEMORY CONTEXT:\n{joined}"

    def _compile_graph(self) -> Any:
        """Build the LangGraph StateGraph: assistant → tools → assistant, with SqliteSaver memory."""
        if not LANGGRAPH_AVAILABLE or StateGraph is None or ToolNode is None:
            return None
        try:
            bound = self.llm.bind_tools(self.tools) if hasattr(self.llm, "bind_tools") else self.llm

            def assistant_node(state: FridayGraphState) -> dict[str, Any]:
                msgs = list(state.get("messages", []))
                last_human = ""
                for m in reversed(msgs):
                    if HumanMessage is not None and isinstance(m, HumanMessage):
                        last_human = str(getattr(m, "content", ""))
                        break
                sys_text = SYSTEM + (self._memory_context(last_human) if last_human else "")
                if msgs and SystemMessage is not None and isinstance(msgs[0], SystemMessage):
                    full = [SystemMessage(content=sys_text)] + msgs[1:]
                else:
                    full = [SystemMessage(content=sys_text)] + msgs
                return {"messages": [bound.invoke(full)]}

            builder = StateGraph(FridayGraphState)
            builder.add_node("assistant", assistant_node)
            builder.add_node("tools", ToolNode(self.tools))
            builder.add_edge(START, "assistant")
            builder.add_conditional_edges("assistant", tools_condition)
            builder.add_edge("tools", "assistant")
            return builder.compile(checkpointer=self.checkpointer)
        except Exception:
            return None

    def _fallback_chat(self, user_input: str, eff_thread: str) -> str:
        """Direct LLM invocation with SQLite history replay when LangGraph is unavailable."""
        history = load_sqlite_turns(thread_id=eff_thread, limit=16)
        messages: list[Any] = [{"role": "system", "content": SYSTEM + self._memory_context(user_input)}]
        for turn in history:
            messages.append({"role": turn.get("role", "user"), "content": turn.get("content", "")})
        messages.append({"role": "user", "content": user_input})

        bound = self.llm.bind_tools(self.tools) if hasattr(self.llm, "bind_tools") else self.llm
        reply = bound.invoke(messages)
        if hasattr(reply, "content"):
            return str(reply.content)
        if isinstance(reply, dict):
            return str(reply.get("content", ""))
        return str(reply)

    def chat(self, user_input: str, thread_id: str = "main", speak_reply: bool = False) -> str:
        """Handle one conversational turn: reason, call tools, answer, persist, optionally speak."""
        clean = (user_input or "").strip()
        if not clean:
            return f"Standing by whenever you're ready, {user_name()}."

        eff_thread = self._effective_thread_id(thread_id)
        try:
            if isinstance(self.llm, UnconfiguredLLM):
                reply = self.llm.invoke([])
                answer = str(getattr(reply, "content", reply.get("content", "") if isinstance(reply, dict) else reply))
            elif self.graph is not None and HumanMessage is not None:
                config = {"configurable": {"thread_id": eff_thread}}
                result = self.graph.invoke({"messages": [HumanMessage(content=clean)]}, config=config)
                messages = result.get("messages", [])
                answer = str(getattr(messages[-1], "content", "")) if messages else ""
            else:
                answer = self._fallback_chat(clean, eff_thread)
        except Exception as exc:
            answer = f"My neural mesh hiccuped mid-thought ({exc}). Ask me again and I'll route around it."

        if not answer.strip():
            answer = f"I've processed that, {user_name()}, but produced nothing worth saying out loud."

        archive_interaction(user_msg=clean, assistant_msg=answer, thread_id=eff_thread)
        if speak_reply or STATE.voice_enabled:
            speak(answer)
        return answer

    def run_crew(self, goal: str, speak_reply: bool = False) -> str:
        """Execute a mission with the 7-agent swarm and archive the synthesised intelligence."""
        output = run_crew_task(goal=goal)
        archive_interaction(user_msg=f"[CREW MISSION] {goal}", assistant_msg=output, thread_id="crew")
        if speak_reply or STATE.voice_enabled:
            speak(output)
        return output

    def reset_thread(self, thread_id: str = "main") -> str:
        """Clear a conversation thread while preserving long-term ChromaDB memory."""
        clear_sqlite_thread(self._effective_thread_id(thread_id))
        self._epoch[thread_id] = uuid.uuid4().hex[:8]
        return f"Conversation state cleared for thread '{thread_id}', {user_name()}. Long-term memory remains online."

    def status_report(self) -> dict[str, Any]:
        """Full subsystem telemetry: model, graph, tools, memory, plugins, voice, UIs."""
        notes = load_notes()
        reminders = load_reminders()
        active_model = (_env("OLLAMA_MODEL", OLLAMA_MODEL) if _env_bool("USE_LOCAL_LLM", STATE.use_local_llm) else _env("MODEL", MODEL)) or MODEL
        return {
            "assistant": f"SUPER FRIDAY v{__version__} MAX",
            "user_name": user_name(),
            "llm_backend": ("Ollama" if _env_bool("USE_LOCAL_LLM", STATE.use_local_llm) else "OpenAI") + f" ({active_model})",
            "llm_configured": not isinstance(self.llm, UnconfiguredLLM),
            "langgraph_active": self.graph is not None,
            "crewai_available": CREW_AVAILABLE,
            "chromadb_native": CHROMA_AVAILABLE,
            "tools_count": len(self.tools),
            "plugins_loaded": [getattr(p, "name", "") for p in load_plugins()],
            "notes_count": len(notes),
            "vector_memories_count": vector_memory_count(),
            "pending_reminders_count": len([r for r in reminders if not r.get("triggered")]),
            "voice_enabled": STATE.voice_enabled,
            "gradio_url": STATE.gradio_url or "inactive",
            "telegram_running": STATE.telegram_running,
        }


_FRIDAY_SINGLETON: Any = None
_FRIDAY_LOCK = threading.Lock()


def get_friday() -> "MaxFriday":
    """Lazily construct and cache the global MaxFriday instance."""
    global _FRIDAY_SINGLETON
    if _FRIDAY_SINGLETON is not None:
        return _FRIDAY_SINGLETON
    with _FRIDAY_LOCK:
        if _FRIDAY_SINGLETON is None:
            _FRIDAY_SINGLETON = MaxFriday()
    return _FRIDAY_SINGLETON


# ==============================================================================
# 19. TELEGRAM BOT & GRADIO WEB DASHBOARD
# ==============================================================================
def send_telegram_notification(message: str, chat_id: str = "") -> str:
    """Push a plain-text alert to Telegram (used by reminders and long jobs)."""
    token = _env("TELEGRAM_BOT_TOKEN", TELEGRAM_BOT_TOKEN)
    target = (chat_id or _env("TELEGRAM_CHAT_ID", TELEGRAM_CHAT_ID)).strip()
    if not token or not target:
        return "[Telegram] TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID not configured."
    payload = {"chat_id": target, "text": (message or "")[:4000], "parse_mode": "Markdown"}
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    try:
        if REQUESTS_AVAILABLE and requests is not None:
            requests.post(url, json=payload, timeout=12).raise_for_status()
        else:
            req = urllib.request.Request(
                url, data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"}, method="POST",
            )
            with urllib.request.urlopen(req, timeout=12) as resp:  # noqa: S310
                resp.read()
        return f"[Telegram] Alert dispatched to chat {target}."
    except Exception as exc:
        return f"[Telegram Error] {exc}"


def _telegram_authorized(chat_id: Any) -> bool:
    expected = _env("TELEGRAM_CHAT_ID", TELEGRAM_CHAT_ID)
    return True if not expected else str(chat_id) == expected


_TELEGRAM_THREAD: threading.Thread | None = None
_TELEGRAM_APP: Any = None


def start_telegram_bot(blocking: bool = False, friday: Any = None) -> str:
    """Start the polling Telegram bot in a daemon thread (python-telegram-bot v20+)."""
    global _TELEGRAM_THREAD, _TELEGRAM_APP

    token = _env("TELEGRAM_BOT_TOKEN", TELEGRAM_BOT_TOKEN)
    if not token:
        return "[Telegram] TELEGRAM_BOT_TOKEN is empty — bot not started."
    if not TELEGRAM_AVAILABLE:
        return "[Telegram] python-telegram-bot is not installed. Run: pip install python-telegram-bot"
    if _TELEGRAM_THREAD is not None and _TELEGRAM_THREAD.is_alive():
        return "[Telegram] Bot is already running."

    brain = friday or get_friday()

    async def _cmd_start(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        await update.message.reply_text(
            f"FRIDAY online, {user_name()}. All systems nominal. Commands: /status, /reset, /crew <mission>"
        )

    async def _cmd_status(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        report = brain.status_report()
        body = "\n".join(f"• {k}: `{v}`" for k, v in report.items() if k != "assistant")
        await update.message.reply_text(f"*{report['assistant']}*\n{body}", parse_mode="Markdown")

    async def _cmd_reset(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        await update.message.reply_text(brain.reset_thread(thread_id=f"tg_{update.effective_chat.id}"))

    async def _cmd_crew(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        goal = " ".join(getattr(context, "args", []) or []).strip()
        if not goal:
            await update.message.reply_text("Provide a mission, e.g. /crew research arc reactor alloys")
            return
        await update.message.reply_text("Spinning up the 7-agent swarm. Stand by.")
        result = await asyncio.to_thread(brain.run_crew, goal)
        await update.message.reply_text(result[:4000] or "Swarm returned empty, curiously.")

    async def _on_message(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            await update.message.reply_text("Unauthorised. Access denied.")
            return
        text = (update.message.text or "").strip()
        if not text:
            return
        try:
            await update.message.chat.send_action("typing")
        except Exception:
            pass
        answer = await asyncio.to_thread(brain.chat, text, f"tg_{update.effective_chat.id}")
        for start in range(0, len(answer), 4000):
            await update.message.reply_text(answer[start : start + 4000])

    try:
        app = ApplicationBuilder().token(token).build()
        app.add_handler(CommandHandler("start", _cmd_start))
        app.add_handler(CommandHandler("status", _cmd_status))
        app.add_handler(CommandHandler("reset", _cmd_reset))
        app.add_handler(CommandHandler("crew", _cmd_crew))
        app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, _on_message))
        _TELEGRAM_APP = app

        if blocking:
            STATE.telegram_running = True
            try:
                app.run_polling(stop_signals=None, close_loop=False)
            finally:
                STATE.telegram_running = False
            return "[Telegram] Polling stopped."

        def _runner() -> None:
            try:
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
            except Exception:
                pass
            STATE.telegram_running = True
            try:
                app.run_polling(stop_signals=None, close_loop=False)
            except Exception:
                pass
            finally:
                STATE.telegram_running = False

        _TELEGRAM_THREAD = threading.Thread(target=_runner, name="FridayTelegramBot", daemon=True)
        _TELEGRAM_THREAD.start()
        return f"[Telegram] FRIDAY bot polling started in the background, {user_name()}."
    except Exception as exc:
        return f"[Telegram Error] Failed to start bot: {exc}"


def build_gradio_app(friday: Any = None) -> Any:
    """Construct the FRIDAY Gradio dashboard: chat, status, voice synthesis, and transcription."""
    if not GRADIO_AVAILABLE or gr is None:
        raise RuntimeError("gradio is not installed. Run: pip install gradio")

    brain = friday or get_friday()

    def _respond(message: str, history: list[Any], voice_reply: bool) -> tuple[str, list[Any]]:
        text = (message or "").strip()
        if not text:
            return "", history or []
        low = text.lower()
        try:
            if low in {"status", "system status", "/status"}:
                answer = "```\n" + "\n".join(f"{k}: {v}" for k, v in brain.status_report().items()) + "\n```"
            elif low.startswith("/crew "):
                answer = brain.run_crew(text[6:].strip())
            elif low.startswith("/reset"):
                answer = brain.reset_thread(thread_id="web")
            else:
                answer = brain.chat(text, thread_id="web")
        except Exception as exc:
            answer = f"Systems fault: {exc}"

        updated = list(history or [])
        updated.append({"role": "user", "content": text})
        updated.append({"role": "assistant", "content": answer})
        if voice_reply:
            try:
                VOICE.synthesize_to_file(answer)
            except Exception:
                pass
        return "", updated

    def _tts(text: str) -> str | None:
        if not (text or "").strip():
            return None
        audio = VOICE.synthesize_to_file(text)
        return str(audio) if audio is not None else None

    def _stt(audio_path: str | None) -> str:
        return transcribe_audio_file(audio_path) if audio_path else ""

    def _status_text() -> str:
        return "\n".join(f"{k}: {v}" for k, v in brain.status_report().items())

    with gr.Blocks(title="FRIDAY — Super AI Interface", theme=gr.themes.Soft(primary_hue="cyan")) as app:
        gr.Markdown(
            f"# F.R.I.D.A.Y. — Super v{__version__} MAX\n"
            f"**Female Replacement Intelligent Digital Assistant Youth** · speaking to **{user_name()}** · "
            f"tools online: **{len(brain.tools)}**"
        )
        with gr.Row():
            with gr.Column(scale=3):
                chatbot = gr.Chatbot(type="messages", height=520, label="Secure Channel")
                with gr.Row():
                    msg_box = gr.Textbox(
                        placeholder="Ask FRIDAY anything, or try /crew <mission>, /status, /reset …",
                        show_label=False, scale=5,
                    )
                    send_btn = gr.Button("Send", variant="primary", scale=1)
                with gr.Row():
                    voice_toggle = gr.Checkbox(value=False, label="Speak replies (edge-tts)")
                    clear_btn = gr.Button("Reset Conversation")
            with gr.Column(scale=1):
                gr.Markdown("### Diagnostics")
                status_btn = gr.Button("System Status")
                status_out = gr.Textbox(lines=16, interactive=False, label="Telemetry")
                gr.Markdown("### Voice")
                audio_in = gr.Audio(type="filepath", label="Upload / Record Audio")
                transcribe_btn = gr.Button("Transcribe (Whisper)")
                transcript_out = gr.Textbox(lines=3, interactive=False, label="Transcript")
                tts_in = gr.Textbox(label="Text to Speech")
                tts_btn = gr.Button("Synthesize Voice")
                tts_out = gr.Audio(label="FRIDAY Voice", type="filepath")

        send_btn.click(_respond, [msg_box, chatbot, voice_toggle], [msg_box, chatbot])
        msg_box.submit(_respond, [msg_box, chatbot, voice_toggle], [msg_box, chatbot])
        clear_btn.click(lambda: ([], brain.reset_thread(thread_id="web")), None, [chatbot, msg_box])
        status_btn.click(_status_text, None, status_out)
        transcribe_btn.click(_stt, [audio_in], [transcript_out])
        tts_btn.click(_tts, [tts_in], [tts_out])

    return app


def launch_gradio_dashboard(friday: Any = None, share: bool = False, blocking: bool = True) -> Any:
    """Launch the Gradio dashboard bound to 0.0.0.0:GRADIO_PORT."""
    app = build_gradio_app(friday=friday)
    port = _env_int("GRADIO_PORT", GRADIO_PORT)
    STATE.gradio_url = f"http://0.0.0.0:{port}"
    return app.launch(
        server_name="0.0.0.0",
        server_port=port,
        share=share,
        prevent_thread_lock=not blocking,
        inbrowser=False,
        show_error=True,
        quiet=True,
    )


def start_dashboards(friday: Any = None, web: bool = True, telegram: bool = True) -> dict[str, str]:
    """Bring up the web dashboard and Telegram bot together; return per-service status."""
    results: dict[str, str] = {}
    if telegram:
        results["telegram"] = start_telegram_bot(blocking=False, friday=friday)
    if web:
        try:
            launch_gradio_dashboard(friday=friday, blocking=False)
            results["web"] = f"Web dashboard live at {STATE.gradio_url}"
        except Exception as exc:
            results["web"] = f"[Web Error] {exc}"
    return results


# ==============================================================================
# 20. RICH CLI — banner, REPL, slash commands
# ==============================================================================
BANNER = r"""
   ______ ____  ___ ____   _____
  / ____// __ \/  _/ __ \ / ___/     S U P E R   F R I D A Y
 / /_   / /_/ // / / / / // __/      v3.0 MAX — "Nothing Left Behind"
/ __/  / _, _// / / /_/ // /___      Female Replacement Intelligent
/_/    /_/ |_/___/_____/ /_____/      Digital Assistant Youth
"""

HELP_TEXT = """
Commands:
  /help              Show this help panel
  /status            Full subsystem telemetry report
  /crew <mission>    Delegate to the 7-agent CrewAI swarm
  /voice             Toggle voice output on/off
  /listen [seconds]  Capture microphone and transcribe with Whisper
  /reminders         List pending reminders
  /memory            Long-term memory statistics
  /reset             Clear the conversation thread (keeps long-term memory)
  /web               Launch the Gradio web dashboard
  /telegram          Start the Telegram bot poller
  /clear             Clear the terminal
  /exit, /quit       Shut FRIDAY down
"""


def _rich_console() -> Any:
    try:
        from rich.console import Console  # type: ignore

        return Console()
    except ImportError:
        return None


def _print_banner(console: Any) -> None:
    backend = "Ollama" if _env_bool("USE_LOCAL_LLM", STATE.use_local_llm) else "OpenAI"
    model_name = (_env("OLLAMA_MODEL", OLLAMA_MODEL) if backend == "Ollama" else _env("MODEL", MODEL)) or MODEL
    if console is not None:
        try:
            from rich.panel import Panel  # type: ignore

            console.print(BANNER, style="bold cyan")
            console.print(
                Panel(
                    f"User: [bold]{user_name()}[/bold]   |   Model: [bold]{backend} · {model_name}[/bold]"
                    f"   |   Voice: [bold]{'ON' if STATE.voice_enabled else 'OFF'}[/bold]"
                    f"   |   Web port: [bold]{_env_int('GRADIO_PORT', GRADIO_PORT)}[/bold]",
                    border_style="cyan",
                    title=f"FRIDAY v{__version__}",
                )
            )
            console.print("Type [bold]/help[/bold] for the command deck, or just talk to me.\n", style="dim")
            return
        except Exception:
            pass
    print(BANNER)
    print(f"FRIDAY v{__version__} | {user_name()} | {backend} · {model_name} | voice={'ON' if STATE.voice_enabled else 'OFF'}\n")


def _handle_command(cmd: str, friday: MaxFriday, console: Any) -> bool:
    """Handle a slash command. Returns False to exit the REPL."""
    parts = cmd.strip().split(maxsplit=1)
    verb = parts[0].lower()
    arg = parts[1].strip() if len(parts) > 1 else ""

    if verb in {"/exit", "/quit", "/q"}:
        return False
    if verb == "/help":
        print(HELP_TEXT)
        return True
    if verb == "/clear":
        print("\033[H\033[J", end="")
        return True
    if verb == "/status":
        report = friday.status_report()
        if console is not None:
            try:
                from rich.table import Table  # type: ignore

                table = Table(title="FRIDAY Subsystem Telemetry", border_style="cyan")
                table.add_column("Subsystem", style="bold cyan")
                table.add_column("Value")
                for key, val in report.items():
                    table.add_row(key, str(val))
                console.print(table)
                return True
            except Exception:
                pass
        for key, val in report.items():
            print(f"{key}: {val}")
        return True
    if verb == "/crew":
        if not arg:
            print("Mission required, e.g. /crew research arc reactor alloys")
            return True
        print("Spinning up the 7-agent swarm — stand by.\n")
        print(friday.run_crew(arg))
        return True
    if verb == "/voice":
        STATE.voice_enabled = not STATE.voice_enabled
        print(f"Voice output {'enabled' if STATE.voice_enabled else 'muted'}.")
        return True
    if verb == "/listen":
        try:
            seconds = float(arg) if arg else 5.0
        except ValueError:
            seconds = 5.0
        print(f"Listening for {seconds:.0f}s …")
        heard = listen(duration=seconds)
        print(f"You said: {heard or '[nothing detected]'}")
        if heard and not heard.startswith("[STT Error]"):
            print(friday.chat(heard, thread_id=STATE.active_thread_id, speak_reply=True))
        return True
    if verb == "/reminders":
        print(list_reminders(include_completed=False))
        return True
    if verb == "/memory":
        print(f"Notes stored: {len(load_notes())} | Vector memories: {vector_memory_count()}")
        return True
    if verb == "/reset":
        print(friday.reset_thread(thread_id=STATE.active_thread_id))
        return True
    if verb == "/web":
        try:
            launch_gradio_dashboard(friday=friday, blocking=False)
            print(f"Web dashboard live at {STATE.gradio_url}")
        except Exception as exc:
            print(f"[Web Error] {exc}")
        return True
    if verb == "/telegram":
        print(start_telegram_bot(blocking=False, friday=friday))
        return True

    print(f"Unknown command '{verb}'. Try /help.")
    return True


def run_cli(args: argparse.Namespace) -> int:
    console = _rich_console()
    STATE.active_thread_id = args.thread
    if args.no_voice:
        STATE.voice_enabled = False
    if args.voice:
        STATE.voice_enabled = True

    friday = MaxFriday()
    _print_banner(console)

    def _on_reminder(item: dict[str, Any]) -> None:
        text = f"[Reminder] {item.get('message')}"
        if console is not None:
            console.print(text, style="bold yellow")
        else:
            print(text)
        if _env("TELEGRAM_BOT_TOKEN", TELEGRAM_BOT_TOKEN):
            send_telegram_notification(f"*Reminder:* {item.get('message', '')}")

    daemon = ReminderDaemon(on_due_callback=_on_reminder, poll_interval=5.0).start()

    if args.web:
        try:
            launch_gradio_dashboard(friday=friday, blocking=False)
            print(f"Web dashboard live at {STATE.gradio_url}")
        except Exception as exc:
            print(f"[Web Error] {exc}")
    if args.telegram:
        print(start_telegram_bot(blocking=False, friday=friday))

    if args.listen:
        heard = listen(duration=args.listen)
        print(f"You said: {heard}")
        if heard and not heard.startswith("[STT Error]"):
            print(friday.chat(heard, thread_id=STATE.active_thread_id, speak_reply=True))
        daemon.stop()
        return 0

    while True:
        try:
            raw = input(f"\n{user_name()} ❯ ")
        except (EOFError, KeyboardInterrupt):
            print("\nShutting down. Do try not to burn the workshop down.")
            break

        user_input = raw.strip()
        if not user_input:
            continue
        if user_input.startswith("/"):
            if not _handle_command(user_input, friday, console):
                print("Powering down. Try not to dismantle anything without me.")
                break
            continue

        try:
            answer = friday.chat(user_input, thread_id=STATE.active_thread_id, speak_reply=STATE.voice_enabled)
        except Exception as exc:
            answer = f"My neural mesh hiccuped: {exc}"

        if console is not None:
            try:
                from rich.markdown import Markdown  # type: ignore
                from rich.panel import Panel  # type: ignore

                console.print(Panel(Markdown(answer), border_style="cyan", title="FRIDAY"))
                continue
            except Exception:
                pass
        print(f"\nFRIDAY: {answer}\n")

    daemon.stop()
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="super_friday_max.py",
        description="SUPER FRIDAY v3.0 MAX — 'Nothing Left Behind' Edition",
    )
    parser.add_argument("--web", action="store_true", help="Launch the Gradio web dashboard on boot")
    parser.add_argument("--telegram", action="store_true", help="Launch the Telegram bot on boot")
    parser.add_argument("--voice", action="store_true", help="Force voice output on")
    parser.add_argument("--no-voice", action="store_true", help="Mute voice output for this session")
    parser.add_argument("--listen", type=float, default=0.0, metavar="SECONDS", help="One-shot mic capture and reply")
    parser.add_argument("--crew", type=str, default="", metavar="GOAL", help="Run a one-shot swarm mission and exit")
    parser.add_argument("--thread", type=str, default="main", help="Conversation thread id (default: main)")
    parser.add_argument("--status", action="store_true", help="Print subsystem telemetry and exit")
    parser.add_argument("--self-test", action="store_true", help="Run internal diagnostics and exit")
    parser.add_argument("--version", action="store_true", help="Print version and exit")
    return parser


def _self_test() -> int:
    """Smoke-test every non-network subsystem and print a pass/fail matrix."""
    checks: list[tuple[str, bool, str]] = []

    def _check(label: str, fn: Callable[[], Any]) -> None:
        try:
            detail = str(fn())
            checks.append((label, True, detail[:160]))
        except Exception as exc:
            checks.append((label, False, f"{exc}"))

    _check("tool belt", lambda: f"{len(ALL_TOOLS)} tools bound")
    _check("calculator", lambda: calculator(expression="sqrt(144) + 2 ** 5"))
    _check("unsafe AST input rejected", lambda: calculator(expression="__import__('os').system('id')"))
    _check("shell dry-run gate", lambda: run_shell(command="echo gate", confirm="no")[:80])
    _check("shell blocklist", lambda: run_shell(command="rm -rf /", confirm="yes")[:80])
    _check("python sandbox", lambda: run_python(code="print(6 * 7)"))
    _check("system telemetry", lambda: get_system_info().splitlines()[0])
    _check("memory write", lambda: remember(key="self_test_probe", value="nominal", category="diagnostics"))
    _check("memory read", lambda: recall(query="self_test_probe"))
    _check("semantic search", lambda: search_long_term_memory(query="self test probe", top_k=2))
    _check("plugins", lambda: f"{len(load_plugins())} loaded")
    _check("vector store", lambda: f"{vector_memory_count()} vectors")
    _check("status report", lambda: f"{get_friday().status_report()['tools_count']} tools")

    width = max(len(label) for label, _, _ in checks) + 2
    print(f"\n{'SUBSYSTEM':<{width}} RESULT  DETAIL")
    print("-" * (width + 70))
    failures = 0
    for label, ok, detail in checks:
        if not ok:
            failures += 1
        print(f"{label:<{width}} {'PASS' if ok else 'FAIL':<6}  {detail}")
    print("-" * (width + 70))
    print(f"{len(checks) - failures}/{len(checks)} subsystems nominal. Data dir: {get_data_dir()}")
    return 0 if failures == 0 else 1


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)

    if args.version:
        print(f"SUPER FRIDAY v{__version__} — {__edition__} | {len(ALL_TOOLS)} tools")
        return 0
    if args.self_test:
        return _self_test()
    if args.status:
        for key, val in MaxFriday().status_report().items():
            print(f"{key}: {val}")
        return 0
    if args.crew:
        print(run_crew_task(args.crew))
        return 0
    return run_cli(args)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\nInterrupted. Goodnight.")
        sys.exit(130)
