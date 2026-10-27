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
  -DBUILD_SHARED_LIBS=ON -DLLAMA_BUILD_COMMON=OFF -DLLAMA_BUILD_TOOLS=OFF \
  -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF -DLLAMA_BUILD_SERVER=OFF \
  -DLLAMA_BUILD_APP=OFF -DLLAMA_CURL=OFF -DGGML_LLAMAFILE=ON -DGGML_OPENMP=OFF \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build-android-arm64 --config Release -j"$(nproc)"

# llama.cpp now splits ggml across several shared libraries. Android only loads
# libraries included in the APK; linking libllama.so does not guarantee that
# its transitive dependencies are packaged. Stage these exact, matching build
# outputs in the standard JNI libs source set so voice-service startup can load
# libjarvis_native.so on-device.
JNI_LIB_DIR="$ROOT/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$JNI_LIB_DIR"
for name in libggml-base.so libggml-cpu.so; do
  source="$TP/llama.cpp/build-android-arm64/bin/$name"
  if [[ ! -s "$source" ]]; then
    echo "Required Android native dependency missing: $source" >&2
    exit 1
  fi
  cp -f "$source" "$JNI_LIB_DIR/$name"
done
