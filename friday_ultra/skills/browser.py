"""Browser Automation (Playwright), Live Web Search (DDGS), and Wikipedia Intelligence Skills."""
from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from typing import Any

from friday_ultra.config import friday_tool, get_data_dir

try:
    from playwright.sync_api import sync_playwright  # type: ignore

    PLAYWRIGHT_AVAILABLE = True
except ImportError:
    sync_playwright = None  # type: ignore
    PLAYWRIGHT_AVAILABLE = False

try:
    import requests  # type: ignore

    REQUESTS_AVAILABLE = True
except ImportError:
    requests = None  # type: ignore
    REQUESTS_AVAILABLE = False

try:
    from bs4 import BeautifulSoup  # type: ignore

    BS4_AVAILABLE = True
except ImportError:
    BeautifulSoup = None  # type: ignore
    BS4_AVAILABLE = False

try:
    from duckduckgo_search import DDGS  # type: ignore

    DDGS_AVAILABLE = True
except ImportError:
    DDGS = None  # type: ignore
    DDGS_AVAILABLE = False

try:
    import wikipedia  # type: ignore

    WIKIPEDIA_AVAILABLE = True
except ImportError:
    wikipedia = None  # type: ignore
    WIKIPEDIA_AVAILABLE = False


_USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
)


def _normalize_url(url: str) -> str:
    clean = url.strip()
    if not clean.startswith(("http://", "https://")):
        clean = "https://" + clean
    return clean


def _strip_html_fallback(raw_html: str) -> tuple[str, str]:
    title_match = re.search(r"<title[^>]*>(.*?)</title>", raw_html, flags=re.IGNORECASE | re.DOTALL)
    title = html.unescape(title_match.group(1).strip()) if title_match else "Untitled"
    body = re.sub(r"<(script|style|noscript)[^>]*>[\s\S]*?</\1>", " ", raw_html, flags=re.IGNORECASE)
    body = re.sub(r"<[^>]+>", " ", body)
    body = re.sub(r"\s+", " ", html.unescape(body)).strip()
    return title, body


def _http_fetch_text(url: str, timeout: int = 15) -> tuple[str, str]:
    if REQUESTS_AVAILABLE and requests is not None:
        resp = requests.get(url, headers={"User-Agent": _USER_AGENT}, timeout=timeout)
        resp.raise_for_status()
        raw_html = resp.text
    else:
        req = urllib.request.Request(url, headers={"User-Agent": _USER_AGENT})
        with urllib.request.urlopen(req, timeout=timeout) as resp:  # noqa: S310
            raw_html = resp.read().decode("utf-8", errors="replace")

    if BS4_AVAILABLE and BeautifulSoup is not None:
        soup = BeautifulSoup(raw_html, "html.parser")
        for tag in soup(["script", "style", "noscript", "svg"]):
            tag.decompose()
        title = (soup.title.string.strip() if soup.title and soup.title.string else "Untitled")
        text = re.sub(r"\s+", " ", soup.get_text(separator=" ", strip=True))
        return title, text
    return _strip_html_fallback(raw_html)


@friday_tool
def browse_url(url: str, take_screenshot: bool = False, wait_ms: int = 1500) -> str:
    """Navigate to a live URL using headless Playwright Chromium (with HTTP/BS4 fallback) and extract page text."""
    target = _normalize_url(url)
    screenshot_note = ""

    if PLAYWRIGHT_AVAILABLE and sync_playwright is not None:
        try:
            with sync_playwright() as pw:
                browser = pw.chromium.launch(headless=True)
                context = browser.new_context(user_agent=_USER_AGENT, viewport={"width": 1440, "height": 900})
                page = context.new_page()
                page.goto(target, wait_until="domcontentloaded", timeout=25000)
                if wait_ms > 0:
                    page.wait_for_timeout(min(int(wait_ms), 10000))
                title = page.title() or "Untitled"
                body_text = page.inner_text("body")
                links: list[dict[str, Any]] = page.eval_on_selector_all(
                    "a[href]",
                    "els => els.slice(0, 12).map(a => ({text: (a.innerText || '').trim(), href: a.href}))",
                )
                if take_screenshot:
                    stamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
                    shot_path = get_data_dir() / "screenshots" / f"browse_{stamp}.png"
                    shot_path.parent.mkdir(parents=True, exist_ok=True)
                    page.screenshot(path=str(shot_path), full_page=False)
                    screenshot_note = f"\nScreenshot saved: {shot_path}"
                browser.close()

            compact = re.sub(r"\s+", " ", body_text).strip()[:8000]
            link_lines = [
                f"  - {item.get('text') or 'link'}: {item.get('href')}"
                for item in links
                if item.get("href")
            ]
            links_block = ("\nTop Links:\n" + "\n".join(link_lines[:10])) if link_lines else ""
            return f"URL: {target}\nTitle: {title}{screenshot_note}\n\nContent:\n{compact}{links_block}"
        except Exception as pw_exc:
            screenshot_note = f"\n[Playwright fallback triggered: {pw_exc}]"

    try:
        title, text = _http_fetch_text(target)
        return f"URL: {target}\nTitle: {title}{screenshot_note}\n\nContent:\n{text[:8000]}"
    except Exception as exc:
        return f"[Browser Error] Could not inspect '{target}': {exc}"


@friday_tool
def web_search(query: str, max_results: int = 6) -> str:
    """Search the live web via DuckDuckGo (DDGS) and return ranked titles, URLs, and snippets."""
    q = query.strip()
    if not q:
        return "Please provide a non-empty search query."
    limit = max(1, min(int(max_results), 15))

    if not DDGS_AVAILABLE or DDGS is None:
        return "[Search Error] duckduckgo-search is not installed. Run: pip install duckduckgo-search"

    try:
        with DDGS() as ddgs:
            results = list(ddgs.text(q, max_results=limit))
        if not results:
            return f"No web search results returned for '{q}'."
        lines = [f"Live Web Search Results for '{q}':"]
        for idx, item in enumerate(results, start=1):
            title = item.get("title", "Untitled")
            href = item.get("href") or item.get("url") or ""
            snippet = item.get("body") or item.get("snippet") or ""
            lines.append(f"{idx}. {title}\n   URL: {href}\n   Summary: {snippet}")
        return "\n".join(lines)
    except Exception as exc:
        return f"[Search Error] DuckDuckGo search failed for '{q}': {exc}"


@friday_tool
def wikipedia_summary(topic: str, sentences: int = 5) -> str:
    """Fetch an authoritative encyclopedia summary from Wikipedia."""
    clean_topic = topic.strip()
    if not clean_topic:
        return "Please provide a Wikipedia topic."
    num_sentences = max(1, min(int(sentences), 15))

    if WIKIPEDIA_AVAILABLE and wikipedia is not None:
        try:
            page = wikipedia.page(clean_topic, auto_suggest=False)
            summary = wikipedia.summary(clean_topic, sentences=num_sentences, auto_suggest=False)
            return f"Wikipedia — {page.title} ({page.url}):\n{summary}"
        except wikipedia.DisambiguationError as disamb:  # type: ignore[attr-defined]
            options = disamb.options[:6]
            if options:
                try:
                    summary = wikipedia.summary(options[0], sentences=num_sentences, auto_suggest=False)
                    return (
                        f"Wikipedia (resolved '{clean_topic}' -> '{options[0]}'; "
                        f"other options: {', '.join(options[1:])}):\n{summary}"
                    )
                except Exception:
                    return f"Wikipedia disambiguation for '{clean_topic}': {', '.join(options)}"
        except Exception:
            try:
                summary = wikipedia.summary(clean_topic, sentences=num_sentences)
                return f"Wikipedia — {clean_topic}:\n{summary}"
            except Exception:
                pass

    encoded = urllib.parse.quote(clean_topic.replace(" ", "_"))
    api_url = f"https://en.wikipedia.org/api/rest_v1/page/summary/{encoded}"
    try:
        if REQUESTS_AVAILABLE and requests is not None:
            resp = requests.get(api_url, headers={"User-Agent": _USER_AGENT}, timeout=10)
            resp.raise_for_status()
            data = resp.json()
        else:
            import json

            req = urllib.request.Request(api_url, headers={"User-Agent": _USER_AGENT})
            with urllib.request.urlopen(req, timeout=10) as resp:  # noqa: S310
                data = json.loads(resp.read().decode("utf-8"))
        extract = data.get("extract", "No summary available.")
        page_url = data.get("content_urls", {}).get("desktop", {}).get("page", api_url)
        return f"Wikipedia — {data.get('title', clean_topic)} ({page_url}):\n{extract}"
    except Exception as exc:
        return f"[Wikipedia Error] Could not retrieve summary for '{clean_topic}': {exc}"


BROWSER_TOOLS = [browse_url, web_search, wikipedia_summary]
