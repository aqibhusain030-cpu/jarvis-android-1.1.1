package com.jarvis.brain

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.models.ModelManager
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmEngine @Inject constructor(
    private val config: ConfigRepository,
    private val logger: JarvisLogger,
    private val models: ModelManager
) {
    private val handle = AtomicLong(0L)

    @Volatile var ready: Boolean = false
        private set

    @Synchronized
    fun loadIfNeeded(): Boolean {
        if (ready && handle.get() != 0L) return true

        val modelName = config.current.value.llmModel
        val asset = models.assetForFilename("llm", modelName)
            ?: run { logger.e(TAG, "No immutable LLM asset for " + modelName); return false }
        val path = models.target(asset)
        val verification = models.verify(asset)
        if (!verification.verified) {
            logger.e(TAG, "LLM asset rejected: " + verification.reason)
            return false
        }

        return try {
            val h = LlamaJNI.nativeInit(
                path.absolutePath,
                config.current.value.llmContextSize,
                config.current.value.llmThreads
            )
            if (h == 0L) {
                logger.e(TAG, "nativeInit returned 0 for " + path)
                return false
            }
            val old = handle.getAndSet(h)
            if (old != 0L) runCatching { LlamaJNI.nativeFree(old) }
            ready = true
            logger.i(TAG, "Verified LLM loaded: " + path.name)
            true
        } catch (t: Throwable) {
            logger.e(TAG, "LLM load failed", t)
            ready = false
            false
        }
    }

    @Suppress("unused")
    fun generate(prompt: String, max_tokens: Int = 700, temperature: Double = 0.2): String {
        val h = handle.get()
        if (h == 0L && !loadIfNeeded()) return ""
        val handleNow = handle.get()
        if (handleNow == 0L) return ""
        return runCatching {
            LlamaJNI.nativeGenerate(
                handleNow,
                prompt,
                max_tokens.coerceIn(1, 2048),
                temperature.toFloat().coerceIn(0.01f, 2f)
            )
        }.onFailure { logger.e(TAG, "generate failed", it) }.getOrDefault("")
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

    fun modelPath(): File? =
        models.assetForFilename("llm", config.current.value.llmModel)?.let(models::target)

    companion object { private const val TAG = "LocalLlmEngine" }
}
