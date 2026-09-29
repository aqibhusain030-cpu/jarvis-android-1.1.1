package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.tools.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ToolResultJsonTest {
    @Test
    fun nestedPayloadRemainsJsonObject() {
        val result = ToolResult.Ok(
            mapOf(
                "status" to "ok",
                "items" to listOf(mapOf("id" to 7, "enabled" to true))
            )
        )

        val root = Json.parseToJsonElement(result.toJson()).jsonObject
        val data = root["data"]!!.jsonObject
        val item = data["items"]!!.let { it as kotlinx.serialization.json.JsonArray }[0].jsonObject

        assertThat(root["ok"]!!.jsonPrimitive.content).isEqualTo("true")
        assertThat(data["status"]!!.jsonPrimitive.content).isEqualTo("ok")
        assertThat(item["id"]!!.jsonPrimitive.int).isEqualTo(7)
        assertThat(item["enabled"]!!.jsonPrimitive.boolean).isTrue()
    }

    @Test
    fun nullPayloadRemainsJsonNull() {
        val root = Json.parseToJsonElement(ToolResult.Ok(null).toJson()).jsonObject
        assertThat(root["data"]).isNotNull()
        assertThat(root["data"] is kotlinx.serialization.json.JsonNull).isTrue()
    }
}
