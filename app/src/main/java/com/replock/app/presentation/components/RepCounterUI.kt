package com.replock.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Design tokens ─────────────────────────────────────────────────────────
private val AccentPrimary = Color(0xFF8B6FFF)
private val AccentCyan    = Color(0xFF29D9C2)
private val AccentGreen   = Color(0xFF22D06A)
private val TextPrimary   = Color(0xFFF0F0FF)
private val TextMuted     = Color(0xFF5E5E78)
private val BgTrack       = Color(0xFF1A1A28)

// ─── Motivational phase copy ────────────────────────────────────────────────
private fun phaseLabel(repCount: Int, targetReps: Int): String {
    val pct = repCount.toFloat() / targetReps
    return when {
        repCount == 0         -> "GET INTO POSITION"
        pct < 0.25f           -> "FINDING YOUR RHYTHM"
        pct < 0.50f           -> "KEEP PUSHING"
        pct < 0.75f           -> "OVER HALFWAY"
        pct < 0.90f           -> "ALMOST THERE"
        repCount >= targetReps -> "UNLOCKING DEVICE"
        else                  -> "FINAL PUSH"
    }
}

private fun phaseColor(repCount: Int, targetReps: Int): Color {
    val pct = repCount.toFloat() / targetReps
    return when {
        repCount >= targetReps -> AccentGreen
        pct >= 0.75f          -> AccentPrimary
        else                  -> TextMuted
    }
}

/**
 * Rep progress widget.
 *
 * Left column: giant rep count + label.
 * Right column: animated arc ring with percentage inside.
 * Bottom row: five pip indicators spaced across the full width.
 */
@Composable
fun RepCounterUI(
    repCount: Int = 0,
    targetReps: Int = 20,
    modifier: Modifier = Modifier
) {
    val isComplete = repCount >= targetReps
    val progress   = (repCount.toFloat() / targetReps).coerceIn(0f, 1f)

    // Smooth arc animation
    val animatedProgress by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label          = "arcProgress"
    )

    // Count-change bounce on the number
    var prevCount by remember { mutableStateOf(repCount) }
    var isBouncing by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue   = if (isBouncing) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { isBouncing = false },
        label         = "countBounce"
    )
    LaunchedEffect(repCount) {
        if (repCount != prevCount) { isBouncing = true; prevCount = repCount }
    }

    Column(modifier = modifier) {

        // ── Main row: count + ring ──────────────────────────────────────────
        Row(
            modifier            = Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: big number
            Column {
                Text(
                    text          = "REPS COMPLETED",
                    color         = TextMuted,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.W600,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text         = "$repCount",
                        color        = if (isComplete) AccentGreen else TextPrimary,
                        fontSize     = (72f * scaleAnim).sp,
                        fontWeight   = FontWeight.W800,
                        letterSpacing = (-3).sp,
                        lineHeight   = 76.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text         = "/ $targetReps",
                        color        = TextMuted,
                        fontSize     = 20.sp,
                        fontWeight   = FontWeight.W300,
                        modifier     = Modifier.padding(bottom = 12.dp)
                    )
                }

                Text(
                    text          = phaseLabel(repCount, targetReps),
                    color         = phaseColor(repCount, targetReps),
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.W600,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right: arc ring
            Box(
                modifier        = Modifier.size(88.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw    = 6.dp.toPx()
                    val inset = sw / 2f
                    val arc   = Size(size.width - sw, size.height - sw)
                    val tl    = Offset(inset, inset)

                    // Track
                    drawArc(
                        color      = BgTrack,
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter  = false, topLeft = tl, size = arc,
                        style      = Stroke(width = sw, cap = StrokeCap.Round)
                    )

                    // Fill
                    if (animatedProgress > 0f) {
                        drawArc(
                            brush = if (isComplete) SolidColor(AccentGreen)
                                    else Brush.sweepGradient(
                                        colors = listOf(AccentPrimary, AccentCyan, AccentPrimary),
                                        center = center
                                    ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter  = false, topLeft = tl, size = arc,
                            style      = Stroke(width = sw, cap = StrokeCap.Round)
                        )
                    }

                    // Glow dot at tip of arc
                    if (animatedProgress > 0.01f && animatedProgress < 0.99f) {
                        val angleDeg = -90f + 360f * animatedProgress
                        val rad      = Math.toRadians(angleDeg.toDouble())
                        val r        = (size.width - sw) / 2f
                        val tipX     = center.x + r * Math.cos(rad).toFloat()
                        val tipY     = center.y + r * Math.sin(rad).toFloat()
                        drawCircle(AccentCyan.copy(alpha = 0.8f), radius = sw / 2f, center = Offset(tipX, tipY))
                    }
                }

                // Percentage text
                Text(
                    text          = "${(progress * 100).toInt()}%",
                    color         = if (isComplete) AccentGreen else TextPrimary,
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.W700,
                    textAlign     = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Pip row: milestone markers ──────────────────────────────────────
        val milestones = 5
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            for (i in 1..milestones) {
                val threshold = targetReps.toFloat() / milestones * i
                val reached   = repCount >= threshold
                val isCurrent = repCount >= (threshold - targetReps.toFloat() / milestones) && repCount < threshold

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(width = if (isCurrent) 28.dp else 20.dp, height = 4.dp)
                            .then(
                                if (reached || isCurrent) Modifier.drawBehindWithBrush(
                                    Brush.horizontalGradient(listOf(AccentPrimary, AccentCyan))
                                ) else Modifier.drawBehindSolid(BgTrack)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text          = "${(threshold).toInt()}",
                        color         = if (reached) AccentCyan else TextMuted.copy(alpha = 0.5f),
                        fontSize      = 8.sp,
                        fontWeight    = if (reached) FontWeight.W700 else FontWeight.W400,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ─── Draw modifier helpers ──────────────────────────────────────────────────

fun Modifier.drawBehindWithBrush(brush: Brush): Modifier = this.then(
    Modifier.drawWithContent {
        drawRect(brush = brush)
        drawContent()
    }
)

fun Modifier.drawBehindSolid(color: Color): Modifier = this.then(
    Modifier.drawWithContent {
        drawRect(color = color)
        drawContent()
    }
)
