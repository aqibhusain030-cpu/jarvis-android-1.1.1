"""Small helpers shared by the Python Brain."""
import os
from functools import lru_cache

_PROMPT_DIR = os.path.join(os.path.dirname(__file__), "prompts")


@lru_cache(maxsize=32)
def load_prompt(name: str) -> str:
    path = os.path.join(_PROMPT_DIR, f"{name}.txt")
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return fh.read()
    except FileNotFoundError:
        return ""


def safe_int(v, default: int = 0) -> int:
    try:
        return int(v)
    except (TypeError, ValueError):
        return default
