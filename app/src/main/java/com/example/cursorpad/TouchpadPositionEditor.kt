package com.example.cursorpad

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadPositionEditor(
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = context.dataStore
    val preferences by dataStore.data.collectAsState(initial = preferencesOf())
    val scope = rememberCoroutineScope()

    // Create state values
    var padX by remember { mutableStateOf(0.dp) }
    var padY by remember { mutableStateOf(0.dp) }
    var padWidth by remember { mutableStateOf(0.dp) }
    var padHeight by remember { mutableStateOf(0.dp) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Edit Touchpad Position") },
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val statusBarHeight = LocalDensity.current.run {
                        WindowInsets.statusBars.getTop(this).toDp()
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                val topBarHeight = 64.dp // default M3 Top Bar Height

                                val yOffset = topBarHeight
                                val absolutePadY = padY + yOffset
                                dataStore.edit { preferences ->
                                    preferences[TOUCHPAD_X_KEY] = padX.value
                                    preferences[TOUCHPAD_Y_KEY] = absolutePadY.value
                                    preferences[TOUCHPAD_WIDTH_KEY] = padWidth.value
                                    preferences[TOUCHPAD_HEIGHT_KEY] = padHeight.value
                                }

                                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            val statusBarHeight = LocalDensity.current.run {
                WindowInsets.statusBars.getTop(this).toDp()
            }

            LaunchedEffect(preferences, maxWidth, maxHeight) {
                val defaultWidth = 140.dp
                val defaultHeight = 140.dp
                val defaultX = maxWidth - defaultWidth - 24.dp
                val defaultY = maxHeight - defaultHeight - 24.dp
                val topBarHeight = 64.dp // default value in M3
                val yOffset = topBarHeight

                // Read saved position values
                padX = preferences[TOUCHPAD_X_KEY]?.dp ?: defaultX
                padY = (preferences[TOUCHPAD_Y_KEY]?.dp ?: (defaultY + yOffset)) - yOffset
                padWidth = preferences[TOUCHPAD_WIDTH_KEY]?.dp ?: defaultWidth
                padHeight = preferences[TOUCHPAD_HEIGHT_KEY]?.dp ?: defaultHeight
            }

            Box(
                modifier = Modifier
                    .offset(x = padX, y = padY)
                    .size(width = padWidth, height = padHeight)
                    .background(color = Color(0xAA333333), shape = RoundedCornerShape(20.dp))
                    .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        var isResizeMode = false

                        detectDragGestures(
                            onDragStart = { offset: Offset ->
                                val offsetX = offset.x.toDp()
                                val offsetY = offset.y.toDp()
                                val handleSize = 30.dp

                                isResizeMode = offsetX <= handleSize && offsetY <= handleSize
                            },
                            onDrag = { change, dragAmount ->
                                val offsetX = dragAmount.x.toDp()
                                val offsetY = dragAmount.y.toDp()
                                change.consume()

                                if (isResizeMode) {
                                    val newX = padX + offsetX
                                    val newY = padY + offsetY
                                    val newWidth = padWidth - offsetX
                                    val newHeight = padHeight - offsetY

                                    if (newWidth >= 50.dp && newX >= 0.dp && newX + newWidth <= maxWidth) {
                                        padX = newX
                                        padWidth = newWidth

                                    }

                                    if (newHeight >= 50.dp && newY >= 0.dp && newY + newHeight <= maxHeight) {
                                        padY = newY
                                        padHeight = newHeight
                                    }
                                } else {
                                    padX = (padX + offsetX).coerceIn(0.dp, maxWidth - padWidth)
                                    padY = (padY + offsetY).coerceIn(0.dp, maxHeight - padHeight)
                                }
                            }
                        )
                    }
            ) {
                // Center Handle for movement
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color = Color.Blue)
                        .border(width = 2.dp, color = Color.White, shape = CircleShape)
                )

                // Top left Handle for resizing
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color = Color.Blue)
                        .border(width = 2.dp, color = Color.White, shape = CircleShape)
                )
            }
        }
    }
}
