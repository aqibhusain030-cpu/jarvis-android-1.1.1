#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"
source "$ROOT/tools/lib/elf_inspection.sh"

READELF="$(resolve_elf_tool)" || {
  echo "Unable to locate a usable llvm-readelf/readelf tool." >&2
  exit 2
}
printf 'ELF tool: %s\n' "$READELF"
printf 'ELF tool version: %s\n' "$("$READELF" --version 2>/dev/null | head -n 1 || printf 'unknown')"

for arch in arm64 x86_64; do
  llama_lib="$TP/llama.cpp/install-android-$arch/lib"
  whisper_lib="$TP/whisper.cpp/install-android-$arch/lib"
  runtime_lib="$(find_ndk_runtime_dir "$arch")" || {
    echo "Unable to locate NDK libc++ runtime directory for $arch." >&2
    exit 1
  }

  [[ -d "$llama_lib" ]] || { echo "Missing llama.cpp install directory: $llama_lib" >&2; exit 1; }
  [[ -d "$whisper_lib" ]] || { echo "Missing whisper.cpp install directory: $whisper_lib" >&2; exit 1; }

  for required in libllama.so libggml.so libggml-base.so; do
    [[ -f "$llama_lib/$required" ]] || {
      echo "Missing required $arch library: $llama_lib/$required" >&2
      exit 1
    }
  done
  [[ -f "$whisper_lib/libwhisper.so" ]] || {
    echo "Missing required $arch library: $whisper_lib/libwhisper.so" >&2
    exit 1
  }
  [[ -f "$runtime_lib/libc++_shared.so" ]] || {
    echo "Missing NDK C++ runtime for $arch: $runtime_lib/libc++_shared.so" >&2
    exit 1
  }

  shopt -s nullglob
  libs=("$llama_lib"/*.so "$whisper_lib"/*.so)
  shopt -u nullglob
  (( ${#libs[@]} > 0 )) || {
    echo "No shared libraries found for $arch." >&2
    exit 1
  }

  for lib in "${libs[@]}"; do
    validate_elf_file "$lib" "$READELF" "$arch" || {
      echo "ELF validation failed for $lib" >&2
      exit 1
    }
    validate_dependency_closure "$lib" "$READELF" "$arch" \
      "$llama_lib" "$whisper_lib" "$runtime_lib" || exit 1
  done

  # llama.cpp and whisper.cpp are deliberately built against one GGML install.
  # Conflicting SONAME providers would make runtime resolution order-dependent.
  validate_no_soname_conflicts "$READELF" "${libs[@]}" || exit 1
  echo "Native ELF architecture, DT_NEEDED closure, SONAME uniqueness, and 16 KiB LOAD alignment passed for $arch"
done
