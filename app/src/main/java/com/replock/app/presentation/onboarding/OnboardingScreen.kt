package com.replock.app.presentation.onboarding

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.data.Difficulty
import com.replock.app.system.notification.ReminderScheduler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── SharedPreferences helpers ─────────────────────────────────────────────────

private const val PREFS_NAME      = "replock_prefs"
private const val KEY_DIFFICULTY   = "selected_difficulty"
private const val KEY_LAST_CHANGE  = "difficulty_last_change_ms"

fun Context.getSavedDifficulty(): Difficulty? {
    val name = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_DIFFICULTY, null) ?: return null
    return Difficulty.fromName(name)
}

fun Context.saveDifficulty(difficulty: Difficulty) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_DIFFICULTY, difficulty.name)
        .putLong(KEY_LAST_CHANGE, System.currentTimeMillis())
        .apply()
}

fun Context.canChangeDifficulty(): Boolean {
    val lastChange = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(KEY_LAST_CHANGE, 0L)
    return System.currentTimeMillis() - lastChange > TimeUnit.DAYS.toMillis(7)
}

fun Context.daysUntilChange(): Int {
    val lastChange = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(KEY_LAST_CHANGE, 0L)
    val remaining = TimeUnit.DAYS.toMillis(7) - (System.currentTimeMillis() - lastChange)
    return (TimeUnit.MILLISECONDS.toDays(remaining) + 1).toInt().coerceAtLeast(0)
}

// ── Theme ─────────────────────────────────────────────────────────────────────

private val ColorBg     = Color(0xFF07070B)
private val ColorCard   = Color(0xFF101018)
private val ColorBorder = Color(0xFF1E1E2E)
private val ColorText   = Color(0xFFF0F0FF)
private val ColorMuted  = Color(0xFF5E5E78)

private data class ProgramStyle(
    val color    : Color,
    val glow     : Color,
    val gradient : List<Color>
)

private val programStyles = mapOf(
    "Easy"    to ProgramStyle(Color(0xFF22D06A), Color(0x3322D06A), listOf(Color(0xFF22D06A), Color(0xFF1A9E50))),
    "Medium"  to ProgramStyle(Color(0xFFFFB74D), Color(0x33FFB74D), listOf(Color(0xFFFFB74D), Color(0xFFE07B2A))),
    "Hard"    to ProgramStyle(Color(0xFFFF7043), Color(0x33FF7043), listOf(Color(0xFFFF7043), Color(0xFFDD2C00))),
    "Extreme" to ProgramStyle(Color(0xFFE040FB), Color(0x33E040FB), listOf(Color(0xFFE040FB), Color(0xFF9C00CC)))
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onProgramSelected: (Difficulty) -> Unit) {
    val context        = LocalContext.current
    val canChange      = context.canChangeDifficulty()
    val currentProgram = remember { context.getSavedDifficulty() }
    var selected       by remember { mutableStateOf(currentProgram ?: Difficulty.MEDIUM) }
    val isFirstLaunch  = currentProgram == null

    // ── Infinite shimmer for selected-card particles ──────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "onboard")
    val shimmer by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label         = "shimmer"
    )

    // ── Staggered entry animations ────────────────────────────────────────────
    val headerOffset = remember { Animatable(48f) }
    val headerAlpha  = remember { Animatable(0f) }
    val cardOffsets  = remember { List(Difficulty.ALL.size) { Animatable(72f) } }
    val cardAlphas   = remember { List(Difficulty.ALL.size) { Animatable(0f) } }
    val btnOffset    = remember { Animatable(40f) }
    val btnAlpha     = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Header slides in first
        coroutineScope {
            launch { headerOffset.animateTo(0f, tween(460, easing = FastOutSlowInEasing)) }
            launch { headerAlpha.animateTo(1f,  tween(380)) }
        }
        // Cards stagger in 80 ms apart
        Difficulty.ALL.indices.forEach { i ->
            launch {
                delay(i * 85L)
                coroutineScope {
                    launch { cardOffsets[i].animateTo(0f, tween(430, easing = FastOutSlowInEasing)) }
                    launch { cardAlphas[i].animateTo(1f,  tween(380)) }
                }
            }
        }
        // Button after the last card
        delay(Difficulty.ALL.size * 85L + 120L)
        coroutineScope {
            launch { btnOffset.animateTo(0f, tween(380, easing = FastOutSlowInEasing)) }
            launch { btnAlpha.animateTo(1f,  tween(340)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Header block
            Column(
                modifier = Modifier.graphicsLayer {
                    translationY = headerOffset.value.dp.toPx()
                    alpha        = headerAlpha.value
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    text = when {
                        isFirstLaunch -> "You can change this once per week. Choose wisely."
                        canChange     -> "You can change your program now."
                        else          -> "Program locked for ${context.daysUntilChange()} more day(s)."
                    },
                    color     = ColorMuted,
                    fontSize  = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(40.dp))

            // Program cards
            Difficulty.ALL.forEachIndexed { index, difficulty ->
                val style    = programStyles[difficulty.name] ?: return@forEachIndexed
                val isActive = selected.name == difficulty.name
                val isLocked = !isFirstLaunch && !canChange && currentProgram?.name != difficulty.name

                ProgramCard(
                    difficulty = difficulty,
                    style      = style,
                    isSelected = isActive,
                    isLocked   = isLocked,
                    shimmer    = shimmer,
                    modifier   = Modifier.graphicsLayer {
                        translationY = cardOffsets[index].value.dp.toPx()
                        alpha        = cardAlphas[index].value
                    },
                    onClick    = { if (!isLocked) selected = difficulty }
                )

                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Confirm button
            val canConfirm = isFirstLaunch || canChange || selected.name == currentProgram?.name
            val btnStyle   = programStyles[selected.name] ?: programStyles["Medium"]!!

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .graphicsLayer {
                        translationY = btnOffset.value.dp.toPx()
                        alpha        = btnAlpha.value
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (canConfirm)
                            Modifier.background(Brush.horizontalGradient(btnStyle.gradient))
                        else
                            Modifier
                                .background(ColorCard)
                                .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
                    )
                    .clickable(enabled = canConfirm) {
                        context.saveDifficulty(selected)
                        ReminderScheduler.schedule(context)
                        onProgramSelected(selected)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text          = if (canConfirm) "LET'S GO →" else "LOCKED",
                    color         = if (canConfirm) Color.Black else ColorMuted,
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.W800,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── ProgramCard ───────────────────────────────────────────────────────────────

@Composable
private fun ProgramCard(
    difficulty : Difficulty,
    style      : ProgramStyle,
    isSelected : Boolean,
    isLocked   : Boolean,
    shimmer    : Float,
    modifier   : Modifier = Modifier,
    onClick    : () -> Unit
) {
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0.2f,
        animationSpec = tween(250),
        label         = "border"
    )
    val bgAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.12f else 0.04f,
        animationSpec = tween(250),
        label         = "bg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(style.color.copy(alpha = bgAlpha))
            // Particle glow drawn behind the card when selected
            .then(
                if (isSelected) Modifier.drawBehind {
                    drawParticleGlow(style.color, shimmer, size.width, size.height)
                } else Modifier
            )
            .border(
                width  = if (isSelected) 1.5.dp else 1.dp,
                color  = style.color.copy(alpha = borderAlpha),
                shape  = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLocked, onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLocked) ColorMuted else style.color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = difficulty.displayName,
                        color      = if (isLocked) ColorMuted else style.color,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 1.sp
                    )
                    if (isLocked) {
                        Spacer(Modifier.width(8.dp))
                        Text("🔒", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = difficulty.description,
                    color     = if (isLocked) ColorMuted.copy(alpha = 0.5f) else ColorMuted,
                    fontSize  = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text          = difficulty.tagline,
                    color         = if (isLocked) ColorMuted.copy(alpha = 0.4f) else style.color.copy(alpha = 0.7f),
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.W600,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text          = "${difficulty.targetReps}",
                    color         = if (isLocked) ColorMuted else style.color,
                    fontSize      = 36.sp,
                    fontWeight    = FontWeight.W900,
                    letterSpacing = (-1).sp
                )
                Text(
                    text          = "REPS",
                    color         = if (isLocked) ColorMuted.copy(alpha = 0.5f) else style.color.copy(alpha = 0.6f),
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.W600,
                    letterSpacing = 1.5.sp
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(style.color),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.W900)
            }
        }
    }
}

// ── Particle glow DrawScope helper ────────────────────────────────────────────

/**
 * Draws 10 small glow circles distributed around the card perimeter.
 * Each particle pulses at a different phase offset derived from [shimmer].
 */
private fun DrawScope.drawParticleGlow(
    color   : Color,
    shimmer : Float,
    w       : Float,
    h       : Float
) {
    val particleCount = 10
    val radius        = 4.dp.toPx()
    val outset        = 10.dp.toPx()

    for (i in 0 until particleCount) {
        // Evenly distribute around the perimeter using a parametric approach
        val t        = i.toFloat() / particleCount.toFloat()
        val perimPos = perimeterPoint(t, w, h, outset)

        // Each particle has its own shimmer phase so they don't all pulse together
        val phase = (shimmer + t) % 1f
        val alpha = if (phase < 0.5f) phase * 2f else (1f - phase) * 2f

        drawCircle(
            color  = color.copy(alpha = alpha * 0.55f),
            radius = radius * (0.6f + alpha * 0.8f),
            center = perimPos
        )
    }
}

/** Maps [t] in [0, 1) to a point along the outer perimeter of a rounded rect. */
private fun perimeterPoint(t: Float, w: Float, h: Float, outset: Float): Offset {
    val perimeter = 2f * (w + h)
    val dist      = t * perimeter
    return when {
        dist < w           -> Offset(dist, -outset)                       // top edge →
        dist < w + h       -> Offset(w + outset, dist - w)               // right edge ↓
        dist < 2f * w + h  -> Offset(w - (dist - w - h), h + outset)    // bottom edge ←
        else               -> Offset(-outset, h - (dist - 2f * w - h))  // left edge ↑
    }
}
