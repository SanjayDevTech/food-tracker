package com.example.foodtracker.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun apply(config: ReminderConfig) {
        if (config.enabled) schedule(config) else cancel(config.kind)
    }

    fun cancel(kind: ReminderKind) {
        val pi = pendingIntent(kind, mutable = false, create = false)
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    /** Wipes all alarms then re-schedules every enabled config. */
    fun rescheduleAll(configs: List<ReminderConfig>) {
        ReminderKind.entries.forEach { cancel(it) }
        configs.filter { it.enabled }.forEach { schedule(it) }
    }

    @SuppressLint("MissingPermission")
    private fun schedule(config: ReminderConfig) {
        val triggerAt = nextTriggerMillis(config.hour, config.minute)
        val pi = pendingIntent(config.kind, mutable = false, create = true)!!

        // setInexactRepeating: meal/water reminders don't need second-level precision;
        // it's friendlier to battery and works on all API levels without requiring the
        // SCHEDULE_EXACT_ALARM permission. The system will fire ~daily at the chosen
        // wall-clock time and we don't mind a few minutes of slack.
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pi,
        )
    }

    private fun pendingIntent(kind: ReminderKind, mutable: Boolean, create: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
        }
        val flags = (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(context, kind.ordinal, intent, flags)
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    companion object {
        const val CHANNEL_ID = "food_tracker_reminders"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Logging reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminds you to log meals, water, and weigh-ins."
            }
            manager.createNotificationChannel(channel)
        }
    }
}
