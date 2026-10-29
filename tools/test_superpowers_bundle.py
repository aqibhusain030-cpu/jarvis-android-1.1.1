#!/usr/bin/env python3
"""Network-free integrity checks for the pinned developer-only Superpowers subset."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "third_party" / "superpowers-lock.json"


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def main() -> None:
    assert MANIFEST.is_file(), f"missing pinned Superpowers manifest: {MANIFEST}"
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    assert manifest["repository"] == "obra/superpowers"
    assert manifest["version"] == "v7.0.1"
    assert manifest["commit"] == "2ce8aa15d05bd57213e6f746f953aafb7ded7a8b"
    assert manifest["license_file"] == "third_party/licenses/SUPERPOWERS-MIT.txt"
    assert manifest["runtime_assets"] is False

    entries = manifest["files"]
    assert entries, "manifest must pin at least one upstream skill file"
    for entry in entries:
        local = Path(entry["local_path"])
        assert local.parts[:2] == (".agents", "skills"), f"skill must remain developer-only: {local}"
        assert "app/src/main/assets" not in local.as_posix(), f"skill must not ship in APK: {local}"
        path = ROOT / local
        assert path.is_file(), f"missing pinned file: {local}"
        actual = git_blob_sha1(path.read_bytes())
        assert actual == entry["upstream_blob_sha"], (
            f"upstream content drift for {local}: expected {entry['upstream_blob_sha']}, got {actual}"
        )
        assert entry["source_url"] == (
            f"https://github.com/{manifest['repository']}/blob/{manifest['commit']}/{entry['source_path']}"
        ), f"unpinned source URL for {local}"

    license_text = (ROOT / manifest["license_file"]).read_text(encoding="utf-8")
    assert "MIT License" in license_text
    assert "Copyright (c) 2025 Jesse Vincent" in license_text
    assert 'THE SOFTWARE IS PROVIDED "AS IS"' in license_text

    guidance = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
    for required in (
        "test-driven-development",
        "systematic-debugging",
        "verification-before-completion",
        "writing-plans",
        "ToolDispatcher",
        "SecurityPolicy",
        "API keys",
    ):
        assert required in guidance, f"AGENTS.md is missing required boundary/workflow text: {required}"

    workflow = (ROOT / ".github" / "workflows" / "ci.yml").read_text(encoding="utf-8")
    assert "python tools/test_superpowers_bundle.py" in workflow
    print(f"Superpowers bundle: PASS ({len(entries)} pinned files, developer-only)")


if __name__ == "__main__":
    main()
