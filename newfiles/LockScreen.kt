package com.replock.app.presentation.lock

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.presentation.components.CameraPreview
import com.replock.app.presentation.components.RepCounterUI
import kotlinx.coroutines.delay
import java.util.Locale
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════════
//  Design tokens
// ═══════════════════════════════════════════════════════════════════
private val ColorBg           = Color(0xFF07070B)
private val ColorSurface      = Color(0xFF0F0F18)
private val ColorCard         = Color(0xFF171722)
private val ColorAccentPurple = Color(0xFF8B6FFF)
private val ColorAccentCyan   = Color(0xFF29D9C2)
private val ColorAccentGreen  = Color(0xFF22D06A)
private val ColorDanger       = Color(0xFFFF4757)
private val ColorTextPrimary  = Color(0xFFF0F0FF)
private val ColorTextMuted    = Color(0xFF5E5E78)

// ═══════════════════════════════════════════════════════════════════
//  LockScreen
// ═══════════════════════════════════════════════════════════════════

/**
 * Full-screen fitness lock overlay.
 *
 * All state is passed in as parameters so this composable has no
 * opinions about the ViewModel wiring — connect it to [LockViewModel]
 * however suits the project.
 */
@Composable
fun LockScreen(
    repCount     : Int     = 0,
    targetReps   : Int     = 20,
    elapsedSecs  : Long    = 0,
    isActive     : Boolean = false,
    isUnlocked   : Boolean = false,
    repState     : String  = "WAITING",
    onStartStop  : () -> Unit = {},
    onQuit       : () -> Unit = {},
    onUnlock    : () -> Unit = {},
    imageAnalysisUseCase: androidx.camera.core.ImageAnalysis? = null,
    currentFrame : LandmarkFrame? = null,
    isFormValid  : Boolean = false,
    feedback     : String = "",
    frameWidth   : Int    = 1,
    frameHeight  : Int    = 1
) {
    val progress = (repCount.toFloat() / targetReps).coerceIn(0f, 1f)

    // ── Ambient glow transitions with active state ──────────────────
    val glowAlpha by animateFloatAsState(
        targetValue   = if (isActive) 0.18f else 0.10f,
        animationSpec = tween(600),
        label         = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
            .drawBehind {
                // Top-centre ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ColorAccentPurple.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = size.width * 0.75f
                    )
                )
                // Bottom-right secondary glow (active = cyan tint)
                if (isActive) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ColorAccentCyan.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.8f, size.height * 0.85f),
                            radius = size.width * 0.5f
                        )
                    )
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // ── Main layout ─────────────────────────────────────────────
        Column(
            modifier             = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // Time display (thin, lock-screen style)
            ClockDisplay()

            Spacer(modifier = Modifier.height(16.dp))

            // App branding row
            BrandingRow(isUnlocked = isUnlocked)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text          = if (isUnlocked) "All reps complete — unlocking device"
                                else "Complete $targetReps push-ups to unlock",
                color         = ColorTextMuted,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.W300,
                letterSpacing = 0.3.sp,
                textAlign     = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Camera preview ──────────────────────────────────────
            CameraPreview(
                modifier  = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                isActive  = isActive,
                repState  = repState,
                imageAnalysisUseCase = imageAnalysisUseCase,
                currentFrame = currentFrame,
                isFormValid  = isFormValid,
                feedback     = feedback,
                frameWidth   = frameWidth,
                frameHeight  = frameHeight
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Rep counter ─────────────────────────────────────────
            RepCounterUI(
                repCount   = repCount,
                targetReps = targetReps,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Progress bar (thin, gradient) ───────────────────────
            ThinProgressBar(progress = progress)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Start / Pause button ────────────────────────────────
            StartButton(
                isActive   = isActive,
                repCount   = repCount,
                onStartStop = onStartStop
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isActive || repCount > 0) {
                TextButton(
                    onClick = onQuit,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text          = "QUIT SESSION",
                        color         = ColorTextMuted,
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.W600,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ── Unlock overlay (slides in over everything) ──────────────
        AnimatedVisibility(
            visible = isUnlocked,
            enter   = fadeIn(tween(400)) + scaleIn(tween(400, easing = FastOutSlowInEasing), initialScale = 0.92f),
            exit    = fadeOut(tween(300))
        ) {
            UnlockOverlay(
                repCount = repCount,
                elapsedSecs = elapsedSecs,
                onUnlock = onUnlock
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Sub-composables
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ClockDisplay() {
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            dateText = java.time.LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                .uppercase()
            delay(1_000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text          = timeText,
            color         = ColorTextPrimary.copy(alpha = 0.25f),
            fontSize      = 52.sp,
            fontWeight    = FontWeight.W100,
            letterSpacing = (-2).sp
        )
        Text(
            text          = dateText,
            color         = ColorTextMuted.copy(alpha = 0.6f),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.W400,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun BrandingRow(isUnlocked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector        = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = null,
            tint               = if (isUnlocked) ColorAccentGreen else ColorAccentPurple,
            modifier           = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text          = "REPLOCK",
            color         = if (isUnlocked) ColorAccentGreen else ColorAccentPurple,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.W700,
            letterSpacing = 4.sp
        )
    }
}

@Composable
private fun ThinProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "progressBar"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .clip(CircleShape)
            .background(ColorCard)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(ColorAccentPurple, ColorAccentCyan)
                        )
                    )
            )
        }
    }
}

@Composable
private fun StartButton(
    isActive: Boolean,
    repCount: Int,
    onStartStop: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue   = if (isActive) ColorDanger.copy(alpha = 0.12f) else ColorAccentPurple,
        animationSpec = tween(300),
        label         = "btnColor"
    )
    val contentColor by animateColorAsState(
        targetValue   = if (isActive) ColorDanger else ColorTextPrimary,
        animationSpec = tween(300),
        label         = "btnContentColor"
    )

    Button(
        onClick  = onStartStop,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = contentColor
        ),
        border   = if (isActive)
            BorderStroke(1.dp, ColorDanger.copy(alpha = 0.4f)) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector        = if (isActive) Icons.Default.Close else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text          = when {
                isActive      -> "PAUSE SESSION"
                repCount > 0  -> "RESUME"
                else          -> "START SESSION"
            },
            fontSize      = 13.sp,
            fontWeight    = FontWeight.W700,
            letterSpacing = 1.5.sp
        )
    }
}

// ─── Unlock overlay ─────────────────────────────────────────────────────────

@Composable
private fun UnlockOverlay(
    repCount: Int,
    elapsedSecs: Long,
    onUnlock: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "unlock_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.2f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val minutes = elapsedSecs / 60
    val seconds = elapsedSecs % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(ColorBg.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Glowing check ring
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .drawBehind {
                        drawCircle(
                            color  = ColorAccentGreen.copy(alpha = pulseAlpha * 0.4f),
                            radius = size.minDimension * 0.65f
                        )
                        drawCircle(
                            color  = ColorAccentGreen.copy(alpha = pulseAlpha * 0.15f),
                            radius = size.minDimension * 0.9f
                        )
                    }
                    .clip(CircleShape)
                    .background(ColorAccentGreen.copy(alpha = 0.12f))
                    .border(1.dp, ColorAccentGreen.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = null,
                    tint               = ColorAccentGreen,
                    modifier           = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text          = "CHALLENGE COMPLETE",
                color         = ColorAccentGreen,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.W800,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text          = "Your device is unlocked",
                color         = ColorTextMuted,
                fontSize      = 13.sp,
                fontWeight    = FontWeight.W300,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rep summary pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text      = repCount.toString(),
                    color     = ColorAccentGreen,
                    fontSize  = 18.sp,
                    fontWeight = FontWeight.W800
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text          = "REPS  ·  $timeFormatted",
                    color         = ColorTextMuted,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.W600,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick  = onUnlock,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = ColorAccentGreen,
                    contentColor   = Color(0xFF07070B)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text          = "OPEN DEVICE",
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.W800,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
