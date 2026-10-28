#!/usr/bin/env python3
"""Regression tests for strict APK zipalign capability and invocation checks."""
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_LIBRARIES = (
    "libjarvis_native.so",
    "libllama.so",
    "libwhisper.so",
    "libggml.so",
    "libggml-base.so",
    "libc++_shared.so",
)


def run_verifier(temp: Path, *, advertise_page_alignment: bool) -> subprocess.CompletedProcess[str]:
    bin_dir = temp / "bin"
    bin_dir.mkdir()
    readelf = bin_dir / "llvm-readelf"
    readelf.write_text(
        """#!/usr/bin/env bash
set -eu
case "$1" in
  -h)
    case "$2" in
      */arm64-v8a/*) machine=AArch64 ;;
      *) machine="Advanced Micro Devices X86-64" ;;
    esac
    printf '  Type: DYN (Shared object file)\\n  Machine: %s\\n' "$machine"
    ;;
  -lW) printf '  LOAD 0x000000 0x000000 0x000000 0x001000 0x001000 R E 0x4000\\n' ;;
  -d|-dW) : ;;
  *) exit 2 ;;
esac
""",
        encoding="utf-8",
    )
    readelf.chmod(0o755)

    zipalign = bin_dir / "zipalign"
    advertised = (
        "Usage: zipalign [-f] [-P <pagesize_kb>] [-v] <align> infile.zip outfile.zip\\n"
        "Valid page sizes: 4, 16, and 64 KiB.\\n"
    )
    if not advertise_page_alignment:
        advertised = "Usage: zipalign [-f] [-v] <align> infile.zip outfile.zip\\n"
    zipalign.write_text(
        """#!/usr/bin/env bash
set -eu
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
  printf '%s' "$ZIPALIGN_HELP"
  for i in $(seq 1 20000); do printf '%s\\n' 'additional help text'; done
  exit 1
fi
[[ "$1" == "-c" && "$2" == "-P" && "$3" == "16" && "$4" == "4" ]]
printf '%s\\n' "$*" >> "$ZIPALIGN_ARGS_LOG"
""",
        encoding="utf-8",
    )
    zipalign.chmod(0o755)

    apk = temp / "app-debug.apk"
    with zipfile.ZipFile(apk, "w") as archive:
        for abi in ("arm64-v8a", "x86_64"):
            for library in REQUIRED_LIBRARIES:
                archive.writestr(f"lib/{abi}/{library}", b"mock ELF")

    args_log = temp / "zipalign-args.txt"
    env = os.environ.copy()
    env.update(
        {
            "PATH": f"{bin_dir}:{env['PATH']}",
            "JARVIS_ELF_TOOL": str(readelf),
            "ZIPALIGN_ARGS_LOG": str(args_log),
            "ZIPALIGN_HELP": advertised,
        }
    )
    env.pop("ANDROID_HOME", None)
    env.pop("ANDROID_SDK_ROOT", None)

    return subprocess.run(
        ["bash", str(ROOT / "tools/verify_native_apk.sh"), str(apk)],
        cwd=ROOT,
        env=env,
        capture_output=True,
        text=True,
        check=False,
    )


with tempfile.TemporaryDirectory(prefix="jarvis-apk-verifier-supported-") as temp_dir:
    temp = Path(temp_dir)
    result = run_verifier(temp, advertise_page_alignment=True)
    if result.returncode != 0:
        raise SystemExit(result.stdout + result.stderr)
    assert "APK native architecture" in result.stdout
    assert "APK verification passed" in result.stdout
    assert (temp / "zipalign-args.txt").read_text(encoding="utf-8").splitlines() == [
        "-c -P 16 4 " + str(temp / "app-debug.apk")
    ]

with tempfile.TemporaryDirectory(prefix="jarvis-apk-verifier-unsupported-") as temp_dir:
    temp = Path(temp_dir)
    result = run_verifier(temp, advertise_page_alignment=False)
    assert result.returncode == 2, (result.stdout, result.stderr)
    assert "without verified -P <pagesize_kb> support" in result.stderr
    assert not (temp / "zipalign-args.txt").exists()

print("native APK verifier regression: PASS")
