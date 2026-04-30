package com.replock.app.presentation.exercise

import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.domain.model.ExerciseType
import com.replock.app.ml.pose.LandmarkFrame
import com.replock.app.presentation.MainViewModel
import com.replock.app.presentation.components.CameraPreview
import com.replock.app.presentation.components.RepCounterUI
import com.replock.app.presentation.components.RepLockColors
import com.replock.app.presentation.components.exerciseAccent

@Composable
fun ExerciseScreen(
    exerciseType: ExerciseType,
    repCount: Int,
    targetReps: Int,
    repState: String,
    elapsedSecs: Long,
    feedback: String,
    formScore: Int,
    isFormValid: Boolean,
    isPoseDetected: Boolean,
    isCameraReady: Boolean,
    imageAnalysisUseCase: androidx.camera.core.ImageAnalysis?,
    currentFrame: LandmarkFrame?,
    frameWidth: Int,
    frameHeight: Int,
    trackingQuality: Float,
    cameraFacing: CameraSelector,
    personalBest: Int,
    todayExerciseReps: Int,
    onFlipCamera: () -> Unit,
    onFinish: () -> Unit
) {
    BackHandler(onBack = onFinish)
    val accent = exerciseAccent(exerciseType)
    val coachingText = if (isPoseDetected && feedback.isNotBlank()) feedback else exerciseType.coachingHint

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RepLockColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RepLockColors.Surface)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RepLockColors.TextPrimary)
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseType.displayName,
                        color = RepLockColors.TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.W800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPoseDetected && feedback.isNotBlank()) feedback else exerciseType.setupHint,
                        color = RepLockColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.size(10.dp))
                IconButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RepLockColors.Surface)
                ) {
                    Icon(Icons.Default.StopCircle, contentDescription = "Finish", tint = RepLockColors.Coral)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RepLockColors.Surface)
            ) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    isActive = true,
                    isCameraReady = isCameraReady,
                    repState = repState,
                    imageAnalysisUseCase = imageAnalysisUseCase,
                    currentFrame = currentFrame,
                    isFormValid = isFormValid,
                    feedback = if (isPoseDetected) feedback else "",
                    frameWidth = frameWidth,
                    frameHeight = frameHeight,
                    isDebugMode = false,
                    trackingQuality = trackingQuality,
                    cameraFacing = cameraFacing,
                    onFlipCamera = onFlipCamera
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiveMetric(label = "Form", value = "$formScore", tint = if (isFormValid) RepLockColors.Lime else RepLockColors.Orange, modifier = Modifier.weight(1f))
                    LiveMetric(label = "Timer", value = MainViewModel.formatDuration(elapsedSecs), tint = RepLockColors.Sky, modifier = Modifier.weight(1f))
                    LiveMetric(label = "Today", value = "${todayExerciseReps + repCount}", tint = accent, modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(RepLockColors.Surface)
                    .border(1.dp, RepLockColors.Stroke, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                RepCounterUI(
                    repCount = repCount,
                    targetReps = targetReps,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusTile("Best", "$personalBest", accent, Modifier.weight(1f))
                    StatusTile("State", repState.replace('_', ' '), RepLockColors.TextPrimary, Modifier.weight(1f))
                    StatusTile(
                        "Camera",
                        if (isCameraReady) "LIVE" else "WARM",
                        if (isCameraReady) RepLockColors.Teal else RepLockColors.TextMuted,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RepLockColors.SurfaceAlt)
                        .padding(12.dp)
                ) {
                    Text("Coaching", color = RepLockColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.W700)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = coachingText,
                        color = RepLockColors.TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.W600,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMetric(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = RepLockColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, color = tint, fontSize = 16.sp, fontWeight = FontWeight.W800, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusTile(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RepLockColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = RepLockColors.TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, color = tint, fontSize = 15.sp, fontWeight = FontWeight.W700, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
