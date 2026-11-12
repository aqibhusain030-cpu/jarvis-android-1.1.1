"""Drop-in FRIDAY Plugin Extension: Cryptographic Digest & Stark Token Generator.

Drop any .py file exposing @friday_tool (or @tool) functions into friday_ultra/plugins/
and FRIDAY automatically binds them into her LangGraph tool belt on boot.
"""
from __future__ import annotations

import hashlib
import uuid

from friday_ultra.config import friday_tool


@friday_tool
def generate_stark_token(prefix: str = "stark") -> str:
    """Generate a unique cryptographic-grade identifier token with an optional prefix."""
    clean = prefix.strip() or "stark"
    return f"{clean}-{uuid.uuid4().hex[:16]}"


@friday_tool
def sha256_digest(text: str) -> str:
    """Compute the hexadecimal SHA-256 cryptographic digest of the given text."""
    digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
    return f"SHA-256: {digest}"


PLUGIN_TOOLS = [generate_stark_token, sha256_digest]
