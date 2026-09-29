package com.jarvis.brain

import javax.inject.Inject
import javax.inject.Singleton

/** JNI bindings to whisper.cpp. One synchronized handle per loaded model. */
@Singleton
class WhisperJNI @Inject constructor() {
    private var handle: Long = 0L

    init { System.loadLibrary("jarvis_native") }

    @Synchronized
    fun init(modelPath: String, threads: Int): Boolean {
        if (handle != 0L) return true
        handle = nativeInit(modelPath, threads.coerceIn(1, 16))
        return handle != 0L
    }

    @Synchronized
    fun transcribe(pcm16le: ByteArray, language: String): String {
        if (handle == 0L) return ""
        return nativeTranscribe(handle, pcm16le, language)
    }

    @Synchronized
    fun close() {
        if (handle != 0L) {
            nativeFree(handle)
            handle = 0L
        }
    }

    private external fun nativeInit(modelPath: String, threads: Int): Long
    private external fun nativeFree(handle: Long)
    private external fun nativeTranscribe(handle: Long, pcm: ByteArray, language: String): String
}
