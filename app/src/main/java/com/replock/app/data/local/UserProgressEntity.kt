package com.replock.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val date: String,
    val repsDone: Int,
    val pb: Int,
    val streakDay: Int,
    val programName: String,
    val durationSecs: Long,
    val completedGoal: Boolean
)