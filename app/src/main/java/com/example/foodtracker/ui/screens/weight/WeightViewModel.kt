package com.example.foodtracker.ui.screens.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.db.WeightLogEntity
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeightUiState(
    val entries: List<WeightLogEntity> = emptyList(),
    val draftKg: String = "",
    val note: String = "",
    val profile: UserProfile = UserProfile(),
    val syncToProfile: Boolean = true,
) {
    val latest: WeightLogEntity? = entries.maxByOrNull { it.dateEpochDay }
    val sevenDayAvg: Double? = entries.takeIf { it.isNotEmpty() }
        ?.filter { it.dateEpochDay >= DateUtil.todayEpochDay() - 7 }
        ?.takeIf { it.isNotEmpty() }
        ?.map { it.weightKg }
        ?.average()
    val change30d: Double? = run {
        val today = DateUtil.todayEpochDay()
        val recent = entries.filter { it.dateEpochDay >= today - 30 }.minByOrNull { it.dateEpochDay }
            ?: return@run null
        val current = latest ?: return@run null
        if (recent.id == current.id) null else current.weightKg - recent.weightKg
    }
}

class WeightViewModel(
    private val repository: FoodRepository,
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val _draft = MutableStateFlow("")
    private val _note = MutableStateFlow("")
    private val _sync = MutableStateFlow(true)

    val state: StateFlow<WeightUiState> = combine(
        repository.observeRecentWeights(180),
        profileStore.profile,
        _draft,
        _note,
        _sync,
    ) { entries, profile, draft, note, sync ->
        WeightUiState(
            entries = entries,
            draftKg = draft,
            note = note,
            profile = profile,
            syncToProfile = sync,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WeightUiState(),
    )

    fun setDraft(value: String) {
        // Permit digits + one decimal point; clamp to <=5 chars total to avoid garbage.
        val filtered = value.filter { it.isDigit() || it == '.' }.take(6)
        _draft.update { filtered }
    }

    fun setNote(value: String) = _note.update { value.take(80) }
    fun setSyncToProfile(sync: Boolean) = _sync.update { sync }

    fun saveEntry(onSaved: () -> Unit = {}) {
        val kg = _draft.value.toDoubleOrNull()?.takeIf { it in 20.0..400.0 } ?: return
        viewModelScope.launch {
            val today = DateUtil.todayEpochDay()
            repository.upsertWeight(
                WeightLogEntity(
                    dateEpochDay = today,
                    weightKg = kg,
                    note = _note.value.ifBlank { null },
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
            if (_sync.value) {
                val current = profileStore.profile.first()
                profileStore.save(current.copy(weightKg = kg), markOnboarded = false)
            }
            _draft.update { "" }
            _note.update { "" }
            onSaved()
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.deleteWeight(id) }
    }
}
