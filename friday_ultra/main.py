"""FRIDAY Ultra CLI: Rich terminal REPL, voice mode, dashboards, and reminder daemon orchestration."""
from __future__ import annotations

import argparse
import sys
import threading
from typing import Any

from friday_ultra import __version__
from friday_ultra.brain import SYSTEM, MaxFriday, run_crew_task
from friday_ultra.config import (
    GRADIO_PORT,
    MODEL,
    OLLAMA_MODEL,
    STATE,
    TELEGRAM_BOT_TOKEN,
    USER_NAME,
    VOICE_ENABLED,
)
from friday_ultra.memory import get_checkpointer  # noqa: F401  (ensures storage bootstraps early)
from friday_ultra.skills.automation import ReminderDaemon
from friday_ultra.voice import VOICE, listen


def _rich() -> tuple[Any, Any, Any, Any]:
    """Import Rich lazily, with graceful plain-text fallbacks."""
    try:
        from rich.console import Console
        from rich.markdown import Markdown
        from rich.panel import Panel
        from rich.table import Table

        return Console(), Markdown, Panel, Table
    except ImportError:
        return None, None, None, None


BANNER = r"""
   ______ ____  ___ ____   _____
  / ____// __ \/  _/ __ \ / ___/     S U P E R   F R I D A Y
 / /_   / /_/ // / / / / // __/      v3.0 MAX — "Nothing Left Behind"
/ __/  / _, _// / / /_/ // /___      Female Replacement Intelligent
/_/    /_/ |_/___/_____/ /_____/      Digital Assistant Youth
"""

HELP_TEXT = """
Commands:
  /help              Show this help panel
  /status            Full subsystem telemetry report
  /crew <mission>    Delegate to the 7-agent CrewAI swarm
  /voice             Toggle voice output on/off
  /listen [seconds]  Capture microphone and transcribe with Whisper
  /reminders         List pending reminders
  /memory            Long-term memory statistics
  /reset             Clear conversation thread (keeps long-term memory)
  /web               Launch Gradio web dashboard
  /telegram          Start the Telegram bot poller
  /clear             Clear the terminal
  /exit, /quit       Shut FRIDAY down
"""


def _print_banner(console: Any, Panel: Any) -> None:
    subtitle = f"{USER_NAME}".upper()
    if console is not None and Panel is not None:
        console.print(BANNER, style="bold cyan")
        console.print(
            Panel(
                f"User: [bold]{subtitle}[/bold]   |   Model: [bold]{OLLAMA_MODEL if STATE.use_local_llm else MODEL}[/bold]"
                f"   |   Voice: [bold]{'ON' if STATE.voice_enabled else 'OFF'}[/bold]"
                f"   |   Web port: [bold]{GRADIO_PORT}[/bold]",
                border_style="cyan",
                title=f"FRIDAY {__version__}",
            )
        )
        console.print("Type [bold]/help[/bold] for the command deck, or just talk to me.\n", style="dim")
    else:
        print(BANNER)
        print(f"FRIDAY {__version__} | {subtitle} | model={OLLAMA_MODEL if STATE.use_local_llm else MODEL}\n")


def _handle_command(
    command: str,
    friday: MaxFriday,
    console: Any,
    Markdown: Any,
    daemon: ReminderDaemon | None,
) -> bool:
    """Process a slash-command. Returns False when the REPL should exit."""
    parts = command.strip().split(maxsplit=1)
    verb = parts[0].lower()
    arg = parts[1].strip() if len(parts) > 1 else ""

    if verb in {"/exit", "/quit", "/q"}:
        return False
    if verb == "/help":
        print(HELP_TEXT)
        return True
    if verb == "/clear":
        print("\033[H\033[J", end="")
        return True
    if verb == "/status":
        report = friday.status_report()
        if console is not None:
            from rich.table import Table

            table = Table(title="FRIDAY Subsystem Telemetry", border_style="cyan")
            table.add_column("Subsystem", style="bold cyan")
            table.add_column("Value")
            for k, v in report.items():
                table.add_row(k, str(v))
            console.print(table)
        else:
            for k, v in report.items():
                print(f"{k}: {v}")
        return True
    if verb == "/crew":
        if not arg:
            print("Mission required, e.g. /crew research arc reactor alloys")
            return True
        print("Spinning up the 7-agent swarm — stand by.\n")
        print(friday.run_crew(arg))
        return True
    if verb == "/voice":
        STATE.voice_enabled = not STATE.voice_enabled
        print(f"Voice output {'enabled' if STATE.voice_enabled else 'muted'}.")
        return True
    if verb == "/listen":
        seconds = 5.0
        if arg:
            try:
                seconds = float(arg)
            except ValueError:
                seconds = 5.0
        print(f"Listening for {seconds:.0f}s …")
        heard = listen(duration=seconds)
        print(f"You said: {heard or '[nothing detected]'}")
        if heard and not heard.startswith("[STT Error]"):
            print(friday.chat(heard, thread_id=STATE.active_thread_id, speak_reply=True))
        return True
    if verb == "/reminders":
        from friday_ultra.memory import load_reminders

        reminders = [r for r in load_reminders() if not r.get("triggered")]
        if not reminders:
            print("No pending reminders.")
        for r in reminders:
            print(f"- [{r.get('id')}] {str(r.get('due_at', ''))[:19]} UTC → {r.get('message')}")
        return True
    if verb == "/memory":
        from friday_ultra.memory import load_notes, vector_memory_count

        print(f"Notes stored: {len(load_notes())} | Vector memories: {vector_memory_count()}")
        return True
    if verb == "/reset":
        print(friday.reset_thread(thread_id=STATE.active_thread_id))
        return True
    if verb == "/web":
        from friday_ultra.ui.web_dashboard import launch_gradio_dashboard

        try:
            launch_gradio_dashboard(friday=friday, blocking=False)
            print(f"Web dashboard live at {STATE.gradio_url}")
        except Exception as exc:
            print(f"[Web Error] {exc}")
        return True
    if verb == "/telegram":
        if not TELEGRAM_BOT_TOKEN:
            print("TELEGRAM_BOT_TOKEN is empty in .env.")
            return True
        from friday_ultra.ui.web_dashboard import start_telegram_bot

        print(start_telegram_bot(blocking=False, friday=friday))
        return True

    print(f"Unknown command '{verb}'. Try /help.")
    return True


def run_cli(args: argparse.Namespace) -> int:
    console, Markdown, Panel, _ = _rich()
    STATE.active_thread_id = args.thread
    if args.no_voice:
        STATE.voice_enabled = False

    friday = MaxFriday()
    _print_banner(console, Panel)

    def _on_reminder(item: dict[str, Any]) -> None:
        text = f"[Reminder] {item.get('message')}"
        if console is not None:
            console.print(text, style="bold yellow")
        else:
            print(text)
        if TELEGRAM_BOT_TOKEN:
            from friday_ultra.ui.web_dashboard import send_telegram_notification

            send_telegram_notification(item.get("message", ""))

    daemon = ReminderDaemon(on_due_callback=_on_reminder, poll_interval=5.0).start()

    if args.web:
        from friday_ultra.ui.web_dashboard import launch_gradio_dashboard

        try:
            launch_gradio_dashboard(friday=friday, blocking=False)
            print(f"Web dashboard live at {STATE.gradio_url}")
        except Exception as exc:
            print(f"[Web Error] {exc}")
    if args.telegram:
        from friday_ultra.ui.web_dashboard import start_telegram_bot

        print(start_telegram_bot(blocking=False, friday=friday))

    if args.listen:
        heard = listen(duration=args.listen)
        print(f"You said: {heard}")
        if heard and not heard.startswith("[STT Error]"):
            print(friday.chat(heard, thread_id=STATE.active_thread_id, speak_reply=True))
        daemon.stop()
        return 0

    while True:
        try:
            raw = input(f"\n{STATE.user_name} ❯ ")
        except (EOFError, KeyboardInterrupt):
            print("\nShutting down. Goodnight.")
            break

        user_input = raw.strip()
        if not user_input:
            continue
        if user_input.startswith("/"):
            if not _handle_command(user_input, friday, console, Markdown, daemon):
                print("Powering down. Try not to dismantle anything without me.")
                break
            continue

        try:
            answer = friday.chat(user_input, thread_id=STATE.active_thread_id, speak_reply=STATE.voice_enabled)
        except Exception as exc:
            answer = f"My neural mesh hiccuped: {exc}"

        if console is not None and Markdown is not None:
            console.print(Panel(Markdown(answer), border_style="cyan", title="FRIDAY"))
        else:
            print(f"\nFRIDAY: {answer}\n")

    daemon.stop()
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="friday_ultra",
        description="SUPER FRIDAY v3.0 MAX — 'Nothing Left Behind' Edition",
    )
    parser.add_argument("--web", action="store_true", help="Launch the Gradio web dashboard on boot")
    parser.add_argument("--telegram", action="store_true", help="Launch the Telegram bot on boot")
    parser.add_argument("--voice", action="store_true", help="Force voice output on")
    parser.add_argument("--no-voice", action="store_true", help="Mute voice output for this session")
    parser.add_argument("--listen", type=float, default=0.0, metavar="SECONDS", help="One-shot mic capture, transcribe, and reply")
    parser.add_argument("--crew", type=str, default="", metavar="GOAL", help="Run a one-shot 7-agent swarm mission and exit")
    parser.add_argument("--thread", type=str, default="main", help="Conversation thread id (default: main)")
    parser.add_argument("--status", action="store_true", help="Print the subsystem telemetry report and exit")
    parser.add_argument("--self-test", action="store_true", help="Run internal subsystem diagnostics and exit")
    parser.add_argument("--version", action="store_true", help="Print version and exit")
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    if args.version:
        print(f"FRIDAY Ultra {__version__} — SYSTEM prompt {len(SYSTEM)} chars")
        return 0
    if args.voice:
        STATE.voice_enabled = True
    if args.status:
        friday = MaxFriday()
        for k, v in friday.status_report().items():
            print(f"{k}: {v}")
        return 0
    if args.self_test:
        return run_self_test()
    if args.crew:
        print(run_crew_task(args.crew))
        return 0
    return run_cli(args)


def run_self_test() -> int:
    """Smoke-test every non-network subsystem and print a pass/fail matrix."""
    from friday_ultra.skills.automation import calculator
    from friday_ultra.skills.coding import run_python, run_shell
    from friday_ultra.skills.files import write_file
    from friday_ultra.skills.system import get_system_info
    from friday_ultra.memory import recall, remember, search_long_term_memory, vector_memory_count
    from friday_ultra.plugins import load_plugins

    checks: list[tuple[str, bool, str]] = []

    def _check(label: str, fn: Any) -> None:
        try:
            checks.append((label, True, str(fn())[:120].replace("\n", " ")))
        except Exception as exc:
            checks.append((label, False, str(exc)[:120]))

    friday = MaxFriday()
    _check("tool belt", lambda: f"{len(friday.tools)} tools bound")
    _check("calculator", lambda: calculator.invoke({"expression": "sqrt(144) + 2 ** 5"}))
    _check("unsafe AST rejected", lambda: calculator.invoke({"expression": "__import__('os').system('id')"}))
    _check("shell dry-run gate", lambda: run_shell.invoke({"command": "echo gate", "confirm": "no"}))
    _check("shell blocklist", lambda: run_shell.invoke({"command": "rm -rf /", "confirm": "yes"}))
    _check("python sandbox", lambda: run_python.invoke({"code": "print(6 * 7)"}))
    _check("system telemetry", lambda: get_system_info.invoke({}))
    _check("memory write", lambda: remember.invoke({"key": "self_test_probe", "value": "nominal"}))
    _check("memory read", lambda: recall.invoke({"query": "self_test_probe"}))
    _check("semantic search", lambda: search_long_term_memory.invoke({"query": "self test", "top_k": 2}))
    _check("plugins", lambda: f"{len(load_plugins())} loaded")
    _check("vector store", lambda: f"{vector_memory_count()} vectors")
    _check("scratch file", lambda: write_file.invoke({"file_path": "/tmp/friday_self_test.txt", "content": "ok"}))

    width = max(len(c[0]) for c in checks) + 2
    print(f"\n{'SUBSYSTEM':<{width}} RESULT  DETAIL")
    print("-" * (width + 60))
    failures = 0
    for label, ok, detail in checks:
        failures += 0 if ok else 1
        print(f"{label:<{width}} {'PASS' if ok else 'FAIL':<6}  {detail}")
    print("-" * (width + 60))
    print(f"{len(checks) - failures}/{len(checks)} subsystems nominal.")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
