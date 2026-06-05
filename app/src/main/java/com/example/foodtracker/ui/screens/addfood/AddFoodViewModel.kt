package com.example.foodtracker.ui.screens.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.FoodCategory
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.NutritionBasis
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.db.FoodEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val sodium: String = "",
    val cholesterol: String = "",
    val calcium: String = "",
    val iron: String = "",
    val magnesium: String = "",
    val potassium: String = "",
    val zinc: String = "",
    val vitaminA: String = "",
    val vitaminC: String = "",
    val vitaminD: String = "",
    val vitaminE: String = "",
    val vitaminK: String = "",
    val vitaminB12: String = "",
    val folate: String = "",
    val saved: Boolean = false,
)

class AddFoodViewModel(private val repository: FoodRepository) : ViewModel() {

    private val _state = MutableStateFlow(AddFoodFormState())
    val state: StateFlow<AddFoodFormState> = _state.asStateFlow()

    fun update(transform: (AddFoodFormState) -> AddFoodFormState) {
        _state.update(transform)
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
                sodium = food.sodium.formatField(),
                cholesterol = food.cholesterol.formatField(),
                calcium = food.calcium.formatField(),
                iron = food.iron.formatField(),
                magnesium = food.magnesium.formatField(),
                potassium = food.potassium.formatField(),
                zinc = food.zinc.formatField(),
                vitaminA = food.vitaminA.formatField(),
                vitaminC = food.vitaminC.formatField(),
                vitaminD = food.vitaminD.formatField(),
                vitaminE = food.vitaminE.formatField(),
                vitaminK = food.vitaminK.formatField(),
                vitaminB12 = food.vitaminB12.formatField(),
                folate = food.folate.formatField(),
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
            sodium = s.sodium.toDoubleOrZero(),
            cholesterol = s.cholesterol.toDoubleOrZero(),
            calcium = s.calcium.toDoubleOrZero(),
            iron = s.iron.toDoubleOrZero(),
            magnesium = s.magnesium.toDoubleOrZero(),
            potassium = s.potassium.toDoubleOrZero(),
            zinc = s.zinc.toDoubleOrZero(),
            vitaminA = s.vitaminA.toDoubleOrZero(),
            vitaminC = s.vitaminC.toDoubleOrZero(),
            vitaminD = s.vitaminD.toDoubleOrZero(),
            vitaminE = s.vitaminE.toDoubleOrZero(),
            vitaminK = s.vitaminK.toDoubleOrZero(),
            vitaminB12 = s.vitaminB12.toDoubleOrZero(),
            folate = s.folate.toDoubleOrZero(),
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
private fun Double.formatField(): String =
    if (this == 0.0) "" else if (this >= 100) this.toInt().toString() else "%.2f".format(this)
