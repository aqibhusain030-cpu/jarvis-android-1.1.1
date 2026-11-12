"""Smart Home, Router Gateway Portal (JioFiber / LAN), and Network Telemetry Skills."""
from __future__ import annotations

import ipaddress
import socket
import time
import urllib.request
import webbrowser

from friday_ultra.config import STATE, friday_tool

try:
    import psutil  # type: ignore

    PSUTIL_AVAILABLE = True
except ImportError:
    psutil = None  # type: ignore
    PSUTIL_AVAILABLE = False

try:
    from playwright.sync_api import sync_playwright  # type: ignore

    PLAYWRIGHT_AVAILABLE = True
except ImportError:
    sync_playwright = None  # type: ignore
    PLAYWRIGHT_AVAILABLE = False


def _tcp_probe(host: str, port: int, timeout: float = 1.5) -> tuple[bool, float]:
    start = time.perf_counter()
    try:
        with socket.create_connection((host, port), timeout=timeout):
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            return True, round(elapsed_ms, 2)
    except OSError:
        return False, 0.0


def _local_ipv4() -> str:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.connect(("10.255.255.255", 1))
            return str(sock.getsockname()[0])
    except OSError:
        return "127.0.0.1"


@friday_tool
def open_jio_admin(portal_url: str = "http://192.168.29.1", open_browser: bool = False) -> str:
    """Inspect or open the JioFiber / local router admin gateway portal (default http://192.168.29.1)."""
    url = portal_url.strip() or "http://192.168.29.1"
    if not url.startswith(("http://", "https://")):
        url = "http://" + url

    host = url.split("://", 1)[1].split("/", 1)[0].split(":", 1)[0]
    port = 443 if url.startswith("https://") else 80
    reachable, latency_ms = _tcp_probe(host, port, timeout=2.0)

    if open_browser:
        try:
            webbrowser.open(url)
        except Exception:
            pass

    if not reachable:
        return (
            f"Router gateway `{url}` ({host}:{port}) is currently unreachable from this network interface, "
            f"{STATE.user_name}. Local IP is {_local_ipv4()}."
        )

    if PLAYWRIGHT_AVAILABLE and sync_playwright is not None:
        try:
            with sync_playwright() as pw:
                browser = pw.chromium.launch(headless=True)
                page = browser.new_page()
                page.goto(url, wait_until="domcontentloaded", timeout=8000)
                title = page.title() or "Router Gateway"
                snippet = " ".join(page.inner_text("body").split())[:600]
                browser.close()
            return (
                f"Router Admin Portal `{url}` online ({latency_ms} ms) — Title: '{title}'.\n"
                f"Portal Preview: {snippet}"
            )
        except Exception:
            pass

    try:
        req = urllib.request.Request(url, headers={"User-Agent": "FRIDAY-Network-Inspector/3.0"})
        with urllib.request.urlopen(req, timeout=4) as resp:  # noqa: S310
            status = resp.status
        return f"Router Admin Portal `{url}` responded with HTTP {status} ({latency_ms} ms), {STATE.user_name}."
    except Exception as exc:
        return f"Gateway `{host}:{port}` TCP-reachable ({latency_ms} ms), HTTP read note: {exc}"


@friday_tool
def network_diagnostics(target_host: str = "8.8.8.8") -> str:
    """Collect local IP, active network interface counters, DNS resolution, and TCP latency."""
    hostname = socket.gethostname()
    local_ip = _local_ipv4()
    lines = [
        f"Network Telemetry for {hostname}:",
        f"- Primary Local IPv4: {local_ip}",
    ]

    try:
        resolved = socket.gethostbyname(target_host.strip())
        ok_443, ms_443 = _tcp_probe(resolved, 443, timeout=1.5)
        ok_53, ms_53 = _tcp_probe(resolved, 53, timeout=1.5)
        if ok_443:
            lines.append(f"- Probe {target_host} ({resolved}:443): REACHABLE ({ms_443} ms)")
        elif ok_53:
            lines.append(f"- Probe {target_host} ({resolved}:53): REACHABLE ({ms_53} ms)")
        else:
            lines.append(f"- Probe {target_host} ({resolved}): DNS resolved (TCP 53/443 closed or filtered)")
    except OSError as exc:
        lines.append(f"- Probe {target_host}: Unresolved/Offline ({exc})")

    if PSUTIL_AVAILABLE and psutil is not None:
        try:
            counters = psutil.net_io_counters()
            sent_mb = counters.bytes_sent / (1024 * 1024)
            recv_mb = counters.bytes_recv / (1024 * 1024)
            lines.append(f"- Cumulative Traffic: {sent_mb:.2f} MB sent | {recv_mb:.2f} MB received")
            addrs = psutil.net_if_addrs()
            active_ifaces = [iface for iface in addrs.keys() if iface != "lo"]
            if active_ifaces:
                lines.append(f"- Interfaces: {', '.join(active_ifaces[:8])}")
        except Exception:
            pass

    return "\n".join(lines)


@friday_tool
def scan_local_ports(host: str = "127.0.0.1", ports: str = "22,80,443,7860,8000,8080,11434") -> str:
    """Check listening TCP ports on localhost or a private LAN device (restricted to loopback/RFC1918)."""
    target = host.strip() or "127.0.0.1"
    try:
        resolved = socket.gethostbyname(target)
        ip_obj = ipaddress.ip_address(resolved)
        if not (ip_obj.is_loopback or ip_obj.is_private):
            return f"[Security Policy] Port inspection is restricted to loopback/private LAN hosts (got {resolved})."
    except Exception as exc:
        return f"[Network Error] Could not resolve '{target}': {exc}"

    port_list: list[int] = []
    for token in ports.split(","):
        token = token.strip()
        if token.isdigit():
            p = int(token)
            if 1 <= p <= 65535:
                port_list.append(p)

    if not port_list:
        return "Provide a comma-separated list of valid TCP port numbers."

    open_ports: list[str] = []
    closed_ports: list[int] = []
    for p in port_list[:32]:
        ok, ms = _tcp_probe(resolved, p, timeout=0.6)
        if ok:
            open_ports.append(f"{p} ({ms} ms)")
        else:
            closed_ports.append(p)

    return (
        f"Local Port Scan on {target} ({resolved}):\n"
        f"- Open: {', '.join(open_ports) if open_ports else 'none'}\n"
        f"- Closed: {', '.join(str(p) for p in closed_ports) if closed_ports else 'none'}"
    )


SMART_HOME_TOOLS = [open_jio_admin, network_diagnostics, scan_local_ports]
