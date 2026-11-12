"""Outbound Gmail SMTP Dispatch and Google Calendar Ready Integration Hooks."""
from __future__ import annotations

import json
import mimetypes
import os
import re
import smtplib
import uuid
from datetime import datetime, timedelta, timezone
from email.message import EmailMessage
from pathlib import Path
from typing import Any

from friday_ultra.config import GMAIL_ADDRESS, GMAIL_APP_PASSWORD, STATE, friday_tool, get_data_dir
from friday_ultra.skills.security import log_security_event


def _calendar_json_path() -> Path:
    cal_dir = get_data_dir() / "calendar"
    cal_dir.mkdir(parents=True, exist_ok=True)
    return cal_dir / "calendar_events.json"


def _load_local_events() -> list[dict[str, Any]]:
    path = _calendar_json_path()
    if not path.is_file():
        return []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return data if isinstance(data, list) else []
    except (json.JSONDecodeError, OSError):
        return []


def _save_local_events(events: list[dict[str, Any]]) -> None:
    path = _calendar_json_path()
    path.write_text(json.dumps(events, indent=2, ensure_ascii=False), encoding="utf-8")


def _parse_event_dt(raw: str) -> datetime:
    cleaned = raw.strip().replace("Z", "+00:00")
    for fmt in (None, "%Y-%m-%d %H:%M", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            if fmt is None:
                dt = datetime.fromisoformat(cleaned)
            else:
                dt = datetime.strptime(cleaned, fmt)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=timezone.utc)
            return dt
        except ValueError:
            continue
    return datetime.now(timezone.utc) + timedelta(hours=1)


def _try_google_calendar_service() -> Any | None:
    """Return an authenticated Google Calendar v3 service when google_token.json is configured."""
    token_file = get_data_dir() / "calendar" / "google_token.json"
    if not token_file.is_file():
        return None
    try:
        from google.oauth2.credentials import Credentials  # type: ignore
        from googleapiclient.discovery import build  # type: ignore

        creds = Credentials.from_authorized_user_file(
            str(token_file),
            ["https://www.googleapis.com/auth/calendar"],
        )
        if creds and creds.valid:
            return build("calendar", "v3", credentials=creds, cache_discovery=False)
    except Exception:
        return None
    return None


@friday_tool
def send_email(
    to_address: str,
    subject: str,
    body: str,
    attachment_path: str = "",
) -> str:
    """Send an email via Gmail SMTP SSL using GMAIL_ADDRESS and GMAIL_APP_PASSWORD."""
    sender = os.environ.get("GMAIL_ADDRESS", GMAIL_ADDRESS).strip()
    password = os.environ.get("GMAIL_APP_PASSWORD", GMAIL_APP_PASSWORD).strip()
    recipient = to_address.strip()

    if not re.match(r"^[^@\s]+@[^@\s]+\.[^@\s]+$", recipient):
        return f"[Email Error] Invalid recipient email address: '{recipient}'"

    if not sender or not password:
        log_security_event(
            action="send_email",
            target=f"{recipient} ({subject})",
            status="SKIPPED_MISSING_CREDENTIALS",
            confirm="yes",
        )
        return (
            f"[Email Unconfigured] GMAIL_ADDRESS / GMAIL_APP_PASSWORD are not set in .env, {STATE.user_name}. "
            f"Draft prepared for {recipient} with subject '{subject}'."
        )

    msg = EmailMessage()
    msg["From"] = sender
    msg["To"] = recipient
    msg["Subject"] = subject.strip()
    msg.set_content(body)

    if attachment_path.strip():
        att_file = Path(attachment_path).expanduser().resolve()
        if not att_file.is_file():
            return f"[Email Error] Attachment file not found: {att_file}"
        mime_type, _ = mimetypes.guess_type(str(att_file))
        maintype, subtype = (mime_type or "application/octet-stream").split("/", 1)
        msg.add_attachment(
            att_file.read_bytes(),
            maintype=maintype,
            subtype=subtype,
            filename=att_file.name,
        )

    try:
        with smtplib.SMTP_SSL("smtp.gmail.com", 465, timeout=20) as smtp:
            smtp.login(sender, password)
            smtp.send_message(msg)
        log_security_event(
            action="send_email",
            target=f"{recipient} | {subject}",
            status="DISPATCHED",
            confirm="yes",
        )
        return f"Email dispatched to {recipient} with subject '{subject}', {STATE.user_name}."
    except Exception as exc:
        log_security_event(
            action="send_email",
            target=f"{recipient} | {subject}",
            status="FAILED",
            confirm="yes",
            details=str(exc),
        )
        return f"[Email Error] SMTP dispatch failed: {exc}"


@friday_tool
def create_calendar_event(
    summary: str,
    start_time: str,
    end_time: str = "",
    description: str = "",
    location: str = "",
) -> str:
    """Schedule a calendar event (syncs with Google Calendar API if authorized + exports RFC-5545 .ics)."""
    start_dt = _parse_event_dt(start_time)
    end_dt = _parse_event_dt(end_time) if end_time.strip() else (start_dt + timedelta(hours=1))
    uid = f"{uuid.uuid4()}@friday.stark"

    ics_stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    ics_start = start_dt.astimezone(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    ics_end = end_dt.astimezone(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    slug = re.sub(r"[^a-z0-9]+", "_", summary.lower()).strip("_")[:40] or "event"
    ics_path = get_data_dir() / "calendar" / f"{slug}_{ics_start}.ics"
    ics_path.parent.mkdir(parents=True, exist_ok=True)

    ics_content = (
        "BEGIN:VCALENDAR\r\n"
        "VERSION:2.0\r\n"
        "PRODID:-//Stark Industries//FRIDAY v3.0 MAX//EN\r\n"
        "BEGIN:VEVENT\r\n"
        f"UID:{uid}\r\n"
        f"DTSTAMP:{ics_stamp}\r\n"
        f"DTSTART:{ics_start}\r\n"
        f"DTEND:{ics_end}\r\n"
        f"SUMMARY:{summary.strip()}\r\n"
        f"DESCRIPTION:{description.strip()}\r\n"
        f"LOCATION:{location.strip()}\r\n"
        "END:VEVENT\r\n"
        "END:VCALENDAR\r\n"
    )
    ics_path.write_text(ics_content, encoding="utf-8")

    record = {
        "id": uid,
        "summary": summary.strip(),
        "start_time": start_dt.isoformat(),
        "end_time": end_dt.isoformat(),
        "description": description.strip(),
        "location": location.strip(),
        "ics_path": str(ics_path),
    }
    events = _load_local_events()
    events.append(record)
    _save_local_events(events)

    gcal_note = "Google Calendar OAuth token not present (local schedule + .ics generated)."
    service = _try_google_calendar_service()
    if service is not None:
        try:
            created = (
                service.events()
                .insert(
                    calendarId="primary",
                    body={
                        "summary": record["summary"],
                        "description": record["description"],
                        "location": record["location"],
                        "start": {"dateTime": record["start_time"]},
                        "end": {"dateTime": record["end_time"]},
                    },
                )
                .execute()
            )
            gcal_note = f"Synced to Google Calendar ({created.get('htmlLink', 'primary')})."
        except Exception as exc:
            gcal_note = f"Google Calendar sync skipped ({exc})."

    return (
        f"Calendar event '{record['summary']}' scheduled ({record['start_time']} -> {record['end_time']}), "
        f"{STATE.user_name}. ICS saved to {ics_path}. {gcal_note}"
    )


@friday_tool
def list_calendar_events(max_results: int = 10) -> str:
    """List upcoming scheduled calendar events from Google Calendar (if linked) and FRIDAY's local calendar."""
    limit = max(1, min(int(max_results), 50))
    lines: list[str] = []

    service = _try_google_calendar_service()
    if service is not None:
        try:
            now_iso = datetime.now(timezone.utc).isoformat()
            resp = (
                service.events()
                .list(calendarId="primary", timeMin=now_iso, maxResults=limit, singleEvents=True, orderBy="startTime")
                .execute()
            )
            for item in resp.get("items", []):
                start = item.get("start", {}).get("dateTime") or item.get("start", {}).get("date", "")
                lines.append(f"- [Google Calendar] {start} | {item.get('summary', 'Untitled')}")
        except Exception:
            pass

    local_events = _load_local_events()
    for ev in local_events[-limit:]:
        loc = f" @ {ev['location']}" if ev.get("location") else ""
        lines.append(f"- [Local/ICS] {ev.get('start_time', '')[:19]} | {ev.get('summary', 'Untitled')}{loc}")

    if not lines:
        return f"No upcoming calendar events on the docket, {STATE.user_name}."
    return "Scheduled Calendar Events:\n" + "\n".join(lines[:limit])


EMAIL_TOOLS = [send_email, create_calendar_event, list_calendar_events]
