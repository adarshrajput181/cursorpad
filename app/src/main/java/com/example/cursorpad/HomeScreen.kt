package com.example.cursorpad

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.first

enum class PermissionType { OVERLAY, ACCESSIBILITY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldHomeScreen(
    stopOverlayService: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var isOverlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isOverlayEnabled = Settings.canDrawOverlays(context)

        if (isOverlayEnabled) {
            context.startService(Intent(context, TrackpadOverlayService::class.java))
        }
    }

    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
    }

    // Update permission status everytime the app comes into foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isOverlayEnabled = Settings.canDrawOverlays(context)
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        }
    }

    // Set default color to dynamic primary color if no default color set
    val defaultColorInt = MaterialTheme.colorScheme.primary.toArgb()
    LaunchedEffect(Unit) {
        val dataStore = context.dataStore
        val preferences = dataStore.data.first()
        val useDynamicColor = preferences[DYNAMIC_COLOR_KEY] ?: true
        if (!preferences.contains(CURSOR_COLOR_KEY) || useDynamicColor) {
            dataStore.edit { it[CURSOR_COLOR_KEY] = defaultColorInt }
        }
    }

    fun checkAndStartOverlay () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, TrackpadOverlayService::class.java))
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = "package:${context.packageName}".toUri()
                }

                overlayLauncher.launch(intent)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("CursorPad")
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                floatingActionButton = {
                    ServiceToggleButton(
                        modifier = Modifier,
                        onStart = { checkAndStartOverlay() },
                        onStop = { stopOverlayService() }
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            onNavigateToSettings()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                contentPadding = PaddingValues(horizontal = 16.dp)
            )
        }
    ) { innerPadding ->
        var showPermissionDialog by remember { mutableStateOf<PermissionType?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = "One hand control for easier reach across your screen.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            Text(
                text = "Required Permissions",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "The following permissions are required for base functionality. Without these permissions the app may not work properly.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            PermissionCard(
                title = "Display over other apps",
                subtitle = "To display the cursor pad",
                isEnabled = isOverlayEnabled,
                onClick = { showPermissionDialog = PermissionType.OVERLAY }
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            PermissionCard(
                title = "Accessibility Service",
                subtitle = "To allow taps on the screen",
                isEnabled = isAccessibilityEnabled,
                onClick = { showPermissionDialog = PermissionType.ACCESSIBILITY }
            )

            if (showPermissionDialog == PermissionType.OVERLAY) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = null },
                    title = { Text("Allow overlay?") },
                    text = {
                        Text(
                            "To display the cursor, the app needs permission to draw over other apps.\n\n" +
                                    "If you see a list of apps on the next screen, please select 'CursorPad' from the list."
                        )
                    },
                    confirmButton = {
                        val context = LocalContext.current
                        TextButton(onClick = {
                            showPermissionDialog = null

                            val intent =
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = "package:${context.packageName}".toUri()
                                }

                            overlayLauncher.launch(intent)
                        }) {
                            Text("Proceed")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPermissionDialog = null
                        }) {
                            Text("No")
                        }
                    }
                )
            } else if (showPermissionDialog == PermissionType.ACCESSIBILITY) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = null },
                    title = { Text("Enable Accessibility?") },
                    text = { Text("Accessibility service permission is needed for simulating taps.\n\n" +
                            "On the next screen, go to Downloaded Apps, then choose CursorPad and enable 'Use CursorPad'.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showPermissionDialog = null

                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            accessibilityLauncher.launch(intent)
                        }) {
                            Text("Proceed")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPermissionDialog = null
                        }) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }
}

@Preview
@Composable
fun HomeScreen(
    stopOverlayService: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isDrawOverlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val isServiceRunning by TrackpadOverlayService.isRunning.collectAsState(initial = false)

    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
    }

    val drawOverlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isDrawOverlayEnabled = Settings.canDrawOverlays(context)
    }

    // Update permission status everytime the app comes into foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isDrawOverlayEnabled = Settings.canDrawOverlays(context)
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        }
    }

    // Set default dynamic color to be used by the service
    val defaultColorInt = MaterialTheme.colorScheme.primary.toArgb()
    LaunchedEffect(Unit) {
        val dataStore = context.dataStore
        val preferences = dataStore.data.first()
        val useDynamicColor = preferences[DYNAMIC_COLOR_KEY] ?: true
        if (!preferences.contains(CURSOR_COLOR_KEY) || useDynamicColor) {
            dataStore.edit { preferences ->
                preferences[CURSOR_COLOR_KEY] = defaultColorInt
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        // Header
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "CursorPad",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Better reach for one-handed grips.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        HomeListRow(
            icon = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
            iconTint = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            title = "Accessibility",
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAccessibilityEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibilityLauncher.launch(intent)
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )

        HomeListRow(
            icon = if (isDrawOverlayEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
            iconTint = if (isDrawOverlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            title = "Display over other apps",
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isDrawOverlayEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDrawOverlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                drawOverlayLauncher.launch(intent)
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )

        HomeListRow(
            icon = Icons.AutoMirrored.Filled.Help,
            title = "How it works",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )

        HomeListRow(
            icon = Icons.Default.PlayArrow,
            title = "Overlay",
            subtitle = (
               if (isAccessibilityEnabled && isDrawOverlayEnabled) {
                   "Click to start the service"
               } else if (isAccessibilityEnabled) {
                   "Requires draw over other apps permission"
               } else {
                   "Requires accessibility permission"
               }
            ),
            trailingContent = {
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = {
                        if (it && isAccessibilityEnabled && isDrawOverlayEnabled) {
                            context.startService(Intent(context, TrackpadOverlayService::class.java))
                        } else if (!it) {
                            stopOverlayService()
                        }
                    },
                    enabled = isAccessibilityEnabled && isDrawOverlayEnabled
                )
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )

        HomeListRow(
            icon = Icons.Default.Settings,
            title = "Settings",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = onNavigateToSettings
        )
    }
}

@Composable
private fun HomeListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingContent: @Composable () -> Unit,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 20.dp), // Generous, thumb-friendly touch target
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
        }
        trailingContent()
    }
}