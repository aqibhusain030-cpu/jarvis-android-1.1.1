#!/usr/bin/env bash
set -euo pipefail
bash -n scripts/build_llama.sh
bash -n scripts/build_whisper.sh
grep -q '526c43b8f7dfea9032e9f35e7a1be9183ca7cc20' scripts/build_llama.sh
grep -q '927cfce34f31707e17f2bff35c349632fb9e2c3a' scripts/build_whisper.sh
grep -q '526c43b8f7dfea9032e9f35e7a1be9183ca7cc20' third_party/LOCKFILE
grep -q '927cfce34f31707e17f2bff35c349632fb9e2c3a' third_party/LOCKFILE
echo 'native lock gates passed'
