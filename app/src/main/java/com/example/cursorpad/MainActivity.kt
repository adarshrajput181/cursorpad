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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.cursorpad.ui.theme.CursorPadTheme

class MainActivity : ComponentActivity() {
    private val overlayPermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CursorPadTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LaunchOverlayButton(
                        modifier = Modifier.padding(innerPadding),
                        onStartOverlay = { startOverlayWithPermissionCheck() }
                    )
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
fun LaunchOverlayButton(
    modifier: Modifier = Modifier,
    onStartOverlay: () -> Unit
) {
    Box(
        modifier=modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button (
            onClick = onStartOverlay
        ) {
            Text("Start Trackpad Overlay")
        }
    }
}
