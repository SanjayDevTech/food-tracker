package com.example.foodtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class HomeUiState(
    val epochDay: Long,
    val profile: UserProfile,
    val goals: NutrientGoals,
    val logs: List<FoodLogWithFood>,
    val waterMl: Int,
    val caloriesBurned: Double,
    val favourites: List<FoodEntity> = emptyList(),
    val recent: List<FoodEntity> = emptyList(),
    val canCopyYesterday: Boolean = false,
) {
    val totalNutrients: Nutrients = logs.fold(Nutrients.Zero) { acc, row ->
        acc + (row.food.nutrientsPer100g() * (row.log.grams / 100.0))
    }
    val mealGroups: Map<MealType, List<FoodLogWithFood>> = logs.groupBy { MealType.fromName(it.log.mealType) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: FoodRepository,
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val selectedDay = MutableStateFlow(DateUtil.todayEpochDay())

    private data class CoreState(
        val profile: UserProfile,
        val logs: List<FoodLogWithFood>,
        val water: Int,
        val burned: Double,
    )

    private data class QuickState(
        val favourites: List<FoodEntity>,
        val recent: List<FoodEntity>,
        val canCopyYesterday: Boolean,
    )

    val state: StateFlow<HomeUiState> = selectedDay
        .flatMapLatest { day ->
            val coreFlow = combine(
                profileStore.profile,
                repository.observeLogForDay(day),
                repository.observeWaterTotalForDay(day),
                repository.observeCaloriesBurnedForDay(day),
            ) { profile, logs, water, burned -> CoreState(profile, logs, water, burned) }

            val quickFlow = combine(
                repository.observeFavouriteFoods(),
                repository.observeRecentFoods(limit = 8),
                repository.observeLogForDay(day - 1),
            ) { favs, recent, yLogs ->
                QuickState(
                    favourites = favs,
                    recent = recent,
                    canCopyYesterday = yLogs.isNotEmpty(),
                )
            }

            combine(coreFlow, quickFlow) { core, quick ->
                HomeUiState(
                    epochDay = day,
                    profile = core.profile,
                    goals = GoalCalculator.goalsFor(core.profile),
                    logs = core.logs,
                    waterMl = core.water,
                    caloriesBurned = core.burned,
                    favourites = quick.favourites,
                    recent = quick.recent,
                    canCopyYesterday = quick.canCopyYesterday,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                epochDay = DateUtil.todayEpochDay(),
                profile = UserProfile(),
                goals = GoalCalculator.goalsFor(UserProfile()),
                logs = emptyList(),
                waterMl = 0,
                caloriesBurned = 0.0,
            ),
        )

    fun shiftDay(delta: Int) {
        selectedDay.value = selectedDay.value + delta
    }

    fun goToToday() {
        selectedDay.value = DateUtil.todayEpochDay()
    }

    fun removeLog(id: Long) {
        viewModelScope.launch { repository.deleteFoodLog(id) }
    }

    /** Re-log a favourite/recent food using its first serving variant at 1 serving. */
    fun quickLog(food: FoodEntity) {
        val variant = food.variantsOrDefault().first()
        viewModelScope.launch {
            repository.addFoodLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = currentMealAt(LocalTime.now()).name,
                    servings = 1.0,
                    grams = variant.grams,
                    servingLabel = variant.label,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    /** Copy every food entry from yesterday into today, preserving meal types. */
    fun copyYesterday() {
        viewModelScope.launch {
            val yesterday = DateUtil.todayEpochDay() - 1
            val rows = repository.observeLogForDay(yesterday).first()
            repository.copyLogsToToday(rows)
        }
    }
}

private fun currentMealAt(time: LocalTime): MealType = when {
    time.hour < 11 -> MealType.Breakfast
    time.hour < 15 -> MealType.Lunch
    time.hour < 19 -> MealType.Snack
    else -> MealType.Dinner
}
