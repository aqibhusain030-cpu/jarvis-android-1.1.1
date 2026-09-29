#include <jni.h>
#include <vector>
#include <cstdint>
#include <string>
#include <algorithm>
#include <whisper.h>

struct WhisperHandle {
    whisper_context* ctx = nullptr;
    int threads = 4;
};

static std::string jstring_to_string(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string out(chars);
    env->ReleaseStringUTFChars(value, chars);
    return out;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_jarvis_brain_WhisperJNI_nativeInit(JNIEnv* env, jclass, jstring modelPath, jint threads) {
    const std::string path = jstring_to_string(env, modelPath);
    if (path.empty()) return 0;

    whisper_context_params cparams = whisper_context_default_params();
    whisper_context* ctx = whisper_init_from_file_with_params(path.c_str(), cparams);
    if (!ctx) return 0;

    auto* h = new WhisperHandle();
    h->ctx = ctx;
    h->threads = std::clamp(static_cast<int>(threads), 1, 16);
    return reinterpret_cast<jlong>(h);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_jarvis_brain_WhisperJNI_nativeTranscribe(
    JNIEnv* env, jclass, jlong handle, jbyteArray jpcm, jstring jlanguage) {
    auto* h = reinterpret_cast<WhisperHandle*>(handle);
    if (!h || !h->ctx || !jpcm) return env->NewStringUTF("");

    const jsize n = env->GetArrayLength(jpcm);
    constexpr jsize kMaxPcmBytes = 60 * 16000 * 2;
    if (n <= 0 || (n % 2) != 0 || n > kMaxPcmBytes) {
        return env->NewStringUTF("");
    }

    std::vector<jbyte> raw(static_cast<size_t>(n));
    env->GetByteArrayRegion(jpcm, 0, n, raw.data());
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return env->NewStringUTF("");
    }

    const size_t samples = static_cast<size_t>(n) / 2u;
    std::vector<float> pcm(samples);
    for (size_t i = 0; i < samples; ++i) {
        const uint16_t lo = static_cast<uint8_t>(raw[2u * i]);
        const uint16_t hi = static_cast<uint8_t>(raw[2u * i + 1u]);
        const int16_t s = static_cast<int16_t>((hi << 8) | lo);
        pcm[i] = static_cast<float>(s) / 32768.0f;
    }

    whisper_full_params p = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    p.print_progress = false;
    p.print_realtime = false;
    p.print_timestamps = false;
    p.print_special = false;
    p.translate = false;
    p.language = jlanguage ? nullptr : "auto";

    const std::string language = jstring_to_string(env, jlanguage);
    if (!language.empty() && language != "auto") {
        p.language = language.c_str();
    } else {
        p.language = "auto";
    }
    p.n_threads = h->threads;

    if (whisper_full(h->ctx, p, pcm.data(), static_cast<int>(pcm.size())) != 0) {
        return env->NewStringUTF("");
    }

    std::string result;
    const int segments = whisper_full_n_segments(h->ctx);
    for (int i = 0; i < segments; ++i) {
        result += whisper_full_get_segment_text(h->ctx, i);
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_brain_WhisperJNI_nativeFree(JNIEnv*, jclass, jlong handle) {
    auto* h = reinterpret_cast<WhisperHandle*>(handle);
    if (!h) return;
    if (h->ctx) whisper_free(h->ctx);
    delete h;
}
