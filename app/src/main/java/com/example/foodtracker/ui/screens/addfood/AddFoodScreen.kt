package com.example.foodtracker.ui.screens.addfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.FoodCategory
import com.example.foodtracker.data.NutritionBasis
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onBack: () -> Unit,
    prefillBarcode: String? = null,
) {
    val vm: AddFoodViewModel = appViewModel { AddFoodViewModel(it.repository, it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    LaunchedEffect(prefillBarcode) {
        if (!prefillBarcode.isNullOrBlank() && state.barcode.isNullOrBlank()) {
            vm.seedBarcode(prefillBarcode)
        }
    }

    AiProgressDialog(state.aiStep, verifies = state.aiVerifies)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add custom food") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val servingGramsInt = state.servingGrams.toDoubleOrNull()?.toInt() ?: 100
            val basisSuffix = when (state.basis) {
                NutritionBasis.Per100g -> "per 100 g"
                NutritionBasis.PerServing -> "per ${servingGramsInt} g serving"
            }

            Text(
                "Enter nutrients $basisSuffix. Leave fields empty if unknown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.barcode?.takeIf { it.isNotBlank() }?.let { code ->
                Text(
                    "Barcode: $code",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> vm.update { it.copy(name = v) } },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.aiEnabled) {
                OutlinedButton(
                    onClick = {
                        haptics.light()
                        vm.autofillFromAi()
                    },
                    enabled = state.name.isNotBlank() && state.aiStep == AiStep.Idle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("✨ Ask AI to fill nutrition")
                }
                state.aiError?.let { err ->
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = state.brand,
                onValueChange = { v -> vm.update { it.copy(brand = v) } },
                label = { Text("Brand (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            CategoryChips(
                selected = state.category,
                onSelect = { cat -> vm.update { it.copy(category = cat) } },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.servingGrams,
                    onValueChange = { v -> vm.update { it.copy(servingGrams = v.filter { ch -> ch.isDigit() || ch == '.' }) } },
                    label = { Text("Serving (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.servingLabel,
                    onValueChange = { v -> vm.update { it.copy(servingLabel = v) } },
                    label = { Text("Serving label") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f),
                )
            }

            Text("Values you're entering are:", style = MaterialTheme.typography.labelLarge)
            BasisRow(
                basis = state.basis,
                servingGramsInt = servingGramsInt,
                onChange = { b -> vm.update { it.copy(basis = b) } },
            )

            SectionHeader("Macros ($basisSuffix)")
            NumberField("Calories (kcal)", state.calories) { v -> vm.update { it.copy(calories = v) } }
            NumberField("Carbs (g)", state.carbs) { v -> vm.update { it.copy(carbs = v) } }
            NumberField("Sugar (g)", state.sugar) { v -> vm.update { it.copy(sugar = v) } }
            NumberField("Fiber (g)", state.fiber) { v -> vm.update { it.copy(fiber = v) } }
            NumberField("Protein (g)", state.protein) { v -> vm.update { it.copy(protein = v) } }
            NumberField("Fat (g)", state.fat) { v -> vm.update { it.copy(fat = v) } }
            NumberField("Saturated fat (g)", state.saturatedFat) { v -> vm.update { it.copy(saturatedFat = v) } }
            NumberField("Omega-3 (g)", state.omega3) { v -> vm.update { it.copy(omega3 = v) } }
            NumberField("Cholesterol (mg)", state.cholesterol) { v -> vm.update { it.copy(cholesterol = v) } }
            NumberField("Sodium (mg)", state.sodium) { v -> vm.update { it.copy(sodium = v) } }

            SectionHeader("Minerals ($basisSuffix)")
            NumberField("Calcium (mg)", state.calcium) { v -> vm.update { it.copy(calcium = v) } }
            NumberField("Iron (mg)", state.iron) { v -> vm.update { it.copy(iron = v) } }
            NumberField("Magnesium (mg)", state.magnesium) { v -> vm.update { it.copy(magnesium = v) } }
            NumberField("Potassium (mg)", state.potassium) { v -> vm.update { it.copy(potassium = v) } }
            NumberField("Zinc (mg)", state.zinc) { v -> vm.update { it.copy(zinc = v) } }
            NumberField("Phosphorus (mg)", state.phosphorus) { v -> vm.update { it.copy(phosphorus = v) } }
            NumberField("Selenium (µg)", state.selenium) { v -> vm.update { it.copy(selenium = v) } }
            NumberField("Copper (mg)", state.copper) { v -> vm.update { it.copy(copper = v) } }
            NumberField("Manganese (mg)", state.manganese) { v -> vm.update { it.copy(manganese = v) } }

            SectionHeader("Vitamins ($basisSuffix)")
            NumberField("Vitamin A (µg)", state.vitaminA) { v -> vm.update { it.copy(vitaminA = v) } }
            NumberField("Vitamin C (mg)", state.vitaminC) { v -> vm.update { it.copy(vitaminC = v) } }
            NumberField("Vitamin D (µg)", state.vitaminD) { v -> vm.update { it.copy(vitaminD = v) } }
            NumberField("Vitamin E (mg)", state.vitaminE) { v -> vm.update { it.copy(vitaminE = v) } }
            NumberField("Vitamin K (µg)", state.vitaminK) { v -> vm.update { it.copy(vitaminK = v) } }
            NumberField("Vitamin B1 / Thiamin (mg)", state.vitaminB1) { v -> vm.update { it.copy(vitaminB1 = v) } }
            NumberField("Vitamin B2 / Riboflavin (mg)", state.vitaminB2) { v -> vm.update { it.copy(vitaminB2 = v) } }
            NumberField("Vitamin B3 / Niacin (mg)", state.vitaminB3) { v -> vm.update { it.copy(vitaminB3 = v) } }
            NumberField("Vitamin B6 (mg)", state.vitaminB6) { v -> vm.update { it.copy(vitaminB6 = v) } }
            NumberField("Vitamin B12 (µg)", state.vitaminB12) { v -> vm.update { it.copy(vitaminB12 = v) } }
            NumberField("Folate (µg)", state.folate) { v -> vm.update { it.copy(folate = v) } }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    vm.save()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.name.isNotBlank(),
            ) { Text("Save food") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { ch -> ch.isDigit() || ch == '.' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BasisRow(
    basis: NutritionBasis,
    servingGramsInt: Int,
    onChange: (NutritionBasis) -> Unit,
) {
    ExclusiveButtonGroup(
        options = listOf(NutritionBasis.Per100g, NutritionBasis.PerServing),
        selected = basis,
        onSelect = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            if (it == NutritionBasis.Per100g) "Per 100 g" else "Per $servingGramsInt g serving"
        },
    )
}

@Composable
private fun CategoryChips(selected: FoodCategory, onSelect: (FoodCategory) -> Unit) {
    // One expressive ButtonGroup per row of 3 — the connected shape applies
    // inside each row; multi-line text is allowed so long category names stay
    // legible instead of ellipsing.
    val rows = FoodCategory.entries.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            ExclusiveButtonGroup(
                options = row,
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
                label = { it.displayName },
            )
        }
    }
}
