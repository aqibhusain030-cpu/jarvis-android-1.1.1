package com.jarvis.brain

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.models.ModelManager
import com.jarvis.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production bootstrap for the Python brain, tool bridge, and local LLM.
 *
 * Python/tool setup is initialized once. LLM loading is deliberately retried:
 * model files may be downloaded after the service has started, and a verified
 * file is not the same thing as a model already loaded into native memory.
 */
@Singleton
class BrainBootstrap @Inject constructor(
    private val python: PythonRuntime,
    private val llm: LocalLlmEngine,
    private val toolBridge: ToolBridgeKotlin,
    private val registry: ToolRegistry,
    private val models: ModelManager,
    private val config: ConfigRepository,
    private val logger: JarvisLogger
) {
    private val bootstrapMutex = Mutex()

    @Volatile
    var bootstrapped: Boolean = false
        private set

    @Volatile
    var llmReady: Boolean = false
        private set

    @Volatile
    private var llmWired: Boolean = false

    /**
     * Returns true when the base runtime was initialized. LLM availability is
     * reported separately by [llmReady], since tool-only fallback mode is valid.
     */
    suspend fun bootstrap(): Boolean = withContext(Dispatchers.IO) {
        bootstrapMutex.withLock {
            try {
                python.startIfNeeded()

                if (!bootstrapped) {
                    // Tool execution remains available even when the local LLM
                    // file is missing, invalid, or temporarily fails to load.
                    python.call("install_tool_dispatcher", toolBridge)
                    publishToolCatalog()
                    bootstrapped = true
                }

                // Do not permanently cache a failed first load. The model may
                // have been downloaded after the first service start, or the
                // first native initialization may have failed transiently.
                llmReady = if (llm.ready) {
                    true
                } else {
                    llm.loadIfNeeded()
                }

                if (llmReady && !llmWired) {
                    python.call("set_llm", llm)
                    // Mark wired only after the Python call has returned.
                    llmWired = true
                    logger.i(TAG, "LLM loaded and wired into Python brain")
                } else if (!llmReady) {
                    logger.w(TAG, "LLM is not ready; model loading will be retried on the next bootstrap")
                }

                true
            } catch (t: Throwable) {
                llmReady = llm.ready && llmWired
                logger.e(TAG, "bootstrap failed", t)
                false
            }
        }
    }

    private fun publishToolCatalog() {
        val arr = buildJsonArray {
            for (tool in registry.all()) {
                add(
                    buildJsonObject {
                        put("name", tool.spec.name)
                        put("description_en", tool.spec.descriptionEn)
                        put("description_hi", tool.spec.descriptionHi)
                        put("confirmation", tool.spec.confirmation.name)
                    }
                )
            }
        }
        val catalog = arr.toString()
        python.call("set_tool_catalog", catalog)
        logger.i(TAG, "Published ${registry.all().size} tool specs to Python")
    }

    /** File verification and runtime readiness are intentionally separate states. */
    fun modelStatus(): Map<String, Boolean> {
        val out = mutableMapOf<String, Boolean>()
        for (e in models.all()) {
            out[e.id] = models.isPresent(e)
        }
        out["llm_engine"] = llm.ready
        out["llm_wired"] = llmWired
        out["llm_ready"] = llm.ready && llmWired
        return out
    }

    companion object {
        private const val TAG = "BrainBootstrap"
    }
}
