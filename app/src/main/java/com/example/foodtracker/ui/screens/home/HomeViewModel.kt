package com.example.foodtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.data.db.WeightLogEntity
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val epochDay: Long,
    val profile: UserProfile,
    val goals: NutrientGoals,
    val logs: List<FoodLogWithFood>,
    val waterMl: Int,
    val caloriesBurned: Double,
    val favourites: List<FoodEntity> = emptyList(),
    val recent: List<FoodEntity> = emptyList(),
    val latestWeight: WeightLogEntity? = null,
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
        val latestWeight: WeightLogEntity?,
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
                repository.observeRecentFoods(limit = 10),
                repository.observeLatestWeight(),
            ) { favs, recent, weight ->
                QuickState(favourites = favs, recent = recent, latestWeight = weight)
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
                    latestWeight = quick.latestWeight,
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

    /**
     * Commit a quick-log entry after the user has confirmed the variant /
     * grams / servings / meal via the shared [ServingSheet]. Math mirrors
     * `LogFoodViewModel.confirmLog` so a Home shortcut and the picker flow
     * produce identical rows.
     */
    fun quickLog(
        food: FoodEntity,
        variant: ServingVariant,
        servings: Double,
        meal: MealType,
    ) {
        if (servings <= 0.0) return
        viewModelScope.launch {
            repository.addFoodLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = meal.name,
                    servings = servings,
                    grams = servings * variant.grams,
                    servingLabel = variant.label,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }
}

