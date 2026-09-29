package com.jarvis.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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

/**
 * Convert tool payloads to real JSON instead of stringifying nested maps/lists.
 * A depth cap keeps a malformed or cyclic object graph from consuming the VM.
 */
internal fun Any?.toJsonElement(depth: Int = 0): JsonElement {
    if (depth > 12) return JsonPrimitive("[max-depth]")
    return when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Map<*, *> -> buildJsonObject {
            for ((key, value) in this@toJsonElement) {
                put(key?.toString() ?: "null", value.toJsonElement(depth + 1))
            }
        }
        is Iterable<*> -> buildJsonArray {
            for (value in this@toJsonElement) add(value.toJsonElement(depth + 1))
        }
        is Array<*> -> buildJsonArray {
            for (value in this@toJsonElement) add(value.toJsonElement(depth + 1))
        }
        is BooleanArray -> buildJsonArray {
            for (value in this@toJsonElement) add(JsonPrimitive(value))
        }
        is ByteArray -> buildJsonArray {
            for (value in this@toJsonElement) add(JsonPrimitive(value.toInt()))
        }
        is ShortArray -> buildJsonArray {
            for (value in this@toJsonElement) add(JsonPrimitive(value.toInt()))
        }
        is IntArray -> buildJsonArray {
            for (value in this@toJsonElement) add(JsonPrimitive(value))
        }
        is LongArray -> buildJsonArray {
            for (value in this@toJsonElement) add(JsonPrimitive(value))
        }
        is FloatArray -> buildJsonArray {
            for (value in this@toJsonElement) {
                if (value.isFinite()) add(JsonPrimitive(value)) else add(JsonPrimitive(value.toString()))
            }
        }
        is DoubleArray -> buildJsonArray {
            for (value in this@toJsonElement) {
                if (value.isFinite()) add(JsonPrimitive(value)) else add(JsonPrimitive(value.toString()))
            }
        }
        is Boolean -> JsonPrimitive(this)
        is Byte, is Short, is Int, is Long -> JsonPrimitive(this as Number)
        is Float -> if (isFinite()) JsonPrimitive(this) else JsonPrimitive(toString())
        is Double -> if (isFinite()) JsonPrimitive(this) else JsonPrimitive(toString())
        is String -> JsonPrimitive(this)
        is Char -> JsonPrimitive(toString())
        else -> JsonPrimitive(toString())
    }
}

sealed class ToolResult {
    data class Ok(val data: Any? = null) : ToolResult()
    data class Err(val message: String, val code: String) : ToolResult()

    fun toJson(): String = when (this) {
        is Ok -> buildJsonObject {
            put("ok", JsonPrimitive(true))
            put("data", data.toJsonElement())
        }.toString()
        is Err -> buildJsonObject {
            put("ok", JsonPrimitive(false))
            put("error", JsonPrimitive(message))
            put("code", JsonPrimitive(code))
        }.toString()
    }
}

interface Tool {
    val spec: ToolSpec
    suspend fun invoke(args: Map<String, Any?>): ToolResult
}