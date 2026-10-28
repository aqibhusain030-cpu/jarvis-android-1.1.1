#!/usr/bin/env bash
set -euo pipefail

# Resolve a usable ELF inspection tool from the actual Android/host environment.
# Prefer NDK-provided llvm-readelf, but accept GNU readelf when it is the only
# compatible option available. The chosen tool must be executable and capable of
# printing ELF headers, program headers, and dynamic section entries.
resolve_elf_tool() {
  local -a candidates=()
  local root

  for root in \
    "${ANDROID_NDK_HOME:-}" \
    "${ANDROID_HOME:-}/ndk" \
    "${ANDROID_SDK_ROOT:-}/ndk"; do
    [[ -n "$root" ]] || continue
    if [[ -d "$root" ]]; then
      while IFS= read -r path; do
        [[ -n "$path" ]] || continue
        if [[ -x "$path" ]]; then
          candidates+=("$path")
        fi
      done < <(find "$root" \( -path '*/toolchains/llvm/prebuilt/*/bin' -o -path '*/bin' \) -type f \( -name 'llvm-readelf' -o -name 'readelf' \) 2>/dev/null | sort)
    fi
  done

  if command -v llvm-readelf >/dev/null 2>&1; then
    candidates+=("$(command -v llvm-readelf)")
  fi
  if command -v readelf >/dev/null 2>&1; then
    candidates+=("$(command -v readelf)")
  fi

  local candidate
  for candidate in "${candidates[@]}"; do
    [[ -n "$candidate" ]] || continue
    if [[ -x "$candidate" ]]; then
      if "$candidate" -h "$0" >/dev/null 2>&1; then
        printf '%s\n' "$candidate"
        return 0
      fi
    fi
  done

  return 1
}

is_hex_power_of_two() {
  local value="$1"
  value="${value#0x}"
  value="${value#0X}"
  [[ "$value" =~ ^[0-9A-Fa-f]+$ ]] || return 1
  local decimal=0
  local nibble
  for nibble in $(printf '%s' "$value" | fold -w 1); do
    decimal=$((decimal * 16 + 16#${nibble}))
  done
  if (( decimal < 16384 )); then
    return 1
  fi
  if (( (decimal & (decimal - 1)) != 0 )); then
    return 1
  fi
  return 0
}

get_elf_machine() {
  local file="$1"
  local readefl="$2"
  "$readefl" -h "$file" 2>/dev/null | awk '/Machine:/ { print $2; exit }'
}

get_elf_needed_libraries() {
  local file="$1"
  local readefl="$2"
  "$readefl" -d "$file" 2>/dev/null | sed -n 's/.*Shared library: \[\([^]]*\)\].*/\1/p'
}

validate_elf_load_alignment() {
  local file="$1"
  local readefl="$2"
  local output
  output="$($readefl -lW "$file" 2>/dev/null)" || {
    echo "Unable to inspect ELF program headers for $file" >&2
    return 1
  }

  local load_count=0
  local invalid_count=0
  local line
  while IFS= read -r line; do
    [[ -n "$line" ]] || continue
    case "$line" in
      *"LOAD"*)
        load_count=$((load_count + 1))
        local align
        align="$(printf '%s\n' "$line" | awk '{print $NF}')"
        [[ -n "$align" ]] || {
          echo "Missing LOAD alignment in $file" >&2
          echo "$line" >&2
          return 1
        }
        if ! is_hex_power_of_two "$align"; then
          echo "Invalid ELF LOAD alignment in $file: $align" >&2
          echo "$line" >&2
          invalid_count=$((invalid_count + 1))
        fi
        ;;
    esac
  done <<< "$output"

  if (( load_count == 0 )); then
    echo "No LOAD segments found in $file" >&2
    return 1
  fi

  if (( invalid_count > 0 )); then
    return 1
  fi

  return 0
}

validate_elf_file() {
  local file="$1"
  local tool="$2"
  local machine
  machine="$(get_elf_machine "$file" "$tool")"
  if [[ -z "$machine" ]]; then
    echo "Not an ELF file or unable to read ELF headers: $file" >&2
    return 1
  fi

  case "$machine" in
    AArch64|x86_64|ARM64|X86_64)
      ;;
    *)
      echo "Unexpected ELF machine type for $file: $machine" >&2
      return 1
      ;;
  esac

  validate_elf_load_alignment "$file" "$tool" || return 1

  local dep
  while IFS= read -r dep; do
    [[ -n "$dep" ]] || continue
    printf '%s\n' "$dep"
  done < <(get_elf_needed_libraries "$file" "$tool")
}

resolve_elf_tool || {
  echo "Unable to locate a usable ELF inspection tool. Set ANDROID_NDK_HOME, ANDROID_HOME, or ANDROID_SDK_ROOT to a valid Android SDK/NDK install." >&2
  exit 2
}

READELF="$(resolve_elf_tool)"
printf 'ELF tool: %s\n' "$READELF"
printf 'ELF tool version: %s\n' "$($READELF --version 2>/dev/null | head -n 1 || printf 'unknown')"

"$READELF" -h /bin/ls >/dev/null 2>&1 || {
  echo "Selected ELF tool cannot inspect ELF files: $READELF" >&2
  exit 2
}

export READELF
