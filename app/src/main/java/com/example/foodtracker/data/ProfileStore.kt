package com.example.foodtracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.foodtracker.ui.theme.ColorPalette
import com.example.foodtracker.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class ProfileStore(
    private val context: Context,
    /**
     * Called after [save] changes the profile, which changes the widgets'
     * goal numbers ([com.example.foodtracker.domain.GoalCalculator]). Wired in
     * [com.example.foodtracker.FoodTrackerApp] to push a widget refresh. Plain
     * lambda so this data class stays decoupled from the widget/UI layer.
     */
    private val onProfileChanged: suspend () -> Unit = {},
) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val AGE = intPreferencesKey("age")
        val SEX = stringPreferencesKey("sex")
        val HEIGHT = intPreferencesKey("height_cm")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val ACTIVITY = stringPreferencesKey("activity_level")
        val GOAL = stringPreferencesKey("goal")
        val WEIGHT_CHANGE_RATE = stringPreferencesKey("weight_change_rate")
        val GOAL_WEIGHT = doublePreferencesKey("goal_weight_kg")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val SEED_VERSION = intPreferencesKey("seed_version")
        // Backups
        val BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
        // AI nutrition autofill (opt-in, user-supplied key)
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
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
            weightChangeRate = WeightChangeRate.fromName(p[Keys.WEIGHT_CHANGE_RATE]),
            goalWeightKg = p[Keys.GOAL_WEIGHT],
        )
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] == true }

    /** Default true — opt-out, not opt-in, since haptics enhance the core taps. */
    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS_ENABLED] ?: true }

    /**
     * Mapped with legacy migration: pre-v3 installs persisted `Amoled` /
     * `AmoledDynamic` under THEME_MODE — both flatten to the new `Dark` mode.
     * The dynamic distinction lives on the [colorPalette] flow now.
     */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { p ->
        when (val raw = p[Keys.THEME_MODE]) {
            "Amoled", "AmoledDynamic" -> ThemeMode.Dark
            else -> ThemeMode.fromName(raw)
        }
    }

    val colorPalette: Flow<ColorPalette> = context.dataStore.data.map { p ->
        // If user had AmoledDynamic before and hasn't changed settings yet,
        // carry the Dynamic preference through automatically.
        if (p[Keys.COLOR_PALETTE] == null && p[Keys.THEME_MODE] == "AmoledDynamic") ColorPalette.Dynamic
        else ColorPalette.fromName(p[Keys.COLOR_PALETTE])
    }

    /** Persisted SAF tree URI for backups, or null when no custom folder is set. */
    val backupFolderUri: Flow<String?> = context.dataStore.data.map { it[Keys.BACKUP_FOLDER_URI] }

    /** Recurring backup cadence in hours. 0 = off. */
    val backupIntervalHours: Flow<Int> = context.dataStore.data.map { it[Keys.BACKUP_INTERVAL_HOURS] ?: 0 }

    /** AI nutrition autofill. Default false — privacy-first, opt-in only. */
    val aiEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AI_ENABLED] ?: false }

    val aiProvider: Flow<AiProvider> =
        context.dataStore.data.map { AiProvider.fromName(it[Keys.AI_PROVIDER]) }

    /** User's own API key for the selected provider, or null when unset. */
    val aiApiKey: Flow<String?> = context.dataStore.data.map { it[Keys.AI_API_KEY] }

    /**
     * Stored model id. Resolved against the current provider's list at read time
     * (see [AiProvider.modelFromId]), so switching providers falls back to that
     * provider's cheapest default automatically.
     */
    val aiModelId: Flow<String?> = context.dataStore.data.map { it[Keys.AI_MODEL] }

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
            p[Keys.WEIGHT_CHANGE_RATE] = profile.weightChangeRate.name
            val goalWeight = profile.goalWeightKg
            if (goalWeight == null) p.remove(Keys.GOAL_WEIGHT) else p[Keys.GOAL_WEIGHT] = goalWeight
            if (markOnboarded) p[Keys.ONBOARDED] = true
        }
        // Goals are derived from the profile, so a save changes what the
        // widgets show — refresh them now rather than waiting for the next
        // log write or app foreground.
        onProfileChanged()
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { p -> p[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setColorPalette(palette: ColorPalette) {
        context.dataStore.edit { p -> p[Keys.COLOR_PALETTE] = palette.name }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.AI_ENABLED] = enabled }
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.dataStore.edit { p -> p[Keys.AI_PROVIDER] = provider.name }
    }

    suspend fun setAiApiKey(key: String?) {
        context.dataStore.edit { p ->
            if (key.isNullOrBlank()) p.remove(Keys.AI_API_KEY) else p[Keys.AI_API_KEY] = key.trim()
        }
    }

    suspend fun setAiModel(modelId: String) {
        context.dataStore.edit { p -> p[Keys.AI_MODEL] = modelId }
    }
}
