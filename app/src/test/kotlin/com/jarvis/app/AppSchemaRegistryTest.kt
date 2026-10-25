package com.jarvis.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppSchemaRegistryTest {
    @Test
    fun creates_generic_schema_without_vendor_hardcoding() {
        val registry = AppSchemaRegistry()
        val schema = registry.generic("com.example.app", "Example")
        assertThat(schema.packageName).isEqualTo("com.example.app")
        assertThat(schema.actions.map { it.action })
            .containsExactly("open", "inspect_ui", "click", "set_text", "scroll")
            .inOrder()
    }
}
