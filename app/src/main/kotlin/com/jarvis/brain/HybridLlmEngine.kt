package com.jarvis.brain

import com.jarvis.logging.JarvisLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes generation between the remote 2T tier and verified local inference.
 * Remote failure always falls back to the local model when available.
 */
@Singleton
class HybridLlmEngine @Inject constructor(
    private val local: LocalLlmEngine,
    private val remote: RemoteLlmEngine,
    private val logger: JarvisLogger
) {
    fun generate(
        prompt: String,
        maxTokens: Int = 700,
        temperature: Double = 0.2
    ): String {
        // The Python bridge remains synchronous, so remote generation is exposed
        // through the blocking coroutine boundary in this adapter only.
        val remoteResult = kotlinx.coroutines.runBlocking {
            remote.generate(prompt, maxTokens, temperature)
        }
        if (remoteResult.isNotBlank()) return remoteResult

        return local.generate(prompt, maxTokens, temperature).also {
            if (it.isBlank()) logger.w(TAG, "Both remote and local inference returned no output")
        }
    }

    fun loadLocal(): Boolean = local.loadIfNeeded()

    fun unloadLocal() = local.unload()

    val localReady: Boolean get() = local.ready

    companion object { private const val TAG = "HybridLlmEngine" }
}
