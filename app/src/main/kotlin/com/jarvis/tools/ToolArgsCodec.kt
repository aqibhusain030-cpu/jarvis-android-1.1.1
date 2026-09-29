package com.jarvis.tools

import kotlinx.serialization.json.*

object ToolArgsCodec {
    private val json = Json { ignoreUnknownKeys = false }

    fun elementToAny(element: JsonElement): Any? = when (element) {
        is JsonObject -> element.mapValues { elementToAny(it.value) }
        is JsonArray -> element.map { elementToAny(it) }
        JsonNull -> null
        is JsonPrimitive -> when {
            element.booleanOrNull != null -> element.booleanOrNull
            element.intOrNull != null -> element.intOrNull
            element.longOrNull != null -> element.longOrNull
            element.doubleOrNull != null && !element.isString -> element.doubleOrNull
            else -> element.content
        }
    }

    fun canonical(args: Map<String, Any?>): String =
        canonicalize(JsonObject(args.mapValues { anyToElement(it.value) }))

    private fun anyToElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map { anyToElement(it) })
        is Map<*, *> -> JsonObject(value.entries.associate { it.key.toString() to anyToElement(it.value) })
        else -> JsonPrimitive(value.toString())
    }

    private fun canonicalize(element: JsonElement): String = when (element) {
        is JsonObject -> element.entries.sortedBy { it.key }.joinToString("{", "}") {
            quote(it.key) + ":" + canonicalize(it.value)
        }
        is JsonArray -> element.joinToString("[", "]") { canonicalize(it) }
        JsonNull -> "null"
        is JsonPrimitive -> if (element.isString) quote(element.content) else element.toString()
    }

    private fun quote(value: String): String =
        Json.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(value))
}
