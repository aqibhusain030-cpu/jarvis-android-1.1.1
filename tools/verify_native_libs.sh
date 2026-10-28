#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"
NDK="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK" ]]; then
  echo "ANDROID_NDK_HOME must point to the installed Android NDK" >&2
  exit 2
fi
READELF="$(find "$NDK/toolchains/llvm/prebuilt" -type f -name llvm-readelf -print -quit)"
if [[ -z "$READELF" ]]; then
  echo "llvm-readelf not found under $NDK" >&2
  exit 2
fi

for arch in arm64 x86_64; do
  llama_lib="$TP/llama.cpp/install-android-$arch/lib"
  whisper_lib="$TP/whisper.cpp/install-android-$arch/lib"
  for lib in libllama.so libggml.so libggml-base.so; do
    test -f "$llama_lib/$lib" || { echo "Missing $arch library: $llama_lib/$lib" >&2; exit 1; }
  done
  test -f "$whisper_lib/libwhisper.so" || { echo "Missing $arch library: $whisper_lib/libwhisper.so" >&2; exit 1; }

  # All GGML DT_NEEDED entries from either engine must resolve to the single
  # llama.cpp GGML install tree, not a second Whisper-vendored GGML build.
  for lib in "$llama_lib"/*.so "$whisper_lib"/*.so; do
    [[ -f "$lib" ]] || continue
    needed="$("$READELF" -d "$lib")"
    while IFS= read -r dependency; do
      [[ -n "$dependency" ]] || continue
      case "$dependency" in
        libggml*.so*) test -e "$llama_lib/$dependency" || {
          echo "$arch: $lib needs $dependency, missing from shared GGML install $llama_lib" >&2
          exit 1
        } ;;
      esac
    done < <(printf '%s\n' "$needed" | sed -n 's/.*Shared library: \[\([^]]*\)\].*/\1/p')

    # NDK r26 needs an explicit max-page-size linker flag for 16 KiB ELF LOADs.
    if ! "$READELF" -l "$lib" | awk '/LOAD/ { count++; if ($NF != "0x4000") bad=1 } END { exit(count > 0 && !bad ? 0 : 1) }'; then
      echo "$arch: $lib does not show 16 KiB LOAD alignment; inspect llvm-readelf -l output" >&2
      "$READELF" -l "$lib" | grep LOAD || true
      exit 1
    fi
  done
  echo "Native dependency and 16 KiB alignment checks passed for $arch"
done
