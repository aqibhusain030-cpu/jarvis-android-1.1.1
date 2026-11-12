# SUPER FRIDAY v3.0 MAX ("Nothing Left Behind" Edition) Implementation Plan

> **Date:** 2026-11-12  
> **Scope:** Complete, zero-placeholder implementation of SUPER FRIDAY v3.0 MAX in both modular package form (`friday_ultra/`) and standalone self-contained script form (`super_friday_max.py`), preserving existing Android/JARVIS source contracts untouched.

## 1. Architecture & Deliverables

1. **Environment & Dependency Specification**
   - `requirements.txt` containing every specified package across Core/Brain, Voice I/O, Browser/Extra Tools, UI/Bots, and Local LLM Support.
   - `.env.example` containing all 14 exact configuration keys (`OPENAI_API_KEY`, `MODEL`, `USER_NAME`, `VOICE_ENABLED`, `TTS_VOICE`, `WHISPER_MODEL`, `OPENWEATHER_API_KEY`, `GMAIL_ADDRESS`, `GMAIL_APP_PASSWORD`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, `OLLAMA_MODEL`, `USE_LOCAL_LLM`, `GRADIO_PORT`) with zero hardcoded secrets.
   - `.gitignore` updates to ensure runtime databases, `.env`, audio temp files, screenshots, and logs are never committed.

2. **Modular Package (`friday_ultra/`)**
   - `friday_ultra/config.py`: `.env` loader, typed config state, data/plugin directories, OpenAI (`ChatOpenAI`) + Ollama (`ChatOllama`) LLM factory.
   - `friday_ultra/memory.py`: Persistent ChromaDB semantic store (with automatic cosine/JSON vector fallback), thread-safe `notes.json` and `reminders.json`, `SqliteSaver` checkpointer factory, and `@tool` functions (`remember`, `recall`, `forget`, `search_long_term_memory`).
   - `friday_ultra/voice.py`: Neural British TTS via `edge-tts` (`en-GB-LibbyNeural`), offline fallback via `pyttsx3`, microphone recording via `sounddevice` + `numpy`, and local Whisper STT (`openai-whisper`).
   - `friday_ultra/skills/security.py`: Strict confirmation gatekeeper (`confirm="yes"` enforcement), catastrophic command blocklist, pending confirmation tracker, and structured JSONL audit logging (`get_security_audit_log`).
   - `friday_ultra/skills/files.py`: `read_file`, `write_file`, `delete_file` (confirmation-gated), `list_dir`, `read_pdf` (`pypdf`), and `markdown_report`.
   - `friday_ultra/skills/browser.py`: `browse_url` (Playwright headless Chromium + screenshot support + `requests`/`BeautifulSoup` fallback), `web_search` (`duckduckgo-search`), and `wikipedia_summary` (`wikipedia` + REST fallback).
   - `friday_ultra/skills/coding.py`: Isolated subprocess `run_python` and confirmation-gated `run_shell` (`confirm="no"` dry-run default).
   - `friday_ultra/skills/email.py`: Gmail SMTP SSL `send_email` (with attachment & confirmation support) plus Google Calendar ready hooks (`create_calendar_event`, `list_calendar_events` with Google Calendar API integration + `.ics` generation).
   - `friday_ultra/skills/smart_home.py`: `open_jio_admin` router portal inspector plus `network_diagnostics` and `scan_local_ports`.
   - `friday_ultra/skills/system.py`: `get_system_info` (`psutil` + GPU telemetry), `list_processes`, `kill_process` (confirmation-gated), `take_screenshot`, and multimodal `describe_image` (OpenAI GPT-4o Vision / Ollama Vision / Pillow metadata fallback).
   - `friday_ultra/skills/automation.py`: `get_current_time`, safe AST `calculator`, `get_weather` (OpenWeatherMap + `wttr.in` fallback), `stock_price` (`yfinance`), `translate` (`deep-translator`), `add_reminder`, `list_reminders`, and background `ReminderDaemon`.
   - `friday_ultra/plugins/`: Dynamic `@tool` plugin loader (`load_plugins`) + drop-in `sample_plugin.py`.
   - `friday_ultra/brain.py`: Exact upgraded `SYSTEM` prompt, exact 7-agent sequential CrewAI swarm (`run_crew_task` + `delegate_to_crew` tool), LangGraph `StateGraph` (`assistant` <-> `ToolNode` with `SqliteSaver`), and `MaxFriday` orchestrator class.
   - `friday_ultra/ui/web_dashboard.py`: Multi-tab Stark HUD Gradio Web UI (`0.0.0.0:GRADIO_PORT`) with text/voice chat, 7-agent swarm launcher, memory/reminder vault, and security telemetry + authorized `python-telegram-bot` background daemon.
   - `friday_ultra/main.py`: Rich CLI banner, slash commands (`/voice`, `/crew`, `/memory`, `/reminders`, `/tools`, `/audit`, `/status`, `/web`, `/reset`, `/exit`), CLI flags, and background thread orchestration.

3. **Self-Contained Master Script (`super_friday_max.py`)**
   - Single-file zero-placeholder master script bundling every subsystem, tool, memory backend, voice pipeline, 7-agent CrewAI swarm, LangGraph `StateGraph`, `MaxFriday` class, Gradio UI, Telegram bot, plugin loader, and Rich CLI loop with 100% parity.

## 2. Verification Strategy (TDD)

1. Write `tools/test_super_friday.py` covering:
   - Configuration & all 14 `.env` keys
   - Exact `SYSTEM` prompt & 7-agent `run_crew_task` structure
   - All tools across `files`, `browser`, `coding`, `email`, `smart_home`, `system`, `security`, `automation`, `memory`, and `plugins`
   - Strict safety rail (`run_shell`, `delete_file`, `kill_process` dry-run when `confirm != "yes"`, execution only when `confirm == "yes"`, hard block on catastrophic commands)
   - Persistent `notes.json`, `reminders.json`, `ReminderDaemon`, and semantic `search_long_term_memory`
   - `MaxFriday` conversation loop & thread isolation
   - Standalone `super_friday_max.py` parity with `friday_ultra/`
2. Run `tools/test_super_friday.py` before implementation to confirm failure (Red).
3. Implement `requirements.txt`, `.env.example`, `friday_ultra/`, and `super_friday_max.py`.
4. Run `tools/test_super_friday.py` and verify 100% pass (Green), then run all existing repository contract checks (`test_source_contracts.py`, `test_superpowers_bundle.py`, `verify_release.sh`).
