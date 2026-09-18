package com.example.foodtracker.domain

import com.example.foodtracker.data.ActivityLevel
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.NutrientGoals
import com.example.foodtracker.data.Sex
import com.example.foodtracker.data.UserProfile
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Computes daily nutrient goals from a user profile using Mifflin-St Jeor
 * for BMR and a configurable activity multiplier for TDEE. Macro splits
 * follow common recommendations (40% carbs / 30% protein / 30% fat) with
 * adjustments for the chosen goal. Micronutrient goals follow the Indian
 * ICMR RDA where reasonable, with sensible defaults otherwise.
 */
object GoalCalculator {

    fun bmr(profile: UserProfile): Double {
        // Mifflin-St Jeor
        val base = (10.0 * profile.weightKg) + (6.25 * profile.heightCm) - (5.0 * profile.ageYears)
        return when (profile.sex) {
            Sex.Male -> base + 5.0
            Sex.Female -> base - 161.0
        }
    }

    fun tdee(profile: UserProfile): Double = bmr(profile) * profile.activityLevel.multiplier

    /**
     * Signed daily kcal delta for the chosen goal at the chosen pace. Lose
     * subtracts, Gain adds, Maintain is always zero. Magnitude follows the
     * ~7700 kcal-per-kg rule scaled to a weekly target.
     */
    fun calorieAdjustment(profile: UserProfile): Double = when (profile.goal) {
        Goal.Maintain -> 0.0
        Goal.Lose -> -profile.weightChangeRate.kcalPerDay
        Goal.Gain -> profile.weightChangeRate.kcalPerDay
    }

    fun targetCalories(profile: UserProfile): Double =
        max(1200.0, tdee(profile) + calorieAdjustment(profile))

    fun goalsFor(profile: UserProfile): NutrientGoals {
        val cals = targetCalories(profile)

        val proteinG = when (profile.goal) {
            Goal.Gain -> profile.weightKg * 1.8
            Goal.Lose -> profile.weightKg * 1.6
            Goal.Maintain -> profile.weightKg * 1.2
        }
        val fatG = (cals * 0.28) / 9.0
        val proteinCals = proteinG * 4.0
        val fatCals = fatG * 9.0
        val carbCals = max(0.0, cals - proteinCals - fatCals)
        val carbsG = carbCals / 4.0
        val fiberG = max(25.0, cals / 1000.0 * 14.0)
        val sugarG = (cals * 0.10) / 4.0
        val satFatG = (cals * 0.10) / 9.0

        // ICMR-leaning micro RDA defaults
        val isFemale = profile.sex == Sex.Female
        return NutrientGoals(
            calories = cals.roundTo(0),
            carbs = carbsG.roundTo(0),
            protein = proteinG.roundTo(0),
            fat = fatG.roundTo(0),
            fiber = fiberG.roundTo(0),
            sugar = sugarG.roundTo(0),
            saturatedFat = satFatG.roundTo(0),
            omega3 = 1.6,
            cholesterol = 300.0,
            sodium = 2300.0,
            calcium = 1000.0,
            iron = if (isFemale) 21.0 else 11.0,
            magnesium = if (isFemale) 310.0 else 400.0,
            potassium = 3500.0,
            zinc = if (isFemale) 8.0 else 11.0,
            phosphorus = 700.0,
            selenium = 55.0,
            copper = 0.9,
            manganese = if (isFemale) 1.8 else 2.3,
            vitaminA = 900.0,
            vitaminC = if (isFemale) 75.0 else 90.0,
            vitaminD = 15.0,
            vitaminE = 15.0,
            vitaminK = if (isFemale) 90.0 else 120.0,
            vitaminB12 = 2.4,
            folate = 400.0,
            vitaminB1 = if (isFemale) 1.1 else 1.2,
            vitaminB2 = if (isFemale) 1.1 else 1.3,
            vitaminB3 = if (isFemale) 14.0 else 16.0,
            vitaminB6 = 1.3,
            waterMl = (profile.weightKg * 35.0).roundToInt().coerceAtLeast(1500),
        )
    }

    private fun Double.roundTo(decimals: Int): Double {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        return Math.round(this * factor) / factor
    }
}
