#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TP="$ROOT/app/src/main/cpp/third_party"
LLAMA_COMMIT="526c43b8f7dfea9032e9f35e7a1be9183ca7cc20"
LLAMA_URL="https://github.com/ggml-org/llama.cpp.git"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
mkdir -p "$TP"
if [[ ! -d "$TP/llama.cpp/.git" ]]; then
  git init "$TP/llama.cpp"
  git -C "$TP/llama.cpp" remote add origin "$LLAMA_URL"
fi
git -C "$TP/llama.cpp" fetch --depth 1 origin "$LLAMA_COMMIT"
git -C "$TP/llama.cpp" checkout --detach --force "$LLAMA_COMMIT"

for pair in "arm64-v8a:arm64" "x86_64:x86_64"; do
  IFS=: read -r abi arch <<< "$pair"
  build_dir="build-android-${arch}"
  install_prefix="$TP/llama.cpp/install-android-${arch}"
  echo "Building llama.cpp + shared GGML for ${abi}"
  rm -rf "$install_prefix"
  cmake -S "$TP/llama.cpp" -B "$TP/llama.cpp/$build_dir" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-31 \
    -DCMAKE_INSTALL_PREFIX="$install_prefix" \
    -DCMAKE_INSTALL_LIBDIR=lib \
    -DBUILD_SHARED_LIBS=ON -DLLAMA_BUILD_COMMON=OFF -DLLAMA_BUILD_TOOLS=OFF \
    -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF -DLLAMA_BUILD_SERVER=OFF \
    -DLLAMA_BUILD_APP=OFF -DLLAMA_CURL=OFF -DGGML_LLAMAFILE=OFF -DGGML_OPENMP=OFF \
    -DGGML_NATIVE=OFF -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384
  cmake --build "$TP/llama.cpp/$build_dir" --config Release -j"$(nproc)"
  cmake --install "$TP/llama.cpp/$build_dir" --config Release

  test -f "$install_prefix/lib/libllama.so"
  test -f "$install_prefix/lib/libggml.so"
  test -f "$install_prefix/lib/libggml-base.so"
done
