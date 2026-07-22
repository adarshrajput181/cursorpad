package com.example.cursorpad

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = context.dataStore
    val scope = rememberCoroutineScope()

    val settings by dataStore.data.collectAsState(initial = preferencesOf())
    var cursorSize by remember { mutableStateOf(settings[CURSOR_SIZE_KEY] ?: 30f)}
    var borderSize by remember { mutableStateOf(settings[BORDER_SIZE_KEY] ?: 2f)}

    // Update state if the settings change
    LaunchedEffect(settings) {
        cursorSize = settings[CURSOR_SIZE_KEY] ?: 30f
        borderSize = settings[BORDER_SIZE_KEY] ?: 2f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                "Cursor Settings",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                Text("Cursor Size: ${cursorSize.toInt()}", fontSize = 16.sp)
                Spacer(
                    modifier = Modifier.width(16.dp)
                )
                Slider(
                    value = cursorSize,
                    onValueChange = { cursorSize = it },
                    onValueChangeFinished = {
                        scope.launch {
                            dataStore.edit { preferences ->
                                preferences[CURSOR_SIZE_KEY] = cursorSize
                            }
                        }
                    },
                    valueRange = 20f..50f,
                    steps = 30
                )
            }

            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                Text("Border Size: ${borderSize.toInt()}", fontSize = 16.sp)
                Slider(
                    value = borderSize,
                    onValueChange = { borderSize = it },
                    onValueChangeFinished = {
                        scope.launch {
                            dataStore.edit { preferences ->
                                preferences[BORDER_SIZE_KEY] = borderSize
                            }
                        }
                    },
                    valueRange = 0f..5f,
                    steps = 4
                )
            }
    }

    }
}
