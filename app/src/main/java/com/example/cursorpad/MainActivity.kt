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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cursorpad.ui.theme.CursorPadTheme
import com.example.cursorpad.ui.theme.Green34
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val overlayPermissionRequestCode = 1001

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

                // WARN: Better to use repeatOnLifecycle
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isOverlayEnabled = Settings.canDrawOverlays(context)
                            isAccessibilityEnabled = isAccessibilityServiceEnabled()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
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
                                    onStart = { startOverlayWithPermissionCheck() },
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

                                        context.startActivity(intent)
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
                                        context.startActivity(intent)
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

    private fun startOverlayWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            } else {
                // Request overlay permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )

                // WARN: Deprecated in favour of Activity Results API. Update it
                startActivityForResult(intent, overlayPermissionRequestCode)
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

    // WARN: Replace this with ActivityResultContracts
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        if (requestCode == overlayPermissionRequestCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startOverlayService()
                }
            }
        }
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