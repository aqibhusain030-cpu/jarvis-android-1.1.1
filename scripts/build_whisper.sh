#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"
WHISPER_COMMIT="927cfce34f31707e17f2bff35c349632fb9e2c3a"
WHISPER_URL="https://github.com/ggml-org/whisper.cpp.git"
mkdir -p "$TP"
if [[ ! -d "$TP/whisper.cpp/.git" ]]; then
  git init "$TP/whisper.cpp"
  git -C "$TP/whisper.cpp" remote add origin "$WHISPER_URL"
fi
git -C "$TP/whisper.cpp" fetch --depth 1 origin "$WHISPER_COMMIT"
git -C "$TP/whisper.cpp" checkout --detach --force "$WHISPER_COMMIT"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
cd "$TP/whisper.cpp"
cmake -B build-android-arm64 \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-31 \
  -DWHISPER_BUILD_TESTS=OFF -DWHISPER_BUILD_EXAMPLES=OFF \
  -DWHISPER_BUILD_SERVER=OFF -DBUILD_SHARED_LIBS=ON \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build-android-arm64 --config Release -j"$(nproc)"
