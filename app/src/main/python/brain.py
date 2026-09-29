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
import hashlib
from typing import Any, Dict, List, Optional

from language import detect_language
from memory import Memory
from planner import plan
from utils import load_prompt

_mem = Memory()
_llm: Any = None
_tool_bridge: Any = None
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


def install_tool_dispatcher(bridge: Any) -> None:
    """Kotlin passes ToolBridgeKotlin instance; has .invoke(name, args_json)."""
    global _tool_bridge
    _tool_bridge = bridge
    try:
        import tools_bridge as tb

        def _dispatch(name: str, args_json: str) -> str:
            return str(bridge.invoke(name, args_json))

        tb.set_dispatcher(_dispatch)
    except Exception as exc:
        _tool_bridge = None
        raise RuntimeError(f"tool dispatcher installation failed: {exc}") from exc


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
    lines = ["# AVAILABLE TOOLS (call with ```tool {\"name\":\"...\",\"args\":{...}} ```)"]
    for t in _tool_catalog[:40]:
        name = t.get("name", "")
        desc = t.get("description_en") or t.get("description_hi") or ""
        lines.append(f"- {name}: {desc}")
    lines.append(
        "If you need a tool, output ONLY a tool block then wait. "
        "Otherwise answer the user directly in their language."
    )
    return "\n".join(lines)


def _parse_tool_calls(text: str) -> List[Dict[str, Any]]:
    calls: List[Dict[str, Any]] = []
    for rx in (_TOOL_CALL_RE, _TOOL_CALL_RE2):
        for m in rx.finditer(text or ""):
            try:
                obj = json.loads(m.group(1))
                if isinstance(obj, dict) and obj.get("name"):
                    calls.append(
                        {
                            "name": str(obj["name"]),
                            "args": obj.get("args") or obj.get("parameters") or {},
                        }
                    )
            except Exception:
                continue
    return calls


def _tool_call_id(name: str, args: Dict[str, Any]) -> str:
    canonical = json.dumps({"name": name, "args": args}, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]


def _run_tool(name: str, args: Dict[str, Any]) -> Dict[str, Any]:
    if _tool_bridge is None:
        return {"ok": False, "error": "no tool bridge", "code": "no_bridge"}
    try:
        raw = _tool_bridge.invoke(name, json.dumps(args, ensure_ascii=False))
        return json.loads(raw) if isinstance(raw, str) else {"ok": False, "error": "bad result"}
    except Exception as exc:
        return {"ok": False, "error": str(exc), "code": "tool_exception"}


def _extract_json_object(raw: str) -> Dict[str, Any]:
    text = (raw or "").strip()
    fenced = re.search(r"\\`\\`\\`(?:json)?\\s*(\\{.*?\\})\\s*\\`\\`\\`", text, re.DOTALL | re.IGNORECASE)
    candidate = fenced.group(1) if fenced else text
    try:
        value = json.loads(candidate)
        return value if isinstance(value, dict) else {}
    except Exception:
        pass
    match = re.search(r"\\{.*\\}", text, re.DOTALL)
    if match:
        try:
            value = json.loads(match.group(0))
            return value if isinstance(value, dict) else {}
        except Exception:
            pass
    return {}


def _normalize_plan(value: Dict[str, Any], lang: str) -> Dict[str, Any]:
    actions = value.get("actions")
    if not isinstance(actions, list):
        actions = []
    out = []
    seen = set()
    for i, raw in enumerate(actions[:8]):
        if not isinstance(raw, dict):
            continue
        tool = str(raw.get("tool") or "").strip()
        action_id = str(raw.get("id") or f"action-{i + 1}").strip()
        args = raw.get("args")
        if not tool or not action_id or action_id in seen or not isinstance(args, dict):
            continue
        seen.add(action_id)
        out.append({
            "id": action_id,
            "tool": tool,
            "args": args,
            "expectedEffect": str(raw.get("expectedEffect") or raw.get("expected_effect") or "").strip(),
        })
    return {
        "understood": str(value.get("understood") or "").strip(),
        "language": lang if lang in ("hi", "en", "hinglish") else "en",
        "actions": out,
    }


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
        f"{system}\\n\\n"
        "You are JARVIS command planner. Return ONLY JSON. Never execute a tool. "
        "Use only exact tool names from the catalog. Return actions=[] for conversation "
        "that does not require an action. Make every argument explicit and deterministic.\\n\\n"
        f"# SCHEMA\\n{json.dumps(schema, ensure_ascii=False)}\\n\\n"
        f"# TOOLS\\n{_tool_block()}\\n\\n"
        f"# LESSONS\\n{json.dumps(lessons, ensure_ascii=False)}\\n\\n"
        f"# HISTORY\\n{json.dumps(history, ensure_ascii=False)}\\n\\n"
        f"# USER\\n{text}\\n\\nJSON:"
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
        f"{system}\\n\\n"
        "Answer conversationally. Do not execute tools and do not emit tool calls. "
        "Commands are handled by the Kotlin command engine.\\n\\n"
        f"# LESSONS\\n{json.dumps(lessons, ensure_ascii=False)}\\n\\n"
        f"# HISTORY\\n{json.dumps(history, ensure_ascii=False)}\\n\\n"
        f"# USER\\n{text}\\n\\n# ASSISTANT\\n"
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
