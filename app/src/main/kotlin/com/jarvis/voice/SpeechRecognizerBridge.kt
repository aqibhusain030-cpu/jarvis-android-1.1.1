package com.jarvis.voice

import com.jarvis.brain.WhisperJNI
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.models.ModelManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognizerBridge @Inject constructor(
    private val whisper: WhisperJNI,
    private val config: ConfigRepository,
    private val logger: JarvisLogger,
    private val models: ModelManager
) {
    @Volatile private var ready = false

    private fun ensureReady(): Boolean {
        if (ready) return true
        val modelName = config.current.value.sttModel
        val asset = models.assetForFilename("stt", modelName)
            ?: run { logger.e(TAG, "No immutable STT asset for " + modelName); return false }
        val model = models.target(asset)
        val verification = models.verify(asset)
        if (!verification.verified) {
            logger.e(TAG, "STT asset rejected: " + verification.reason)
            return false
        }
        ready = whisper.init(model.absolutePath, config.current.value.sttThreads)
        return ready
    }

    fun transcribe(pcm: ByteArray): String {
        if (!ensureReady()) return ""
        val lang = when (config.current.value.sttLanguage) {
            "auto", "" -> "auto"
            else -> config.current.value.sttLanguage
        }
        return runCatching { whisper.transcribe(pcm, lang) }
            .onFailure { logger.e(TAG, "Whisper transcription failed", it) }
            .getOrDefault("")
    }

    fun close() {
        if (ready) {
            runCatching { whisper.close() }
            ready = false
        }
    }

    companion object { private const val TAG = "SpeechRecognizerBridge" }
}
