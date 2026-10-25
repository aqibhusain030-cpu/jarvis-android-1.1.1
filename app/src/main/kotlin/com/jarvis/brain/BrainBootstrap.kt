package com.jarvis.brain

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.models.ModelManager
import com.jarvis.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot production bootstrap:
 * 1. Ensure Python is running
 * 2. Load local LLM into [LocalLlmEngine] and pass it to Python `set_llm`
 * 3. Install [ToolBridgeKotlin] as Python `tools_bridge.set_dispatcher`
 * 4. Publish tool specs so the LLM can emit structured tool calls
 */
@Singleton
class BrainBootstrap @Inject constructor(
    private val python: PythonRuntime,
    private val llm: HybridLlmEngine,
    private val toolBridge: ToolBridgeKotlin,
    private val registry: ToolRegistry,
    private val models: ModelManager,
    private val config: ConfigRepository,
    private val logger: JarvisLogger
) {
    @Volatile
    var bootstrapped: Boolean = false
        private set

    @Volatile
    var llmReady: Boolean = false
        private set

    suspend fun bootstrap(): Boolean = withContext(Dispatchers.IO) {
        if (bootstrapped) return@withContext llmReady
        try {
            python.startIfNeeded()

            // Wire tool dispatcher into Python first (works even without LLM).
            python.call("install_tool_dispatcher", toolBridge)
            publishToolCatalog()

            // Load LLM if model file is present and verified.
            llmReady = if (config.current.value.remoteInferenceEnabled) true else llm.loadLocal()
            if (llmReady) {
                python.call("set_llm", llm)
                logger.i(TAG, "LLM wired into Python brain")
            } else {
                logger.w(TAG, "LLM not loaded — brain will use fallback replies")
            }

            bootstrapped = true
            true
        } catch (t: Throwable) {
            logger.e(TAG, "bootstrap failed", t)
            false
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
                        put("parameters", Json.encodeToJsonElement(tool.spec.parameters))
                        put("permissions", Json.encodeToJsonElement(tool.spec.permissions))
                        put("timeout_ms", tool.spec.timeoutMs)
                    }
                )
            }
        }
        val catalog = arr.toString()
        python.call("set_tool_catalog", catalog)
        logger.i(TAG, "Published ${registry.all().size} tool specs to Python")
    }

    /** Best-effort model presence report for UI/onboarding. */
    fun modelStatus(): Map<String, Boolean> {
        val out = mutableMapOf<String, Boolean>()
        for (e in models.all()) {
            out[e.id] = models.isPresent(e)
        }
        out["llm_engine"] = llm.localReady
        return out
    }

    companion object {
        private const val TAG = "BrainBootstrap"
    }
}
