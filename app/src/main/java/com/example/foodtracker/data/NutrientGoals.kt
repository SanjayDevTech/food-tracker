package com.example.foodtracker.data

import androidx.compose.runtime.Immutable

@Immutable
data class NutrientGoals(
    val calories: Double,
    val carbs: Double,
    val sugar: Double,
    val fiber: Double,
    val protein: Double,
    val fat: Double,
    val saturatedFat: Double,
    val cholesterol: Double,
    val sodium: Double,
    val calcium: Double,
    val iron: Double,
    val magnesium: Double,
    val potassium: Double,
    val zinc: Double,
    val vitaminA: Double,
    val vitaminC: Double,
    val vitaminD: Double,
    val vitaminE: Double,
    val vitaminK: Double,
    val vitaminB12: Double,
    val folate: Double,
    val waterMl: Int,
)
