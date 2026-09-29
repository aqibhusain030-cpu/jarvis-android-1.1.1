package com.jarvis.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class Confirmation { NONE, VOICE, PIN, NOTIFICATION }

@Serializable
data class ToolSpec(
    val name: String,
    val descriptionHi: String,
    val descriptionEn: String,
    val parameters: Map<String, ParamSpec> = emptyMap(),
    val permissions: List<String> = emptyList(),
    val confirmation: Confirmation = Confirmation.NONE,
    val rateLimitPerHour: Int = 0,
    val audit: Boolean = true,
    val timeoutMs: Long = 15_000,
    val requiresUnlock: Boolean = false,
    val foregroundOnly: Boolean = false
)

@Serializable
data class ParamSpec(
    val type: String,
    val required: Boolean = true,
    val description: String = "",
    val default: String? = null
)

sealed class ToolResult {
    data class Ok(val data: Any? = null) : ToolResult()
    data class Err(val message: String, val code: String) : ToolResult()

    fun toJson(): String = when (this) {
        is Ok -> Json.encodeToString(
            buildJsonObject {
                put("ok", true)
                put("data", JsonPrimitive(data?.toString() ?: "null"))
            }
        )
        is Err -> Json.encodeToString(
            buildJsonObject {
                put("ok", false)
                put("error", message)
                put("code", code)
            }
        )
    }
}

interface Tool {
    val spec: ToolSpec
    suspend fun invoke(args: Map<String, Any?>): ToolResult
}
