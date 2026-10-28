cmake_minimum_required(VERSION 3.22)
project(jarvis_native LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(NATIVE_DIR "${CMAKE_CURRENT_LIST_DIR}")
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(ARCH arm64)
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(ARCH x86_64)
else()
    message(FATAL_ERROR "jarvis_native: unsupported Android ABI '${ANDROID_ABI}'")
endif()

set(LLAMA_ROOT "${NATIVE_DIR}/third_party/llama.cpp")
set(WHISPER_ROOT "${NATIVE_DIR}/third_party/whisper.cpp")
set(LLAMA_INSTALL "${LLAMA_ROOT}/install-android-${ARCH}")
set(LLAMA_LIB_DIR "${LLAMA_INSTALL}/lib")
set(WHISPER_BIN "${WHISPER_ROOT}/build-android-${ARCH}/bin")

foreach(required_file
        "${LLAMA_LIB_DIR}/libllama.so"
        "${LLAMA_LIB_DIR}/libggml.so"
        "${LLAMA_LIB_DIR}/libggml-base.so"
        "${WHISPER_BIN}/libwhisper.so")
    if(NOT EXISTS "${required_file}")
        message(FATAL_ERROR
            "Missing native dependency: ${required_file}. "
            "Run scripts/build_llama.sh and scripts/build_whisper.sh for ABI ${ANDROID_ABI}.")
    endif()
endforeach()

add_library(llama SHARED IMPORTED GLOBAL)
set_target_properties(llama PROPERTIES
    IMPORTED_LOCATION "${LLAMA_LIB_DIR}/libllama.so"
    INTERFACE_INCLUDE_DIRECTORIES "${LLAMA_ROOT}/include")

add_library(whisper SHARED IMPORTED GLOBAL)
set_target_properties(whisper PROPERTIES
    IMPORTED_LOCATION "${WHISPER_BIN}/libwhisper.so"
    INTERFACE_INCLUDE_DIRECTORIES "${WHISPER_ROOT}/include")

# Both upstream projects are configured to use the exact same llama.cpp GGML
# installation. Import and package every GGML shared object from that one prefix.
file(GLOB GGML_SHARED_LIBS CONFIGURE_DEPENDS "${LLAMA_LIB_DIR}/libggml*.so")
if(NOT GGML_SHARED_LIBS)
    message(FATAL_ERROR "No libggml*.so libraries found in ${LLAMA_LIB_DIR}")
endif()

set(GGML_IMPORTED_TARGETS "")
foreach(ggml_library IN LISTS GGML_SHARED_LIBS)
    get_filename_component(ggml_name "${ggml_library}" NAME)
    string(REGEX REPLACE "[^A-Za-z0-9_]" "_" ggml_target_suffix "${ggml_name}")
    set(ggml_target "ggml_import_${ggml_target_suffix}")
    add_library("${ggml_target}" SHARED IMPORTED GLOBAL)
    set_target_properties("${ggml_target}" PROPERTIES IMPORTED_LOCATION "${ggml_library}")
    list(APPEND GGML_IMPORTED_TARGETS "${ggml_target}")
endforeach()

add_library(jarvis_native SHARED llama_jni.cpp whisper_jni.cpp)
target_include_directories(jarvis_native PRIVATE
    "${LLAMA_ROOT}/include"
    "${LLAMA_ROOT}/ggml/include"
    "${WHISPER_ROOT}/include")

target_link_libraries(jarvis_native PRIVATE
    android
    log
    llama
    whisper
    ${GGML_IMPORTED_TARGETS})

target_link_options(jarvis_native PRIVATE -Wl,--no-undefined -Wl,-z,max-page-size=16384)
target_compile_options(jarvis_native PRIVATE
    $<$<CONFIG:Release>:-O3;-ffast-math;-fno-finite-math-only>)
