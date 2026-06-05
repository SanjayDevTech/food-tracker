package com.example.foodtracker.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.ActivityType
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.db.ActivityLogEntity
import com.example.foodtracker.data.db.DailyCaloriesTotal
import com.example.foodtracker.domain.CalorieBurn
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityUiState(
    val entries: List<ActivityLogEntity> = emptyList(),
    val totalBurned: Double = 0.0,
    val profile: UserProfile = UserProfile(),
    val selectedType: ActivityType = ActivityType.Walking,
    val minutes: Int = 30,
    val weekTotals: List<Pair<Long, Double>> = emptyList(),
) {
    val previewBurned: Double = CalorieBurn.caloriesBurned(selectedType.metValue, minutes, profile.weightKg)
}

private data class DailyData(
    val entries: List<ActivityLogEntity>,
    val totalBurned: Double,
    val weekTotals: List<Pair<Long, Double>>,
)

private data class FormData(
    val type: ActivityType,
    val minutes: Int,
)

class ActivityViewModel(
    private val repository: FoodRepository,
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val today = DateUtil.todayEpochDay()
    private val _selectedType = MutableStateFlow(ActivityType.Walking)
    private val _minutes = MutableStateFlow(30)

    private val daily = combine(
        repository.observeActivityForDay(today),
        repository.observeCaloriesBurnedForDay(today),
        repository.observeCaloriesBurnedForRange(today - 6, today),
    ) { entries, total, range ->
        val weekMap = range.associate { it.dateEpochDay to it.totalCalories }
        DailyData(
            entries = entries,
            totalBurned = total,
            weekTotals = (0L..6L).map { offset ->
                val day = today - 6 + offset
                day to (weekMap[day] ?: 0.0)
            },
        )
    }

    private val form = combine(_selectedType, _minutes) { type, minutes -> FormData(type, minutes) }

    val state: StateFlow<ActivityUiState> = combine(
        daily,
        profileStore.profile,
        form,
    ) { d, profile, f ->
        ActivityUiState(
            entries = d.entries,
            totalBurned = d.totalBurned,
            profile = profile,
            selectedType = f.type,
            minutes = f.minutes,
            weekTotals = d.weekTotals,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    fun setType(type: ActivityType) = _selectedType.update { type }
    fun setMinutes(min: Int) = _minutes.update { min.coerceIn(1, 360) }

    fun logCurrent() {
        viewModelScope.launch {
            val profile = state.value.profile
            val type = _selectedType.value
            val minutes = _minutes.value
            val burned = CalorieBurn.caloriesBurned(type.metValue, minutes, profile.weightKg)
            repository.addActivity(
                ActivityLogEntity(
                    dateEpochDay = today,
                    activityType = type.name,
                    minutes = minutes,
                    caloriesBurned = burned,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch { repository.deleteActivity(id) }
    }
}
