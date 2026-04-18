package com.replock.app.presentation.settings

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.presentation.onboarding.canChangeDifficulty
import com.replock.app.presentation.onboarding.daysUntilChange
import com.replock.app.system.notification.ReminderScheduler

// ── Shared preferences helpers ────────────────────────────────────────────────

private const val PREFS_NAME      = "replock_prefs"
private const val KEY_SOUND       = "sound_enabled"

fun Context.isSoundEnabled(): Boolean =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_SOUND, true)

fun Context.setSoundEnabled(enabled: Boolean) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_SOUND, enabled).apply()
}

// ── Theme ────────────────────────────────────────────────────────────────────

private val ColorBg           = Color(0xFF07070B)
private val ColorSurface      = Color(0xFF0F0F18)
private val ColorCard         = Color(0xFF171722)
private val ColorBorder       = Color(0xFF1E1E2E)
private val ColorAccentPurple = Color(0xFF8B6FFF)
private val ColorAccentCyan   = Color(0xFF29D9C2)
private val ColorDanger       = Color(0xFFFF4757)
private val ColorTextPrimary  = Color(0xFFF0F0FF)
private val ColorTextMuted    = Color(0xFF5E5E78)

// ── Frequency option model ────────────────────────────────────────────────────

private data class FreqOption(val label: String, val hours: Int)

private val FREQ_OPTIONS = listOf(
    FreqOption("Every 2h", 2),
    FreqOption("Every 3h", 3),
    FreqOption("Every 6h", 6),
    FreqOption("Off",       0)
)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onBack          : () -> Unit,
    onChangeProgram : () -> Unit
) {
    val context      = LocalContext.current
    val canChange    = remember { context.canChangeDifficulty() }
    val daysLeft     = remember { context.daysUntilChange() }

    var intervalHours by remember { mutableIntStateOf(ReminderScheduler.getIntervalHours(context)) }
    var soundEnabled  by remember { mutableStateOf(context.isSoundEnabled()) }

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
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorSurface)
                ) {
                    Icon(
                        imageVector  = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint     = ColorTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Text(
                    text       = "SETTINGS",
                    color      = ColorTextPrimary,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Reminder Frequency ───────────────────────────────────────────
            SettingsSection(title = "REMINDERS") {
                Text(
                    text      = "How often should we nudge you?",
                    color     = ColorTextMuted,
                    fontSize  = 12.sp,
                    lineHeight = 18.sp,
                    modifier  = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FREQ_OPTIONS.forEach { option ->
                        FreqChip(
                            label      = option.label,
                            isSelected = intervalHours == option.hours,
                            modifier   = Modifier.weight(1f),
                            onClick    = {
                                intervalHours = option.hours
                                ReminderScheduler.setIntervalHours(context, option.hours)
                            }
                        )
                    }
                }

                if (intervalHours > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text     = "Active between 7 am – 10 pm",
                        color    = ColorTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Sound ────────────────────────────────────────────────────────
            SettingsSection(title = "AUDIO") {
                SoundToggleRow(
                    label    = "Rep count sound",
                    subtitle = "A short click plays on each completed rep",
                    enabled  = soundEnabled,
                    onToggle = { enabled ->
                        soundEnabled = enabled
                        context.setSoundEnabled(enabled)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Change Program ───────────────────────────────────────────────
            SettingsSection(title = "PROGRAM") {
                if (canChange) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ColorAccentPurple, ColorAccentCyan)
                                )
                            )
                            .clickable { onChangeProgram() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text          = "CHANGE PROGRAM",
                            color         = Color.Black,
                            fontSize      = 13.sp,
                            fontWeight    = FontWeight.W800,
                            letterSpacing = 1.5.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ColorCard)
                            .border(1.dp, ColorBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text       = "Program locked",
                                color      = ColorTextMuted,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.W600
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text     = "Available in $daysLeft day${if (daysLeft == 1) "" else "s"}",
                                color    = ColorTextMuted.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector        = Icons.Default.Lock,
                            contentDescription = null,
                            tint               = ColorTextMuted.copy(alpha = 0.5f),
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title   : String,
    content : @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text          = title,
            color         = ColorTextMuted,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.W700,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(bottom = 12.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ColorSurface)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun FreqChip(
    label      : String,
    isSelected : Boolean,
    modifier   : Modifier = Modifier,
    onClick    : () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) ColorAccentPurple.copy(alpha = 0.18f) else ColorCard,
        animationSpec = tween(200),
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ColorAccentPurple else ColorBorder,
        animationSpec = tween(200),
        label = "chip_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) ColorAccentPurple else ColorTextMuted,
        animationSpec = tween(200),
        label = "chip_text"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 11.sp,
            fontWeight = if (isSelected) FontWeight.W700 else FontWeight.W500,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun SoundToggleRow(
    label    : String,
    subtitle : String,
    enabled  : Boolean,
    onToggle : (Boolean) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text       = label,
                color      = ColorTextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.W600
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = subtitle,
                color    = ColorTextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        Switch(
            checked         = enabled,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = ColorAccentPurple,
                uncheckedThumbColor  = ColorTextMuted,
                uncheckedTrackColor  = ColorCard
            )
        )
    }
}
