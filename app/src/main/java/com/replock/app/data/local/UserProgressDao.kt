package com.replock.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: UserProgressEntity)

    @Query("SELECT * FROM user_progress WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): UserProgressEntity?

    @Query("SELECT * FROM user_progress ORDER BY date DESC")
    fun getAllDescending(): Flow<List<UserProgressEntity>>

    @Query("SELECT date FROM user_progress WHERE completedGoal = 1 ORDER BY date DESC")
    suspend fun getAllCompletedDates(): List<String>

    @Query("SELECT MAX(repsDone) FROM user_progress WHERE programName = :programName")
    suspend fun getPbForProgram(programName: String): Int?
}