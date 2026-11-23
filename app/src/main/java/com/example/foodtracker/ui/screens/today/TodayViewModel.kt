package com.example.foodtracker.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.CookingOil
import com.example.foodtracker.data.StyleOption
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.UserProfile
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.data.db.PlanItemWithFood
import com.example.foodtracker.data.db.nutrients
import com.example.foodtracker.ui.screens.plan.plannedRemaining
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

data class TodayUiState(
    val epochDay: Long,
    val profile: UserProfile,
    val goals: NutrientGoals,
    val logs: List<FoodLogWithFood>,
    val waterMl: Int,
    val plan: List<PlanItemWithFood> = emptyList(),
    val latestWeight: WeightLogEntity? = null,
    /**
     * False only for the synthetic first emission.
     *
     * That emission is built from a **default** [UserProfile], so its goals are
     * not the user's — a 2 468 kcal target that belongs to nobody. The plan is
     * right that this screen should not have a spinner, but rendering
     * fabricated numbers as though they were real is worse than rendering none:
     * the figures are plausible, so there is nothing to tell the reader they
     * are about to change. The screen shows its structure with em-dashes until
     * this flips, then crossfades.
     */
    val loaded: Boolean = true,
) {
    val totalNutrients: Nutrients = logs.fold(Nutrients.Zero) { acc, row ->
        acc + row.nutrients()
    }
    val mealGroups: Map<MealType, List<FoodLogWithFood>> = logs.groupBy { MealType.fromName(it.log.mealType) }

    /** True only when the day on screen is the one a tick would be written against. */
    val isToday: Boolean = epochDay == DateUtil.todayEpochDay()

    /**
     * How many planned lines are not yet in today's diary — the one number the
     * plan row on this screen shows.
     *
     * Today deliberately holds the count and not the rows. The list itself
     * lives on the plan-for-today page: a whole day planned is ~10 rows
     * mirroring the meal structure directly above the real meal structure,
     * which pushed the diary and the nutrient summary off the fold. That is why
     * the block became a page.
     *
     * Zero on any day but today, so the row disappears while you are looking at
     * last Tuesday — a tick there would write to today's diary, which is not the
     * day on screen.
     */
    val planRemaining: Int = if (isToday) plannedRemaining(plan, logs) else 0

    /** Whether the plan row appears at all. Absent for anyone who never set one. */
    val hasPlan: Boolean = isToday && plan.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: FoodRepository,
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val selectedDay = MutableStateFlow(DateUtil.todayEpochDay())

    /** The last swipe-deleted entry, held until the undo snackbar goes away. */
    private val pendingUndo = java.util.concurrent.atomic.AtomicReference<FoodLogEntity?>(null)

    private data class CoreState(
        val profile: UserProfile,
        val logs: List<FoodLogWithFood>,
        val water: Int,
    )

    val state: StateFlow<TodayUiState> = selectedDay
        .flatMapLatest { day ->
            val coreFlow = combine(
                profileStore.profile,
                repository.observeLogForDay(day),
                repository.observeWaterTotalForDay(day),
            ) { profile, logs, water -> CoreState(profile, logs, water) }

            // Weight is the one non-core source left on this screen: it feeds
            // Today's stat tile only, so it is combined in directly rather than
            // through a second state holder. The favourites and recent-food legs
            // that used to share that holder existed for the quick-log row, which
            // is gone — every write to the diary now goes through the serving
            // sheet, so Home no longer needs a food list of its own.
            combine(
                coreFlow,
                repository.observeLatestWeight(),
                repository.observePlan(),
            ) { core, weight, plan ->
                TodayUiState(
                    epochDay = day,
                    profile = core.profile,
                    goals = GoalCalculator.goalsFor(core.profile),
                    logs = core.logs,
                    waterMl = core.water,
                    plan = plan,
                    latestWeight = weight,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(
                epochDay = DateUtil.todayEpochDay(),
                profile = UserProfile(),
                goals = GoalCalculator.goalsFor(UserProfile()),
                logs = emptyList(),
                waterMl = 0,
                loaded = false,
            ),
        )

    /** Jump straight to a day (the new DayStrip picks one, it doesn't step). */
    fun setDay(epochDay: Long) {
        selectedDay.value = epochDay.coerceAtMost(DateUtil.todayEpochDay())
    }

    /** Mirror of the picker flow's behaviour: remember the pick immediately. */
    fun rememberVariant(foodId: Long, oil: CookingOil?, style: StyleOption?) {
        viewModelScope.launch {
            repository.rememberVariant(foodId, oil?.name, style?.label)
        }
    }

    fun removeLog(id: Long) {
        viewModelScope.launch { repository.deleteFoodLog(id) }
    }

    /**
     * Delete an entry, keeping it in memory so [undoDelete] can put it back.
     *
     * Swipe-to-delete without undo would be strictly worse than the four trash
     * icons it replaces: the icons at least required a deliberate tap. The
     * window is deliberately held by the caller's snackbar rather than a timer
     * here — when the snackbar goes, so does the chance to undo, and those two
     * should not be able to disagree.
     */
    fun deleteWithUndo(entry: FoodLogEntity) {
        pendingUndo.set(entry)
        viewModelScope.launch { repository.deleteFoodLog(entry.id) }
    }

    /**
     * Re-insert the last deleted entry.
     *
     * Re-inserted rather than resurrected: the row is gone, so this writes a
     * new one. `id` is reset to 0 so Room assigns a fresh primary key — passing
     * the old id back would reuse an identifier the rest of the app may still
     * be holding a reference to.
     */
    fun undoDelete() {
        val entry = pendingUndo.getAndSet(null) ?: return
        viewModelScope.launch { repository.addFoodLog(entry.copy(id = 0)) }
    }

    /** Drop the undo buffer — the snackbar has gone, the delete is final. */
    fun clearUndo() {
        pendingUndo.set(null)
    }

    /**
     * Commit an edit made in the serving sheet.
     *
     * The sheet hands back a complete row because it has already recomputed
     * grams, servings and the preparation variant together; splitting that into
     * per-field setters would let the three disagree halfway through.
     */
    fun editLog(entry: FoodLogEntity) {
        viewModelScope.launch { repository.updateFoodLog(entry) }
    }
}

