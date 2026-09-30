package com.jarvis.capability.accessibility

import android.os.Build
import com.jarvis.core.capability.*

class AccessibilityBackend(private val controller: AccessibilityController) : CapabilityBackend {
    override val backendId = "android.accessibility"
    override val capabilities = setOf(
        Capability("accessibility.back", "Press Android Back", requiresAccessibility = true),
        Capability("accessibility.home", "Go to Android Home", requiresAccessibility = true),
        Capability("accessibility.recents", "Open Android Recents", requiresAccessibility = true),
        Capability("accessibility.notifications", "Open notification shade", requiresAccessibility = true),
        Capability("accessibility.quick_settings", "Open Quick Settings", requiresAccessibility = true),
        Capability("accessibility.power_dialog", "Open Android power dialog", Capability.RiskLevel.HIGH, requiresAccessibility = true),
        Capability("accessibility.lock_screen", "Lock the Android device", Capability.RiskLevel.HIGH, requiresAccessibility = true),
        Capability("accessibility.take_screenshot", "Take a screenshot", requiresAccessibility = true)
    )

    override suspend fun execute(r: CapabilityRequest): CapabilityResult {
        if (!controller.isConnected)
            return CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.MISSING_PERMISSION, "JARVIS Accessibility Service is not connected.")

        val action = when (r.capabilityId) {
            "accessibility.back" -> AccessibilityController.GlobalAction.BACK
            "accessibility.home" -> AccessibilityController.GlobalAction.HOME
            "accessibility.recents" -> AccessibilityController.GlobalAction.RECENTS
            "accessibility.notifications" -> AccessibilityController.GlobalAction.NOTIFICATIONS
            "accessibility.quick_settings" -> AccessibilityController.GlobalAction.QUICK_SETTINGS
            "accessibility.power_dialog" -> AccessibilityController.GlobalAction.POWER_DIALOG
            "accessibility.lock_screen" -> AccessibilityController.GlobalAction.LOCK_SCREEN
            "accessibility.take_screenshot" -> AccessibilityController.GlobalAction.TAKE_SCREENSHOT
            else -> return CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Unsupported accessibility action.")
        }

        if (action == AccessibilityController.GlobalAction.TAKE_SCREENSHOT && Build.VERSION.SDK_INT < 28)
            return CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.UNSUPPORTED, "Screenshot requires Android 9 or later.")

        return if (controller.performGlobalAction(action))
            CapabilityResult.Success(r.requestId, "Android action executed.")
        else
            CapabilityResult.Failed(r.requestId, CapabilityResult.FailureCode.EXECUTION_FAILED, "Android did not execute the requested action.")
    }
}
