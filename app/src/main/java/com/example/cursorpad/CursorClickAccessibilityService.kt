package com.example.cursorpad

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent

class CursorClickAccessibilityService : AccessibilityService() {
    companion object {
        private var instance: CursorClickAccessibilityService? = null

        /**
         * Perform a click on absolute coordinates x & y
         * Returns true if the gesture was dispatched, false otherwise
         */
        fun performClick(x: Float, y: Float) : Boolean {
            return instance?.injectClick(x, y) ?: false
        }

        fun performLongPress(x: Float, y: Float) : Boolean {
            return instance?.injectLongPress(x, y) ?: false
        }

        fun isServiceEnabled() : Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("CursorClickAccessibility", "Service Connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d("CursorClickAccessibility", "Service Destroyed")
    }

    override fun onInterrupt() {
        Log.d("CursorClickAccessibility", "Service Interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    private fun injectClick(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun injectLongPress(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0,
                ViewConfiguration.getLongPressTimeout().toLong() + 50
            ))
            .build()

        return dispatchGesture(gesture, null, null)
    }
}
