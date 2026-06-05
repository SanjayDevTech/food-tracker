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
                        )
                    }
                    lookup.cached != null -> _state.update {
                        it.copy(
                            isLoading = false,
                            product = null,
                            cached = lookup.cached,
                            resolvedSource = lookup.source,
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

    fun retry() {
        _state.update { BarcodeUiState() }
    }

    fun quickLog(meal: MealType, onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            val food = when {
                s.product != null -> repository.commitBarcodeProduct(s.product, s.basis)
                s.cached != null -> s.cached
                else -> return@launch
            }
            repository.addFoodLog(
                FoodLogEntity(
                    foodId = food.id,
                    dateEpochDay = DateUtil.todayEpochDay(),
                    mealType = meal.name,
                    servings = 1.0,
                    grams = food.servingGrams,
                    loggedAtMillis = System.currentTimeMillis(),
                )
            )
            onDone()
        }
    }
}
