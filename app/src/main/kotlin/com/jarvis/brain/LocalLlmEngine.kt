package com.jarvis.brain

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.models.ModelChecksum
import com.jarvis.models.ModelManager
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production on-device LLM façade over [LlamaJNI].
 * Exposed to Python as an object with a `generate(prompt, max_tokens, temperature)` method
 * via Chaquopy (Python calls the Kotlin instance methods by name).
 */
@Singleton
class LocalLlmEngine @Inject constructor(
    private val config: ConfigRepository,
    private val logger: JarvisLogger,
    private val models: ModelManager
) {
    private val handle = AtomicLong(0L)

    @Volatile
    var ready: Boolean = false
        private set

    @Synchronized
    fun loadIfNeeded(): Boolean {
        if (ready && handle.get() != 0L) return true
        val modelName = config.current.value.llmModel
        val entry = models.all().firstOrNull { it.kind == "llm" && it.filename == modelName }
            ?: run { logger.e(TAG, "No locked model manifest entry for $modelName"); return false }
        val path = config.modelFile("llm", modelName)
        if (!path.exists() || path.length() < 1_000_000L) {
            logger.w(TAG, "LLM model missing or too small: $path")
            return false
        }
        if (!ModelChecksum.verify(path, entry.sha256)) {
            logger.e(TAG, "LLM checksum verification failed: ${path.name}")
            return false
        }
        return try {
            val h = LlamaJNI.nativeInit(
                path.absolutePath,
                config.current.value.llmContextSize,
                config.current.value.llmThreads
            )
            if (h == 0L) {
                logger.e(TAG, "nativeInit returned 0 for $path")
                return false
            }
            val old = handle.getAndSet(h)
            if (old != 0L) runCatching { LlamaJNI.nativeFree(old) }
            ready = true
            logger.i(TAG, "LLM loaded: ${path.name} ctx=${config.current.value.llmContextSize}")
            true
        } catch (t: Throwable) {
            logger.e(TAG, "LLM load failed", t)
            ready = false
            false
        }
    }

    /** Called from Python: `_llm.generate(prompt, max_tokens=..., temperature=...)`. */
    @Suppress("unused")
    fun generate(prompt: String, max_tokens: Int = 700, temperature: Double = 0.2): String {
        val h = handle.get()
        if (h == 0L) {
            if (!loadIfNeeded()) return ""
        }
        val handleNow = handle.get()
        if (handleNow == 0L) return ""
        return try {
            LlamaJNI.nativeGenerate(
                handleNow,
                prompt,
                max_tokens.coerceIn(1, 2048),
                temperature.toFloat().coerceIn(0.01f, 2f)
            )
        } catch (t: Throwable) {
            logger.e(TAG, "generate failed", t)
            ""
        }
    }

    fun cancel() {
        val h = handle.get()
        if (h != 0L) runCatching { LlamaJNI.nativeCancel(h) }
    }

    @Synchronized
    fun unload() {
        val h = handle.getAndSet(0L)
        if (h != 0L) runCatching { LlamaJNI.nativeFree(h) }
        ready = false
    }

    fun modelPath(): File = config.modelFile("llm", config.current.value.llmModel)

    companion object {
        private const val TAG = "LocalLlmEngine"
    }
}
