package com.example.cursorpad

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cursorpad.ui.theme.CursorPadTheme
import com.example.cursorpad.ui.theme.Green34

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CursorPadTheme {
                HomeScreen(
                    stopOverlayService = { stopOverlayService() }
                )
            }
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, TrackpadOverlayService::class.java)
        stopService(intent)
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