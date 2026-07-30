package com.example.cursorpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.launch

val colorOptions = listOf(
    "Dynamic" to null,
    "Red" to Color.Red,
    "Green" to Color.Green,
    "Blue" to Color.Blue,
    "White" to Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit = {},
    onNavigateToTouchpadPositionEditor: () -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = context.dataStore
    val scope = rememberCoroutineScope()

    val settings by dataStore.data.collectAsState(initial = preferencesOf())
    var cursorSize by remember { mutableFloatStateOf(settings[CURSOR_SIZE_KEY] ?: 30f) }
    var borderSize by remember { mutableFloatStateOf(settings[BORDER_SIZE_KEY] ?: 2f) }
    val useDynamicColor = settings[DYNAMIC_COLOR_KEY] ?: true
    val savedColorInt = settings[CURSOR_COLOR_KEY] ?: Color.Red.toArgb()
    val currentDynamicColorInt = MaterialTheme.colorScheme.primary.toArgb()

    var colorDropdownExpanded by remember { mutableStateOf(false) }
    var selectedColorText = if (useDynamicColor) {
        "Dynamic"
    } else {
        colorOptions.find { (_, color) ->
            color?.toArgb() == savedColorInt
        }?.first ?: "White"
    }

    var showDot = settings[SHOW_DOT_KEY] ?: true
    var sensitivity by remember { mutableFloatStateOf(settings[TOUCHPAD_SENSITIVITY_KEY] ?: 1.6f) }

    // Update state if the settings change
    LaunchedEffect(settings) {
        cursorSize = settings[CURSOR_SIZE_KEY] ?: 30f
        borderSize = settings[BORDER_SIZE_KEY] ?: 2f
        sensitivity = settings[TOUCHPAD_SENSITIVITY_KEY] ?: 1.6f
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
                SectionHeading("Cursor Settings")
                Spacer(modifier = Modifier.height(10.dp))

                CursorPreviewArea(
                    cursorColor = if (useDynamicColor) MaterialTheme.colorScheme.primary else Color(
                        savedColorInt
                    ),
                    cursorSize = cursorSize,
                    borderSize = borderSize,
                    showDot = showDot
                )

                Spacer(modifier = Modifier.height(10.dp))

                LabelledSlider(
                    property = cursorSize,
                    label = "Cursor Size",
                    onChange = { cursorSize = it },
                    onChangeFinished = {
                        scope.launch {
                            dataStore.edit { preferences ->
                                preferences[CURSOR_SIZE_KEY] = cursorSize
                            }
                        }
                    },
                    startRange = 10f,
                    endRange = 50f,
                    steps = 29
                )

                LabelledSlider(
                    property = borderSize,
                    label = "Border Size",
                    onChange = { borderSize = it },
                    onChangeFinished = {
                        scope.launch {
                            dataStore.edit { preferences ->
                                preferences[BORDER_SIZE_KEY] = borderSize
                            }
                        }
                    },
                    startRange = 0f,
                    endRange = 5f,
                    steps = 4
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { colorDropdownExpanded = !colorDropdownExpanded }
                    .padding(vertical = 10.dp, horizontal = 24.dp)
            ) {
                Text("Cursor Color", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = selectedColorText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                DropdownMenu(
                    expanded = colorDropdownExpanded,
                    onDismissRequest = { colorDropdownExpanded = false },
                    modifier = Modifier
                        .width(150.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            RoundedCornerShape(4.dp)
                        ),
                    offset = DpOffset(x = 0.dp, y = (-58).dp)
                ) {
                    colorOptions.forEach { (name, color) ->
                        DropdownMenuItem(
                            modifier = Modifier.padding(start = 10.dp),
                            text = {
                                Text(name, fontSize = 15.sp)
                            },
                            onClick = {
                                selectedColorText = name
                                colorDropdownExpanded = false

                                scope.launch {
                                    dataStore.edit { preferences ->
                                        if (name == "Dynamic") {
                                            preferences[DYNAMIC_COLOR_KEY] = true
                                            preferences[CURSOR_COLOR_KEY] =
                                                currentDynamicColorInt
                                        } else {
                                            preferences[DYNAMIC_COLOR_KEY] = false
                                            preferences[CURSOR_COLOR_KEY] =
                                                color?.toArgb() ?: Color.White.toArgb()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Show Center Dot", fontSize = 16.sp)
                Switch(
                    checked = showDot,
                    onCheckedChange = { newValue ->
                        scope.launch {
                            dataStore.edit { preferences ->
                                showDot = newValue
                                preferences[SHOW_DOT_KEY] = newValue
                            }
                        }
                    }
                )
            }

            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                SectionHeading("Touchpad Settings")
                Spacer(modifier = Modifier.height(10.dp))

                TouchpadPositionCard(onEditClick = { onNavigateToTouchpadPositionEditor() })
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    Text("Touchpad Sensitivity: ${(sensitivity * 100).toInt()}%", fontSize = 16.sp)
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        onValueChangeFinished = {
                            scope.launch {
                                dataStore.edit { preferences ->
                                    preferences[TOUCHPAD_SENSITIVITY_KEY] = sensitivity
                                }
                            }
                        },
                        valueRange = 1.0f..2.0f,
                        steps = 9
                    )
                }
            }
        }

    }
}

@Composable
fun CursorPreviewArea(
    cursorColor: Color,
    cursorSize: Float,
    borderSize: Float,
    showDot: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(cursorSize.dp)
                .clip(CircleShape)
                .background(cursorColor)
                .border(
                    BorderStroke(borderSize.dp, Color.White),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun LabelledSlider(
    property: Float,
    label: String,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    startRange: Float,
    endRange: Float,
    steps: Int = 0,
) {
    Column(
        modifier = Modifier
            .padding(vertical = 5.dp)
    ) {
        Text("$label: ${property.toInt()}", fontSize = 16.sp)
        Slider(
            value = property,
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = startRange..endRange,
            steps = steps,
        )
    }
}

@Composable
fun SectionHeading(
    heading: String,
) {
    Text(
        heading,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun TouchpadPositionCard(
    onEditClick: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Touchpad Layout",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customize the placement of the touchpad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))


            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 18.dp)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onEditClick,
                contentPadding = PaddingValues(horizontal = 14.dp, 8.dp)
            ) {
                Text("Edit")
            }
        }
    }
}