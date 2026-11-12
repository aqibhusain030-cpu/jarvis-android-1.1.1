"""File Management, PDF Extraction, and Markdown Document Skills."""
from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path

from friday_ultra.config import STATE, friday_tool
from friday_ultra.skills.security import log_security_event, verify_confirmation

try:
    from pypdf import PdfReader  # type: ignore

    PYPDF_AVAILABLE = True
except ImportError:
    PdfReader = None  # type: ignore
    PYPDF_AVAILABLE = False


def _human_size(num_bytes: int) -> str:
    value = float(num_bytes)
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if value < 1024.0 or unit == "TB":
            return f"{int(value)} B" if unit == "B" else f"{value:.1f} {unit}"
        value /= 1024.0
    return f"{num_bytes} B"


def _extract_pdf_text(path: Path, max_pages: int = 30) -> str:
    if not PYPDF_AVAILABLE or PdfReader is None:
        return "[PDF Error] pypdf is not installed. Run: pip install pypdf"
    try:
        reader = PdfReader(str(path))
        total = len(reader.pages)
        limit = max(1, min(int(max_pages), total))
        chunks = [f"PDF: {path.name} ({total} pages total, reading first {limit})"]
        for idx in range(limit):
            page_text = (reader.pages[idx].extract_text() or "").strip()
            chunks.append(f"\n--- Page {idx + 1} ---\n{page_text or '[No extractable text]'}")
        return "\n".join(chunks)
    except Exception as exc:
        return f"[PDF Error] Could not parse '{path}': {exc}"


@friday_tool
def read_file(file_path: str, max_chars: int = 20000) -> str:
    """Read a local text, code, Markdown, JSON, or PDF file cleanly."""
    path = Path(file_path).expanduser().resolve()
    if not path.exists():
        return f"[File Error] Path does not exist: {path}"
    if path.is_dir():
        return f"[File Error] '{path}' is a directory. Use list_dir instead."

    if path.suffix.lower() == ".pdf":
        text = _extract_pdf_text(path)
        return text[: max(500, int(max_chars))]

    try:
        content = path.read_text(encoding="utf-8", errors="replace")
        limit = max(500, int(max_chars))
        if len(content) > limit:
            return content[:limit] + f"\n\n... [Truncated at {limit} chars out of {len(content)} total]"
        return content
    except Exception as exc:
        return f"[File Error] Failed to read '{path}': {exc}"


@friday_tool
def write_file(file_path: str, content: str, mode: str = "w") -> str:
    """Write or append text content to a local file, creating parent directories if required.

    Args:
        file_path: Destination file path.
        content: Full text or code content to write.
        mode: 'w' to overwrite/create (default) or 'a' to append.
    """
    write_mode = "a" if mode.strip().lower() == "a" else "w"
    path = Path(file_path).expanduser().resolve()
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open(write_mode, encoding="utf-8") as fh:
            fh.write(content)
        log_security_event(
            action="write_file",
            target=str(path),
            status="COMPLETED",
            confirm="yes",
            details=f"mode={write_mode}, chars={len(content)}",
        )
        verb = "Appended" if write_mode == "a" else "Wrote"
        return f"{verb} {len(content)} characters to '{path}', {STATE.user_name}."
    except Exception as exc:
        return f"[File Error] Could not write '{path}': {exc}"


@friday_tool
def delete_file(file_path: str, confirm: str = "no") -> str:
    """Delete a local file permanently. Strictly requires confirm='yes'."""
    path = Path(file_path).expanduser().resolve()
    if not path.exists():
        return f"[File Error] File not found: {path}"
    if path.is_dir():
        return f"[File Error] Refusing to remove directory '{path}' via delete_file."

    allowed, msg = verify_confirmation(action="delete_file", target=str(path), confirm=confirm)
    if not allowed:
        return msg

    try:
        path.unlink()
        return f"Deleted '{path}', {STATE.user_name}."
    except Exception as exc:
        return f"[File Error] Could not delete '{path}': {exc}"


@friday_tool
def list_dir(directory: str = ".", recursive: bool = False) -> str:
    """List files and subdirectories with sizes and last-modified timestamps."""
    base = Path(directory).expanduser().resolve()
    if not base.exists():
        return f"[File Error] Directory not found: {base}"
    if not base.is_dir():
        return f"[File Error] Not a directory: {base}"

    try:
        entries = sorted(base.rglob("*") if recursive else base.iterdir(), key=lambda p: (not p.is_dir(), str(p).lower()))
        lines = [f"Directory listing for {base} ({min(len(entries), 200)} of {len(entries)} entries):"]
        for item in entries[:200]:
            rel = item.relative_to(base)
            try:
                stat = item.stat()
                mtime = datetime.fromtimestamp(stat.st_mtime).strftime("%Y-%m-%d %H:%M")
                kind = "DIR " if item.is_dir() else f"FILE ({_human_size(stat.st_size)})"
            except OSError:
                mtime = "unknown"
                kind = "ENTRY"
            lines.append(f"- {kind:<16} | {mtime} | {rel}")
        return "\n".join(lines)
    except Exception as exc:
        return f"[File Error] Failed to list '{base}': {exc}"


@friday_tool
def read_pdf(file_path: str, max_pages: int = 30) -> str:
    """Extract text page-by-page from a PDF document using pypdf."""
    path = Path(file_path).expanduser().resolve()
    if not path.is_file():
        return f"[PDF Error] PDF file not found: {path}"
    return _extract_pdf_text(path, max_pages=max_pages)


@friday_tool
def markdown_report(file_path: str, title: str, sections: str) -> str:
    """Generate and save a formatted Markdown technical report or briefing document."""
    path = Path(file_path).expanduser().resolve()
    if path.suffix.lower() not in {".md", ".markdown"}:
        path = path.with_suffix(".md")
    stamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    doc = (
        f"# {title.strip()}\n\n"
        f"> **Prepared by:** FRIDAY v3.0 MAX  \n"
        f"> **Authorized Recipient:** {STATE.user_name}  \n"
        f"> **Generated:** {stamp}\n\n"
        f"---\n\n"
        f"{sections.strip()}\n"
    )
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(doc, encoding="utf-8")
        return f"Markdown report '{title}' saved to '{path}' ({len(doc)} chars), {STATE.user_name}."
    except Exception as exc:
        return f"[File Error] Failed to save Markdown report: {exc}"


FILE_TOOLS = [read_file, write_file, delete_file, list_dir, read_pdf, markdown_report]
