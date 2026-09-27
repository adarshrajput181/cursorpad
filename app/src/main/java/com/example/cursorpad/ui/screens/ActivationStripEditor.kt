package com.example.cursorpad.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import com.example.cursorpad.data.ACTIVATION_STRIP_LEFT_HEIGHT
import com.example.cursorpad.data.ACTIVATION_STRIP_LEFT_TOP
import com.example.cursorpad.data.ACTIVATION_STRIP_RIGHT_HEIGHT
import com.example.cursorpad.data.ACTIVATION_STRIP_RIGHT_TOP
import com.example.cursorpad.data.ACTIVATION_STRIP_WIDTH
import com.example.cursorpad.data.dataStore
import com.example.cursorpad.ui.theme.CursorPadTheme
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationStripEditor(
    side: String = "left",
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val dataStore = context.dataStore
    val preferences by dataStore.data.collectAsState(initial = preferencesOf())

    val defaultWidth = 20.dp
    val defaultHeight = 200.dp
    val defaultX = 0.dp
    var defaultY = 0.dp

    var stripX by remember { mutableStateOf(defaultX) }
    var stripY by remember { mutableStateOf(defaultY) }
    var stripWidth by remember { mutableStateOf(defaultWidth) }
    var stripHeight by remember { mutableStateOf(defaultHeight) }

    val buttonNavHeight = 80.dp
    val currentNavHeight = WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
    val extraReservedBottomHeight = (buttonNavHeight - currentNavHeight).coerceAtLeast(0.dp)

    // Top padding provided by Scaffold
    var contentTopPadding by remember { mutableStateOf(0.dp) }

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
                                val yOffset = contentTopPadding
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
        contentTopPadding = innerPadding.calculateTopPadding()
        val dashedLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(extraReservedBottomHeight) {
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
                                    val totalHeight = size.height.toDp() - extraReservedBottomHeight

                                    if (newHeight >= 50.dp && totalHeight >= stripY + newHeight) {
                                        stripHeight = newHeight
                                    }
                                }
                            }
                        }
                    )
                }
        ) {

            val contentBottomPadding = innerPadding.calculateBottomPadding()
            LaunchedEffect(preferences) {
                defaultY = maxHeight - defaultHeight - contentBottomPadding
                stripWidth = preferences[ACTIVATION_STRIP_WIDTH]?.dp ?: defaultWidth
                // the amount by which the y value has to be shifted to reflect absolute Y coordinate
                val yOffset = contentTopPadding

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
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
                    .clip(RoundedCornerShape(0.dp))
            )

            // Top Handle
            Box(
                modifier = Modifier
                    .offset(y = stripY - 1.dp)
                    .size(width = maxWidth, height = 2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
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
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            // Bottom Handle
            Box(
                modifier = Modifier
                    .offset(y = stripY + stripHeight)
                    .size(width = maxWidth, height = 2.dp)
                    .background(MaterialTheme.colorScheme.outline)
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
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            val bottomY = ((stripY + stripHeight).value) * density
            val gestureNavMaxHeight = 200 * density
            val y = bottomY - gestureNavMaxHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = dashedLineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            Text(
                "Gesture navigation may intercept touches above this line",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = if (side == "left") TextAlign.End else TextAlign.Start,
                modifier = Modifier
                    .align(if (side == "left") Alignment.TopStart else Alignment.TopEnd)
                    .offset(y = stripY + stripHeight - 200.dp)
                    .padding(
                        start = if (side == "left") 45.dp else 5.dp,
                        end = if (side == "right") 45.dp else 5.dp
                    )
            )
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewStripEditor() {
    CursorPadTheme {
        ActivationStripEditor()
    }
}
