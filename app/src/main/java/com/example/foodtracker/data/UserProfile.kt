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
)
