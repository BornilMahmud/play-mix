package com.example.feature.onboarding

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SoftCyan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
 * Lightweight, self-contained 3D Audio Sphere & Sound-Wave Intro Overlay used ONLY for the initial
 * startup transition.
 *
 * Uses a hardware-accelerated native 3D perspective renderer (matching the bundled
 * `assets/intro/three.min.js` + `assets/intro/intro.html` scene geometry) by default so that
 * containerized/virtualized WebView renderer processes (`aw_browser_terminator.cc`) never crash
 * or allocate multiprocess Chromium overhead during app launch. Also includes a hardened
 * `onRenderProcessGone`-protected WebView loader when `preferWebViewRenderer` is explicitly enabled
 * on non-virtualized physical devices.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeJsIntroOverlay(
    isShortTransition: Boolean,
    onIntroFinished: () -> Unit,
    preferWebViewRenderer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val canUseWebViewSafely = remember(preferWebViewRenderer) {
        preferWebViewRenderer && !isVirtualizedOrEmulatorEnvironment()
    }
    var useNative3DEngine by remember { mutableStateOf(!canUseWebViewSafely) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var completed by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableFloatStateOf(0f) }

    fun disposeWebViewSafely(webView: WebView?) {
        if (webView == null) return
        runCatching {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.webChromeClient = null
            webView.removeAllViews()
            webView.destroy()
        }
    }

    fun finishSafely() {
        if (completed) return
        completed = true
        val targetWebView = webViewRef
        webViewRef = null
        disposeWebViewSafely(targetWebView)
        onIntroFinished()
    }

    // Smoothly animate progress and auto-complete after duration
    LaunchedEffect(isShortTransition) {
        val totalDurationMs = if (isShortTransition) 1250L else 2600L
        val stepMs = 16L
        var elapsed = 0L
        while (elapsed < totalDurationMs && !completed) {
            progressFraction = (elapsed.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            delay(stepMs)
            elapsed += stepMs
        }
        progressFraction = 1f
        finishSafely()
    }

    DisposableEffect(Unit) {
        onDispose {
            val targetWebView = webViewRef
            webViewRef = null
            disposeWebViewSafely(targetWebView)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06060A))
            .testTag("threejs_intro_overlay")
    ) {
        if (!useNative3DEngine) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    try {
                        WebView(ctx).apply {
                            setBackgroundColor(AndroidColor.parseColor("#06060A"))
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            settings.apply {
                                javaScriptEnabled = true
                                allowFileAccess = true
                                domStorageEnabled = false
                                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                                mediaPlaybackRequiresUserGesture = false
                            }
                            addJavascriptInterface(
                                PlayMixIntroBridge { finishSafely() },
                                "PlayMixIntroBridge"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    useNative3DEngine = true
                                    disposeWebViewSafely(view)
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    // Returning true prevents Android aw_browser_terminator from killing the app
                                    useNative3DEngine = true
                                    webViewRef = null
                                    disposeWebViewSafely(view)
                                    return true
                                }
                            }
                            val query = if (isShortTransition) "?short=1" else "?short=0"
                            loadUrl("file:///android_asset/intro/intro.html$query")
                            webViewRef = this
                        }
                    } catch (_: Throwable) {
                        useNative3DEngine = true
                        View(ctx)
                    }
                }
            )
        }

        if (useNative3DEngine) {
            DirectThreeJs3DSceneCanvas(
                progressFraction = progressFraction
            )
        }

        // Skip Intro button (always accessible at top-right)
        OutlinedButton(
            onClick = { finishSafely() },
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .testTag("skip_threejs_intro_btn")
        ) {
            Text("Skip", color = SoftCyan)
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip Intro",
                tint = SoftCyan
            )
        }
    }
}

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class Particle3D(
    val basePos: Vec3,
    val color: Color,
    val radiusPx: Float
)

/**
 * Renders the 3D Wireframe Icosahedron Audio Core, 3 Tilted Concentric Sound-Wave Rings,
 * and 3D Perspective Particle Field directly using 3D rotation & perspective projection matrices.
 * Matches the visual design of `assets/intro/intro.html` + `assets/intro/three.min.js` with zero
 * WebView multiprocess overhead.
 */
@Composable
private fun DirectThreeJs3DSceneCanvas(
    progressFraction: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "threejs_3d_scene")
    val anglePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle_phase"
    )
    val beatPulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beat_pulse"
    )

    // Precompute Golden Ratio Icosahedron vertices & wireframe edges (12 vertices, 30 edges)
    val icosahedronData = remember {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
        val rawVertices = listOf(
            Vec3(-1f, t, 0f), Vec3(1f, t, 0f), Vec3(-1f, -t, 0f), Vec3(1f, -t, 0f),
            Vec3(0f, -1f, t), Vec3(0f, 1f, t), Vec3(0f, -1f, -t), Vec3(0f, 1f, -t),
            Vec3(t, 0f, -1f), Vec3(t, 0f, 1f), Vec3(-t, 0f, -1f), Vec3(-t, 0f, 1f)
        ).map { v ->
            val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
            Vec3(v.x / len * 1.15f, v.y / len * 1.15f, v.z / len * 1.15f)
        }

        val faces = listOf(
            intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
            intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
            intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
            intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1)
        )

        val edgeSet = LinkedHashSet<Pair<Int, Int>>()
        for (f in faces) {
            val pairs = listOf(f[0] to f[1], f[1] to f[2], f[2] to f[0])
            for ((a, b) in pairs) {
                val minIdx = minOf(a, b)
                val maxIdx = maxOf(a, b)
                edgeSet.add(minIdx to maxIdx)
            }
        }
        rawVertices to edgeSet.toList()
    }

    // Precompute 3D particle cloud
    val particles = remember {
        List(72) { idx ->
            val theta = (idx * 2.3999632f) // golden angle
            val phi = ((idx % 18) / 18f) * PI.toFloat()
            val radius = 1.65f + (idx % 7) * 0.24f
            val x = radius * sin(phi) * cos(theta)
            val y = radius * cos(phi) * 0.85f
            val z = radius * sin(phi) * sin(theta)
            Particle3D(
                basePos = Vec3(x, y, z),
                color = if (idx % 2 == 0) SoftCyan else ElectricViolet,
                radiusPx = if (idx % 3 == 0) 4.2f else 2.8f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF15102E),
                        Color(0xFF06060A)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.43f
            val focalLength = size.minDimension * 0.82f
            val cameraZ = 4.3f

            fun rotate3D(v: Vec3, rx: Float, ry: Float, rz: Float, scale: Float = 1f): Vec3 {
                var x = v.x * scale
                var y = v.y * scale
                var z = v.z * scale

                // Rotate X
                val cosX = cos(rx)
                val sinX = sin(rx)
                val y1 = y * cosX - z * sinX
                val z1 = y * sinX + z * cosX
                y = y1
                z = z1

                // Rotate Y
                val cosY = cos(ry)
                val sinY = sin(ry)
                val x2 = x * cosY + z * sinY
                val z2 = -x * sinY + z * cosY
                x = x2
                z = z2

                // Rotate Z
                val cosZ = cos(rz)
                val sinZ = sin(rz)
                val x3 = x * cosZ - y * sinZ
                val y3 = x * sinZ + y * cosZ
                return Vec3(x3, y3, z)
            }

            fun project(v: Vec3): Pair<Offset, Float> {
                val depth = (cameraZ - v.z).coerceAtLeast(0.6f)
                val perspective = focalLength / depth
                return Offset(cx + v.x * perspective, cy - v.y * perspective) to perspective
            }

            // 1. Draw 3D Particle Field
            particles.forEach { p ->
                val rotated = rotate3D(
                    v = p.basePos,
                    rx = anglePhase * 0.25f,
                    ry = -anglePhase * 0.45f,
                    rz = anglePhase * 0.15f
                )
                val (screenPt, _) = project(rotated)
                val depthAlpha = ((cameraZ - rotated.z) / 6.5f).coerceIn(0.18f, 0.85f)
                drawCircle(
                    color = p.color.copy(alpha = depthAlpha),
                    radius = p.radiusPx,
                    center = screenPt
                )
            }

            // 2. Draw 3 Concentric 3D Tilted Sound-Wave Rings
            val ringRadii = listOf(1.55f, 1.98f, 2.42f)
            val ringColors = listOf(SoftCyan, ElectricViolet, SoftCyan)
            ringRadii.forEachIndexed { rIdx, radius ->
                val segments = 56
                val ringPath = Path()
                val ringScale = 1f + 0.06f * sin(anglePhase * 2f + rIdx)
                for (i in 0..segments) {
                    val theta = (i.toFloat() / segments) * (2f * PI.toFloat())
                    val rawPt = Vec3(
                        x = radius * cos(theta),
                        y = radius * sin(theta),
                        z = 0.12f * sin(theta * 4f + anglePhase * 2f)
                    )
                    val rotated = rotate3D(
                        v = rawPt,
                        rx = 1.08f + 0.18f * sin(anglePhase + rIdx),
                        ry = anglePhase * (if (rIdx % 2 == 0) 0.55f else -0.45f),
                        rz = rIdx * 0.5f,
                        scale = ringScale
                    )
                    val (pt, _) = project(rotated)
                    if (i == 0) ringPath.moveTo(pt.x, pt.y) else ringPath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = ringPath,
                    color = ringColors[rIdx].copy(alpha = 0.42f - rIdx * 0.08f),
                    style = Stroke(width = (3.4f - rIdx * 0.6f), cap = StrokeCap.Round)
                )
            }

            // 3. Draw 3D Wireframe Icosahedron Audio Core
            val (vertices, edges) = icosahedronData
            val transformedVertices = vertices.map { v ->
                rotate3D(
                    v = v,
                    rx = anglePhase * 0.85f,
                    ry = anglePhase * 1.15f,
                    rz = anglePhase * 0.4f,
                    scale = beatPulse
                )
            }
            val projectedVertices = transformedVertices.map { project(it).first }

            // Inner glowing core aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.42f),
                        SoftCyan.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.30f * beatPulse
                ),
                radius = size.minDimension * 0.30f * beatPulse,
                center = Offset(cx, cy)
            )

            edges.forEach { (i, j) ->
                val avgZ = (transformedVertices[i].z + transformedVertices[j].z) * 0.5f
                val edgeAlpha = ((avgZ + 1.5f) / 3.0f).coerceIn(0.28f, 0.95f)
                val edgeColor = if ((i + j) % 2 == 0) ElectricViolet else SoftCyan
                drawLine(
                    color = edgeColor.copy(alpha = edgeAlpha),
                    start = projectedVertices[i],
                    end = projectedVertices[j],
                    strokeWidth = 3.2f,
                    cap = StrokeCap.Round
                )
            }

            projectedVertices.forEachIndexed { idx, pt ->
                val vAlpha = ((transformedVertices[idx].z + 1.5f) / 3.0f).coerceIn(0.4f, 1f)
                drawCircle(
                    color = SoftCyan.copy(alpha = vAlpha),
                    radius = 5.2f,
                    center = pt
                )
            }
        }

        // Bottom Brand Overlay & Loading Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 64.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PLAY MIX",
                style = MaterialTheme.typography.headlineLarge.copy(
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
            Text(
                text = "HIGH-PRECISION ANDROID AUDIO ENGINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.4.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = SoftCyan.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                color = SoftCyan,
                trackColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .border(0.5.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
            )
        }
    }
}

private fun isVirtualizedOrEmulatorEnvironment(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val hardware = Build.HARDWARE.lowercase()
    val brand = Build.BRAND.lowercase()
    return fingerprint.startsWith("generic") ||
        fingerprint.contains("emulator") ||
        fingerprint.contains("sdk_gphone") ||
        model.contains("sdk") ||
        model.contains("emulator") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        hardware.contains("ranchu") ||
        hardware.contains("goldfish") ||
        hardware.contains("cuttlefish") ||
        brand.startsWith("generic")
}
