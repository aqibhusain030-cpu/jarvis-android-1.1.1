package com.jarvis.brain

/** Small, defensive JNI surface for the bundled llama.cpp build. */
object LlamaJNI {
    init { System.loadLibrary("jarvis_native") }

    external fun nativeInit(modelPath: String, contextSize: Int, threads: Int): Long
    external fun nativeGenerate(ctx: Long, prompt: String, maxTokens: Int, temperature: Float): String
    external fun nativeFree(ctx: Long)
    external fun nativeCancel(ctx: Long)
}
