"""Dynamic Drop-In @tool Plugin Discovery and Loader."""
from __future__ import annotations

import importlib.util
from pathlib import Path
from typing import Any

from friday_ultra.config import PLUGINS_DIR, is_tool_object


def load_plugins(plugins_dir: Path | None = None) -> list[Any]:
    """Scan friday_ultra/plugins/*.py and return all discovered @tool extensions."""
    folder = plugins_dir or PLUGINS_DIR
    if not folder.is_dir():
        return []

    discovered: list[Any] = []
    seen_names: set[str] = set()

    for py_file in sorted(folder.glob("*.py")):
        if py_file.name.startswith("_"):
            continue
        mod_name = f"friday_ultra_plugin_{py_file.stem}"
        try:
            spec = importlib.util.spec_from_file_location(mod_name, py_file)
            if spec is None or spec.loader is None:
                continue
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)

            candidates: list[Any] = []
            if hasattr(module, "PLUGIN_TOOLS") and isinstance(module.PLUGIN_TOOLS, (list, tuple)):
                candidates.extend(module.PLUGIN_TOOLS)
            else:
                for attr_name in dir(module):
                    if attr_name.startswith("_"):
                        continue
                    obj = getattr(module, attr_name)
                    if is_tool_object(obj):
                        candidates.append(obj)

            for tool_obj in candidates:
                tname = getattr(tool_obj, "name", None)
                if tname and tname not in seen_names and is_tool_object(tool_obj):
                    seen_names.add(tname)
                    discovered.append(tool_obj)
        except Exception:
            continue

    return discovered
