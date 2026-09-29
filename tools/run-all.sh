#!/usr/bin/env bash
set -euo pipefail
bash tools/check_wrapper.sh
bash tools/test_fetch_models.sh
bash tools/test_build_native.sh
bash tools/test_license_split.sh
bash tools/test_release_manifest.sh
bash tools/verify_release.sh
python3 tools/test_source_contracts.py
for f in scripts/*.sh tools/*.sh; do bash -n "$f"; done
python3 - <<'PY'
import ast, pathlib
for p in pathlib.Path("app/src/main/python").rglob("*.py"):
    ast.parse(p.read_text(encoding="utf-8"), filename=str(p))
print("Python AST gate passed")
PY
python3 - <<'PY'
import pathlib, xml.etree.ElementTree as ET
for p in pathlib.Path('app/src/main').rglob('*.xml'):
    ET.parse(p)
print('XML parse gate passed')
PY
echo "all static release gates passed"
