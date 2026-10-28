#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"

source "$ROOT/tools/lib/elf_inspection.sh"

if [[ -z "${READELF:-}" ]]; then
  READELF="$(resolve_elf_tool)" || {
    echo "Unable to locate a usable ELF inspection tool. Set ANDROID_NDK_HOME, ANDROID_HOME, or ANDROID_SDK_ROOT to a valid Android SDK/NDK install." >&2
    exit 2
  }
fi

printf 'ELF tool: %s\n' "$READELF"
printf 'ELF tool version: %s\n' "$($READELF --version 2>/dev/null | head -n 1 || printf 'unknown')"

for arch in arm64 x86_64; do
  llama_lib="$TP/llama.cpp/install-android-$arch/lib"
  whisper_lib="$TP/whisper.cpp/install-android-$arch/lib"

  if [[ ! -d "$llama_lib" ]]; then
    echo "Missing llama.cpp install directory for $arch: $llama_lib" >&2
    exit 1
  fi
  if [[ ! -d "$whisper_lib" ]]; then
    echo "Missing whisper.cpp install directory for $arch: $whisper_lib" >&2
    exit 1
  fi

  for lib in libllama.so libggml.so libggml-base.so; do
    test -f "$llama_lib/$lib" || { echo "Missing $arch library: $llama_lib/$lib" >&2; exit 1; }
  done
  test -f "$whisper_lib/libwhisper.so" || { echo "Missing $arch library: $whisper_lib/libwhisper.so" >&2; exit 1; }

  shopt -s nullglob
  libs=("$llama_lib"/*.so "$whisper_lib"/*.so)
  shopt -u nullglob

  if (( ${#libs[@]} == 0 )); then
    echo "No shared libraries found under $llama_lib or $whisper_lib for $arch" >&2
    exit 1
  fi

  for lib in "${libs[@]}"; do
    validate_elf_file "$lib" "$READELF" >/dev/null 2>&1 || {
      echo "ELF validation failed for $lib" >&2
      validate_elf_file "$lib" "$READELF" >&2 || true
      exit 1
    }

    needed="$(get_elf_needed_libraries "$lib" "$READELF")"
    while IFS= read -r dependency; do
      [[ -n "$dependency" ]] || continue
      case "$dependency" in
        libggml*.so*)
          if [[ ! -e "$llama_lib/$dependency" ]]; then
            echo "$arch: $lib needs $dependency, missing from shared GGML install $llama_lib" >&2
            exit 1
          fi
          ;;
      esac
    done <<< "$needed"
  done

  echo "Native dependency and 16 KiB alignment checks passed for $arch"
done
