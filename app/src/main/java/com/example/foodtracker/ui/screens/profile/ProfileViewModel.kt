package com.example.foodtracker.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.backup.BackupManager
import com.example.foodtracker.data.backup.BackupScheduler
import com.example.foodtracker.data.backup.LocalBackup
import com.example.foodtracker.domain.GoalCalculator
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

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val draft: UserProfile = UserProfile(),
    val editing: Boolean = false,
    val goals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
    val hapticsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.Amoled,
    val backups: List<LocalBackup> = emptyList(),
    val backupBusy: Boolean = false,
    val backupMessage: String? = null,
    val backupFolderLabel: String? = null,
    val backupCadence: BackupCadence = BackupCadence.Off,
)

class ProfileViewModel(
    private val profileStore: ProfileStore,
    private val backupManager: BackupManager,
    private val backupScheduler: BackupScheduler,
) : ViewModel() {

    private val _editing = MutableStateFlow(false)
    private val _draft = MutableStateFlow<UserProfile?>(null)
    private val _backups = MutableStateFlow<List<LocalBackup>>(emptyList())
    private val _backupBusy = MutableStateFlow(false)
    private val _backupMessage = MutableStateFlow<String?>(null)
    private val _folderLabel = MutableStateFlow<String?>(null)

    init {
        refreshBackups()
        refreshFolderLabel()
    }

    val state: StateFlow<ProfileUiState> = combine(
        combine(profileStore.profile, _editing, _draft, profileStore.hapticsEnabled, profileStore.themeMode) {
            profile, editing, draft, haptics, theme ->
            ProfileBase(profile, editing, draft, haptics, theme)
        },
        combine(_backups, _backupBusy, _backupMessage, _folderLabel, profileStore.backupIntervalHours) {
            backups, busy, message, label, hours ->
            BackupBase(backups, busy, message, label, BackupCadence.fromHours(hours))
        },
    ) { base, backup ->
        val effective = if (base.editing) (base.draft ?: base.profile) else base.profile
        ProfileUiState(
            profile = base.profile,
            draft = effective,
            editing = base.editing,
            goals = GoalCalculator.goalsFor(effective),
            hapticsEnabled = base.haptics,
            themeMode = base.theme,
            backups = backup.backups,
            backupBusy = backup.busy,
            backupMessage = backup.message,
            backupFolderLabel = backup.folderLabel,
            backupCadence = backup.cadence,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private data class BackupBase(
        val backups: List<LocalBackup>,
        val busy: Boolean,
        val message: String?,
        val folderLabel: String?,
        val cadence: BackupCadence,
    )

    private data class ProfileBase(
        val profile: UserProfile,
        val editing: Boolean,
        val draft: UserProfile?,
        val haptics: Boolean,
        val theme: ThemeMode,
    )

    fun beginEdit() {
        _draft.value = state.value.profile
        _editing.value = true
    }

    fun cancelEdit() {
        _editing.value = false
        _draft.value = null
    }

    fun updateDraft(transform: (UserProfile) -> UserProfile) {
        _draft.update { (it ?: state.value.profile).let(transform) }
    }

    fun save() {
        val draft = _draft.value ?: return
        viewModelScope.launch {
            profileStore.save(draft, markOnboarded = true)
            _editing.value = false
            _draft.value = null
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { profileStore.setHapticsEnabled(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { profileStore.setThemeMode(mode) }
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
