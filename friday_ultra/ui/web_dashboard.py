"""FRIDAY Ultra Web Dashboard (Gradio 5) and Background Telegram Bot Bridge."""
from __future__ import annotations

import asyncio
import os
import threading
import time
from pathlib import Path
from typing import Any, Callable

from friday_ultra.config import (
    GRADIO_PORT,
    STATE,
    TELEGRAM_BOT_TOKEN,
    TELEGRAM_CHAT_ID,
    get_data_dir,
)

try:
    import gradio as gr  # type: ignore

    GRADIO_AVAILABLE = True
except ImportError:
    gr = None  # type: ignore
    GRADIO_AVAILABLE = False

try:
    import requests  # type: ignore

    REQUESTS_AVAILABLE = True
except ImportError:
    requests = None  # type: ignore
    REQUESTS_AVAILABLE = False

try:
    from telegram import Update  # type: ignore
    from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes, MessageHandler, filters  # type: ignore

    TELEGRAM_AVAILABLE = True
except ImportError:
    Update = Any  # type: ignore
    ApplicationBuilder = None  # type: ignore
    CommandHandler = None  # type: ignore
    ContextTypes = Any  # type: ignore
    MessageHandler = None  # type: ignore
    filters = None  # type: ignore
    TELEGRAM_AVAILABLE = False


_TELEGRAM_THREAD: threading.Thread | None = None
_TELEGRAM_APP: Any = None


_FRIDAY_SINGLETON: Any = None
_FRIDAY_LOCK = threading.Lock()


def _get_friday() -> Any:
    """Lazily import and instantiate the singleton MaxFriday brain (avoids import cycles)."""
    global _FRIDAY_SINGLETON
    if _FRIDAY_SINGLETON is not None:
        return _FRIDAY_SINGLETON
    with _FRIDAY_LOCK:
        if _FRIDAY_SINGLETON is None:
            from friday_ultra.brain import MaxFriday

            _FRIDAY_SINGLETON = MaxFriday()
    return _FRIDAY_SINGLETON


# ====================== TELEGRAM NOTIFICATION BRIDGE ======================
def send_telegram_notification(message: str, chat_id: str = "") -> str:
    """Push an outbound alert to Telegram via the Bot API (used by the ReminderDaemon)."""
    token = os.environ.get("TELEGRAM_BOT_TOKEN", TELEGRAM_BOT_TOKEN).strip()
    target_chat = (chat_id or os.environ.get("TELEGRAM_CHAT_ID", TELEGRAM_CHAT_ID)).strip()
    if not token or not target_chat:
        return "[Telegram] TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID not configured."
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    payload = {"chat_id": target_chat, "text": message[:4000], "parse_mode": "Markdown"}

    try:
        if REQUESTS_AVAILABLE and requests is not None:
            resp = requests.post(url, json=payload, timeout=12)
            resp.raise_for_status()
            return f"[Telegram] Dispatched to chat {target_chat}."
        import json
        import urllib.request

        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=12) as resp:  # noqa: S310
            resp.read()
        return f"[Telegram] Dispatched to chat {target_chat}."
    except Exception as exc:
        return f"[Telegram Error] {exc}"


def _telegram_authorized(chat_id: Any) -> bool:
    expected = os.environ.get("TELEGRAM_CHAT_ID", TELEGRAM_CHAT_ID).strip()
    if not expected:
        return True
    return str(chat_id) == expected


def start_telegram_bot(blocking: bool = False, friday: Any = None) -> str:
    """Launch FRIDAY's Telegram bot in a background daemon thread (python-telegram-bot v20+)."""
    global _TELEGRAM_THREAD, _TELEGRAM_APP

    token = os.environ.get("TELEGRAM_BOT_TOKEN", TELEGRAM_BOT_TOKEN).strip()
    if not token:
        return "[Telegram] TELEGRAM_BOT_TOKEN is empty — bot not started."
    if not TELEGRAM_AVAILABLE:
        return "[Telegram] python-telegram-bot is not installed. Run: pip install python-telegram-bot"
    if _TELEGRAM_THREAD is not None and _TELEGRAM_THREAD.is_alive():
        return "[Telegram] Bot is already running."

    brain = friday or _get_friday()

    async def _cmd_start(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        await update.message.reply_text(
            f"FRIDAY online, {STATE.user_name}. All systems nominal — talk to me. "
            f"Commands: /status, /reset, /crew <mission>"
        )

    async def _cmd_status(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        report = brain.status_report()
        lines = [f"*{report['assistant']} — Systems Report*"]
        lines += [f"• {k}: `{v}`" for k, v in report.items() if k != "assistant"]
        await update.message.reply_text("\n".join(lines), parse_mode="Markdown")

    async def _cmd_reset(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        await update.message.reply_text(brain.reset_thread(thread_id=f"tg_{update.effective_chat.id}"))

    async def _cmd_crew(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            return
        goal = " ".join(context.args) if context.args else ""
        if not goal:
            await update.message.reply_text("Provide a mission, e.g. /crew research arc reactor materials")
            return
        await update.message.reply_text("Spinning up the 7-agent swarm. Stand by.")
        result = await asyncio.to_thread(brain.run_crew, goal)
        await update.message.reply_text(result[:4000] or "Swarm returned empty, curiously.")

    async def _on_message(update: Any, context: Any) -> None:
        if not _telegram_authorized(update.effective_chat.id):
            await update.message.reply_text("Unauthorized. Access denied.")
            return
        text = (update.message.text or "").strip()
        if not text:
            return
        await update.message.chat.send_action("typing")
        answer = await asyncio.to_thread(brain.chat, text, f"tg_{update.effective_chat.id}")
        for chunk_start in range(0, len(answer), 4000):
            await update.message.reply_text(answer[chunk_start : chunk_start + 4000])

    try:
        app = ApplicationBuilder().token(token).build()
        app.add_handler(CommandHandler("start", _cmd_start))
        app.add_handler(CommandHandler("status", _cmd_status))
        app.add_handler(CommandHandler("reset", _cmd_reset))
        app.add_handler(CommandHandler("crew", _cmd_crew))
        app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, _on_message))
        _TELEGRAM_APP = app

        if blocking:
            STATE.telegram_running = True
            try:
                app.run_polling(stop_signals=None, close_loop=False)
            finally:
                STATE.telegram_running = False
            return "[Telegram] Bot polling stopped."

        def _runner() -> None:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            STATE.telegram_running = True
            try:
                app.run_polling(stop_signals=None, close_loop=False)
            except Exception:
                pass
            finally:
                STATE.telegram_running = False

        _TELEGRAM_THREAD = threading.Thread(target=_runner, name="FridayTelegramBot", daemon=True)
        _TELEGRAM_THREAD.start()
        return f"[Telegram] FRIDAY bot polling started in background, {STATE.user_name}."
    except Exception as exc:
        return f"[Telegram Error] Failed to start bot: {exc}"


# ====================== GRADIO WEB DASHBOARD ======================
def build_gradio_app(friday: Any = None) -> Any:
    """Construct the FRIDAY Gradio web interface (chat, voice reply, status panel, file tools)."""
    if not GRADIO_AVAILABLE or gr is None:
        raise RuntimeError("gradio is not installed. Run: pip install gradio")

    brain = friday or _get_friday()

    def _respond(message: str, history: list[Any], voice_reply: bool) -> tuple[str, list[Any]]:
        text = (message or "").strip()
        if not text:
            return "", history or []
        if text.lower() in {"status", "system status", "/status"}:
            answer = "```\n" + "\n".join(f"{k}: {v}" for k, v in brain.status_report().items()) + "\n```"
        elif text.lower().startswith("/crew "):
            answer = brain.run_crew(text[6:].strip())
        elif text.lower().startswith("/reset"):
            answer = brain.reset_thread(thread_id="web")
        else:
            answer = brain.chat(text, thread_id="web")

        updated = list(history or [])
        updated.append({"role": "user", "content": text})
        updated.append({"role": "assistant", "content": answer})

        if voice_reply:
            try:
                from friday_ultra.voice import VOICE

                audio_path = VOICE.synthesize_to_file(answer)
                if audio_path is not None:
                    return "", updated
            except Exception:
                pass
        return "", updated

    def _voice_reply_file(message: str) -> str | None:
        text = (message or "").strip()
        if not text:
            return None
        from friday_ultra.voice import VOICE

        audio = VOICE.synthesize_to_file(text)
        return str(audio) if audio is not None else None

    def _transcribe_upload(audio_path: str | None) -> str:
        if not audio_path:
            return ""
        from friday_ultra.voice import transcribe_audio_file

        return transcribe_audio_file(audio_path)

    with gr.Blocks(title="FRIDAY — Super AI Interface", theme=gr.themes.Soft(primary_hue="cyan")) as app:
        gr.Markdown(
            f"""
            # F.R.I.D.A.Y. — Super v3.0 MAX
            **Female Replacement Intelligent Digital Assistant Youth** · Tony Stark's executive AI
            Speaking to **{STATE.user_name}** · Tools online: **{len(brain.tools)}**
            """
        )
        with gr.Row():
            with gr.Column(scale=3):
                chatbot = gr.Chatbot(type="messages", height=520, label="Secure Channel")
                with gr.Row():
                    msg_box = gr.Textbox(
                        placeholder="Ask FRIDAY anything, or try /crew <mission>, /status, /reset …",
                        show_label=False,
                        scale=5,
                    )
                    send_btn = gr.Button("Send", variant="primary", scale=1)
                with gr.Row():
                    voice_toggle = gr.Checkbox(value=False, label="Speak replies (edge-tts)")
                    clear_btn = gr.Button("Reset Conversation")
            with gr.Column(scale=1):
                gr.Markdown("### Diagnostics")
                status_btn = gr.Button("System Status")
                status_out = gr.Textbox(lines=16, interactive=False, label="Telemetry")
                gr.Markdown("### Voice")
                audio_in = gr.Audio(type="filepath", label="Speak / Upload Audio")
                transcribe_btn = gr.Button("Transcribe (Whisper)")
                transcript_out = gr.Textbox(lines=3, interactive=False, label="Transcript")
                tts_in = gr.Textbox(label="Text to Speech")
                tts_btn = gr.Button("Synthesize Voice")
                tts_out = gr.Audio(label="FRIDAY Voice", type="filepath")

        send_btn.click(_respond, [msg_box, chatbot, voice_toggle], [msg_box, chatbot])
        msg_box.submit(_respond, [msg_box, chatbot, voice_toggle], [msg_box, chatbot])

        def _clear() -> tuple[list[Any], str]:
            text = brain.reset_thread(thread_id="web")
            return [], text

        clear_btn.click(_clear, None, [chatbot, msg_box])
        status_btn.click(lambda: "\n".join(f"{k}: {v}" for k, v in brain.status_report().items()), None, status_out)
        transcribe_btn.click(_transcribe_upload, [audio_in], [transcript_out])
        tts_btn.click(_voice_reply_file, [tts_in], [tts_out])

    return app


def launch_gradio_dashboard(friday: Any = None, share: bool = False, blocking: bool = True) -> Any:
    """Launch the FRIDAY Gradio dashboard on 0.0.0.0:GRADIO_PORT and register the live URL."""
    app = build_gradio_app(friday=friday)
    port = int(os.environ.get("GRADIO_PORT", GRADIO_PORT))
    STATE.gradio_url = f"http://0.0.0.0:{port}"
    return app.launch(
        server_name="0.0.0.0",
        server_port=port,
        share=share,
        prevent_thread_lock=not blocking,
        inbrowser=False,
        show_error=True,
        quiet=True,
    )


def start_dashboards(friday: Any = None, web: bool = True, telegram: bool = True) -> dict[str, str]:
    """Boot both web dashboard and Telegram bridge, returning their status messages."""
    results: dict[str, str] = {}
    if telegram:
        results["telegram"] = start_telegram_bot(blocking=False, friday=friday)
    if web:
        try:
            launch_gradio_dashboard(friday=friday, blocking=False)
            results["web"] = f"Web dashboard live at {STATE.gradio_url}"
        except Exception as exc:
            results["web"] = f"[Web Error] {exc}"
    return results


__all__ = [
    "build_gradio_app",
    "launch_gradio_dashboard",
    "send_telegram_notification",
    "start_dashboards",
    "start_telegram_bot",
]
