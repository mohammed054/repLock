package com.replock.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.domain.model.ExerciseType
import com.replock.app.presentation.SettingsState
import com.replock.app.presentation.components.RepLockColors
import com.replock.app.presentation.components.exerciseAccent

private val reminderOptions = listOf(0, 2, 3, 6)

@Composable
fun SettingsScreen(
    state: SettingsState,
    selectedExercise: ExerciseType,
    onSelectExercise: (ExerciseType) -> Unit,
    onTargetRepsChange: (ExerciseType, Int) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onReminderIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RepLockColors.Background),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    color = RepLockColors.TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.W800
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tune sound, reminders, defaults, and daily rep targets for each exercise.",
                    color = RepLockColors.TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        item {
            SettingsSection(title = "Default exercise") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExerciseToggle(
                        exerciseType = ExerciseType.PUSH_UP,
                        selected = selectedExercise == ExerciseType.PUSH_UP,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectExercise(ExerciseType.PUSH_UP) }
                    )
                    ExerciseToggle(
                        exerciseType = ExerciseType.PULL_UP,
                        selected = selectedExercise == ExerciseType.PULL_UP,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectExercise(ExerciseType.PULL_UP) }
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Daily targets") {
                TargetRow(
                    exerciseType = ExerciseType.PUSH_UP,
                    reps = state.pushUpTargetReps,
                    onDecrease = { onTargetRepsChange(ExerciseType.PUSH_UP, state.pushUpTargetReps - 2) },
                    onIncrease = { onTargetRepsChange(ExerciseType.PUSH_UP, state.pushUpTargetReps + 2) }
                )
                Spacer(Modifier.height(12.dp))
                TargetRow(
                    exerciseType = ExerciseType.PULL_UP,
                    reps = state.pullUpTargetReps,
                    onDecrease = { onTargetRepsChange(ExerciseType.PULL_UP, state.pullUpTargetReps - 1) },
                    onIncrease = { onTargetRepsChange(ExerciseType.PULL_UP, state.pullUpTargetReps + 1) }
                )
            }
        }

        item {
            SettingsSection(title = "Experience") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RepLockColors.SurfaceAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = RepLockColors.Teal
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text("Rep sound", color = RepLockColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W700)
                            Text("Play a crisp click for every completed rep.", color = RepLockColors.TextMuted, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = state.soundEnabled,
                        onCheckedChange = onSoundEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = RepLockColors.Teal,
                            uncheckedThumbColor = RepLockColors.TextMuted,
                            uncheckedTrackColor = RepLockColors.SurfaceAlt
                        )
                    )
                }
            }
        }

        item {
            SettingsSection(title = "Reminders") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = RepLockColors.Orange)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "Nudges run during the daytime window and resume automatically after a reboot.",
                        color = RepLockColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    reminderOptions.forEach { hours ->
                        ReminderChip(
                            hours = hours,
                            selected = state.reminderIntervalHours == hours,
                            modifier = Modifier.weight(1f),
                            onClick = { onReminderIntervalChange(hours) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = RepLockColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.W700
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(RepLockColors.Surface)
                .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun ExerciseToggle(
    exerciseType: ExerciseType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = exerciseAccent(exerciseType)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) RepLockColors.SurfaceRaised else RepLockColors.SurfaceAlt)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else RepLockColors.Stroke,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(exerciseType.displayName, color = RepLockColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W700)
        Spacer(Modifier.height(4.dp))
        Text(exerciseType.setupHint, color = RepLockColors.TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun TargetRow(
    exerciseType: ExerciseType,
    reps: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val accent = exerciseAccent(exerciseType)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(exerciseType.displayName, color = RepLockColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W700)
            Text("Daily rep target", color = RepLockColors.TextMuted, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton(icon = Icons.Default.Remove, tint = accent, onClick = onDecrease)
            Text("$reps", color = accent, fontSize = 24.sp, fontWeight = FontWeight.W800)
            StepButton(icon = Icons.Default.Add, tint = accent, onClick = onIncrease)
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(RepLockColors.SurfaceAlt)
            .border(1.dp, RepLockColors.Stroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ReminderChip(
    hours: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (hours == 0) "Off" else "${hours}h"
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) RepLockColors.SurfaceRaised else RepLockColors.SurfaceAlt)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) RepLockColors.Orange else RepLockColors.Stroke,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) RepLockColors.Orange else RepLockColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.W700
        )
    }
}
