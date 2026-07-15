package com.example.cursorpad

import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cursorpad.ui.theme.CursorPadTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val overlayPermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CursorPadTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("CursorPad")
                            },
                            colors = TopAppBarDefaults.topAppBarColors( containerColor = MaterialTheme.colorScheme.background)
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        ServiceToggleButton(
                            modifier = Modifier,
                            onStart = { startOverlayWithPermissionCheck() },
                            onStop = { stopOverlayService() }
                        )
                    }
                ) { innerPadding ->
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
                    Uri.parse("package:$packageName")
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
        modifier = modifier.padding(16.dp),
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