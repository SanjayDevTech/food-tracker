package com.example.foodtracker.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.domain.GoalCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val draft: UserProfile = UserProfile(),
    val editing: Boolean = false,
    val goals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
)

/**
 * The user's own details and the targets derived from them. App behaviour —
 * appearance, AI autofill, backups, custom foods — lives in
 * [com.example.foodtracker.ui.screens.settings.SettingsViewModel] instead.
 */
class ProfileViewModel(
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val _editing = MutableStateFlow(false)
    private val _draft = MutableStateFlow<UserProfile?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        profileStore.profile,
        _editing,
        _draft,
    ) { profile, editing, draft ->
        val effective = if (editing) (draft ?: profile) else profile
        ProfileUiState(
            profile = profile,
            draft = effective,
            editing = editing,
            goals = GoalCalculator.goalsFor(effective),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

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

    /**
     * Update goal AND seed a sensible default goal weight when entering
     * Lose/Gain for the first time. Clearing goal back to Maintain wipes
     * the goal weight so we don't carry a stale target.
     */
    fun setDraftGoal(goal: Goal) = updateDraft { p ->
        val nextGoalWeight = when (goal) {
            Goal.Maintain -> null
            Goal.Lose -> p.goalWeightKg ?: (p.weightKg - 5.0).coerceAtLeast(30.0)
            Goal.Gain -> p.goalWeightKg ?: (p.weightKg + 5.0).coerceAtMost(250.0)
        }
        p.copy(goal = goal, goalWeightKg = nextGoalWeight)
    }

    fun save() {
        val draft = _draft.value ?: return
        viewModelScope.launch {
            profileStore.save(draft, markOnboarded = true)
            _editing.value = false
            _draft.value = null
        }
    }
}
