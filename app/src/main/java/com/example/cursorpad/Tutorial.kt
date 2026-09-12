package com.example.cursorpad

import android.content.res.Configuration
import androidx.collection.mutableFloatSetOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.cursorpad.ui.theme.CursorPadTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tutorial(
    onBackPressed: () -> Unit = {}
) {
    var isRightSwipe by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1566)
            isRightSwipe = !isRightSwipe
        }
    }

    val translationX = if (isRightSwipe) 30f else -30f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Tutorial")
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(vertical = 24.dp, horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Toggling the Touchpad",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(116.dp, 216.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(20.dp, 100.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(0.25f)
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp, 100.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(0.25f)
                        )
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(y = 50.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            this.translationX = translationX.dp.toPx()
                        }
                ) {
                    if (isRightSwipe) {
                        SwipeGestureAnimation(R.raw.swipe_left)
                    } else {
                        SwipeGestureAnimation(
                            R.raw.swipe_left,
                            modifier = Modifier.graphicsLayer { scaleX = -1f })
                    }
                }

            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "To summon the touchpad and the cursor, swipe inwards from the sides, where the strips are located. To remove the touchpad, repeat the same gesture while the touchpad is enabled.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Control the Cursor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(116.dp, 216.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .align(Alignment.BottomEnd)
                ) {
                    DragGestureAnimation(R.raw.swipe_left_down)
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .offset(x = 20.dp, y = 50.dp)
                ) {
                    DragGestureAnimation(R.raw.cursor_left_down)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "You can use the touchpad to move the cursor, dragging on the touchpad will move it accordingly. To tap the element under the cursor, tap on the touchpad. For long press, hold on the touchpad.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SwipeGestureAnimation(resourceId: Int, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resourceId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

@Composable
fun DragGestureAnimation(resourceId: Int) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resourceId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun TutorialPreview() {
    CursorPadTheme() {
        Tutorial()
    }
}
