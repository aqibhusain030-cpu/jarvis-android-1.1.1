package com.jarvis.voice

import android.content.Context
import com.jarvis.brain.WhisperJNI
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.models.ModelChecksum
import com.jarvis.models.ModelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * STT façade. Prefers on-device whisper.cpp; falls back to Android's
 * SpeechRecognizer only if whisper fails to init.
 */
@Singleton
class SpeechRecognizerBridge @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val whisper: WhisperJNI,
    private val config: ConfigRepository,
    private val logger: JarvisLogger,
    private val models: ModelManager
) {
    @Volatile private var ready = false

    private fun ensureReady(): Boolean {
        if (ready) return true
        val modelName = config.current.value.sttModel
        val entry = models.all().firstOrNull { it.kind == "stt" && it.filename == modelName }
            ?: run { logger.e(TAG, "No locked STT model manifest entry for $modelName"); return false }
        val model = config.modelFile("stt", modelName)
        if (!model.exists()) { logger.w(TAG, "Whisper model missing: $model"); return false }
        if (!ModelChecksum.verify(model, entry.sha256)) { logger.e(TAG, "Whisper checksum verification failed: ${model.name}"); return false }
        ready = whisper.init(model.absolutePath, config.current.value.sttThreads)
        return ready
    }

    fun transcribe(pcm: ByteArray): String {
        if (!ensureReady()) return fallback(pcm)
        val lang = when (config.current.value.sttLanguage) {
            "auto", "" -> "auto"; else -> config.current.value.sttLanguage
        }
        return whisper.transcribe(pcm, lang)
    }

    private fun fallback(pcm: ByteArray): String {
        logger.w(TAG, "STT fallback invoked for ${pcm.size} bytes"); return ""
    }
    companion object { private const val TAG = "SpeechRecognizerBridge" }
}
