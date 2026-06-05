package com.example.foodtracker

import android.app.Application
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.OpenFoodFactsApi
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UsdaFoodApi
import com.example.foodtracker.data.backup.BackupManager
import com.example.foodtracker.data.backup.BackupScheduler
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.IndianFoodSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FoodTrackerApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val api: OpenFoodFactsApi by lazy { OpenFoodFactsApi() }
    val usdaApi: UsdaFoodApi by lazy { UsdaFoodApi() }
    val repository: FoodRepository by lazy {
        FoodRepository(
            foodDao = database.foodDao(),
            foodLogDao = database.foodLogDao(),
            waterLogDao = database.waterLogDao(),
            activityLogDao = database.activityLogDao(),
            weightLogDao = database.weightLogDao(),
            api = api,
            usdaApi = usdaApi,
        )
    }
    val profileStore: ProfileStore by lazy { ProfileStore(this) }
    val reminderStore: com.example.foodtracker.reminder.ReminderStore by lazy {
        com.example.foodtracker.reminder.ReminderStore(this)
    }
    val reminderScheduler: com.example.foodtracker.reminder.ReminderScheduler by lazy {
        com.example.foodtracker.reminder.ReminderScheduler(this)
    }
    val backupManager: BackupManager by lazy { BackupManager(this, database, profileStore) }
    val backupScheduler: BackupScheduler by lazy { BackupScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        com.example.foodtracker.reminder.ReminderScheduler.ensureChannel(this)
        appScope.launch {
            val freshInstall = repository.seedIfEmpty()
            val storedSeedVersion = profileStore.getSeedVersion()
            val refreshed = if (!freshInstall) repository.refreshSeedIfStale(storedSeedVersion) else false
            if (freshInstall || refreshed) {
                profileStore.setSeedVersion(IndianFoodSeed.SEED_VERSION)
            }
            // Re-arm any persisted reminders on cold start so they survive reboots
            // (paired with BOOT_COMPLETED receiver in the manifest).
            reminderScheduler.rescheduleAll(reminderStore.snapshot())
            // Re-establish the periodic backup job using the user's saved cadence.
            backupScheduler.apply(profileStore.backupIntervalHours.first())
        }
    }
}
