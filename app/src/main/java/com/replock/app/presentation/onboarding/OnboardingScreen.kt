package com.replock.app.presentation.onboarding

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.data.Difficulty
import java.util.concurrent.TimeUnit

private const val PREFS_NAME     = "replock_prefs"
private const val KEY_DIFFICULTY  = "selected_difficulty"
private const val KEY_LAST_CHANGE = "difficulty_last_change_ms"

fun Context.getSavedDifficulty(): Difficulty? {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val name  = prefs.getString(KEY_DIFFICULTY, null) ?: return null
    return Difficulty.fromName(name)
}

fun Context.saveDifficulty(difficulty: Difficulty) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_DIFFICULTY, difficulty.name)
        .putLong(KEY_LAST_CHANGE, System.currentTimeMillis())
        .apply()
}

fun Context.canChangeDifficulty(): Boolean {
    val prefs      = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastChange = prefs.getLong(KEY_LAST_CHANGE, 0L)
    val elapsed    = System.currentTimeMillis() - lastChange
    return elapsed > TimeUnit.DAYS.toMillis(7)
}

fun Context.daysUntilChange(): Int {
    val prefs      = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastChange = prefs.getLong(KEY_LAST_CHANGE, 0L)
    val elapsed    = System.currentTimeMillis() - lastChange
    val remaining  = TimeUnit.DAYS.toMillis(7) - elapsed
    return (TimeUnit.MILLISECONDS.toDays(remaining) + 1).toInt().coerceAtLeast(0)
}

private val ColorBg       = Color(0xFF07070B)
private val ColorCard     = Color(0xFF101018)
private val ColorBorder   = Color(0xFF1E1E2E)
private val ColorText     = Color(0xFFF0F0FF)
private val ColorMuted    = Color(0xFF5E5E78)

private data class ProgramStyle(val color: Color, val glow: Color, val gradient: List<Color>)

private val programStyles = mapOf(
    "Easy"    to ProgramStyle(Color(0xFF22D06A), Color(0x3322D06A), listOf(Color(0xFF22D06A), Color(0xFF1A9E50))),
    "Medium"  to ProgramStyle(Color(0xFFFFB74D), Color(0x33FFB74D), listOf(Color(0xFFFFB74D), Color(0xFFE07B2A))),
    "Hard"    to ProgramStyle(Color(0xFFFF7043), Color(0x33FF7043), listOf(Color(0xFFFF7043), Color(0xFFDD2C00))),
    "Extreme" to ProgramStyle(Color(0xFFE040FB), Color(0x33E040FB), listOf(Color(0xFFE040FB), Color(0xFF9C00CC)))
)

@Composable
fun OnboardingScreen(onProgramSelected: (Difficulty) -> Unit) {
    val context        = LocalContext.current
    val canChange      = context.canChangeDifficulty()
    val currentProgram = remember { context.getSavedDifficulty() }
    var selected       by remember { mutableStateOf(currentProgram ?: Difficulty.MEDIUM) }
    val isFirstLaunch  = currentProgram == null

    val infiniteTransition = rememberInfiniteTransition(label = "onboard")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer"
    )

    Box(modifier = Modifier.fillMaxSize().background(ColorBg).statusBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text          = if (isFirstLaunch) "CHOOSE YOUR\nPROGRAM" else "YOUR PROGRAM",
                color         = ColorText,
                fontSize      = 32.sp,
                fontWeight    = FontWeight.W900,
                textAlign     = TextAlign.Center,
                letterSpacing = (-1).sp,
                lineHeight    = 36.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text       = if (isFirstLaunch) "You can change this once per week. Choose wisely." else if (canChange) "You can change your program now." else "Program locked for ${context.daysUntilChange()} more day(s).",
                color      = ColorMuted,
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(40.dp))

            Difficulty.ALL.forEach { difficulty ->
                val style     = programStyles[difficulty.name] ?: return@forEach
                val isActive  = selected.name == difficulty.name
                val isLocked  = !isFirstLaunch && !canChange && currentProgram?.name != difficulty.name

                ProgramCard(difficulty, style, isActive, isLocked, shimmer, onClick = { if (!isLocked) selected = difficulty })

                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(32.dp))

            val canConfirm = isFirstLaunch || canChange || selected.name == currentProgram?.name
            val btnStyle   = programStyles[selected.name] ?: programStyles["Medium"]!!

            Box(
                modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp))
                    .then(if (canConfirm) Modifier.background(Brush.horizontalGradient(btnStyle.gradient)) else Modifier.background(ColorCard).border(1.dp, ColorBorder, RoundedCornerShape(16.dp)))
                    .clickable(enabled = canConfirm) { context.saveDifficulty(selected); onProgramSelected(selected) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (canConfirm) "LET'S GO →" else "LOCKED", color = if (canConfirm) Color.Black else ColorMuted, fontSize = 15.sp, fontWeight = FontWeight.W800, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProgramCard(difficulty: Difficulty, style: ProgramStyle, isSelected: Boolean, isLocked: Boolean, shimmer: Float, onClick: () -> Unit) {
    val borderAlpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.2f, animationSpec = tween(250), label = "border")
    val bgAlpha by animateFloatAsState(targetValue = if (isSelected) 0.12f else 0.04f, animationSpec = tween(250), label = "bg")

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(style.color.copy(alpha = bgAlpha))
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = style.color.copy(alpha = borderAlpha), shape = RoundedCornerShape(16.dp))
            .clickable(enabled = !isLocked, onClick = onClick)
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (isLocked) ColorMuted else style.color))
                    Spacer(Modifier.width(10.dp))
                    Text(text = difficulty.displayName, color = if (isLocked) ColorMuted else style.color, fontSize = 18.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp)
                    if (isLocked) { Spacer(Modifier.width(8.dp)); Text("🔒", fontSize = 12.sp) }
                }

                Spacer(Modifier.height(4.dp))
                Text(text = difficulty.description, color = if (isLocked) ColorMuted.copy(alpha = 0.5f) else ColorMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(text = difficulty.tagline, color = if (isLocked) ColorMuted.copy(alpha = 0.4f) else style.color.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${difficulty.targetReps}", color = if (isLocked) ColorMuted else style.color, fontSize = 36.sp, fontWeight = FontWeight.W900, letterSpacing = (-1).sp)
                Text(text = "REPS", color = if (isLocked) ColorMuted.copy(alpha = 0.5f) else style.color.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.W600, letterSpacing = 1.5.sp)
            }
        }

        if (isSelected) {
            Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clip(androidx.compose.foundation.shape.CircleShape).background(style.color), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.W900)
            }
        }
    }
}