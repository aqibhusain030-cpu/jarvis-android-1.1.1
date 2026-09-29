package com.jarvis.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        service.set(this)
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        service.compareAndSet(this, null)
        super.onDestroy()
    }

    fun snapshot(maxNodes: Int = 120): List<NodeSnapshot> {
        val root = rootInActiveWindow ?: return emptyList()
        val out = ArrayList<NodeSnapshot>(minOf(maxNodes, 32))
        walk(root, out, maxNodes)
        return out
    }

    fun click(text: String?, resourceId: String?): Boolean =
        find(text, resourceId)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun type(text: String?, resourceId: String?, value: String): Boolean {
        val node = find(text, resourceId) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = when (direction.lowercase()) {
            "up", "backward" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "down", "forward" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> return false
        }
        return root.performAction(action) || findScrollable(root, action)
    }

    fun global(action: String): Boolean = when (action.lowercase()) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        else -> false
    }

    private fun find(text: String?, resourceId: String?): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        if (!resourceId.isNullOrBlank()) {
            root.findAccessibilityNodeInfosByViewId(resourceId).firstOrNull()?.let { return it }
        }
        if (!text.isNullOrBlank()) {
            root.findAccessibilityNodeInfosByText(text).firstOrNull()?.let { return it }
        }
        return null
    }

    private fun findScrollable(root: AccessibilityNodeInfo, action: Int): Boolean {
        if (root.isScrollable && root.performAction(action)) return true
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            if (findScrollable(child, action)) return true
        }
        return false
    }

    private fun walk(node: AccessibilityNodeInfo, out: MutableList<NodeSnapshot>, max: Int) {
        if (out.size >= max) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        out += NodeSnapshot(
            text = node.text?.toString().orEmpty(),
            contentDescription = node.contentDescription?.toString().orEmpty(),
            resourceId = node.viewIdResourceName.orEmpty(),
            className = node.className?.toString().orEmpty(),
            clickable = node.isClickable,
            editable = node.isEditable,
            scrollable = node.isScrollable,
            bounds = bounds.toShortString()
        )
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { walk(it, out, max) }
            if (out.size >= max) return
        }
    }

    data class NodeSnapshot(
        val text: String,
        val contentDescription: String,
        val resourceId: String,
        val className: String,
        val clickable: Boolean,
        val editable: Boolean,
        val scrollable: Boolean,
        val bounds: String
    )

    companion object {
        private val service = AtomicReference<JarvisAccessibilityService?>()
        fun current(): JarvisAccessibilityService? = service.get()
    }
}
