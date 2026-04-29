package com.replock.app.data.repository

import android.content.Context
import com.replock.app.domain.model.ExerciseType
import com.replock.app.system.notification.ReminderScheduler

data class UserPreferences(
    val selectedExercise: ExerciseType = ExerciseType.PUSH_UP,
    val pushUpTargetReps: Int = ExerciseType.PUSH_UP.defaultTargetReps,
    val pullUpTargetReps: Int = ExerciseType.PULL_UP.defaultTargetReps,
    val soundEnabled: Boolean = true,
    val reminderIntervalHours: Int = 3
) {
    fun targetFor(exerciseType: ExerciseType): Int {
        return when (exerciseType) {
            ExerciseType.PUSH_UP -> pushUpTargetReps
            ExerciseType.PULL_UP -> pullUpTargetReps
        }
    }
}

class UserPreferencesRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): UserPreferences {
        return UserPreferences(
            selectedExercise = ExerciseType.fromName(
                prefs.getString(KEY_SELECTED_EXERCISE, ExerciseType.PUSH_UP.name)
            ),
            pushUpTargetReps = prefs.getInt(
                KEY_PUSH_UP_TARGET,
                ExerciseType.PUSH_UP.defaultTargetReps
            ).coerceIn(6, 80),
            pullUpTargetReps = prefs.getInt(
                KEY_PULL_UP_TARGET,
                ExerciseType.PULL_UP.defaultTargetReps
            ).coerceIn(3, 40),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            reminderIntervalHours = ReminderScheduler.getIntervalHours(context)
        )
    }

    fun setSelectedExercise(exerciseType: ExerciseType) {
        prefs.edit().putString(KEY_SELECTED_EXERCISE, exerciseType.name).apply()
    }

    fun setTargetReps(exerciseType: ExerciseType, reps: Int) {
        val key = when (exerciseType) {
            ExerciseType.PUSH_UP -> KEY_PUSH_UP_TARGET
            ExerciseType.PULL_UP -> KEY_PULL_UP_TARGET
        }
        prefs.edit().putInt(key, reps).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun setReminderIntervalHours(hours: Int) {
        ReminderScheduler.setIntervalHours(context, hours)
    }

    companion object {
        private const val PREFS_NAME = "replock_prefs"
        private const val KEY_SELECTED_EXERCISE = "selected_exercise"
        private const val KEY_PUSH_UP_TARGET = "push_up_target_reps"
        private const val KEY_PULL_UP_TARGET = "pull_up_target_reps"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
    }
}
