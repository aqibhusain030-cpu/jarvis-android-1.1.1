#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"
LLAMA_COMMIT="526c43b8f7dfea9032e9f35e7a1be9183ca7cc20"
LLAMA_URL="https://github.com/ggml-org/llama.cpp.git"
mkdir -p "$TP"
if [[ ! -d "$TP/llama.cpp/.git" ]]; then
  git init "$TP/llama.cpp"
  git -C "$TP/llama.cpp" remote add origin "$LLAMA_URL"
fi
git -C "$TP/llama.cpp" fetch --depth 1 origin "$LLAMA_COMMIT"
git -C "$TP/llama.cpp" checkout --detach --force "$LLAMA_COMMIT"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
cd "$TP/llama.cpp"
cmake -B build-android-arm64 \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-31 \
  -DBUILD_SHARED_LIBS=ON -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF \
  -DLLAMA_BUILD_SERVER=OFF -DLLAMA_CURL=OFF -DGGML_LLAMAFILE=ON -DGGML_OPENMP=OFF \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build-android-arm64 --config Release -j"$(nproc)"
