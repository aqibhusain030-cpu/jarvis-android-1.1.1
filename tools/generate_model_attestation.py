#!/usr/bin/env python3
"""Generate deterministic model/artifact provenance for a JARVIS release.

The output is intentionally runtime-neutral. It records the source commit,
model/profile metadata supplied by the caller, and SHA-256 digests of local
artifact files. This mirrors the useful part of large CI artifact systems:
artifact identity is explicit and independently checkable.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--source-revision", required=True)
    p.add_argument("--artifact", action="append", default=[])
    p.add_argument("--metadata", type=Path)
    args = p.parse_args()

    artifacts = []
    for raw in args.artifact:
        path = Path(raw)
        if not path.is_file():
            raise SystemExit(f"artifact not found: {path}")
        artifacts.append({
            "path": str(path).replace("\\", "/"),
            "size_bytes": path.stat().st_size,
            "sha256": digest(path),
        })

    metadata = {}
    if args.metadata:
        metadata = json.loads(args.metadata.read_text(encoding="utf-8"))

    result = {
        "schema": 1,
        "source_revision": args.source_revision,
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "artifacts": sorted(artifacts, key=lambda x: x["path"]),
        "metadata": metadata,
    }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(result, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(result, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
