package com.example.foodtracker.ui.screens.you

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.AiModel
import com.example.foodtracker.data.AiProvider
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.backup.BackupManager
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.domain.GoalProgress
import com.example.foodtracker.domain.goalProgress
import com.example.foodtracker.reminder.ReminderConfig
import com.example.foodtracker.reminder.ReminderKind
import com.example.foodtracker.reminder.ReminderStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Everything the **You** page and its three sheets read.
 *
 * This is the old `SettingsViewModel` with the profile half of `ProfileViewModel`
 * folded back in and the backup machinery lifted out. The shape follows the
 * page: You is now one screen showing *who you are* and *how the app behaves*,
 * so one state object serves it, while the two things that got their own pages
 * (editing the profile, managing backups) got their own view models.
 *
 * **Why the backup work is not here any more.** The old split existed because
 * `SettingsViewModel.init` listed backup files off disk, and that had no
 * business running when the user opened Profile. You has the same problem for
 * the same reason: it is a bottom-bar destination now, opened far more often
 * than Settings ever was. So the only thing it keeps of backup is a count, it
 * reads that count on demand rather than in `init` (see [refreshBackupCount]),
 * and everything that actually writes a file lives in [DataBackupViewModel].
 *
 * The AI block is carried whole, API key included, because the AI-autofill
 * sheet is hosted by You rather than by a page of its own. The key is never
 * logged and never leaves [com.example.foodtracker.data.ProfileStore]'s
 * separate encrypted-at-rest store; this class only shuttles it to a field that
 * renders it masked.
 */
class YouViewModel(
    private val profileStore: ProfileStore,
    private val reminderStore: ReminderStore,
    private val backupManager: BackupManager,
    private val repository: FoodRepository,
) : ViewModel() {

    private val _backupCount = MutableStateFlow(0)
    private val _message = MutableStateFlow<String?>(null)

    val state: StateFlow<YouUiState> = combine(
        // The profile and the weight history that gives the goal meter a
        // starting point. Both are cheap and neither is optional on this page.
        combine(profileStore.profile, repository.observeRecentWeights()) { profile, weights ->
            ProfileBase(
                profile = profile,
                goals = GoalCalculator.goalsFor(profile),
                progress = goalProgress(profile),
                // observeRecent is DESC, so the tail is the oldest weigh-in we
                // still hold. That is not "the weight you started this goal at"
                // — nothing records when a goal was set — but it is a real
                // number the user actually stood on, which beats inventing one.
                startWeightKg = weights.lastOrNull()?.weightKg,
            )
        },
        combine(
            profileStore.hapticsEnabled,
            reminderStore.configs,
            profileStore.backupIntervalHours,
            _backupCount,
            repository.observeCustomVisibleFoods(),
        ) { haptics, reminders, backupHours, backupCount, customFoods ->
            AppBase(
                haptics = haptics,
                reminderSummary = reminderSummary(reminders),
                backupCadence = BackupCadence.fromHours(backupHours),
                backupCount = backupCount,
                customFoodCount = customFoods.size,
            )
        },
        combine(
            profileStore.aiEnabled,
            profileStore.aiProvider,
            profileStore.aiApiKey,
            profileStore.aiModelId,
        ) { enabled, provider, key, modelId ->
            AiBase(enabled, provider, key ?: "", provider.modelFromId(modelId))
        },
        _message,
    ) { profile, app, ai, message ->
        YouUiState(
            profile = profile.profile,
            goals = profile.goals,
            goalProgress = profile.progress,
            startWeightKg = profile.startWeightKg,
            hapticsEnabled = app.haptics,
            reminderSummary = app.reminderSummary,
            backupCadence = app.backupCadence,
            backupCount = app.backupCount,
            customFoodCount = app.customFoodCount,
            aiEnabled = ai.enabled,
            aiProvider = ai.provider,
            aiApiKey = ai.key,
            aiModel = ai.model,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouUiState())

    private data class ProfileBase(
        val profile: UserProfile,
        val goals: NutrientGoals,
        val progress: GoalProgress?,
        val startWeightKg: Double?,
    )

    private data class AppBase(
        val haptics: Boolean,
        val reminderSummary: String,
        val backupCadence: BackupCadence,
        val backupCount: Int,
        val customFoodCount: Int,
    )

    private data class AiBase(
        val enabled: Boolean,
        val provider: AiProvider,
        val key: String,
        val model: AiModel,
    )

    /**
     * Count the snapshots on disk.
     *
     * Called from the screen rather than from `init` so the filesystem is only
     * touched once the page is actually on screen — and so it is re-counted on
     * the way back from Data & backup, where the user may well have just made
     * or deleted one.
     */
    fun refreshBackupCount() {
        viewModelScope.launch {
            _backupCount.update { backupManager.listBackups().size }
        }
    }

    fun consumeMessage() = _message.update { null }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { profileStore.setHapticsEnabled(enabled) }
    }

    // ─── AI nutrition autofill ────────────────────────────────────────────
    // Verbatim from SettingsViewModel. Each setter writes straight through to
    // the store; there is no local draft, so the sheet cannot hold a key the
    // user thinks they saved.

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch { profileStore.setAiEnabled(enabled) }
    }

    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch { profileStore.setAiProvider(provider) }
    }

    fun setAiApiKey(key: String) {
        viewModelScope.launch { profileStore.setAiApiKey(key) }
    }

    fun setAiModel(model: AiModel) {
        viewModelScope.launch { profileStore.setAiModel(model.id) }
    }

    /**
     * Bulk-delete every food the user marked as custom. [cascadeLogs] = true
     * also wipes their historical food-log rows; false soft-hides them so the
     * diary keeps rendering.
     */
    fun deleteAllCustomFoods(cascadeLogs: Boolean) {
        viewModelScope.launch {
            repository.deleteAllCustomFoods(cascadeLogs)
            _message.update {
                if (cascadeLogs) "Custom foods and their log history removed."
                else "Custom foods hidden — past entries preserved."
            }
        }
    }
}

data class YouUiState(
    val profile: UserProfile = UserProfile(),
    val goals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
    val goalProgress: GoalProgress? = null,
    val startWeightKg: Double? = null,
    val hapticsEnabled: Boolean = true,
    val reminderSummary: String = "Off",
    val backupCadence: BackupCadence = BackupCadence.Off,
    val backupCount: Int = 0,
    val customFoodCount: Int = 0,
    val aiEnabled: Boolean = false,
    val aiProvider: AiProvider = AiProvider.Gemini,
    val aiApiKey: String = "",
    val aiModel: AiModel = AiProvider.Gemini.defaultModel,
    val message: String? = null,
) {
    /** Right-hand value on the Data & backup row: `Daily · 4`. */
    val backupSummary: String
        get() = if (backupCount == 0) backupCadence.label else "${backupCadence.label} · $backupCount"

    /** Right-hand value on the AI autofill row: the provider, or `Off`. */
    val aiSummary: String get() = if (aiEnabled) aiProvider.displayName else "Off"

    /**
     * How far along the weight goal the user is, 0..1, or null when there is
     * nothing honest to draw: no goal, no goal weight, or no weigh-in older
     * than today to measure from.
     */
    val goalFraction: Float?
        get() {
            val remaining = goalProgress?.deltaKg ?: return null
            val start = startWeightKg ?: return null
            val total = abs(start - (profile.goalWeightKg ?: return null))
            if (total <= 0.01) return null
            return ((total - remaining) / total).toFloat().coerceIn(0f, 1f)
        }
}

/**
 * `Meals, water` — the four meal slots collapse to one word because listing
 * "Breakfast, Lunch, Snack, Dinner, Water" does not fit a settings row and
 * does not tell the user anything the word "meals" doesn't.
 */
private fun reminderSummary(configs: List<ReminderConfig>): String {
    val on = configs.filter { it.enabled }.map { it.kind }
    if (on.isEmpty()) return "Off"
    val parts = buildList {
        if (on.any { it in MEAL_KINDS }) add("Meals")
        if (ReminderKind.Water in on) add("Water")
        if (ReminderKind.WeighIn in on) add("Weigh-in")
    }
    return parts.joinToString(", ")
}

private val MEAL_KINDS = setOf(
    ReminderKind.Breakfast,
    ReminderKind.Lunch,
    ReminderKind.Snack,
    ReminderKind.Dinner,
)
