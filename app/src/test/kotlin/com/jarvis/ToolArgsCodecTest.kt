package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.tools.ToolArgsCodec
import kotlinx.serialization.json.Json
import org.junit.Test

class ToolArgsCodecTest {
    @Test fun nestedValuesAreConverted() {
        val value = ToolArgsCodec.elementToAny(
            Json.parseToJsonElement("""{"name":"jarvis","count":2,"enabled":true,"items":["a","b"]}""")
        )
        assertThat(value).isInstanceOf(Map::class.java)
        val map = value as Map<*, *>
        assertThat(map["name"]).isEqualTo("jarvis")
        assertThat(map["count"]).isEqualTo(2)
        assertThat(map["enabled"]).isEqualTo(true)
    }

    @Test fun canonicalizationIsDeterministicForNestedObjects() {
        val one = ToolArgsCodec.canonical(mapOf("outer" to mapOf("b" to 2, "a" to 1)))
        val two = ToolArgsCodec.canonical(mapOf("outer" to mapOf("a" to 1, "b" to 2)))
        assertThat(one).isEqualTo(two)
    }
}
