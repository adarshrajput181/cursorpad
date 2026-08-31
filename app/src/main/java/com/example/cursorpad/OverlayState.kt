package com.example.cursorpad

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.Preferences

data class CursorState(
    val color: Int,
    val size: Float,
    val borderSize: Float,
    val showDot: Boolean
)

// State for individual touchpads
data class TouchpadConfig(
    val active: Boolean,
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val color: Int,
)

data class TouchpadState(
    val separateLayout: Boolean,
    val leftTouchpad: TouchpadConfig,
    val rightTouchpad: TouchpadConfig,
    val sharedTouchpad: TouchpadConfig
)

// State for individual strips
data class StripConfig(
    val active: Boolean = true,
    val top: Float = 0f, val width: Float = 30f, val height: Float = 140f,
)

data class StripState(
    val leftStrip: StripConfig = StripConfig(),
    val rightStrip: StripConfig = StripConfig()
)

fun Preferences.toCursorState(): CursorState {
    return CursorState(
        color = this[CURSOR_COLOR_KEY] ?: Color.Green.toArgb(),
        size = this[CURSOR_SIZE_KEY] ?: 30f,
        borderSize = this[BORDER_SIZE_KEY] ?: 2f,
        showDot = this[SHOW_DOT_KEY] ?: true
    )
}

fun Preferences.toTouchpadState(
    defaultLeftX: Float,
    defaultRightX: Float,
    defaultY: Float,
): TouchpadState {
    return TouchpadState(
        separateLayout = this[TOUCHPAD_SEPARATE_LAYOUT_KEY] ?: true,
        leftTouchpad = TouchpadConfig(
            active = this[TOUCHPAD_SEPARATE_LAYOUT_KEY] ?: true,
            x = this[TOUCHPAD_LEFT_X_KEY] ?: defaultLeftX,
            y = this[TOUCHPAD_LEFT_Y_KEY] ?: defaultY,
            width = this[TOUCHPAD_LEFT_WIDTH_KEY] ?: 140f,
            height = this[TOUCHPAD_LEFT_HEIGHT_KEY] ?: 140f,
            color = this[TOUCHPAD_COLOR_KEY] ?: 0xAA333333.toInt()
        ),
        rightTouchpad = TouchpadConfig(
            active = this[TOUCHPAD_SEPARATE_LAYOUT_KEY] ?: true,
            x = this[TOUCHPAD_RIGHT_X_KEY] ?: defaultRightX,
            y = this[TOUCHPAD_RIGHT_Y_KEY] ?: defaultY,
            width = this[TOUCHPAD_RIGHT_WIDTH_KEY] ?: 140f,
            height = this[TOUCHPAD_RIGHT_HEIGHT_KEY] ?: 140f,
            color = this[TOUCHPAD_COLOR_KEY] ?: 0xAA333333.toInt()
        ),
        sharedTouchpad = TouchpadConfig(
            active = !(this[TOUCHPAD_SEPARATE_LAYOUT_KEY] ?: true),
            x = this[TOUCHPAD_X_KEY] ?: defaultRightX,
            y = this[TOUCHPAD_Y_KEY] ?: defaultY,
            width = this[TOUCHPAD_WIDTH_KEY] ?: 140f,
            height = this[TOUCHPAD_HEIGHT_KEY] ?: 140f,
            color = this[TOUCHPAD_COLOR_KEY] ?: 0xAA333333.toInt()
        )
    )
}

fun Preferences.toStripState(
    defaultTop: Float
): StripState {
    return StripState(
        leftStrip = StripConfig(
            active = this[ACTIVATION_STRIP_LEFT_ENABLED] ?: true,
            top = this[ACTIVATION_STRIP_LEFT_TOP] ?: defaultTop,
            width = this[ACTIVATION_STRIP_WIDTH] ?: 20f,
            height = this[ACTIVATION_STRIP_LEFT_HEIGHT] ?: 140f
        ),
        rightStrip = StripConfig(
            active = this[ACTIVATION_STRIP_RIGHT_ENABLED] ?: true,
            top = this[ACTIVATION_STRIP_RIGHT_TOP] ?: defaultTop,
            width = this[ACTIVATION_STRIP_WIDTH] ?: 20f,
            height = this[ACTIVATION_STRIP_RIGHT_HEIGHT] ?: 140f
        )
    )
}