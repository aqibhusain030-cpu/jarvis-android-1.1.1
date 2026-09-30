package com.jarvis.capability.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        HOLDER.set(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        HOLDER.compareAndSet(this, null)
    }

    override fun onDestroy() {
        HOLDER.compareAndSet(this, null)
        super.onDestroy()
    }

    companion object {
        private val HOLDER = AtomicReference<JarvisAccessibilityService?>(null)

        fun controller(): AccessibilityController = object : AccessibilityController {
            override val isConnected: Boolean get() = HOLDER.get() != null

            override fun performGlobalAction(
                action: AccessibilityController.GlobalAction
            ): Boolean {
                val service = HOLDER.get() ?: return false
                val androidAction = when (action) {
                    AccessibilityController.GlobalAction.BACK -> GLOBAL_ACTION_BACK
                    AccessibilityController.GlobalAction.HOME -> GLOBAL_ACTION_HOME
                    AccessibilityController.GlobalAction.RECENTS -> GLOBAL_ACTION_RECENTS
                    AccessibilityController.GlobalAction.NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
                    AccessibilityController.GlobalAction.QUICK_SETTINGS -> GLOBAL_ACTION_QUICK_SETTINGS
                    AccessibilityController.GlobalAction.POWER_DIALOG -> GLOBAL_ACTION_POWER_DIALOG
                    AccessibilityController.GlobalAction.LOCK_SCREEN ->
                        if (android.os.Build.VERSION.SDK_INT >= 28) GLOBAL_ACTION_LOCK_SCREEN else return false
                    AccessibilityController.GlobalAction.TAKE_SCREENSHOT ->
                        if (android.os.Build.VERSION.SDK_INT >= 28) GLOBAL_ACTION_TAKE_SCREENSHOT else return false
                }
                return service.performGlobalAction(androidAction)
            }
        }
    }
}
