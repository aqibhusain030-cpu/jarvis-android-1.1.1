"""Security Gatekeeper, Destructive Command Filter, and Structured Audit Logging."""
from __future__ import annotations

import json
import re
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from friday_ultra.config import STATE, friday_tool, get_data_dir

_AUDIT_LOCK = threading.RLock()

# Catastrophic system-wiping patterns blocked unconditionally even if confirm="yes"
CATASTROPHIC_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"(^|\s|;|&&|\|\|)rm\s+(-[a-zA-Z]*r[a-zA-Z]*f|-[a-zA-Z]*f[a-zA-Z]*r)\s+(/|\*/?)\s*($|;|&&|\|\|)"),
    re.compile(r"rm\s+--no-preserve-root"),
    re.compile(r"\bmkfs(\.[a-z0-9]+)?\b", re.IGNORECASE),
    re.compile(r"\bdd\s+if=.*of=/dev/(sd|nvme|mmcblk|vd|hd)", re.IGNORECASE),
    re.compile(r":\(\)\s*\{\s*:\s*\|\s*:\s*&\s*\}\s*;\s*:"),
    re.compile(r"\b(format\s+[cdefgh]:|del\s+/f\s+/s\s+/q\s+c:\\)", re.IGNORECASE),
)


def _audit_log_path() -> Path:
    log_dir = get_data_dir() / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)
    return log_dir / "security_audit.log"


def log_security_event(
    action: str,
    target: str,
    status: str,
    confirm: str = "no",
    details: str = "",
) -> None:
    """Append a structured JSONL record to FRIDAY's security audit log."""
    entry: dict[str, Any] = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "user": STATE.user_name,
        "action": action,
        "target": target[:500],
        "confirm": confirm,
        "status": status,
        "details": details[:500],
    }
    with _AUDIT_LOCK:
        with _audit_log_path().open("a", encoding="utf-8") as fh:
            fh.write(json.dumps(entry, ensure_ascii=False) + "\n")


def is_catastrophic_command(command: str) -> bool:
    """Return True if a shell command matches an irreversible OS-wiping pattern."""
    normalized = command.strip()
    return any(pattern.search(normalized) for pattern in CATASTROPHIC_PATTERNS)


def verify_confirmation(action: str, target: str, confirm: str = "no") -> tuple[bool, str]:
    """Gatekeeper enforcing explicit confirm='yes' before executing destructive or shell actions."""
    if action == "run_shell" and is_catastrophic_command(target):
        log_security_event(
            action=action,
            target=target,
            status="HARD_BLOCKED",
            confirm=confirm,
            details="Matched catastrophic OS-destructive command signature.",
        )
        return (
            False,
            f"[SECURITY HARD BLOCKED] Refusing catastrophic command `{target}`, {STATE.user_name}. "
            "Even under override protocols, wiping root filesystems or fork-bombing the host is off-limits.",
        )

    is_confirmed = str(confirm).strip().lower() == "yes"
    if not is_confirmed:
        log_security_event(
            action=action,
            target=target,
            status="DRY_RUN_PENDING_CONFIRMATION",
            confirm=confirm,
            details="Execution withheld until explicit confirm='yes'.",
        )
        return (
            False,
            f"[DRY RUN — CONFIRMATION REQUIRED] Action `{action}` on `{target}` was NOT executed. "
            f"Report the exact operation to {STATE.user_name} and await explicit authorization before calling with confirm='yes'.",
        )

    log_security_event(
        action=action,
        target=target,
        status="AUTHORIZED",
        confirm="yes",
        details="Explicit confirm='yes' verified.",
    )
    return True, "Authorized"


@friday_tool
def get_security_audit_log(lines: int = 20) -> str:
    """Read the most recent entries from FRIDAY's security gatekeeper audit log."""
    path = _audit_log_path()
    if not path.is_file():
        return f"Security audit log is clean (no sensitive actions recorded yet), {STATE.user_name}."

    count = max(1, min(int(lines), 200))
    with _AUDIT_LOCK:
        all_lines = path.read_text(encoding="utf-8").splitlines()
    recent = all_lines[-count:]
    if not recent:
        return f"Security audit log is empty, {STATE.user_name}."

    formatted = [f"Security Audit Trail (last {len(recent)} events):"]
    for raw in recent:
        try:
            item = json.loads(raw)
            formatted.append(
                f"- [{item.get('timestamp', '')[:19]}] {item.get('status')} | "
                f"{item.get('action')} -> {item.get('target')} (confirm={item.get('confirm')})"
            )
        except json.JSONDecodeError:
            formatted.append(f"- {raw}")
    return "\n".join(formatted)


SECURITY_TOOLS = [get_security_audit_log]
