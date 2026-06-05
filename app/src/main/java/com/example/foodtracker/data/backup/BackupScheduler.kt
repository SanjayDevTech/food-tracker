package com.example.foodtracker.data.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Owns the single periodic WorkManager job that takes recurring backups.
 *
 * The interval is user-controlled — see Profile → Data → Recurring backup.
 * Passing 0 (or anything < 1 h) cancels the job entirely; the worker only
 * runs when the user has explicitly enabled it.
 */
class BackupScheduler(private val context: Context) {

    private val workManager get() = WorkManager.getInstance(context)

    /** Apply [intervalHours]: 0 = disabled, anything ≥ 1 = recurring. */
    fun apply(intervalHours: Int) {
        if (intervalHours <= 0) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        // WorkManager enforces a minimum periodic interval of 15 minutes;
        // anything coarser is fine.
        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalHours.toLong(), TimeUnit.HOURS,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        const val WORK_NAME = "recurring-backup"
    }
}
