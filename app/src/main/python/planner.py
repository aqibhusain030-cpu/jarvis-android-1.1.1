"""Goal → PlanStep planner."""
from dataclasses import dataclass, field
from typing import Any, Callable, Dict, List
import json
import uuid
from utils import load_prompt


@dataclass
class Step:
    id: str
    description: str
    tool: str = ""
    args: Dict[str, Any] = field(default_factory=dict)
    status: str = "PENDING"
    result: str = ""
    attempts: int = 0
    startedAt: Any = None
    finishedAt: Any = None


def plan(llm_generate: Callable[..., str], title: str, description: str) -> List[Step]:
    prompt = f"{load_prompt('planner')}\n\nGOAL: {title}\nDETAILS: {description}\n"
    raw = llm_generate(prompt, max_tokens=500, temperature=0.1)
    steps = _parse(raw)
    if not steps:
        steps = [Step(id=str(uuid.uuid4())[:8], description=title)]
    return steps


def _parse(raw: str) -> List[Step]:
    try:
        start = raw.index("["); end = raw.rindex("]") + 1
        data = json.loads(raw[start:end])
        out: List[Step] = []
        for item in data:
            out.append(Step(
                id=str(item.get("id") or uuid.uuid4())[:8],
                description=str(item.get("description", "")),
                tool=str(item.get("tool", "") or ""),
                args=item.get("args", {}) or {}))
        return out
    except (ValueError, json.JSONDecodeError):
        return []
