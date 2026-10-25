#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include <atomic>
#include "llama.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "jarvis.llama", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "jarvis.llama", __VA_ARGS__)

struct LlamaHandle {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    const llama_vocab* vocab = nullptr;
    std::atomic<bool> cancel{false};
    std::mutex mu;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_jarvis_brain_LlamaJNI_nativeInit(
    JNIEnv* env, jobject, jstring jpath, jint ctxSize, jint threads) {
    llama_backend_init();
    if (!jpath) return 0;

    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (!path) return 0;

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    llama_model* model = llama_load_model_from_file(path, mp);
    env->ReleaseStringUTFChars(jpath, path);
    if (!model) {
        LOGE("model load failed");
        return 0;
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = ctxSize > 0 ? static_cast<uint32_t>(ctxSize) : 2048;
    cp.n_threads = threads > 0 ? threads : 4;
    cp.n_batch = 512;

    llama_context* ctx = llama_new_context_with_model(model, cp);
    if (!ctx) {
        llama_free_model(model);
        LOGE("ctx create failed");
        return 0;
    }

    auto* h = new LlamaHandle();
    h->model = model;
    h->ctx = ctx;
    h->vocab = llama_model_get_vocab(model);
    LOGI("llama init ok ctx=%u threads=%d", cp.n_ctx, cp.n_threads);
    return reinterpret_cast<jlong>(h);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_brain_LlamaJNI_nativeFree(JNIEnv*, jobject, jlong handle) {
    auto* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h) return;
    std::lock_guard<std::mutex> lk(h->mu);
    if (h->ctx) llama_free(h->ctx);
    if (h->model) llama_free_model(h->model);
    delete h;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_jarvis_brain_LlamaJNI_nativeGenerate(
    JNIEnv* env, jobject, jlong handle, jstring jprompt, jint maxTokens, jfloat temp) {
    auto* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h || !jprompt) return env->NewStringUTF("");

    h->cancel.store(false);
    const char* prompt = env->GetStringUTFChars(jprompt, nullptr);
    if (!prompt) return env->NewStringUTF("");
    std::string promptStr(prompt);
    env->ReleaseStringUTFChars(jprompt, prompt);

    std::string out;
    std::lock_guard<std::mutex> lk(h->mu);

    // Each generate() call receives a complete prompt from BrainBridge.
    // Do not leak KV/cache state from a previous independent request.
    llama_memory_clear(llama_get_memory(h->ctx), true);

    int n_prompt = -llama_tokenize(
        h->vocab, promptStr.c_str(), static_cast<int>(promptStr.size()),
        nullptr, 0, true, false);
    if (n_prompt <= 0) return env->NewStringUTF("");

    std::vector<llama_token> tokens(static_cast<size_t>(n_prompt));
    if (llama_tokenize(
            h->vocab, promptStr.c_str(), static_cast<int>(promptStr.size()),
            tokens.data(), n_prompt, true, false) != n_prompt) {
        return env->NewStringUTF("");
    }

    llama_batch batch = llama_batch_get_one(tokens.data(), static_cast<int>(tokens.size()));
    if (llama_decode(h->ctx, batch) != 0) return env->NewStringUTF("");

    llama_sampler* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (!smpl) return env->NewStringUTF("");

    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temp > 0.0f ? temp : 0.2f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    const int limit = std::max(1, std::min(static_cast<int>(maxTokens), 2048));
    for (int i = 0; i < limit && !h->cancel.load(); ++i) {
        llama_token id = llama_sampler_sample(smpl, h->ctx, -1);
        if (llama_vocab_is_eog(h->vocab, id)) break;

        char buf[512];
        const int n = llama_token_to_piece(h->vocab, id, buf, sizeof(buf), 0, true);
        if (n > 0) out.append(buf, n);

        llama_batch b = llama_batch_get_one(&id, 1);
        if (llama_decode(h->ctx, b) != 0) break;
    }

    llama_sampler_free(smpl);
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_jarvis_brain_LlamaJNI_nativeCancel(JNIEnv*, jobject, jlong handle) {
    auto* h = reinterpret_cast<LlamaHandle*>(handle);
    if (h) h->cancel.store(true);
}
