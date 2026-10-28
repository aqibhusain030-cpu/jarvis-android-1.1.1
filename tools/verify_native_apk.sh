#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${1:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
source "$ROOT/tools/lib/elf_inspection.sh"

[[ -f "$APK" ]] || { echo "APK not found: $APK" >&2; exit 2; }

READELF="$(resolve_elf_tool)" || {
  echo "Unable to locate a usable llvm-readelf/readelf tool." >&2
  exit 2
}
printf 'ELF tool: %s\n' "$READELF"
printf 'ELF tool version: %s\n' "$("$READELF" --version 2>/dev/null | head -n 1 || printf 'unknown')"

resolve_zipalign() {
  local -a candidates=()
  local root candidate
  if command -v zipalign >/dev/null 2>&1; then
    candidates+=("$(command -v zipalign)")
  fi
  for root in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
    [[ -n "$root" && -d "$root/build-tools" ]] || continue
    while IFS= read -r candidate; do
      [[ -n "$candidate" ]] && candidates+=("$candidate")
    done < <(find "$root/build-tools" -type f -name zipalign -perm -u+x -print 2>/dev/null | sort -V -r)
  done
  for candidate in "${candidates[@]}"; do
    [[ -x "$candidate" ]] || continue
    if "$candidate" -h 2>&1 | grep -q -- '-P'; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  echo "A zipalign supporting -P 16 is required to verify APK native-library page alignment." >&2
  return 1
}

ZIPALIGN="$(resolve_zipalign)" || exit 2
"$ZIPALIGN" -c -P 16 4 "$APK" || {
  echo "APK ZIP alignment check failed (required 16 KiB native-library alignment): $APK" >&2
  exit 1
}

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
unzip -q "$APK" -d "$TMP"

for abi in arm64-v8a x86_64; do
  case "$abi" in
    arm64-v8a) arch="arm64" ;;
    x86_64) arch="x86_64" ;;
  esac
  dir="$TMP/lib/$abi"
  [[ -d "$dir" ]] || {
    echo "APK is missing ABI directory lib/$abi" >&2
    exit 1
  }

  for required in libjarvis_native.so libllama.so libwhisper.so libggml.so libggml-base.so libc++_shared.so; do
    [[ -f "$dir/$required" ]] || {
      echo "APK is missing required library lib/$abi/$required" >&2
      exit 1
    }
  done

  shopt -s nullglob
  libs=("$dir"/*.so)
  shopt -u nullglob
  (( ${#libs[@]} > 0 )) || {
    echo "APK contains no shared libraries under lib/$abi" >&2
    exit 1
  }

  for lib in "${libs[@]}"; do
    validate_elf_file "$lib" "$READELF" "$arch" || {
      echo "APK ELF validation failed for $lib" >&2
      exit 1
    }
    validate_dependency_closure "$lib" "$READELF" "$arch" "$dir" || exit 1
  done

  # Fail on SONAME collisions, including duplicate vendored GGML implementations.
  validate_no_soname_conflicts "$READELF" "${libs[@]}" || exit 1

  echo "APK native architecture, dependency closure, SONAME uniqueness, and 16 KiB LOAD alignment passed for $abi"
done

echo "APK verification passed: $APK"
