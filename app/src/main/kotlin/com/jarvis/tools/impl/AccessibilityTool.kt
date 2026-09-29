package com.jarvis.tools.impl

import com.jarvis.accessibility.JarvisAccessibilityService
import com.jarvis.tools.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityTool @Inject constructor() : Tool {
    override val spec = ToolSpec(
        name = "accessibility_action",
        descriptionHi = "ऐप के UI को नियंत्रित करें",
        descriptionEn = "Inspect and control the foreground app UI through Android Accessibility",
        parameters = mapOf(
            "action" to ParamSpec("string", true, "inspect|click|type|scroll|back|home|recents|notifications"),
            "text" to ParamSpec("string", false, "Visible text or content description"),
            "resourceId" to ParamSpec("string", false, "Android view resource id"),
            "value" to ParamSpec("string", false, "Text to enter"),
            "direction" to ParamSpec("string", false, "up|down")
        ),
        rateLimitPerHour = 300
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val service = JarvisAccessibilityService.current()
            ?: return ToolResult.Err("Accessibility service is not enabled", "accessibility_disabled")
        val action = (args["action"] as? String)?.trim()?.lowercase()
            ?: return ToolResult.Err("action required", "bad_arg")
        return when (action) {
            "inspect" -> {
                val nodes = service.snapshot()
                val json = buildJsonArray {
                    nodes.forEach { n ->
                        add(buildJsonObject {
                            put("text", n.text)
                            put("contentDescription", n.contentDescription)
                            put("resourceId", n.resourceId)
                            put("className", n.className)
                            put("clickable", n.clickable)
                            put("editable", n.editable)
                            put("scrollable", n.scrollable)
                            put("bounds", n.bounds)
                        })
                    }
                }
                ToolResult.Ok(Json.encodeToString(json))
            }
            "click" -> fromBoolean(
                service.click(args["text"] as? String, args["resourceId"] as? String),
                "clicked",
                "click_failed"
            )
            "type" -> {
                val value = args["value"] as? String
                    ?: return ToolResult.Err("value required", "bad_arg")
                ToolResult.fromBoolean(
                    service.type(args["text"] as? String, args["resourceId"] as? String, value),
                    "typed",
                    "type_failed"
                )
            }
            "scroll" -> ToolResult.fromBoolean(
                service.scroll(args["direction"] as? String ?: "down"),
                "scrolled",
                "scroll_failed"
            )
            "back", "home", "recents", "notifications" -> ToolResult.fromBoolean(
                service.global(action), "actioned", "global_action_failed"
            )
            else -> ToolResult.Err("Unsupported accessibility action", "bad_arg")
        }
    }

    private fun fromBoolean(ok: Boolean, success: String, failure: String): ToolResult =
        if (ok) ToolResult.Ok(success) else ToolResult.Err(failure, failure)
}
