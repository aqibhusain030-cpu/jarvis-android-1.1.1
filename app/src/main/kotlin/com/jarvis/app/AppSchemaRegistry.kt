package com.jarvis.app

import com.jarvis.capability.accessibility.AccessibilityUiSnapshot
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class AppActionSchema(
    val action: String,
    val description: String,
    val risk: String
)

data class AppSchema(
    val packageName: String,
    val label: String,
    val actions: List<AppActionSchema>,
    val latestUi: AccessibilityUiSnapshot? = null
)

@Singleton
class AppSchemaRegistry @Inject constructor() {
    private val schemas = ConcurrentHashMap<String, AppSchema>()

    fun update(schema: AppSchema) {
        schemas[schema.packageName] = schema
    }

    fun get(packageName: String): AppSchema? = schemas[packageName]

    fun all(): List<AppSchema> = schemas.values.sortedBy { it.label.lowercase() }

    fun generic(packageName: String, label: String): AppSchema =
        AppSchema(
            packageName,
            label,
            listOf(
                AppActionSchema("open", "Launch the application", "LOW"),
                AppActionSchema("inspect_ui", "Inspect exposed accessibility controls", "MEDIUM"),
                AppActionSchema("click", "Activate a matching visible control", "MEDIUM"),
                AppActionSchema("set_text", "Enter text into a matching editable control", "HIGH"),
                AppActionSchema("scroll", "Scroll the visible application view", "LOW")
            )
        )
}
