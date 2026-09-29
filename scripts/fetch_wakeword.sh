#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/models/wakeword"
BASE="https://github.com/dscripka/openWakeWord/releases/download/v0.5.1"

declare -A SHA=(
  [melspectrogram.onnx]="${JARVIS_WAKEWORD_MELSPEC_SHA256:-}"
  [embedding_model.onnx]="${JARVIS_WAKEWORD_EMBEDDING_SHA256:-}"
  [hey_jarvis_v0.1.onnx]="${JARVIS_WAKEWORD_SHA256:-}"
)

mkdir -p "$DEST"

for name in melspectrogram.onnx embedding_model.onnx hey_jarvis_v0.1.onnx; do
  digest="${SHA[$name]}"
  [[ "$digest" =~ ^[0-9A-Fa-f]{64}$ ]] || {
    echo "Set the corresponding independently verified SHA-256 for $name." >&2
    exit 2
  }

  url="$BASE/$name"
  tmp="$DEST/$name.part"
  curl --fail --location --retry 3 --retry-delay 5     --proto '=https' --tlsv1.2 -o "$tmp" "$url"
  echo "$digest  $tmp" | sha256sum -c -
  mv -f "$tmp" "$DEST/$name"
  printf '%s  %s\n' "$digest" "$name" > "$DEST/$name.sha256"
  echo "verified: $name"
done

echo "All openWakeWord assets verified into $DEST"
