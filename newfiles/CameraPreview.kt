package com.replock.app.presentation.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame

// ─── Design tokens ─────────────────────────────────────────────────────────
private val BgSurface     = Color(0xFF0F0F18)
private val AccentPrimary = Color(0xFF8B6FFF)
private val AccentCyan    = Color(0xFF29D9C2)
private val AccentGreen   = Color(0xFF22D06A)
private val TextMuted     = Color(0xFF5E5E78)

/**
 * Camera viewfinder with tactical corner-bracket overlay, live/standby badge,
 * skeleton pose overlay, and a scanline sweep animation while [isActive].
 *
 * @param frameWidth  Width of the camera frame (rotation-corrected) in pixels.
 *                    Used to compute the FILL_CENTER crop so the skeleton
 *                    lands in the right position on the preview.
 * @param frameHeight Height of the camera frame (rotation-corrected) in pixels.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    repState: String = "WAITING",
    imageAnalysisUseCase: androidx.camera.core.ImageAnalysis? = null,
    currentFrame: LandmarkFrame? = null,
    isFormValid: Boolean = false,
    feedback: String = "",
    frameWidth: Int = 1,
    frameHeight: Int = 1
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cam_anim")

    val scanlineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Box(
        modifier = modifier
            .background(BgSurface, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Camera feed or placeholder
        if (isActive) {
            LiveCameraFeed(
                modifier = Modifier.fillMaxSize(),
                imageAnalysisUseCase = imageAnalysisUseCase
            )
        } else {
            PlaceholderGrid(modifier = Modifier.fillMaxSize())
        }

        // Scanline sweep (active only)
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val y = size.height * scanlineProgress
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    AccentCyan.copy(alpha = 0.35f),
                                    AccentCyan.copy(alpha = 0.55f),
                                    AccentCyan.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            ),
                            start       = Offset(0f, y),
                            end         = Offset(size.width, y),
                            strokeWidth = 2f
                        )
                    }
            )
        }

        // Tactical corner brackets
        val bracketColor = if (isActive) AccentCyan else AccentPrimary.copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val len = 32.dp.toPx()
                    val sw  = 2.5f
                    val p   = 18.dp.toPx()
                    // TL
                    drawLine(bracketColor, Offset(p, p), Offset(p + len, p), strokeWidth = sw, cap = StrokeCap.Square)
                    drawLine(bracketColor, Offset(p, p), Offset(p, p + len), strokeWidth = sw, cap = StrokeCap.Square)
                    // TR
                    drawLine(bracketColor, Offset(size.width - p, p), Offset(size.width - p - len, p), strokeWidth = sw, cap = StrokeCap.Square)
                    drawLine(bracketColor, Offset(size.width - p, p), Offset(size.width - p, p + len), strokeWidth = sw, cap = StrokeCap.Square)
                    // BL
                    drawLine(bracketColor, Offset(p, size.height - p), Offset(p + len, size.height - p), strokeWidth = sw, cap = StrokeCap.Square)
                    drawLine(bracketColor, Offset(p, size.height - p), Offset(p, size.height - p - len), strokeWidth = sw, cap = StrokeCap.Square)
                    // BR
                    drawLine(bracketColor, Offset(size.width - p, size.height - p), Offset(size.width - p - len, size.height - p), strokeWidth = sw, cap = StrokeCap.Square)
                    drawLine(bracketColor, Offset(size.width - p, size.height - p), Offset(size.width - p, size.height - p - len), strokeWidth = sw, cap = StrokeCap.Square)
                }
        )

        // Live / Standby badge (top-right)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) AccentGreen.copy(alpha = dotAlpha)
                        else TextMuted.copy(alpha = 0.6f)
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text          = if (isActive) "LIVE" else "STANDBY",
                color         = if (isActive) AccentGreen else TextMuted,
                fontSize      = 9.sp,
                fontWeight    = FontWeight.W700,
                letterSpacing = 1.5.sp
            )
        }

        // Pose state badge (bottom-left, active only)
        if (isActive) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(AccentCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text          = "POSE DETECTION  ·  $repState",
                    color         = AccentCyan.copy(alpha = 0.9f),
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.W500,
                    letterSpacing = 1.sp
                )
            }
        }

        // ── Skeleton Overlay ──────────────────────────────────────────────────
        if (isActive && currentFrame != null) {
            SkeletonOverlay(
                modifier    = Modifier.fillMaxSize(),
                frame       = currentFrame,
                isFormValid = isFormValid,
                frameWidth  = frameWidth,
                frameHeight = frameHeight
            )
        }

        // ── Feedback Banner ───────────────────────────────────────────────────
        if (isActive && feedback.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isFormValid) AccentGreen.copy(alpha = 0.9f)
                        else Color.Black.copy(alpha = 0.7f)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text       = feedback,
                    color      = if (isFormValid) Color.Black else Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Skeleton overlay ────────────────────────────────────────────────────────

/**
 * Draws the pose skeleton on a Canvas that sits on top of the PreviewView.
 *
 * Landmark coordinates arrive pre-normalised to [0,1] in *camera frame* space.
 * PreviewView uses FILL_CENTER: it scales the frame to cover the composable,
 * then crops the overflow symmetrically.  Without correcting for that crop the
 * skeleton joints are offset — up to ~25% in the vertical axis on a typical
 * 16:9 camera frame displayed in a 3:4-ish preview window.
 *
 * Derivation of the FILL_CENTER mapping
 * ─────────────────────────────────────
 * Let fa = frameWidth/frameHeight, ca = canvasWidth/canvasHeight.
 *
 * Case A  fa < ca  (frame taller than canvas aspect — match width, crop top/bottom)
 *   cropY_norm = (1 − fa/ca) / 2
 *   canvasY    = (frameY − cropY_norm) * (ca/fa)   [clamp to [0,1]]
 *   canvasX    = frameX
 *
 * Case B  fa ≥ ca  (frame wider than canvas aspect — match height, crop sides)
 *   cropX_norm = (1 − ca/fa) / 2
 *   canvasX    = (frameX − cropX_norm) * (fa/ca)
 *   canvasY    = frameY
 */
@Composable
private fun SkeletonOverlay(
    modifier: Modifier,
    frame: LandmarkFrame,
    isFormValid: Boolean,
    frameWidth: Int,
    frameHeight: Int
) {
    Canvas(modifier = modifier) {
        val color = if (isFormValid) AccentGreen else AccentCyan

        val fa = if (frameHeight > 0) frameWidth.toFloat() / frameHeight else 1f
        val ca = if (size.height > 0f) size.width / size.height else 1f

        // Compute the normalised crop offset and stretch factor for each axis
        val cropX: Float
        val cropY: Float
        val stretchX: Float  // how much [0,1] frame space maps to [0,1] canvas space
        val stretchY: Float

        if (fa < ca) {
            // Frame taller → fill width, crop top & bottom
            cropY    = (1f - fa / ca) / 2f
            cropX    = 0f
            stretchX = 1f
            stretchY = ca / fa
        } else {
            // Frame wider (or equal) → fill height, crop left & right
            cropX    = (1f - ca / fa) / 2f
            cropY    = 0f
            stretchX = fa / ca
            stretchY = 1f
        }

        // Map a normalised-frame joint to a canvas pixel Offset
        fun toCanvas(j: Joint): Offset {
            val nx = ((j.x - cropX) * stretchX).coerceIn(0f, 1f)
            val ny = ((j.y - cropY) * stretchY).coerceIn(0f, 1f)
            return Offset(nx * size.width, ny * size.height)
        }

        fun drawBone(j1: Joint?, j2: Joint?) {
            if (j1 == null || j2 == null) return
            drawLine(
                color       = color.copy(alpha = 0.85f),
                start       = toCanvas(j1),
                end         = toCanvas(j2),
                strokeWidth = 6f,
                cap         = StrokeCap.Round
            )
        }

        // Arms
        drawBone(frame.leftShoulder,  frame.leftElbow)
        drawBone(frame.leftElbow,     frame.leftWrist)
        drawBone(frame.rightShoulder, frame.rightElbow)
        drawBone(frame.rightElbow,    frame.rightWrist)

        // Torso
        drawBone(frame.leftShoulder,  frame.rightShoulder)
        drawBone(frame.leftShoulder,  frame.leftHip)
        drawBone(frame.rightShoulder, frame.rightHip)
        drawBone(frame.leftHip,       frame.rightHip)

        // Legs
        drawBone(frame.leftHip,   frame.leftKnee)
        drawBone(frame.leftKnee,  frame.leftAnkle)
        drawBone(frame.rightHip,  frame.rightKnee)
        drawBone(frame.rightKnee, frame.rightAnkle)

        // Joint dots
        listOfNotNull(
            frame.leftShoulder, frame.rightShoulder,
            frame.leftElbow,    frame.rightElbow,
            frame.leftWrist,    frame.rightWrist,
            frame.leftHip,      frame.rightHip,
            frame.leftKnee,     frame.rightKnee,
            frame.leftAnkle,    frame.rightAnkle
        ).forEach { j ->
            drawCircle(
                color  = color,
                radius = 8f,
                center = toCanvas(j)
            )
        }
    }
}

// ─── Private helpers ─────────────────────────────────────────────────────────

@Composable
private fun LiveCameraFeed(
    modifier: Modifier,
    imageAnalysisUseCase: androidx.camera.core.ImageAnalysis? = null
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView    = remember { PreviewView(context) }

    LaunchedEffect(imageAnalysisUseCase) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview  = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        provider.unbindAll()
        if (imageAnalysisUseCase != null) {
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysisUseCase
            )
        } else {
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview
            )
        }
    }

    AndroidView(
        factory  = { previewView },
        modifier = modifier.clip(RoundedCornerShape(20.dp))
    )
}

@Composable
private fun PlaceholderGrid(modifier: Modifier) {
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            // Rule-of-thirds grid
            for (i in 1..2) {
                drawLine(Color.White.copy(alpha = 0.04f), Offset(size.width / 3 * i, 0f), Offset(size.width / 3 * i, size.height), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, size.height / 3 * i), Offset(size.width, size.height / 3 * i), strokeWidth = 1f)
            }
            // Centre crosshair
            val cx = size.width / 2f; val cy = size.height / 2f; val cl = 14.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.08f), Offset(cx - cl, cy), Offset(cx + cl, cy), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.08f), Offset(cx, cy - cl), Offset(cx, cy + cl), strokeWidth = 1f)
            drawCircle(Color.White.copy(alpha = 0.05f), radius = 22.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1f))
        },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.CameraAlt,
                contentDescription = null,
                tint               = TextMuted.copy(alpha = 0.28f),
                modifier           = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text          = "TAP START TO ACTIVATE",
                color         = TextMuted.copy(alpha = 0.22f),
                fontSize      = 9.sp,
                fontWeight    = FontWeight.W500,
                letterSpacing = 2.sp
            )
        }
    }
}
