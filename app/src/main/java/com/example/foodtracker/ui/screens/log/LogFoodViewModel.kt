package com.example.foodtracker.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.MealType
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

enum class LogTab { Search, Favourites, Recent, Frequent }

enum class OnlineSearchPhase { Idle, Loading, Empty, Error }

data class LogUiState(
    val query: String = "",
    val tab: LogTab = LogTab.Search,
    val results: List<FoodEntity> = emptyList(),
    val favourites: List<FoodEntity> = emptyList(),
    val recent: List<FoodEntity> = emptyList(),
    val frequent: List<FoodEntity> = emptyList(),
    val mealType: MealType = MealType.Breakfast,
    val selected: FoodEntity? = null,
    val selectedVariant: ServingVariant = ServingVariant.Default,
    val selectedServings: Double = 1.0,
    val onlineResults: List<OnlineSearchResult> = emptyList(),
    val onlinePhase: OnlineSearchPhase = OnlineSearchPhase.Idle,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LogFoodViewModel(
    private val repository: FoodRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _tab = MutableStateFlow(LogTab.Search)
    private val _mealType = MutableStateFlow(MealType.Breakfast)
    private val _selected = MutableStateFlow<FoodEntity?>(null)
    private val _variant = MutableStateFlow(ServingVariant.Default)
    private val _servings = MutableStateFlow(1.0)
    private val _onlineResults = MutableStateFlow<List<OnlineSearchResult>>(emptyList())
    private val _onlinePhase = MutableStateFlow(OnlineSearchPhase.Idle)

    private val resultsFlow = _query.flatMapLatest { q ->
        if (q.isBlank()) repository.observeAllFoods() else repository.searchFoods(q)
    }
    private val favouritesFlow = repository.observeFavouriteFoods()
    private val recentFlow = repository.observeRecentFoods()
    private val frequentFlow = repository.observeFrequentFoods()

    private data class Quick(
        val favourites: List<FoodEntity>,
        val recent: List<FoodEntity>,
        val frequent: List<FoodEntity>,
    )

    private val quickFlow = combine(favouritesFlow, recentFlow, frequentFlow) { f, r, fq ->
        Quick(f, r, fq)
    }

    private data class Selection(
        val food: FoodEntity?,
        val variant: ServingVariant,
        val servings: Double,
    )

    private val selectionFlow = combine(_selected, _variant, _servings) { sel, v, s ->
        Selection(sel, v, s)
    }

    private data class Online(
        val results: List<OnlineSearchResult>,
        val phase: OnlineSearchPhase,
    )

    private val onlineFlow = combine(_onlineResults, _onlinePhase) { r, p -> Online(r, p) }

    val state: StateFlow<LogUiState> = combine(
        combine(_query, _tab, _mealType) { q, t, m -> Triple(q, t, m) },
        resultsFlow,
        quickFlow,
        selectionFlow,
        onlineFlow,
    ) { headTriple, results, quick, sel, online ->
        val (q, t, m) = headTriple
        LogUiState(
            query = q,
            tab = t,
            results = results,
            favourites = quick.favourites,
            recent = quick.recent,
            frequent = quick.frequent,
            mealType = m,
            selected = sel.food,
            selectedVariant = sel.variant,
            selectedServings = sel.servings,
            onlineResults = online.results,
            onlinePhase = online.phase,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LogUiState(),
    )

    fun setQuery(q: String) {
        _query.update { q }
        if (q.isNotBlank() && _tab.value != LogTab.Search) _tab.update { LogTab.Search }
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
            _onlineResults.update { emptyList() }
            _onlinePhase.update { OnlineSearchPhase.Idle }
        }
    }
    fun setTab(tab: LogTab) = _tab.update { tab }
    fun setMealType(type: MealType) = _mealType.update { type }
    fun select(food: FoodEntity?) {
        _selected.update { food }
        _variant.update { food?.variantsOrDefault()?.firstOrNull() ?: ServingVariant.Default }
        _servings.update { 1.0 }
    }
    fun setVariant(variant: ServingVariant) = _variant.update { variant }
    fun setServings(value: Double) = _servings.update { value.coerceAtLeast(0.0) }

    fun toggleFavourite(food: FoodEntity) {
        viewModelScope.launch {
            repository.setFoodFavourite(food.id, !food.isFavourite)
        }
    }

    /** One-tap re-log: insert the food with its default variant at 1 serving. */
    fun quickLog(food: FoodEntity, onDone: () -> Unit = {}) {
        val variant = food.variantsOrDefault().firstOrNull() ?: ServingVariant.Default
        viewModelScope.launch {
            repository.addFoodLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = _mealType.value.name,
                    servings = 1.0,
                    grams = variant.grams,
                    servingLabel = variant.label,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
            onDone()
        }
    }

    fun confirmLog(onDone: () -> Unit) {
        val food = _selected.value ?: return
        val variant = _variant.value
        val servings = _servings.value
        if (servings <= 0.0) return
        viewModelScope.launch {
            repository.addFoodLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = _mealType.value.name,
                    servings = servings,
                    grams = servings * variant.grams,
                    servingLabel = variant.label,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
            _selected.update { null }
            onDone()
        }
    }
}
