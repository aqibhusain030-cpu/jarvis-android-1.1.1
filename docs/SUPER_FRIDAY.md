# SUPER FRIDAY v3.0 MAX — "Nothing Left Behind" Edition

FRIDAY (*Female Replacement Intelligent Digital Assistant Youth*) is the desktop/ workstation
companion brain shipped alongside the JARVIS Android app. It is delivered **twice**, with full
feature parity:

| Entry point | Purpose |
| --- | --- |
| `super_friday_max.py` | Single-file master script. Zero intra-repo imports — copy it anywhere. |
| `friday_ultra/` | Modular package (`main`, `config`, `brain`, `memory`, `voice`, `skills/`, `ui/`, `plugins/`, `data/`). |

```bash
pip install -r requirements.txt
playwright install chromium          # optional: JS-rendered browsing
cp .env.example .env                 # fill in OPENAI_API_KEY etc.

python3 super_friday_max.py           # Rich CLI REPL
python3 super_friday_max.py --web     # + Gradio dashboard on 0.0.0.0:7860
python3 super_friday_max.py --telegram
python3 super_friday_max.py --crew "research arc-reactor alloys and email me a summary"
python3 super_friday_max.py --status
python3 super_friday_max.py --self-test

python3 -m friday_ultra               # identical CLI, modular package
python3 -m friday_ultra --self-test
```

## Architecture

```
.env / environment  ──►  config (14 keys)  ──►  build_llm()  ──►  OpenAI GPT-4o  or  Ollama
                                                   │
                                    MaxFriday (brain.py) ── LangGraph StateGraph
                                                   │            assistant ⇄ ToolNode
                                                   │            SqliteSaver checkpointer
                                                   ├─ ChromaDB vector memory (+cosine JSON fallback)
                                                   ├─ SQLite conversation history per thread_id
                                                   ├─ 37 bound tools (skills + memory + plugins)
                                                   └─ run_crew_task() — 7-agent CrewAI swarm
```

* **Prompt** — the exact upgraded `SYSTEM` constant lives in both `friday_ultra/brain.py` and
  `super_friday_max.py` (tool supremacy, memory continuity, safety rails, presentation).
* **Swarm** — `run_crew_task(goal)` builds seven sequential agents (Lead Intelligence Researcher,
  Browser & Network Automation Specialist, Long-Term Memory & Context Archivist, Operations &
  Communications Scheduler, Principal Engineer & Code Executor, Chief Security & Quality Critic,
  FRIDAY Executive Supervisor) with `Process.sequential`. Exposed to the LLM as
  `delegate_crew_swarm` / `delegate_to_crew`.
* **Graceful degradation** — every third-party import is optional. Without LangGraph the brain
  falls back to a direct tool-bound LLM loop; without CrewAI the swarm reports and defers; without
  ChromaDB the vector store becomes a cosine-scored JSON index; without a model key the
  `UnconfiguredLLM` explains exactly which variable is missing while tools stay callable.

## Tool belt (37 tools)

| Module | Tools |
| --- | --- |
| `skills/files.py` | `read_file`, `write_file`, `delete_file`*, `list_dir`, `read_pdf`, `markdown_report` |
| `skills/browser.py` | `browse_url` (Playwright → HTTP+BS4 fallback), `web_search` (DuckDuckGo), `wikipedia_summary` |
| `skills/coding.py` | `run_python` (isolated subprocess), `run_shell`* (dry run by default) |
| `skills/email.py` | `send_email` (Gmail SMTP, drafts `.eml` when unconfigured), `create_calendar_event` (Google Calendar hook → local `.ics`), `list_calendar_events` |
| `skills/smart_home.py` | `open_jio_admin`, `network_diagnostics`, `scan_local_ports` |
| `skills/system.py` | `get_system_info`, `list_processes`, `kill_process`*, `take_screenshot`, `describe_image` (GPT-4o / Ollama vision) |
| `skills/security.py` | `get_security_audit_log` |
| `skills/automation.py` | `get_current_time`, `calculator` (safe AST), `get_weather`, `stock_price`, `translate`, `add_reminder`, `list_reminders` |
| `memory.py` | `remember`, `recall`, `forget`, `search_long_term_memory` |
| `brain.py` | `delegate_to_crew` / `delegate_crew_swarm` |
| `plugins/` | `generate_stark_token`, `sha256_digest` (dynamic drop-ins) |

`*` gated by `confirm="yes"` and the catastrophic-command blocklist.

## Safety rails

1. `run_shell`, `delete_file`, and `kill_process` default to `confirm="no"` → **dry run**, printing
   the exact operation and refusing to act.
2. Catastrophic signatures (`rm -rf /`, `mkfs`, `dd of=/dev/sdX`, fork bombs, `curl | sh`, …) are
   hard-blocked even *with* `confirm="yes"`.
3. Every gated decision is appended to `data/logs/security_audit.jsonl` and readable via
   `get_security_audit_log`.
4. `calculator` parses with `ast` — no `eval`, no attribute access, no imports; unsafe input returns
   `[Calculator Error] … Unsupported or unsafe expression construct`.
5. `run_python` executes in a `-I` isolated subprocess with a hard timeout.

## Configuration (`.env`)

`OPENAI_API_KEY`, `MODEL`, `USER_NAME`, `VOICE_ENABLED`, `TTS_VOICE`, `WHISPER_MODEL`,
`OPENWEATHER_API_KEY`, `GMAIL_ADDRESS`, `GMAIL_APP_PASSWORD`, `TELEGRAM_BOT_TOKEN`,
`TELEGRAM_CHAT_ID`, `OLLAMA_MODEL`, `USE_LOCAL_LLM`, `GRADIO_PORT`.

Runtime overrides (no `.env` edit needed): `FRIDAY_DATA_DIR` (state vault location, honoured on every
call), `FRIDAY_PLUGINS_DIR` (alternate plugin folder).

## Voice, reminders, UIs

* **Voice** — `edge-tts` neural (default `en-GB-LibbyNeural`) → `pyttsx3` offline → silent; Whisper
  for STT via `listen()` / `transcribe_audio_file()`. Markdown and URLs are stripped before speaking.
* **`ReminderDaemon`** — background thread polling `reminders.json`, firing the callback, speaking the
  alert, and optionally pushing Telegram. Understands `in 15 minutes`, `18:30`, ISO timestamps.
* **Gradio** — chat (with `/crew`, `/status`, `/reset`), telemetry panel, Whisper transcription, TTS
  playback; bound to `0.0.0.0:GRADIO_PORT`.
* **Telegram** — `/start`, `/status`, `/reset`, `/crew <mission>` plus free-form chat, restricted to
  `TELEGRAM_CHAT_ID` when set.

## Plugins

Drop a module into `friday_ultra/plugins/`:

```python
from friday_ultra.config import friday_tool

@friday_tool
def weather_satellite(pass_id: str) -> str:
    """Return the next orbital pass for a satellite NORAD id."""
    ...
```

Anything decorated with `@friday_tool` (or exposing `name`/`description`/`invoke`) is picked up by
`load_plugins()` at boot and merged into `ALL_TOOLS`. Files starting with `_` are skipped.

## State vault

```
friday_ultra/data/           (override with FRIDAY_DATA_DIR)
├── chroma_db/               vector store
├── fallback_vectors.json    cosine index when ChromaDB is absent
├── notes.json               structured remember/recall facts
├── reminders.json           scheduler state
├── friday_memory.sqlite3    conversation history + LangGraph checkpoints
├── screenshots/             desktop & page captures
├── calendar/                .ics events (+ token.json for Google sync)
└── logs/security_audit.jsonl
```

## Verification

```bash
python3 tools/test_super_friday.py     # 37-tool, gated-action, daemon, spec & parity checks
python3 -m friday_ultra --self-test
python3 super_friday_max.py --self-test
```
