"""In-process memory façade. Persistent storage is Kotlin's Room DB."""
from typing import Dict, List


class Memory:
    def __init__(self) -> None:
        self._facts: Dict[str, str] = {}
        self._pending: List[dict] = []

    def put_fact(self, key: str, value: str, category: str = "fact") -> None:
        self._facts[key] = value
        self._pending.append({"key": key, "value": value, "category": category})

    def get_fact(self, key: str) -> str:
        return self._facts.get(key, "")

    def build_context_block(self, user_text: str) -> str:
        if not self._facts:
            return "(no stored facts)"
        lines = [f"- {k}: {v}" for k, v in self._facts.items()]
        return "\n".join(lines[:30])

    def remember_message(self, role: str, content: str, lang: str) -> None:
        self._pending.append({"message": {"role": role, "content": content, "language": lang}})

    def drain(self) -> List[dict]:
        out = list(self._pending); self._pending.clear(); return out
