package com.example.cursorpad.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cursorpad.R
import kotlinx.coroutines.launch

data class GuideStepData(
    val title: String,
    val description: String,
    val buttonText: String? = null,
    val imageID: Int
)

@Composable
fun AccessibilityGuideDialog(
    onDismiss: () -> Unit = {},
    isAccessibilityServiceEnabled: () -> Boolean = { true },
) {
    val context = LocalContext.current
    val steps = listOf(
        GuideStepData(
            "Step 1",
            "Open Accessibility settings and press CursorPad in the downloaded apps list. If you see a 'Use CursorPad' toggle, press it too.",
            "Open Accessibility Settings",
            imageID = R.drawable.step1_disabled_app
        ),
        GuideStepData(
            "Step 2",
            "Go to App Info and toggle 'Allow restricted settings'. It may be under the three dots or available in the options.",
            "Open App Info",
            imageID = R.drawable.step2_app_info
        ),
        GuideStepData("Step 3", "Return to the Accessibility settings and go into CursorPad.", imageID = R.drawable.step3_find_app),
        GuideStepData("Step 4", "Toggle ON 'Use CursorPad'.", "Open Accessibility Settings", imageID = R.drawable.step4_toggle)
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    var hasLeftApp by remember { mutableStateOf(false) }
    var showPermissionError by remember { mutableStateOf(false) }

    fun launchAction(step: GuideStepData) {
        when (step.buttonText) {
            "Open Accessibility Settings" -> {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
                hasLeftApp = true
            }

            "Open App Info" -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                hasLeftApp = true
            }

            // "Next" button doesn't launch intents
            else -> {  }
        }
    }

    // Lifecycle observer to advance the step when the user returns
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLeftApp) {
                // Only advance if the user is on a step that sends them away
                if (pagerState.currentPage in listOf(0, 1, 3)) {
                    scope.launch {
                        if (isAccessibilityServiceEnabled()) {
                            onDismiss()
                        }

                        if (pagerState.currentPage < steps.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            showPermissionError = true
                        }
                    }

                    hasLeftApp = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Enable Accessibility",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Page ${pagerState.currentPage + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                ) { page ->
                    val step = steps[page]
                    Card(
                        modifier = Modifier.defaultMinSize(minHeight = 400.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                            ) {
                                Image(
                                    painter = painterResource(step.imageID),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (page == steps.size - 1 && showPermissionError) {
                            Text(
                                text = "You haven't enabled CursorPad yet. Please toggle it ON and come back.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))

                val currentStep = steps[pagerState.currentPage]
                if (currentStep.buttonText != null) {
                    Button(
                        onClick = {
                            launchAction(currentStep)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentStep.buttonText)
                    }
                } else {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next")
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Not now")
                }
            }
        }
    }
}