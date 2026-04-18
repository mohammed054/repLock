package com.replock.app.presentation.exercise

import android.view.HapticFeedbackConstants
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.domain.model.SessionState
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.presentation.components.CameraPreview
import com.replock.app.presentation.components.RepCounterUI

// ── Design tokens ─────────────────────────────────────────────────────────────
private val ColorBg           = Color(0xFF07070B)
private val ColorCard         = Color(0xFF171722)
private val ColorAccentPurple = Color(0xFF8B6FFF)
private val ColorAccentCyan   = Color(0xFF29D9C2)
private val ColorAccentGreen  = Color(0xFF22D06A)
private val ColorDanger       = Color(0xFFFF4757)
private val ColorAmber        = Color(0xFFFFB74D)
private val ColorTextPrimary  = Color(0xFFF0F0FF)
private val ColorTextMuted    = Color(0xFF5E5E78)
private val ColorGold         = Color(0xFFFFD700)

@Composable
fun ExerciseScreen(
    // ── Core rep state ────────────────────────────────────────────────────────
    repCount        : Int          = 0,
    targetReps      : Int          = 20,   // Phase 5.4: driven by config, not hardcoded
    completedSetReps: Int          = 0,
    sessionState    : SessionState = SessionState.STANDBY,
    isActive        : Boolean      = true,
    isCameraReady   : Boolean      = false,
    repState        : String       = "STANDBY",
    elapsedSecs     : Long         = 0L,
    // ── PB (Phase 5.3) ────────────────────────────────────────────────────────
    personalBest    : Int          = 0,
    isNewPb         : Boolean      = false,
    // ── READY flash (Phase 5.2) ───────────────────────────────────────────────
    readyFlash      : Boolean      = false,
    // ── Callbacks ────────────────────────────────────────────────────────────
    onStop          : () -> Unit   = {},
    onBack          : () -> Unit   = {},
    onDismissSet    : () -> Unit   = {},
    onOpenSettings  : () -> Unit   = {},
    // ── Camera ───────────────────────────────────────────────────────────────
    imageAnalysisUseCase: androidx.camera.core.ImageAnalysis? = null,
    currentFrame    : LandmarkFrame? = null,
    isFormValid     : Boolean      = false,
    feedback        : String       = "",
    frameWidth      : Int          = 1,
    frameHeight     : Int          = 1,
    isDebugMode     : Boolean      = false,
    trackingQuality : Float        = 0f,
    cameraFacing    : CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    onFlipCamera    : () -> Unit   = {},
    onToggleOrientation: () -> Unit = {}
) {
    // ── Haptic on every new rep ───────────────────────────────────────────────
    val view = LocalView.current
    LaunchedEffect(repCount) {
        if (repCount > 0) {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    // ── Rep-count scale burst ─────────────────────────────────────────────────
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

    // ── Full-screen green flash on goal completion ────────────────────────────
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
        // ── Camera feed ───────────────────────────────────────────────────────
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

        // ── Bottom scrim ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, ColorBg.copy(alpha = 0.96f)))
                )
        )

        // ── Goal flash overlay ────────────────────────────────────────────────
        if (goalFlashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorAccentGreen.copy(alpha = goalFlashAlpha.value))
            )
        }

        // ── READY flash overlay (Phase 5.2) ───────────────────────────────────
        AnimatedVisibility(
            visible = readyFlash,
            enter   = fadeIn(tween(120)),
            exit    = fadeOut(tween(600))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorAccentGreen.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "GO!",
                    color      = ColorAccentGreen,
                    fontSize   = 72.sp,
                    fontWeight = FontWeight.W900,
                    letterSpacing = 4.sp
                )
            }
        }

        // ── Top bar ───────────────────────────────────────────────────────────
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                    tint = ColorTextPrimary, modifier = Modifier.size(18.dp))
            }

            // Session-state chip (Phase 5.2 — replaces static "STANDBY")
            SessionStateChip(sessionState = sessionState)

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
                        fontSize   = 13.sp, fontWeight = FontWeight.W700
                    )
                    // Phase 5.4: targetReps from config, not hardcoded
                    Text(text = " / $targetReps", color = ColorTextMuted,
                        fontSize = 11.sp, fontWeight = FontWeight.W400)
                }

                IconButton(
                    onClick  = onOpenSettings,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(ColorBg.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings",
                        tint = ColorTextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Bottom panel ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().scale(repBurstScale.value)) {
                RepCounterUI(
                    repCount   = repCount,
                    targetReps = targetReps,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            // ── PB badge (Phase 5.3) — only shown when we have a PB ──────────
            if (personalBest > 0) {
                Spacer(Modifier.height(8.dp))
                PbBadge(personalBest = personalBest, isNewPb = isNewPb)
            }

            Spacer(Modifier.height(20.dp))

            // Phase 5.4: elapsedSecs only ticks in ACTIVE, pace only shown then
            StatRow(
                repCount    = repCount,
                targetReps  = targetReps,
                elapsedSecs = elapsedSecs,
                isActive    = sessionState == SessionState.ACTIVE
            )
        }

        // ── Floating stop/start button ────────────────────────────────────────
        FloatingStopButton(
            isActive = isActive,
            onClick  = onStop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 32.dp)
        )

        // ── SET_COMPLETE summary banner (Phase 5.2) ───────────────────────────
        AnimatedVisibility(
            visible = sessionState == SessionState.SET_COMPLETE,
            enter   = fadeIn() + scaleIn(initialScale = 0.92f),
            exit    = fadeOut() + scaleOut(targetScale = 0.92f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SetCompleteBanner(
                reps         = completedSetReps,
                isNewPb      = isNewPb,
                personalBest = personalBest,
                onDismiss    = onDismissSet
            )
        }
    }
}

// ── SessionStateChip (Phase 5.2) ──────────────────────────────────────────────
@Composable
private fun SessionStateChip(sessionState: SessionState) {
    val (label, color) = when (sessionState) {
        SessionState.STANDBY     -> "STANDBY"      to ColorTextMuted
        SessionState.READY       -> "READY"         to ColorAccentGreen
        SessionState.ACTIVE      -> "ACTIVE"        to ColorDanger
        SessionState.SET_COMPLETE -> "DONE"         to ColorAccentPurple
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ColorBg.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sessionState == SessionState.ACTIVE) {
            val dotAlpha by rememberInfiniteTransition(label = "dot")
                .animateFloat(
                    initialValue  = 0.4f,
                    targetValue   = 1f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label         = "dot_alpha"
                )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ColorDanger.copy(alpha = dotAlpha))
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text          = label,
            color         = color,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.W700,
            letterSpacing = 1.sp
        )
    }
}

// ── PB badge (Phase 5.3) ──────────────────────────────────────────────────────
@Composable
private fun PbBadge(personalBest: Int, isNewPb: Boolean) {
    val color = if (isNewPb) ColorGold else ColorTextMuted
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isNewPb) {
            Text("🏆 ", fontSize = 12.sp)
        }
        Text(
            text       = "PB: $personalBest",
            color      = color,
            fontSize   = 11.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = 0.5.sp
        )
    }
}

// ── SET_COMPLETE banner (Phase 5.2) ───────────────────────────────────────────
@Composable
private fun SetCompleteBanner(
    reps         : Int,
    isNewPb      : Boolean,
    personalBest : Int,
    onDismiss    : () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ColorCard)
            .border(1.dp, ColorAccentPurple.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = "Set Complete!",
            color      = ColorTextPrimary,
            fontSize   = 22.sp,
            fontWeight = FontWeight.W800
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text       = "$reps reps",
            color      = ColorAccentCyan,
            fontSize   = 42.sp,
            fontWeight = FontWeight.W900
        )
        Spacer(Modifier.height(8.dp))

        if (isNewPb) {
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "🏆 New Personal Best!",
                color      = ColorGold,
                fontSize   = 15.sp,
                fontWeight = FontWeight.W700,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        } else if (personalBest > 0) {
            Text(
                text       = "PB: $personalBest",
                color      = ColorTextMuted,
                fontSize   = 12.sp,
                fontWeight = FontWeight.W500
            )
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text       = "Auto-resetting in 3 s…",
            color      = ColorTextMuted,
            fontSize   = 11.sp,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onDismiss) {
            Text(
                text       = "DISMISS",
                color      = ColorAccentPurple,
                fontSize   = 12.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── FloatingStopButton ────────────────────────────────────────────────────────
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
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.08f))))
            .border(1.5.dp, color.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label,
                tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(1.dp))
            Text(text = label, color = color, fontSize = 8.sp,
                fontWeight = FontWeight.W800, letterSpacing = 1.sp)
        }
    }
}

// ── StatRow ───────────────────────────────────────────────────────────────────
@Composable
private fun StatRow(repCount: Int, targetReps: Int, elapsedSecs: Long, isActive: Boolean) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // PACE: only computed when ACTIVE and at least one rep done (Phase 5.4)
        StatCard(
            label    = "PACE",
            value    = if (isActive && elapsedSecs > 0 && repCount > 0)
                           String.format("%.1f /m", repCount / (elapsedSecs / 60.0))
                       else "—",
            accent   = ColorAccentCyan,
            modifier = Modifier.weight(1f)
        )
        // REMAINING: Phase 5.4 — live countdown from config targetReps
        StatCard(
            label    = "REMAINING",
            value    = "${(targetReps - repCount).coerceAtLeast(0)}",
            accent   = ColorAccentPurple,
            modifier = Modifier.weight(1f)
        )
        // ELAPSED: only ticks in ACTIVE (timer started on ACTIVE transition)
        StatCard(
            label    = "ELAPSED",
            value    = formatElapsed(elapsedSecs),
            accent   = ColorAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = ColorTextMuted, fontSize = 8.sp,
            fontWeight = FontWeight.W600, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = value, color = accent, fontSize = 16.sp,
            fontWeight = FontWeight.W700, letterSpacing = (-0.5).sp)
    }
}

private fun formatElapsed(secs: Long): String {
    val m = secs / 60; val s = secs % 60
    return "%02d:%02d".format(m, s)
}
