package com.replock.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_sessions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WorkoutSessionEntity>>
}
