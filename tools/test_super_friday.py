#!/usr/bin/env python3
"""Comprehensive verification suite for SUPER FRIDAY v3.0 MAX (friday_ultra + super_friday_max.py)."""
from __future__ import annotations

import os
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def invoke(tool_obj, **kwargs):
    """Invoke either a LangChain StructuredTool or a fallback tool wrapper uniformly."""
    if hasattr(tool_obj, "invoke"):
        return tool_obj.invoke(kwargs)
    return tool_obj(**kwargs)


def test_requirements_and_env_spec() -> None:
    req_path = ROOT / "requirements.txt"
    assert req_path.is_file(), "Missing requirements.txt"
    req_text = req_path.read_text(encoding="utf-8").lower()
    required_pkgs = [
        "openai",
        "langgraph",
        "langchain",
        "langchain-openai",
        "langchain-community",
        "langchain-core",
        "crewai",
        "crewai-tools",
        "duckduckgo-search",
        "chromadb",
        "rich",
        "python-dotenv",
        "edge-tts",
        "pyttsx3",
        "sounddevice",
        "numpy",
        "openai-whisper",
        "torch",
        "torchaudio",
        "playwright",
        "requests",
        "beautifulsoup4",
        "wikipedia",
        "deep-translator",
        "yfinance",
        "pypdf",
        "pillow",
        "psutil",
        "gradio",
        "python-telegram-bot",
        "ollama",
    ]
    for pkg in required_pkgs:
        assert pkg in req_text, f"Missing required package in requirements.txt: {pkg}"

    env_path = ROOT / ".env.example"
    assert env_path.is_file(), "Missing .env.example"
    env_text = env_path.read_text(encoding="utf-8")
    expected_keys = [
        "OPENAI_API_KEY=",
        "MODEL=gpt-4o",
        "USER_NAME=sir",
        "VOICE_ENABLED=true",
        "TTS_VOICE=en-GB-LibbyNeural",
        "WHISPER_MODEL=base",
        "OPENWEATHER_API_KEY=",
        "GMAIL_ADDRESS=",
        "GMAIL_APP_PASSWORD=",
        "TELEGRAM_BOT_TOKEN=",
        "TELEGRAM_CHAT_ID=",
        "OLLAMA_MODEL=llama3.1",
        "USE_LOCAL_LLM=false",
        "GRADIO_PORT=7860",
    ]
    for entry in expected_keys:
        assert entry in env_text, f"Missing expected .env.example entry: {entry}"
    assert "sk-" not in env_text, ".env.example must not include secret-like prefixes"


def test_directory_structure() -> None:
    expected_paths = [
        "super_friday_max.py",
        "friday_ultra/main.py",
        "friday_ultra/config.py",
        "friday_ultra/brain.py",
        "friday_ultra/memory.py",
        "friday_ultra/voice.py",
        "friday_ultra/skills/files.py",
        "friday_ultra/skills/browser.py",
        "friday_ultra/skills/coding.py",
        "friday_ultra/skills/email.py",
        "friday_ultra/skills/smart_home.py",
        "friday_ultra/skills/system.py",
        "friday_ultra/skills/security.py",
        "friday_ultra/skills/automation.py",
        "friday_ultra/ui/web_dashboard.py",
        "friday_ultra/data",
        "friday_ultra/plugins",
    ]
    for rel in expected_paths:
        assert (ROOT / rel).exists(), f"Missing required path: {rel}"


def test_modular_friday_ultra(tmp_dir: Path) -> None:
    os.environ["FRIDAY_DATA_DIR"] = str(tmp_dir / "ultra_data")
    os.environ["USER_NAME"] = "sir"
    os.environ["VOICE_ENABLED"] = "false"

    import sys
    if str(ROOT) not in sys.path:
        sys.path.insert(0, str(ROOT))

    from friday_ultra import brain, config, memory, voice
    from friday_ultra.plugins import load_plugins
    from friday_ultra.skills import (
        automation,
        browser,
        coding,
        email,
        files,
        security,
        smart_home,
        system,
    )
    from friday_ultra.ui import web_dashboard

    # 1. Config check
    assert config.MODEL == "gpt-4o"
    assert config.TTS_VOICE == "en-GB-LibbyNeural"
    assert config.GRADIO_PORT == 7860

    # 2. Brain prompt & 7-agent swarm definition check
    for phrase in (
        "Female Replacement Intelligent Digital Assistant Youth",
        "TOOL SUPREMACY",
        "MEMORY CONTINUITY",
        "SAFETY RAILS",
        "PRESENTATION",
    ):
        assert phrase in brain.SYSTEM, f"SYSTEM prompt missing: {phrase}"

    brain_src = (ROOT / "friday_ultra" / "brain.py").read_text(encoding="utf-8")
    for role in (
        "Lead Intelligence Researcher",
        "Browser & Network Automation Specialist",
        "Long-Term Memory & Context Archivist",
        "Operations & Communications Scheduler",
        "Principal Engineer & Code Executor",
        "Chief Security & Quality Critic",
        "FRIDAY Executive Supervisor",
    ):
        assert role in brain_src, f"7-agent swarm missing role: {role}"

    # 3. Memory check (notes.json + semantic long-term search)
    res_rem = invoke(memory.remember, key="arc_reactor_output", value="3.2 gigajoules per second", category="engineering")
    assert "arc_reactor_output" in res_rem
    res_rec = invoke(memory.recall, query="arc_reactor")
    assert "3.2 gigajoules" in res_rec
    res_ltm = invoke(memory.search_long_term_memory, query="gigajoules reactor", top_k=3)
    assert "3.2 gigajoules" in res_ltm or "arc_reactor" in res_ltm
    res_fgt = invoke(memory.forget, key="arc_reactor_output")
    assert "Removed" in res_fgt or "Deleted" in res_fgt or "arc_reactor_output" in res_fgt

    # 4. Security & Coding tools check
    marker_file = tmp_dir / "should_not_exist.txt"
    dry_res = invoke(coding.run_shell, command=f"touch {marker_file}", confirm="no")
    assert not marker_file.exists(), "run_shell executed without confirm='yes'!"
    assert "DRY RUN" in dry_res.upper() or "CONFIRM" in dry_res.upper()

    exec_res = invoke(coding.run_shell, command="echo StarkIndustriesOK", confirm="yes")
    assert "StarkIndustriesOK" in exec_res

    blocked_res = invoke(coding.run_shell, command="rm -rf /", confirm="yes")
    assert "BLOCKED" in blocked_res.upper()

    audit_res = invoke(security.get_security_audit_log, lines=10)
    assert "StarkIndustriesOK" in audit_res

    py_res = invoke(coding.run_python, code="print(6 * 7)")
    assert "42" in py_res

    # 5. Files tools check
    sample_file = tmp_dir / "blueprint.txt"
    w_res = invoke(files.write_file, file_path=str(sample_file), content="Mark LXXXV Nano-Tech")
    assert sample_file.is_file(), w_res
    r_res = invoke(files.read_file, file_path=str(sample_file))
    assert "Mark LXXXV Nano-Tech" in r_res
    ls_res = invoke(files.list_dir, directory=str(tmp_dir))
    assert "blueprint.txt" in ls_res

    del_dry = invoke(files.delete_file, file_path=str(sample_file), confirm="no")
    assert sample_file.exists(), "delete_file deleted without confirm='yes'!"
    assert "CONFIRM" in del_dry.upper() or "DRY RUN" in del_dry.upper()
    del_ok = invoke(files.delete_file, file_path=str(sample_file), confirm="yes")
    assert not sample_file.exists(), del_ok

    md_path = tmp_dir / "report.md"
    md_res = invoke(files.markdown_report, file_path=str(md_path), title="Suit Diagnostics", sections="All systems nominal.")
    assert md_path.is_file(), md_res

    # 6. Automation tools check
    time_res = invoke(automation.get_current_time, timezone_name="UTC")
    assert "UTC" in time_res

    calc_res = invoke(automation.calculator, expression="sqrt(144) + 2 ** 5")
    assert "44" in calc_res
    bad_calc = invoke(automation.calculator, expression="__import__('os').system('id')")
    assert "Error" in bad_calc or "Unsupported" in bad_calc or "Invalid" in bad_calc

    rem_add = invoke(automation.add_reminder, message="Calibrate thrusters", when="in 1 second")
    assert "Calibrate thrusters" in rem_add
    rem_list = invoke(automation.list_reminders, include_completed=False)
    assert "Calibrate thrusters" in rem_list

    fired: list[str] = []
    daemon = automation.ReminderDaemon(on_due_callback=lambda r: fired.append(r["message"]), poll_interval=0.2)
    daemon.start()
    deadline = time.time() + 4.0
    while time.time() < deadline and not fired:
        time.sleep(0.15)
    daemon.stop()
    assert "Calibrate thrusters" in fired, f"ReminderDaemon did not fire due reminder: {fired}"

    # 7. Calendar, System, Smart Home, Voice, Plugins
    cal_res = invoke(
        email.create_calendar_event,
        summary="Stark Expo Keynote",
        start_time="2026-11-15T10:00:00",
        end_time="2026-11-15T11:00:00",
        description="Main stage presentation",
        location="Mumbai Convention Centre",
    )
    assert ".ics" in cal_res
    cal_list = invoke(email.list_calendar_events, max_results=5)
    assert "Stark Expo Keynote" in cal_list

    sys_res = invoke(system.get_system_info)
    assert "Python" in sys_res and ("CPU" in sys_res or "Platform" in sys_res)

    net_res = invoke(smart_home.network_diagnostics, target_host="127.0.0.1")
    assert "127.0.0.1" in net_res

    port_res = invoke(smart_home.scan_local_ports, host="127.0.0.1", ports="22,65530")
    assert "127.0.0.1" in port_res

    cleaned_speech = voice.clean_text_for_speech("**Hello** `print(1)` https://example.com sir!")
    assert "https://" not in cleaned_speech and "**" not in cleaned_speech

    plugins = load_plugins()
    assert len(plugins) >= 1, "Expected at least sample_plugin to load from friday_ultra/plugins"

    # 8. MaxFriday orchestrator check
    friday = brain.MaxFriday()
    status = friday.status_report()
    assert status["tools_count"] >= 20, f"Expected full tool belt, got {status['tools_count']}"
    assert callable(web_dashboard.build_gradio_app)
    assert callable(web_dashboard.start_telegram_bot)
    assert callable(browser.browse_url.invoke if hasattr(browser.browse_url, "invoke") else browser.browse_url)


def test_standalone_super_friday_max(tmp_dir: Path) -> None:
    os.environ["FRIDAY_DATA_DIR"] = str(tmp_dir / "standalone_data")
    os.environ["VOICE_ENABLED"] = "false"

    import importlib.util
    spec = importlib.util.spec_from_file_location("super_friday_max", ROOT / "super_friday_max.py")
    assert spec and spec.loader
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)

    assert "Female Replacement Intelligent Digital Assistant Youth" in mod.SYSTEM
    assert callable(mod.run_crew_task)
    assert len(mod.ALL_TOOLS) >= 20

    assert "44" in invoke(mod.calculator, expression="sqrt(144) + 32")
    dry = invoke(mod.run_shell, command="echo nope", confirm="no")
    assert "DRY RUN" in dry.upper() or "CONFIRM" in dry.upper()
    ok = invoke(mod.run_shell, command="echo StandaloneOK", confirm="yes")
    assert "StandaloneOK" in ok

    invoke(mod.remember, key="mark_suit", value="Mark LXXXV", category="armor")
    assert "Mark LXXXV" in invoke(mod.recall, query="mark_suit")
    assert "Mark LXXXV" in invoke(mod.search_long_term_memory, query="armor Mark")

    friday = mod.MaxFriday()
    st = friday.status_report()
    assert st["tools_count"] == len(mod.ALL_TOOLS)


def main() -> None:
    test_requirements_and_env_spec()
    test_directory_structure()
    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        test_modular_friday_ultra(tmp_path)
        test_standalone_super_friday_max(tmp_path)
    print("SUPER FRIDAY v3.0 MAX verification suite: PASS")


if __name__ == "__main__":
    main()
