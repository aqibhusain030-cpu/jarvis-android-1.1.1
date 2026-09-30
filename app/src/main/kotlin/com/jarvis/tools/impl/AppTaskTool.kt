package com.jarvis.tools.impl

import com.jarvis.capability.accessibility.AccessibilityController
import com.jarvis.capability.accessibility.JarvisAccessibilityService
import com.jarvis.capability.accessibility.NodeSelector
import com.jarvis.tools.Confirmation
import com.jarvis.tools.ParamSpec
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import com.jarvis.tools.ToolSpec
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppTaskTool @Inject constructor() : Tool {
    override val spec = ToolSpec(
        name = "execute_app_task",
        descriptionHi = "ऐप के अंदर voice-planned task चलाएँ",
        descriptionEn = "Execute a bounded app task using accessibility semantics discovered at runtime",
        parameters = mapOf(
            "package" to ParamSpec("string", true, "Expected active Android package"),
            "steps" to ParamSpec("string", true, "JSON array of bounded steps"),
            "max_steps" to ParamSpec("int", false, "Maximum steps, 1..12", "8")
        ),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 20,
        timeoutMs = 30_000
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val expectedPackage = (args["package"] as? String)?.trim().orEmpty()
        if (expectedPackage.isBlank()) return ToolResult.Err("package required", "bad_arg")

        val stepsRaw = args["steps"] as? String
            ?: return ToolResult.Err("steps required", "bad_arg")
        val maxSteps = (args["max_steps"] as? Number)?.toInt()?.coerceIn(1, 12) ?: 8
        val steps = runCatching { Json.parseToJsonElement(stepsRaw).jsonArray }.getOrNull()
            ?: return ToolResult.Err("steps must be a JSON array", "bad_arg")
        if (steps.isEmpty()) return ToolResult.Err("steps cannot be empty", "bad_arg")
        if (steps.size > maxSteps) return ToolResult.Err("Task exceeds maximum step count.", "too_many_steps")

        val controller = JarvisAccessibilityService.controller()
        if (!controller.isConnected) return ToolResult.Err("Accessibility service unavailable.", "accessibility_unavailable")

        val executed = ArrayList<String>()
        for (index in steps.indices) {
            val obj = steps[index] as? JsonObject
                ?: return ToolResult.Err("Step " + (index + 1) + " is not an object.", "bad_step")
            val action = obj["action"]?.jsonPrimitive?.contentOrNull?.lowercase()
                ?: return ToolResult.Err("Step " + (index + 1) + " has no action.", "bad_step")

            if (controller.activePackage != expectedPackage) {
                return ToolResult.Err("Active app changed unexpectedly; task stopped.", "active_app_changed")
            }

            val ok = when (action) {
                "click" -> controller.click(selector(obj))
                "set_text" -> {
                    val value = obj["value"]?.jsonPrimitive?.contentOrNull
                        ?: return ToolResult.Err("Step " + (index + 1) + " set_text requires value.", "bad_step")
                    controller.setText(selector(obj), value)
                }
                "scroll" -> controller.scroll(
                    selector = null,
                    forward = obj["forward"]?.jsonPrimitive?.booleanOrNull ?: true
                )
                "back" -> controller.performGlobalAction(AccessibilityController.GlobalAction.BACK)
                "wait" -> {
                    val ms = (obj["ms"]?.jsonPrimitive?.intOrNull ?: 500).coerceIn(100, 3_000)
                    delay(ms.toLong())
                    true
                }
                else -> return ToolResult.Err("Unsupported app task action: " + action, "unsupported_step")
            }

            if (!ok) return ToolResult.Err("Step " + (index + 1) + " failed: " + action + ".", "step_failed")
            executed += action
            if (index + 1 < steps.size) delay(250)
        }

        return ToolResult.Ok(
            mapOf(
                "package" to expectedPackage,
                "steps" to executed,
                "count" to executed.size
            )
        )
    }

    private fun selector(obj: JsonObject): NodeSelector =
        NodeSelector(
            text = obj["text"]?.jsonPrimitive?.contentOrNull,
            contentDescription = obj["contentDescription"]?.jsonPrimitive?.contentOrNull,
            viewIdResourceName = obj["viewId"]?.jsonPrimitive?.contentOrNull,
            className = obj["class"]?.jsonPrimitive?.contentOrNull,
            occurrence = obj["occurrence"]?.jsonPrimitive?.intOrNull ?: 0,
            path = obj["path"]?.jsonPrimitive?.contentOrNull
        )
}
