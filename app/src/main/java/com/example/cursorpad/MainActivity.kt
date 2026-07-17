package com.example.cursorpad

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cursorpad.ui.theme.CursorPadTheme
import com.example.cursorpad.ui.theme.Green34
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    enum class PermissionType { OVERLAY, ACCESSIBILITY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CursorPadTheme {
                val context = LocalContext.current
                var isOverlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                var isAccessibilityEnabled by remember {
                    mutableStateOf(isAccessibilityServiceEnabled())
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
                    isAccessibilityEnabled = isAccessibilityServiceEnabled()
                }

                // Update permission status everytime the app comes into foreground
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(Unit) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        isOverlayEnabled = Settings.canDrawOverlays(context)
                        isAccessibilityEnabled = isAccessibilityServiceEnabled()
                    }
                }

                fun checkAndStartOverlay () {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(context)) {
                            context.startService(Intent(context, TrackpadOverlayService::class.java))
                        } else {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
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
                                    onClick = {}
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
                                    val context = LocalContext.current

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
        }
    }
    private fun startOverlayService() {
        val intent = Intent(this, TrackpadOverlayService::class.java)
        startService(intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, TrackpadOverlayService::class.java)
        stopService(intent)
    }

    // REFACTOR: duplicate code, method exists in trackpad overlay service
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo?.serviceInfo?.packageName == packageName }
    }
}

@Composable
fun ServiceToggleButton(
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isServiceRunning by TrackpadOverlayService.isRunning.collectAsState(initial = false)

    FloatingActionButton(
        modifier = modifier,
        onClick = {
            if (isServiceRunning)
                onStop()
            else
                onStart()
        }
    ) {
        Icon(
            imageVector = if (isServiceRunning) Icons.Default.Close else Icons.Default.PlayArrow,
            contentDescription = if (isServiceRunning) "Close" else "Play"
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    subtitle: String,
    isEnabled: Boolean = false,
    onClick: () -> Unit,
) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEnabled) {
                    Text(
                        text = "Enabled",
                        color = Green34,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        tint = Green34,
                        contentDescription = null
                    )
                } else {
                    Text(
                        text = "Disabled",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}