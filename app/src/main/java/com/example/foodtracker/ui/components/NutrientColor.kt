package com.example.foodtracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.foodtracker.data.NutrientCategory
import com.example.foodtracker.data.NutrientField
import com.example.foodtracker.ui.theme.MacroBurned
import com.example.foodtracker.ui.theme.MacroCarbs
import com.example.foodtracker.ui.theme.MacroFat
import com.example.foodtracker.ui.theme.MacroFiber
import com.example.foodtracker.ui.theme.MacroProtein
import com.example.foodtracker.ui.theme.MacroWater

/**
 * The colour a nutrient is drawn in, wherever it is drawn.
 *
 * Shared rather than per-screen so protein is the same colour on the home
 * bars and in a search result — a reader who has learned one has learned the
 * other. The named macros get their own hue; everything else falls back to a
 * per-category colour, which is enough to tell a mineral from a vitamin
 * without inventing thirty distinct hues nobody could tell apart.
 */
fun NutrientField.color(): Color = when (key) {
    "calories" -> MacroBurned
    "carbs" -> MacroCarbs
    "protein" -> MacroProtein
    "fat", "saturatedFat" -> MacroFat
    "fiber" -> MacroFiber
    else -> when (category) {
        NutrientCategory.Macros -> MacroBurned
        NutrientCategory.Minerals -> MacroFiber
        NutrientCategory.Vitamins -> MacroWater
    }
}
