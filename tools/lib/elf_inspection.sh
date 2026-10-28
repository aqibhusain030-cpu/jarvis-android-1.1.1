# Shared ELF inspection helpers. Source this file from verification scripts.
# Deliberately does not mutate shell options or resolve a tool as a source-time side effect.

resolve_elf_tool() {
  local -a candidates=()
  local -a roots=()
  local root candidate probe

  if [[ -n "${JARVIS_ELF_TOOL:-}" ]]; then
    candidates+=("${JARVIS_ELF_TOOL}")
  fi
  if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
    roots+=("${ANDROID_NDK_HOME}")
  fi
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    roots+=("${ANDROID_HOME}/ndk")
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    roots+=("${ANDROID_SDK_ROOT}/ndk")
  fi

  for root in "${roots[@]}"; do
    [[ -d "$root" ]] || continue
    while IFS= read -r candidate; do
      [[ -n "$candidate" ]] || continue
      candidates+=("$candidate")
    done < <(
      find "$root" \( -type f -o -type l \) \
        \( -name 'llvm-readelf' -o -name 'readelf' \) \
        -print 2>/dev/null | sort -u
    )
  done

  if command -v llvm-readelf >/dev/null 2>&1; then
    candidates+=("$(command -v llvm-readelf)")
  fi
  if command -v readelf >/dev/null 2>&1; then
    candidates+=("$(command -v readelf)")
  fi

  # Probe with an actual ELF binary, never the calling shell script ($0).
  probe="${ELF_INSPECTION_PROBE_FILE:-/bin/ls}"
  if [[ ! -f "$probe" ]]; then
    probe="$(command -v bash 2>/dev/null || true)"
  fi
  [[ -n "$probe" && -f "$probe" ]] || {
    echo "Cannot find a known ELF probe file for readelf validation." >&2
    return 1
  }

  for candidate in "${candidates[@]}"; do
    [[ -n "$candidate" && -x "$candidate" ]] || continue
    if "$candidate" -h "$probe" >/dev/null 2>&1 &&
       "$candidate" -lW "$probe" >/dev/null 2>&1 &&
       "$candidate" -d "$probe" >/dev/null 2>&1; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  echo "Neither llvm-readelf nor compatible readelf was found in the configured NDK/SDK or PATH." >&2
  return 1
}

get_elf_type() {
  local file="$1" tool="$2" output
  output="$("$tool" -h "$file" 2>/dev/null)" || {
    echo "Unable to read ELF header: $file" >&2
    return 1
  }
  printf '%s\n' "$output" |
    sed -n 's/^[[:space:]]*Type:[[:space:]]*//p' | head -n 1
}

get_elf_machine() {
  local file="$1" tool="$2" output
  output="$("$tool" -h "$file" 2>/dev/null)" || {
    echo "Unable to read ELF machine type: $file" >&2
    return 1
  }
  printf '%s\n' "$output" |
    sed -n 's/^[[:space:]]*Machine:[[:space:]]*//p' | head -n 1
}

validate_elf_machine() {
  local file="$1" tool="$2" expected_arch="$3"
  local machine normalized
  machine="$(get_elf_machine "$file" "$tool")" || return 1
  normalized="$(printf '%s' "$machine" | tr '[:upper:]' '[:lower:]')"

  case "$expected_arch" in
    arm64|arm64-v8a|aarch64)
      if [[ "$normalized" != *aarch64* && "$normalized" != *arm64* ]]; then
        echo "Wrong ELF machine for ARM64: $file reports '$machine'" >&2
        return 1
      fi
      ;;
    x86_64|x64)
      if [[ "$normalized" != *x86-64* &&
            "$normalized" != *x86_64* &&
            "$normalized" != *em_x86_64* ]]; then
        echo "Wrong ELF machine for x86_64: $file reports '$machine'" >&2
        return 1
      fi
      ;;
    *)
      echo "Unsupported expected ELF architecture '$expected_arch' for $file" >&2
      return 1
      ;;
  esac
}

is_hex_power_of_two_at_least_16k() {
  local value="$1" decimal
  [[ "$value" =~ ^0[xX][0-9A-Fa-f]+$ ]] || return 1
  decimal=$((value))
  (( decimal >= 16384 )) || return 1
  (( (decimal & (decimal - 1)) == 0 )) || return 1
  return 0
}

validate_elf_load_alignment() {
  local file="$1" tool="$2" output line alignment
  output="$("$tool" -lW "$file" 2>/dev/null)" || {
    echo "Unable to inspect ELF program headers for $file" >&2
    return 1
  }

  local load_count=0 invalid_count=0
  while IFS= read -r line; do
    [[ "$line" =~ ^[[:space:]]*LOAD[[:space:]] ]] || continue
    load_count=$((load_count + 1))
    alignment="$(printf '%s\n' "$line" | awk '{ print $NF }')"

    if ! is_hex_power_of_two_at_least_16k "$alignment"; then
      echo "Invalid 16 KiB LOAD alignment '$alignment' in $file" >&2
      echo "$line" >&2
      invalid_count=$((invalid_count + 1))
    fi
  done <<< "$output"

  if (( load_count == 0 )); then
    echo "No ELF LOAD segments found in $file" >&2
    return 1
  fi
  (( invalid_count == 0 )) || return 1
  return 0
}

get_elf_needed_libraries() {
  local file="$1" tool="$2" output
  output="$("$tool" -dW "$file" 2>/dev/null)" || {
    echo "Unable to inspect DT_NEEDED entries for $file" >&2
    return 1
  }
  printf '%s\n' "$output" |
    sed -n 's/.*Shared library: \[\([^]]*\)\].*/\1/p'
}

get_elf_soname() {
  local file="$1" tool="$2" output
  output="$("$tool" -dW "$file" 2>/dev/null)" || {
    echo "Unable to inspect DT_SONAME for $file" >&2
    return 1
  }
  printf '%s\n' "$output" |
    sed -n 's/.*Library soname: \[\([^]]*\)\].*/\1/p' | head -n 1
}

validate_elf_file() {
  local file="$1" tool="$2" expected_arch="$3"
  local type
  [[ -f "$file" ]] || {
    echo "ELF file does not exist: $file" >&2
    return 1
  }

  type="$(get_elf_type "$file" "$tool")" || return 1
  if [[ "$type" != *DYN* && "$type" != *"Shared object"* ]]; then
    echo "Expected a shared ELF object but '$file' reports type '$type'" >&2
    return 1
  fi

  validate_elf_machine "$file" "$tool" "$expected_arch" || return 1
  validate_elf_load_alignment "$file" "$tool" || return 1
  get_elf_needed_libraries "$file" "$tool" >/dev/null || return 1
  return 0
}

is_android_public_system_library() {
  local name normalized
  name="$(basename "$1")"
  normalized="$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]')"
  case "$normalized" in
    libc.so|libm.so|libdl.so|liblog.so|libandroid.so|libz.so|\
    libjnigraphics.so|libnativewindow.so|libegl.so|libglesv1_cm.so|\
    libglesv2.so|libglesv3.so|libopensles.so|libaaudio.so|\
    libvulkan.so|libmediandk.so|libcamera2ndk.so|libsync.so|\
    libneuralnetworks.so|libamidi.so|libbinder_ndk.so|libhardware.so)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

find_elf_dependency() {
  local needed="$1" tool="$2"
  shift 2
  local directory candidate soname
  local -a matches=()

  for directory in "$@"; do
    [[ -d "$directory" ]] || continue
    if [[ -f "$directory/$needed" ]]; then
      matches+=("$directory/$needed")
    fi
  done

  if (( ${#matches[@]} == 1 )); then
    printf '%s\n' "${matches[0]}"
    return 0
  elif (( ${#matches[@]} > 1 )); then
    echo "Ambiguous dependency '$needed' is present by filename in multiple directories: ${matches[*]}" >&2
    return 1
  fi

  for directory in "$@"; do
    [[ -d "$directory" ]] || continue
    local -a candidates=("$directory"/*.so)
    for candidate in "${candidates[@]}"; do
      [[ -f "$candidate" ]] || continue
      soname="$(get_elf_soname "$candidate" "$tool")" || return 1
      if [[ "$soname" == "$needed" ]]; then
        matches+=("$candidate")
      fi
    done
  done

  if (( ${#matches[@]} == 1 )); then
    printf '%s\n' "${matches[0]}"
    return 0
  elif (( ${#matches[@]} > 1 )); then
    echo "Ambiguous DT_SONAME '$needed' resolved to multiple files: ${matches[*]}" >&2
    return 1
  fi
  return 1
}

validate_dependency_closure() {
  local file="$1" tool="$2" expected_arch="$3"
  shift 3
  local dependencies dependency resolved
  dependencies="$(get_elf_needed_libraries "$file" "$tool")" || return 1

  while IFS= read -r dependency; do
    [[ -n "$dependency" ]] || continue
    if is_android_public_system_library "$dependency"; then
      continue
    fi

    if ! resolved="$(find_elf_dependency "$dependency" "$tool" "$@")"; then
      echo "Unresolved DT_NEEDED dependency '$dependency' referenced by $file" >&2
      return 1
    fi

    validate_elf_machine "$resolved" "$tool" "$expected_arch" || return 1
  done <<< "$dependencies"
  return 0
}

validate_no_soname_conflicts() {
  local tool="$1"
  shift
  local file soname previous
  declare -A seen_sonames=()

  for file in "$@"; do
    [[ -f "$file" ]] || continue
    soname="$(get_elf_soname "$file" "$tool")" || return 1
    [[ -n "$soname" ]] || continue
    previous="${seen_sonames[$soname]:-}"
    if [[ -n "$previous" && "$previous" != "$file" ]]; then
      echo "Conflicting DT_SONAME '$soname' is provided by '$previous' and '$file'" >&2
      return 1
    fi
    seen_sonames["$soname"]="$file"
  done
}

find_ndk_runtime_dir() {
  local arch="$1" triple root file
  case "$arch" in
    arm64|arm64-v8a) triple="aarch64-linux-android" ;;
    x86_64) triple="x86_64-linux-android" ;;
    *) return 1 ;;
  esac

  local -a roots=()
  if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
    roots+=("${ANDROID_NDK_HOME}")
  fi
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    roots+=("${ANDROID_HOME}/ndk")
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    roots+=("${ANDROID_SDK_ROOT}/ndk")
  fi

  for root in "${roots[@]}"; do
    [[ -d "$root" ]] || continue
    file="$(find "$root" -type f \
      -path "*/sysroot/usr/lib/$triple/libc++_shared.so" -print 2>/dev/null | sort -V | tail -n 1)"
    if [[ -n "$file" ]]; then
      dirname "$file"
      return 0
    fi
  done
  return 1
}
