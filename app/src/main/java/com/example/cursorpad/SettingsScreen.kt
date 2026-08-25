package com.example.cursorpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

val touchpadColorOptions = listOf(
    "Dynamic" to null,
    "Default" to 0xAA333333,
    "Dark" to 0xAA222222,
    "Light" to 0xAAE0E0E0,
    "Blue" to 0xAA4A90E2,
    "Green" to 0xAA66BB6A
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit = {},
    onNavigateToTouchpadPositionEditor: (String) -> Unit = {},
    onNavigateToStripEditor: (String) -> Unit = {}
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
    val separateLayoutEnabled = settings[TOUCHPAD_SEPARATE_LAYOUT_KEY] ?: true
    val touchpadLayoutSide = settings[TOUCHPAD_LAYOUT_SIDE_KEY] ?: "left"
    var sensitivity by remember { mutableFloatStateOf(settings[TOUCHPAD_SENSITIVITY_KEY] ?: 1.6f) }

    var touchpadColor by remember {
        mutableIntStateOf(
            settings[TOUCHPAD_COLOR_KEY] ?: 0xAA333333.toInt()
        )
    }
    // Extract the rgb values from ARGB
    var rgb = touchpadColor and 0x00FFFFFF
    // Extract the alpha channel value and convert it to percent
    val initialAlphaPercent = ((touchpadColor shr 24) and 0xFF) * 100 / 255
    var alphaPercent by remember { mutableIntStateOf(initialAlphaPercent) }


    // Update state if the settings change
    LaunchedEffect(settings) {
        cursorSize = settings[CURSOR_SIZE_KEY] ?: 30f
        borderSize = settings[BORDER_SIZE_KEY] ?: 2f
        sensitivity = settings[TOUCHPAD_SENSITIVITY_KEY] ?: 1.6f
        touchpadColor = settings[TOUCHPAD_COLOR_KEY] ?: 0xAA333333.toInt()
        rgb = touchpadColor and 0x00FFFFFF
        val savedAlpha = ((touchpadColor shr 24) and 0xFF)
        alphaPercent = (savedAlpha * 100) / 255
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Separate Layout", style = MaterialTheme.typography.bodyLarge)
                        Text("Use different touchpad for each strip", style = MaterialTheme.typography.bodySmall)
                    }

                    Switch(
                        checked = separateLayoutEnabled,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                dataStore.edit { preferences ->
                                    preferences[TOUCHPAD_SEPARATE_LAYOUT_KEY] = newValue
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TouchpadPositionCard(
                    onEditClick = { side ->
                        onNavigateToTouchpadPositionEditor(side)
                    },
                    separateLayoutEnabled = separateLayoutEnabled,
                    selectedSide = touchpadLayoutSide,
                    updateSelectedSide = { side ->
                        scope.launch {
                            dataStore.edit { preferences ->
                                preferences[TOUCHPAD_LAYOUT_SIDE_KEY] = side
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    Text("Sensitivity: ${(sensitivity * 100).toInt()}%", fontSize = 16.sp)
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
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Touchpad Color", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(10.dp))
                // Touchpad preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    TransparencyGrid(modifier = Modifier.fillMaxSize())

                    Box(
                        modifier = Modifier
                            .size(140.dp, 80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(touchpadColor))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    touchpadColorOptions.forEach { (label, color) ->
                        var swatchRgb: Int
                        val colorInt =
                            color?.toInt()
                                ?: MaterialTheme.colorScheme.primaryContainer.toArgb()
                        swatchRgb = colorInt.stripAlpha()
                        val isSelected = swatchRgb == touchpadColor.stripAlpha()

                        ColorSwatchItem(
                            label = label,
                            colorInt = colorInt,
                            isSelected = isSelected,
                            onClick = {
                                val currentAlpha = (touchpadColor shr 24) and 0xFF
                                val newColor = (currentAlpha shl 24) or swatchRgb

                                scope.launch {
                                    dataStore.edit { preferences ->
                                        preferences[TOUCHPAD_COLOR_KEY] = newColor
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Opacity: $alphaPercent%", fontSize = 16.sp)
                Slider(
                    value = alphaPercent.toFloat(),
                    onValueChange = { alphaPercent = it.toInt() },
                    onValueChangeFinished = {
                        // Calculate alpha channel value
                        val alphaInt = alphaPercent * 255 / 100
                        // Combine the alpha with rgb
                        val newColor = (alphaInt shl 24) or rgb

                        scope.launch {
                            dataStore.edit { preferences ->
                                preferences[TOUCHPAD_COLOR_KEY] = newColor
                            }
                        }
                    },
                    valueRange = 0.0f..100.0f,
                )
            }

            ActivationStripSettings(
                onNavigateToStripEditor = onNavigateToStripEditor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActivationStripSettings(
    onNavigateToStripEditor: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = context.dataStore
    val scope = rememberCoroutineScope()

    val settings by dataStore.data.collectAsState(initial = preferencesOf())
    var activationStripWidth by remember { mutableStateOf(settings[ACTIVATION_STRIP_WIDTH] ?: 20f) }
    var leftStripEnabled by remember {
        mutableStateOf(
            settings[ACTIVATION_STRIP_LEFT_ENABLED] ?: true
        )
    }
    var rightStripEnabled by remember {
        mutableStateOf(
            settings[ACTIVATION_STRIP_RIGHT_ENABLED] ?: true
        )
    }

    LaunchedEffect(settings) {
        activationStripWidth = settings[ACTIVATION_STRIP_WIDTH] ?: 20f
        leftStripEnabled = settings[ACTIVATION_STRIP_LEFT_ENABLED] ?: true
        rightStripEnabled = settings[ACTIVATION_STRIP_RIGHT_ENABLED] ?: true
    }

    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp)) {
        SectionHeading("Activation Strip Settings")

        Spacer(modifier = Modifier.height(10.dp))
        LabelledSlider(
            property = activationStripWidth,
            label = "Width",
            onChange = { activationStripWidth = it },
            onChangeFinished = {
                scope.launch {
                    dataStore.edit { preferences ->
                        preferences[ACTIVATION_STRIP_WIDTH] = activationStripWidth
                    }
                }
            },
            startRange = 10f,
            endRange = 40f,
            steps = 29
        )

        Spacer(modifier = Modifier.height(10.dp))

        StripEditorCard(
            side = "left",
            stripEnabled = leftStripEnabled,
            onStripUpdate = { value -> leftStripEnabled = value },
            onNavigateToStripEditor = onNavigateToStripEditor
        )

        Spacer(modifier = Modifier.height(10.dp))

        StripEditorCard(
            side = "right",
            stripEnabled = rightStripEnabled,
            onStripUpdate = { value -> rightStripEnabled = value },
            onNavigateToStripEditor = onNavigateToStripEditor
        )

    }
}

@Composable
fun StripEditorCard(
    side: String = "left",
    stripEnabled: Boolean = true,
    onStripUpdate: (Boolean) -> Unit,
    onNavigateToStripEditor: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStore = context.dataStore

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
                    text = "${if (side == "left") "Left" else "Right"} Strip",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Swipe from the $side edge to open touchpad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            val contentAlignment =
                if (side == "left") Alignment.CenterStart else Alignment.CenterEnd
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
                contentAlignment = contentAlignment
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 18.dp)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = stripEnabled,
                onCheckedChange = { newValue ->
                    scope.launch {
                        dataStore.edit { preferences ->
                            onStripUpdate(newValue)
                            if (side == "left") {
                                preferences[ACTIVATION_STRIP_LEFT_ENABLED] = newValue
                            } else {
                                preferences[ACTIVATION_STRIP_RIGHT_ENABLED] = newValue
                            }
                        }
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {

            Button(
                onClick = { onNavigateToStripEditor(side) },
                contentPadding = PaddingValues(horizontal = 14.dp, 8.dp),
                enabled = stripEnabled
            ) {
                Text("Edit")
            }
        }
    }
}

@Composable
fun ColorSwatchItem(
    label: String,
    colorInt: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(colorInt))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
        ) {
            if (colorInt == MaterialTheme.colorScheme.primaryContainer.toArgb()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Dynamic Color",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun Int.stripAlpha(): Int {
    return this and 0x00FFFFFF
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
    onEditClick: (String) -> Unit,
    separateLayoutEnabled: Boolean,
    selectedSide: String,
    updateSelectedSide: (String) -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start =16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
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


            val boxAlignment = if (selectedSide == "right") Alignment.BottomEnd else Alignment.BottomStart
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
                contentAlignment = boxAlignment
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 18.dp)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { if (separateLayoutEnabled) onEditClick(selectedSide) else onEditClick("shared") },
                contentPadding = PaddingValues(horizontal = 14.dp, 8.dp)
            ) {
                Text("Edit")
            }
        }

        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                SegmentedButton(
                    selected = selectedSide == "left",
                    onClick = { updateSelectedSide("left") },
                    enabled = separateLayoutEnabled,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Left")
                }

                SegmentedButton(
                    selected = selectedSide == "right",
                    onClick = { updateSelectedSide("right") },
                    enabled = separateLayoutEnabled,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Right")
                }
            }
        }
    }
}

@Composable
fun TransparencyGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val tileSize = 12.dp.toPx()
        val width = size.width
        val height = size.height
        val lightTile = Color(0xFFE0E0E0)
        val darkTile = Color(0xFF9E9E9E)

        for (x in 0..(width / tileSize).toInt()) {
            for (y in 0..(height / tileSize).toInt()) {
                val isEven = (x + y) % 2 == 0

                drawRect(
                    color = if (isEven) lightTile else darkTile,
                    topLeft = Offset(x * tileSize, y * tileSize),
                    size = Size(tileSize, tileSize)
                )
            }
        }
    }
}