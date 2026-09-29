cmake_minimum_required(VERSION 3.22)
project(jarvis_native LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(jarvis_native SHARED llama_jni.cpp whisper_jni.cpp)

# This CMake file lives in app/src/main/cpp. The pinned upstream builds
# emit their shared libraries into <build-dir>/bin.
set(NATIVE_DIR ${CMAKE_CURRENT_LIST_DIR})
set(LLAMA_DIR ${NATIVE_DIR}/third_party/llama.cpp/build-android-arm64/bin)
set(WHISPER_DIR ${NATIVE_DIR}/third_party/whisper.cpp/build-android-arm64/bin)

target_include_directories(jarvis_native PRIVATE
    ${NATIVE_DIR}/third_party/llama.cpp/include
    ${NATIVE_DIR}/third_party/llama.cpp/ggml/include
    ${NATIVE_DIR}/third_party/whisper.cpp/include
    ${NATIVE_DIR}/third_party/whisper.cpp/ggml/include)

target_link_libraries(jarvis_native
    android log
    ${LLAMA_DIR}/libllama.so
    ${LLAMA_DIR}/libggml.so
    ${WHISPER_DIR}/libwhisper.so)

target_compile_options(jarvis_native PRIVATE -O3 -ffast-math -fno-finite-math-only)
