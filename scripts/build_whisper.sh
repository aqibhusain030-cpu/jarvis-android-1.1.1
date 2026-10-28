#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"
WHISPER_COMMIT="927cfce34f31707e17f2bff35c349632fb9e2c3a"
WHISPER_URL="https://github.com/ggml-org/whisper.cpp.git"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
mkdir -p "$TP"
if [[ ! -d "$TP/whisper.cpp/.git" ]]; then
  git init "$TP/whisper.cpp"
  git -C "$TP/whisper.cpp" remote add origin "$WHISPER_URL"
fi
git -C "$TP/whisper.cpp" fetch --depth 1 origin "$WHISPER_COMMIT"
git -C "$TP/whisper.cpp" checkout --detach --force "$WHISPER_COMMIT"

for pair in "arm64-v8a:arm64" "x86_64:x86_64"; do
  IFS=: read -r abi arch <<< "$pair"
  build_dir="$TP/whisper.cpp/build-android-${arch}"
  ggml_prefix="$TP/llama.cpp/install-android-${arch}"
  test -f "$ggml_prefix/lib/cmake/ggml/ggml-config.cmake" || {
    echo "Missing llama.cpp GGML package for ${abi}: $ggml_prefix" >&2
    echo "Run scripts/build_llama.sh first." >&2
    exit 1
  }
  echo "Building whisper.cpp for ${abi} against the shared llama.cpp GGML package"
  cmake -S "$TP/whisper.cpp" -B "$build_dir" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-31 \
    -DCMAKE_PREFIX_PATH="$ggml_prefix" \
    -DWHISPER_USE_SYSTEM_GGML=ON \
    -DWHISPER_BUILD_TESTS=OFF -DWHISPER_BUILD_EXAMPLES=OFF \
    -DWHISPER_BUILD_SERVER=OFF -DWHISPER_CURL=OFF \
    -DBUILD_SHARED_LIBS=ON -DGGML_NATIVE=OFF \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384
  cmake --build "$build_dir" --config Release -j"$(nproc)"
  test -f "$build_dir/bin/libwhisper.so"
done
