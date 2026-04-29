package com.replock.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import com.replock.app.presentation.DashboardState
import com.replock.app.presentation.SessionHistoryItem
import com.replock.app.presentation.components.RepLockColors
import com.replock.app.presentation.components.exerciseAccent

@Composable
fun HistoryScreen(
    dashboardState: DashboardState,
    history: List<SessionHistoryItem>,
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
                    text = "History",
                    color = RepLockColors.TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.W800
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Review weekly volume, current momentum, and each saved set.",
                    color = RepLockColors.TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SnapshotTile(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Current streak",
                    value = "${dashboardState.currentStreak} d",
                    tint = RepLockColors.Orange,
                    modifier = Modifier.weight(1f)
                )
                SnapshotTile(
                    icon = Icons.Default.EmojiEvents,
                    label = "Longest streak",
                    value = "${dashboardState.longestStreak} d",
                    tint = RepLockColors.Lime,
                    modifier = Modifier.weight(1f)
                )
                SnapshotTile(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    label = "Weekly reps",
                    value = "${dashboardState.weeklyReps}",
                    tint = RepLockColors.Sky,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RepLockColors.Surface)
                    .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Weekly volume",
                    color = RepLockColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Last seven days of completed reps across all exercises.",
                    color = RepLockColors.TextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(20.dp))
                WeeklyVolumeChart(dashboardState = dashboardState)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saved sessions",
                    color = RepLockColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700
                )
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RepLockColors.Surface)
                            .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
                            .padding(18.dp)
                    ) {
                        Text("No workout history yet.", color = RepLockColors.TextMuted, fontSize = 13.sp)
                    }
                } else {
                    history.forEach { item ->
                        HistoryRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
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
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.height(12.dp))
        Text(label, color = RepLockColors.TextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = RepLockColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.W800)
    }
}

@Composable
private fun WeeklyVolumeChart(dashboardState: DashboardState) {
    val maxReps = dashboardState.weeklyVolume.maxOfOrNull { it.reps }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        dashboardState.weeklyVolume.forEach { day ->
            val heightFraction = (day.reps.toFloat() / maxReps).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text("${day.reps}", color = RepLockColors.TextMuted, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFraction.coerceAtLeast(0.08f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (day.isToday) RepLockColors.Teal else RepLockColors.SurfaceRaised)
                )
                Spacer(Modifier.height(8.dp))
                Text(day.dayLabel, color = RepLockColors.TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun HistoryRow(item: SessionHistoryItem) {
    val accent = exerciseAccent(item.exerciseType)
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
            Text(item.exerciseType.displayName, color = RepLockColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W700)
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.dayLabel}  |  ${item.durationLabel}  |  Form ${item.formScore}",
                color = RepLockColors.TextMuted,
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${item.reps}", color = accent, fontSize = 24.sp, fontWeight = FontWeight.W800)
            Text(
                text = if (item.completedGoal) "goal completed" else "target ${item.targetReps}",
                color = RepLockColors.TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
