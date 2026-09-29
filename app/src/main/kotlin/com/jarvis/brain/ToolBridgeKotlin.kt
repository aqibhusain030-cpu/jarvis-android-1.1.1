package com.jarvis.brain

import com.jarvis.logging.JarvisLogger
import com.jarvis.tools.ToolDispatcher
import com.jarvis.tools.ToolResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin side of the Python tool bridge.
 * All requests go through ToolDispatcher, including schema validation and
 * authorization. Nested JSON is preserved instead of being stringified.
 */
@Singleton
class ToolBridgeKotlin @Inject constructor(
    private val dispatcher: ToolDispatcher,
    private val logger: JarvisLogger
) {
    private val json = Json { ignoreUnknownKeys = false }

    fun invoke(tool: String, argsJson: String): String {
        return try {
            val args = parseArgs(argsJson)
            val result = runBlocking { dispatcher.invoke(tool, args) }
            resultToJson(result)
        } catch (t: Throwable) {
            logger.e(TAG, "bridge invoke failed for $tool", t)
            resultToJson(ToolResult.Err("Tool bridge rejected the request", "bridge_error"))
        }
    }

    private fun parseArgs(raw: String): Map<String, Any?> {
        if (raw.isBlank()) return emptyMap()
        val obj = json.parseToJsonElement(raw).jsonObject
        return obj.mapValues { (_, v) -> elementToAny(v) }
    }

    private fun elementToAny(element: kotlinx.serialization.json.JsonElement): Any? = when (element) {
        is JsonObject -> element.mapValues { (_, v) -> elementToAny(v) }
        is JsonArray -> element.map { elementToAny(it) }
        is JsonNull -> null
        is JsonPrimitive -> when {
            element.contentOrNull == null -> null
            element.booleanOrNull != null -> element.booleanOrNull
            element.intOrNull != null -> element.intOrNull
            element.longOrNull != null -> element.longOrNull
            element.doubleOrNull != null && !element.isString -> element.doubleOrNull
            else -> element.content
        }
        else -> element.toString()
    }

    private fun resultToJson(r: ToolResult): String = when (r) {
        is ToolResult.Ok -> json.encodeToString(
            kotlinx.serialization.json.buildJsonObject {
                put("ok", true)
                put("data", JsonPrimitive(r.data?.toString() ?: ""))
            }
        )
        is ToolResult.Err -> json.encodeToString(
            kotlinx.serialization.json.buildJsonObject {
                put("ok", false)
                put("error", r.message)
                put("code", r.code)
            }
        )
    }

    companion object {
        private const val TAG = "ToolBridgeKotlin"
    }
}
