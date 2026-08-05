package com.example.foodtracker.ui.screens.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.db.WaterLogEntity
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WaterUiState(
    val entries: List<WaterLogEntity> = emptyList(),
    val totalMl: Int = 0,
    val goals: NutrientGoals = GoalCalculator.goalsFor(com.example.foodtracker.data.UserProfile()),
    val weekTotals: List<Pair<Long, Int>> = emptyList(),
)

class WaterViewModel(
    private val repository: FoodRepository,
    profileStore: ProfileStore,
) : ViewModel() {

    private val today = DateUtil.todayEpochDay()

    val state: StateFlow<WaterUiState> = combine(
        repository.observeWaterForDay(today),
        repository.observeWaterTotalForDay(today),
        profileStore.profile.map { GoalCalculator.goalsFor(it) },
        repository.observeWaterTotalsForRange(today - 6, today).map { it.associate { d -> d.dateEpochDay to d.totalMl } },
    ) { entries, total, goals, weekMap ->
        val week = (0L..6L).map { offset ->
            val day = today - 6 + offset
            day to (weekMap[day] ?: 0)
        }
        WaterUiState(entries, total, goals, week)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WaterUiState())

    fun addWater(ml: Int) {
        viewModelScope.launch {
            repository.addWater(
                WaterLogEntity(
                    dateEpochDay = today,
                    amountMl = ml,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    fun removeWater(id: Long) {
        viewModelScope.launch { repository.deleteWater(id) }
    }
}
