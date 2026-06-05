package com.example.foodtracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.foodtracker.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class ProfileStore(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val AGE = intPreferencesKey("age")
        val SEX = stringPreferencesKey("sex")
        val HEIGHT = intPreferencesKey("height_cm")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val ACTIVITY = stringPreferencesKey("activity_level")
        val GOAL = stringPreferencesKey("goal")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEED_VERSION = intPreferencesKey("seed_version")
        // Backups
        val BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
    }

    val profile: Flow<UserProfile> = context.dataStore.data.map { p ->
        UserProfile(
            name = p[Keys.NAME] ?: "",
            ageYears = p[Keys.AGE] ?: 25,
            sex = Sex.fromName(p[Keys.SEX] ?: Sex.Male.name),
            heightCm = p[Keys.HEIGHT] ?: 170,
            weightKg = p[Keys.WEIGHT] ?: 65.0,
            activityLevel = ActivityLevel.fromName(p[Keys.ACTIVITY] ?: ActivityLevel.Moderate.name),
            goal = Goal.fromName(p[Keys.GOAL] ?: Goal.Maintain.name),
        )
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] == true }

    /** Default true — opt-out, not opt-in, since haptics enhance the core taps. */
    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS_ENABLED] ?: true }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { ThemeMode.fromName(it[Keys.THEME_MODE]) }

    /** Persisted SAF tree URI for backups, or null when no custom folder is set. */
    val backupFolderUri: Flow<String?> = context.dataStore.data.map { it[Keys.BACKUP_FOLDER_URI] }

    /** Recurring backup cadence in hours. 0 = off. */
    val backupIntervalHours: Flow<Int> = context.dataStore.data.map { it[Keys.BACKUP_INTERVAL_HOURS] ?: 0 }

    suspend fun setBackupFolderUri(uri: String?) {
        context.dataStore.edit { p ->
            if (uri == null) p.remove(Keys.BACKUP_FOLDER_URI) else p[Keys.BACKUP_FOLDER_URI] = uri
        }
    }

    suspend fun setBackupIntervalHours(hours: Int) {
        context.dataStore.edit { p -> p[Keys.BACKUP_INTERVAL_HOURS] = hours }
    }

    suspend fun getSeedVersion(): Int = context.dataStore.data.map { it[Keys.SEED_VERSION] ?: 0 }.first()

    suspend fun setSeedVersion(version: Int) {
        context.dataStore.edit { p -> p[Keys.SEED_VERSION] = version }
    }

    suspend fun save(profile: UserProfile, markOnboarded: Boolean = true) {
        context.dataStore.edit { p ->
            p[Keys.NAME] = profile.name
            p[Keys.AGE] = profile.ageYears
            p[Keys.SEX] = profile.sex.name
            p[Keys.HEIGHT] = profile.heightCm
            p[Keys.WEIGHT] = profile.weightKg
            p[Keys.ACTIVITY] = profile.activityLevel.name
            p[Keys.GOAL] = profile.goal.name
            if (markOnboarded) p[Keys.ONBOARDED] = true
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { p -> p[Keys.THEME_MODE] = mode.name }
    }
}
