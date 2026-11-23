package com.example.foodtracker.ui.screens.plan

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
import com.example.foodtracker.data.db.PlanItemEntity
import com.example.foodtracker.data.db.PlanItemWithFood
import com.example.foodtracker.data.db.nutrients
import com.example.foodtracker.domain.GoalCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The plan editor's state: every planned line, grouped by the meal it belongs
 * to, in [MealType] order.
 *
 * Meals with nothing planned are present as empty lists rather than missing,
 * because the editor renders all four — it is the one screen where an empty
 * Lunch is information ("you have not planned lunch") rather than clutter. On
 * Today, where the plan is only a shortcut, the empty ones are dropped.
 */
data class PlanEditorState(
    val groups: Map<MealType, List<PlanItemWithFood>> = MealType.entries.associateWith { emptyList() },
    val itemCount: Int = 0,
    /**
     * Everything the plan adds up to, at the amounts planned.
     *
     * Summed here rather than in the sheet so the row that opens the breakdown
     * and the breakdown itself cannot state different totals — they are the
     * same figure, shown twice.
     */
    val totals: Nutrients = Nutrients.Zero,
    /**
     * The user's daily targets, as the reference the breakdown reads against.
     *
     * Defaulted from a default profile rather than from zeros, the same
     * compromise Today's first emission makes: a goal of zero is not "no
     * target", it is a bug, and every reading divided by it would read as a
     * shortfall for the one frame before the profile arrives.
     */
    val goals: NutrientGoals = GoalCalculator.goalsFor(UserProfile()),
)

class PlanViewModel(
    private val repository: FoodRepository,
    profileStore: ProfileStore,
) : ViewModel() {

    val state: StateFlow<PlanEditorState> = combine(
        repository.observePlan(),
        profileStore.profile,
    ) { items, profile ->
            val byMeal = items.groupBy { MealType.fromName(it.item.mealType) }
            PlanEditorState(
                groups = MealType.entries.associateWith { byMeal[it].orEmpty() },
                itemCount = items.size,
                totals = items.fold(Nutrients.Zero) { acc, row -> acc + row.nutrients() },
                goals = GoalCalculator.goalsFor(profile),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanEditorState(),
        )

    fun remove(id: Long) {
        viewModelScope.launch { repository.deletePlanItem(id) }
    }

    fun clear() {
        viewModelScope.launch { repository.clearPlan() }
    }
}

/**
 * Put a food on the plan, and find one to put there.
 *
 * Separate from [PlanViewModel] because the two have no state in common: the
 * editor reads the plan, this writes to it, and the two screens that write —
 * the picker page and Rich-in, when it was opened from the editor — have no
 * use for the plan's contents at all.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlanAddViewModel(private val repository: FoodRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /**
     * Local foods only — no online search, no barcode, no AI.
     *
     * Planning is the deliberate half of the app: you are choosing from what
     * you already eat, with Rich-in next door for when you want to discover
     * something. A network round-trip in the middle of that is a different
     * activity wearing the same control, and it is the one the Log picker
     * already offers.
     */
    val results: StateFlow<List<FoodEntity>> = _query
        .debounce(120)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeAllFoods() else repository.searchFoods(q)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setQuery(value: String) = _query.update { value }

    fun add(item: PlanItemEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.putPlanItem(item)
            onDone()
        }
    }
}

/**
 * Today's plan paired with today's diary, for the plan-for-today page.
 *
 * Reads the day from the clock on every emission rather than holding it, so a
 * phone left on this page overnight does not keep ticking lines off against
 * yesterday.
 */
data class PlanTodayState(
    val groups: Map<MealType, List<PlannedRow>> = emptyMap(),
)

class PlanTodayViewModel(private val repository: FoodRepository) : ViewModel() {

    val state: StateFlow<PlanTodayState> = combine(
        repository.observePlan(),
        repository.observeLogForDay(DateUtil.todayEpochDay()),
    ) { plan, logs ->
        PlanTodayState(groups = plannedGroups(plan, logs))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanTodayState(),
    )

    /**
     * Write a planned line into today's diary.
     *
     * An ordinary log write — the plan is a shortcut to this call and nothing
     * more. Nothing is marked on the plan itself, because the tick is read back
     * off the entry this creates; see [PlannedRow].
     */
    fun logPlanned(entry: FoodLogEntity) {
        viewModelScope.launch { repository.addFoodLog(entry) }
    }
}
