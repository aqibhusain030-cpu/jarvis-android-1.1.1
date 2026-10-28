#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${1:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
NDK="${ANDROID_NDK_HOME:-}"
[[ -f "$APK" ]] || { echo "APK not found: $APK" >&2; exit 2; }
[[ -n "$NDK" ]] || { echo "ANDROID_NDK_HOME is required" >&2; exit 2; }
READELF="$(find "$NDK/toolchains/llvm/prebuilt" -type f -name llvm-readelf -print -quit)"
[[ -n "$READELF" ]] || { echo "llvm-readelf not found under $NDK" >&2; exit 2; }
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
unzip -q "$APK" -d "$TMP"

for abi in arm64-v8a x86_64; do
  dir="$TMP/lib/$abi"
  [[ -d "$dir" ]] || { echo "APK is missing ABI directory lib/$abi" >&2; exit 1; }
  for name in libjarvis_native.so libllama.so libwhisper.so libggml.so libggml-base.so; do
    [[ -f "$dir/$name" ]] || { echo "APK is missing lib/$abi/$name" >&2; exit 1; }
  done
  # A single GGML SONAME per ABI avoids packaging collisions and ambiguous resolution.
  count="$(find "$dir" -maxdepth 1 -type f -name 'libggml.so' | wc -l | tr -d ' ')"
  [[ "$count" == "1" ]] || { echo "Expected exactly one libggml.so in lib/$abi; found $count" >&2; exit 1; }

  for name in libjarvis_native.so libllama.so libwhisper.so "$dir"/libggml*.so; do
    [[ -f "$dir/$name" ]] && lib="$dir/$name" || lib="$name"
    [[ -f "$lib" ]] || continue
    needed="$("$READELF" -d "$lib")"
    while IFS= read -r dependency; do
      [[ -n "$dependency" ]] || continue
      case "$dependency" in
        libggml*.so*) [[ -f "$dir/$dependency" ]] || {
          echo "$abi: $(basename "$lib") needs $dependency, but it is not packaged" >&2
          exit 1
        } ;;
      esac
    done < <(printf '%s\n' "$needed" | sed -n 's/.*Shared library: \[\([^]]*\)\].*/\1/p')

    if ! "$READELF" -l "$lib" | awk '/LOAD/ { if ($NF == "0x4000") found=1 } END { exit(found ? 0 : 1) }'; then
      echo "$abi: $(basename "$lib") lacks 16 KiB LOAD alignment" >&2
      "$READELF" -l "$lib" | grep LOAD || true
      exit 1
    fi
  done
  echo "APK native linkage checks passed for $abi"
done
