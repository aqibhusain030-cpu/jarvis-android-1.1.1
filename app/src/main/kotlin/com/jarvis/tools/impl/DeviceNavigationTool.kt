package com.jarvis.tools.impl

import com.jarvis.capability.accessibility.AccessibilityController
import com.jarvis.capability.accessibility.JarvisAccessibilityService
import com.jarvis.tools.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceNavigationTool @Inject constructor() : Tool {
    override val spec = ToolSpec(
        name = "device_navigation",
        descriptionHi = "Android का Back, Home, Recents, notifications या Quick Settings नियंत्रित करें",
        descriptionEn = "Perform bounded Android navigation actions through the user-enabled JARVIS Accessibility Service",
        parameters = mapOf(
            "action" to ParamSpec(
                "string",
                required = true,
                description = "back, home, recents, notifications, quick_settings"
            )
        ),
        rateLimitPerHour = 120,
        timeoutMs = 5_000
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val value = (args["action"] as? String)?.trim()?.lowercase()
            ?: return ToolResult.Err("action required", "bad_arg")

        val action = when (value) {
            "back" -> AccessibilityController.GlobalAction.BACK
            "home" -> AccessibilityController.GlobalAction.HOME
            "recents", "recent_apps" -> AccessibilityController.GlobalAction.RECENTS
            "notifications", "notification_shade" -> AccessibilityController.GlobalAction.NOTIFICATIONS
            "quick_settings" -> AccessibilityController.GlobalAction.QUICK_SETTINGS
            else -> return ToolResult.Err("Unsupported navigation action: $value", "unsupported_action")
        }

        val controller = JarvisAccessibilityService.controller()
        if (!controller.isConnected) {
            return ToolResult.Err(
                "JARVIS Accessibility Service is not enabled or connected.",
                "accessibility_unavailable"
            )
        }

        return if (controller.performGlobalAction(action)) {
            ToolResult.Ok("Executed $value.")
        } else {
            ToolResult.Err("Android rejected the requested navigation action.", "action_failed")
        }
    }
}
