package com.example.foodtracker.ui.screens.rich

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.AllNutrientFields
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.NutrientTarget
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.domain.GoalCalculator
import com.example.foodtracker.domain.NutrientRanking
import com.example.foodtracker.domain.RichFood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * The nutrients this screen can search for: every field with a daily goal to
 * measure against, minus the capped ones.
 *
 * [NutrientTarget.Limit] fields are excluded on purpose. Sugar, sodium,
 * saturated fat and cholesterol are numbers to stay under, so "foods richest
 * in sodium" would be a league table of what to avoid presented exactly like a
 * league table of what to eat. Calories are excluded for the same reason —
 * "richest in calories" is not a search anyone should be nudged towards by a
 * tracker. Ranking a limit needs its own inverted screen, not a chip here.
 */
val RichSearchableFields: List<NutrientField> =
    AllNutrientFields.filter { it.target != NutrientTarget.Limit && it.key != "calories" }

data class RichFoodsState(
    val selectedKeys: Set<String> = emptySet(),
    val results: List<RichFood> = emptyList(),
    val goals: NutrientGoals? = null,
    /**
     * The picked nutrient that no food has — the one single-handedly emptying
     * the list. Null unless the list IS empty.
     *
     * The ranking can still reach zero results even though it never filters:
     * a food scores by its weakest nutrient, so one nutrient that nothing on
     * file contains drives every score to zero. Omega-3 does exactly this —
     * barely any food here carries a figure for it. Naming the nutrient, and
     * letting the user drop it in one tap, turns a dead end into a fix.
     */
    val blockingField: NutrientField? = null,
) {
    val selectedFields: List<NutrientField>
        get() = RichSearchableFields.filter { it.key in selectedKeys }
}

class RichFoodsViewModel(
    repository: FoodRepository,
    profileStore: ProfileStore,
) : ViewModel() {

    private val selected = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<RichFoodsState> = combine(
        repository.observeAllFoods(),
        profileStore.profile,
        selected,
    ) { foods, profile, keys ->
        val goals = GoalCalculator.goalsFor(profile)
        val fields = RichSearchableFields.filter { it.key in keys }
        val results = NutrientRanking.rank(foods, fields, goals)
        RichFoodsState(
            selectedKeys = keys,
            results = results,
            goals = goals,
            blockingField = if (results.isNotEmpty() || fields.size < 2) null else {
                // The culprit is the nutrient whose best food is worst: the
                // list can only ever be as good as its weakest pick.
                fields.minByOrNull { field ->
                    val goal = field.goal(goals)
                    if (goal <= 0.0) 0.0 else foods.maxOfOrNull {
                        field.read(it.nutrientsPer100g() * (it.servingGrams / 100.0)) / goal
                    } ?: 0.0
                }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RichFoodsState())

    fun toggle(key: String) {
        selected.value = selected.value.let { if (key in it) it - key else it + key }
    }

    fun clear() {
        selected.value = emptySet()
    }
}
