package com.example.cursorpad

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.preferencesOf

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

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            val defaultWidth = 140.dp
            val defaultHeight = 140.dp
            val defaultX = maxWidth - defaultWidth - 24.dp
            val defaultY = maxHeight - defaultHeight - 24.dp

            // Read saved position values
            var savedWidth = preferences[TOUCHPAD_WIDTH_KEY]?.dp ?: defaultWidth
            var savedHeight = preferences[TOUCHPAD_HEIGHT_KEY]?.dp ?: defaultHeight
            var savedX = preferences[TOUCHPAD_X_KEY]?.dp ?: defaultX
            var savedY = preferences[TOUCHPAD_Y_KEY]?.dp ?: defaultY

            // Create state values
            var padX by remember { mutableStateOf(savedX) }
            var padY by remember { mutableStateOf(savedY) }
            var padWidth by remember { mutableStateOf(savedWidth) }
            var padHeight by remember { mutableStateOf(savedHeight) }

            Box(
                modifier = Modifier
                    .offset(x = padX, y = padY)
                    .size(width = padWidth, height = padHeight)
                    .background(color = Color.Red, shape = RoundedCornerShape(8.dp))
                    .border(width = 2.dp, color = Color.Green, shape = RoundedCornerShape(8.dp))
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
