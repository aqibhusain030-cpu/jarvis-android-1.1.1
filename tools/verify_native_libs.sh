#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"

resolve_elf_tool() {
  local ndk_root="${ANDROID_NDK_HOME:-${ANDROID_HOME:-}}"
  local candidates=()

  if [[ -n "$ndk_root" ]]; then
    while IFS= read -r path; do
      candidates+=("$path")
    done < <(find "$ndk_root" \( -type f -o -type l \) \( -name llvm-readelf -o -name readelf \) 2>/dev/null | sort)
  fi

  if command -v llvm-readelf >/dev/null 2>&1; then
    candidates+=("$(command -v llvm-readelf)")
  fi
  if command -v readelf >/dev/null 2>&1; then
    candidates+=("$(command -v readelf)")
  fi

  local tool=""
  for candidate in "${candidates[@]}"; do
    [[ -n "$candidate" ]] || continue
    if [[ -x "$candidate" ]]; then
      tool="$candidate"
      break
    fi
  done

  if [[ -z "$tool" ]]; then
    echo "Neither llvm-readelf nor readelf is available. Set ANDROID_NDK_HOME to a valid Android NDK installation." >&2
    return 1
  fi

  printf '%s\n' "$tool"
}

READELF="$(resolve_elf_tool)"
if [[ -z "$READELF" ]]; then
  echo "Unable to locate an ELF inspection tool." >&2
  exit 2
fi

for arch in arm64 x86_64; do
  llama_lib="$TP/llama.cpp/install-android-$arch/lib"
  whisper_lib="$TP/whisper.cpp/install-android-$arch/lib"
  for lib in libllama.so libggml.so libggml-base.so; do
    test -f "$llama_lib/$lib" || { echo "Missing $arch library: $llama_lib/$lib" >&2; exit 1; }
  done
  test -f "$whisper_lib/libwhisper.so" || { echo "Missing $arch library: $whisper_lib/libwhisper.so" >&2; exit 1; }

  for lib in "$llama_lib"/*.so "$whisper_lib"/*.so; do
    [[ -f "$lib" ]] || continue
    needed="$($READELF -d "$lib")"
    while IFS= read -r dependency; do
      [[ -n "$dependency" ]] || continue
      case "$dependency" in
        libggml*.so*) test -e "$llama_lib/$dependency" || {
          echo "$arch: $lib needs $dependency, missing from shared GGML install $llama_lib" >&2
          exit 1
        } ;;
      esac
    done < <(printf '%s\n' "$needed" | sed -n 's/.*Shared library: \[\([^]]*\)\].*/\1/p')

    # Every PT_LOAD segment must meet 16 KiB alignment. Checking only whether
    # one segment contains 0x4000 would allow another segment aligned to 4 KiB
    # to slip through and produce a false-positive verification result.
    if ! load_table="$("$READELF" -lW "$lib")"; then
      echo "$arch: ELF program-header inspection failed for $lib using $READELF" >&2
      exit 1
    fi
    load_alignments="$(printf '%s\n' "$load_table" | awk '$1 == "LOAD" { print $NF }')"
    if [[ -z "$load_alignments" ]]; then
      echo "$arch: no LOAD segments found in $lib" >&2
      exit 1
    fi
    while IFS= read -r alignment; do
      [[ -n "$alignment" ]] || continue
      if [[ ! "$alignment" =~ ^0[xX][0-9a-fA-F]+$ ]]; then
        echo "$arch: unrecognized LOAD alignment '$alignment' in $lib" >&2
        printf '%s\n' "$load_table" | grep LOAD >&2 || true
        exit 1
      fi
      alignment_value=$((alignment))
      if (( alignment_value < 16384 || (alignment_value & (alignment_value - 1)) != 0 )); then
        echo "$arch: invalid 16 KiB LOAD alignment '$alignment' in $lib" >&2
        printf '%s\n' "$load_table" | grep LOAD >&2 || true
        exit 1
      fi
    done <<< "$load_alignments"
  done
  echo "Native dependency and 16 KiB alignment checks passed for $arch"
done
