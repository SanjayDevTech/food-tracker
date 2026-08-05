package com.example.foodtracker.data

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val name: String = "",
    val ageYears: Int = 25,
    val sex: Sex = Sex.Male,
    val heightCm: Int = 170,
    val weightKg: Double = 65.0,
    val activityLevel: ActivityLevel = ActivityLevel.Moderate,
    val goal: Goal = Goal.Maintain,
    val weightChangeRate: WeightChangeRate = WeightChangeRate.Standard,
    /**
     * Target weight in kg, only meaningful when [goal] is `Lose` or `Gain`.
     * Null = not set; the app then shows no progress banner and no goal marker
     * in the BMI visual. The caloric deficit/surplus is still driven entirely
     * by [weightChangeRate]; this field is purely for progress feedback.
     */
    val goalWeightKg: Double? = null,
)
