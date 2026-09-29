#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
: "${JARVIS_KEYSTORE:?}" "${JARVIS_KEY_ALIAS:?}" "${JARVIS_STORE_PASS:?}" "${JARVIS_KEY_PASS:?}"
./gradlew :app:assembleRelease \
  -Pandroid.injected.signing.store.file="$JARVIS_KEYSTORE" \
  -Pandroid.injected.signing.store.password="$JARVIS_STORE_PASS" \
  -Pandroid.injected.signing.key.alias="$JARVIS_KEY_ALIAS" \
  -Pandroid.injected.signing.key.password="$JARVIS_KEY_PASS"
echo "✓ Signed APK in app/build/outputs/apk/release/"
