package com.example.cursorpad

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
fun ActivationStripEditor(
    side: String = "left",
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = context.dataStore
    val preferences by dataStore.data.collectAsState(initial = preferencesOf())

    val defaultWidth = 20.dp
    val defaultHeight = 140.dp
    val defaultX = 0.dp
    var defaultY = 0.dp

    var stripX by remember { mutableStateOf(defaultX) }
    var stripY by remember { mutableStateOf(defaultY) }
    var stripWidth by remember { mutableStateOf(defaultWidth) }
    var stripHeight by remember { mutableStateOf(defaultHeight) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Edit Activation Position") },
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val topBarHeight = 64.dp // default height for the top bar in M3
                                val yOffset = topBarHeight

                                val absoluteY = stripY + yOffset
                                dataStore.edit { preferences ->
                                    if (side == "left") {
                                        preferences[ACTIVATION_STRIP_LEFT_TOP] = absoluteY.value
                                        preferences[ACTIVATION_STRIP_LEFT_HEIGHT] =
                                            stripHeight.value
                                    } else {
                                        preferences[ACTIVATION_STRIP_RIGHT_TOP] = absoluteY.value
                                        preferences[ACTIVATION_STRIP_RIGHT_HEIGHT] =
                                            stripHeight.value
                                    }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    val hitSlop = 30.dp
                    var mode = 0

                    detectDragGestures(
                        onDragStart = { position: Offset ->
                            val touchY = position.y.toDp()
                            val topHandleY = stripY
                            val bottomHandleY = stripY + stripHeight

                            mode = when {
                                // Check if touch is near top handle
                                touchY >= (topHandleY - hitSlop) && touchY <= (topHandleY + hitSlop) -> 1
                                // Check if touch is near bottom handle
                                touchY >= (bottomHandleY - hitSlop) && touchY <= (bottomHandleY + hitSlop) -> 2
                                else -> 0
                            }
                        },

                        onDrag = { change, drag ->
                            val dragY = drag.y.toDp()

                            if (mode != 0) {
                                change.consume()
                            }

                            when (mode) {
                                // Top Handle Logic
                                1 -> {
                                    val newY = stripY + dragY
                                    val newHeight = stripHeight - dragY

                                    if (newY >= 0.dp && newHeight >= 50.dp) {
                                        stripY = newY
                                        stripHeight = newHeight
                                    }
                                }

                                2 -> {
                                    val newHeight = stripHeight + dragY

                                    if (newHeight >= 50.dp) {
                                        stripHeight = newHeight
                                    }
                                }
                            }
                        }
                    )
                }
        ) {

            LaunchedEffect(preferences) {
                defaultY = maxHeight - defaultHeight
                stripWidth = preferences[ACTIVATION_STRIP_WIDTH]?.dp ?: defaultWidth
                val topBarHeight = 64.dp
                // the amount by which the y value has to be shifted to reflect absolute Y coordinate
                val yOffset = topBarHeight

                if (side == "left") {
                    stripY = (preferences[ACTIVATION_STRIP_LEFT_TOP]?.dp
                        ?: (defaultY + yOffset)) - yOffset
                    stripHeight = preferences[ACTIVATION_STRIP_LEFT_HEIGHT]?.dp ?: defaultHeight
                } else {
                    stripX = maxWidth - stripWidth
                    stripY = (preferences[ACTIVATION_STRIP_RIGHT_TOP]?.dp
                        ?: (defaultY + yOffset)) - yOffset
                    stripHeight = preferences[ACTIVATION_STRIP_RIGHT_HEIGHT]?.dp ?: defaultHeight
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = stripX, y = stripY)
                    .size(width = stripWidth, height = stripHeight)
                    .background(color = MaterialTheme.colorScheme.secondary)
                    .clip(RoundedCornerShape(0.dp))
            )

            // Top Handle
            Box(
                modifier = Modifier
                    .offset(y = stripY - 1.dp)
                    .size(width = maxWidth, height = 2.dp)
                    .background(Color.White)
            )

            Box(
                modifier = Modifier
                    .offset(y = stripY - 5.dp)
                    .size(width = maxWidth, height = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(width = 60.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            // Bottom Handle
            Box(
                modifier = Modifier
                    .offset(y = stripY + stripHeight)
                    .size(width = maxWidth, height = 2.dp)
                    .background(Color.White)
            )

            Box(
                modifier = Modifier
                    .offset(y = stripY + stripHeight)
                    .size(width = maxWidth, height = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(width = 60.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}