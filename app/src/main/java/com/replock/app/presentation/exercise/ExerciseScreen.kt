package com.replock.app.presentation.exercise

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.presentation.components.CameraPreview
import com.replock.app.presentation.components.RepCounterUI

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
private val ColorAmber        = Color(0xFFFFB74D)
private val ColorTextPrimary  = Color(0xFFF0F0FF)
private val ColorTextMuted    = Color(0xFF5E5E78)

// ═══════════════════════════════════════════════════════════════════
//  ExerciseScreen
// ═══════════════════════════════════════════════════════════════════

/**
 * Active workout screen — shown once the user taps Start.
 * Focused on maximum real-estate for the camera feed and an at-a-glance
 * rep counter. A floating bottom sheet surfaces session stats.
 */
@Composable
fun ExerciseScreen(
    repCount    : Int     = 0,
    targetReps  : Int     = 20,
    isActive    : Boolean = true,
    repState    : String  = "WAITING",
    elapsedSecs : Long    = 0L,
    onStop      : () -> Unit = {},
    onBack      : () -> Unit = {}
) {
    val progress = (repCount.toFloat() / targetReps).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
            .statusBarsPadding()
    ) {

        // ── Camera takes all the vertical space ─────────────────────
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            isActive = isActive,
            repState = repState
        )

        // ── Dimmed gradient overlay at bottom ───────────────────────
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

        // ── Top bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ColorBg.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ColorTextPrimary, modifier = Modifier.size(18.dp))
            }

            // Session timer
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
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text          = formatElapsed(elapsedSecs),
                    color         = ColorTextPrimary,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.W600,
                    letterSpacing = 1.sp
                )
            }

            // Reps badge (quick glance)
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
                    text      = " / $targetReps",
                    color     = ColorTextMuted,
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.W400
                )
            }
        }

        // ── Bottom panel ─────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RepCounterUI(
                repCount   = repCount,
                targetReps = targetReps,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stat row
            StatRow(repCount = repCount, targetReps = targetReps, elapsedSecs = elapsedSecs)

            Spacer(modifier = Modifier.height(20.dp))

            // Stop button
            Button(
                onClick  = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = ColorDanger.copy(alpha = 0.14f),
                    contentColor   = ColorDanger
                ),
                border   = BorderStroke(1.dp, ColorDanger.copy(alpha = 0.35f)),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text          = "END SESSION",
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.W700,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

@Composable
private fun StatRow(repCount: Int, targetReps: Int, elapsedSecs: Long) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label   = "PACE",
            value   = if (elapsedSecs > 0 && repCount > 0) {
                val repsPerMin = repCount / (elapsedSecs / 60.0)
                String.format("%.1f /m", repsPerMin)
            } else "—",
            accent  = ColorAccentCyan,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label   = "REMAINING",
            value   = "${(targetReps - repCount).coerceAtLeast(0)}",
            accent  = ColorAccentPurple,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label   = "ELAPSED",
            value   = formatElapsed(elapsedSecs),
            accent  = ColorAmber,
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
        modifier            = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = ColorTextMuted, fontSize = 8.sp, fontWeight = FontWeight.W600, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.5).sp)
    }
}

private fun formatElapsed(secs: Long): String {
    val m = secs / 60
    val s = secs % 60
    return "%02d:%02d".format(m, s)
}
