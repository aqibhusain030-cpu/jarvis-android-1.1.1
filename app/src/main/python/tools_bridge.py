"""Python-side view of the Kotlin ToolDispatcher.

Kotlin installs a callback via brain.install_tool_dispatcher(bridge).
That path also calls tools_bridge.set_dispatcher so executor/planner can use it.
"""
from __future__ import annotations

from typing import Callable, Dict, Optional
import json

_dispatcher: Optional[Callable[[str, str], str]] = None


def set_dispatcher(fn: Callable[[str, str], str]) -> None:
    global _dispatcher
    _dispatcher = fn


def invoke(tool: str, args: Dict) -> Dict:
    if _dispatcher is None:
        return {"ok": False, "error": "no dispatcher", "code": "no_dispatcher"}
    raw = _dispatcher(tool, json.dumps(args, ensure_ascii=False))
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return {"ok": False, "error": "bad dispatcher JSON", "code": "bad_json"}
