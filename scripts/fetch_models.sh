#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/.model-cache"
mkdir -p "$DEST/llm" "$DEST/stt"

fetch_verified() {
  local rel="$1" url="$2" sha256="$3"
  local out="$DEST/$rel"
  local tmp="${out}.part"
  mkdir -p "$(dirname "$out")"
  if [[ -s "$out" ]]; then
    echo "verify $rel"
    echo "$sha256  $out" | sha256sum -c -
    return
  fi
  echo "download $rel"
  curl --fail --location --retry 3 --retry-delay 5 --proto '=https' --tlsv1.2 -o "$tmp" "$url"
  echo "$sha256  $tmp" | sha256sum -c -
  mv -f "$tmp" "$out"
}

fetch_verified   "llm/Qwen2.5-3B-Instruct-Q4_K_M.gguf"   "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d/qwen2.5-3b-instruct-q4_k_m.gguf"   "626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d"

fetch_verified   "stt/ggml-small.bin"   "https://huggingface.co/ggerganov/whisper.cpp/resolve/1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b/ggml-small.bin"   "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b"

echo "verified default JARVIS models"
