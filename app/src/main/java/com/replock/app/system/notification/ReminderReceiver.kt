package com.replock.app.system.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.replock.app.R
import com.replock.app.data.local.AppDatabase
import com.replock.app.data.repository.ProgressRepository
import com.replock.app.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MSG_INDEX  = "replock_msg_index"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val msgIndex = intent.getIntExtra(EXTRA_MSG_INDEX, 0)

        // goAsync lets us do a quick DB read without blocking the main thread.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao   = AppDatabase.getInstance(context).userProgressDao()
                val today = ProgressRepository.todayString()
                val entry = dao.getByDate(today)

                if (entry?.completedGoal == true) {
                    // User already hit their goal — suppress today's remaining alarms
                    // and reschedule for tomorrow's window start.
                    ReminderScheduler.scheduleFromTomorrow(context)
                } else {
                    showNotification(context, msgIndex)
                    // Schedule the next alarm in today's sequence.
                    ReminderScheduler.schedule(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun showNotification(context: Context, msgIndex: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        ensureChannel(manager)

        val message = ReminderScheduler.MESSAGES[
            msgIndex.coerceIn(0, ReminderScheduler.MESSAGES.lastIndex)
        ]

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RepLock")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(ReminderScheduler.CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                ReminderScheduler.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily push-up reminders"
                enableLights(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
