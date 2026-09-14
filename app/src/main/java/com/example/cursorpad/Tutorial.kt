package com.example.cursorpad

import android.content.res.Configuration
import androidx.collection.mutableFloatSetOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
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
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.cursorpad.ui.theme.CursorPadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

            val sharedProgress = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                while (true) {
                    sharedProgress.snapTo(0f)
                    sharedProgress.animateTo(1f, tween(2500, easing = LinearEasing))
                }
            }

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
                    DragGestureAnimation(R.raw.swipe_left_down, sharedProgress.value)
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .offset(x = 20.dp, y = 50.dp)
                ) {
                    DragGestureAnimation(R.raw.cursor_left_down, sharedProgress.value)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "You can use the touchpad to move the cursor, dragging on the touchpad will move it accordingly. ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            val scaleProgress = remember { Animatable(1f) }
            val rippleProgress = remember { Animatable(0f) }
            val tapProgress = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                while (true) {
                    val scaleJob = launch {
                        delay(700)
                        scaleProgress.animateTo(
                            targetValue = 0.8f,
                            animationSpec = tween(160, easing = FastOutSlowInEasing),
                        )

                        scaleProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        )
                    }

                    val rippleJob = launch {
                        delay(700)
                        rippleProgress.snapTo(0f)
                        rippleProgress.animateTo(1f, tween(600, easing = LinearEasing))
                    }

                    val tapJob = launch {
                        tapProgress.animateTo(
                            targetValue = 0.25f,
                            animationSpec = tween(
                                durationMillis = 1000,
                                easing = LinearEasing
                            )
                        )
                        tapProgress.snapTo(0f)
                    }

                    scaleJob.join()
                    rippleJob.join()
                    tapJob.join()

                    delay(800)
                }
            }

            Box(
                modifier = Modifier
                    .size(116.dp, 216.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally)
            ) {
                // Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(60.dp, 30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Click Me!", style = MaterialTheme.typography.bodySmall)

                    // Ripple
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(140.dp)
                            .graphicsLayer {
                                val s = rippleProgress.value
                                scaleX = s * 1.3f
                                scaleY = s * 1.3f
                                alpha = 0.35f * (1f - kotlin.math.abs(s - 0.4f) / 0.6f).coerceIn(0f, 1f)
                            }
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(12.dp)
                        .graphicsLayer { scaleX = scaleProgress.value; scaleY = scaleProgress.value}
                        .clip(CircleShape)
                        .background(Color(0xff24a8af))
                )

                Box(
                    modifier = Modifier
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .align(Alignment.BottomEnd)
                ) {
                    Box(modifier = Modifier.size(60.dp)) {
                        TapGestureAnimation(R.raw.swipe_left, progress = tapProgress.value)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "To tap the element under the cursor, tap on the touchpad. For long press, hold on the touchpad.",
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
fun DragGestureAnimation(resourceId: Int, progress: Float) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resourceId))

    LottieAnimation(
        composition = composition,
        progress = { progress },
    )
}

@Composable
fun TapGestureAnimation(resourceId: Int, progress: Float) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resourceId))

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.graphicsLayer { scaleX = 1.3f; scaleY = 1.3f}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun TutorialPreview() {
    CursorPadTheme() {
        Tutorial()
    }
}
