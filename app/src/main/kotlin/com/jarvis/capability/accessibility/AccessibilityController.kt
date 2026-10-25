package com.jarvis.capability.accessibility

data class AccessibilityNodeSchema(
    val path: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val viewIdResourceName: String?,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
    val enabled: Boolean,
    val visibleToUser: Boolean,
    val childCount: Int
)

data class AccessibilityUiSnapshot(
    val packageName: String,
    val className: String?,
    val nodeCount: Int,
    val nodes: List<AccessibilityNodeSchema>
)

data class NodeSelector(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val className: String? = null,
    val occurrence: Int = 0,
    val path: String? = null
)

interface AccessibilityController {
    val isConnected: Boolean
    val activePackage: String?
    fun performGlobalAction(action: GlobalAction): Boolean
    fun snapshot(maxNodes: Int = 250): AccessibilityUiSnapshot?
    fun click(selector: NodeSelector): Boolean
    fun setText(selector: NodeSelector, value: String): Boolean
    fun scroll(selector: NodeSelector? = null, forward: Boolean = true): Boolean
    enum class GlobalAction { BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, POWER_DIALOG, LOCK_SCREEN, TAKE_SCREENSHOT }
}
