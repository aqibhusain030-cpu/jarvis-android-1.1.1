"""System Telemetry, Process Management, Desktop Capture, and Multimodal Vision Skills."""
from __future__ import annotations

import base64
import mimetypes
import os
import platform
import shutil
import signal
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

from friday_ultra.config import (
    MODEL,
    OLLAMA_MODEL,
    OPENAI_API_KEY,
    STATE,
    USE_LOCAL_LLM,
    friday_tool,
    get_data_dir,
)
from friday_ultra.skills.security import verify_confirmation

try:
    import psutil  # type: ignore

    PSUTIL_AVAILABLE = True
except ImportError:
    psutil = None  # type: ignore
    PSUTIL_AVAILABLE = False

try:
    from PIL import Image, ImageGrab  # type: ignore

    PILLOW_AVAILABLE = True
except ImportError:
    Image = None  # type: ignore
    ImageGrab = None  # type: ignore
    PILLOW_AVAILABLE = False


@friday_tool
def get_system_info() -> str:
    """Gather comprehensive OS, CPU, RAM, disk, battery, Python, and GPU hardware telemetry."""
    uname = platform.uname()
    disk = shutil.disk_usage(Path.cwd())
    lines = [
        f"System Telemetry Report ({datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}):",
        f"- Platform: {uname.system} {uname.release} ({uname.machine})",
        f"- Hostname: {uname.node}",
        f"- Python Runtime: {sys.version.split()[0]} ({sys.executable})",
        f"- Logical CPU Cores: {os.cpu_count() or 1}",
    ]

    if PSUTIL_AVAILABLE and psutil is not None:
        try:
            cpu_pct = psutil.cpu_percent(interval=0.15)
            vm = psutil.virtual_memory()
            lines.append(f"- CPU Load: {cpu_pct:.1f}%")
            lines.append(
                f"- Memory: {vm.used / (1024**3):.2f} GB / {vm.total / (1024**3):.2f} GB ({vm.percent:.1f}%)"
            )
            batt = getattr(psutil, "sensors_battery", lambda: None)()
            if batt is not None:
                plug = "Charging/AC" if batt.power_plugged else "On Battery"
                lines.append(f"- Battery: {batt.percent:.0f}% ({plug})")
        except Exception:
            pass
    elif Path("/proc/meminfo").is_file():
        try:
            mem_raw = Path("/proc/meminfo").read_text(encoding="utf-8")
            kv = {}
            for row in mem_raw.splitlines()[:6]:
                parts = row.split(":")
                if len(parts) == 2:
                    kv[parts[0].strip()] = int(parts[1].strip().split()[0])
            if "MemTotal" in kv and "MemAvailable" in kv:
                total_gb = kv["MemTotal"] / (1024**2)
                used_gb = (kv["MemTotal"] - kv["MemAvailable"]) / (1024**2)
                lines.append(f"- Memory: {used_gb:.2f} GB / {total_gb:.2f} GB")
        except Exception:
            pass

    lines.append(
        f"- Disk ({Path.cwd().anchor or '/'}): "
        f"{disk.used / (1024**3):.2f} GB used / {disk.total / (1024**3):.2f} GB total "
        f"({disk.free / (1024**3):.2f} GB free)"
    )

    try:
        import torch  # type: ignore

        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            lines.append(f"- Accelerator: CUDA GPU Online ({gpu_name})")
        else:
            lines.append("- Accelerator: CPU Mode (CUDA not detected)")
    except ImportError:
        lines.append("- Accelerator: Standard CPU Runtime")

    return "\n".join(lines)


@friday_tool
def list_processes(top_n: int = 15, filter_name: str = "") -> str:
    """List top active processes sorted by memory/CPU usage, optionally filtered by name."""
    limit = max(1, min(int(top_n), 50))
    needle = filter_name.strip().lower()

    if PSUTIL_AVAILABLE and psutil is not None:
        procs = []
        for proc in psutil.process_iter(["pid", "name", "cpu_percent", "memory_percent"]):
            try:
                info = proc.info
                pname = str(info.get("name") or "")
                if needle and needle not in pname.lower():
                    continue
                procs.append(info)
            except Exception:
                continue
        procs.sort(key=lambda p: float(p.get("memory_percent") or 0.0), reverse=True)
        if not procs:
            return f"No running processes matched '{filter_name}'."
        lines = [f"Active Processes (top {min(limit, len(procs))}):"]
        for p in procs[:limit]:
            lines.append(
                f"- PID {p['pid']:<7} | MEM {float(p.get('memory_percent') or 0.0):5.1f}% | {p.get('name', 'unknown')}"
            )
        return "\n".join(lines)

    if shutil.which("ps"):
        res = subprocess.run(
            ["ps", "-eo", "pid,pmem,pcpu,comm", "--sort=-pmem"],
            capture_output=True,
            text=True,
            check=False,
        )
        rows = res.stdout.splitlines()
        if needle:
            header = rows[:1]
            rows = header + [r for r in rows[1:] if needle in r.lower()]
        return "\n".join(rows[: limit + 1])
    return "[Process Error] Neither psutil nor system `ps` is available."


@friday_tool
def kill_process(pid: int, confirm: str = "no") -> str:
    """Terminate a system process by PID. Strictly requires confirm='yes'."""
    if int(pid) <= 1 or int(pid) == os.getpid():
        return f"[Security Refusal] Refusing to terminate protected PID {pid}, {STATE.user_name}."

    allowed, msg = verify_confirmation(action="kill_process", target=f"PID {pid}", confirm=confirm)
    if not allowed:
        return msg

    try:
        os.kill(int(pid), signal.SIGTERM)
        return f"Sent SIGTERM to PID {pid}, {STATE.user_name}."
    except ProcessLookupError:
        return f"Process PID {pid} is no longer running."
    except PermissionError:
        return f"[Permission Denied] Insufficient privileges to terminate PID {pid}."
    except Exception as exc:
        return f"[Process Error] Failed to terminate PID {pid}: {exc}"


@friday_tool
def take_screenshot(filename: str = "") -> str:
    """Capture a desktop screenshot into FRIDAY's screenshots vault."""
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    clean_name = filename.strip() or f"screen_{stamp}.png"
    if not clean_name.lower().endswith(".png"):
        clean_name += ".png"
    out_path = get_data_dir() / "screenshots" / Path(clean_name).name
    out_path.parent.mkdir(parents=True, exist_ok=True)

    if PILLOW_AVAILABLE and ImageGrab is not None:
        try:
            img = ImageGrab.grab()
            img.save(str(out_path))
            return f"Desktop screenshot saved to '{out_path}', {STATE.user_name}."
        except Exception:
            pass

    for cmd in (
        ["screencapture", "-x", str(out_path)],
        ["scrot", str(out_path)],
        ["import", "-window", "root", str(out_path)],
    ):
        if shutil.which(cmd[0]):
            res = subprocess.run(cmd, capture_output=True, text=True, check=False)
            if res.returncode == 0 and out_path.is_file():
                return f"Desktop screenshot saved to '{out_path}', {STATE.user_name}."

    return "[Screenshot Unavailable] Headless display or missing GUI capture backend on this host."


@friday_tool
def describe_image(
    image_path: str,
    prompt: str = "Describe everything in this image with sharp technical precision.",
) -> str:
    """Inspect and analyze a local image using GPT-4o Vision (or local Ollama Vision)."""
    path = Path(image_path).expanduser().resolve()
    if not path.is_file():
        return f"[Vision Error] Image file not found: {path}"

    meta_info = f"File: {path.name} ({path.stat().st_size} bytes)"
    if PILLOW_AVAILABLE and Image is not None:
        try:
            with Image.open(path) as im:
                meta_info = f"File: {path.name} | Format: {im.format} | Size: {im.size[0]}x{im.size[1]} | Mode: {im.mode}"
        except Exception:
            pass

    use_local = os.environ.get("USE_LOCAL_LLM", str(USE_LOCAL_LLM)).strip().lower() in {"1", "true", "yes"}
    if use_local:
        try:
            import ollama  # type: ignore

            vision_model = os.environ.get("OLLAMA_MODEL", OLLAMA_MODEL)
            resp = ollama.chat(
                model=vision_model,
                messages=[{"role": "user", "content": prompt, "images": [str(path)]}],
            )
            content = resp.get("message", {}).get("content", "")
            if content:
                return f"Vision Analysis ({vision_model}) [{meta_info}]:\n{content}"
        except Exception as exc:
            return f"[Local Vision Note] {meta_info} — Ollama vision call failed: {exc}"

    api_key = os.environ.get("OPENAI_API_KEY", OPENAI_API_KEY).strip()
    if api_key:
        try:
            from openai import OpenAI  # type: ignore

            mime_type, _ = mimetypes.guess_type(str(path))
            mime_type = mime_type or "image/png"
            b64 = base64.b64encode(path.read_bytes()).decode("ascii")
            client = OpenAI(api_key=api_key)
            completion = client.chat.completions.create(
                model=os.environ.get("MODEL", MODEL) or "gpt-4o",
                messages=[
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt},
                            {"type": "image_url", "image_url": {"url": f"data:{mime_type};base64,{b64}"}},
                        ],
                    }
                ],
                max_tokens=900,
            )
            answer = completion.choices[0].message.content or ""
            return f"Vision Analysis [{meta_info}]:\n{answer}"
        except Exception as exc:
            return f"[Vision API Error] {meta_info} — {exc}"

    return (
        f"[Vision Offline Metadata] {meta_info}. "
        f"Configure OPENAI_API_KEY (for GPT-4o Vision) or USE_LOCAL_LLM=true with a multimodal Ollama model, {STATE.user_name}."
    )


SYSTEM_TOOLS = [get_system_info, list_processes, kill_process, take_screenshot, describe_image]
