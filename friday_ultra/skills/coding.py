"""Code Execution & Confirmation-Gated Shell Skills."""
from __future__ import annotations

import subprocess
import sys

from friday_ultra.config import friday_tool
from friday_ultra.skills.security import log_security_event, verify_confirmation


@friday_tool
def run_python(code: str, timeout: int = 30) -> str:
    """Execute a Python snippet in an isolated subprocess and capture stdout, stderr, and exit code."""
    clean_code = code.strip()
    if not clean_code:
        return "No Python code provided."

    limit = max(1, min(int(timeout), 180))
    log_security_event(
        action="run_python",
        target=clean_code.splitlines()[0][:120],
        status="EXECUTING",
        confirm="yes",
        details=f"lines={len(clean_code.splitlines())}, timeout={limit}s",
    )
    try:
        completed = subprocess.run(
            [sys.executable, "-I", "-c", clean_code],
            capture_output=True,
            text=True,
            timeout=limit,
            check=False,
        )
        out = (completed.stdout or "").strip()
        err = (completed.stderr or "").strip()
        parts = [f"Exit code: {completed.returncode}"]
        if out:
            parts.append(f"STDOUT:\n{out[:12000]}")
        if err:
            parts.append(f"STDERR:\n{err[:8000]}")
        if not out and not err:
            parts.append("(Process completed with no console output.)")
        return "\n".join(parts)
    except subprocess.TimeoutExpired:
        return f"[Python Timeout] Execution exceeded {limit} seconds and was terminated."
    except Exception as exc:
        return f"[Python Execution Error] {exc}"


@friday_tool
def run_shell(command: str, confirm: str = "no", timeout: int = 30) -> str:
    """Execute a system shell command ONLY when confirm='yes'. Defaults to safe dry-run mode (confirm='no').

    Args:
        command: Exact shell command to inspect or execute.
        confirm: Must be explicitly 'yes' after user authorization; defaults to 'no' (dry run).
        timeout: Maximum execution time in seconds (default 30).
    """
    clean_cmd = command.strip()
    if not clean_cmd:
        return "No shell command provided."

    allowed, gate_msg = verify_confirmation(action="run_shell", target=clean_cmd, confirm=confirm)
    if not allowed:
        return gate_msg

    limit = max(1, min(int(timeout), 300))
    try:
        completed = subprocess.run(
            clean_cmd,
            shell=True,  # noqa: S602 - intentionally gated behind verify_confirmation + catastrophic filter
            capture_output=True,
            text=True,
            timeout=limit,
            check=False,
        )
        out = (completed.stdout or "").strip()
        err = (completed.stderr or "").strip()
        parts = [f"Shell exit code: {completed.returncode}"]
        if out:
            parts.append(f"STDOUT:\n{out[:12000]}")
        if err:
            parts.append(f"STDERR:\n{err[:8000]}")
        if not out and not err:
            parts.append("(Command completed silently.)")
        return "\n".join(parts)
    except subprocess.TimeoutExpired:
        return f"[Shell Timeout] Command exceeded {limit} seconds and was terminated."
    except Exception as exc:
        return f"[Shell Error] {exc}"


CODING_TOOLS = [run_python, run_shell]
