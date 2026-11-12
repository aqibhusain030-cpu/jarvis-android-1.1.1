"""Automation Skills: Time, Safe AST Calculator, Weather, Stocks, Translation, and Reminder Daemon."""
from __future__ import annotations

import ast
import json
import math
import operator
import os
import re
import threading
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any, Callable
from zoneinfo import ZoneInfo

from friday_ultra.config import OPENWEATHER_API_KEY, STATE, friday_tool
from friday_ultra.memory import load_reminders, save_reminders
from friday_ultra.voice import speak

try:
    import requests  # type: ignore

    REQUESTS_AVAILABLE = True
except ImportError:
    requests = None  # type: ignore
    REQUESTS_AVAILABLE = False

try:
    import yfinance as yf  # type: ignore

    YFINANCE_AVAILABLE = True
except ImportError:
    yf = None  # type: ignore
    YFINANCE_AVAILABLE = False

try:
    from deep_translator import GoogleTranslator  # type: ignore

    TRANSLATOR_AVAILABLE = True
except ImportError:
    GoogleTranslator = None  # type: ignore
    TRANSLATOR_AVAILABLE = False


# ====================== SAFE AST SCIENTIFIC CALCULATOR ======================
_ALLOWED_BINOPS: dict[type[ast.operator], Callable[[Any, Any], Any]] = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod,
    ast.Pow: operator.pow,
}

_ALLOWED_UNARYOPS: dict[type[ast.unaryop], Callable[[Any], Any]] = {
    ast.UAdd: operator.pos,
    ast.USub: operator.neg,
}

_ALLOWED_MATH_NAMES: dict[str, Any] = {
    "pi": math.pi,
    "e": math.e,
    "tau": math.tau,
    "sqrt": math.sqrt,
    "sin": math.sin,
    "cos": math.cos,
    "tan": math.tan,
    "asin": math.asin,
    "acos": math.acos,
    "atan": math.atan,
    "log": math.log,
    "log10": math.log10,
    "log2": math.log2,
    "exp": math.exp,
    "factorial": math.factorial,
    "gcd": math.gcd,
    "ceil": math.ceil,
    "floor": math.floor,
    "abs": abs,
    "round": round,
    "degrees": math.degrees,
    "radians": math.radians,
    "hypot": math.hypot,
    "pow": pow,
    "min": min,
    "max": max,
}


def _eval_ast_node(node: ast.AST) -> float | int:
    if isinstance(node, ast.Expression):
        return _eval_ast_node(node.body)
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)) and not isinstance(node.value, bool):
        return node.value
    if isinstance(node, ast.UnaryOp) and type(node.op) in _ALLOWED_UNARYOPS:
        return _ALLOWED_UNARYOPS[type(node.op)](_eval_ast_node(node.operand))
    if isinstance(node, ast.BinOp) and type(node.op) in _ALLOWED_BINOPS:
        left = _eval_ast_node(node.left)
        right = _eval_ast_node(node.right)
        if isinstance(node.op, ast.Pow) and abs(right) > 4096:
            raise ValueError("Exponent magnitude exceeds safety ceiling (4096).")
        return _ALLOWED_BINOPS[type(node.op)](left, right)
    if isinstance(node, ast.Name) and node.id in _ALLOWED_MATH_NAMES:
        val = _ALLOWED_MATH_NAMES[node.id]
        if isinstance(val, (int, float)):
            return val
        raise ValueError(f"'{node.id}' is a function, not a numeric constant.")
    if isinstance(node, ast.Call) and isinstance(node.func, ast.Name) and node.func.id in _ALLOWED_MATH_NAMES:
        fn = _ALLOWED_MATH_NAMES[node.func.id]
        if not callable(fn) or node.keywords:
            raise ValueError(f"Invalid function call: {node.func.id}")
        args = [_eval_ast_node(arg) for arg in node.args]
        return fn(*args)
    raise ValueError(f"Unsupported or unsafe expression construct: {type(node).__name__}")


@friday_tool
def get_current_time(timezone_name: str = "local") -> str:
    """Return the exact current date, weekday, time, and ISO-8601 timestamp in local or IANA timezone."""
    tz_label = timezone_name.strip() or "local"
    try:
        if tz_label.lower() in {"local", "system", ""}:
            now = datetime.now().astimezone()
            zone_display = str(now.tzinfo or "local")
        elif tz_label.upper() == "UTC":
            now = datetime.now(timezone.utc)
            zone_display = "UTC"
        else:
            now = datetime.now(ZoneInfo(tz_label))
            zone_display = tz_label
        return (
            f"Current Time ({zone_display}): {now.strftime('%A, %Y-%m-%d %H:%M:%S %Z')} "
            f"(ISO: {now.isoformat()})"
        )
    except Exception as exc:
        now = datetime.now(timezone.utc)
        return f"Current Time (UTC fallback; '{tz_label}' invalid: {exc}): {now.isoformat()}"


@friday_tool
def calculator(expression: str) -> str:
    """Evaluate a mathematical or scientific expression safely using AST parsing (never eval)."""
    cleaned = expression.strip().replace("^", "**")
    if not cleaned:
        return "Provide a mathematical expression to evaluate."
    try:
        parsed = ast.parse(cleaned, mode="eval")
        result = _eval_ast_node(parsed)
        if isinstance(result, float) and result.is_integer():
            result = int(result)
        return f"{expression.strip()} = {result}"
    except Exception as exc:
        return f"[Calculator Error] Invalid expression '{expression}': {exc}"


def _http_get_json(url: str, timeout: int = 10) -> dict[str, Any]:
    headers = {"User-Agent": "FRIDAY-Weather-Telemetry/3.0"}
    if REQUESTS_AVAILABLE and requests is not None:
        resp = requests.get(url, headers=headers, timeout=timeout)
        resp.raise_for_status()
        return dict(resp.json())
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=timeout) as resp:  # noqa: S310
        return dict(json.loads(resp.read().decode("utf-8")))


@friday_tool
def get_weather(city: str) -> str:
    """Fetch live weather telemetry for a city via OpenWeatherMap (with wttr.in live fallback)."""
    clean_city = city.strip()
    if not clean_city:
        return "Please specify a city name."

    owm_key = os.environ.get("OPENWEATHER_API_KEY", OPENWEATHER_API_KEY).strip()
    if owm_key:
        try:
            q = urllib.parse.quote(clean_city)
            url = f"https://api.openweathermap.org/data/2.5/weather?q={q}&appid={owm_key}&units=metric"
            data = _http_get_json(url)
            main = data.get("main", {})
            wind = data.get("wind", {})
            desc = (data.get("weather") or [{}])[0].get("description", "clear").capitalize()
            country = data.get("sys", {}).get("country", "")
            return (
                f"Weather in {data.get('name', clean_city)}, {country}: {desc} | "
                f"Temp: {main.get('temp')}°C (Feels like {main.get('feels_like')}°C) | "
                f"Humidity: {main.get('humidity')}% | Wind: {wind.get('speed')} m/s"
            )
        except Exception:
            pass

    try:
        q = urllib.parse.quote(clean_city)
        data = _http_get_json(f"https://wttr.in/{q}?format=j1")
        curr = (data.get("current_condition") or [{}])[0]
        desc = (curr.get("weatherDesc") or [{}])[0].get("value", "Unknown")
        return (
            f"Weather in {clean_city}: {desc} | "
            f"Temp: {curr.get('temp_C')}°C (Feels like {curr.get('FeelsLikeC')}°C) | "
            f"Humidity: {curr.get('humidity')}% | Wind: {curr.get('windspeedKmph')} km/h"
        )
    except Exception as exc:
        return f"[Weather Error] Could not retrieve live weather for '{clean_city}': {exc}"


@friday_tool
def stock_price(ticker: str) -> str:
    """Fetch real-time / latest stock or index quote, daily delta, and 5-day range using yfinance."""
    sym = ticker.strip().upper()
    if not sym:
        return "Please provide a stock or index ticker symbol (e.g. 'AAPL', 'NVDA', 'RELIANCE.NS')."
    if not YFINANCE_AVAILABLE or yf is None:
        return "[Finance Error] yfinance is not installed. Run: pip install yfinance"

    try:
        tk = yf.Ticker(sym)
        hist = tk.history(period="5d")
        if hist is None or hist.empty:
            return f"No market data returned for ticker '{sym}'."
        latest_close = float(hist["Close"].iloc[-1])
        prev_close = float(hist["Close"].iloc[-2]) if len(hist) >= 2 else latest_close
        delta = latest_close - prev_close
        pct = (delta / prev_close * 100.0) if prev_close else 0.0
        high_5d = float(hist["High"].max())
        low_5d = float(hist["Low"].min())
        direction = "+" if delta >= 0 else ""
        return (
            f"Ticker {sym}: {latest_close:,.2f} ({direction}{delta:,.2f} / {direction}{pct:.2f}% vs prior session) | "
            f"5-Day Range: {low_5d:,.2f} – {high_5d:,.2f}"
        )
    except Exception as exc:
        return f"[Finance Error] Failed to fetch quote for '{sym}': {exc}"


@friday_tool
def translate(text: str, target_language: str = "en", source_language: str = "auto") -> str:
    """Translate text into any target language code or name using deep-translator."""
    if not text.strip():
        return "Provide non-empty text to translate."
    if not TRANSLATOR_AVAILABLE or GoogleTranslator is None:
        return "[Translator Error] deep-translator is not installed. Run: pip install deep-translator"

    try:
        translated = GoogleTranslator(
            source=source_language.strip() or "auto",
            target=target_language.strip() or "en",
        ).translate(text.strip())
        return f"Translation ({source_language} -> {target_language}):\n{translated}"
    except Exception as exc:
        return f"[Translator Error] {exc}"


# ====================== REMINDERS & BACKGROUND DAEMON ======================
_REMINDER_LOCK = threading.RLock()


def _parse_reminder_when(when: str) -> datetime:
    now = datetime.now(timezone.utc)
    raw = when.strip()

    rel_match = re.match(
        r"^(?:in\s+)?(\d+(?:\.\d+)?)\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days)$",
        raw,
        flags=re.IGNORECASE,
    )
    if rel_match:
        amount = float(rel_match.group(1))
        unit = rel_match.group(2).lower()
        if unit.startswith("s"):
            return now + timedelta(seconds=amount)
        if unit.startswith("m"):
            return now + timedelta(minutes=amount)
        if unit.startswith("h"):
            return now + timedelta(hours=amount)
        return now + timedelta(days=amount)

    hhmm_match = re.match(r"^(\d{1,2}):(\d{2})(?::(\d{2}))?$", raw)
    if hhmm_match:
        local_now = datetime.now().astimezone()
        target_local = local_now.replace(
            hour=int(hhmm_match.group(1)),
            minute=int(hhmm_match.group(2)),
            second=int(hhmm_match.group(3) or 0),
            microsecond=0,
        )
        if target_local <= local_now:
            target_local += timedelta(days=1)
        return target_local.astimezone(timezone.utc)

    cleaned = raw.replace("Z", "+00:00")
    for fmt in (None, "%Y-%m-%d %H:%M", "%Y-%m-%d %H:%M:%S"):
        try:
            dt = datetime.fromisoformat(cleaned) if fmt is None else datetime.strptime(cleaned, fmt)
            if dt.tzinfo is None:
                dt = dt.astimezone().astimezone(timezone.utc)
            else:
                dt = dt.astimezone(timezone.utc)
            return dt
        except ValueError:
            continue

    raise ValueError(
        f"Unrecognized reminder time '{when}'. Use formats like 'in 10 minutes', '18:30', or '2026-11-12 19:00'."
    )


@friday_tool
def add_reminder(message: str, when: str) -> str:
    """Schedule a persistent reminder in reminders.json ('in 15 minutes', '18:30', or 'YYYY-MM-DD HH:MM')."""
    clean_msg = message.strip()
    if not clean_msg:
        return "Reminder message cannot be empty."
    try:
        due_dt = _parse_reminder_when(when)
    except ValueError as exc:
        return f"[Reminder Error] {exc}"

    item = {
        "id": uuid.uuid4().hex[:8],
        "message": clean_msg,
        "due_at": due_dt.isoformat(),
        "created_at": datetime.now(timezone.utc).isoformat(),
        "triggered": False,
    }
    with _REMINDER_LOCK:
        reminders = load_reminders()
        reminders.append(item)
        save_reminders(reminders)

    return (
        f"Reminder [{item['id']}] set for {due_dt.strftime('%Y-%m-%d %H:%M:%S UTC')}: "
        f"'{clean_msg}', {STATE.user_name}."
    )


@friday_tool
def list_reminders(include_completed: bool = False) -> str:
    """List pending (and optionally completed) reminders from reminders.json."""
    with _REMINDER_LOCK:
        reminders = load_reminders()

    filtered = [r for r in reminders if include_completed or not r.get("triggered", False)]
    if not filtered:
        return f"No active reminders pending, {STATE.user_name}."

    lines = ["Scheduled Reminders:"]
    for r in sorted(filtered, key=lambda x: str(x.get("due_at", ""))):
        status = "DONE" if r.get("triggered") else "PENDING"
        lines.append(f"- [{r.get('id')}] ({status}) Due {str(r.get('due_at', ''))[:19]} UTC -> {r.get('message')}")
    return "\n".join(lines)


class ReminderDaemon:
    """Background daemon thread polling reminders.json and firing voice/UI/Telegram alerts on time."""

    def __init__(
        self,
        on_due_callback: Callable[[dict[str, Any]], None] | None = None,
        poll_interval: float = 5.0,
    ) -> None:
        self.on_due_callback = on_due_callback
        self.poll_interval = max(0.1, float(poll_interval))
        self._stop_event = threading.Event()
        self._thread: threading.Thread | None = None

    def check_once(self) -> list[dict[str, Any]]:
        due_items: list[dict[str, Any]] = []
        now = datetime.now(timezone.utc)
        with _REMINDER_LOCK:
            reminders = load_reminders()
            changed = False
            for r in reminders:
                if r.get("triggered"):
                    continue
                try:
                    due_dt = datetime.fromisoformat(str(r["due_at"]).replace("Z", "+00:00"))
                    if due_dt.tzinfo is None:
                        due_dt = due_dt.replace(tzinfo=timezone.utc)
                except Exception:
                    continue
                if due_dt <= now:
                    r["triggered"] = True
                    r["triggered_at"] = now.isoformat()
                    changed = True
                    due_items.append(dict(r))
            if changed:
                save_reminders(reminders)

        for item in due_items:
            alert_text = f"Reminder alert, {STATE.user_name}: {item.get('message')}"
            if self.on_due_callback is not None:
                try:
                    self.on_due_callback(item)
                except Exception:
                    pass
            speak(alert_text)
        return due_items

    def _run_loop(self) -> None:
        while not self._stop_event.is_set():
            try:
                self.check_once()
            except Exception:
                pass
            self._stop_event.wait(self.poll_interval)

    def start(self) -> "ReminderDaemon":
        if self._thread is None or not self._thread.is_alive():
            self._stop_event.clear()
            self._thread = threading.Thread(target=self._run_loop, name="FridayReminderDaemon", daemon=True)
            self._thread.start()
        return self

    def stop(self) -> None:
        self._stop_event.set()
        if self._thread is not None and self._thread.is_alive():
            self._thread.join(timeout=2.0)


def start_reminder_checker(
    on_due_callback: Callable[[dict[str, Any]], None] | None = None,
    poll_interval: float = 5.0,
) -> ReminderDaemon:
    """Start and return FRIDAY's background reminder checker daemon."""
    return ReminderDaemon(on_due_callback=on_due_callback, poll_interval=poll_interval).start()


AUTOMATION_TOOLS = [
    get_current_time,
    calculator,
    get_weather,
    stock_price,
    translate,
    add_reminder,
    list_reminders,
]
