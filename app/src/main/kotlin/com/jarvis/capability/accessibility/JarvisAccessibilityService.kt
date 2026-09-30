package com.jarvis.capability.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

        fun controller(): AccessibilityController = Controller

        private object Controller : AccessibilityController {
            override val isConnected: Boolean get() = HOLDER.get() != null

            override val activePackage: String?
                get() = HOLDER.get()?.rootInActiveWindow?.packageName?.toString()

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
                        if (Build.VERSION.SDK_INT >= 28) GLOBAL_ACTION_LOCK_SCREEN else return false
                    AccessibilityController.GlobalAction.TAKE_SCREENSHOT ->
                        if (Build.VERSION.SDK_INT >= 30) GLOBAL_ACTION_TAKE_SCREENSHOT else return false
                }
                return service.performGlobalAction(androidAction)
            }

            override fun snapshot(maxNodes: Int): AccessibilityUiSnapshot? {
                val service = HOLDER.get() ?: return null
                val root = service.rootInActiveWindow ?: return null
                val packageName = root.packageName?.toString() ?: return null
                val nodes = ArrayList<AccessibilityNodeSchema>(minOf(maxNodes, 500))
                collect(root, "", nodes, maxNodes.coerceIn(1, 500))
                return AccessibilityUiSnapshot(
                    packageName = packageName,
                    className = root.className?.toString(),
                    nodeCount = nodes.size,
                    nodes = nodes
                )
            }

            override fun click(selector: NodeSelector): Boolean =
               withNode(selector) { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }

            override fun setText(selector: NodeSelector, value: String): Boolean {
                if (value.length > 8192) return false
                return withNode(selector) { node ->
                    if (!node.isEditable && node.actionList.none {
                            it.id == AccessibilityNodeInfo.ACTION_SET_TEXT
                        }) {
                        false
                    } else {
                        val args = Bundle().apply {
                            putCharSequence(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                value
                            )
                        }
                        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    }
                }
            }

            override fun scroll(selector: NodeSelector?, forward: Boolean): Boolean {
                val root = HOLDER.get()?.rootInActiveWindow ?: return false
                val node = selector?.let { findNode(root, it) } ?: root
                val action = if (forward) {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                } else {
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }
                return node.performAction(action)
            }

            private fun withNode(
                selector: NodeSelector,
                block: (AccessibilityNodeInfo) -> Boolean
            ): Boolean {
                val root = HOLDER.get()?.rootInActiveWindow ?: return false
                val node = findNode(root, selector) ?: return false
                return block(node)
            }

            private fun findNode(
                root: AccessibilityNodeInfo?,
                selector: NodeSelector
            ): AccessibilityNodeInfo? {
                if (root == null) return null
                val matches = ArrayList<AccessibilityNodeInfo>(4)
                findRecursive(root, selector, matches, 250)
                return matches.getOrNull(selector.occurrence.coerceAtLeast(0))
            }

            private fun findRecursive(
                node: AccessibilityNodeInfo,
                selector: NodeSelector,
                out: MutableList<AccessibilityNodeInfo>,
                limit: Int,
                path: String = ""
            ) {
                if (out.size >= limit) return
                val matches = when {
                    selector.path != null -> path == selector.path
                    selector.viewIdResourceName != null ->
                        node.viewIdResourceName == selector.viewIdResourceName &&
                            optionalEquals(node.className?.toString(), selector.className)
                    selector.contentDescription != null ->
                        node.contentDescription?.toString()?.equals(selector.contentDescription, true) == true &&
                            optionalEquals(node.className?.toString(), selector.className)
                    selector.text != null ->
                        node.text?.toString()?.equals(selector.text, true) == true &&
                            optionalEquals(node.className?.toString(), selector.className)
                    selector.className != null ->
                        node.className?.toString() == selector.className
                    else -> false
                }
                if (matches) out += node
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    findRecursive(child, selector, out, limit, if (path.isEmpty()) i.toString() else path + "/" + i)
                }
            }

            private fun optionalEquals(actual: String?, expected: String?): Boolean =
                expected == null || actual == expected

            private fun collect(
                node: AccessibilityNodeInfo,
                path: String,
                out: MutableList<AccessibilityNodeSchema>,
                limit: Int
            ) {
                if (out.size >= limit) return
                out += AccessibilityNodeSchema(
                    path = path,
                    className = node.className?.toString() ?: "unknown",
                    text = node.text?.toString()?.take(512),
                    contentDescription = node.contentDescription?.toString()?.take(512),
                    viewIdResourceName = node.viewIdResourceName?.take(512),
                    clickable = node.isClickable,
                    editable = node.isEditable,
                    scrollable = node.isScrollable,
                    enabled = node.isEnabled,
                    visibleToUser = node.isVisibleToUser,
                    childCount = node.childCount
                )
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    collect(child, if (path.isEmpty()) i.toString() else path + "/" + i, out, limit)
                }
            }
        }
    }
}
