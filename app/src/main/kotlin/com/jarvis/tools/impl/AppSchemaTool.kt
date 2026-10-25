package com.jarvis.tools.impl

import com.jarvis.app.AppActionSchema
import com.jarvis.app.AppSchema
import com.jarvis.app.AppSchemaRegistry
import com.jarvis.capability.accessibility.JarvisAccessibilityService
import com.jarvis.tools.ParamSpec
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import com.jarvis.tools.ToolSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSchemaTool @Inject constructor(
    private val registry: AppSchemaRegistry
) : Tool {
    override val spec = ToolSpec(
        name = "inspect_app_ui",
        descriptionHi = "वर्तमान ऐप के UI controls और schema देखें",
        descriptionEn = "Inspect the currently visible app UI and build a dynamic app schema",
        parameters = mapOf(
            "max_nodes" to ParamSpec("int", false, "Maximum UI nodes to expose, 1..500", "250")
        ),
        rateLimitPerHour = 60,
        timeoutMs = 5_000
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val controller = JarvisAccessibilityService.controller()
        if (!controller.isConnected)
            return ToolResult.Err("JARVIS Accessibility Service is not enabled or connected.", "accessibility_unavailable")

        val snapshot = controller.snapshot(
            (args["max_nodes"] as? Number)?.toInt()?.coerceIn(1, 500) ?: 250
        ) ?: return ToolResult.Err(
            "The current app did not expose an accessibility UI tree.",
            "ui_unavailable"
        )

        val existing = registry.get(snapshot.packageName)
        val schema = existing ?: AppSchema(
            packageName = snapshot.packageName,
            label = snapshot.packageName.substringAfterLast('.'),
            actions = listOf(
                AppActionSchema("open", "Launch the application", "LOW"),
                AppActionSchema("inspect_ui", "Inspect visible accessibility controls", "MEDIUM"),
                AppActionSchema("click", "Activate a matching visible control", "MEDIUM"),
                AppActionSchema("set_text", "Enter text into a matching editable control", "HIGH"),
                AppActionSchema("scroll", "Scroll the visible application view", "LOW")
            )
        )
        registry.update(schema.copy(latestUi = snapshot))

        return ToolResult.Ok(
            mapOf(
                "package" to snapshot.packageName,
                "nodeCount" to snapshot.nodeCount,
                "nodes" to snapshot.nodes.map {
                    mapOf(
                        "path" to it.path,
                        "class" to it.className,
                        "text" to (it.text ?: ""),
                        "contentDescription" to (it.contentDescription ?: ""),
                        "viewId" to (it.viewIdResourceName ?: ""),
                        "clickable" to it.clickable,
                        "editable" to it.editable,
                        "scrollable" to it.scrollable,
                        "enabled" to it.enabled,
                        "visible" to it.visibleToUser
                    )
                }
            )
        )
    }
}
