#!/usr/bin/env bash
set -euo pipefail
bash -n scripts/fetch_models.sh
bash -n scripts/fetch_wakeword.sh
grep -q '/resolve/626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d/' scripts/fetch_models.sh
grep -q '/resolve/1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b/' scripts/fetch_models.sh
grep -q 'sha256sum -c' scripts/fetch_models.sh
grep -q 'JARVIS_WAKEWORD_SHA256' scripts/fetch_wakeword.sh
grep -q 'sha256sum -c' scripts/fetch_wakeword.sh
echo 'model fetch security gates passed'
