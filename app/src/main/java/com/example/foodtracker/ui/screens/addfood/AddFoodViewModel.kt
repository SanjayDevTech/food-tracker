package com.example.foodtracker.ui.screens.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.AiNutrition
import com.example.foodtracker.data.FoodCategory
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.NutritionBasis
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ProfileStore
import com.example.foodtracker.data.db.FoodEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddFoodFormState(
    val name: String = "",
    val brand: String = "",
    val barcode: String? = null,
    val category: FoodCategory = FoodCategory.Custom,
    val servingGrams: String = "100",
    val servingLabel: String = "1 serving (100 g)",
    /** Which basis the user is entering values in; we scale to per-100g on save. */
    val basis: NutritionBasis = NutritionBasis.Per100g,
    val calories: String = "",
    val carbs: String = "",
    val sugar: String = "",
    val fiber: String = "",
    val protein: String = "",
    val fat: String = "",
    val saturatedFat: String = "",
    val omega3: String = "",
    val sodium: String = "",
    val cholesterol: String = "",
    val calcium: String = "",
    val iron: String = "",
    val magnesium: String = "",
    val potassium: String = "",
    val zinc: String = "",
    val phosphorus: String = "",
    val selenium: String = "",
    val copper: String = "",
    val manganese: String = "",
    val vitaminA: String = "",
    val vitaminC: String = "",
    val vitaminD: String = "",
    val vitaminE: String = "",
    val vitaminK: String = "",
    val vitaminB12: String = "",
    val folate: String = "",
    val vitaminB1: String = "",
    val vitaminB2: String = "",
    val vitaminB3: String = "",
    val vitaminB6: String = "",
    val saved: Boolean = false,
    /** Whether the "Ask AI" affordance is available (global setting is on). */
    val aiEnabled: Boolean = false,
    /** Current stage of the two-pass AI autofill (Idle when not running). */
    val aiStep: AiStep = AiStep.Idle,
    /** Whether this run includes the verify pass (skipped for premium models). */
    val aiVerifies: Boolean = true,
    val aiError: String? = null,
)

/** Stages of the AI autofill, surfaced as an animated progress dialog. */
enum class AiStep { Idle, Fetching, Verifying, Done }

class AddFoodViewModel(
    private val repository: FoodRepository,
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AddFoodFormState())
    val state: StateFlow<AddFoodFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileStore.aiEnabled.collect { enabled ->
                _state.update { it.copy(aiEnabled = enabled) }
            }
        }
    }

    fun update(transform: (AddFoodFormState) -> AddFoodFormState) {
        _state.update(transform)
    }

    /**
     * Two-pass AI autofill for the food the user named:
     *  1. Fetch — an AI proposes a typical serving + per-serving nutrition, which
     *     is prefilled into the form immediately.
     *  2. Verify — a second AI pass fact-checks and corrects those values; if it
     *     succeeds the form is updated with the verified numbers, otherwise the
     *     prefilled values are kept.
     * Progress is surfaced via [AddFoodFormState.aiStep]. The form stays fully
     * editable throughout. No-op unless the user typed a name / AI is enabled.
     */
    fun autofillFromAi(instructions: String = "") {
        val name = _state.value.name.trim()
        if (name.isBlank() || _state.value.aiStep != AiStep.Idle) return
        _state.update { it.copy(aiStep = AiStep.Fetching, aiError = null) }
        viewModelScope.launch {
            val provider = profileStore.aiProvider.first()
            val key = profileStore.aiApiKey.first()
            val aiModel = provider.modelFromId(profileStore.aiModelId.first())
            val model = aiModel.id
            // Premium models are strong enough on the first pass — skip the
            // extra verify call to save cost/latency.
            val verifies = !aiModel.premium
            if (key.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        aiStep = AiStep.Idle,
                        aiError = "Add your ${provider.displayName} API key in Settings first.",
                    )
                }
                return@launch
            }
            _state.update { it.copy(aiVerifies = verifies) }

            val first = repository.fetchAiNutrition(name, instructions, provider, model, key)
            if (first == null) {
                _state.update {
                    it.copy(
                        aiStep = AiStep.Idle,
                        aiError = repository.lastAiError()
                            ?: "Couldn't fetch nutrition. Check your key and connection, then try again.",
                    )
                }
                return@launch
            }
            // Prefill immediately, then run the verification pass unless skipped.
            applyResult(first)

            if (verifies) {
                _state.update { it.copy(aiStep = AiStep.Verifying) }
                val verified = repository.verifyAiNutrition(name, first, instructions, provider, model, key)
                if (verified != null) applyResult(verified)
            }

            // Brief "done" beat so the success animation reads before dismissing.
            _state.update { it.copy(aiStep = AiStep.Done) }
            delay(900)
            _state.update { it.copy(aiStep = AiStep.Idle) }
        }
    }

    /** Write an AI result into the form on the per-serving basis. */
    private fun applyResult(result: AiNutrition) {
        val n = result.nutrients
        _state.update { s ->
            s.copy(
                aiError = null,
                // AI returns a typical serving + per-serving values; set the
                // serving and enter values on the PerServing basis so the save
                // path scales them to per-100 g for storage.
                category = result.category,
                servingGrams = result.servingGrams.formatGrams(),
                servingLabel = result.servingLabel,
                basis = NutritionBasis.PerServing,
                calories = n.calories.formatField(),
                carbs = n.carbs.formatField(),
                sugar = n.sugar.formatField(),
                fiber = n.fiber.formatField(),
                protein = n.protein.formatField(),
                fat = n.fat.formatField(),
                saturatedFat = n.saturatedFat.formatField(),
                omega3 = n.omega3.formatField(),
                sodium = n.sodium.formatField(),
                cholesterol = n.cholesterol.formatField(),
                calcium = n.calcium.formatField(),
                iron = n.iron.formatField(),
                magnesium = n.magnesium.formatField(),
                potassium = n.potassium.formatField(),
                zinc = n.zinc.formatField(),
                phosphorus = n.phosphorus.formatField(),
                selenium = n.selenium.formatField(),
                copper = n.copper.formatField(),
                manganese = n.manganese.formatField(),
                vitaminA = n.vitaminA.formatField(),
                vitaminC = n.vitaminC.formatField(),
                vitaminD = n.vitaminD.formatField(),
                vitaminE = n.vitaminE.formatField(),
                vitaminK = n.vitaminK.formatField(),
                vitaminB12 = n.vitaminB12.formatField(),
                folate = n.folate.formatField(),
                vitaminB1 = n.vitaminB1.formatField(),
                vitaminB2 = n.vitaminB2.formatField(),
                vitaminB3 = n.vitaminB3.formatField(),
                vitaminB6 = n.vitaminB6.formatField(),
            )
        }
    }

    /** Pre-fill just the barcode field — the rest of the form starts blank for manual entry. */
    fun seedBarcode(barcode: String) {
        _state.update { it.copy(barcode = barcode, category = FoodCategory.Packaged) }
    }

    fun seedFromBarcodeFood(food: FoodEntity) {
        _state.update {
            AddFoodFormState(
                name = food.name,
                brand = food.brand ?: "",
                category = FoodCategory.fromName(food.category),
                servingGrams = food.servingGrams.takeIf { it > 0 }?.toString() ?: "100",
                servingLabel = food.servingLabel,
                calories = food.calories.formatField(),
                carbs = food.carbs.formatField(),
                sugar = food.sugar.formatField(),
                fiber = food.fiber.formatField(),
                protein = food.protein.formatField(),
                fat = food.fat.formatField(),
                saturatedFat = food.saturatedFat.formatField(),
                omega3 = food.omega3.formatField(),
                sodium = food.sodium.formatField(),
                cholesterol = food.cholesterol.formatField(),
                calcium = food.calcium.formatField(),
                iron = food.iron.formatField(),
                magnesium = food.magnesium.formatField(),
                potassium = food.potassium.formatField(),
                zinc = food.zinc.formatField(),
                phosphorus = food.phosphorus.formatField(),
                selenium = food.selenium.formatField(),
                copper = food.copper.formatField(),
                manganese = food.manganese.formatField(),
                vitaminA = food.vitaminA.formatField(),
                vitaminC = food.vitaminC.formatField(),
                vitaminD = food.vitaminD.formatField(),
                vitaminE = food.vitaminE.formatField(),
                vitaminK = food.vitaminK.formatField(),
                vitaminB12 = food.vitaminB12.formatField(),
                folate = food.folate.formatField(),
                vitaminB1 = food.vitaminB1.formatField(),
                vitaminB2 = food.vitaminB2.formatField(),
                vitaminB3 = food.vitaminB3.formatField(),
                vitaminB6 = food.vitaminB6.formatField(),
            )
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        val servingGrams = s.servingGrams.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
        val entered = Nutrients(
            calories = s.calories.toDoubleOrZero(),
            carbs = s.carbs.toDoubleOrZero(),
            sugar = s.sugar.toDoubleOrZero(),
            fiber = s.fiber.toDoubleOrZero(),
            protein = s.protein.toDoubleOrZero(),
            fat = s.fat.toDoubleOrZero(),
            saturatedFat = s.saturatedFat.toDoubleOrZero(),
            omega3 = s.omega3.toDoubleOrZero(),
            sodium = s.sodium.toDoubleOrZero(),
            cholesterol = s.cholesterol.toDoubleOrZero(),
            calcium = s.calcium.toDoubleOrZero(),
            iron = s.iron.toDoubleOrZero(),
            magnesium = s.magnesium.toDoubleOrZero(),
            potassium = s.potassium.toDoubleOrZero(),
            zinc = s.zinc.toDoubleOrZero(),
            phosphorus = s.phosphorus.toDoubleOrZero(),
            selenium = s.selenium.toDoubleOrZero(),
            copper = s.copper.toDoubleOrZero(),
            manganese = s.manganese.toDoubleOrZero(),
            vitaminA = s.vitaminA.toDoubleOrZero(),
            vitaminC = s.vitaminC.toDoubleOrZero(),
            vitaminD = s.vitaminD.toDoubleOrZero(),
            vitaminE = s.vitaminE.toDoubleOrZero(),
            vitaminK = s.vitaminK.toDoubleOrZero(),
            vitaminB12 = s.vitaminB12.toDoubleOrZero(),
            folate = s.folate.toDoubleOrZero(),
            vitaminB1 = s.vitaminB1.toDoubleOrZero(),
            vitaminB2 = s.vitaminB2.toDoubleOrZero(),
            vitaminB3 = s.vitaminB3.toDoubleOrZero(),
            vitaminB6 = s.vitaminB6.toDoubleOrZero(),
        )
        // Storage is always per-100g — scale up when the user typed in per-serving.
        val per100g = when (s.basis) {
            NutritionBasis.Per100g -> entered
            NutritionBasis.PerServing -> entered * (100.0 / servingGrams)
        }
        viewModelScope.launch {
            repository.upsertFood(
                FoodEntity.fromNutrientsPer100g(
                    name = s.name.trim(),
                    brand = s.brand.trim().takeIf { it.isNotBlank() },
                    barcode = s.barcode?.takeIf { it.isNotBlank() },
                    category = s.category.name,
                    isCustom = true,
                    servingGrams = servingGrams,
                    servingLabel = s.servingLabel.ifBlank { "1 serving (${servingGrams.toInt()} g)" },
                    n = per100g,
                )
            )
            _state.update { it.copy(saved = true) }
        }
    }
}

private fun String.toDoubleOrZero(): Double = this.toDoubleOrNull() ?: 0.0
private fun Double.formatGrams(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
private fun Double.formatField(): String =
    if (this == 0.0) "" else if (this >= 100) this.toInt().toString() else "%.2f".format(this)
