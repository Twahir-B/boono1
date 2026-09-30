package com.aether.agent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aether.agent.agent.UiElement
import com.aether.agent.agent.UiTree
import org.json.JSONArray
import org.json.JSONObject

/**
 * Core service for:
 * - Notch touch zone (via overlay + this service for actions)
 * - Quick Cursor gestures
 * - Agent UI tree reading + taps/swipes
 */
class AetherAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can be used later for live window tracking
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /** Build a structured UI tree for the AI agent. */
    fun captureUiTree(maxNodes: Int = 120): UiTree {
        val root = rootInActiveWindow ?: return UiTree(packageName = null, elements = emptyList())
        val elements = mutableListOf<UiElement>()
        var idCounter = 0

        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || elements.size >= maxNodes || depth > 18) return
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() <= 0 || rect.height() <= 0) {
                for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
                return
            }

            val text = node.text?.toString()?.take(80)
            val desc = node.contentDescription?.toString()?.take(80)
            val cls = node.className?.toString()?.substringAfterLast('.') ?: "View"
            val clickable = node.isClickable || node.isCheckable
            val editable = node.isEditable

            if (clickable || editable || !text.isNullOrBlank() || !desc.isNullOrBlank()) {
                elements.add(
                    UiElement(
                        id = ++idCounter,
                        type = cls,
                        text = text,
                        contentDescription = desc,
                        clickable = clickable,
                        editable = editable,
                        bounds = rect,
                        packageName = node.packageName?.toString()
                    )
                )
            }
            for (i in 0 until node.childCount) {
                walk(node.getChild(i), depth + 1)
            }
        }

        walk(root, 0)
        return UiTree(
            packageName = root.packageName?.toString(),
            elements = elements
        )
    }

    fun tap(x: Float, y: Float, durationMs: Long = 50L): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300L): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun globalBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun globalHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun globalRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun globalNotifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun globalPowerDialog() = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

    companion object {
        @Volatile
        var instance: AetherAccessibilityService? = null
            private set

        fun isEnabled(): Boolean = instance != null
    }
}
