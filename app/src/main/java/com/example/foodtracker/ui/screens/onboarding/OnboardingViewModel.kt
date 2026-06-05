package com.example.foodtracker.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.ActivityLevel
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.Sex
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.domain.GoalCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val profile: UserProfile = UserProfile(),
    val previewGoals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
    val saved: Boolean = false,
)

class OnboardingViewModel(private val profileStore: ProfileStore) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private fun mutate(block: (UserProfile) -> UserProfile) {
        _state.update {
            val updated = block(it.profile)
            it.copy(profile = updated, previewGoals = GoalCalculator.goalsFor(updated))
        }
    }

    fun setName(name: String) = mutate { it.copy(name = name) }
    fun setAge(age: Int) = mutate { it.copy(ageYears = age.coerceIn(10, 100)) }
    fun setSex(sex: Sex) = mutate { it.copy(sex = sex) }
    fun setHeight(cm: Int) = mutate { it.copy(heightCm = cm.coerceIn(100, 230)) }
    fun setWeight(kg: Double) = mutate { it.copy(weightKg = kg.coerceIn(30.0, 250.0)) }
    fun setActivityLevel(level: ActivityLevel) = mutate { it.copy(activityLevel = level) }
    fun setGoal(goal: Goal) = mutate { it.copy(goal = goal) }

    fun save() {
        viewModelScope.launch {
            profileStore.save(_state.value.profile, markOnboarded = true)
            _state.update { it.copy(saved = true) }
        }
    }
}
