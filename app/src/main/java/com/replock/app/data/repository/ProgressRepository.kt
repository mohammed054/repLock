package com.replock.app.data.repository

import com.replock.app.data.local.UserProgressDao
import com.replock.app.data.local.UserProgressEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class ProgressRepository(private val dao: UserProgressDao) {
    suspend fun getPbForProgram(programName: String): Int =
        dao.getPbForProgram(programName) ?: 0

    suspend fun getCurrentStreak(): Int {
        val today = todayString()
        val yesterday = offsetDate(today, -1)

        val todayEntry = dao.getByDate(today)
        if (todayEntry?.completedGoal == true) return todayEntry.streakDay

        val yesterdayEntry = dao.getByDate(yesterday)
        if (yesterdayEntry?.completedGoal == true) return yesterdayEntry.streakDay

        return 0
    }

    fun getAllSessions(): Flow<List<UserProgressEntity>> = dao.getAllDescending()

    suspend fun saveDaySession(
        date: String,
        repsDone: Int,
        programName: String,
        durationSecs: Long,
        completedGoal: Boolean
    ) {
        val currentPb = getPbForProgram(programName)
        val newPb = maxOf(currentPb, repsDone)

        val streakDay = if (completedGoal) {
            val yesterday = offsetDate(date, -1)
            val yesterdayEntry = dao.getByDate(yesterday)
            if (yesterdayEntry?.completedGoal == true) yesterdayEntry.streakDay + 1 else 1
        } else 0

        dao.insertOrUpdate(
            UserProgressEntity(
                date = date,
                repsDone = repsDone,
                pb = newPb,
                streakDay = streakDay,
                programName = programName,
                durationSecs = durationSecs,
                completedGoal = completedGoal
            )
        )
    }

    companion object {
        private val sdf get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun todayString(): String = sdf.format(Date())

        fun offsetDate(dateStr: String, deltaDays: Int): String {
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(dateStr)!!
            cal.add(Calendar.DAY_OF_YEAR, deltaDays)
            return sdf.format(cal.time)
        }

        fun formatDateLabel(dateStr: String): String {
            return try {
                val date = sdf.parse(dateStr) ?: return dateStr
                val label = SimpleDateFormat("MMM d", Locale.US)
                label.format(date)
            } catch (e: Exception) {
                dateStr
            }
        }

        fun formatDuration(secs: Long): String {
            val m = secs / 60
            val s = secs % 60
            return "%02d:%02d".format(m, s)
        }
    }
}