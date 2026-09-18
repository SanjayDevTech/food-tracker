package com.example.foodtracker.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.db.DailyWaterTotal
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.data.db.WeightLogEntity
import com.example.foodtracker.ui.components.LinePoint
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

/** Trailing window the progress charts summarise. */
enum class ProgressRange(val label: String, val days: Int) {
    Week("Week", 7),
    Month("Month", 30),
    ThreeMonths("3 months", 90),
    SixMonths("6 months", 180),
}

data class ProgressUiState(
    val range: ProgressRange = ProgressRange.Week,
    val caloriesIn: List<LinePoint> = emptyList(),
    val caloriesInAvg: Int = 0,
    val weight: List<LinePoint> = emptyList(),
    val weightChange: Double? = null,
    val goalWeightKg: Double? = null,
    val water: List<LinePoint> = emptyList(),
    val waterAvgMl: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val repository: FoodRepository,
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val selectedRange = MutableStateFlow(ProgressRange.Week)

    fun setRange(range: ProgressRange) {
        selectedRange.value = range
    }

    val state: StateFlow<ProgressUiState> = selectedRange
        .flatMapLatest { range ->
            val today = DateUtil.todayEpochDay()
            val start = today - range.days + 1
            combine(
                repository.observeLogForRange(start, today),
                repository.observeWeightsForRange(start, today),
                repository.observeWaterTotalsForRange(start, today),
                profileStore.profile,
            ) { logs, weights, water, profile ->
                buildState(range, start, today, logs, weights, water, profile)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    private fun buildState(
        range: ProgressRange,
        start: Long,
        today: Long,
        logs: List<FoodLogWithFood>,
        weights: List<WeightLogEntity>,
        water: List<DailyWaterTotal>,
        profile: UserProfile,
    ): ProgressUiState {
        val days = (start..today).toList()
        // For a week the day-of-week reads best; longer windows need a date so
        // repeated weekday names don't collapse together.
        val labelFor: (Long) -> String =
            if (range == ProgressRange.Week) DateUtil::shortDay else DateUtil::shortDate

        // Calories eaten per day — same math as HomeViewModel.totalNutrients
        // (per-100g nutrients scaled by the logged grams).
        val calByDay = logs.groupBy { it.log.dateEpochDay }
            .mapValues { (_, rows) -> rows.sumOf { it.food.calories * it.log.grams / 100.0 } }
        val caloriesIn = days.map { LinePoint(labelFor(it), calByDay[it] ?: 0.0) }
        val loggedCalDays = days.count { (calByDay[it] ?: 0.0) > 0.0 }
        val caloriesInAvg =
            if (loggedCalDays > 0) (days.sumOf { calByDay[it] ?: 0.0 } / loggedCalDays).roundToInt() else 0

        val waterByDay = water.associate { it.dateEpochDay to it.totalMl }
        val waterPts = days.map { LinePoint(labelFor(it), (waterByDay[it] ?: 0).toDouble()) }
        val waterAvgMl = if (days.isNotEmpty())
            (days.sumOf { (waterByDay[it] ?: 0).toDouble() } / days.size).roundToInt() else 0

        // Weight is logged sparsely, not daily — plot only real weigh-ins so the
        // line reflects actual measurements rather than zero-filled gaps.
        val weightSorted = weights.sortedBy { it.dateEpochDay }
        val weightPts = weightSorted.map { LinePoint(labelFor(it.dateEpochDay), it.weightKg) }
        val weightChange =
            if (weightSorted.size >= 2) weightSorted.last().weightKg - weightSorted.first().weightKg else null

        return ProgressUiState(
            range = range,
            caloriesIn = caloriesIn,
            caloriesInAvg = caloriesInAvg,
            weight = weightPts,
            weightChange = weightChange,
            goalWeightKg = profile.goalWeightKg,
            water = waterPts,
            waterAvgMl = waterAvgMl,
        )
    }
}
