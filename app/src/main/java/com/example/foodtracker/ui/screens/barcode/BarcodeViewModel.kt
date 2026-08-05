package com.example.foodtracker.ui.screens.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.data.BarcodeProduct
import com.example.foodtracker.data.BarcodeSource
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutritionBasis
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BarcodeUiState(
    val scanning: Boolean = true,
    val lastBarcode: String? = null,
    /** Fresh hit from OFF/USDA — both bases available, user picks via [basis]. */
    val product: BarcodeProduct? = null,
    /** Stale cached row from the local DB; shown only when every online source missed. */
    val cached: FoodEntity? = null,
    val resolvedSource: BarcodeSource? = null,
    val basis: NutritionBasis = NutritionBasis.Per100g,
    /**
     * Serving size the user is about to log (grams). Initialised from the scanned
     * product but editable on the resolve panel — overrides the source default
     * when the printed pack disagrees with the contributor data.
     */
    val servingGrams: Double = 0.0,
    val notFound: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
)

class BarcodeViewModel(private val repository: FoodRepository) : ViewModel() {

    private val _state = MutableStateFlow(BarcodeUiState())
    val state: StateFlow<BarcodeUiState> = _state.asStateFlow()

    fun onBarcode(barcode: String) {
        if (!_state.value.scanning) return
        _state.update {
            it.copy(
                scanning = false,
                lastBarcode = barcode,
                isLoading = true,
                error = null,
                notFound = false,
            )
        }
        viewModelScope.launch {
            try {
                val lookup = repository.fetchByBarcode(barcode)
                when {
                    lookup.product != null -> _state.update {
                        it.copy(
                            isLoading = false,
                            product = lookup.product,
                            cached = null,
                            resolvedSource = lookup.source,
                            basis = lookup.product.defaultBasis,
                            servingGrams = lookup.product.servingGrams,
                        )
                    }
                    lookup.cached != null -> _state.update {
                        it.copy(
                            isLoading = false,
                            product = null,
                            cached = lookup.cached,
                            resolvedSource = lookup.source,
                            servingGrams = lookup.cached.servingGrams,
                        )
                    }
                    else -> _state.update { it.copy(isLoading = false, notFound = true) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Lookup failed") }
            }
        }
    }

    fun setBasis(basis: NutritionBasis) {
        _state.update { it.copy(basis = basis) }
    }

    /** Override the source's serving size before commit. Clamped to [1, 2000] g. */
    fun setServingGrams(grams: Double) {
        val clamped = grams.coerceIn(1.0, 2000.0)
        _state.update { it.copy(servingGrams = clamped) }
    }

    fun retry() {
        _state.update { BarcodeUiState() }
    }

    fun quickLog(meal: MealType, onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            val edited = s.servingGrams.takeIf { it > 0.0 }
            val food = when {
                s.product != null -> {
                    // If the user edited the serving, persist it on the food row
                    // and rebuild the label so future scans/logs show the override.
                    val labelOverride = edited?.let { g ->
                        if (g != s.product.servingGrams) "1 serving (${g.toInt()} g)" else null
                    }
                    repository.commitBarcodeProduct(
                        product = s.product,
                        basis = s.basis,
                        servingGrams = edited,
                        servingLabel = labelOverride,
                    )
                }
                s.cached != null -> s.cached
                else -> return@launch
            }
            val gramsForLog = edited ?: food.servingGrams
            repository.addFoodLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = meal.name,
                    servings = 1.0,
                    grams = gramsForLog,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
            onDone()
        }
    }
}
