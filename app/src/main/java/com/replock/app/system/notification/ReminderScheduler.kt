package com.replock.app.system.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules exact push-up reminder alarms at fixed intervals within the active window
 * (07:00–22:00). On goal completion, call [scheduleFromTomorrow] to suppress the rest
 * of today's alarms and pick up again at the window start tomorrow.
 */
object ReminderScheduler {

    private const val PREFS_NAME         = "replock_reminder_prefs"
    private const val KEY_INTERVAL_HOURS = "reminder_interval_hours"

    const val CHANNEL_ID       = "replock_reminders"
    const val CHANNEL_NAME     = "Workout Reminders"
    const val REQUEST_CODE_BASE = 2000
    const val MAX_SLOTS         = 24   // safety ceiling for daily alarms

    private const val WINDOW_START = 7   // 07:00 inclusive
    private const val WINDOW_END   = 22  // 22:00 exclusive

    val MESSAGES = listOf(
        "Your streak is waiting 🔥",
        "You haven't done your reps today",
        "Don't break the chain 💪",
        "Time to lock in. Get your reps done.",
        "One set. That's all it takes.",
        "Rep by rep. Day by day. 🏆",
        "Your future self will thank you.",
        "The grind doesn't stop. Neither do you."
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /** Cancel any pending alarms, then schedule today's remaining reminders. */
    fun schedule(context: Context) {
        val intervalHours = getIntervalHours(context)
        cancelAll(context)
        if (intervalHours <= 0) return

        val now    = Calendar.getInstance()
        val nowH   = now.get(Calendar.HOUR_OF_DAY)
        val nowMin = now.get(Calendar.MINUTE)

        // Find the first slot strictly after now within today's window.
        var slotIndex = 0
        var hour      = WINDOW_START
        while (hour < WINDOW_END) {
            val isAfterNow = hour > nowH || (hour == nowH && 0 > nowMin)
            if (isAfterNow) {
                scheduleAlarm(context, hour, 0, slotIndex)
                slotIndex++
            }
            hour += intervalHours
            if (slotIndex >= MAX_SLOTS) break
        }

        // If nothing was scheduled today (past window), start fresh tomorrow.
        if (slotIndex == 0) scheduleWindowStart(context, daysAhead = 1)
    }

    /**
     * Called when the user completes their daily goal.
     * Cancels today's remaining alarms and schedules the first reminder for tomorrow.
     */
    fun scheduleFromTomorrow(context: Context) {
        cancelAll(context)
        if (getIntervalHours(context) > 0) {
            scheduleWindowStart(context, daysAhead = 1)
        }
    }

    fun cancelAll(context: Context) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        repeat(MAX_SLOTS) { i ->
            buildIntent(context, i)
                ?.let { mgr.cancel(it) }
        }
    }

    fun getIntervalHours(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL_HOURS, 3)

    fun setIntervalHours(context: Context, hours: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_INTERVAL_HOURS, hours).apply()
        schedule(context) // immediately reschedule with new cadence
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Schedule the window-start alarm N days from now (used for boot restore & tomorrow). */
    private fun scheduleWindowStart(context: Context, daysAhead: Int) {
        scheduleAlarm(context, WINDOW_START, 0, slotIndex = 0, daysAhead = daysAhead)
        // After the first fires, ReminderReceiver will call schedule() to fill the rest.
    }

    private fun scheduleAlarm(
        context    : Context,
        hour       : Int,
        minute     : Int,
        slotIndex  : Int,
        daysAhead  : Int = 0
    ) {
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (daysAhead > 0) add(Calendar.DAY_OF_YEAR, daysAhead)
        }

        val pi = buildIntent(context, slotIndex, msgIndex = slotIndex % MESSAGES.size)
            ?: return

        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pi)
    }

    private fun buildIntent(
        context   : Context,
        slotIndex : Int,
        msgIndex  : Int? = null
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            msgIndex?.let { putExtra(ReminderReceiver.EXTRA_MSG_INDEX, it) }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE_BASE + slotIndex, intent, flags)
    }
}
