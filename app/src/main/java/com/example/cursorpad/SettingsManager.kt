package com.example.cursorpad

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "customization_settings")

val CURSOR_SIZE_KEY = floatPreferencesKey(name = "cursor_size")
val BORDER_SIZE_KEY = floatPreferencesKey(name = "border_size")
