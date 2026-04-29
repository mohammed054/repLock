package com.replock.app.data.repository

import com.replock.app.data.local.SessionDao
import com.replock.app.data.local.WorkoutSessionEntity
import com.replock.app.domain.model.ExerciseType
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkoutRepository(private val sessionDao: SessionDao) {

    fun observeSessions(): Flow<List<WorkoutSessionEntity>> = sessionDao.observeAll()

    suspend fun saveSession(
        exerciseType: ExerciseType,
        reps: Int,
        targetReps: Int,
        durationSecs: Long,
        averageFormScore: Int,
        createdAt: Long = System.currentTimeMillis()
    ) {
        sessionDao.insert(
            WorkoutSessionEntity(
                exerciseType = exerciseType.name,
                reps = reps,
                targetReps = targetReps,
                durationSecs = durationSecs,
                averageFormScore = averageFormScore,
                completedGoal = reps >= targetReps,
                createdAt = createdAt,
                dayKey = dayKey(createdAt)
            )
        )
    }

    companion object {
        private val dayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val shortDayFormatter = SimpleDateFormat("EEE", Locale.US)
        private val monthDayFormatter = SimpleDateFormat("MMM d", Locale.US)

        fun dayKey(timeMillis: Long): String = dayFormatter.format(Date(timeMillis))

        fun formatDayLabel(dayKey: String): String {
            return try {
                val parsed = dayFormatter.parse(dayKey) ?: return dayKey
                monthDayFormatter.format(parsed)
            } catch (_: Exception) {
                dayKey
            }
        }

        fun formatWeekday(dayKey: String): String {
            return try {
                val parsed = dayFormatter.parse(dayKey) ?: return dayKey
                shortDayFormatter.format(parsed).uppercase(Locale.US)
            } catch (_: Exception) {
                dayKey
            }
        }

        fun currentStreak(dayKeysDescending: List<String>): Int {
            val uniqueDays = dayKeysDescending.distinct()
            if (uniqueDays.isEmpty()) return 0

            val today = dayKey(System.currentTimeMillis())
            val startDay = when {
                uniqueDays.first() == today -> today
                uniqueDays.first() == shiftDay(today, -1) -> uniqueDays.first()
                else -> return 0
            }

            var streak = 0
            var cursor = startDay
            while (uniqueDays.contains(cursor)) {
                streak++
                cursor = shiftDay(cursor, -1)
            }
            return streak
        }

        fun longestStreak(dayKeysDescending: List<String>): Int {
            val sortedUnique = dayKeysDescending.distinct().sortedDescending()
            if (sortedUnique.isEmpty()) return 0

            var best = 1
            var running = 1
            for (index in 0 until sortedUnique.lastIndex) {
                val current = sortedUnique[index]
                val next = sortedUnique[index + 1]
                if (shiftDay(current, -1) == next) {
                    running++
                    best = maxOf(best, running)
                } else {
                    running = 1
                }
            }
            return best
        }

        fun shiftDay(dayKey: String, offsetDays: Int): String {
            val calendar = Calendar.getInstance()
            calendar.time = dayFormatter.parse(dayKey) ?: Date()
            calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
            return dayFormatter.format(calendar.time)
        }
    }
}
