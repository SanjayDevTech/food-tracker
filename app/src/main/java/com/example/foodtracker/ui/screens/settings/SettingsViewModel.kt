package com.example.foodtracker.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.AiModel
import com.example.foodtracker.data.AiProvider
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.backup.AnalysisRange
import com.example.foodtracker.data.backup.BackupManager
import com.example.foodtracker.data.backup.BackupScheduler
import com.example.foodtracker.data.backup.LocalBackup
import com.example.foodtracker.ui.theme.ColorPalette
import com.example.foodtracker.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BackupCadence(val hours: Int, val label: String) {
    Off(0, "Off"),
    Daily(24, "Daily"),
    Weekly(24 * 7, "Weekly");

    companion object {
        fun fromHours(h: Int): BackupCadence =
            entries.firstOrNull { it.hours == h } ?: Off
    }
}

data class SettingsUiState(
    val hapticsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val colorPalette: ColorPalette = ColorPalette.Normal,
    val backups: List<LocalBackup> = emptyList(),
    val backupBusy: Boolean = false,
    val backupMessage: String? = null,
    val backupFolderLabel: String? = null,
    val backupCadence: BackupCadence = BackupCadence.Off,
    val aiEnabled: Boolean = false,
    val aiProvider: AiProvider = AiProvider.Gemini,
    val aiApiKey: String = "",
    val aiModel: AiModel = AiProvider.Gemini.defaultModel,
)

/**
 * Everything on the Settings screen: appearance, AI autofill, backup/restore,
 * export/import and the custom-food purge.
 *
 * Split out of ProfileViewModel, which now owns only the profile and its
 * derived targets. The split is not just tidiness: this one lists backup files
 * off disk in [init], and that work has no business running when the user opens
 * Profile.
 */
class SettingsViewModel(
    private val profileStore: ProfileStore,
    private val backupManager: BackupManager,
    private val backupScheduler: BackupScheduler,
    private val repository: FoodRepository,
) : ViewModel() {

    private val _backups = MutableStateFlow<List<LocalBackup>>(emptyList())
    private val _backupBusy = MutableStateFlow(false)
    private val _backupMessage = MutableStateFlow<String?>(null)
    private val _folderLabel = MutableStateFlow<String?>(null)

    init {
        refreshBackups()
        refreshFolderLabel()
    }

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            profileStore.hapticsEnabled,
            profileStore.themeMode,
            profileStore.colorPalette,
        ) { haptics, theme, palette -> AppearanceBase(haptics, theme, palette) },
        combine(_backups, _backupBusy, _backupMessage, _folderLabel, profileStore.backupIntervalHours) {
            backups, busy, message, label, hours ->
            BackupBase(backups, busy, message, label, BackupCadence.fromHours(hours))
        },
        combine(
            profileStore.aiEnabled,
            profileStore.aiProvider,
            profileStore.aiApiKey,
            profileStore.aiModelId,
        ) { enabled, provider, key, modelId ->
            AiBase(enabled, provider, key ?: "", provider.modelFromId(modelId))
        },
    ) { appearance, backup, ai ->
        SettingsUiState(
            hapticsEnabled = appearance.haptics,
            themeMode = appearance.theme,
            colorPalette = appearance.palette,
            backups = backup.backups,
            backupBusy = backup.busy,
            backupMessage = backup.message,
            backupFolderLabel = backup.folderLabel,
            backupCadence = backup.cadence,
            aiEnabled = ai.enabled,
            aiProvider = ai.provider,
            aiApiKey = ai.key,
            aiModel = ai.model,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private data class AppearanceBase(
        val haptics: Boolean,
        val theme: ThemeMode,
        val palette: ColorPalette,
    )

    private data class BackupBase(
        val backups: List<LocalBackup>,
        val busy: Boolean,
        val message: String?,
        val folderLabel: String?,
        val cadence: BackupCadence,
    )

    private data class AiBase(
        val enabled: Boolean,
        val provider: AiProvider,
        val key: String,
        val model: AiModel,
    )

    // ─── Appearance & feedback ────────────────────────────────────────────

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { profileStore.setHapticsEnabled(enabled) }
    }

    fun setColorPalette(palette: ColorPalette) {
        viewModelScope.launch { profileStore.setColorPalette(palette) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { profileStore.setThemeMode(mode) }
    }

    // ─── AI nutrition autofill ────────────────────────────────────────────

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

    // ─── Backup / export / import ─────────────────────────────────────────

    fun consumeBackupMessage() = _backupMessage.update { null }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.update { true }
            val result = backupManager.exportTo(uri)
            _backupBusy.update { false }
            _backupMessage.update {
                result.fold(
                    onSuccess = { "Exported snapshot." },
                    onFailure = { "Export failed: ${it.message ?: "unknown error"}" },
                )
            }
        }
    }

    /** Export just the food logs (compact, foods de-duplicated) for AI analysis. */
    fun exportAnalysisLogs(uri: Uri, range: AnalysisRange) {
        viewModelScope.launch {
            _backupBusy.update { true }
            val result = backupManager.exportAnalysisTo(uri, range)
            _backupBusy.update { false }
            _backupMessage.update {
                result.fold(
                    onSuccess = { count -> "Exported $count log entries (${range.label.lowercase()})." },
                    onFailure = { "Export failed: ${it.message ?: "unknown error"}" },
                )
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.update { true }
            val result = backupManager.importFrom(uri)
            _backupBusy.update { false }
            _backupMessage.update {
                result.fold(
                    onSuccess = { "Imported snapshot — all data replaced." },
                    onFailure = { "Import failed: ${it.message ?: "unknown error"}" },
                )
            }
            refreshBackups()
        }
    }

    fun saveBackupNow() {
        viewModelScope.launch {
            _backupBusy.update { true }
            val result = backupManager.saveLocalBackup()
            _backupBusy.update { false }
            _backupMessage.update {
                result.fold(
                    onSuccess = { "Saved to Downloads/FoodTracker/${it.displayName}" },
                    onFailure = { "Backup failed: ${it.message ?: "unknown error"}" },
                )
            }
            refreshBackups()
        }
    }

    fun restoreBackup(backup: LocalBackup) {
        viewModelScope.launch {
            _backupBusy.update { true }
            val result = backupManager.restoreFrom(backup)
            _backupBusy.update { false }
            _backupMessage.update {
                result.fold(
                    onSuccess = { "Restored ${backup.displayName}." },
                    onFailure = { "Restore failed: ${it.message ?: "unknown error"}" },
                )
            }
        }
    }

    fun deleteBackup(backup: LocalBackup) {
        viewModelScope.launch {
            backupManager.deleteLocalBackup(backup)
            refreshBackups()
        }
    }

    private fun refreshBackups() {
        viewModelScope.launch {
            _backups.update { backupManager.listBackups() }
        }
    }

    private fun refreshFolderLabel() {
        viewModelScope.launch {
            _folderLabel.update { backupManager.currentFolderLabel() }
        }
    }

    /** User picked a folder via the SAF tree picker. */
    fun setBackupFolder(uri: Uri) {
        viewModelScope.launch {
            backupManager.setCustomFolder(uri)
            _backupMessage.update { "Backups will now go to your chosen folder." }
            refreshFolderLabel()
            refreshBackups()
        }
    }

    /** Drop the custom folder; revert to Downloads/FoodTracker. */
    fun clearBackupFolder() {
        viewModelScope.launch {
            backupManager.clearCustomFolder()
            _backupMessage.update { "Reverted to Downloads/FoodTracker." }
            refreshFolderLabel()
            refreshBackups()
        }
    }

    /**
     * Bulk-delete every food the user marked as custom. [cascadeLogs] = true
     * also wipes their historical food-log rows; false soft-hides them so the
     * diary keeps rendering.
     */
    fun deleteAllCustomFoods(cascadeLogs: Boolean) {
        viewModelScope.launch {
            repository.deleteAllCustomFoods(cascadeLogs)
            _backupMessage.update {
                if (cascadeLogs) "Custom foods and their log history removed."
                else "Custom foods hidden — past entries preserved."
            }
        }
    }

    /** Apply user's recurring-backup choice. WorkManager reflects it immediately. */
    fun setBackupCadence(cadence: BackupCadence) {
        viewModelScope.launch {
            profileStore.setBackupIntervalHours(cadence.hours)
            backupScheduler.apply(cadence.hours)
            _backupMessage.update {
                if (cadence == BackupCadence.Off) "Recurring backup turned off."
                else "Recurring backup set to ${cadence.label.lowercase()}."
            }
        }
    }
}
