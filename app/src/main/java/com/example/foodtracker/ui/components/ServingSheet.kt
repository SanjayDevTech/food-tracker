package com.example.foodtracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.db.FoodEntity

/** Sentinel prefix on a [ServingVariant.label] marking the free-form
 *  custom-grams mode in [ServingSheet]. The remainder of the label is the
 *  grams value, kept human-readable so the diary still renders sensibly. */
const val CUSTOM_GRAMS_PREFIX = "Custom · "

/**
 * Reusable bottom-sheet UI for picking a serving variant (or custom grams) plus
 * servings and meal slot before logging a food. Lives in `components` so both
 * the LogFood picker flow and Home's Quick log shortcut can pop the same UI —
 * users get a consistent confirmation step everywhere except the home-screen
 * widget (which stays one-tap by design).
 */
@Composable
fun ServingSheet(
    food: FoodEntity,
    meal: MealType,
    variant: ServingVariant,
    servings: Double,
    onVariantChange: (ServingVariant) -> Unit,
    onServingsChange: (Double) -> Unit,
    onMealChange: (MealType) -> Unit,
    onConfirm: () -> Unit,
) {
    val variants = food.variantsOrDefault()
    // Custom-grams mode is signalled by a sentinel label prefix. In this mode
    // `variant.grams` IS the gram value the user typed; servings is pinned to
    // 1.0 so the existing log-write math (servings × variant.grams) still works.
    val isCustomGrams = variant.label.startsWith(CUSTOM_GRAMS_PREFIX)
    val effectiveGrams = if (isCustomGrams) variant.grams else variant.grams * servings
    val n = food.nutrientsPer100g() * (effectiveGrams / 100.0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(food.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (!food.brand.isNullOrBlank()) {
            Text(
                food.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Serving size", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        // Variants live in a horizontally-scrollable row so longer labels like
        // "1 masala dosa (150 g)" never get clipped. Last chip is the
        // free-form custom-grams toggle.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            variants.forEach { v ->
                HapticFilterChip(
                    selected = !isCustomGrams && v.label == variant.label && v.grams == variant.grams,
                    onClick = {
                        onVariantChange(v)
                        onServingsChange(1.0)
                    },
                    label = { Text("${v.label} · ${v.grams.toInt()} g", maxLines = 1) },
                )
            }
            HapticFilterChip(
                selected = isCustomGrams,
                onClick = {
                    // Seed the custom-grams editor with the gram total the
                    // user is currently looking at — feels continuous when
                    // they tap "Custom g" mid-adjustment.
                    val seed = (variant.grams * servings).coerceIn(1.0, 2000.0)
                    onVariantChange(ServingVariant("${CUSTOM_GRAMS_PREFIX}${seed.toInt()} g", seed))
                    onServingsChange(1.0)
                },
                label = { Text("Custom g", maxLines = 1) },
            )
        }

        Spacer(Modifier.height(12.dp))
        if (isCustomGrams) {
            Text(
                "Grams: ${variant.grams.toInt()} g",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberStepper(
                    value = "${variant.grams.toInt()} g",
                    onDecrement = {
                        val next = (variant.grams - 5.0).coerceAtLeast(1.0)
                        onVariantChange(ServingVariant("${CUSTOM_GRAMS_PREFIX}${next.toInt()} g", next))
                    },
                    onIncrement = {
                        val next = (variant.grams + 5.0).coerceAtMost(2000.0)
                        onVariantChange(ServingVariant("${CUSTOM_GRAMS_PREFIX}${next.toInt()} g", next))
                    },
                    decrementEnabled = variant.grams > 1.0,
                    incrementEnabled = variant.grams < 2000.0,
                )
            }
            Spacer(Modifier.height(4.dp))
            HapticSlider(
                value = variant.grams.toFloat().coerceIn(5f, 500f),
                onValueChange = { v ->
                    val rounded = v.toDouble().coerceIn(1.0, 2000.0)
                    onVariantChange(ServingVariant("${CUSTOM_GRAMS_PREFIX}${rounded.toInt()} g", rounded))
                },
                valueRange = 5f..500f,
                steps = 98,
                granularity = 5f,
            )
        } else {
            Text(
                "Servings: ${"%.2f".format(servings)} · ${(variant.grams * servings).toInt()} g",
                style = MaterialTheme.typography.titleMedium,
            )
            HapticSlider(
                value = servings.toFloat().coerceIn(0.25f, 5f),
                onValueChange = { onServingsChange(it.toDouble()) },
                valueRange = 0.25f..5f,
                steps = 18,
                granularity = 0.25f,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("Meal", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        ExclusiveButtonGroup(
            options = MealType.entries,
            selected = meal,
            onSelect = onMealChange,
            modifier = Modifier.fillMaxWidth(),
            label = { it.displayName },
        )

        Spacer(Modifier.height(16.dp))
        NutrientPreview(n)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            val buttonText = if (isCustomGrams) {
                "Add ${variant.grams.toInt()} g"
            } else {
                "Add ${"%.1f".format(servings)} × ${variant.label}"
            }
            Text(
                buttonText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NutrientPreview(n: Nutrients) {
    val items = listOf(
        "Calories" to "${n.calories.toInt()} kcal",
        "Carbs" to "${"%.1f".format(n.carbs)} g",
        "Protein" to "${"%.1f".format(n.protein)} g",
        "Fat" to "${"%.1f".format(n.fat)} g",
        "Fiber" to "${"%.1f".format(n.fiber)} g",
        "Sodium" to "${n.sodium.toInt()} mg",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (k, v) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
