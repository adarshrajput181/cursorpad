package com.example.cursorpad.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "customization_settings")

val CURSOR_SIZE_KEY = floatPreferencesKey(name = "cursor_size")
val BORDER_SIZE_KEY =
    floatPreferencesKey(name = "border_size") // TODO: Make this CURSOR_BORDER_SIZE_KEY
val CURSOR_COLOR_KEY = intPreferencesKey(name = "cursor_color")
val DYNAMIC_COLOR_KEY = booleanPreferencesKey(name = "dynamic_color")
val SHOW_DOT_KEY = booleanPreferencesKey(name = "show_dot")

val TOUCHPAD_X_KEY = floatPreferencesKey(name = "touchpad_x")
val TOUCHPAD_Y_KEY = floatPreferencesKey(name = "touchpad_y")
val TOUCHPAD_WIDTH_KEY = floatPreferencesKey(name = "touchpad_width")
val TOUCHPAD_HEIGHT_KEY = floatPreferencesKey(name = "touchpad_height")

val TOUCHPAD_LEFT_X_KEY = floatPreferencesKey(name = "touchpad_left_x")
val TOUCHPAD_LEFT_Y_KEY = floatPreferencesKey(name = "touchpad_left_y")
val TOUCHPAD_LEFT_WIDTH_KEY = floatPreferencesKey(name = "touchpad_left_width")
val TOUCHPAD_LEFT_HEIGHT_KEY = floatPreferencesKey(name = "touchpad_left_height")

val TOUCHPAD_RIGHT_X_KEY = floatPreferencesKey(name = "touchpad_right_x")
val TOUCHPAD_RIGHT_Y_KEY = floatPreferencesKey(name = "touchpad_right_y")
val TOUCHPAD_RIGHT_WIDTH_KEY = floatPreferencesKey(name = "touchpad_right_width")
val TOUCHPAD_RIGHT_HEIGHT_KEY = floatPreferencesKey(name = "touchpad_right_height")

val TOUCHPAD_SENSITIVITY_KEY = floatPreferencesKey(name = "touchpad_sensitivity")
val TOUCHPAD_COLOR_KEY = intPreferencesKey(name = "touchpad_color")
val TOUCHPAD_SEPARATE_LAYOUT_KEY = booleanPreferencesKey(name = "touchpad_separate_layout")
val TOUCHPAD_LAYOUT_SIDE_KEY = stringPreferencesKey(name = "touchpad_layout_side")

val ACTIVATION_STRIP_WIDTH = floatPreferencesKey(name = "activation_strip_width")
val ACTIVATION_STRIP_LEFT_ENABLED = booleanPreferencesKey(name = "activation_strip_left_enabled")
val ACTIVATION_STRIP_LEFT_TOP = floatPreferencesKey(name = "activation_strip_left_top")
val ACTIVATION_STRIP_LEFT_HEIGHT = floatPreferencesKey(name = "activation_strip_left_height")
val ACTIVATION_STRIP_RIGHT_ENABLED = booleanPreferencesKey(name = "activation_strip_right_enabled")
val ACTIVATION_STRIP_RIGHT_TOP = floatPreferencesKey(name = "activation_strip_right_top")
val ACTIVATION_STRIP_RIGHT_HEIGHT = floatPreferencesKey(name = "activation_strip_right_height")