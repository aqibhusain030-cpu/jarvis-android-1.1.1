#!/usr/bin/env bash
set -euo pipefail
f=app/src/main/assets/licenses/THIRD_PARTY.txt
grep -q 'openWakeWord source code' "$f"
grep -q 'openWakeWord pretrained models' "$f"
grep -q 'CC BY-NC-SA 4.0' "$f"
echo 'license split gate passed'
