"""JARVIS Brain — production respond() with optional tool-calling loop.

Kotlin calls:
  respond(payload_json) -> reply_json
  plan_goal(title, details) -> steps_json
  set_llm(handle)
  install_tool_dispatcher(bridge)
  set_tool_catalog(catalog_json)
"""
from __future__ import annotations

import json
import re
from typing import Any, Dict, List, Optional

from language import detect_language
from memory import Memory
from planner import plan
from utils import load_prompt

_mem = Memory()
_llm: Any = None
_tool_catalog: List[Dict[str, Any]] = []
_MAX_TOOL_ROUNDS = 4

_TOOL_CALL_RE = re.compile(
    r"```tool\s*(\{.*?\})\s*```",
    re.DOTALL | re.IGNORECASE,
)
_TOOL_CALL_RE2 = re.compile(
    r"<tool_call>\s*(\{.*?\})\s*</tool_call>",
    re.DOTALL | re.IGNORECASE,
)


def set_llm(handle: Any) -> None:
    global _llm
    _llm = handle


def install_tool_dispatcher(_bridge: Any) -> None:
    """Compatibility hook. Python has no command execution authority."""
    return None


def set_tool_catalog(catalog_json: str) -> None:
    global _tool_catalog
    try:
        _tool_catalog = json.loads(catalog_json)
    except Exception:
        _tool_catalog = []


def _llm_generate(prompt: str, max_tokens: int = 700, temperature: float = 0.2) -> str:
    if _llm is None:
        return ""
    try:
        return _llm.generate(prompt, max_tokens=max_tokens, temperature=temperature) or ""
    except Exception:
        return ""


def _tool_block() -> str:
    if not _tool_catalog:
        return ""
    lines = ["# AVAILABLE TOOLS"]
    for t in _tool_catalog[:80]:
        name = t.get("name", "")
        desc = t.get("description_en") or t.get("description_hi") or ""
        params = t.get("parameters") or {}
        lines.append(
            f"- {name}: {desc}; parameters="
            f"{json.dumps(params, ensure_ascii=False, sort_keys=True)}"
        )
    return "\n".join(lines)



def plan_command(payload_json: str) -> str:
    payload = json.loads(payload_json)
    text = str(payload.get("userText") or "").strip()
    lang = str(payload.get("detectedLanguage") or detect_language(text))
    history = payload.get("recentMessages", [])[-8:]
    lessons = payload.get("lessons", [])[-8:]
    system = load_prompt(f"system_{lang if lang in ('hi', 'en', 'hinglish') else 'hi'}")
    schema = {
        "understood": "one sentence describing the intended action",
        "language": lang,
        "actions": [{
            "id": "action-1",
            "tool": "exact_tool_name",
            "args": {},
            "expectedEffect": "specific effect after execution"
        }]
    }
    prompt = (
        f"{system}\n\n"
        "You are JARVIS command planner. Return ONLY JSON. Never execute a tool. "
        "Use only exact tool names from the catalog. Return actions=[] for conversation "
        "that does not require an action. Make every argument explicit and deterministic.\n\n"
        f"# SCHEMA\n{json.dumps(schema, ensure_ascii=False)}\n\n"
        f"# TOOLS\n{_tool_block()}\n\n"
        f"# LESSONS\n{json.dumps(lessons, ensure_ascii=False)}\n\n"
        f"# HISTORY\n{json.dumps(history, ensure_ascii=False)}\n\n"
        f"# USER\n{text}\n\nJSON:"
    )
    raw = _llm_generate(prompt, max_tokens=900, temperature=0.05)
    return json.dumps(_normalize_plan(_extract_json_object(raw), lang), ensure_ascii=False)


def respond(payload_json: str) -> str:
    payload = json.loads(payload_json)
    text = str(payload.get("userText") or "").strip()
    lang = str(payload.get("detectedLanguage") or detect_language(text))
    history = payload.get("recentMessages", [])[-8:]
    lessons = payload.get("lessons", [])[-8:]
    system = load_prompt(f"system_{lang if lang in ('hi', 'en', 'hinglish') else 'hi'}")
    prompt = (
        f"{system}\n\n"
        "Answer conversationally. Do not execute tools and do not emit tool calls. "
        "Commands are handled by the Kotlin command engine.\n\n"
        f"# LESSONS\n{json.dumps(lessons, ensure_ascii=False)}\n\n"
        f"# HISTORY\n{json.dumps(history, ensure_ascii=False)}\n\n"
        f"# USER\n{text}\n\n# ASSISTANT\n"
    )
    reply = _llm_generate(prompt, max_tokens=700, temperature=0.2).strip()
    if not reply:
        reply = _fallback(lang, text)
    return json.dumps({"text": reply, "language": lang, "toolCalls": []}, ensure_ascii=False)

def plan_goal(title: str, description: str) -> str:
    steps = plan(_llm_generate, title, description)
    return json.dumps([s.__dict__ for s in steps], ensure_ascii=False)


def _fallback(lang: str, user_text: str = "") -> str:
    """Rule-based fallback when LLM is unavailable — still useful for demos."""
    t = (user_text or "").lower()
    # Minimal offline intents
    if any(k in t for k in ("battery", "बैटरी", "बैटरी")):
        return (
            "सर, बैटरी की जानकारी के लिए मुझे tool चाहिए — कृपया LLM मॉडल लोड करें।"
            if lang != "en"
            else "Sir, battery status needs the local model and tools loaded."
        )
    if any(k in t for k in ("time", "टाइम", "समय", "समय क्या")):
        from datetime import datetime

        now = datetime.now().strftime("%I:%M %p, %A")
        return f"सर, अभी लगभग {now} है।" if lang != "en" else f"Sir, it is about {now}."
    if lang == "en":
        return (
            "Sir, the on-device language model is not loaded yet. "
            "Please download the model from Settings, then try again."
        )
    return (
        "सर, ऑन-डिवाइस भाषा मॉडल अभी लोड नहीं है। "
        "सेटिंग्स से मॉडल डाउनलोड करें, फिर फिर से पूछें।"
    )
