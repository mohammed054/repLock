package com.replock.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseType: String,
    val reps: Int,
    val targetReps: Int,
    val durationSecs: Long,
    val averageFormScore: Int,
    val completedGoal: Boolean,
    val createdAt: Long,
    val dayKey: String
)
