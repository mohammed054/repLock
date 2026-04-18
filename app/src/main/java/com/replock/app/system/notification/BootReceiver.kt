package com.replock.app.system.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.replock.app.presentation.onboarding.getSavedDifficulty

/**
 * Re-schedules reminder alarms after a device reboot.
 * AlarmManager alarms do not survive a power cycle; this receiver
 * restores them as soon as the system has finished booting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Only schedule if the user has already completed onboarding.
        val hasDifficulty = context.getSavedDifficulty() != null
        if (hasDifficulty) {
            ReminderScheduler.schedule(context)
        }
    }
}
