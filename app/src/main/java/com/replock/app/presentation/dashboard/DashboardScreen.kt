package com.replock.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
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
import com.replock.app.presentation.DashboardState
import com.replock.app.presentation.SessionHistoryItem
import com.replock.app.presentation.components.RepLockColors
import com.replock.app.presentation.components.exerciseAccent
import com.replock.app.presentation.components.exerciseGradient

@Composable
fun DashboardScreen(
    state: DashboardState,
    onSelectExercise: (ExerciseType) -> Unit,
    onStartWorkout: () -> Unit,
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
                    text = "REPLOCK",
                    color = RepLockColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Train with intent.",
                    color = RepLockColors.TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.W800
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Live rep tracking, exercise streaks, weekly volume, and clean session history in one place.",
                    color = RepLockColors.TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroChip(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Current streak",
                    value = "${state.currentStreak} d",
                    tint = RepLockColors.Orange,
                    modifier = Modifier.weight(1f)
                )
                HeroChip(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    label = "Weekly reps",
                    value = "${state.weeklyReps}",
                    tint = RepLockColors.Teal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Exercises",
                    color = RepLockColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExerciseCard(
                        exerciseType = ExerciseType.PUSH_UP,
                        selected = state.selectedExercise == ExerciseType.PUSH_UP,
                        targetReps = state.pushUpTargetReps,
                        progressValue = state.todayPushUps,
                        onClick = { onSelectExercise(ExerciseType.PUSH_UP) },
                        modifier = Modifier.weight(1f)
                    )
                    ExerciseCard(
                        exerciseType = ExerciseType.PULL_UP,
                        selected = state.selectedExercise == ExerciseType.PULL_UP,
                        targetReps = state.pullUpTargetReps,
                        progressValue = state.todayPullUps,
                        onClick = { onSelectExercise(ExerciseType.PULL_UP) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            FocusBand(
                state = state,
                onStartWorkout = onStartWorkout
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    icon = Icons.Default.CalendarToday,
                    label = "Sessions",
                    value = "${state.weeklySessions}",
                    caption = "Last 7 days",
                    tint = RepLockColors.Sky,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.FitnessCenter,
                    label = "Total reps",
                    value = "${state.totalReps}",
                    caption = "Lifetime",
                    tint = RepLockColors.Lime,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.Timer,
                    label = "Best run",
                    value = "${state.longestStreak} d",
                    caption = "Longest streak",
                    tint = RepLockColors.Coral,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Personal bests",
                    color = RepLockColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PersonalBestTile(
                        title = ExerciseType.PUSH_UP.displayName,
                        reps = state.personalBestPushUps,
                        tint = exerciseAccent(ExerciseType.PUSH_UP),
                        modifier = Modifier.weight(1f)
                    )
                    PersonalBestTile(
                        title = ExerciseType.PULL_UP.displayName,
                        reps = state.personalBestPullUps,
                        tint = exerciseAccent(ExerciseType.PULL_UP),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Recent work",
                    color = RepLockColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700
                )
                if (state.recentSessions.isEmpty()) {
                    EmptyStatePanel("Start a session and your history will land here.")
                } else {
                    state.recentSessions.forEach { session ->
                        RecentSessionRow(session = session)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RepLockColors.Surface)
            .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = RepLockColors.TextMuted, fontSize = 11.sp)
            Text(value, color = RepLockColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.W700)
        }
    }
}

@Composable
private fun ExerciseCard(
    exerciseType: ExerciseType,
    selected: Boolean,
    targetReps: Int,
    progressValue: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = exerciseAccent(exerciseType)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) RepLockColors.SurfaceRaised else RepLockColors.Surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else RepLockColors.Stroke,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = exerciseType.displayName,
            color = RepLockColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.W700
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = exerciseType.setupHint,
            color = RepLockColors.TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "${progressValue.coerceAtLeast(0)} / $targetReps",
            color = accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.W800
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(RepLockColors.SurfaceAlt)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((progressValue.toFloat() / targetReps.coerceAtLeast(1)).coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(exerciseGradient(exerciseType))
            )
        }
    }
}

@Composable
private fun FocusBand(
    state: DashboardState,
    onStartWorkout: () -> Unit
) {
    val accent = exerciseAccent(state.selectedExercise)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RepLockColors.Surface)
            .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "Today's focus",
            color = RepLockColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.W700
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${state.selectedExercise.displayName} session",
            color = RepLockColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.W800
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.selectedExercise.coachingHint,
            color = RepLockColors.TextMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Target", color = RepLockColors.TextMuted, fontSize = 11.sp)
                Text("${state.targetReps} reps", color = accent, fontSize = 26.sp, fontWeight = FontWeight.W800)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(exerciseGradient(state.selectedExercise))
                    .clickable(onClick = onStartWorkout)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start live session", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.W800)
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    caption: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RepLockColors.Surface)
            .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(12.dp))
        Text(label, color = RepLockColors.TextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = RepLockColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.W800)
        Spacer(Modifier.height(2.dp))
        Text(caption, color = RepLockColors.TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun PersonalBestTile(
    title: String,
    reps: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RepLockColors.Surface)
            .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = RepLockColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.W700)
            Spacer(Modifier.height(4.dp))
            Text("Best completed set", color = RepLockColors.TextMuted, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$reps", color = tint, fontSize = 28.sp, fontWeight = FontWeight.W800)
            Spacer(Modifier.width(4.dp))
            Text("reps", color = RepLockColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun RecentSessionRow(session: SessionHistoryItem) {
    val tint = exerciseAccent(session.exerciseType)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RepLockColors.Surface)
            .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.exerciseType.displayName, color = RepLockColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W700)
            Spacer(Modifier.height(3.dp))
            Text("${session.dayLabel}  |  ${session.durationLabel}", color = RepLockColors.TextMuted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${session.reps}", color = tint, fontSize = 24.sp, fontWeight = FontWeight.W800)
            Text(
                text = if (session.completedGoal) "goal hit" else "target ${session.targetReps}",
                color = RepLockColors.TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EmptyStatePanel(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RepLockColors.Surface)
            .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
            .padding(18.dp)
    ) {
        Text(message, color = RepLockColors.TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
