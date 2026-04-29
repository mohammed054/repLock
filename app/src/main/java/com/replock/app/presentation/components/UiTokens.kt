package com.replock.app.presentation.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.replock.app.domain.model.ExerciseType

object RepLockColors {
    val Background = Color(0xFF061015)
    val Surface = Color(0xFF101B21)
    val SurfaceAlt = Color(0xFF17262E)
    val SurfaceRaised = Color(0xFF20313A)
    val Stroke = Color(0xFF2E434E)
    val TextPrimary = Color(0xFFF3F8FB)
    val TextMuted = Color(0xFF8DA1AD)
    val Teal = Color(0xFF1FD3B0)
    val Sky = Color(0xFF60B8FF)
    val Lime = Color(0xFFB4E33D)
    val Orange = Color(0xFFFFA94D)
    val Coral = Color(0xFFFF6B5E)
}

fun exerciseAccent(exerciseType: ExerciseType): Color {
    return when (exerciseType) {
        ExerciseType.PUSH_UP -> RepLockColors.Teal
        ExerciseType.PULL_UP -> RepLockColors.Orange
    }
}

fun exerciseGradient(exerciseType: ExerciseType): Brush {
    return when (exerciseType) {
        ExerciseType.PUSH_UP -> Brush.horizontalGradient(
            listOf(RepLockColors.Teal, RepLockColors.Sky)
        )
        ExerciseType.PULL_UP -> Brush.horizontalGradient(
            listOf(RepLockColors.Orange, RepLockColors.Coral)
        )
    }
}
