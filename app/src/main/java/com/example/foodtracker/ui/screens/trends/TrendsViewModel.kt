package com.example.foodtracker.ui.screens.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DailyTotals(
    val epochDay: Long,
    val nutrients: Nutrients,
    val waterMl: Int,
    val caloriesBurned: Double,
)

data class TrendsUiState(
    val days: List<DailyTotals> = emptyList(),
    val goals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
)

class TrendsViewModel(
    private val repository: FoodRepository,
    profileStore: ProfileStore,
) : ViewModel() {

    private val today = DateUtil.todayEpochDay()
    private val startDay = today - 6

    val state: StateFlow<TrendsUiState> = combine(
        repository.observeLogForRange(startDay, today),
        repository.observeWaterTotalsForRange(startDay, today),
        repository.observeCaloriesBurnedForRange(startDay, today),
        profileStore.profile,
    ) { logs, waters, burns, profile ->
        val nutrientsByDay = logs.groupBy { it.log.dateEpochDay }
            .mapValues { (_, rows) ->
                rows.fold(Nutrients.Zero) { acc, row ->
                    acc + (row.food.nutrientsPer100g() * (row.log.grams / 100.0))
                }
            }
        val waterMap = waters.associate { it.dateEpochDay to it.totalMl }
        val burnMap = burns.associate { it.dateEpochDay to it.totalCalories }
        val days = (0L..6L).map { offset ->
            val day = startDay + offset
            DailyTotals(
                epochDay = day,
                nutrients = nutrientsByDay[day] ?: Nutrients.Zero,
                waterMl = waterMap[day] ?: 0,
                caloriesBurned = burnMap[day] ?: 0.0,
            )
        }
        TrendsUiState(days = days, goals = GoalCalculator.goalsFor(profile))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsUiState())
}
