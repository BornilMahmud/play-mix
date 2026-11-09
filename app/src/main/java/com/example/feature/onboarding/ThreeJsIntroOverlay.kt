package com.example.feature.onboarding

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SoftCyan
import kotlinx.coroutines.delay

/**
 * Named JavaScript bridge class preserved by ProGuard/R8 rules.
 */
class PlayMixIntroBridge(
    private val onCompletedCallback: () -> Unit
) {
    @JavascriptInterface
    fun onIntroCompleted() {
        Handler(Looper.getMainLooper()).post {
            onCompletedCallback()
        }
    }
}

/**
 * Non-skippable Play Mix Intro Video Overlay (`res/raw/play_mix_intro.mp4`).
 *
 * Playback rules:
 * 1. Plays 1 time on the very first install of Play Mix.
 * 2. Plays 1 time if the app has not been opened for 7 straight days (`>= 7 * 24 * 60 * 60 * 1000L` ms).
 * 3. Strictly non-skippable while playing (no Skip button, system Back button blocked, touch gestures consumed).
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun ThreeJsIntroOverlay(
    isShortTransition: Boolean = false,
    onIntroFinished: () -> Unit,
    preferWebViewRenderer: Boolean = false,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var videoStarted by remember { mutableStateOf(false) }
    var videoFallbackActive by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableFloatStateOf(0f) }

    // Block system Back gesture/button so the intro is strictly non-skippable
    BackHandler(enabled = true) {
        // Non-skippable intro: intentionally ignore back press until video completes
    }

    fun releaseVideoSafely(videoView: VideoView?) {
        if (videoView == null) return
        runCatching {
            videoView.stopPlayback()
            (videoView.parent as? ViewGroup)?.removeView(videoView)
        }
    }

    fun finishSafely() {
        if (completed) return
        completed = true
        val targetVideo = videoViewRef
        videoViewRef = null
        releaseVideoSafely(targetVideo)
        onIntroFinished()
    }

    // Track progress of the 4.3-second intro video and guarantee completion
    LaunchedEffect(isShortTransition) {
        val totalDurationMs = if (isShortTransition) 2200L else 4350L
        val stepMs = 16L
        var elapsed = 0L
        while (elapsed < totalDurationMs && !completed) {
            val currentVideo = videoViewRef
            val customProgress = runCatching {
                if (currentVideo != null && videoStarted && currentVideo.duration > 0) {
                    (currentVideo.currentPosition.toFloat() / currentVideo.duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }
            }.getOrNull()

            progressFraction = customProgress ?: (elapsed.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            delay(stepMs)
            elapsed += stepMs
        }
        progressFraction = 1f
        finishSafely()
    }

    DisposableEffect(Unit) {
        onDispose {
            val targetVideo = videoViewRef
            videoViewRef = null
            releaseVideoSafely(targetVideo)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050508))
            .pointerInput(Unit) {
                // Consume all taps so underlying screens cannot be interacted with during non-skippable intro
                detectTapGestures(onTap = {}, onDoubleTap = {}, onLongPress = {})
            }
            .testTag("threejs_intro_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient backdrop + brand emblem while video initializes or in headless test environments
        PlayMixLogoBackdrop(
            progressFraction = progressFraction,
            isFallbackVisible = !videoStarted || videoFallbackActive
        )

        if (!videoFallbackActive) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("intro_video_player"),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        try {
                            val videoView = VideoView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    Gravity.CENTER
                                )
                                setOnTouchListener { _, _ -> true }
                                val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.play_mix_intro}")
                                setVideoURI(uri)
                                setOnPreparedListener { mediaPlayer ->
                                    runCatching {
                                        mediaPlayer.isLooping = false
                                        mediaPlayer.setVideoScalingMode(
                                            android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                                        )
                                    }
                                    videoStarted = true
                                    start()
                                }
                                setOnCompletionListener {
                                    finishSafely()
                                }
                                setOnErrorListener { _, _, _ ->
                                    // If hardware codec fails in a headless/virtual environment, keep showing the animated logo until timer finishes
                                    videoFallbackActive = true
                                    true
                                }
                            }
                            addView(videoView)
                            videoViewRef = videoView
                        } catch (_: Throwable) {
                            videoFallbackActive = true
                        }
                    }
                }
            )
        }

        // Bottom non-skippable progress indicator & badge
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 44.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF0E0E18).copy(alpha = 0.78f),
                modifier = Modifier.border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            ElectricViolet.copy(alpha = 0.55f),
                            SoftCyan.copy(alpha = 0.55f)
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Text(
                    text = "PLAY MIX • INITIALIZING AUDIO ENGINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SoftCyan,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                color = SoftCyan,
                trackColor = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .border(0.5.dp, ElectricViolet.copy(alpha = 0.45f), RoundedCornerShape(99.dp))
                    .testTag("intro_progress_indicator")
            )
        }
    }
}

@Composable
private fun PlayMixLogoBackdrop(
    progressFraction: Float,
    isFallbackVisible: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "intro_logo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF15102E),
                        Color(0xFF050508)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isFallbackVisible) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_play_mix_logo),
                    contentDescription = "Play Mix Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(168.dp)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(38.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(ElectricViolet, SoftCyan)),
                            shape = RoundedCornerShape(38.dp)
                        )
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Play Mix",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftCyan.copy(alpha = 0.85f)
                )
            }
        }
    }
}
