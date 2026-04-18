package com.replock.app.presentation.exercise

import android.view.HapticFeedbackConstants
import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.presentation.components.CameraPreview
import com.replock.app.presentation.components.RepCounterUI

private val ColorBg           = Color(0xFF07070B)
private val ColorCard         = Color(0xFF171722)
private val ColorAccentPurple = Color(0xFF8B6FFF)
private val ColorAccentCyan   = Color(0xFF29D9C2)
private val ColorAccentGreen  = Color(0xFF22D06A)
private val ColorDanger       = Color(0xFFFF4757)
private val ColorAmber        = Color(0xFFFFB74D)
private val ColorTextPrimary  = Color(0xFFF0F0FF)
private val ColorTextMuted    = Color(0xFF5E5E78)

@Composable
fun ExerciseScreen(
    repCount    : Int     = 0,
    targetReps  : Int     = 20,
    isActive    : Boolean = true,
    isCameraReady: Boolean = false,
    repState    : String  = "WAITING",
    elapsedSecs : Long    = 0L,
    onStop      : () -> Unit = {},
    onBack      : () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    imageAnalysisUseCase: androidx.camera.core.ImageAnalysis? = null,
    currentFrame  : LandmarkFrame? = null,
    isFormValid   : Boolean = false,
    feedback      : String = "",
    frameWidth    : Int = 1,
    frameHeight   : Int = 1,
    isDebugMode   : Boolean = false,
    trackingQuality: Float = 0f,
    cameraFacing  : CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    onFlipCamera  : () -> Unit = {},
    onToggleOrientation: () -> Unit = {}
) {
    val view = LocalView.current
    LaunchedEffect(repCount) {
        if (repCount > 0) {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    val repBurstScale = remember { Animatable(1f) }
    LaunchedEffect(repCount) {
        if (repCount > 0) {
            repBurstScale.snapTo(1.18f)
            repBurstScale.animateTo(
                targetValue   = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            )
        }
    }

    val goalFlashAlpha = remember { Animatable(0f) }
    var goalFlashFired by remember { mutableStateOf(false) }
    LaunchedEffect(repCount, targetReps) {
        if (repCount >= targetReps && targetReps > 0 && !goalFlashFired) {
            goalFlashFired = true
            goalFlashAlpha.animateTo(0.28f, animationSpec = tween(80))
            goalFlashAlpha.animateTo(0f,    animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
            .statusBarsPadding()
    ) {
        // ── Camera feed ───────────────────────────────────────────────────
        CameraPreview(
            modifier             = Modifier.fillMaxSize(),
            isActive             = isActive,
            isCameraReady        = isCameraReady,
            repState             = repState,
            imageAnalysisUseCase = imageAnalysisUseCase,
            currentFrame         = currentFrame,
            isFormValid          = isFormValid,
            feedback             = feedback,
            frameWidth           = frameWidth,
            frameHeight          = frameHeight,
            isDebugMode          = isDebugMode,
            trackingQuality      = trackingQuality,
            cameraFacing         = cameraFacing,
            onFlipCamera         = onFlipCamera,
            onToggleOrientation  = onToggleOrientation
        )

        // ── Bottom scrim ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ColorBg.copy(alpha = 0.96f))
                    )
                )
        )

        // ── Goal flash overlay ────────────────────────────────────────────
        if (goalFlashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorAccentGreen.copy(alpha = goalFlashAlpha.value))
            )
        }

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ColorBg.copy(alpha = 0.7f))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint     = ColorTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Timer pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorBg.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isActive) ColorDanger else ColorTextMuted)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text          = formatElapsed(elapsedSecs),
                    color         = ColorTextPrimary,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.W600,
                    letterSpacing = 1.sp
                )
            }

            // Rep pill + settings gear
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (repCount > 0) ColorAccentPurple.copy(alpha = 0.2f)
                            else ColorBg.copy(alpha = 0.7f)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "$repCount",
                        color      = if (repCount > 0) ColorAccentCyan else ColorTextMuted,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.W700
                    )
                    Text(
                        text       = " / $targetReps",
                        color      = ColorTextMuted,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.W400
                    )
                }

                IconButton(
                    onClick  = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorBg.copy(alpha = 0.7f))
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint     = ColorTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Bottom panel ──────────────────────────────────────────────────
        // Phase 4.1 fix: the FloatingStopButton is now INSIDE this Column,
        // centred above the stat cards. It can no longer overlap ELAPSED.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rep counter widget
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(repBurstScale.value)
            ) {
                RepCounterUI(
                    repCount   = repCount,
                    targetReps = targetReps,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Floating START / END button — centred, no longer absolute ──
            FloatingStopButton(
                isActive = isActive,
                onClick  = onStop
            )

            Spacer(Modifier.height(16.dp))

            // ── Three equal-width stat cards ──────────────────────────────
            // Phase 4.1 fix: each card gets Modifier.weight(1f) in a Row so
            // they share the width equally and no card text is ever clipped.
            StatRow(
                repCount    = repCount,
                targetReps  = targetReps,
                elapsedSecs = elapsedSecs,
                modifier    = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloatingStopButton(
    isActive : Boolean,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    val label = if (isActive) "END" else "START"
    val color = if (isActive) ColorDanger else ColorAccentGreen
    val icon  = if (isActive) Icons.Default.Close else Icons.Default.PlayArrow

    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.08f))
                )
            )
            .border(1.5.dp, color.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = color,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text          = label,
                color         = color,
                fontSize      = 8.sp,
                fontWeight    = FontWeight.W800,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StatRow(
    repCount: Int,
    targetReps: Int,
    elapsedSecs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label    = "PACE",
            value    = if (elapsedSecs > 0 && repCount > 0)
                           String.format("%.1f /m", repCount / (elapsedSecs / 60.0))
                       else "—",
            accent   = ColorAccentCyan,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label    = "REMAINING",
            value    = "${(targetReps - repCount).coerceAtLeast(0)}",
            accent   = ColorAccentPurple,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label    = "ELAPSED",
            value    = formatElapsed(elapsedSecs),
            accent   = ColorAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label    : String,
    value    : String,
    accent   : Color,
    modifier : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCard)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text          = label,
            color         = ColorTextMuted,
            fontSize      = 8.sp,
            fontWeight    = FontWeight.W600,
            letterSpacing = 1.sp,
            maxLines      = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text          = value,
            color         = accent,
            fontSize      = 16.sp,
            fontWeight    = FontWeight.W700,
            letterSpacing = (-0.5).sp,
            maxLines      = 1
        )
    }
}

private fun formatElapsed(secs: Long): String {
    val m = secs / 60; val s = secs % 60
    return "%02d:%02d".format(m, s)
}
