package com.example.foodtracker.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.CookingOil
import com.example.foodtracker.data.StyleOption
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.OnlineSearchResult
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Which set of foods the picker is showing — design-plan §4.4, decision 2.
 *
 * **Four tabs, not five.** `Recent` and `Frequent` merged into [Often]: on a
 * repetitive diet the two lists are ~80% identical, so they were two tabs
 * showing the same dozen foods in a slightly different order, and the user had
 * to guess which order they wanted before they could look. `Search` is gone
 * too — it was a tab *and* a field, which meant the field only filtered that
 * one tab and searching your own favourites was impossible. One mental model
 * now: the tab picks the set, the field narrows it.
 */
enum class LogTab { Often, Favourites, All, Custom }

/** Sub-filter inside the Custom tab — choose between active and soft-deleted rows. */
enum class CustomFilter { Visible, Hidden }

enum class OnlineSearchPhase { Idle, Loading, Empty, Error }

data class LogUiState(
    val query: String = "",
    val tab: LogTab = LogTab.Often,
    val often: List<FoodEntity> = emptyList(),
    val favourites: List<FoodEntity> = emptyList(),
    val all: List<FoodEntity> = emptyList(),
    val customVisible: List<FoodEntity> = emptyList(),
    val customHidden: List<FoodEntity> = emptyList(),
    val customFilter: CustomFilter = CustomFilter.Visible,
    val mealType: MealType = MealType.Breakfast,
    val selected: FoodEntity? = null,
    val selectedVariant: ServingVariant = ServingVariant.Default,
    val selectedServings: Double = 1.0,
    val selectedOil: CookingOil? = null,
    val selectedStyle: StyleOption? = null,
    val onlineResults: List<OnlineSearchResult> = emptyList(),
    val onlinePhase: OnlineSearchPhase = OnlineSearchPhase.Idle,
) {
    /** The list the active tab is showing, so the screen does not re-derive it. */
    val visibleList: List<FoodEntity>
        get() = when (tab) {
            LogTab.Often -> often
            LogTab.Favourites -> favourites
            LogTab.All -> all
            LogTab.Custom -> if (customFilter == CustomFilter.Hidden) customHidden else customVisible
        }
}

/**
 * Matches the DAO's `search` predicate — name or brand, substring,
 * case-insensitive.
 *
 * The four short lists are filtered here rather than in SQL because they are
 * already in memory and at most a few dozen rows: a round trip to Room per
 * keystroke, per tab, to re-filter twelve recent foods would be slower than the
 * filter. Only the All tab, which is the whole 166-row catalogue and is
 * `LIMIT 100` in SQL, stays a query.
 */
private fun List<FoodEntity>.matching(query: String): List<FoodEntity> {
    if (query.isBlank()) return this
    val q = query.trim()
    return filter {
        it.name.contains(q, ignoreCase = true) || it.brand?.contains(q, ignoreCase = true) == true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogFoodViewModel(
    private val repository: FoodRepository,
    private val profileStore: ProfileStore,
    initialMeal: MealType = MealType.Breakfast,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _tab = MutableStateFlow(LogTab.Often)
    private val _customFilter = MutableStateFlow(CustomFilter.Visible)
    private val _mealType = MutableStateFlow(initialMeal)
    private val _selected = MutableStateFlow<FoodEntity?>(null)
    private val _variant = MutableStateFlow(ServingVariant.Default)
    private val _servings = MutableStateFlow(1.0)
    /** Oil chosen for the entry being composed. Null = the food's own default. */
    private val _oil = MutableStateFlow<CookingOil?>(null)
    private val _style = MutableStateFlow<StyleOption?>(null)
    private val _onlineResults = MutableStateFlow<List<OnlineSearchResult>>(emptyList())
    private val _onlinePhase = MutableStateFlow(OnlineSearchPhase.Idle)

    private val allFlow = _query.flatMapLatest { q ->
        if (q.isBlank()) repository.observeAllFoods() else repository.searchFoods(q)
    }
    private val favouritesFlow = repository.observeFavouriteFoods()
    private val recentFlow = repository.observeRecentFoods()
    private val frequentFlow = repository.observeFrequentFoods()
    private val customVisibleFlow = repository.observeCustomVisibleFoods()
    private val customHiddenFlow = repository.observeCustomHiddenFoods()

    /**
     * Recent and frequent, merged — decision 2.
     *
     * Recency leads. What you ate an hour ago is the single best predictor of
     * what you are about to log (the same meal two days running, the second
     * idli, the coffee that follows lunch), and a food you eat often but ate
     * this morning is already near the top of recents anyway. The frequents
     * then fill in behind, de-duplicated by id, so a staple you last had a
     * fortnight ago is still one tap away without pushing today's food down.
     */
    private val oftenFlow = combine(recentFlow, frequentFlow) { recent, frequent ->
        val seen = recent.mapTo(mutableSetOf()) { it.id }
        recent + frequent.filterNot { it.id in seen }
    }

    private data class Lists(
        val often: List<FoodEntity>,
        val favourites: List<FoodEntity>,
        val customVisible: List<FoodEntity>,
        val customHidden: List<FoodEntity>,
    )

    private val listsFlow = combine(
        oftenFlow, favouritesFlow, customVisibleFlow, customHiddenFlow,
    ) { often, favourites, customVisible, customHidden ->
        Lists(often, favourites, customVisible, customHidden)
    }

    private data class Selection(
        val food: FoodEntity?,
        val variant: ServingVariant,
        val servings: Double,
        val oil: CookingOil?,
        val style: StyleOption?,
    )

    private val selectionFlow = combine(
        _selected,
        _variant,
        _servings,
        _oil,
        _style,
    ) { sel, v, s, pickedOil, pickedStyle ->
        // Fall back to what this food remembers, so the sheet opens on the
        // preparation the user last chose rather than on nothing.
        Selection(
            food = sel,
            variant = v,
            servings = s,
            oil = sel?.let { pickedOil ?: it.preselectedOil() },
            style = sel?.let { pickedStyle ?: it.preselectedStyle() },
        )
    }

    private data class Online(
        val results: List<OnlineSearchResult>,
        val phase: OnlineSearchPhase,
    )

    private val onlineFlow = combine(_onlineResults, _onlinePhase) { r, p -> Online(r, p) }

    private data class Head(
        val query: String,
        val tab: LogTab,
        val meal: MealType,
        val customFilter: CustomFilter,
    )

    private val headFlow = combine(_query, _tab, _mealType, _customFilter) { q, t, m, cf ->
        Head(q, t, m, cf)
    }

    val state: StateFlow<LogUiState> = combine(
        headFlow,
        allFlow,
        listsFlow,
        selectionFlow,
        onlineFlow,
    ) { head, all, lists, sel, online ->
        // The query narrows every tab, not just one. The All tab is already
        // filtered by SQL; the rest are filtered here against the same
        // predicate so "dos" means the same thing on every tab.
        LogUiState(
            query = head.query,
            tab = head.tab,
            often = lists.often.matching(head.query),
            favourites = lists.favourites.matching(head.query),
            all = all,
            customVisible = lists.customVisible.matching(head.query),
            customHidden = lists.customHidden.matching(head.query),
            customFilter = head.customFilter,
            mealType = head.meal,
            selected = sel.food,
            selectedVariant = sel.variant,
            selectedServings = sel.servings,
            selectedOil = sel.oil,
            selectedStyle = sel.style,
            onlineResults = online.results,
            onlinePhase = online.phase,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        // First frame uses the constructor's initial meal — otherwise the
        // title briefly reads Breakfast (the data-class default) before the
        // combine flow's first emission lands, producing a visible "morph"
        // when the user opened Log Food from a non-Breakfast meal row.
        LogUiState(mealType = initialMeal),
    )

    /**
     * Typing no longer moves the user to another tab.
     *
     * It used to force a jump to the Search tab, which is what made "search my
     * favourites" impossible: the moment you typed, you were not on favourites
     * any more. The tab is the user's choice about *which set*; the query is
     * their choice about *which rows*, and the two are independent.
     */
    fun setQuery(q: String) {
        _query.update { q }
        // Any new keystroke invalidates the previous online search.
        if (_onlinePhase.value != OnlineSearchPhase.Idle) {
            _onlineResults.update { emptyList() }
            _onlinePhase.update { OnlineSearchPhase.Idle }
        }
    }

    /**
     * Fetch results from OpenFoodFacts + USDA for the current query. Manual —
     * triggered by the "Search online" button, never automatic. Local DB
     * remains untouched until the user picks a result.
     */
    fun searchOnline() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        _onlinePhase.update { OnlineSearchPhase.Loading }
        _onlineResults.update { emptyList() }
        viewModelScope.launch {
            val results = runCatching { repository.searchOnline(q) }.getOrNull()
            if (results == null) {
                _onlinePhase.update { OnlineSearchPhase.Error }
            } else {
                _onlineResults.update { results }
                _onlinePhase.update { if (results.isEmpty()) OnlineSearchPhase.Empty else OnlineSearchPhase.Idle }
            }
        }
    }

    /**
     * Persist an online-search result and immediately open the serving sheet for it.
     */
    fun pickOnline(result: OnlineSearchResult) {
        viewModelScope.launch {
            val saved = repository.saveOnlineResult(result.food)
            _selected.update { saved }
            _variant.update { saved.variantsOrDefault().firstOrNull() ?: ServingVariant.Default }
            _servings.update { 1.0 }
            _oil.value = null
            _style.value = null
            _onlineResults.update { emptyList() }
            _onlinePhase.update { OnlineSearchPhase.Idle }
        }
    }
    fun setTab(tab: LogTab) = _tab.update { tab }
    fun setCustomFilter(filter: CustomFilter) = _customFilter.update { filter }
    fun setMealType(type: MealType) = _mealType.update { type }
    fun select(food: FoodEntity?) {
        _oil.value = null
        _style.value = null
        _selected.update { food }
        _variant.update { food?.variantsOrDefault()?.firstOrNull() ?: ServingVariant.Default }
        _servings.update { 1.0 }
    }
    fun setVariant(variant: ServingVariant) = _variant.update { variant }

    fun setOil(oil: CookingOil) {
        _oil.update { oil }
        rememberVariant()
    }

    fun setStyle(style: StyleOption) {
        _style.update { style }
        rememberVariant()
    }

    /**
     * Write the current picks back to the food so the next visit opens on
     * them. Fired on selection, not on log — see [FoodDao.rememberVariant].
     */
    private fun rememberVariant() {
        val food = _selected.value ?: return
        val oil = _oil.value?.name
        val style = _style.value?.label
        viewModelScope.launch { repository.rememberVariant(food.id, oil, style) }
    }
    fun setServings(value: Double) = _servings.update { value.coerceAtLeast(0.0) }

    fun toggleFavourite(food: FoodEntity) {
        viewModelScope.launch {
            repository.setFoodFavourite(food.id, !food.isFavourite)
        }
    }

    /**
     * Remove a custom food from the user's catalog. [cascadeLogs] = true also
     * wipes every historical food-log entry that referenced it; false soft-hides
     * the row so the diary keeps rendering past entries.
     */
    fun deleteCustomFood(food: FoodEntity, cascadeLogs: Boolean) {
        if (!food.isCustom) return
        viewModelScope.launch {
            repository.deleteCustomFood(food.id, cascadeLogs)
        }
    }

    /** Bring a soft-deleted custom food back into the picker. */
    fun restoreCustomFood(food: FoodEntity) {
        if (!food.isCustom || !food.isHidden) return
        viewModelScope.launch {
            repository.restoreCustomFood(food.id)
        }
    }

    fun confirmLog(onDone: () -> Unit) {
        val food = _selected.value ?: return
        val variant = _variant.value
        val servings = _servings.value
        if (servings <= 0.0) return
        // Only record an oil for foods it actually applies to — a log row for
        // a banana claiming "Palmolein" would be noise in the diary and in any
        // later export.
        val oil = if (food.usesCookingOil()) state.value.selectedOil else null
        val style = if (food.hasStyleAxis()) state.value.selectedStyle else null
        viewModelScope.launch {
            repository.addFoodLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = _mealType.value.name,
                    servings = servings,
                    grams = servings * variant.grams,
                    servingLabel = variant.label,
                    oil = oil?.name,
                    style = style?.label,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
            _selected.update { null }
            onDone()
        }
    }
}
