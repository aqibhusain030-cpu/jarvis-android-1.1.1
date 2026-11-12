"""Unified export of all FRIDAY Ultra skill modules and tools."""
from __future__ import annotations

from friday_ultra.skills.automation import (
    AUTOMATION_TOOLS,
    ReminderDaemon,
    add_reminder,
    calculator,
    get_current_time,
    get_weather,
    list_reminders,
    start_reminder_checker,
    stock_price,
    translate,
)
from friday_ultra.skills.browser import BROWSER_TOOLS, browse_url, web_search, wikipedia_summary
from friday_ultra.skills.coding import CODING_TOOLS, run_python, run_shell
from friday_ultra.skills.email import (
    EMAIL_TOOLS,
    create_calendar_event,
    list_calendar_events,
    send_email,
)
from friday_ultra.skills.files import (
    FILE_TOOLS,
    delete_file,
    list_dir,
    markdown_report,
    read_file,
    read_pdf,
    write_file,
)
from friday_ultra.skills.security import SECURITY_TOOLS, get_security_audit_log, verify_confirmation
from friday_ultra.skills.smart_home import (
    SMART_HOME_TOOLS,
    network_diagnostics,
    open_jio_admin,
    scan_local_ports,
)
from friday_ultra.skills.system import (
    SYSTEM_TOOLS,
    describe_image,
    get_system_info,
    kill_process,
    list_processes,
    take_screenshot,
)

CORE_SKILL_TOOLS = (
    FILE_TOOLS
    + BROWSER_TOOLS
    + CODING_TOOLS
    + EMAIL_TOOLS
    + SMART_HOME_TOOLS
    + SYSTEM_TOOLS
    + SECURITY_TOOLS
    + AUTOMATION_TOOLS
)

__all__ = [
    "AUTOMATION_TOOLS",
    "BROWSER_TOOLS",
    "CODING_TOOLS",
    "CORE_SKILL_TOOLS",
    "EMAIL_TOOLS",
    "FILE_TOOLS",
    "ReminderDaemon",
    "SECURITY_TOOLS",
    "SMART_HOME_TOOLS",
    "SYSTEM_TOOLS",
    "add_reminder",
    "browse_url",
    "calculator",
    "create_calendar_event",
    "delete_file",
    "describe_image",
    "get_current_time",
    "get_security_audit_log",
    "get_system_info",
    "get_weather",
    "kill_process",
    "list_calendar_events",
    "list_dir",
    "list_processes",
    "list_reminders",
    "markdown_report",
    "network_diagnostics",
    "open_jio_admin",
    "read_file",
    "read_pdf",
    "run_python",
    "run_shell",
    "scan_local_ports",
    "send_email",
    "start_reminder_checker",
    "stock_price",
    "take_screenshot",
    "translate",
    "verify_confirmation",
    "web_search",
    "wikipedia_summary",
    "write_file",
]
