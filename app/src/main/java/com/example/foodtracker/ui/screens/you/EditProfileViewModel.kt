package com.example.foodtracker.ui.screens.you

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

data class EditProfileUiState(
    /** What is on disk. The left-hand number in the live-targets preview. */
    val saved: UserProfile = UserProfile(),
    /** What the user is building. Falls back to [saved] until they touch something. */
    val draft: UserProfile = UserProfile(),
    val savedGoals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
    val draftGoals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
) {
    /** True once the draft diverges — drives the Save button and the `→` arrows. */
    val dirty: Boolean get() = draft != saved
}

/**
 * The Edit profile & goals page.
 *
 * This is the old `ProfileViewModel` with its `editing` flag removed. The flag
 * existed because the profile screen was one layout in two modes, and the
 * top bar had to be a view/cancel/save tri-state to say which mode you were in.
 * A page that exists only to edit does not need to be told that it is editing,
 * so the flag went and the two flows it gated collapsed into one.
 *
 * `_draft` stays nullable for the same reason it always was: null means
 * "untouched", which is what lets the page follow a profile changed elsewhere
 * (onboarding, a restored backup) right up until the user types, and is also
 * how [EditProfileUiState.dirty] can be a plain `!=` rather than a flag the
 * setters have to remember to raise.
 *
 * Goals are computed for *both* profiles because the live preview shows
 * `Calories 2 100 → 2 040`: you used to have to save, back out and find the
 * Daily targets card to see what moving the activity slider had done.
 * [GoalCalculator.goalsFor] is pure, so the second call is free.
 */
class EditProfileViewModel(
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val _draft = MutableStateFlow<UserProfile?>(null)

    val state: StateFlow<EditProfileUiState> = combine(
        profileStore.profile,
        _draft,
    ) { saved, draft ->
        val effective = draft ?: saved
        EditProfileUiState(
            saved = saved,
            draft = effective,
            savedGoals = GoalCalculator.goalsFor(saved),
            draftGoals = GoalCalculator.goalsFor(effective),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditProfileUiState())

    fun updateDraft(transform: (UserProfile) -> UserProfile) {
        _draft.update { (it ?: state.value.saved).let(transform) }
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

    /**
     * Persist, then hand control back.
     *
     * [onSaved] is a callback rather than something the screen runs straight
     * after calling this, because popping the page clears this view model and
     * cancels [viewModelScope] with it. Fire-and-navigate would race the
     * DataStore write it just started.
     */
    fun save(onSaved: () -> Unit) {
        val draft = _draft.value
        if (draft == null) {
            onSaved()
            return
        }
        viewModelScope.launch {
            profileStore.save(draft, markOnboarded = true)
            _draft.value = null
            onSaved()
        }
    }
}
