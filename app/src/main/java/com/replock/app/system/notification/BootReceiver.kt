package com.replock.app.system.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules reminder alarms after a device reboot.
 * AlarmManager alarms do not survive a power cycle; this receiver
 * restores them as soon as the system has finished booting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        ReminderScheduler.schedule(context)
    }
}
