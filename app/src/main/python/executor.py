"""Executes a plan by calling back into Kotlin's ToolDispatcher via tools_bridge."""
from typing import Callable, List
from planner import Step
import time

_dispatcher: Callable[[str, dict], dict] | None = None


def set_dispatcher(d: Callable[[str, dict], dict]) -> None:
    global _dispatcher
    _dispatcher = d


def run_plan(steps: List[Step]) -> List[Step]:
    if _dispatcher is None:
        return steps
    for step in steps:
        if not step.tool:
            step.status = "DONE"; continue
        step.status = "RUNNING"
        step.startedAt = time.time()
        try:
            result = _dispatcher(step.tool, step.args)
            step.result = str(result.get("data", ""))
            step.status = "DONE" if result.get("ok") else "FAILED"
        except Exception as exc:  # noqa: BLE001
            step.result = str(exc); step.status = "FAILED"
        step.attempts += 1; step.finishedAt = time.time()
    return steps
