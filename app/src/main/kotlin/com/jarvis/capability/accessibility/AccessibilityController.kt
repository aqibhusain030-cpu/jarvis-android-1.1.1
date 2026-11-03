package com.jarvis.capability.accessibility

interface AccessibilityController {
    val isConnected: Boolean
    fun performGlobalAction(action: GlobalAction): Boolean
    enum class GlobalAction { BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, POWER_DIALOG, LOCK_SCREEN, TAKE_SCREENSHOT }
}
