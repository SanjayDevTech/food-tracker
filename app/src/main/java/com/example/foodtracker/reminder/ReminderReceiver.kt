package com.example.foodtracker.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.foodtracker.FoodTrackerApp
import com.example.foodtracker.MainActivity
import com.example.foodtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> handleFire(context, intent)
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> rearmAlarms(context)
        }
    }

    private fun rearmAlarms(context: Context) {
        // BroadcastReceiver onReceive runs on the main thread with a short timeout;
        // goAsync() keeps the process alive until we explicitly finish().
        val app = context.applicationContext as? FoodTrackerApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val configs = app.reminderStore.snapshot()
                app.reminderScheduler.rescheduleAll(configs)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleFire(context: Context, intent: Intent) {
        val kindName = intent.getStringExtra(EXTRA_KIND) ?: return
        val kind = runCatching { ReminderKind.valueOf(kindName) }.getOrNull() ?: return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ReminderScheduler.ensureChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPI = PendingIntent.getActivity(
            context,
            kind.ordinal,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(kind.title)
            .setContentText(kind.body)
            .setContentIntent(contentPI)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(kind.ordinal, notification)
    }

    companion object {
        const val ACTION_FIRE = "com.example.foodtracker.action.REMINDER_FIRE"
        const val EXTRA_KIND = "kind"
    }
}
