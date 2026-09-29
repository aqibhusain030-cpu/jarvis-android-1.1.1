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


def respond(payload_json: str) -> str:
    payload = json.loads(payload_json)
    text = payload.get("userText", "")
    lang = payload.get("detectedLanguage") or detect_language(text)
    history = payload.get("recentMessages", [])[-8:]

    _mem.remember_message("user", text, lang)

    system = load_prompt(f"system_{lang if lang in ('hi', 'en', 'hinglish') else 'hi'}")
    context = _mem.build_context_block(text)
    tools = _tool_block()

    base = (
        f"{system}\n\n# CONTEXT\n{context}\n\n"
        f"{tools}\n\n"
        f"# HISTORY\n" + "\n".join(history) + "\n\n"
        f"# USER\n{text}\n\n# ASSISTANT\n"
    )

    tool_trace: List[str] = []
    seen_tool_calls = set()
    reply_text = ""
    prompt = base

    for _ in range(_MAX_TOOL_ROUNDS):
        reply_text = _llm_generate(prompt, max_tokens=700, temperature=0.2)
        if not reply_text.strip():
            break
        calls = _parse_tool_calls(reply_text)
        if not calls:
            # Strip any residual tool fences from final answer
            clean = _TOOL_CALL_RE.sub("", reply_text)
            clean = _TOOL_CALL_RE2.sub("", clean).strip()
            reply_text = clean or reply_text
            break
        # Execute tools and continue
        results = []
        for c in calls:
            args = c.get("args") or {}
            call_id = _tool_call_id(c["name"], args)
            if call_id in seen_tool_calls:
                results.append({"tool": c["name"], "result": {"ok": False, "error": "duplicate tool call suppressed", "code": "duplicate_call"}})
                continue
            seen_tool_calls.add(call_id)
            r = _run_tool(c["name"], args)
            tool_trace.append(c["name"])
            results.append({"id": call_id, "tool": c["name"], "result": r})
        observation = json.dumps(results, ensure_ascii=False)
        prompt = (
            base
            + f"\n# PREVIOUS ASSISTANT\n{reply_text}\n\n"
            + f"# TOOL RESULTS\n{observation}\n\n"
            + "# ASSISTANT (final answer for user, no more tools unless needed)\n"
        )

    if not (reply_text or "").strip():
        reply_text = _fallback(lang, text)

    _mem.remember_message("assistant", reply_text, lang)
    return json.dumps(
        {
            "text": reply_text.strip(),
            "language": lang,
            "toolCalls": tool_trace,
        },
        ensure_ascii=False,
    )


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
